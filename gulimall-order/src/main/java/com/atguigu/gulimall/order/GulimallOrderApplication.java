package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 使用RabbitMQ
 *      1、引入amqp场景；RabbitAutoConfiguration就会自动生效
 *      2、给容器中自动配置了
 *          RabbitTemplate、AmqpAdmin、RabbitMessagingTemplate、ConnectionFactory
 *          所有属性都是
 *          @ConfiguratonProperties(prefix = "spring.rabbitmq")
 *          public class RabbitAutoConfiguration {}
 *      3、给配置文件中配置spring.rabbitmaq.*配置
 *      4、@EnableRabbit开启使用RabbitMQ
 */
@EnableRabbit
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
