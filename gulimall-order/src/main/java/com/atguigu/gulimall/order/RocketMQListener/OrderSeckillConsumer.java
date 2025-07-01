//package com.atguigu.gulimall.order.RocketMQListener;
//
//import com.atguigu.common.to.mq.SeckillOrderTo;
//import com.atguigu.gulimall.order.service.OrderService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.ConsumeMode;
//import org.apache.rocketmq.spring.annotation.MessageModel;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//// OrderSeckillConsumer.java
//@Slf4j
//@Component
//@RocketMQMessageListener(
//        topic = "seckill-order-topic",
//        consumerGroup = "seckill-order-consumer-group",
//        consumeMode = ConsumeMode.CONCURRENTLY, // 并发消费
//        messageModel = MessageModel.CLUSTERING  // 集群模式
//)
//public class OrderSeckillConsumer implements RocketMQListener<SeckillOrderTo> {
//
//    @Autowired
//    private OrderService orderService;
//
//    @Override
//    public void onMessage(SeckillOrderTo seckillOrder) {
//        try {
//            log.info("接收到秒杀订单消息: {}", seckillOrder.getOrderSn());
//            orderService.createSeckillOrder(seckillOrder);
//        } catch (Exception e) {
//            log.error("秒杀订单处理失败: {}", seckillOrder.getOrderSn(), e);
//            throw new RuntimeException(e); // 触发重试
//        }
//    }
//}
