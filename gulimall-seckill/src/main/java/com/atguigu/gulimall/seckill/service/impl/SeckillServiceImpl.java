package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeiService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionWithSkusVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {


    @Autowired
    CouponFeiService couponFeiService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private static final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private static final String SKU_STOCK_SEMAPHORE = "seckill:stock:skus:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1.扫描最近三天需要参与秒杀的活动
        R session = couponFeiService.getLast3DaySession();
        if (session.getCode() == 0) {
            // 2.获取到活动中的所有商品
            List<SeckillSessionWithSkusVo> sessionData = (List<SeckillSessionWithSkusVo>) session.getData(new TypeReference<List<SeckillSessionWithSkusVo>>() {
            });
            // 3.缓存到Redis
            //  1）、缓存活动信息
            //key: starttime_endtime value: [skuIds]
            saveSessionInfos(sessionData);
            //  2）、缓存活动关联的商品
            //key: skuId value: [sessionId, startTime, endTime]
            saveSessionSkuInfos(sessionData);
        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1、确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        if (!keys.isEmpty()) {
            for (String key : keys) {
                //key: seckill:sessions:1746468000000_1746385200000
                String replace = key.replace(SESSION_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                Long start = Long.parseLong(s[0]);
                Long end = Long.parseLong(s[1]);
                if (time >= start && time <= end) {
                    //2、获取这个秒杀场次需要的所有商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    assert range != null;
                    List<String> list = hashOps.multiGet(range);
                    if (list != null && !list.isEmpty()) {
                        List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                            SeckillSkuRedisTo redisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
//                            redisTo.setRandomCode(null);//当前秒杀开始就需要随机码
                            return redisTo;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }
            }
        }
        return null;

    }

    @Override
    public SeckillSkuRedisTo getSekuSeckillInfo(Long skuId) {
        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
        //1、找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (!keys.isEmpty() && keys != null) {
            String reg = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(reg, key)) {
                    String value = hashOps.get(key);
                    redisTo = JSON.parseObject(value, SeckillSkuRedisTo.class);
                    if (redisTo != null) {
                        //随机码
                        long current = new Date().getTime();
                        if (current >= redisTo.getStartTime() && current <= redisTo.getEndTime()) {

                        } else {
                            redisTo.setRandomCode(null);
                        }

                        return redisTo;
                    }

                }
            }
        }

        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num)  {
        long s1=System.currentTimeMillis();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();

        //1、获取当前秒杀商品信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (s == null) {
            return null;
        } else {
            SeckillSkuRedisTo redisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
            //校验合法性
            //1、验证时间合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            if (System.currentTimeMillis() >= startTime && System.currentTimeMillis() <= endTime) {
                //2、验证随机码和商品id
                String skuId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (redisTo.getRandomCode().equals(key) && killId.equals(skuId)) {
                    //3、验证购买数量是否超过限制
                    if (new BigDecimal(num).compareTo(redisTo.getSeckillLimit()) <= 0) {
                        //4、验证是否已购买过
                        //幂等性：如果秒杀成功就去redis占位
                        String redisKey = loginUser.getId() + "_" + skuId;
                        //自动过期
                        long ttl = endTime - System.currentTimeMillis();

                        Boolean b = redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(num), ttl, TimeUnit.MILLISECONDS);
                        if (Boolean.TRUE.equals(b)){//占位成功，说明没有买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + redisTo.getRandomCode());
                            try {
                                boolean tryAcquire = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                                if (tryAcquire) {
                                    //秒杀成功
                                    //快速下单：发送MQ消息
                                    String timeId = IdWorker.getTimeId();
                                    SeckillOrderTo orderTo = new SeckillOrderTo();
                                    orderTo.setOrderSn(timeId);
                                    orderTo.setMemberId(loginUser.getId());
                                    orderTo.setNum(num);
                                    orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                    orderTo.setSkuId(redisTo.getSkuId());
                                    orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                    rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                    long s2=System.currentTimeMillis();
                                    log.info("秒杀成功，耗时："+(s2-s1));
                                    return timeId;
                                }else {
                                    return "手慢了，秒杀失败";
                                }
                            } catch (InterruptedException e) {
                                return "手慢了，秒杀失败";
                            }

                        }else {//占位失败，说明买过了
                            return "您已购买过此商品";
                        }
                    }else {
                        return "秒杀数量超过限额！";
                    }
                }else {
                    return "数据校验失败！";
                }
            }else {
                return "不在秒杀时间内！";
            }
        }

    }

    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);

            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                // 缓存活动信息
                redisTemplate.opsForList().leftPushAll(key, collect);
            }

        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionWithSkusVo> sessions) {
        sessions.forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().forEach(sku -> {
                //生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");

                if (Boolean.FALSE.equals(ops.hasKey(sku.getPromotionSessionId() + "_" + sku.getSkuId().toString()))) {
                    //缓存sku信息
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //1、sku的基本信息
                    R skuInfo = productFeignService.getSkuInfo(sku.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo info = (SkuInfoVo) skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }

                    //2、sku秒杀信息
                    BeanUtils.copyProperties(sku, redisTo);

                    //3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4、设置随机码 : 防止恶意攻击，用户秒杀接口被多次调用
                    redisTo.setRandomCode(token);

                    String s = JSON.toJSONString(redisTo);
                    ops.put(sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString(), s);
                    //如果当前这个场次的库存信息已经上架就不需要上架
                    //5、使用库存作为分布式的信号量 作用：限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(sku.getSeckillCount().intValue());
                }

            });
        });
    }
}
