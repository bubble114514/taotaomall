//package com.atguigu.gulimall.member.config;
//
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeRequests()
//                .antMatchers("/member/member/register").permitAll() // 允许匿名访问
//                .antMatchers("/member").permitAll()
//                .anyRequest().authenticated() // 其他接口需要认证
//                .and()
//                .csrf().disable(); // 根据需求决定是否关闭 CSRF
//    }
//}