package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        entities = entities.stream().filter(Objects::nonNull).collect(Collectors.toList());

        //2.组装成父子的树形结构

        //2.1)、找到所有的一级分类
        List<CategoryEntity> finalEntities = entities;
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(menu -> menu != null && menu.getParentCid() != null && menu.getParentCid() == 0)
                .peek(menu -> {
                    if (menu != null) {
                        List<CategoryEntity> children = getChildrens(menu, finalEntities);
                        if (children != null) {
                            menu.setChildren(children);
                        }
                    }
                })
                .sorted((menu1, menu2) -> {
                    if (menu1 == null || menu2 == null) {
                        return 0;
                    }
                    Integer sort1 = menu1.getSort() == null ? 0 : menu1.getSort();
                    Integer sort2 = menu2.getSort() == null ? 0 : menu2.getSort();
                    return sort1.compareTo(sort2);
                })
                .collect(Collectors.toList());


        return level1Menus;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        if (root == null || all == null) {
            return Collections.emptyList();
        }
        List<CategoryEntity> children = all.stream()
                .filter(categoryEntity -> categoryEntity != null && Objects.equals(categoryEntity.getParentCid(), root.getCatId()))
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    Integer sort1 = menu1.getSort() == null ? 0 : menu1.getSort();
                    Integer sort2 = menu2.getSort() == null ? 0 : menu2.getSort();
                    return sort1.compareTo(sort2);
                })
                .collect(Collectors.toList());
        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> aslist) {

        //TODO 1、检查当前删除的菜单，是否被其他地方引用

        //逻辑删除

        baseMapper.deleteBatchIds(aslist);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath= findParentPath(catelogId,paths);

        Collections.reverse(parentPath);

        return  (Long[])parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联度数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId=this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

}