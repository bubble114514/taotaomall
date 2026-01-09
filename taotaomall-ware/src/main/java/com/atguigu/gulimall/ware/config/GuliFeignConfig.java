package com.atguigu.gulimall.ware.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@Configuration
public class GuliFeignConfig {
    @Bean
    public HttpMessageConverter<String> textHtmlConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

}
