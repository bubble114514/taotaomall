package com.atguigu.gulimall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HelloController {
    @Value("${sso.server.url}")
    private String ssoServerUrl;
    /**
     * 测试
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
    @GetMapping("/boss")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token",required = false) String token) {
        //去ssoserver登录成功后跳回来会带上token
        if (token!=null){
            //TODO 1、去ssoserver获取当前token真正对应的用户信息
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> forEntity = restTemplate.getForEntity(ssoServerUrl + "/userInfo?token=" + token, String.class);
            String body = forEntity.getBody();
            session.setAttribute("loginUser",body);
        }
        if (session.getAttribute("loginUser") == null){
            // 未登录，跳转到登录服务器登录

            // 跳转过去后，使用 redirect_url 参数告诉登录服务器，登录成功后跳回这个地址
            return "redirect:" + ssoServerUrl+"?redirect_url=http://client2.com:8082/boss";
        }else{
            List<String> emps = new ArrayList<>();
            emps.add("张三");
            emps.add("李四");
            model.addAttribute("emps", emps);
            return "list";
        }
    }
}
