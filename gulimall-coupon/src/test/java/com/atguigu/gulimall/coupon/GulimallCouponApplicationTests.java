package com.atguigu.gulimall.coupon;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@SpringBootTest
class GulimallCouponApplicationTests {

    @Test
    void contextLoads() {
        //计算最近三天
        LocalDate now = LocalDate.now();
        LocalDate plus1 = now.plusDays(1);
        LocalDate plus2 = now.plusDays(2);

        System.out.println("now = " + now);
        System.out.println("plus1 = " + plus1);
        System.out.println("plus2 = " + plus2);

        LocalTime min = LocalTime.MIN;
        LocalTime max = LocalTime.MAX;

        System.out.println("min = " + min);
        System.out.println("max = " + max);

        LocalDateTime start= LocalDateTime.of(now,min);
        LocalDateTime end= LocalDateTime.of(plus2,max);
        System.out.println("start = " + start);
        System.out.println("end = " + end);
    }

}
