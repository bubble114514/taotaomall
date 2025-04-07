package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
 *
 * 3、JSR303
 *  1)、给Bean添加校验注解：javax.validation.constraints.*，并定义自己的message提示
 *  2)、在对应Controller类开启校验功能@Valid
 *      效果：校验错误以后会有默认的响应，返回给前端
 *  3）、给校验的bean后紧跟一个BindingResult，就可以获取到校验的错误信息
 *  4）、分组校验 (多场景的复杂校验）
 *      1)@NotBlank(message = "品牌名不能为空", groups = {AddGroup.class, UpdateGroup.class})
 *      给校验注释标注蛇魔情况需要进行校验
 *      2)@Validated({AddGroup.class})
 *      给方法标注，实现指定分组校验功能
 *      3)默认没有制定分组的校验注解@NotBlank，在分组校验情况下不生效，只会在@Validated(AddGroup.class)生效
 *  5）、自定义校验
 *    1)、编写一个自定义校验注解
 *    2)、编写一个自定义校验器@Component
 *    3)、关联自定义校验注解和自定义校验器
 *      @Documented
 *      @Constraint(validatedBy = {ListValueConstraintValidator.class})【可以指定多个不同的校验器】
 *      @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
 *      @Retention(RetentionPolicy.RUNTIME)
 * 4.统一异常处理Exception类
 *
 * 5.模版引擎
 *  1)、thymeleaf-start：关闭缓存
 *  2)、静态资源都放在static文件夹下就可以按照路径直接访问
 *  3)、页面放在templates下，按照路径拼接进行访问
 *      Springboot：访问项目时，默认会找index
 *  4)、页面修改不重启服务器实时更新
 *      添加热部署
 *      <dependency>
 *          <groupId>org.springframework.boot</groupId>
 *          <artifactId>spring-boot-devtools</artifactId>
 *          <optional>true</optional>
 *      </dependency>
 * 6、整合redis
 *  1)、引入redis的starter
 *  2)、配置redis的host
 *  3)、使用springboot自动配置好的StringRedisTemplate来操作redis
 *      redis->Map 类似
 * 7、整合redisson作为分布式锁等功能框架
 *  1)、引入redisson的starter依赖
 *  2)、配置redisson
 * 8、整合SpringCache缓存简化缓存开发
 *      1）、引入依赖 spring-boot-starter-cache   spring-boot-starter-data-redis
 *      2）、写配置
 *          1）、自动配好了缓存管理器
 *          2）、配置使用Redis作为缓存
 *      3）、测试使用缓存
 *          @Cacheable: 代表当前方法的结果需要缓存，将数据保存到缓存中
 *          @Cachevict：将数据从缓存中删除
 *          @CachePut：不影响方法执行更新缓存
 *          @Caching：组合以上多个操作
 *          @CacheConfig：在类级别共享缓存的相同配置
 *          1）、开启缓存功能 @EnableCaching
 *          2)、只需要使用注解就能完成缓存操作
 *      4）、原理
 *          CacheAutoConfiguration -> CacheManagerAutoConfiguration
 *          自动配置了RedisCacheManager->初始化所有的缓存->每个缓存决定使用什么配置
 *          ->如果redisCacheManager有就用已有的，没有就用默认配置
 *          ->想改缓存配置，只需要给容器中放一个RedisCacheConfiguration即可
 *          ->就会应用到当前RedisCacheManager管理的所有缓存分区中
 *      5）、SpringCache不足
 *          1）、读模式：
*               缓存穿透：查询一个null数据，解决：缓存空数据：cache-null-values: true
 *              缓存击穿：大量并发进来查询一个正好过期的数据。解决：加锁；默认不加锁
 *                      使用sync = true加锁来解决击穿问题
 *              缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间 time-to-live: 3600000
 *          2）、写模式：
 *              1）、读写加锁
 *              2）、引入Canal，感知到MySQL的更新，更新Redis
 *              3）、读多写多，直接去数据库查询
 *      总结：
 *          常规数据（读多写少，即时性，一致性要求不高的数据）：完全可以使用SpringCache；写模式（只要缓存的数据有过期时间就行）
 *          特殊数据：特殊设计
 *
 *
 *
 */

@EnableCaching
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
