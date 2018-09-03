package com.sohu.cache.async.impl;


import com.sohu.cache.async.AsyncService;
import com.sohu.cache.async.KeyCallable;
import com.sohu.cache.async.KeyFuture;
import com.sohu.cache.async.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Created by yijunzhang on 14-6-18.
 */
public class AsyncServiceImpl implements AsyncService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final int WARNING_TIMEOUT = 2000;

    private final int INTERRUPT_TIMEOUT = 10000;

    private final String DEFAULT_THREAD_POOL = "default_thread_pool";

    private final String COLLECT_THREAD_POOL = "collect_thread_pool";

    private final ExecutorService defaultThreadPool;

    private final ExecutorService collectThreadPool;

    private final ThreadPoolExecutor collectQueueInfoPool;

    private final ThreadPoolExecutor observeFuturePool;

    public ConcurrentMap<String, ExecutorService> threadPoolMap;

    private final BlockingQueue<KeyFuture<?>> futureQueue;

    private final BlockingQueue<Runnable> collectQueue;

    public AsyncServiceImpl() {
        futureQueue = new LinkedBlockingQueue<KeyFuture<?>>();
        collectQueue = new LinkedBlockingQueue<Runnable>(100000);

        defaultThreadPool = new ThreadPoolExecutor(1, 256,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new NamedThreadFactory("async", true));

        collectThreadPool = new ThreadPoolExecutor(1000, 1000,
                0L, TimeUnit.MILLISECONDS,
                collectQueue,
                new NamedThreadFactory("collect", true));

        observeFuturePool = new ThreadPoolExecutor(3, 3,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new NamedThreadFactory("future-observe", true));

        collectQueueInfoPool = new ThreadPoolExecutor(3, 5,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new NamedThreadFactory("queueInfo", true));
        threadPoolMap = new ConcurrentSkipListMap<String, ExecutorService>();

        threadPoolMap.put(DEFAULT_THREAD_POOL, defaultThreadPool);
        threadPoolMap.put(COLLECT_THREAD_POOL, collectThreadPool);
        startObserveFuture();
    }

    private final Runnable observeTask = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    KeyFuture<?> keyFuture = futureQueue.take();
                    String key = keyFuture.getKey();
                    Future<?> future = keyFuture.getFuture();
                    try {
                        long begin = System.currentTimeMillis();
                        future.get(INTERRUPT_TIMEOUT, TimeUnit.MILLISECONDS);
                        long costTime = System.currentTimeMillis() - begin;
                        if (costTime >= WARNING_TIMEOUT) {
                            logger.warn("WARNING:future={} costTime={}", key, costTime);
                        } else {
                            logger.info("future={} costTime={}", key, costTime);
                        }
                    } catch (TimeoutException te) {
                        logger.error("ERROR:Timeout:future={},costTime={}", key, INTERRUPT_TIMEOUT);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    };


    private void startObserveFuture() {
        observeFuturePool.execute(observeTask);
        observeFuturePool.execute(observeTask);
    }

    private void startCollectQueneInfo() {
        collectQueueInfoPool.execute(observeTask);
    }

    @Override
    public boolean submitFuture(KeyCallable<?> callable) {
        return submitFuture(DEFAULT_THREAD_POOL, callable);
    }

    @Override
    public boolean submitCollect(KeyCallable<?> callable){
        return submitFuture(COLLECT_THREAD_POOL, callable);
    }

    @Override
    public boolean submitFuture(String threadPoolKey, KeyCallable<?> callable) {
        try {
            Future<?> future = getExecutorService(threadPoolKey).submit(callable);
            //忽略queue溢出
            logger.info("向futureQueue中插入future,key = {},future = {}",callable.getKey(), future);
            futureQueue.put(new KeyFuture(callable.getKey(), future));
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage() + callable.getKey(), e);
            return false;
        }
    }

    @Override
    public Future<?> submitFuture(Callable<?> callable) {
        try {
            Future<?> future = defaultThreadPool.submit(callable);
            //忽略queue溢出
            futureQueue.put(new KeyFuture(callable.getClass().getName(), future));
            return future;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private ExecutorService getExecutorService(String key) {
        return threadPoolMap.get(key);
    }

    @Override
    public void assemblePool(String threadPoolKey, ThreadPoolExecutor threadPool) {
        ExecutorService executorService = threadPoolMap.putIfAbsent(threadPoolKey, threadPool);
        if (executorService != null) {
            logger.error("{} is assembled", threadPoolKey);
        }
    }

    public void destory() {
        for (ExecutorService executorService : threadPoolMap.values()) {
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
        threadPoolMap.clear();
    }

    @Override
    public int getCollectQueueCurrSize(){
        return collectQueue.size();
    }


    @Override
    public void submitFuture(Runnable runnable) {
        try {
            collectQueueInfoPool.submit(runnable);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
