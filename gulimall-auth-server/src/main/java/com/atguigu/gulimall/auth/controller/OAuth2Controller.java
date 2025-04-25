package com.atguigu.gulimall.auth.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.common.vo.SocialUserVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {
    @Autowired
    private MemberFeignService memberFeignService;
    @GetMapping(value = "/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session, HttpServletResponse servletResponse) throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id", "2795031263");
        map.put("client_secret", "3264769cc72e9c32e5446b975ac196fe");
       // map.put("client_id", "2129105835");
//        map.put("client_secret", "cafa2b4884a793f1c731c1e1afe28486");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        //1、根据code换取Access Token
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", headers, querys, map);
        System.out.println("code = " + code);
        // 2、处理
        if (response.getStatusLine().getStatusCode()==200){
            //获取accessToken
            String json = EntityUtils.toString(response.getEntity());
            SocialUserVo user = JSON.parseObject(json, SocialUserVo.class);

            System.out.println("socialUserVo = " + user);

            //登录或者注册，调用远程服务
            // 1）、如果用户第一次登录，自动注册(为当前社交用户生成一个会员信息账号，以后再登录就不用注册了)
            R r = memberFeignService.oauthLogin(user);
            if (r.getCode()==0){
                Object data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                System.out.println("登录成功,用户信息："+ data);
                log.info("登录成功,用户信息：{}：", data.toString());

                //1、第一次使用session，命令浏览器保存卡号。JSESSIONID这个cookie就是sessionId
                //以后浏览器访问哪个网站，就带上这个JSESSIONID
                //子域之间；gulimall.com  auth.gulimall.com  order.gulimall.com  ;.  .  .
                //发卡的时候（指定域名为父域名），即使是子域系统发的卡，也能让父域直接使用
                //TODO 1、默认发的令牌。session=唯一字符串。作用域：当前域；解决子域session共享问题
                //TODO 2、使用JSON的序列化方式来序列化对象到Redis中
                session.setAttribute("loginUser", data);

                //2、登录成功以后，将用户的信息放在session中
                return "redirect:http://gulimall.com";
            }else{
                //登录失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else{
            //失败
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
