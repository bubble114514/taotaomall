package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
 */

@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
