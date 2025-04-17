package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.util.SMSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sms")
public class SmSendController {

    @Autowired
    SMSUtil smsUtil;

    /**
     * 提供给被动服务进行调用
     * @param phone
     * @return
     */
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code){
        smsUtil.sendMessage("SMS_154950909",phone,code);
        return R.ok();
    }
}
