package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        //查出所有1级分类
        List<CategoryEntity> categoryEntities = categoryService.getlevel1Categories();

        //视图解析器进行拼串
        // classpath:/templates/index.html
        model.addAttribute("categorys", categoryEntities);

        return "index";
    }

    // /index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //1、获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redisson.getLock("my-lock");

        //2、加锁
//        lock.lock();//阻塞式等待。默认加的锁都是30s时间
        //1）、锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s。不用担心业务时间长，锁自动过期被删掉
        //2）、加锁的业务只要运行完成，就不会续期，即使不手动解锁，锁默认在30s以后自动删除

        lock.lock(10, TimeUnit.SECONDS);//10秒钟以后自动解锁，自动解锁时间一定要大于业务执行时间
        //问题：lock.lock(10, TimeUnit.SECONDS);在锁到期后不会自动续期
        //1、如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是指定的时间
        //2、如果未指定所得超时时间，就使用30s【LockWatchdogTimeout看门狗的默认时间】
        //   只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间、新的过期时间就是看门狗的默认时间】
        //   续期internalLockLeaseTime【看门狗时间】/3  10s

        //最佳实战：
        // 1）、使用lock.lock(30, TimeUnit.SECONDS);省掉了整个续期操作，手动解锁
        try {
            System.out.println("加锁成功，执行业务。。。" + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            //3、解锁
            System.out.println("释放锁。。。" + Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }

    //保证一定能读到最新数据，修改期间，写锁是一个排它锁（互斥锁）。读锁是一个共享锁
    //写锁没释放，读锁必须等待
    //写 + 读：等待写锁释放
    //写 + 写：阻塞方式
    //读 + 写：有读锁。写也需要等待
    //只要有写存在，都必须等待

    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = readWriteLock.writeLock();
        try {
            //1、改数据加写锁，读数据加读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            rLock.unlock();
        }

        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        //加读锁
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        String s = "";
        try {

             s = redisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();;
        }finally {
            rLock.unlock();
        }

        return s;
    }

    /**
     * 车库停车
     * 3车位
     * 信号量也可以用作分布式限流
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore semaphore = redisson.getSemaphore("park");
//        semaphore.acquire();//获取一个信号，获取一个值，占一个车位，信号量的值-1
        boolean b = semaphore.tryAcquire();
        if (b){
            //执行业务
        }else {
            return "error";
        }
        return "ok=>"+b;
    }
    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore semaphore = redisson.getSemaphore("park");
        semaphore.release();//释放一个车位
        return "释放车位";
    }

    /**
     * 放假，锁门
     * 5个班全部走完，才锁门
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();//等待闭锁完成
        return "放假了。。。";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();//减一
        return id+"班的人走完";
    }
}
