package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：
 *      1、定时任务：@EnableScheduling
 *      2、任务执行：@Scheduled(cron = "* * * * * ?")
 * 异步任务：
 *      1、@EnableAsync  开启异步任务功能
 *      2、异步任务：@Async
 *      3、自动配置类：TaskSchedulerAutoConfiguration 属性绑定在  TaskSchedulerProperties 类中
 *
 */
@Slf4j
@Component
@EnableAsync
@EnableScheduling
public class HelloScheduled {

    /**
     * 1、spring中6位组成的表达式，秒、分、时、日、月、周几
     * 2、周几的位置：1-7，代表周一 - 周日
     * 3、定时任务不应该阻塞。默认是阻塞的；
     *      1、可以让业务运行以异步的方式，自己提交到线程池，不阻塞主线程。
     *      2、支持定时任务线程池，设置参数spring.task.execution.pool.core-size=8
     *      3、让定时任务异步执行
     */
//    @Async
//    @Scheduled(cron = "*/5 * * * * 7")
//    public void hello() {
//        log.info("hello...");
//    }
}
