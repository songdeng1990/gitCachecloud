package com.sohu.cache.machine;

import com.sohu.cache.constant.MachineInfoEnum.TypeEnum;
import com.sohu.cache.entity.*;
import com.sohu.cache.exception.SSHException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于host的操作
 *
 * User: lingguo
 * Date: 14-6-12
 * Time: 上午10:32
 */
public interface MachineCenter {

	/**
	 * 同步当前redis实例信息到机器上，以供监控
	 * @param ip 机器ip
	 * @throws SSHException 
	 */
    void syncInstanceInfoFile(String ip);
	
    /**
     * 为当前host创建trigger，并部署
     *
     * @param hostId    机器id
     * @param ip        ip
     * @return          是否部署成功
     */
    boolean deployMachineCollection(final long hostId, final String ip);
    
    /**
     * 为当前host删除trigger,取消部署
     *
     * @param hostId    机器id
     * @param ip        ip
     * @return          是否取消部署成功
     */
    boolean unDeployMachineCollection(final long hostId, final String ip);

    /**
     * 收集host的状态信息
     *
     * @param hostId        机器id
     * @param collectTime   收集时间
     * @param ip            ip
     * @return              机器的信息
     */
    Map<String, Object> collectMachineInfo(final long hostId, final long collectTime, final String ip);
    
    /**
     * 异步收集host的状态信息
     *
     * @param hostId        机器id
     * @param collectTime   收集时间
     * @param ip            ip
     */
    void asyncCollectMachineInfo(final long hostId, final long collectTime, final String ip);

    /**
     * 为当前机器的监控删除trigger
     *
     * @param hostId    机器id
     * @param ip    ip
     * @return      取消部署成功返回true， 否则返回false
     */
    boolean unDeployMachineMonitor(final long hostId, final String ip);
    
    /**
     * 为当前机器的监控创建trigger
     *
     * @param hostId    机器id
     * @param ip    ip
     * @return      部署成功返回true， 否则返回false
     */
    boolean deployMachineMonitor(final long hostId, final String ip);

    /**
     * 监控机器的状态信息，向上层汇报或者报警
     *
     * @param hostId    机器id
     * @param ip        ip
     * @return
     */
    void monitorMachineStats(final long hostId, final String ip);
    
    /**
     * 异步监控机器的状态信息，向上层汇报或者报警
     *
     * @param hostId    机器id
     * @param ip        ip
     * @return
     */
    void asyncMonitorMachineStats(final long hostId, final String ip);

    /**
     * 在主机ip上的端口port上启动一个进程，并check是否启动成功；
     *
     * @param ip    ip
     * @param port  端口
     * @param shell shell命令
     * @return 是否成功
     */
    boolean startProcessAtPort(String ip, int port, final String shell);

    /**
     * 执行shell命令并获取返回结果
     *
     * @param ip
     * @param shell
     * @return
     */
    String executeShell(final String ip, String shell);

    /**
     * 根据类型返回机器可用端口
     *
     * @param ip
     * @param type
     * @return
     */
    Integer getAvailablePort(final String ip, final int type);

    /**
     * 创建远程文件
     *
     * @param host
     * @param fileName
     * @param content
     * @return 是否创建成功
     */
    String createRemoteFile(final String host, String fileName, List<String> content);


    /**
     * 获取机器列表
     * @param ipLike
     * @extraDesc extraDesc
     * @return
     */
    List<MachineStats> getMachineStats(String ipLike,String extraDesc,String groupName, int start, int size);


    int countMachine(String ipLike, String extraDesc,String groupName);
    
   
    MachineStats getMachineStatsByIp(String ip);

    /**
     * 根据ip获取机器信息
     * @param ip
     * @return
     */
    MachineInfo getMachineInfoByIp(String ip);


    MachineStats getMachineMemoryDetail(String ip);
    
    /**
     * 获取一台机器的所有实例
     * @param ip
     * @return
     */
    List<InstanceInfo> getMachineInstanceInfo(String ip);
    
    
    /**
     * 获取一台机器的所有实例统计信息
     * @param ip
     * @return
     */
    List<InstanceStats> getMachineInstanceStatsByIp(String ip);

    /**
     * 获取指定机器某个redis端口的最近日志
     * @param maxLineNum
     * @return
     */
    String showInstanceRecentLog(InstanceInfo instanceInfo, int maxLineNum);

    /**
     * 根据机器类型获取机器列表
     * @param typeEnum
     * @return
     */
    List<MachineInfo> getMachineInfoByType(TypeEnum typeEnum);
    
    /**
     * 为当前ip创建trigger，并部署
     *
     * @param hostId    机器id
     * @param ip        ip
     * @return          是否部署成功
     */
    boolean deployServerCollection(long hostId, String ip);
    
    /**
     * 为当前服务器状态收集删除trigger
     * @param hostId    机器id
     * @param ip    ip
     * @return      取消部署成功返回true， 否则返回false
     */
    boolean unDeployServerCollection(final long hostId, final String ip);

    /**
     * 获取所有机房信息
     * @return
     */
    List<MachineRoom> getAllRoom();

    /**
     * 初始化监控脚本
     * @param ip
     */
	void initMonitorScript(String ip);

	
	/**
	 * 初始化所有机器的监控脚本，和监控信息
	 * @return true 更新成功，false更新遇到异常。
	 */
	boolean updateAllMonitorScript();

	/**
	 * 获取所有已经能连接的机器列表
	 * @param ipLike
	 * @param extraDesc
	 * @return
	 */
	List<MachineStats> getAllMachineStats(String ipLike, String extraDesc);

	/**
	 * 获取机器标签说明的组
	 * @return
	 */
	Set<String> getGroups();

	/**
	 * 直接调用对应dao返回数据库结果
	 * @return
	 */
	List<MachineStats> getSimpleMachineStats();

	//List<MachineStats> getAllMachineStats();

    /**
     * 按分组获取机器的统计信息
     * @return
     */
    List<MachineGroupStats> getMachineStatsByGroup();


    /**
     * 获取某个分组下的机器的总实例统计
     * @return
     */
    MachineInstanceStats getMachineInstanceStatsByGroup(int groupId);

    /**
     * 获取所有机器分组名
     * @return
     */
    List<String> getMachineGroups();


    /**
     * 获取特定分组下的最新一小时总ops
     * @param groupId
     * @return
     */
    long getMachineGroupOps(int groupId);

    long getGroupIdByName(String name);

	boolean updateMachineInfo();
}
