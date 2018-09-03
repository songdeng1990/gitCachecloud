package com.sohu.cache.dao;

import com.sohu.cache.entity.*;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Created by yijunzhang on 14-6-9.
 */
public interface AppStatsDao {

    public static final int MINUTE_DIMENSIONALITY = 0;

    public static final int HOUR_DIMENSIONALITY = 1;

    /**
     * 插入AppStats分钟统计，如果存在则忽略
     */
    public int mergeMinuteAppStats(AppStats appStats);
    
    /**
     * 批量插入
     * @param appStats
     * @return
     */
    public int batchMergeMinuteAppStats(List<AppStats> appStats);
    
    /**
     * 更新更新AppStats分钟统计
     * @param appStats
     */
    public void updateMinuteAppStats(AppStats appStats);

    /**
     * 插入AppCommandStats分钟统计
     */
    public int mergeMinuteCommandStatus(AppCommandStats commandStats);
    
    public int batchMergeMinuteCommandStatus(List<AppCommandStats> AppCommandStats);
    
    /**
     * 更新AppCommandStats分钟统计
     */
    public void updateMinuteCommandStatus(AppCommandStats commandStats);

    /**
     * 插入或更新AppStats小时统计
     */
    public void mergeHourAppStats(AppStats appStats);
    
    public void batchMergeHourAppStats(List<AppStats> appStats);

    /**
     * 插入或更新AppCommandStats小时统计
     */
    public void mergeHourCommandStatus(AppCommandStats commandStats);
    
    public void batchMergeHourCommandStatus(List<AppCommandStats> AppCommandStats);
    
    /**
     * 按时间查询应用统计
     *
     * @param appId 应用id
     * @param td    时间维度
     * @return
     */
    public List<AppStats> getAppStatsList(@Param("appId") long appId, @Param("td") TimeDimensionality td);

    /**
     * 按照分钟查询应用统计
     * @param appId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<AppStats> getAppStatsByMinute(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 按照小时查询应用统计
     * @param appId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<AppStats> getAppStatsByHour(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 按时间查询应用命令统计
     *
     * @param appId       应用id
     * @param commandName 命令名称
     * @param td          时间维度
     * @return
     */
    public List<AppCommandStats> getAppCommandStatsList(@Param("appId") long appId, @Param("commandName") String commandName,
                                                        @Param("td") TimeDimensionality td);

    /**
     * 按应用命令统计
     *
     * @param appId       应用id
     * @param td          时间维度
     * @return
     */
    public List<AppCommandStats> getAppAllCommandStatsList(@Param("appId") long appId,@Param("td") TimeDimensionality td);

    /**
     * 
     * @param appId
     * @param beginTime
     * @param endTime
     * @param commandName
     * @return
     */
    public List<AppCommandStats> getAppCommandStatsListByMinuteWithCommand(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime, @Param("commandName") String commandName);

    /**
     * 
     * @param appId
     * @param beginTime
     * @param endTime
     * @param commandName
     * @return
     */
    public List<AppCommandStats> getAppCommandStatsListByHourWithCommand(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime, @Param("commandName") String commandName);

    /**
     * 
     * @param appId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<AppCommandStats> getAppAllCommandStatsListByMinute(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 
     * @param appId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<AppCommandStats> getAppAllCommandStatsListByHour(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 查询一天中应用的命令执行次数的topN
     *
     * @param appId 应用id
     * @param td    时间维度
     * @return
     */
    public List<AppCommandStats> getTopAppCommandStatsList(@Param("appId") long appId, @Param("td") TimeDimensionality td, @Param("top") int top);

    /**
     * 查询一段时间内，各个命令执行次数分布
     *
     * @param appId 应用id
     * @param td    时间维度
     * @return
     */
    public List<AppCommandStats> getTopAppCommandGroupSum(@Param("appId") long appId, @Param("td") TimeDimensionality td, @Param("top") int top);
    
    /**
     * 获取一定时间内命令峰值
     *
     * @param appId
     * @param commandName
     * @param td          时间维度
     * @return
     */
    public AppCommandStats getCommandClimax(@Param("appId") long appId, @Param("commandName") String commandName, @Param("td") TimeDimensionality td);

    /**
     * 获取应用命令调用次数分布
     *
     * @param appId
     * @param td    时间维度
     * @return
     */
    public List<AppCommandGroup> getAppCommandGroup(@Param("appId") long appId, @Param("td") TimeDimensionality td);

    /**
     * 应用分钟统计
     * @param appId
     * @param beginTime
     * @param endTime
     * @return
     */
    public Map<String, Object> getAppMinuteStat(@Param("appId") long appId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);


    /**
     * 插入当前剩余任务数统计
     * @param queneSizeStatus
     */
    public void mergeQueneSizeStatus(QueneSizeStatus queneSizeStatus);


    /**
     * 获取时间区间内任务堆积统计
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<QueneSizeStatus> getQueneSizeList(@Param("beginTime") long beginTime, @Param("endTime") long endTime);



    Long getAppLastHourOpt(@Param("app_id") long app_id);
}
