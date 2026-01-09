package com.atguigu.gulimallcart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性必须重写他的get方法，保证每次获取属性都会进行计算
 */
public class Cart {
    List<CartItem> items;

    private Integer countNum;//商品数量

    private Integer countType;//商品类型数量

    private BigDecimal totalAmount;//总价

    private BigDecimal reduce = new BigDecimal(0);//减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int countNum = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                countNum += item.getCount();
            }
        }
        return countNum;
    }

    public void setCountNum(Integer countNum) {
        this.countNum = countNum;
    }

    public Integer getCountType() {
        int countType = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                countType += 1;
            }
        }
        return countType;
    }

    public void setCountType(Integer countType) {
        this.countType = countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal totalAmount = new BigDecimal(0);
        //1、遍历购物车所有商品，计算购物项总价
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                if (item.getCheck()) {
                    //总价
                    item.getTotalPrice().multiply(new BigDecimal(item.getCount()));
                    totalAmount = totalAmount.add(item.getTotalPrice());
                }

            }
        }

        //2、减去优惠总价
        return totalAmount.subtract(getReduce());

    }


    public BigDecimal getReduce() {

        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
