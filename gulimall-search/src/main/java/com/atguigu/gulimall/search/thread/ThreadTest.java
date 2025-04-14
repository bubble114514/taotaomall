package com.atguigu.gulimall.search.thread;


import java.util.concurrent.*;

public class ThreadTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main线程。。。start");

        /**
         * 方法完成后的感知
         */
//        CompletableFuture.runAsync(() -> {
//            System.out.println("当前运行线程"+Thread.currentThread().getId());
//            int i = 10/2;
//            System.out.println("**********计算完成: "+i);
//        },executor);

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前运行线程" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("**********计算完成" + i);
//            return i;
//        }, executor).whenCompleteAsync((res,excption)->{
//            System.out.println("**********异步获取结果："+res);
//            System.out.println("**********异常信息："+excption);//只能得到异常信息，无法修改返回结果
//        }).exceptionally(throwable -> {
//            //可以感知异常，同时返回默认值
//            return 10;
//        });
        /**
         * 方法执行完成后的处理
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//                    System.out.println("当前运行线程" + Thread.currentThread().getId());
//                    int i = 10 / 2;
//                    System.out.println("**********计算完成" + i);
//                    return i;
//                }, executor).handle((res, excption) -> {
//                    if (res!=null){
//                        return res*2;
//                    }
//                    if (excption!=null){
//                        return 0;
//                    }
//                    return 0;
//        });

        /**
         * 线程串行化
         * 1、thenRun：不能获取上一步的执行结果，无返回值
         *        .thenRunAsync(() -> {
         *             System.out.println("任务2启动了");
         *         }, executor);
         * 2、thenAccept：能接受上一步的执行结果，无返回值
         * 3、thenApply：能接受上一步的执行结果，有返回值
         */
//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前运行线程" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("**********计算完成" + i);
//            return i;
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println("任务2启动了。。。。" + res);
//            return "hello " + res;
//        }, executor);

        /**
         * 双任务组合
         */
//        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1线程" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("**********任务1结束");
//            return i;
//        }, executor);
//
//        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2线程" + Thread.currentThread().getId());
//            System.out.println("**********任务2结束");
//            return "hello02";
//        }, executor);

        //runAfterBoth不能感知结果
//        CompletableFuture<Void> future03 = future01.runAfterBothAsync(future02, () -> {
//            System.out.println("任务3启动了。。。。");
//        }, executor);

        // thenAcceptBoth接受两个CompletableFuture的结果，并且不返回结果
//        future01.thenAcceptBothAsync(future02,(f1,f2) ->{
//            System.out.println("任务3启动了。。。。" + f1 +"----" + f2);
//        },executor);

        // thenCombine接受两个CompletableFuture的结果，并且返回结果
//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            return f1 + "--> " + f2 + "-->" + "hello";
//        }, executor);
//


        /**
         * 两个任务，只要有一个完成，继续执行后续任务
         * runAfterEitherAsync：不能获取上一步的执行结果，无返回值
         * acceptEitherAsync：能接受上一步的执行结果，无返回值
         * applyToEitherAsync：能接受上一步的执行结果，有返回值
         */
//        future01.runAfterEitherAsync(future02, () -> {
//            System.out.println("任务3启动了。。。。");
//        }, executor);
//        future01.acceptEitherAsync(future02, (res) -> {
//            System.out.println("任务3启动了。。。。"+res);
//        }, executor);
//        CompletableFuture<String> future = future01.applyToEitherAsync(future02, (res) -> {
//            System.out.println("任务3启动了。。。。" + res);
//            return "hello" + res;
//        }, executor);

        /**
         * 多任务组合
         */
        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {

            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        }, executor);
        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片属性");
            return "8+256";
        }, executor);
        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片介绍");
            return "华为";
        }, executor);
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        allOf.get();

        System.out.println("main线程。。。end..." );

    }

    public static void maintest(String[] args) {
        /**
         * 1)、继承线程类Thread
         *    1）、继承Thread类，重写run方法，执行线程任务
         *    2）、启动线程：start()
         * 2)、实现Runnable接口
         *     Thread t1 = new Thread(new Runnable();
         *     t1.start();
         * 3)、实现Callable接口 + FutureTask（可以拿到返回结果，可以处理异常）
         *      FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
         *      new Thread(futureTask).start();
         *      Integer integer = futureTask.get();
         *      System.out.println("main线程获取到返回结果："+integer);
         * 4)、线程池
         *      给线程池直接提交任务
         *
         * 区别：
         *      1、2不能得到返回值，只能拿到任务执行结果，通过异常处理任务是否执行成功
         *      3可以拿到返回值
         *      1、2、3不能控制资源
         *      4可以控制资源，性能稳定
         *
         * 在业务代码中，尽量使用线程池，提高性能，线程复用，提升效率，避免频繁创建线程，提升效率。
         *
         */
//        //Callable接口 + FutureTask
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//               new Thread(futureTask).start();
//        try {
//            Integer integer = futureTask.get();
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }

        //当前系统中池只有一两个，每个异步任务，提交给线程池让他自己去执行

//        service.submit(new Callable01());//submit方法返回一个future对象，可以拿到返回结果，可以处理异常
//        service.execute(new Runnable01());

        /**
         * 七大参数
         * 1、corePoolSize：核心线程数；默认情况下核心线程会一直存活，即使没有任务可执行，线程池也会一直拥有这些线程，直到程序关闭；
         * 2、maximumPoolSize：最大线程数；线程池允许创建的最大线程数，包括核心线程；
         * 3、KeepAliveTime：存活时间；当线程数大于核心线程数时，多余的空闲线程的存活时间；当空闲线程超过存活时间，就会被回收；
         * 4、TimeUnit：存活时间单位；
         * 5、workQueue：阻塞队列；当核心线程都被占用，且阻塞队列已满时，新的线程会被创建；
         * 6、threadFactory：线程工厂；创建线程时使用；
         * 7、handler：拒绝策略；当阻塞队列已满（线程池处理能力不能满足所有任务）时，会拒绝任务，并执行拒绝策略；
         *
         * 工作流程：
         * 1、根据给定的参数创建一个线程池，准备好core数量的核心线程，准备接受任务；
         *  1.1核心线程已满，新提交的任务会放在阻塞队列中；空闲的核心线程会自己去阻塞队列获取任务执行
         *  1.2如果阻塞队列已满，且没有达到线程池的最大线程数量，会创建非核心线程执行任务；
         *  1.3如果阻塞队列已满，且达到线程池的最大线程数量，会执行拒绝策略
         *  1.4如果线程池中的线程数大于core数量，空闲时间超过keepAliveTime，多余线程会被回收
         *
         *  一个线程池 core 7，max 20，queue：50, 100并发过来怎么处理？
         *  7个会立即执行，剩余50个放在阻塞队列，再开13个执行，剩下的继续放在阻塞队列，如果阻塞队列已满，会执行拒绝策略
         */
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
//        Executors.newCachedThreadPool();//缓存线程池,core是0，所有线程都可回收
//        Executors.newFixedThreadPool();//固定数量线程池
//        Executors.newScheduledThreadPool(5);//定时线程池
//        Executors.newSingleThreadExecutor();//单线程池
//        Executors.newWorkStealingPool();//并行流线程池


    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前运行线程" + Thread.currentThread().getName());
            int i = 10 / 2;
            System.out.println("**********计算完成" + i);

        }
    }

    public static class Runnable01 implements Runnable {
        @Override
        public void run() {
            System.out.println("当前运行线程" + Thread.currentThread().getName());
            int i = 10 / 2;
            System.out.println("**********计算完成" + i);

        }
    }

    public static class Callable01 implements java.util.concurrent.Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("**********come in Callable");
            int i = 10 / 2;

            System.out.println("**********计算完成" + i);
            return i;
        }
    }
}
