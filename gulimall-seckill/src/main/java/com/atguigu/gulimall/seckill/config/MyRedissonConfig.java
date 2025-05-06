package com.atguigu.gulimall.seckill.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson得到使用都是通过RedissonClient对象
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        // 1、创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://169.254.139.10:6379")
                                .setConnectionPoolSize(30)
                                .setConnectTimeout(10000)
                                .setTimeout(10000);
        // 2、根据Config创建出RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);

        return redissonClient;
    }
}
