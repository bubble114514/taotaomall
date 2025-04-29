package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {
//    @Autowired
//    private AmqpAdmin amqpAdmin;
//    @Autowired
//    private RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessage() {


        //1、发送消息,如果发送的消息是对象，会使用序列化机制，将对象写出去。对象必须实现Serializable接口
        String msg = "Hello World";
        //2、发送的对象类型的消息可以是一个JSON
//        for (int i = 0; i < 10; i++){
//            if (i % 2 == 0){
//                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
//                reasonEntity.setId(1L);
//                reasonEntity.setCreateTime(new Date());
//                reasonEntity.setName("哈哈--" + i);
//                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reasonEntity);
//            }else {
//                OrderEntity orderEntity = new OrderEntity();
//                orderEntity.setOrderSn(UUID.randomUUID().toString());
//                orderEntity.setCreateTime(new Date());
//                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderEntity);
//            }
//            log.info("消息发送完毕...{}");
//        }


    }

    /**
     * 1、如何创建Exchange、Queue、Binding
     *      1）、使用AmqpAdmin创建
     *      2）、通过SpringProvide自动创建
     * 2、如何收发消息
     */
    @Test
    void createExchange() {
        //amqpAdmin
        //Exchange
        /**
         * Direct Exchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments )
         */
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java-exchange");
    }

    @Test
    void creatQueue(){
        Queue queue = new Queue("hello-java-queue",  true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Exchange[{}]创建成功", "hello-java-queue");
    }

    @Test
    void createBinding(){
        // String destination【目的地】,
        // DestinationType destinationType【目的地类型】,
        // String exchange【交换机】,
        // String routingKey【路由键】,
        // Map<String, Object> arguments【参数】
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", "hello-java-binding");
    }

}
