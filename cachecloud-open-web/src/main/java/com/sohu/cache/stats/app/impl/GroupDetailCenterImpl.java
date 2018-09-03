package com.sohu.cache.stats.app.impl;

import com.sohu.cache.constant.TimeDimensionalityEnum;
import com.sohu.cache.dao.AppStatsDao;
import com.sohu.cache.dao.GroupDao;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.dao.InstanceStatsDao;
import com.sohu.cache.entity.*;
import com.sohu.cache.stats.app.GroupDetailCenter;
import com.sohu.cache.web.vo.GroupDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by jiguang on 2016/9/20.
 */
public class GroupDetailCenterImpl implements GroupDetailCenter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private GroupDao groupDao;

    private InstanceDao instanceDao;

    private AppStatsDao appStatsDao;

    private InstanceStatsDao instanceStatsDao;

    private final static String COLLECT_DATE_FORMAT = "yyyyMMddHHmm";

    /**
     * 获取统计的全局信息
     */
    public GroupDetailVO getGroupDetail(long groupId) {
        //根据组id获取该组下的所有应用
        List<AppDesc> appDescList = groupDao.getAppDescByGroupId(groupId);

        if (appDescList == null) {
            return null;
        }

        GroupDetailVO resultVO = new GroupDetailVO();
        //设置该组对应的应用列表
        resultVO.setAppDescList(appDescList);

        Set<String> machines = new HashSet<String>();
        //获取该应用所有实例的基本信息
        List<InstanceInfo> instanceList = new ArrayList<>();
        for(AppDesc appDesc : appDescList) {
            List<InstanceInfo> instances = instanceDao.getInstListByAppId(appDesc.getAppId());
            for(InstanceInfo instance : instances) {
                instanceList.add(instance);
            }
        }

        if (instanceList == null || instanceList.isEmpty()) {
            return resultVO;
        }
        long hits = 0L;
        long miss = 0L;
        long allUsedMemory = 0L;
        long allMaxMemory = 0L;
        //获取该应用所有实例的统计信息
        List<InstanceStats> instanceStatsList = new ArrayList<>();
        for(AppDesc appDesc : appDescList) {
            List<InstanceStats> statsList = instanceStatsDao.getInstanceStatsByAppId(appDesc.getAppId());
            for(InstanceStats instanceStats : statsList) {
                instanceStatsList.add(instanceStats);
            }
        }
        if(instanceStatsList != null && instanceStatsList.size() > 0){
            Map<Long, InstanceStats> instanceStatMap = new HashMap<Long, InstanceStats>();
            for (InstanceStats stats : instanceStatsList) {
                instanceStatMap.put(stats.getInstId(), stats);
            }

            for (InstanceInfo instanceInfo : instanceList) {
                if (instanceInfo.isOffline()) {
                    continue;
                }
                machines.add(instanceInfo.getIp());
                InstanceStats instanceStats = instanceStatMap.get(Long.valueOf(instanceInfo.getId()));
                if (instanceStats == null) {
                    continue;
                }
                boolean isMaster = isMaster(instanceStats);

                long usedMemory = instanceStats.getUsedMemory();
                long usedMemoryMB = usedMemory / 1024 / 1024;

                allUsedMemory += usedMemory;
                allMaxMemory += instanceStats.getMaxMemory();

                hits += instanceStats.getHits();
                miss += instanceStats.getMisses();
                if (isMaster) {
                    resultVO.setMem(resultVO.getMem() + instanceInfo.getMem());
                    resultVO.setCurrentMem(resultVO.getCurrentMem() + usedMemoryMB);
                    resultVO.setCurrentObjNum(resultVO.getCurrentObjNum() + instanceStats.getCurrItems());
                    resultVO.setMasterNum(resultVO.getMasterNum() + 1);
                    //按instanceStats计算conn
                    resultVO.setConn(resultVO.getConn() + instanceStats.getCurrConnections());
                } else {
                    resultVO.setSlaveNum(resultVO.getSlaveNum() + 1);
                }
            }
        }

        resultVO.setMachineNum(machines.size());
        if (allMaxMemory == 0L) {
            resultVO.setMemUsePercent(0.0D);
        } else {
            double percent = 100 * (double) allUsedMemory / (allMaxMemory);
            DecimalFormat df = new DecimalFormat("##.##");
            resultVO.setMemUsePercent(Double.parseDouble(df.format(percent)));
        }

        if (miss == 0L) {
            if (hits > 0) {
                resultVO.setHitPercent(100.0D);
            } else {
                resultVO.setHitPercent(0.0D);
            }
        } else {
            double percent = 100 * (double) hits / (hits + miss);
            DecimalFormat df = new DecimalFormat("##.##");
            resultVO.setHitPercent(Double.parseDouble(df.format(percent)));
        }

        return resultVO;
    }


    /**
     * 通过时间区间查询group的分钟统计数据
     * @param groupId
     * @param beginTime 时间，格式：yyyyMMddHHmm
     * @param endTime   时间，格式：yyyyMMddHHmm
     * @return
     */
    @Override
    public List<GroupStats> getGroupStatsList(final long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum) {
        Assert.isTrue(groupId > 0);
        Assert.isTrue(beginTime > 0 && endTime > 0);

        List<GroupStats> groupStatsList = null;
        try {
            if (TimeDimensionalityEnum.MINUTE.equals(timeDimensionalityEnum)) {
                groupStatsList = groupDao.getGroupStatsByMinute(groupId, beginTime, endTime);
            } else if(TimeDimensionalityEnum.HOUR.equals(timeDimensionalityEnum)) {
                groupStatsList = groupDao.getGroupStatsByHour(groupId, beginTime, endTime);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return groupStatsList;
    }

    @Override
    public List<GroupStats> getGroupStatsListByMinuteTime(long groupId, long beginTime, long endTime) {
        Assert.isTrue(groupId > 0);
        Assert.isTrue(beginTime > 0 && endTime > 0);

        List<GroupStats> groupStatsList = null;
        try {
            groupStatsList = groupDao.getGroupStatsList(groupId, new TimeDimensionality(beginTime, endTime, COLLECT_DATE_FORMAT));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return groupStatsList;
    }

    @Override
    public List<GroupCommandStats> getCommandStatsListV2(long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum, String commandName) {
        if (TimeDimensionalityEnum.MINUTE.equals(timeDimensionalityEnum)) {
            return groupDao.getGroupCommandStatsListByMinuteWithCommand(groupId, beginTime, endTime, commandName);
        } else if(TimeDimensionalityEnum.HOUR.equals(timeDimensionalityEnum)) {
            return groupDao.getGroupCommandStatsListByHourWithCommand(groupId, beginTime, endTime, commandName);
        }
        return Collections.emptyList();
    }

    @Override
    public List<GroupCommandStats> getCommandStatsListV2(long groupId, long beginTime, long endTime, TimeDimensionalityEnum timeDimensionalityEnum) {
        if (TimeDimensionalityEnum.MINUTE.equals(timeDimensionalityEnum)) {
            return groupDao.getGroupAllCommandStatsListByMinute(groupId, beginTime, endTime);
        } else if(TimeDimensionalityEnum.HOUR.equals(timeDimensionalityEnum)) {
            return groupDao.getGroupAllCommandStatsListByHour(groupId, beginTime, endTime);
        }
        return Collections.emptyList();
    }

    @Override
    public List<GroupCommandStats> getCommandStatsList(long groupId, long beginTime, long endTime, String commandName) {
        return groupDao.getGroupCommandStatsList(groupId, commandName, new TimeDimensionality(beginTime, endTime, COLLECT_DATE_FORMAT));
    }

    @Override
    public List<GroupCommandStats> getCommandStatsList(long groupId, long beginTime, long endTime) {
        return groupDao.getGroupAllCommandStatsList(groupId, new TimeDimensionality(beginTime, endTime, COLLECT_DATE_FORMAT));
    }

    @Override
    public List<GroupCommandStats> getTopLimitGroupCommandStatsList(long groupId, long begin, long end, int limit) {
        Assert.isTrue(groupId > 0);
        Assert.isTrue(begin > 0L);
        Assert.isTrue(end > 0L);

        List<GroupCommandStats> topGroupCmdList = null;
        try {
            topGroupCmdList = groupDao.getTopGroupCommandStatsList(groupId, new TimeDimensionality(begin, end, COLLECT_DATE_FORMAT), limit);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return topGroupCmdList;
    }

    /**
     * 查询应用指定命令的峰值
     *
     * @param groupId     业务组id
     * @param beginTime   时间，格式：yyyyMMddHHmm
     * @param endTime     时间，格式：yyyyMMddHHmm
     * @param commandName 命令名
     * @return
     */
    @Override
    public GroupCommandStats getCommandClimax(long groupId, Long beginTime, Long endTime, String commandName) {
        return groupDao.getCommandClimax(groupId, commandName, new TimeDimensionality(beginTime, endTime, COLLECT_DATE_FORMAT));
    }

    @Override
    public List<GroupCommandStats> getTop5GroupCommandStatsList(final long groupId, long begin, long end) {
        Assert.isTrue(groupId > 0);
        Assert.isTrue(begin > 0L);
        Assert.isTrue(end > 0L);

        List<GroupCommandStats> topGroupCmdList = null;
        try {
            topGroupCmdList = groupDao.getTopGroupCommandGroupSum(groupId, new TimeDimensionality(begin, end, COLLECT_DATE_FORMAT), 5);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return topGroupCmdList;
    }


    private boolean isMaster(InstanceStats instanceStats) {
        return instanceStats.getRole() == 1 ? true : false;
    }

    public void setInstanceDao(InstanceDao instanceDao) {
        this.instanceDao = instanceDao;
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    public void setInstanceStatsDao(InstanceStatsDao instanceStatsDao) {
        this.instanceStatsDao = instanceStatsDao;
    }

    public void setAppStatsDao(AppStatsDao appStatsDao) {
        this.appStatsDao = appStatsDao;
    }
}
