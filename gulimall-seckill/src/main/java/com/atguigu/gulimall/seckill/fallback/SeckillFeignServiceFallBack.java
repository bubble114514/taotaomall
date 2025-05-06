//package com.atguigu.gulimall.seckill.fallback;
//
//
//import com.atguigu.common.exception.BizCodeEnume;
//import com.atguigu.common.utils.R;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class SeckillFeignServiceFallBack implements SeckillFeignService {
//    @Override
//    public R getSkuSeckillInfo(Long skuId) {
//        log.error("熔断方法调用...getSkuSeckillInfo");
//        return R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(),BizCodeEnume.TO_MANY_REQUEST.getMsg());
//    }
//}
