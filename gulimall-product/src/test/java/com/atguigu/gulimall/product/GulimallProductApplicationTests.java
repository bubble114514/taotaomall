package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemBaseAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 1、引入oss-starter
 * 2、在application.yml中配置key、secret、endpoint
 * 3、使用OSSClient进行相关操作
 */

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    AttrGroupDao attrGroupDao;
    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void testFindSkuSaleAttrValuesBySpuId() {
        List<SkuItemSaleAttrVo> values = skuSaleAttrValueDao.getSaleAttrsBySpuId(8L);
        System.out.println(values);
    }

    @Test
    public void testFindAttrGroupWithAttrsBySpuIdAndCatelogId() {

        List<SpuItemBaseAttrGroupVo> vos = attrGroupDao.getAttrGroupWithAttrsBySpuId(8L, 225L);
        System.out.println(vos);
    }


    @Test
    public void redisson(){
        System.out.println(redissonClient);
    }

    @Test
    public void teststringRedisTemplate() {
        ValueOperations<String, String> ops =stringRedisTemplate.opsForValue();
        ops.set("hello", "world"+ UUID.randomUUID().toString());
        String hello = ops.get("hello");
        System.out.println("之前保存的数据是："+hello);
    }


    @Test
    public void testFindPath() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径：{}", Arrays.asList(catelogPath));
        System.out.println(Arrays.asList(catelogPath));
    }


    @Test
    public void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setBrandId(1L);
//        brandEntity.setDescript("手机");

//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("保存成功");

//        brandService.updateById(brandEntity);

        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach(item -> {
            System.out.println(item);
        });

    }






}
