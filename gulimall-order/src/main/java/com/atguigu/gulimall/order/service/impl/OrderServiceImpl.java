package com.atguigu.gulimall.order.service.impl;


import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.order.OrderConstant;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.exception.NoStockException;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        //获取主线程的请求域
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //将主线程的请求域共享给子线程
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //1、远程查询所有的收货地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(loginUser.getId());
            confirmVo.setMemberAddressVos(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //将主线程的请求域共享给子线程
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //2、远程查询购物车所有选中的购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<Long> idCollect = confirmVo.getItems().stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R<List<SkuStockVo>> hasStock = wmsFeignService.getSkuHasStock(idCollect);
            List<SkuStockVo> data = hasStock.getData("data", new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }

        }, executor);


        //3、查询用户积分
        Integer integration = loginUser.getIntegration();
        confirmVo.setIntegration(integration);

        //4、其他数据【如总价】，自动计算

        //TODO 5、防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + loginUser.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setUniqueToken(token);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }
    //本地事务，在分布式系统喜爱只能控制自己的回滚，控制不了其他事务的回滚
    //分布式事务：使用分布式事务的最大原因：网络问题+分布式机器

//    @GlobalTransactional 性能低，无法高并发
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        submitVoThreadLocal.set(submitVo);

        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        //创建订单，验令牌，验价格，锁库存，下订单，减库存，清空购物车
        //1、验证令牌【令牌的对比和删除必须保证原子性】
        // 原子性 lua脚本
        // 0-令牌失败 1-令牌成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = submitVo.getUniqueToken();
        // 原子验证和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Collections.singletonList(USER_ORDER_TOKEN_PREFIX + loginUser.getId()),
                orderToken);
        //TODO 测试完去掉注释符号
//        if (result == 0L) {
//            // 令牌验证失败
//            response.setCode(1);
//            return response;
//
//        } else {
            // 令牌验证成功
            //下单：1、创建订单，创建订单项2、验价格，3、远程调用库存服务锁定库存，4、远程调用支付服务创建订单。
            //1、创建订单，订单项等信息
            OrderCreateTo order = createOrder();
            //2、验价格

            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();

            if (Math.abs(payPrice.subtract(payAmount).doubleValue()) < 0.01) {
                // 价格验证成功
                //TODO 3、保存订单
                saveOrder(order);
                //4、锁定库存，只要有异常，回滚订单数据
                //订单号，所有订单项：skuId，skuName，num
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                //订单项
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 4、远程锁库存
                //库存成功了，但是网络超时，订单回滚，库存已完成不会回滚
                //为保证高并发，库存自己回滚。可以发消息给库存服务。
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    // 锁定成功
                    response.setOrder(order.getOrder());
                    //TODO 5、扣减积分
//                    int i=10/0;
                    //TODO 订单创建成功发消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());

                    return response;
                } else {
                    //  锁定失败
                    Long msg= (Long) r.get("msg");
                    throw new NoStockException(msg);

                }
            } else {
                response.setCode(2);
                return response;
            }

    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //先查询当前订单的最新状态
        OrderEntity order = this.getById(orderEntity.getId());
        if(Objects.equals(order.getStatus(), OrderConstant.OrderStatusEnum.CREATE_NEW.getCode())){
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(order.getId());
            update.setStatus(OrderConstant.OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            //发给MQ一个
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order, orderTo);
            try {
                //TODO 确保消息发送成功,每一个消息都在数据库保存日志记录
                //TODO 定期扫描数据库，对没有发送成功的消息进行重新发送
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            }catch (Exception e){
                //TODO 将没发送成功的消息重试发送

            }
        }
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        //1、保存订单
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        //2、保存订单项
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、生成一个订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);

        //2、获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3、验价  计算价格相关
        assert itemEntities != null;
        computePrice(orderEntity, itemEntities);
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        orderCreateTo.setPayPrice(orderEntity.getPayAmount());


        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {

        BigDecimal totalAmount = new BigDecimal("0.0");
        //优惠价格
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        //订单的总额，叠加每一个订单项的总额信息
        for (OrderItemEntity itemEntity : itemEntities) {
            coupon = coupon.add(itemEntity.getCouponAmount());
            integration = integration.add(itemEntity.getIntegrationAmount());
            promotion = promotion.add(itemEntity.getPromotionAmount());
            totalAmount = totalAmount.add(itemEntity.getRealAmount());

            gift = gift.add(new BigDecimal(itemEntity.getGiftIntegration()));
            growth = growth.add(new BigDecimal(itemEntity.getGiftGrowth()));
        }
        //1、订单价格相关
        orderEntity.setTotalAmount(totalAmount);
        //应付金额
        orderEntity.setPayAmount(totalAmount.add(orderEntity.getFreightAmount()));
        //优惠金额
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);

        //

        //设置积分，成长值...
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();

        OrderEntity order = new OrderEntity();
        order.setMemberId(loginUser.getId());
        order.setOrderSn(orderSn);
        //获取当前用户所有地址
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = (FareVo) fare.getData("data", new TypeReference<FareVo>() {
        });
        //运费
        order.setFreightAmount(fareResp.getFare());
        //收货人信息
        order.setReceiverCity(fareResp.getAddress().getCity());
        order.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        order.setReceiverName(fareResp.getAddress().getName());
        order.setReceiverPhone(fareResp.getAddress().getPhone());
        order.setReceiverPostCode(fareResp.getAddress().getPostCode());
        order.setReceiverProvince(fareResp.getAddress().getProvince());
        order.setReceiverRegion(fareResp.getAddress().getRegion());
        order.setMemberId(loginUser.getId());
        order.setMemberUsername(loginUser.getUsername());

        order.setCreateTime(new Date());


        //设置订单状态信息
        order.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        order.setAutoConfirmDay(7);


        return order;
    }

    /**
     * 构建订单项数据
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && !currentUserCartItems.isEmpty()) {
            return currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());

        }
        return null;
    }

    /**
     * 构建每一个订单项
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1、订单信息：订单号，订单总额，运费信息，收货地址，用户信息，优惠信息，积分信息，成长值信息
        //2、商品spu信息
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        SpuInfoVo spuInfo = (SpuInfoVo) r.getData("data", new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfo.getId());
        itemEntity.setSpuName(spuInfo.getSpuName());
        itemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        itemEntity.setCategoryId(spuInfo.getCatalogId());

        //3、商品sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());

        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);

        itemEntity.setSkuQuantity(cartItem.getCount());
        //4、TODO 优惠信息

        //5、积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount())).intValue());

        //6、订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际金额
        BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal real = orign.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(real);

        return itemEntity;
    }

}