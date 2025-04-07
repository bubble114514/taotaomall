package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 *
 * catalog3Id=225&keyword=华为&sort=1_desc&hasStock=0/1&skuPrice=2_500&brandId=1,2&attr=2_5寸:6寸&attr=1_6寸:8寸
 */
@Data
public class SearchParam {
    private String keyword;//页面传递过来的全文匹配关键字
    private String catalog3Id;//三级分类id

    /**
     *  sort = saleCount_asc/desc
     *  sort = hasStock_asc/desc
     *  sort = skuPrice_asc/desc
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock（是否有货）、skuPrice（价格区间）、brandId（品牌）、catalog3Id（三级分类）、attrs（规格参数）
     * hasStock=0/1
     * skuPrice=2_500
     * brandId=1,2
     * attrs=2_5寸:6寸
     */
    private Integer hasStock;//是否只显示有货
    private String skuPrice;//价格区间查询
    private String brandId;//品牌选择
    private List<String> attrs;//按照属性筛选
    private Integer pageNum;//页码
}
