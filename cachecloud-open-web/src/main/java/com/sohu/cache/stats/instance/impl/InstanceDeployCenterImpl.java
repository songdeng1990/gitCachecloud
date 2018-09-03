package com.sohu.cache.stats.instance.impl;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sohu.cache.constant.AppCheckEnum;
import com.sohu.cache.constant.InstanceStatusEnum;
import com.sohu.cache.constant.Result;
import com.sohu.cache.dao.AppAuditDao;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.protocol.RedisProtocol;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.stats.instance.InstanceDeployCenter;
import com.sohu.cache.util.TypeUtil;

/**
 * Created by yijunzhang on 14-11-26.
 */
public class InstanceDeployCenterImpl implements InstanceDeployCenter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private InstanceDao instanceDao;

	private RedisCenter redisCenter;

	private RedisDeployCenter redisDeployCenter;

	private MachineCenter machineCenter;

	private AppAuditDao appAuditDao;

	@Override
	public boolean startExistInstance(int instanceId) {
		Assert.isTrue(instanceId > 0L);
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		Assert.isTrue(instanceInfo != null);
		int type = instanceInfo.getType();
		String host = instanceInfo.getIp();
		int port = instanceInfo.getPort();
		boolean isRun;
		if (TypeUtil.isRedisType(type)) {
			isRun = redisCenter.isRun(host, port);
			if (isRun) {
				logger.warn("{}:{} instance is Running", host, port);
			} else {
				String runShell;
				if (TypeUtil.isRedisCluster(type)) {
					runShell = RedisProtocol.getRunShell(port, true);
				} else if (TypeUtil.isRedisSentinel(type)) {
					runShell = RedisProtocol.getSentinelShell(port);
				} else {
					runShell = RedisProtocol.getRunShell(port, false);
				}
				boolean isRunShell = machineCenter.startProcessAtPort(host, port, runShell);
				if (!isRunShell) {
					logger.error("startProcessAtPort-> {}:{} shell= {} failed", host, port, runShell);
					return false;
				} else {
					logger.warn("{}:{} instance has Run", host, port);
				}
				isRun = redisCenter.isRun(host, port);
			}
		} else {
			logger.error("type={} not match!", type);
			isRun = false;
		}
		if (isRun) {
			instanceInfo.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
			instanceDao.update(instanceInfo);
			if (TypeUtil.isRedisType(type)) {
				redisCenter.deployRedisCollection(instanceInfo.getAppId(), instanceInfo.getIp(), instanceInfo.getPort());
			}
			machineCenter.syncInstanceInfoFile(instanceInfo.getIp());
		}

		redisDeployCenter.updateToUnioncache(instanceInfo.getAppId());
		return isRun;
	}

	@Override
	public Result deleteExistInstance(int instanceId) {
		Result rs = Result.fail("");
		try{
			InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
			Assert.isTrue(instanceInfo != null);
			int type = instanceInfo.getType();
			String forgetHost = instanceInfo.getIp();
			int forgetPort = instanceInfo.getPort();
			
			boolean isShutdown = false;
			if (TypeUtil.isRedisCluster(type)) {
				rs = redisDeployCenter.checkClusterForget(instanceId);
				if (rs.isSuccess()) {
					
					long appId = instanceInfo.getAppId();
					List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
					for (InstanceInfo info : instanceList) {
						String clusterIp = info.getIp();
						int clusterPort = info.getPort();
						if (clusterIp.equals(forgetHost) && clusterPort == forgetPort) {
							continue;
						} else {
							if (RedisUtil.isRun(clusterIp, clusterPort, null)) {
								RedisUtil.forgetNode(clusterIp, clusterPort, forgetHost, forgetPort);
							}
						}
					}
					
					isShutdown = shutdownExistInstance(instanceId);
					
					if (!isShutdown){
						String failedMsg = String.format("shut down %s %d failed. ", forgetHost,forgetPort);
						rs = Result.fail(failedMsg);
					}
				}else{
					return rs;
				}
			}else{
				isShutdown = shutdownExistInstance(instanceId);
			}

			

			if (isShutdown){
				instanceDao.delete(instanceInfo.getId());
				redisDeployCenter.updateToUnioncache(instanceInfo.getAppId());
				rs = Result.success("");
			}
		}catch(Exception e){
			rs=Result.fail(e.getMessage());
		}
		return rs;
	}

	@Override
	public boolean shutdownExistInstance(int instanceId) {
		Assert.isTrue(instanceId > 0L);
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		Assert.isTrue(instanceInfo != null);
		int type = instanceInfo.getType();
		String host = instanceInfo.getIp();
		int port = instanceInfo.getPort();
		boolean isShutdown;
		if (TypeUtil.isRedisType(type)) {
			redisCenter.unDeployRedisCollection(instanceInfo.getAppId(), instanceInfo.getIp(), instanceInfo.getPort());
			redisCenter.unDeployRedisSlowLogCollection(instanceInfo.getAppId(), host, port);
			instanceInfo.setStatus(InstanceStatusEnum.OFFLINE_STATUS.getStatus());
			instanceDao.update(instanceInfo);
			machineCenter.syncInstanceInfoFile(instanceInfo.getIp());

			isShutdown = redisCenter.shutdown(host, port);
			if (isShutdown) {
				logger.warn("{}:{} redis is shutdown", host, port);
			} else {
				logger.error("{}:{} redis shutdown error", host, port);
			}
		} else {
			logger.error("type={} not match!", type);
			isShutdown = false;
		}

		redisDeployCenter.updateToUnioncache(instanceInfo.getAppId());
		return isShutdown;
	}

	@Override
	public String showInstanceRecentLog(int instanceId, int maxLineNum) {
		Assert.isTrue(instanceId > 0L);
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		Assert.isTrue(instanceInfo != null);
		try {
			return machineCenter.showInstanceRecentLog(instanceInfo, maxLineNum);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return "";
		}
	}

	@Override
	public boolean modifyInstanceConfig(Long appAuditId, String host, int port, String instanceConfigKey, String instanceConfigValue) {
		Assert.isTrue(appAuditId != null && appAuditId > 0L);
		Assert.isTrue(StringUtils.isNotBlank(host));
		Assert.isTrue(port > 0);
		Assert.isTrue(StringUtils.isNotBlank(instanceConfigKey));
		Assert.isTrue(StringUtils.isNotBlank(instanceConfigValue));
		boolean isModify = redisDeployCenter.modifyInstanceConfig(host, port, instanceConfigKey, instanceConfigValue);
		if (isModify) {
			// 改变审核状态
			appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
		}
		return isModify;
	}

	public void setInstanceDao(InstanceDao instanceDao) {
		this.instanceDao = instanceDao;
	}

	public void setRedisCenter(RedisCenter redisCenter) {
		this.redisCenter = redisCenter;
	}

	public void setMachineCenter(MachineCenter machineCenter) {
		this.machineCenter = machineCenter;
	}

	public void setRedisDeployCenter(RedisDeployCenter redisDeployCenter) {
		this.redisDeployCenter = redisDeployCenter;
	}

	public void setAppAuditDao(AppAuditDao appAuditDao) {
		this.appAuditDao = appAuditDao;
	}

}
