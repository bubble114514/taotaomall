package com.atguigu.gulimall.order.service.impl;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import org.springframework.amqp.core.Message;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queue:声明的队列名称
     *
     * 参数可以写以下类型
     * 1、Message：消息本身；头+体
     * 2、T<发送消息的类型>：
     * 3、Channel：当前传输数据的通道
     *
     * Queue:可以很多人都来监听。只要一个消息被一个消费者消费，消息就从队列中删除；
     *      1）、订单服务启动多个:同一个消息，只能有一个客户端收到
     *      2）、只有一个消息完全处理完。方法运行结束，才可以接收下一个消息
     */

    @RabbitHandler
    public void recieveMessage(Message message, OrderReturnReasonEntity content, Channel channel) {

        byte[] body = message.getBody();
        System.out.println("监听到消息...："+message+"===》内容："+content);
        //消息头属性信息
        MessageProperties messageProperties = message.getMessageProperties();
//        Thread.sleep(3000);
        System.out.println("消息处理完成=>"+content.getName());

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag===>"+deliveryTag);
        try{

            if (deliveryTag % 2 == 0){
                //收货确认
                channel.basicAck(deliveryTag, false);
                System.out.println("签收了货物..."+deliveryTag);
            }else {
                //退货
                //basicNack(long deliveryTag, boolean multiple, boolean requeue)
                //multiple：是否批量   requeue：是否重新入队
                channel.basicNack(deliveryTag, false, true);
                //basicReject(long deliveryTag, boolean requeue)
//                channel.basicReject(deliveryTag, true);
                System.out.println("退货了..."+deliveryTag);
            }
        }catch (IOException e){
            // 网络中断、服务器挂了
        }
    }

    @RabbitHandler
    public void recieveMessage2(OrderEntity content) throws InterruptedException {

        System.out.println("监听到消息...："+content);

    }

}