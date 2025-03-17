package com.atguigu.gulimall.lomboktest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LombokTestApplicationTests {

    @Test
    void contextLoads() {
        User user = new User();
        user.setId(1L);
        user.setName("paopao");
        System.out.println(user);
    }

}
