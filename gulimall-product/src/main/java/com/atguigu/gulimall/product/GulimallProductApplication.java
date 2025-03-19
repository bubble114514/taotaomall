package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1、整合Mybatis-Plus
 * 1)导入依赖
 * 2)配置
 *   1.配置数据源
 *      1）导入数据库的驱动
 *      2）在application.yml配置数据源相关信息
 *   2.配置MyBatis-Plus
 *    1）使用@MapperScan
 *    2） 告诉MyBatis-Plus，sql映射文件位置
 * 2.逻辑删除
 *  1)、配置全局巨大逻辑删除规则
 *  2)、配置逻辑删除的组件（MybatisPlus 3.1.0之后的版本可以不配置）
 *  3）、在Bean上添加@TableLogic逻辑删除注解
 */


@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
