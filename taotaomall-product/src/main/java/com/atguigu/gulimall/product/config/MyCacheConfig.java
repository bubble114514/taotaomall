package com.atguigu.gulimall.product.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 配置类，用于自定义Redis缓存配置。
 * 该类启用了Spring的缓存功能，并定义了一个Redis缓存配置Bean。
 */

@EnableConfigurationProperties(CacheProperties.class)
@Configuration
@EnableCaching
public class MyCacheConfig {

//    @Autowired
//    private CacheProperties cacheProperties;
    /**
     * 配置文件中的配置没有生效
     * 1、原来和配置文件绑定的配置类是 @ConfigurationProperties(prefix = "spring.cache")
     * 2、要让他生效：1、@EnableConfigurationProperties(CacheProperties.class)
     *
     * 创建并配置一个RedisCacheConfiguration Bean。
     * 该配置指定了缓存键和值的序列化方式，键使用StringRedisSerializer，值使用GenericJackson2JsonRedisSerializer。
     *
     * @return 配置好的RedisCacheConfiguration实例
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        // 获取默认的Redis缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        // 配置缓存键的序列化方式为StringRedisSerializer
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        // 配置缓存值的序列化方式为GenericJackson2JsonRedisSerializer
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        //将配置文件中的所有配置生效
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        //设置配置文件中的各项配置，如过期时间
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }

        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
