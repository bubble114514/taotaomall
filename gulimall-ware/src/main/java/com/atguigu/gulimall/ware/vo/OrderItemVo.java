package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 */
@Data
public class OrderItemVo {
    private Long skuId;                     // skuId
    private String title;                   // 标题
    private String image;                   // 图片
    private List<String> skuAttr;           // 销售属性
    private BigDecimal price;               // 单价
    private Integer count;                  // 商品件数
    private BigDecimal totalPrice;          // 总价
    private BigDecimal weight;
}
