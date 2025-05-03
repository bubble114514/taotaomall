package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.util.List;

@Data

public class WareSkuLockVo {
    private String orderSn;//订单号

    private List<OrderItemVo> locks;//订单中所包含的商品


}
