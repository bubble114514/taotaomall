//package com.atguigu.gulimall.order.config;
//
//import org.apache.commons.lang3.concurrent.BasicThreadFactory;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.concurrent.Executors;
//
//@Configuration
//public class RocketMQConfig {
//    @Bean
//    public RocketMQTemplate rocketMQTemplate(RocketMQTemplate template) {
//        template.setAsyncSenderExecutor(
//            Executors.newFixedThreadPool(32,
//                new BasicThreadFactory.Builder()
//                    .namingPattern("rmq-async-%d")
//                    .daemon(true)
//                    .build()
//            )
//        );
//        return template;
//    }
//}
