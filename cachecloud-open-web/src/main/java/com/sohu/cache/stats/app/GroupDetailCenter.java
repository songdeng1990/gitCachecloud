package com.sohu.cache.stats.app;

import com.sohu.cache.constant.TimeDimensionalityEnum;
import com.sohu.cache.entity.AppCommandStats;
import com.sohu.cache.entity.GroupCommandStats;
import com.sohu.cache.entity.GroupStats;
import com.sohu.cache.web.vo.GroupDetailVO;

import java.util.List;


/**
 * Created by jiguang on 2016/9/20.
 */
public interface GroupDetailCenter {

    /**
     * 通过groupId获取该分组的统计信息
     */
    public GroupDetailVO getGroupDetail(long groupId);

    /**
     * 通过时间区间查询group的分钟统计数据
     */
    public List<GroupStats> getGroupStatsList(final long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum);

    /**
     * 通过时间区间查询group的分钟统计数据
     */
    public List<GroupStats> getGroupStatsListByMinuteTime(long groupId, long beginTime, long endTime);

    public List<GroupCommandStats> getCommandStatsListV2(long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum, String commandName);

    public List<GroupCommandStats> getCommandStatsListV2(long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum);

    /**
     * 查询应用指定时间段，指定命令名的结果集合
     *
     * @param groupId 业务组id
     * @param beginTime 时间，格式：yyyyMMddHHmm
     * @param endTime 时间，格式：yyyyMMddHHmm
     * @param commandName 命令名
     * @return
     */
    public List<GroupCommandStats> getCommandStatsList(long groupId, long beginTime, long endTime, String commandName);

    /**
     * 查询应用指定时间段，指定命令名的结果集合
     *
     * @param groupId 业务组id
     * @param beginTime 时间，格式：yyyyMMddHHmm
     * @param endTime 时间，格式：yyyyMMddHHmm
     * @return
     */
    public List<GroupCommandStats> getCommandStatsList(long groupId, long beginTime, long endTime);


    public List<GroupCommandStats> getTopLimitGroupCommandStatsList(final long groupId, long begin, long end, int limit);

    public GroupCommandStats getCommandClimax(long groupId, Long beginTime, Long endTime, String commandName);

    public List<GroupCommandStats> getTop5GroupCommandStatsList(final long groupId, long begin, long end);
}
