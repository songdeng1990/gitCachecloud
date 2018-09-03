package com.sohu.cache.machine.impl;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.sohu.cache.dao.MachineDao;
import com.sohu.cache.dao.MachineStatsDao;
import com.sohu.cache.dao.ServerStatusDao;
import com.sohu.cache.entity.MachineInfo;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.machine.MachineDeployCenter;
import com.sohu.cache.protocol.MachineProtocol;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.FileUtils;
import com.sohu.cache.util.HttpPostUtil;

/**
 * 机器部署相关
 * @author leifu
 * changed @Date 2016-4-24
 * @Time 下午5:07:30
 */
public class MachineDeployCenterImpl implements MachineDeployCenter {
    private Logger logger = LoggerFactory.getLogger(MachineDeployCenterImpl.class);

    private MachineDao machineDao;

    private MachineCenter machineCenter;

    private MachineStatsDao machineStatsDao;
    
    private ServerStatusDao serverStatusDao;

    /**
     * 将机器加入资源池并统计、监控
     *
     * @param machineInfo
     * @return
     */
    @Override
    public boolean addMachine(MachineInfo machineInfo) {
        boolean success = true;

        if (machineInfo == null || Strings.isNullOrEmpty(machineInfo.getIp())) {
            logger.error("machineInfo is null or ip is valid.");
            return false;
        }
        // 将机器信息保存到db中
        try {
            machineDao.saveMachineInfo(machineInfo);
        } catch (Exception e) {
            logger.error("save machineInfo: {} to db error.", machineInfo.toString(), e);
            return false;
        }

        // 为机器添加统计和监控的定时任务
        try {
            MachineInfo thisMachine = machineDao.getMachineInfoByIp(machineInfo.getIp());
            if (thisMachine != null) {
                long hostId = thisMachine.getId();
                String ip = thisMachine.getIp();
                if (!machineCenter.deployMachineCollection(hostId, ip)) {
                    logger.error("deploy machine collection error, machineInfo: {}", thisMachine.toString());
                    success = false;
                }
                if (!machineCenter.deployMachineMonitor(hostId, ip)) {
                    logger.error("deploy machine monitor error, machineInfo: {}", thisMachine.toString());
                    success = false;
                }
                if(thisMachine.getCollect() == 1) {
                	if (!machineCenter.deployServerCollection(hostId, ip)) {
                		logger.error("deploy server monitor error, machineInfo: {}", thisMachine.toString());
                		success = false;
                	}
                } else {
                	if (!machineCenter.unDeployServerCollection(hostId, ip)) {
                		logger.error("undeploy server monitor error, machineInfo: {}", thisMachine.toString());
                		success = false;
                	}
                }
            }
        } catch (Exception e) {
            logger.error("query machineInfo from db error, ip: {}", machineInfo.getIp(), e);
        }

        if (success) {
            logger.info("save and deploy machine ok, machineInfo: {}", machineInfo.toString());
        }
        return success;
    }

    /**
     * 删除机器，并删除相关的定时任务
     *
     * @param machineInfo
     * @return
     */
    @Override
    public boolean removeMachine(MachineInfo machineInfo) {
        if (machineInfo == null || Strings.isNullOrEmpty(machineInfo.getIp())) {
            logger.warn("machineInfo is null or ip is empty.");
            return false;
        }
        String machineIp = machineInfo.getIp();
        
        //从quartz中删除相关的定时任务
        try {
            MachineInfo thisMachine = machineDao.getMachineInfoByIp(machineIp);
            long hostId = thisMachine.getId();
            
            if (!machineCenter.unDeployMachineCollection(hostId, machineIp)) {
                logger.error("remove trigger for machine error: {}", thisMachine.toString());
                return false;
            }
            if (!machineCenter.unDeployMachineMonitor(hostId, machineIp)) {
                logger.error("remove trigger for machine monitor error: {}", thisMachine.toString());
                return false;
            }
            if (!machineCenter.unDeployServerCollection(hostId, machineIp)) {
                logger.error("remove trigger for server monitor error: {}", thisMachine.toString());
                return false;
            }
        } catch (Exception e) {
            logger.error("query machineInfo from db error: {}", machineInfo.toString());
        }
        
        // 从db中删除machine和相关统计信息
        try {
            machineDao.removeMachineInfoByIp(machineIp);
            machineStatsDao.deleteMachineStatsByIp(machineIp);
            serverStatusDao.deleteServerInfo(machineIp);
        } catch (Exception e) {
            logger.error("remove machineInfo from db error, machineInfo: {}", machineInfo.toString(), e);
            return false;
        }
        logger.info("remove and undeploy machine ok: {}", machineInfo.toString());
        return true;
    }

    @Override
    public String getMachineInfo(String idcId, List<String> ipList) {
        return HttpPostUtil.sendHttpPostRequest(ConstUtils.MACHINE_URL,"action=api&batch_ip=" + toIpList(ipList));
    }
    
    private String toIpList(List<String> ipList){
    	StringBuffer rsp=new StringBuffer();
    	for (int c=0;c < ipList.size(); c++){
    		if(c == ipList.size() -1){
    			rsp.append(ipList.get(c));
    		}else{
    			rsp.append(ipList.get(c)).append(",");
    		}
    			
    	}
    	
    	return rsp.toString();
    }
    
    @Override
    public boolean installRedis(String ip)
    {
    	boolean result = true;
    	
    	String tmpScriptPath = MachineProtocol.TMP_DIR + ConstUtils.REDIS_SCRIPT_NAME;
    	File tmpFile = new File(tmpScriptPath);
    	try
    	{
    		//判断安装脚本是否存在。不能存在则创建，然后直接拷贝到远程目录。  	
    		if (!tmpFile.exists())
    		{
    			FileUtils.createTmpScript(tmpFile,ConstUtils.REDIS_SCRIPT_PATH);
    		}
    		
    		SSHUtil.scpFileToRemoteDir(ip, tmpFile.getAbsolutePath(), "~");
    		
    		String returnString = SSHUtil.execute(ip, "cd ~;chmod +x " + ConstUtils.REDIS_SCRIPT_NAME + ";sh " + ConstUtils.REDIS_SCRIPT_NAME + ";");
    		if (StringUtils.isEmpty(returnString))
    		{
    			result = false;
    		}
    		
    		machineCenter.initMonitorScript(ip);
    	}
    	catch (Throwable e)
    	{
    		logger.error("",e);
    		result = false;
    	}
    	
    	if (result)
    	{
    		machineDao.setRedisInstalled(ip);
    		logger.info(String.format("Installation of redis on %s succeeded.", ip));    		
    	}
    	else
    	{
    		logger.error(String.format("Installation of redis on %s failed.", ip));    		
    	}
    	
    	return result;
    }
    
    public void setMachineDao(MachineDao machineDao) {
        this.machineDao = machineDao;
    }

    public void setMachineCenter(MachineCenter machineCenter) {
        this.machineCenter = machineCenter;
    }

    public void setMachineStatsDao(MachineStatsDao machineStatsDao) {
        this.machineStatsDao = machineStatsDao;
    }

	public void setServerStatusDao(ServerStatusDao serverStatusDao) {
		this.serverStatusDao = serverStatusDao;
	}

    class MachineInfoParams {
        private String idcId;
        private List<String> ipList;

        public String getIdcId() {
            return idcId;
        }

        public void setIdcId(String idcId) {
            this.idcId = idcId;
        }

        public List<String> getIpList() {
            return ipList;
        }

        public void setIpList(List<String> ipList) {
            this.ipList = ipList;
        }
    }
}
