package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {
    private Long addrId;//收货地址id
    private Integer payType;//支付方式

    //无需提交需要购买的商品，去购物车再获取一遍

    //TODO 优惠，发票...

    private String uniqueToken;//订单令牌
    private BigDecimal payPrice;//应付金额  验价
    private String note;//订单备注

    //用户相关信息直接从session中取出登录的用户
}
