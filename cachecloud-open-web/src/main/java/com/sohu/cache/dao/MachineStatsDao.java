package com.sohu.cache.dao;

import com.sohu.cache.entity.*;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by yijunzhang on 14-6-25.
 */
public interface MachineStatsDao {

    /**
     * 插入或更新machineInfo
     */
    public void mergeMachineStats(MachineStats machineStats);

    /**
     * 查询机器下的所有实例的信息
     *
     * @param ip
     * @return
     */
    public List<InstanceInfo> getInstInfoOfMachine(@Param("ip") String ip);

    /**
     * 查询ip所在的机器的最新状态信息
     *
     * @param ip    ip
     * @return      机器的最新状态
     */
    public MachineStats getMachineStatsByIp(@Param("ip") String ip);

    /**
     * 根据机器的hostId查询机器的最新状态信息
     *
     * @param hostId
     * @return
     */
    public MachineStats getMachineStatsByHostId(@Param("hostId") long hostId);

    /**
     * 查询机器下的所有实例的最新状态信息
     *
     * @param hostId    机器的hostId
     * @return          该机器下所有实例的最新统计状态
     */
    public List<InstanceStats> getInstStatOfMachine(@Param("hostId") long hostId);

    /**
     * 分页查询机器统计
     * @param ipLike
     * @return
     */
    public List<MachineStats> getMachineStats(@Param("ipLike") String ipLike);

    /**
     * 获取全部机器统计
     * @return
     */
    public List<MachineStats> getAllMachineStats();

    /**
     * 删除机器统计信息
     * @param ip
     * @return
     */
    public void deleteMachineStatsByIp(@Param("ip") String ip);

    /**
     * 按分组获取机器统计信息
     * @return
     */
    List<MachineGroupStats> getMachineStatsByGroup();


    /**
     * 获取某个分组机器下的总实例统计信息
     * @param groupId
     * @return
     */
    MachineInstanceStats getMachineInstanceStatsByGroup(@Param("groupId") int groupId);


    List<Long> getMachineApps(@Param("groupId") int groupId);
}
