//package com.atguigu.gulimall.thirdparty.component;
//
//import cn.hutool.http.HttpResponse;
//import com.atguigu.gulimall.thirdparty.util.HttpUtils;
//import lombok.Data;
//import org.apache.http.util.EntityUtils;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Data
//@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
//@Component
//public class SmsComponent {
//
//    private String host;
//
//    private String path;
//
//    private String skin;
//
//    private String sign;
//
//    private String appCode;
//
//    public String sendSmsCode(String phone, String code){
//        String method = "GET";
//        Map<String, String> headers = new HashMap<String, String>();
//        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
//        headers.put("Authorization", "APPCODE " + this.appCode);
//        Map<String, String> querys = new HashMap<String, String>();
//        querys.put("code", code);
//        querys.put("phone", phone);
//        querys.put("skin", this.skin);
//        querys.put("sign", this.sign);
//        HttpResponse response = null;
//        try {
//            response = (HttpResponse) HttpUtils.doGet(this.host, this.path, method, headers, querys);
//            //获取response的body
//            if(((org.apache.http.HttpResponse) response).getStatusLine().getStatusCode() == 200){
//                return EntityUtils.toString(((org.apache.http.HttpResponse) response).getEntity());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "fail_" + ((org.apache.http.HttpResponse) response).getStatusLine().getStatusCode();
//    }
//}
//
