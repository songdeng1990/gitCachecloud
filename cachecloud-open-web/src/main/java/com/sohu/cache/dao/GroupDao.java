package com.sohu.cache.dao;

import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.GroupCommandStats;
import com.sohu.cache.entity.GroupStats;
import com.sohu.cache.entity.TimeDimensionality;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by caijt on 2016/9/20.
 * 分组统计相关dao操作
 */
public interface GroupDao {
    /**
     * 通过groupId获取对应的app
     * @param groupId
     * @return
     */
    public List<AppDesc> getAppDescByGroupId(@Param("groupId") long groupId);

    /**
     * 插入或更新GroupStats分钟统计
     */
    public void mergeMinuteGroupStats(GroupStats groupStats);

    /**
     * 插入或更新GroupStats小时统计
     */
    public void mergeHourGroupStats(GroupStats groupStats);

    /**
     * 按照分钟查询业务组统计
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<GroupStats> getGroupStatsByMinute(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 按照小时查询业务组统计
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<GroupStats> getGroupStatsByHour(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     * 按时间查询group统计
     *
     * @param businessGroupId 应用id
     * @param td    时间维度
     * @return
     */
    public List<GroupStats> getGroupStatsList(@Param("businessGroupId") long businessGroupId, @Param("td") TimeDimensionality td);

    /**
     *
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<GroupCommandStats> getGroupAllCommandStatsListByMinute(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     *
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @return
     */
    public List<GroupCommandStats> getGroupAllCommandStatsListByHour(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime);

    /**
     *
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @param commandName
     * @return
     */
    public List<GroupCommandStats> getGroupCommandStatsListByMinuteWithCommand(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime, @Param("commandName") String commandName);

    /**
     *
     * @param businessGroupId
     * @param beginTime
     * @param endTime
     * @param commandName
     * @return
     */
    public List<GroupCommandStats> getGroupCommandStatsListByHourWithCommand(@Param("businessGroupId") long businessGroupId, @Param("beginTime") long beginTime, @Param("endTime") long endTime, @Param("commandName") String commandName);

    /**
     * 插入或更新GroupCommandStats分钟统计
     */
    public void mergeMinuteCommandStatus(GroupCommandStats commandStats);

    /**
     * 插入或更新GroupCommandStats小时统计
     */
    public void mergeHourCommandStatus(GroupCommandStats commandStats);

    /**
     * 按时间查询应用命令统计
     *
     * @param businessGroupId  业务组id
     * @param commandName 命令名称
     * @param td          时间维度
     * @return
     */
    public List<GroupCommandStats> getGroupCommandStatsList(@Param("businessGroupId") long businessGroupId, @Param("commandName") String commandName,
                                                        @Param("td") TimeDimensionality td);

    /**
     * 按应用命令统计
     *
     * @param businessGroupId  业务组id
     * @param td          时间维度
     * @return
     */
    public List<GroupCommandStats> getGroupAllCommandStatsList(@Param("businessGroupId") long businessGroupId,@Param("td") TimeDimensionality td);

    /**
     * 查询一天中业务组的命令执行次数的topN
     *
     * @param businessGroupId 业务组id
     * @param td    时间维度
     * @return
     */
    public List<GroupCommandStats> getTopGroupCommandStatsList(@Param("businessGroupId") long businessGroupId, @Param("td") TimeDimensionality td, @Param("top") int top);

    /**
     * 获取一定时间内命令峰值
     *
     * @param businessGroupId
     * @param commandName
     * @param td          时间维度
     * @return
     */
    public GroupCommandStats getCommandClimax(@Param("businessGroupId") long businessGroupId, @Param("commandName") String commandName, @Param("td") TimeDimensionality td);

    /**
     * 查询一段时间内，各个命令执行次数分布
     *
     * @param businessGroupId 业务组id
     * @param td    时间维度
     * @return
     */
    public List<GroupCommandStats> getTopGroupCommandGroupSum(@Param("businessGroupId") long businessGroupId, @Param("td") TimeDimensionality td, @Param("top") int top);
}
