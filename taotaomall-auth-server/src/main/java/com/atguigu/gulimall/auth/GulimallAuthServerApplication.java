package com.atguigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * SpringSession核心原理
 * 1）、@EnableRedisHttpSession导入RedisHttpSessionConfiguration配置
 *      1、给容器添加了一个组件
 *          RedisIndexedSessionRepository：redis操作session。session的增删改查封装类
 *      2、SessionRepositoryFilter==》Filter：session存储过滤器：每个请求过来都必须经过filter；
 *          1、创建的时候，就自动创建了一个redis操作session的组件
 *          2、原始的Request，原始的Response，都被包装了，sessionRepositoryRequestWrapper，sessionRepositoryResponseWrapper
 *          3、以后取得session，request.getSession();
 *          4、以后存取session，sessionRepositoryRequestWrapper.getSession();===》sessionRepositoryResponse中获取的
 *   装饰者模式
 *   自动延期：redis中key的过期时间是30分钟，如果请求过来，session刚过期，就会自动续期，重新设置过期时间。
 *
 */
@EnableRedisHttpSession // 整合redis作为session存储
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthServerApplication.class, args);
    }

}
