package com.sohu.cache.machine;

import com.sohu.cache.entity.MachineInfo;

import java.util.List;

/**
 * 机器部署相关
 * @author leifu
 * changed @Date 2016-4-24
 * @Time 下午5:07:30
 */
public interface MachineDeployCenter {

    /**
     * 增加一台机器:入db和开启统计
     *
     * @param machineInfo
     */
    public boolean addMachine(MachineInfo machineInfo);

    /**
     * 移除一台机器：删db数据和关闭统计
     *
     * @param machineInfo
     */
    public boolean removeMachine(MachineInfo machineInfo);

    /**
     * 通过机器IP，获取机器信息
     */
    public String getMachineInfo(String idcId, List<String> ipList);

    /**
     * 在iP所代表机器上执行redis安装脚本。
     * @param ip
     * @return
     */
	boolean installRedis(String ip);
}
