package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架：
 *    每天晚上3点：扫描最近3天需要上架的商品，上架到Redis中
 *    当天00:00 - 23:59
 *    明天00:00 - 23:59
 *    后天00:00 - 23:59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {
    @Autowired
    private SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    //TODO 幂等性处理
    @Scheduled(cron = "0 0 * * * ?")
    public void uploadSeckillSkuLatest3Days() {
        //1、重复上架无需处理
        log.info("上架秒杀的商品信息...");
        //分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }

    }
}
