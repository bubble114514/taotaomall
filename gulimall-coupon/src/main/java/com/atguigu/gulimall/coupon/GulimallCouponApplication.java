package com.atguigu.gulimall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


/***
 * 一、如何使用NACOS作为配置中心统一管理配置
 * 1.引入依赖
 * <dependency>
 *             <groupId>org.springframework.cloud</groupId>
 *             <artifactId>spring-cloud-starter-loadbalancer</artifactId>
 *             <version>3.0.3</version> <!-- 确保与 Spring Cloud 版本兼容 -->
 *         </dependency>
 *  2.创建一个bootstrap.properties，并添加以下内容：
 *      spring.application.name=gulimall-coupon
 *      spring.cloud.nacos.config.server-addr=127.0.0.1:8848
 *  3.在配置中心没人添加一个数据集（Data Id） gulimall-coupon.properties，并添加配置内容。
 *  4.动态获取配置
 *   @RefreshScope：动态获取并刷新配置
 *   @Value("${配置项的名}")：获取到新配置
 *  如果配置中心和当前配置文件都配置了相同的项，优先使用配置中心的配置。
 *
 *  二、细节
 *  1.命名空间
 *    默认是public(保留空间）：默认新增的所有配置都在public 空间下。
 *    1）开发、测试、生产：利用命名空间来做环境隔离。
 *       注意：在bootstrap.porperties配置上，需要添加命名空间的配置
 *       spring.cloud.nacos.config.namespace=5a294f4e-bc06-40df-a9ed-c5bf56c6e241
 *    2）每一个微服务之间互相隔离配置，每一个微服务都创建自己的命名空间，只加载自己的命名空间下的所有配置
 *
 *  2.配置集：所有配置的集合
 *
 *  3.配置集的id：类似文件名
 *      Data ID：spring.application.name的值，默认是配置文件名
 *
 *  4.配置分组
 *      默认所有的配置集都属于DEFAULT_GROUP；
 *      1）开发、测试、生产：利用分组来做环境隔离。
 *每个微服务创建自己的命名空间，使用配置分组区分环境，dev,test,prod
 *
 */

@EnableDiscoveryClient
@SpringBootApplication
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
