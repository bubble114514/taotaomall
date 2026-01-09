package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@Configuration
public class GuliFeignConfig {
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                /**
                 * 1、使用RequestContextHolder工具获取请求数据（调用toTrade方法的那个请求数据（request））
                 *      @GetMapping("/toTrade")
                 *     public String toTrade(Model model, HttpServletRequest request)
                 */
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if(requestAttributes!=null){
                    HttpServletRequest request = requestAttributes.getRequest();//老请求
                    //2、同步请求头数据（cookie）
                    String cookie = request.getHeader("Cookie");
                    //3、给新请求同步cookie
                    template.header("Cookie",cookie);
                }

            }
        };
    }
    @Bean
    public HttpMessageConverter<String> textHtmlConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

}
