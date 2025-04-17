package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.UserLoginVo;
import com.atguigu.common.vo.UserRegistVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class loginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送一个请求直接跳转到一个页面
     * SpringMVC：将请求和页面映射过来
     */

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 1.接口防刷


        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.isNotEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {// 60秒内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2、验证码的再次校验。redis。存key-phone，value-code   sms:code:18500000000 -> 12345
//        String code = generateRandomNumericCode(5) + "_" + System.currentTimeMillis();
//        System.out.println("************** code = " + code + " ********************");

        String smsCode = generateRandomNumericCode(5);
        System.out.println("************** smsCode = " + smsCode + " ********************");


        // redis缓存验证码，防止同一个手机号，在60秒内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, smsCode, 10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone, smsCode);
        return R.ok();
    }

    /**
     * 重定向携带数据，利用session原理。将数据放在session中，只要跳到下一个页面取出这个数据以后，session里面的数据就会删掉
     * RedirectAttributes redirectAttributes:模拟重定向携带数据
     *
     * @param vo
     * @param result
     * @param model
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/register")
    public String regist(@Valid UserRegistVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes) {

        //1、校验
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField, // 键：字段名
                            FieldError::getDefaultMessage, // 值：错误信息
                            (existingValue, newValue) -> existingValue + "; " + newValue // 合并函数：将重复键的值拼接起来
                    ));
//            model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            //校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //2、调用远程服务注册
        //2、1、校验验证码
        String code = vo.getCode();
        String smsCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(smsCode)) {
            if (code.equals(smsCode)) {
                // 删除验证码，令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过，调用远程服务注册
                R r = memberFeignService.register(vo);
                if (r.getCode() == 0) {
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", (String) r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }

        return null;
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes) {
        // 远程登录
        R r = memberFeignService.login(vo);
        if (r.getCode()==0){
            return "redirect:http://gulimall.com";
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", (String) r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

    private String generateRandomNumericCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 生成0-9之间的随机数字
        }
        return sb.toString();
    }

}
