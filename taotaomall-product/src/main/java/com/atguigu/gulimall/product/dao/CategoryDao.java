package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author paopao
 * @email 1903980165@qq.com
 * @date 2025-03-17 08:25:57
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
