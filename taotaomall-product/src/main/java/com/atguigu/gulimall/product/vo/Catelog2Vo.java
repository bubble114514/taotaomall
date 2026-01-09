package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 二级分类vo
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catelog2Vo {
    private String catalog1Id;//一级父分类Id
    private List<Catelog3Vo> catalog3List; //三级子分类
    private String id;
    private String name;

    //三级子分类vo
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catelog3Vo {
        private String catalog2Id;//二级父分类Id
        private String id;
        private String name;
    }
}
