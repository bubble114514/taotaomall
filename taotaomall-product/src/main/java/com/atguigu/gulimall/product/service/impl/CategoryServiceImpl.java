package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    private Map<String, Object> cache = new HashMap<>();

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redisson;

    // 布隆过滤器
    private RBloomFilter<String> categoryBloomFilter;
    @PostConstruct
    public void initBloomFilter() {
        // 初始化布隆过滤器
        categoryBloomFilter = redisson.getBloomFilter("categoryBloomFilter");
        // 预期元素数量10万，误判率1%
        categoryBloomFilter.tryInit(100000L, 0.01);

        // 加载所有分类ID
        List<CategoryEntity> allCategories = baseMapper.selectList(null);
        allCategories.forEach(c ->
                categoryBloomFilter.add(c.getCatId().toString())
        );
    }

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

        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);

        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联度数据
     * @CacheEvict：失效模式
     * 1、@Caching：同时进行多种缓存操作
     * 2、指定删除某个分区下的所有数据：@CacheEvict(value = {"category"}, allEntries = true)
     * 3、存储同一类型的数据，都可以指定成同一个分区。分区名默认就是缓存前缀
     * @param category
     */
//    @Caching(
//            evict = {
//                    @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"),
//                    @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//            }
//    )
    @CacheEvict(value = {"category"}, allEntries = true)//失效模式
//    @CachePut(value = {"category"}, key = "#root.methodName")//双写模式
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        //同时修改缓存中的数据
    }

    //每一个需要缓存的数据都要指定放到哪个名字的缓存中，【缓存的分区（按照业务类型分）】默认就是方法的返回值
    @Cacheable(value = {"category"}, key = "'getLevel1Categorys'", sync = true) // 代表当前方法的结果需要缓存，如果缓存中有，方法不用调用。如果缓存中没有，会调用方法，最后将结果存到缓存
    @Override
    public List<CategoryEntity> getlevel1Categories() {
        System.out.println("getlevel1Categories");
        return this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Cacheable(value = {"category"}, key = "'getCatalogJson'")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("没有缓存，查询数据库");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1、查出所有1级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);
        //2、封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(),
                v -> {
                    //1、每一个1级分类，查到这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    //2、封装到2级分类
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //1、找到当前二级分类的三级分类，封装成vo
                            List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());
                            if (level3Catelog != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    //2、封装成指定格式
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());

                    }
                    return catelog2Vos;
                }));
        return parent_cid;
    }

    //TODO 产生堆外内存溢出：OutOfDirectMemoryError
    //1）、springboot2.0以后默认使用lettuce作为操作redis的客户端，它使用netty进行网络通信
    //2）、lettuce的bug导致netty堆外内存溢出：-XX:+HeapDumpOnOutOfMemoryError
    //      netty如果没有指定堆外内存，默认使用-Xmx指定内存的80%作为堆外内存
    //      可以通过-Dio.netty.maxDirectMemory来指定堆外内存大小
    //解决方案：不能使用-Dio.netty.maxDirectMemory只去调大堆外内存
    //  1）、升级lettuce客户端 2）、切换使用jedis
    //
    //@Override
    public Map<String, List<Catelog2Vo>> getCatalogJsonByRedis() {
        //给缓冲中放JSON字符串，拿出的JSON字符串还要逆转为能用的对象类型【序列化与反序列化】

        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         * 3、加锁：解决缓存击穿
         */

        //1、加入缓存逻辑，缓存中存的数据为json字符串
        //JSON跨语言跨平台兼容
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //2、缓存中没有，查数据库
            System.out.println("缓存不命中，查询数据库");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();

            return catalogJsonFromDb;
        }
        System.out.println("缓存命中，直接返回");
        //逆转为指定对象返回
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });

    }

    /**
     * 缓存里面的数据如何和数据库保持一致
     * 缓存数据一致性：
     * 1）、双写模式：先写数据库在写缓存
     * 2）、失效模式：先写数据库再删缓存
     * @return
     */
    //分布式锁查数据库
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        // 1. 先尝试无锁读缓存
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.isEmpty(catelogJson)) {
            return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        }

        // 2. 布隆过滤器检查（防缓存穿透）
        if (!categoryBloomFilter.contains("catelogJson")) {
            log.warn("布隆过滤器拦截非法请求");
            redisTemplate.opsForValue().set("catelogJson", "{}", 5, TimeUnit.MINUTES);
            return Collections.emptyMap();
        }

        // 3. 获取读写锁（读锁）
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("catalogJson-rw-lock");
        RLock readLock = readWriteLock.readLock();

        try {
            // 3.1 加读锁（非阻塞式尝试）
            if (readLock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // 4. 双重检查缓存
                catelogJson = redisTemplate.opsForValue().get("catelogJson");
                if (!StringUtils.isEmpty(catelogJson)) {
                    return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
                }

                // 5. 查询数据库
                Map<String, List<Catelog2Vo>> dataFromDb = getDataFromDb();

                // 6. 获取写锁升级（注意：必须先释放读锁）
                readLock.unlock();
                RLock writeLock = readWriteLock.writeLock();
                try {
                    if (writeLock.tryLock(10, 30, TimeUnit.SECONDS)) {
                        // 7. 再次双重检查（防止其他线程已更新）
                        catelogJson = redisTemplate.opsForValue().get("catelogJson");
                        if (!StringUtils.isEmpty(catelogJson)) {
                            return JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
                        }

                        // 8. 写入缓存（设置随机过期时间防雪崩）
                        String jsonString = JSON.toJSONString(dataFromDb);
                        int randomOffset = new Random().nextInt(4 * 3600);
                        redisTemplate.opsForValue().set(
                                "catelogJson",
                                jsonString,
                                24 * 3600 + randomOffset,
                                TimeUnit.SECONDS
                        );
                        return dataFromDb;
                    }
                } finally {
                    if (writeLock.isHeldByCurrentThread()) {
                        writeLock.unlock();
                    }
                }
            }
            throw new RuntimeException("获取锁失败，请重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断", e);
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            //缓存不为空直接返回
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }
        System.out.println("没有缓存，查询数据库");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1、查出所有1级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        //2、封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(),
                v -> {
                    //1、每一个1级分类，查到这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    //2、封装到2级分类
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //1、找到当前二级分类的三级分类，封装成vo
                            List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());
                            if (level3Catelog != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    //2、封装成指定格式
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());

                    }
                    return catelog2Vos;
                }));
        //查到的数据再放入缓存，将查出的对象转为json放在缓存
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);//设置过期时间

        return parent_cid;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //1、占分布式锁，去redis占坑，去redis设置过期时间，防止死锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            System.out.println("获取分布式锁成功。。。");
            //加锁成功，执行业务
            //2、设置过期时间，必须和加锁是同步的，原子的
//            redisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            //执行业务
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock", uuid));
            }

            //获取值对比+对比成功删除=原子操作  Lua脚本解锁
//            String lockValue=redisTemplate.opsForValue().get("lock");
//            if (Objects.equals(lockValue,uuid)){
//                redisTemplate.delete("lock");//释放锁
//            }

            return dataFromDb;
        } else {
            System.out.println("获取分布式锁失败。。。等待重试");
            //加锁失败，重试。synchronized()
            //休眠100ms重试
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();//自旋
        }

    }


    // 从数据库查询并封装分类数据（本地锁）
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

//        //1、如果缓冲中有就用缓冲的
//        Map<String, List<Catelog2Vo>> catalogJson = (Map<String, List<Catelog2Vo>>) cache.get("catalogJson");
//        if ( cache.get("catalogJson") == null){
//        //调用业务
//        //返回数据又放入缓存
//          cache.put("catalogJson",parent_cid);
//          }
//        return catalogJson;

//        只要是同一把锁，就能锁住需要这个锁的所有线程
//        1、synchronized (this)：springboot所有的组件在spring中都是单例的
//        本地锁：synchronized，JUC（Lock)，在分布式情况下，想要锁住所有，必须使用分布式锁

        synchronized (this) {
            //得到锁后，应该先去缓存中确认一次，如果没有才需要继续查询
            return getDataFromDb();

        }
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parent_cid) {
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return selectList.stream().filter(item -> Objects.equals(item.getParentCid(), parent_cid)).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

}