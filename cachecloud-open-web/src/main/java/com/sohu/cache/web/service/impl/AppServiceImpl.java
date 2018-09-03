package com.sohu.cache.web.service.impl;

import com.sohu.cache.constant.*;
import com.sohu.cache.dao.*;
import com.sohu.cache.entity.*;
import com.sohu.cache.exception.SSHException;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.protocol.RedisProtocol;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.AppKeyUtil;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.SecurityUtil;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.service.AppService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.Executors;

/**
 * 应用操作实现类
 *
 * @author leifu
 * @Time 2014年10月21日
 */
public class AppServiceImpl implements AppService {

	private Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

	/**
	 * 应用相关dao
	 */
	private AppDao appDao;

	/**
	 * 应用日志相关dao
	 */
	private AppAuditLogDao appAuditLogDao;

	/**
	 * 实例相关dao
	 */
	private InstanceDao instanceDao;

	/**
	 * 应用用户关系相关dao
	 */
	private AppToUserDao appToUserDao;

	/**
	 * 应用申请相关dao
	 */
	private AppAuditDao appAuditDao;

	/**
	 * 用户信息dao
	 */
	private AppUserDao appUserDao;

	private InstanceStatsDao instanceStatsDao;

	private RedisCenter redisCenter;

	private MachineCenter machineCenter;

	private MachineStatsDao machineStatsDao;
	
	private RedisDeployCenter redisDeployCenter;

	
	@Override
	public int getAppDescCount(AppUser appUser, AppSearch appSearch) {
		int count = 0;
		// 管理员获取全部应用
		if (AppUserTypeEnum.ADMIN_USER.value().equals(appUser.getType())) {
			count = appDao.getAllAppCount(appSearch);
		} else {
			count = appDao.getUserAppCount(appUser.getId());
		}
		return count;
	}

	@Override
	public List<AppDesc> getAppDescList(AppUser appUser, AppSearch appSearch) {
		List<AppDesc> list = new ArrayList<AppDesc>();
		// 管理员获取全部应用
		if (AppUserTypeEnum.ADMIN_USER.value().equals(appUser.getType())) {
			list = appDao.getAllAppDescList(appSearch);
		} else {
			list = appDao.getAppDescList(appUser.getId());
		}
		return list;
	}

	@Override
	public AppDesc getByAppId(Long appId) {
		Assert.isTrue(appId > 0);

		AppDesc appDesc = null;
		try {
			appDesc = appDao.getAppDescById(appId);
			if (appDesc != null) {
				appDesc.setBusinessGroupName(appDao.getGroupName(appDesc.getBusinessGroupId()));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return appDesc;
	}

	@Override
	public int save(AppDesc appDesc) {
		return appDao.save(appDesc);
	}

	@Override
	public int update(AppDesc appDesc) {
		return appDao.update(appDesc);
	}

	@Override
	public List<BusinessGroup> getAllBusinessGroup() {
		return appDao.getAllBusinessGroup();
	}

	@Override
	public String getGroupName(long businessGroupId) {
		return appDao.getGroupName(businessGroupId);
	}

	@Override
	public boolean saveAppToUser(Long appId, Long userId) {
		try {
			// 用户id下应用
			List<AppToUser> list = appToUserDao.getByUserId(userId);
			if (CollectionUtils.isNotEmpty(list)) {
				for (AppToUser appToUser : list) {
					if (appToUser.getAppId().equals(appId)) {
						return true;
					}
				}
			}
			appToUserDao.save(new AppToUser(userId, appId));
			redisDeployCenter.syncUserPriToUnionCache(appId);			
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public void updateAppAuditStatus(Long id, Long appId, Integer status, AppUser appUser) {
		appAuditDao.updateAppAudit(id, status);
		AppDesc appDesc = appDao.getAppDescById(appId);

		if (AppCheckEnum.APP_PASS.value().equals(status)) {
			appDesc.setStatus(AppStatusEnum.STATUS_PUBLISHED.getStatus());
			appDesc.setPassedTime(new Date());
			appDao.update(appDesc);
		} else if (AppCheckEnum.APP_REJECT.value().equals(status)) {
			appDesc.setStatus(AppStatusEnum.STATUS_DENY.getStatus());
			appDao.update(appDesc);
		}
		AppAudit appAudit = appAuditDao.getAppAudit(id);
		// 保存审批日志
		AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, appAudit.getId(),
				AppAuditLogTypeEnum.APP_CHECK);
		if (appAuditLog != null) {
			appAuditLogDao.save(appAuditLog);
		}
	}

	@Override
	public void updateUserAuditStatus(Long id, Integer status) {
		appAuditDao.updateAppAudit(id, status);
	}

	@Override
	public List<AppToUser> getAppToUserList(Long appId) {
		return appToUserDao.getByAppId(appId);
	}

	@Override
	public AppDesc getAppByName(String appName) {
		return appDao.getByAppName(appName);
	}

	@Override
	public boolean restoreBackup(final Long appId,final String date) {
		if (!recoveryPrepareCheck(appId, date)){
			return false;
		}
		Executors.newFixedThreadPool(1).execute(new Runnable(){

			@Override
			public void run() {
				Map<String, String> configStore = new HashMap<String, String>();
				if (offLineApp(appId, configStore)) {
					recovery(appId, date, configStore);
					onLineApp(appId, configStore);
				}
				
				logger.warn("{} recovered to {} successfully.",appId,date);
			}
			
		});
		
		return true;
	}
	
	private boolean recoveryPrepareCheck(long appId, String date){
		
		try{
			List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
			// 首先检查一次所有的备份是否存在，如果所有备份存在，才能发起备份恢复
			for (InstanceInfo info : instanceInfos) {
				final int port = info.getPort();
				final int type = info.getType();
				final String ip = info.getIp();
				if (!RedisUtil.isRun(ip, port, null)){
					logger.error("{} {} is not run, {} backup restore prepare check return false.",ip,port,appId);
					return false;
				}
				
				Jedis jedis = new Jedis(ip,port);
				try{
					if (StringUtils.isNotEmpty(jedis.configGet("slaveof").get(1))){
						continue;
					}
					if (TypeUtil.isRedisDataType(type)) {

						String dbFileName = jedis.configGet("dbfilename").get(1);
						String backupdir = ConstUtils.BAKCUP_DIR + "/" + appId + "/" + date + "/" + ip + "-" + port + "-"
								+ dbFileName;

						if (!SSHUtil.isRemouteFileExist(ip, backupdir)) {
							logger.error(backupdir + " path not exist.BackupRestore stoppped, because backup on " + date
									+ " is not complete for app " + appId);
							return false;
						}

					}
				}finally{
					jedis.close();
				}

			}
			
		}catch(Exception e){
			logger.error("",e);
			return false;
		}
		return true;
	}

	private boolean recovery(long appId, String date, Map<String, String> configStore) {
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		for (InstanceInfo info : instanceInfos) {
			final int port = info.getPort();
			final int type = info.getType();
			final String ip = info.getIp();
			
			if (StringUtils.isNotEmpty(configStore.get(ip+port+"slaveof"))){
				continue;
			}
			
			if (TypeUtil.isRedisDataType(type)) {

				try {

					String dbFileName = configStore.get(ip + port + "dbfilename");
					String dstDumpFilePath = ConstUtils.BAKCUP_DIR + "/" + appId + "/" + date + "/" + ip + "-" + port
							+ "-" + dbFileName;

					String srcDumpFilePath = configStore.get(ip + port + "dir") + "/" + dbFileName;
					String cmd = "rsync -av --bwlimit=" + ConstUtils.COPY_SPEED_LIMIT + " " + dstDumpFilePath + " "
							+ srcDumpFilePath;
					logger.warn(cmd);

					SSHUtil.execute(ip, cmd);
				} catch (SSHException e) {
					throw new  RuntimeException(e);
				}
			}

		}

		return true;
	}

	private boolean onLineApp(Long appId, Map<String, String> aofConfigStore) {
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);

		for (InstanceInfo instanceInfo : instanceInfos) {
			Assert.isTrue(instanceInfo != null);
			int type = instanceInfo.getType();
			String host = instanceInfo.getIp();
			int port = instanceInfo.getPort();
			boolean isRun;
			if (TypeUtil.isRedisType(type)) {
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
			} else {
				logger.error("type={} not match!", type);
				isRun = false;
			}
			if (isRun) {
				String aofconfig = aofConfigStore.get(host + port + "appendonly");
				Jedis jedis = new Jedis(host, port);
				jedis.configSet("appendonly", aofconfig);
				instanceInfo.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
				instanceDao.update(instanceInfo);
				if (TypeUtil.isRedisType(type)) {
					redisCenter.deployRedisCollection(instanceInfo.getAppId(), instanceInfo.getIp(),
							instanceInfo.getPort());
					redisCenter.deployRedisSlowLogCollection(appId, host, port);
				}
			}
		}
		Set<String> ipSet = new HashSet<String>();
		for (InstanceInfo instanceInfo : instanceInfos) {
			ipSet.add(instanceInfo.getIp());
		}
		for (String ip : ipSet) {
			machineCenter.syncInstanceInfoFile(ip);
		}
		return true;
	}

	private boolean offLineApp(Long appId, Map<String, String> configStore) {
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		AppDesc appDesc = getByAppId(appId);
		if (appDesc == null) {
			logger.error("appId={} not exist");
			return false;
		}
		// 机器上运行着自动拉起redis的程序，要下线应用，首先需要通知拉起程序对应的redis已经下线。
		Set<String> ipSet = new HashSet<String>();
		for (InstanceInfo instanceInfo : instanceInfos) {
			ipSet.add(instanceInfo.getIp());
		}

		if (instanceInfos != null) {
			for (InstanceInfo instanceInfo : instanceInfos) {
				final String ip = instanceInfo.getIp();
				final int port = instanceInfo.getPort();
				int type = instanceInfo.getType();
				ipSet.add(instanceInfo.getIp());
				if (TypeUtil.isRedisType(type)) {
					// 取消收集
					redisCenter.unDeployRedisCollection(appId, ip, port);
					redisCenter.unDeployRedisSlowLogCollection(appId, ip, port);

				}
			}

			for (InstanceInfo instanceInfo : instanceInfos) {
				instanceInfo.setStatus(InstanceStatusEnum.OFFLINE_STATUS.getStatus());
				instanceDao.update(instanceInfo);
			}

			for (String ip : ipSet) {
				machineCenter.syncInstanceInfoFile(ip);
			}

			// 更新实例下线
			for (InstanceInfo instanceInfo : instanceInfos) {
				final String ip = instanceInfo.getIp();
				final int port = instanceInfo.getPort();
				int type = instanceInfo.getType();
				ipSet.add(instanceInfo.getIp());
				if (TypeUtil.isRedisType(type)) {
					
					Jedis jedis = new Jedis(ip, port);	
					String aofConfig = jedis.configGet("appendonly").get(1);
					String dbFileName = jedis.configGet("dbfilename").get(1);
					String srcDumpFilePath = jedis.configGet("dir").get(1);
					String slaveof = jedis.configGet("slaveof").get(1);
					configStore.put(ip + port + "appendonly", aofConfig);
					configStore.put(ip + port + "dbfilename", dbFileName);
					configStore.put(ip + port + "dir", srcDumpFilePath);
					configStore.put(ip + port + "slaveof",slaveof);
					jedis.configSet("appendonly", "no");
					jedis.configRewrite();
					jedis.close();
					// 取消收集
					boolean isShutdown = redisCenter.shutdown(ip, port);
					if (!isShutdown) {
						logger.error("{}:{} redis not shutdown!", ip, port);
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public List<String> getAppBackupList(Long appId) {
		List<String> backupList = new ArrayList<String>();
		List<InstanceInfo> resultList = instanceDao.getInstListByAppId(appId);
		InstanceInfo info = resultList.get(0);
		String backupDir = ConstUtils.BAKCUP_DIR + "/" + appId;
		try {
			String dir = SSHUtil.execute(info.getIp(), "ls -t " + backupDir);
			String[] array = dir.split("\\s");
			if (array.length > 0) {
				for (String backup : array) {
					backupList.add(backup);
				}
			}
		} catch (SSHException e) {
			logger.error("", e);
			throw new RuntimeException(e);
		}
		return backupList;
	}

	@Override
	public List<InstanceInfo> getAppInstanceInfo(Long appId) {
		List<InstanceInfo> resultList = instanceDao.getInstListByAppId(appId);
		if (resultList != null && resultList.size() > 0) {
			for (InstanceInfo instanceInfo : resultList) {
				int type = instanceInfo.getType();
				if (instanceInfo.getStatus() != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
					continue;
				}
				if (TypeUtil.isRedisType(type)) {
					if (TypeUtil.isRedisSentinel(type)) {
						continue;
					}
					String host = instanceInfo.getIp();
					int port = instanceInfo.getPort();
					Boolean isMaster = redisCenter.isMaster(host, port);
					instanceInfo.setRoleDesc(isMaster);
					if (isMaster != null && !isMaster) {
						HostAndPort hap = redisCenter.getMaster(host, port);
						if (hap != null) {
							instanceInfo.setMasterHost(hap.getHost());
							instanceInfo.setMasterPort(hap.getPort());
							for (InstanceInfo innerInfo : resultList) {
								if (innerInfo.getIp().equals(hap.getHost()) && innerInfo.getPort() == hap.getPort()) {
									instanceInfo.setMasterInstanceId(innerInfo.getId());
									break;
								}
							}
						}
					}

				}
			}
		}
		return resultList;
	}

	@Override
	public List<InstanceStats> getAppInstanceStats(Long appId) {
		List<InstanceStats> instanceStats = instanceStatsDao.getInstanceStatsByAppId(appId);
		return instanceStats;
	}

	@Override
	public SuccessEnum deleteAppToUser(Long appId, Long userId) {
		try {
			appToUserDao.deleteAppToUser(appId, userId);
			redisDeployCenter.syncUserPriToUnionCache(appId);
			return SuccessEnum.SUCCESS;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return SuccessEnum.FAIL;
		}
	}

	@Override
	public List<AppAudit> getAppAudits(Integer status, Integer type) {
		List<AppAudit> list = appAuditDao.selectWaitAppAudits(status, type);
		for (Iterator<AppAudit> i = list.iterator(); i.hasNext();) {
			AppAudit appAudit = i.next();
			AppDesc appDesc = appDao.getAppDescById(appAudit.getAppId());
			// if (appDesc == null) {
			// i.remove();
			// }
			appAudit.setAppDesc(appDesc);
		}
		return list;
	}

	@Override
	public AppAudit saveAppScaleApply(AppDesc appDesc, AppUser appUser, String applyMemSize, String appScaleReason,
			AppAuditType appScale) {
		AppAudit appAudit = new AppAudit();
		appAudit.setAppId(appDesc.getAppId());
		appAudit.setUserId(appUser.getId());
		appAudit.setUserName(appUser.getName());
		appAudit.setModifyTime(new Date());
		appAudit.setParam1(applyMemSize);
		appAudit.setParam2(appScaleReason);
		appAudit.setInfo("扩容申请---申请容量:" + applyMemSize + ", 申请原因: " + appScaleReason);
		appAudit.setStatus(AppCheckEnum.APP_WATING_CHECK.value());
		appAudit.setType(appScale.getValue());
		appAuditDao.insertAppAudit(appAudit);

		// 保存扩容申请
		AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, appAudit.getId(),
				AppAuditLogTypeEnum.APP_SCALE_APPLY);
		if (appAuditLog != null) {
			appAuditLogDao.save(appAuditLog);
		}

		return appAudit;
	}

	@Override
	public AppAudit saveAppChangeConfig(AppDesc appDesc, AppUser appUser, Long instanceId, String appConfigKey,
			String appConfigValue, String appConfigReason, AppAuditType modifyConfig) {
		AppAudit appAudit = new AppAudit();
		appAudit.setAppId(appDesc.getAppId());
		appAudit.setUserId(appUser.getId());
		appAudit.setUserName(appUser.getName());
		appAudit.setModifyTime(new Date());
		appAudit.setParam1(String.valueOf(instanceId));
		appAudit.setParam2(appConfigKey);
		appAudit.setParam3(appConfigValue);
		appAudit.setInfo("修改配置项:" + appConfigKey + ", 配置值: " + appConfigValue + ", 修改原因: " + appConfigReason);
		appAudit.setStatus(AppCheckEnum.APP_WATING_CHECK.value());
		appAudit.setType(modifyConfig.getValue());
		appAuditDao.insertAppAudit(appAudit);

		// 保存日志
		AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, appAudit.getId(),
				AppAuditLogTypeEnum.APP_CONFIG_APPLY);
		if (appAuditLog != null) {
			appAuditLogDao.save(appAuditLog);
		}

		return appAudit;

	}

	@Override
	public AppAudit saveInstanceChangeConfig(AppDesc appDesc, AppUser appUser, Long instanceId,
			String instanceConfigKey, String instanceConfigValue, String instanceConfigReason,
			AppAuditType instanceModifyConfig) {
		AppAudit appAudit = new AppAudit();
		long appId = appDesc.getAppId();
		appAudit.setAppId(appId);
		appAudit.setUserId(appUser.getId());
		appAudit.setUserName(appUser.getName());
		appAudit.setModifyTime(new Date());
		appAudit.setParam1(String.valueOf(instanceId));
		appAudit.setParam2(instanceConfigKey);
		appAudit.setParam3(instanceConfigValue);
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		String hostPort = instanceInfo == null ? "" : (instanceInfo.getIp() + ":" + instanceInfo.getPort());
		appAudit.setInfo("appId=" + appId + "下的" + hostPort + "实例申请修改配置项:" + instanceConfigKey + ", 配置值: "
				+ instanceConfigValue + ", 修改原因: " + instanceConfigReason);
		appAudit.setStatus(AppCheckEnum.APP_WATING_CHECK.value());
		appAudit.setType(instanceModifyConfig.getValue());
		appAuditDao.insertAppAudit(appAudit);

		// 保存日志
		AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, appAudit.getId(),
				AppAuditLogTypeEnum.INSTANCE_CONFIG_APPLY);
		if (appAuditLog != null) {
			appAuditLogDao.save(appAuditLog);
		}

		return appAudit;
	}

	@Override
	public SuccessEnum updateRefuseReason(AppAudit appAudit, AppUser userInfo) {
		try {
			appAuditDao.updateRefuseReason(appAudit.getId(), appAudit.getRefuseReason());
			return SuccessEnum.SUCCESS;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return SuccessEnum.FAIL;
		}
	}

	@Override
	public int getUserAppCount(Long userId) {
		int count = 0;
		try {
			// 表比较小
			List<AppToUser> list = appToUserDao.getByUserId(userId);
			if (CollectionUtils.isNotEmpty(list)) {
				count = list.size();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return count;
	}

	@Override
	public List<MachineStats> getAppMachineDetail(Long appId) {
		// 应用信息
		Assert.isTrue(appId != null && appId > 0L);
		AppDesc appDesc = appDao.getAppDescById(appId);
		if (appDesc == null) {
			logger.error("appDesc:id={} is not exist");
			return Collections.emptyList();
		}

		// 应用实例列表
		List<InstanceInfo> appInstanceList = getAppInstanceInfo(appId);
		if (CollectionUtils.isEmpty(appInstanceList)) {
			return Collections.emptyList();
		}

		// 防止重复
		Set<String> instanceMachineHosts = new HashSet<String>();
		// 结果列表
		List<MachineStats> machineDetailVOList = new ArrayList<MachineStats>();
		// 应用的机器信息
		for (InstanceInfo instanceInfo : appInstanceList) {
			String ip = instanceInfo.getIp();
			if (instanceMachineHosts.contains(ip)) {
				continue;
			} else {
				instanceMachineHosts.add(ip);
			}
			MachineStats machineStats = machineStatsDao.getMachineStatsByIp(ip);
			if (machineStats == null) {
				continue;
			}
			// 已经分配的内存
			int memoryHost = instanceDao.getMemoryByHost(ip);
			machineStats.setMemoryAllocated(memoryHost);
			// 机器信息
			MachineInfo machineInfo = machineCenter.getMachineInfoByIp(ip);
			if (machineInfo == null) {
				continue;
			}
			// 下线机器不展示
			if (machineInfo.isOffline()) {
				continue;
			}
			machineStats.setInfo(machineInfo);
			machineDetailVOList.add(machineStats);
		}
		return machineDetailVOList;
	}

	@Override
	public AppAudit getAppAuditById(Long appAuditId) {
		return appAuditDao.getAppAudit(appAuditId);
	}

	@Override
	public List<AppAudit> getAppAuditListByAppId(Long appId) {
		Assert.isTrue(appId != null && appId > 0L);
		List<AppAudit> appAudits = appAuditDao.getAppAuditByAppId(appId);
		if (CollectionUtils.isNotEmpty(appAudits)) {
			for (AppAudit appAudit : appAudits) {
				Long appAuditId = appAudit.getId();
				AppAuditLog log = appAuditLogDao.getAuditByType(appAuditId, AppAuditLogTypeEnum.APP_CHECK.value());
				if (log != null) {
					log.setAppUser(appUserDao.get(log.getUserId()));
				}
				appAudit.setAppAuditLog(log);
			}
		}
		return appAudits;
	}

	@Override
	public AppAudit saveRegisterUserApply(AppUser appUser, AppAuditType registerUserApply) {
		AppAudit appAudit = new AppAudit();
		appAudit.setAppId(0);
		appAudit.setUserId(appUser.getId());
		appAudit.setUserName(appUser.getName());
		appAudit.setModifyTime(new Date());
		appAudit.setInfo(
				appUser.getChName() + "申请成为Cachecloud用户, 手机:" + appUser.getMobile() + ",邮箱:" + appUser.getEmail());
		appAudit.setStatus(AppCheckEnum.APP_WATING_CHECK.value());
		appAudit.setType(registerUserApply.getValue());
		appAuditDao.insertAppAudit(appAudit);
		return appAudit;
	}

	@Override
	public List<AppDesc> getAllAppDesc() {
		return appDao.getAllAppDescList(null);
	}

	@Override
	public SuccessEnum changeAppAlertConfig(long appId, int memAlertValue, int clientConnAlertValue, int openAlarm, AppUser appUser) {
		if (appId <= 0 || memAlertValue <= 0 || clientConnAlertValue <= 0) {
			return SuccessEnum.FAIL;
		}
		AppDesc appDesc = appDao.getAppDescById(appId);
		if (appDesc == null) {
			return SuccessEnum.FAIL;
		}
		try {
			// 修改报警阀值
			appDesc.setMemAlertValue(memAlertValue);
			appDesc.setClientConnAlertValue(clientConnAlertValue);
			appDesc.setOpenAlarm(openAlarm);
			appDao.update(appDesc);
			// 添加日志
			AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, 0L, AppAuditLogTypeEnum.APP_CHANGE_ALERT);
			if (appAuditLog != null) {
				appAuditLogDao.save(appAuditLog);
			}
			return SuccessEnum.SUCCESS;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return SuccessEnum.FAIL;
		}
	}

	@Override
	public void updateAuditType(long appAuditId, int redisType) {
		String type = "";
		if (redisType == ConstUtils.CACHE_REDIS_SENTINEL) {
			type = "redis-sentinel";
		} else if (redisType == ConstUtils.CACHE_REDIS_STANDALONE) {
			type = "redis-standalone";
		} else if (redisType == ConstUtils.CACHE_TYPE_REDIS_CLUSTER) {
			type = "redis-cluster";
		}
		appAuditDao.updateAuditType(appAuditId, type);
	}

	@Override
	public void updateAppKey(long appId) {
		appDao.updateAppKey(appId, AppKeyUtil.genSecretKey(appId));
	}
	
	@Override
	public boolean setPwd(Long appId, String pwd){
		try{
			appDao.updateAppPwd(appId, SecurityUtil.encrypt(pwd));
		}catch(Exception e){
			logger.warn("",e);
			return false;
		}
		return true;
	}

	public void setAppDao(AppDao appDao) {
		this.appDao = appDao;
	}

	public void setAppAuditLogDao(AppAuditLogDao appAuditLogDao) {
		this.appAuditLogDao = appAuditLogDao;
	}

	public void setAppToUserDao(AppToUserDao appToUserDao) {
		this.appToUserDao = appToUserDao;
	}

	public void setInstanceDao(InstanceDao instanceDao) {
		this.instanceDao = instanceDao;
	}

	public void setAppAuditDao(AppAuditDao appAuditDao) {
		this.appAuditDao = appAuditDao;
	}

	public void setInstanceStatsDao(InstanceStatsDao instanceStatsDao) {
		this.instanceStatsDao = instanceStatsDao;
	}

	public void setRedisCenter(RedisCenter redisCenter) {
		this.redisCenter = redisCenter;
	}

	public void setMachineCenter(MachineCenter machineCenter) {
		this.machineCenter = machineCenter;
	}

	public void setMachineStatsDao(MachineStatsDao machineStatsDao) {
		this.machineStatsDao = machineStatsDao;
	}

	public void setAppUserDao(AppUserDao appUserDao) {
		this.appUserDao = appUserDao;
	}
	
	public void setRedisDeployCenter(RedisDeployCenter redisDeployCenter) {
		this.redisDeployCenter = redisDeployCenter;
	}

}
