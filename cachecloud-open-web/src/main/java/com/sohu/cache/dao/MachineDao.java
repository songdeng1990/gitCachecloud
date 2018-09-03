package com.sohu.cache.dao;

import com.sohu.cache.entity.MachineInfo;

import com.sohu.cache.entity.MachineRoom;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机器相关的操作
 *
 * User: lingguo
 * Date: 14-6-12
 * Time: 下午2:33
 */
public interface MachineDao {

    /**
     * 返回所有可用的机器资源
     *
     * @return
     */
    List<MachineInfo> getAllMachines();
    
    

    /**
     * 通过ip查询机器信息
     *
     * @param ip
     * @return
     */
    MachineInfo getMachineInfoByIp(@Param("ip") String ip);
    
    /**
     * 通过ip模糊查询机器信息
     * @param ipLike
     * @param extraDesc
     * @return
     */
    List<MachineInfo> getMachineInfoByConditions(@Param("ipLike")String ipLike,@Param("extraDesc")String extraDesc,@Param("groupName")String groupName,@Param("start") int start,@Param("size") int size);

    /**
     * 统计符合条件的机器数
     * @param ipLike
     * @param extraDesc
     * @return
     */
    int countMachine(@Param("ipLike")String ipLike,@Param("extraDesc")String extraDesc,@Param("groupName")String groupName);

    /**
     * 保存一条机器信息
     *
     * @param machineInfo
     */
    void saveMachineInfo(MachineInfo machineInfo);

    /**
     * 根据ip删除一台机器的信息；
     *
     * @param ip
     */
    void removeMachineInfoByIp(@Param("ip") String ip);
    
    /**
     * 通过type查询机器列表
     * @param type
     * @return
     */
    List<MachineInfo> getMachineInfoByType(@Param("type") int type);
    
    /**
     * 更新机器type
     * @return
     */
    int updateMachineType(@Param("id") long id, @Param("type") int type);
    
    /**
     * 设置redis已安装
     * @return
     */
    int setRedisInstalled(@Param("ip") String ip);

    List<MachineRoom> getAllRoom();

    /**
     * 获取所有机器分组的名称
     * @return
     */
    List<String> getMachineGroups();

    long getGroupIdByName(String name);
}
