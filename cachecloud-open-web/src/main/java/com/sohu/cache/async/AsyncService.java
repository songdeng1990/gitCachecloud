package com.sohu.cache.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步服务类
 * Created by yijunzhang on 14-6-18.
 */
public interface AsyncService {

    /**
     * 提交任务
     *
     * @param callable
     * @return 返回是否提交成功
     */
    public boolean submitFuture(KeyCallable<?> callable);

    /**
     * 提交任务
     *
     * @param threadPoolKey
     * @param callable
     * @return 返回是否提交成功
     */
    public boolean submitFuture(String threadPoolKey, KeyCallable<?> callable);

    /**
     * 提交任务
     *
     * @param callable
     * @return 返回成功结果
     */
    public Future<?> submitFuture(Callable<?> callable);

    /**
     * 装配key对应的线程池
     *
     * @param threadPoolKey
     * @param threadPool
     */
    public void assemblePool(String threadPoolKey, ThreadPoolExecutor threadPool);

    /**
     * 提交数据采集任务
     * @param callable
     * @return 返回是否提交成功
     */
    public boolean submitCollect(KeyCallable<?> callable);


    /**
     * 获取当前采集任务的堆积数
     * @return
     */
    public int getCollectQueueCurrSize();


    /**
     * 提交获取堆积数任务
     * @param runnable
     */
    public void submitFuture(Runnable runnable);

}
