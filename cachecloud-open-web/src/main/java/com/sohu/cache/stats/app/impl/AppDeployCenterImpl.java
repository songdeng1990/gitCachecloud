package com.sohu.cache.stats.app.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;
import com.sohu.cache.async.NamedThreadFactory;
import com.sohu.cache.constant.AppAuditLogTypeEnum;
import com.sohu.cache.constant.AppAuditType;
import com.sohu.cache.constant.AppCheckEnum;
import com.sohu.cache.constant.AppStatusEnum;
import com.sohu.cache.constant.InstanceStatusEnum;
import com.sohu.cache.constant.MachineInfoEnum;
import com.sohu.cache.constant.Result;
import com.sohu.cache.dao.AppAuditDao;
import com.sohu.cache.dao.AppAuditLogDao;
import com.sohu.cache.dao.AppDao;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.entity.AppAudit;
import com.sohu.cache.entity.AppAuditLog;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.MachineInfo;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisClusterNode;
import com.sohu.cache.redis.RedisClusterReshard;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.redis.ReshardProcess;
import com.sohu.cache.stats.app.AppDeployCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.service.AppService;
import com.sohu.cache.web.util.AppEmailUtil;

import redis.clients.jedis.HostAndPort;
import redis.clients.util.ClusterNodeInformation;

/**
 * Created by yijunzhang on 14-10-20.
 */
public class AppDeployCenterImpl implements AppDeployCenter {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private AppService appService;

	private RedisDeployCenter redisDeployCenter;

	private RedisCenter redisCenter;

	private AppEmailUtil appEmailUtil;

	private AppAuditDao appAuditDao;

	private MachineCenter machineCenter;

	private InstanceDao instanceDao;

	private AppAuditLogDao appAuditLogDao;

	private AppDao appDao;
	
	private static final Object reshardLock = new Object();

	private Map<Long, Map<String,ReshardProcess>> appIdProcessMap = new ConcurrentSkipListMap<Long, Map<String,ReshardProcess>>();

	private ExecutorService processThreadPool = new ThreadPoolExecutor(0, 256, 0L, TimeUnit.MILLISECONDS,
			new SynchronousQueue<Runnable>(), new NamedThreadFactory("redis-cluster-reshard", false));
	
	private Set<Long> deployCheckIdSet = new  ConcurrentHashSet<Long>();

	@Override
	public boolean isDeployConfigDuplicate(long deployConfigId){
		if (!deployCheckIdSet.contains(deployConfigId)){
			deployCheckIdSet.add(deployConfigId);
			return false;
		}else{
			return true;
		}
	}
	
	
	@Override
	public boolean createApp(AppDesc appDesc, AppUser appUser, String memSize) {
		try {
			appService.save(appDesc);
			// 保存应用和用户的关系
			appService.saveAppToUser(appDesc.getAppId(), appDesc.getUserId());
			// 更新appKey
			long appId = appDesc.getAppId();
			appService.updateAppKey(appId);

			// 保存应用审批信息
			AppAudit appAudit = new AppAudit();
			appAudit.setAppId(appId);
			appAudit.setUserId(appUser.getId());
			appAudit.setUserName(appUser.getName());
			appAudit.setModifyTime(new Date());
			appAudit.setParam1(memSize);
			appAudit.setParam2(appDesc.getTypeDesc());
			appAudit.setInfo("类型:" + appDesc.getTypeDesc() + ";初始申请空间:" + memSize);
			appAudit.setStatus(AppCheckEnum.APP_WATING_CHECK.value());
			appAudit.setType(AppAuditType.APP_AUDIT.getValue());
			appAuditDao.insertAppAudit(appAudit);

			// 发邮件
			appEmailUtil.noticeAppResult(appDesc, appAudit);

			// 保存申请日志
			AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, appAudit.getId(),
					AppAuditLogTypeEnum.APP_DESC_APPLY);
			if (appAuditLog != null) {
				appAuditLogDao.save(appAuditLog);
			}

			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Result batchAddSlaveDeploy(Long appId, String slaveText) throws Exception{
		if (appId == null) {
			logger.error("appId is null");
			return Result.fail("应用id不能为空!");
		}
		if (StringUtils.isBlank(slaveText)) {
			logger.error("slaveText is null");
			return Result.fail("部署节点列表不能为空!");
		}
		
		String[] nodeInfoList = slaveText.split(ConstUtils.NEXT_LINE);
		
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			if (StringUtils.isBlank(nodeInfo)) {
				return Result.fail(String.format("部署列表%s中存在空行", slaveText));
			}
			String[] array = nodeInfo.split(ConstUtils.COLON);
			if (array == null || array.length == 0) {
				return Result.fail(String.format("部署列表%s中存在空行", slaveText));
			}
			
			if (array.length != 3){
				return Result.fail(String.format("%s需要符合master:port:slave的格式", nodeInfo));
			}
			
			String masterHost = array[0];
			String port = array[1];
			String slaveHost = array[2];
			
			InstanceInfo masterInfo = instanceDao.getInstByIpAndPort(masterHost, Integer.valueOf(port));
			
			redisDeployCenter.addSlaveAndBlock(appId, masterInfo.getId(), slaveHost);			
		}
		
		return Result.success("所有slave部署成功");
	}
	@Override
	public Result batchAddSlaveCheck(Long appId, String slaveText){
		if (appId == null) {
			logger.error("appId is null");
			return Result.fail("应用id不能为空!");
		}
		if (StringUtils.isBlank(slaveText)) {
			logger.error("slaveText is null");
			return Result.fail("部署节点列表不能为空!");
		}
		String[] nodeInfoList = slaveText.split(ConstUtils.NEXT_LINE);
		if (nodeInfoList == null || nodeInfoList.length == 0) {
			logger.error("nodeInfoList is null");
			return Result.fail("部署节点列表不能为空!");
		}
		
		AppDesc appDesc = appService.getByAppId(appId);
		if (appDesc == null) {
			logger.error("appDesc:id={} is not exist");
			return Result.fail(String.format("appId=%s不存在", appId));
		}
		int type = appDesc.getType();
		StringBuffer physicalIpConflictBuffer = new StringBuffer();
		
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			if (StringUtils.isBlank(nodeInfo)) {
				return Result.fail(String.format("部署列表%s中存在空行", slaveText));
			}
			String[] array = nodeInfo.split(ConstUtils.COLON);
			if (array == null || array.length == 0) {
				return Result.fail(String.format("部署列表%s中存在空行", slaveText));
			}
			
			if (array.length != 3){
				return Result.fail(String.format("%s需要符合master:port:slave的格式", nodeInfo));
			}
			
			String masterHost = array[0];
			String port = array[1];
			String slaveHost = array[2];
			
			
			
			if (!checkHostExist(masterHost)) {
				return Result.fail(String.format("%s中的ip=%s不存在，请在机器管理中添加!", nodeInfo, masterHost));
			}
			if (StringUtils.isNotBlank(port) && !NumberUtils.isDigits(port)) {
				return Result.fail(String.format("%s中的中的memSize=%s不是整数!", nodeInfo, port));
			}
			if (StringUtils.isNotBlank(slaveHost) && !checkHostExist(slaveHost)) {
				return Result.fail(String.format("%s中的ip=%s不存在，请在机器管理中添加!", nodeInfo, slaveHost));
			}
			
			if (!checkInstanceInApp(appId, masterHost, port)){
				return Result.fail(String.format("%s,%s 不存在于应用%s中，请纠正配置",masterHost,port,appId));
			}
			
			
			String rs = checkPhysicalIpConflick(nodeInfo,masterHost,slaveHost);
			if (StringUtils.isNotEmpty(rs)){
				physicalIpConflictBuffer.append(rs).append(ConstUtils.NEXT_LINE);
			}
		}
		
		if (StringUtils.isNotEmpty(physicalIpConflictBuffer.toString())){
			return Result.fail(String.format("存在主从在相同物理母机或者物理机信息为空的现象，请修正配置，详情如下:%s%s",ConstUtils.NEXT_LINE,physicalIpConflictBuffer.toString()));
		}
		
		
		return Result.success("格式正确，可以开始部署");
	}
	
	@Override
	public Result checkAppDeployDetail(Long appAuditId, String appDeployText) {
		if (appAuditId == null) {
			logger.error("appAuditId is null");
			return Result.fail("审核id不能为空!");
		}
		if (StringUtils.isBlank(appDeployText)) {
			logger.error("appDeployText is null");
			return Result.fail("部署节点列表不能为空!");
		}
		String[] nodeInfoList = appDeployText.split(ConstUtils.NEXT_LINE);
		if (nodeInfoList == null || nodeInfoList.length == 0) {
			logger.error("nodeInfoList is null");
			return Result.fail("部署节点列表不能为空!");
		}
		AppAudit appAudit = appAuditDao.getAppAudit(appAuditId);
		if (appAudit == null) {
			logger.error("appAudit:id={} is not exist", appAuditId);
			return Result.fail(String.format("审核id=%s不存在", appAuditId));
		}
		long appId = appAudit.getAppId();
		AppDesc appDesc = appService.getByAppId(appId);
		if (appDesc == null) {
			logger.error("appDesc:id={} is not exist");
			return Result.fail(String.format("appId=%s不存在", appId));
		}
		int type = appDesc.getType();

		// Map<String, Integer> memNeed = new HashMap<>();
		// Integer mem;

		// 检查每一行
		
		StringBuffer physicalIpConflictBuffer = new StringBuffer();
		
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			if (StringUtils.isBlank(nodeInfo)) {
				return Result.fail(String.format("部署列表%s中存在空行", appDeployText));
			}
			String[] array = nodeInfo.split(ConstUtils.COLON);
			if (array == null || array.length == 0) {
				return Result.fail(String.format("部署列表%s中存在空行", appDeployText));
			}
			String masterHost = null;
			String memSize = null;
			String slaveHost = null;
			if (TypeUtil.isRedisCluster(type)) {
				if (array.length == 2) {
					masterHost = array[0];
					memSize = array[1];
				} else if (array.length == 3) {
					masterHost = array[0];
					memSize = array[1];
					slaveHost = array[2];
				} else {
					return Result.fail(String.format("部署列表中%s, 格式错误!", nodeInfo));
				}
			} else if (TypeUtil.isRedisSentinel(type)) {
				if (array.length == 3) {
					masterHost = array[0];
					memSize = array[1];
					slaveHost = array[2];
				} else if (array.length == 1) {
					masterHost = array[0];
				} else {
					return Result.fail(String.format("部署列表中%s, 格式错误!", nodeInfo));
				}
			} else if (TypeUtil.isRedisStandalone(type)) {
				if (array.length == 3) {
					masterHost = array[0];
					memSize = array[1];
					slaveHost = array[2];
				} else if(array.length == 2) {
					masterHost = array[0];
					memSize = array[1];
				} else {
					return Result.fail(String.format("部署列表中%s, 格式错误!", nodeInfo));
				}
			}
			if (!checkHostExist(masterHost)) {
				return Result.fail(String.format("%s中的ip=%s不存在，请在机器管理中添加!", nodeInfo, masterHost));
			}
			if (StringUtils.isNotBlank(memSize) && !NumberUtils.isDigits(memSize)) {
				return Result.fail(String.format("%s中的中的memSize=%s不是整数!", nodeInfo, memSize));
			}
			if (StringUtils.isNotBlank(slaveHost) && !checkHostExist(slaveHost)) {
				return Result.fail(String.format("%s中的ip=%s不存在，请在机器管理中添加!", nodeInfo, slaveHost));
			}
			
			if (StringUtils.isNotBlank(slaveHost)){
				String rs = checkPhysicalIpConflick(nodeInfo,masterHost,slaveHost);
				if (StringUtils.isNotEmpty(rs)){
					physicalIpConflictBuffer.append(rs).append(ConstUtils.NEXT_LINE);
				}
			}
			
			// 计算每个机器需要的内存
			// memNeed.put(masterHost, (mem=memNeed.get(masterHost)) == null ?
			// Integer.parseInt(memSize) : mem + Integer.parseInt(memSize));
			// if(StringUtils.isNotBlank(slaveHost)) {
			// memNeed.put(slaveHost, (mem=memNeed.get(slaveHost)) == null ?
			// Integer.parseInt(memSize) : mem + Integer.parseInt(memSize));
			// }
		}
		
		if (StringUtils.isNotEmpty(physicalIpConflictBuffer.toString())){
			return Result.fail(String.format("存在主从在相同物理母机或者物理机信息为空的现象，请修正配置，详情如下:%s%s",ConstUtils.NEXT_LINE,physicalIpConflictBuffer.toString()));
		}

		// 检查机器剩余可用内存是否足够
		// List<MachineStats> machineList = machineCenter.getAllMachineStats();
		// for(MachineStats machineStats : machineList) {
		// //System.out.println("机器IP"+machineStats.getIp()+"，已分配："+machineStats.getMemoryAllocated()+"，预分配："+((mem=memNeed.get(machineStats.getIp()))
		// == null ? 0 : mem) +
		// "，总内存："+(Long.parseLong(machineStats.getMemoryTotal())/(1024*1024)));
		// if(machineStats.getMemoryAllocated() +
		// ((mem=memNeed.get(machineStats.getIp())) == null ? 0 : mem) >
		// (Long.parseLong(machineStats.getMemoryTotal())/(1024*1024))) {
		// return
		// DataFormatCheckResult.fail(String.format("[%s]机器剩余的内存不足，请选择其他机器进行分配!",
		// machineStats.getIp()));
		// }
		// }

		// 检查sentinel类型:数据节点一行，sentinel节点多行
		if (TypeUtil.isRedisSentinel(type)) {
			return checkSentinelAppDeploy(nodeInfoList);
			// 检查单点类型:只能有一行数据节点
		}
		// standardalone 格式要求一次性可以部署多个，所以不再作单点检查。
		/*
		 * else if (TypeUtil.isRedisStandalone(type)) { return
		 * checkStandaloneAppDeploy(nodeInfoList); }
		 */
		return Result.success("应用部署格式正确，可以开始部署了!");
	}

	/**
	 * 检查单点格式
	 * 
	 * @param nodeInfoList
	 * @return
	 */
	private Result checkStandaloneAppDeploy(String[] nodeInfoList) {
		int redisLineNum = 0;
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			String[] array = nodeInfo.split(ConstUtils.COLON);
			if (array.length == 2) {
				redisLineNum++;
			}
		}
		// redis节点只有一行
		if (redisLineNum != 1) {
			return Result.fail("应用部署格式错误, Standalone格式必须是一行masterIp:memSize(M)");
		}
		return Result.success("应用部署格式正确，可以开始部署了!");
	}

	/**
	 * 检查redis sentinel格式
	 * 
	 * @param nodeInfoList
	 * @return
	 */
	private Result checkSentinelAppDeploy(String[] nodeInfoList) {
		int redisLineNum = 0;
		int sentinelLineNum = 0;
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			String[] array = nodeInfo.split(ConstUtils.COLON);
			if (array.length == 3) {
				redisLineNum++;
			} else if (array.length == 1) {
				sentinelLineNum++;
			}
		}
		// redis节点只有redisLineMustNum行
		final int redisLineMustNum = 1;
		if (redisLineNum < redisLineMustNum) {
			return Result.fail("应用部署格式错误, Sentinel应用中必须有Redis数据节点!");
		} else if (redisLineNum > redisLineMustNum) {
			return Result.fail("应用部署格式错误, Sentinel应用中Redis数据节点只能有一行!");
		}

		// sentinel节点至少有sentinelLessNum个
		final int sentinelLessNum = 3;
		if (sentinelLineNum < sentinelLessNum) {
			return Result.fail("应用部署格式错误, Sentinel应用中Sentinel节点至少要有" + sentinelLessNum + "个!");
		}
		return Result.success("应用部署格式正确，可以开始部署了!");
	}

	/**
	 * 查看host是否存在
	 * 
	 * @param host
	 * @return
	 */
	private boolean checkHostExist(String host) {
		try {
			MachineInfo machineInfo = machineCenter.getMachineInfoByIp(host);
			if (machineInfo == null) {
				return false;
			}
			if (machineInfo.isOffline()) {
				logger.warn("host {} is offline", host);
				return false;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	private String checkPhysicalIpConflick(String text,String masterHost,String slaveHost){
		MachineInfo masterInfo = machineCenter.getMachineInfoByIp(masterHost);
		MachineInfo slaveInfo = machineCenter.getMachineInfoByIp(slaveHost);
		
		if (masterInfo.getVirtual() == 0 || slaveInfo.getVirtual() == 0){
			return "";
		}
		
		if (StringUtils.isEmpty(masterInfo.getRealIp())
				|| StringUtils.isEmpty(slaveInfo.getRealIp())){
				return String.format("%s  ,物理机信息 master:%s slave:%s",text,masterInfo.getRealIp(),slaveInfo.getRealIp());
		}
		
		if (masterInfo.getRealIp().equals(slaveInfo.getRealIp())){
			return String.format("%s  有共同的物理母机ip %s",text,masterInfo.getRealIp());
		}
		
		
		
		
		
		return "";
	}
	
	private boolean checkInstanceInApp(long appId,String host,String port){
		InstanceInfo info = instanceDao.getInstByIpAndPort(host, Integer.valueOf(port));
		if (info == null){
			return false;
		}
		return info.getAppId() == appId;
	}

	@Override
	public boolean allocateResourceApp(Long appAuditId, List<String> nodeInfoList, AppUser auditUser) {
		if (appAuditId == null || appAuditId <= 0L) {
			logger.error("appAuditId is null");
			return false;
		}
		if (nodeInfoList == null || nodeInfoList.isEmpty()) {
			logger.error("nodeInfoList is null");
			return false;
		}
		AppAudit appAudit = appAuditDao.getAppAudit(appAuditId);
		if (appAudit == null) {
			logger.error("appAudit:id={} is not exist", appAuditId);
			return false;
		}
		long appId = appAudit.getAppId();
		AppDesc appDesc = appService.getByAppId(appId);
		if (appDesc == null) {
			logger.error("appDesc:id={} is not exist");
			return false;
		}
		int type = appDesc.getType();
		int templateId = appDesc.getTemplateId();
		List<String[]> nodes = new ArrayList<String[]>();
		for (String nodeInfo : nodeInfoList) {
			nodeInfo = StringUtils.trim(nodeInfo);
			if (StringUtils.isBlank(nodeInfo)) {
				continue;
			}
			String[] array = nodeInfo.split(":");
			// if (array.length < 2) {
			// logger.error("error nodeInfo:{}", Arrays.toString(array));
			// continue;
			// }
			nodes.add(array);
		}

		boolean isAudited = false;
		if (TypeUtil.isRedisType(type)) {
			if (TypeUtil.isRedisCluster(type)) {
				isAudited = deployCluster(appId, nodes, templateId);
			} else if (nodes.size() > 0) {
				if (TypeUtil.isRedisSentinel(type)) {
					isAudited = deploySentinel(appId, nodes, templateId);
				} else {
					isAudited = deployStandalone(appId, nodes, templateId);
				}
			} else {
				logger.error("nodeInfoList={} is error");
			}
		} else {
			logger.error("unknown type : {}", type);
			return false;
		}

		// 审核通过
		if (isAudited) {
			// 改变审核状态
			appAuditDao.updateAppAudit(appAudit.getId(), AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
		}

		return true;
	}

	@Override
	public boolean offLineApp(Long appId) {
		Assert.isTrue(appId != null && appId > 0L);
		AppDesc appDesc = appService.getByAppId(appId);
		if (appDesc == null) {
			logger.error("appId={} not exist");
			return false;
		}
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);

		// 机器上运行着自动拉起redis的程序，要下线应用，首先需要通知拉起程序对应的redis已经下线。
		Set<String> ipSet = new HashSet<String>();
		for (InstanceInfo instanceInfo : instanceInfos) {
			ipSet.add(instanceInfo.getIp());
		}

		int type = appDesc.getType();
		if (instanceInfos != null) {
			for (InstanceInfo instanceInfo : instanceInfos) {
				final String ip = instanceInfo.getIp();
				final int port = instanceInfo.getPort();
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
				ipSet.add(instanceInfo.getIp());
				
				if (TypeUtil.isRedisType(type)) {
					// 取消收集
					boolean isShutdown = redisCenter.shutdown(ip, port);
					if (!isShutdown) {
						logger.error("{}:{} redis not shutdown!", ip, port);
						return false;
					}
				}
			}
		}
		
		
		// 更新应用信息
		appDesc.setStatus(AppStatusEnum.STATUS_OFFLINE.getStatus());
		appService.update(appDesc);
		
		redisDeployCenter.deleteToUnioncache(appId);
		return true;
	}

	@Override
	public boolean modifyAppConfig(Long appId, Long appAuditId, String key, String value) {
		Assert.isTrue(appId != null && appId > 0L);
		Assert.isTrue(appAuditId != null && appAuditId > 0L);
		Assert.isTrue(StringUtils.isNotBlank(key));
		Assert.isTrue(StringUtils.isNotBlank(value));
		boolean isModify = redisDeployCenter.modifyAppConfig(appId, key, value);
		if (isModify) {
			// 改变审核状态
			appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
		}
		return isModify;
	}

	private boolean deploySentinel(long appId, List<String[]> nodes, int templateId) {
		// 数据节点
		String[] dataNodeInfo = nodes.get(0);
		String master = dataNodeInfo[0];
		int memory = NumberUtils.createInteger(dataNodeInfo[1]);
		String slave = dataNodeInfo[2];
		// sentinel节点
		List<String> sentinelList = new ArrayList<String>();
		if (nodes.size() < 2) {
			logger.error("sentinelList is none,don't generate sentinel app!");
			return false;
		}

		// sentinel节点
		for (int i = 1; i < nodes.size(); i++) {
			String[] nodeInfo = nodes.get(i);
			if (nodeInfo.length == 0 || StringUtils.isBlank(nodeInfo[0])) {
				logger.error("sentinel line {} may be empty", i);
				return false;
			}
			sentinelList.add(nodeInfo[0]);
		}

		return redisDeployCenter.deploySentinelInstance(appId, master, slave, memory, sentinelList, templateId);
	}

	private boolean deployCluster(long appId, List<String[]> nodes, int templateId) {
		List<RedisClusterNode> clusterNodes = new ArrayList<RedisClusterNode>();
		int maxMemory = 0;
		for (String[] array : nodes) {
			String master = array[0];
			int memory = NumberUtils.createInteger(array[1]);
			String slave = null;
			if (array.length > 2) {
				slave = array[2];
			}
			RedisClusterNode node = new RedisClusterNode(master, slave);
			maxMemory = memory;
			clusterNodes.add(node);
		}
		return redisDeployCenter.deployClusterInstance(appId, clusterNodes, maxMemory, templateId);
	}

	private boolean deployStandalone(long appId, List<String[]> nodes, int templateId) {
		return redisDeployCenter.deployStandaloneInstance(appId, nodes, templateId);
	}

	@Override
	public boolean verticalExpansion(Long appId, Long appAuditId, final int memory) {
		Assert.isTrue(appId != null && appId > 0L);
		Assert.isTrue(appAuditId != null && appAuditId > 0L);
		Assert.isTrue(memory > 0);
		/*
		 * boolean isInProcess = isInProcess(appId); if (isInProcess) { return
		 * false; }
		 */
		AppDesc appDesc = appService.getByAppId(appId);
		Assert.isTrue(appDesc != null);
		int type = appDesc.getType();
		if (!TypeUtil.isRedisType(type)) {
			logger.error("appId={};type={} is not redis!", appDesc, type);
			return false;
		}
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		if (instanceInfos == null || instanceInfos.isEmpty()) {
			logger.error("instanceInfos is null");
			return false;
		}
		for (InstanceInfo instanceInfo : instanceInfos) {
			int instanceType = instanceInfo.getType();
			if (TypeUtil.isRedisSentinel(instanceType)) {
				continue;
			}
			// 下线实例不做操作
			if (instanceInfo.isOffline()) {
				continue;
			}
			String host = instanceInfo.getIp();
			int port = instanceInfo.getPort();

			final long maxMemoryBytes = Long.valueOf(memory) * 1024 * 1024;
			boolean isConfig = redisDeployCenter.modifyInstanceConfig(host, port, "maxmemory",
					String.valueOf(maxMemoryBytes));
			if (!isConfig) {
				logger.error("{}:{} set maxMemory error", host, port);
				return false;
			}
			// 更新instanceInfo配置
			instanceInfo.setMem(memory);
			instanceDao.update(instanceInfo);
		}
		// 改变审核状态
		appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
		return true;
	}

	@Override
	public boolean addAppClusterSharding(Long appId, String masterHost, String slaveHost, int memory) {
		Assert.isTrue(appId != null && appId > 0L);
		Assert.isTrue(StringUtils.isNotBlank(masterHost));
		Assert.isTrue(memory > 0);
		AppDesc appDesc = appService.getByAppId(appId);
		Assert.isTrue(appDesc != null);
		int type = appDesc.getType();
		if (!TypeUtil.isRedisCluster(type)) {
			logger.error("appId={};type={} is not redis cluster!", appDesc, type);
			return false;
		}
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		if (instanceInfos == null || instanceInfos.isEmpty()) {
			logger.error("app:{} instanceInfos isEmpty", appId);
			return false;
		}
		Integer masterPort = machineCenter.getAvailablePort(masterHost, ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
		if (masterPort == null) {
			logger.error("host={} getAvailablePort is null", masterHost);
			return false;
		}
		Integer slavePort = 0;
		boolean hasSlave = StringUtils.isNotBlank(slaveHost);
		if (hasSlave) {
			slavePort = machineCenter.getAvailablePort(slaveHost, ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
			if (slavePort == null) {
				logger.error("host={} getAvailablePort is null", slaveHost);
				return false;
			}
		}

		// 运行节点
		boolean isMasterCreate = redisDeployCenter.createRunNode(masterHost, masterPort, memory, true,
				appDesc.getTemplateId());
		if (!isMasterCreate) {
			logger.error("createRunNode master failed {}:{}", masterHost, masterPort);
			return false;
		}
		if (hasSlave) {
			// 运行节点
			boolean isSlaveCreate = redisDeployCenter.createRunNode(slaveHost, slavePort, memory, true,
					appDesc.getTemplateId());
			if (!isSlaveCreate) {
				logger.error("createRunNode slave failed {}:{}", slaveHost, slavePort);
				return false;
			}
		}
		Set<HostAndPort> clusterHosts = new LinkedHashSet<HostAndPort>();
		for (InstanceInfo instance : instanceInfos) {
			clusterHosts.add(new HostAndPort(instance.getIp(), instance.getPort()));
		}
		clusterHosts.add(new HostAndPort(masterHost, masterPort));
		if (hasSlave) {
			clusterHosts.add(new HostAndPort(slaveHost, slavePort));
		}
		RedisClusterReshard clusterReshard = new RedisClusterReshard(clusterHosts);
		boolean joinCluster = clusterReshard.joinCluster(masterHost, masterPort, slaveHost, slavePort);
		if (joinCluster) {
			// 保存实例
			redisDeployCenter.saveInstance(appId, masterHost, masterPort, memory, type, "");
			redisDeployCenter.updateToUnioncache(appId);
			redisCenter.deployRedisCollection(appId, masterHost, masterPort);
			if (hasSlave) {
				redisDeployCenter.saveInstance(appId, slaveHost, slavePort, memory,type,"");
				redisCenter.deployRedisCollection(appId, slaveHost, slavePort);
			}			
			
		}
		
		
		return joinCluster;
	}

	@Override
	public boolean offLineClusterNode(final Long appId, final String host, final int port) {
		Assert.isTrue(appId != null && appId > 0L);
		Assert.isTrue(StringUtils.isNotBlank(host));
		Assert.isTrue(port > 0);
		AppDesc appDesc = appService.getByAppId(appId);
		Assert.isTrue(appDesc != null);
		int type = appDesc.getType();
		if (!TypeUtil.isRedisCluster(type)) {
			logger.error("appId={};type={} is not redis cluster!", appDesc, type);
			return false;
		}
		boolean isInProcess = isInProcess(appId,host + ":" + port);
		if (isInProcess) {
			return false;
		}
		final List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		if (instanceInfos == null || instanceInfos.isEmpty()) {
			logger.error("app:{} instanceInfos isEmpty", appId);
			return false;
		}
		
		final Map<String,ReshardProcess> processMap = getProcessMap(appId);
		processThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				Set<HostAndPort> clusterHosts = new LinkedHashSet<HostAndPort>();
				for (InstanceInfo instance : instanceInfos) {
					clusterHosts.add(new HostAndPort(instance.getIp(), instance.getPort()));
				}
				RedisClusterReshard clusterReshard = new RedisClusterReshard(clusterHosts);
				// 添加进度
				processMap.put((host + ":" + port), clusterReshard.getReshardProcess());

				boolean joinCluster = false;
				synchronized (reshardLock) {
					joinCluster = clusterReshard.offLineMaster(host, port);
				}
				if (joinCluster) {
					InstanceInfo instanceInfo = instanceDao.getLiveInstByIpAndPort(host, port);
					if (instanceInfo != null) {
						// 更新实例下线
						instanceInfo.setStatus(InstanceStatusEnum.OFFLINE_STATUS.getStatus());
						instanceDao.update(instanceInfo);
					}
				}
				logger.warn("async:appId={} joinCluster={} done result={}", appId, joinCluster,
						clusterReshard.getReshardProcess());
			}
		});

		redisDeployCenter.updateToUnioncache(appId);
		return false;
	}
	
	private Map<String,ReshardProcess> getProcessMap(Long appId){
		Map<String,ReshardProcess> processMap = appIdProcessMap.get(appId);
		if (processMap == null){
			processMap = new ConcurrentSkipListMap<String,ReshardProcess>();
			appIdProcessMap.put(appId, processMap);
		}
		return processMap;		
	}

	@Override
	public boolean cleanAppData(long appId, AppUser appUser, JSONObject errMsg) {
		try {
			AppDesc appDesc = appDao.getAppDescById(appId);
			if (appDesc == null) {
				return false;
			}
			if (TypeUtil.isRedisType(appDesc.getType())) {
				return redisCenter.cleanAppData(appDesc, appUser, errMsg);
			} else {
				return false;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}
	
	@Override
	public Set<HostAndPort> getHosts(Long appId)
	{
		final List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
		if (instanceInfos == null || instanceInfos.isEmpty()) {
			logger.error("app:{} instanceInfos isEmpty", appId);
			return null;
		}
		
		Set<HostAndPort> clusterHosts = new LinkedHashSet<HostAndPort>();
		for (InstanceInfo instance : instanceInfos) {
			clusterHosts.add(new HostAndPort(instance.getIp(), instance.getPort()));
		}
		
		return clusterHosts;
	}

	@Override
	public boolean horizontalExpansion(final Long appId, final String host, final int port, final Long appAuditId,final Set<HostAndPort> clusterHosts) {
		Assert.isTrue(appId != null && appId > 0L);
		Assert.isTrue(StringUtils.isNotBlank(host));
		Assert.isTrue(port > 0);
		boolean isInProcess = isInProcess(appId,host + ":" + port);
		if (isInProcess) {
			logger.warn("ip={};port={}; is in migrating process already !", host,port);
			return false;
		}
		
		AppDesc appDesc = appService.getByAppId(appId);
		Assert.isTrue(appDesc != null);
		int type = appDesc.getType();
		if (!TypeUtil.isRedisCluster(type)) {
			logger.error("appId={};type={} is not redis cluster!", appDesc, type);
			return false;
		}
		
		final Map<String,ReshardProcess> processMap = getProcessMap(appId);
		processThreadPool.execute(new Runnable() {
			@Override
			public void run() {

				RedisClusterReshard clusterReshard = new RedisClusterReshard(clusterHosts);
				// 添加进度
				processMap.put(host + ":" + port, clusterReshard.getReshardProcess());

				boolean joinCluster = false;
				synchronized (reshardLock) {
					joinCluster = clusterReshard.joinNewMaster(host, port);
				}

				logger.warn("async:appId={} joinCluster={} done result={}", appId, joinCluster,
						clusterReshard.getReshardProcess());
				if (joinCluster) {
					InstanceInfo instanceInfo = instanceDao.getAllInstByIpAndPort(host, port);
					if (instanceInfo != null
							&& instanceInfo.getStatus() != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
						instanceInfo.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
						instanceDao.update(instanceInfo);
					}
				}
				
				//每个reshard进程执行完成后都检查，所有reshard过程都成功才更新审核状态
				if (checkReshardOK(appId)) {				
					appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
				}
			}
		});
		
		logger.warn("reshard appId={} instance={}:{} deploy done", appId, host, port);
		return true;
	}
	
	@Override
	public boolean nodeMigrate(final Long appId,final Long appAuditId,String migrationInfo){
		Assert.isTrue(appId != null && appId > 0L);
		
		AppDesc appDesc = appService.getByAppId(appId);
		Assert.isTrue(appDesc != null);
		int type = appDesc.getType();
		if (!TypeUtil.isRedisCluster(type)) {
			logger.error("appId={};type={} is not redis cluster!", appDesc, type);
			return false;
		}
		
		try{
			String[] migrateItems = migrationInfo.split(ConstUtils.NEXT_LINE);
			final Set<HostAndPort> hosts = getHosts(appId);
			for (String migrateItem : migrateItems){
				migrateItem = migrateItem.trim();
				final HostAndPort srcNode = getNodeInfo(migrateItem.split(",")[0]);
				boolean isInProcess = isInProcess(appId,srcNode.toString());
				if (isInProcess) {
					logger.warn("{} is in migrating process already!", srcNode.toString());
					continue;
				}
				
				final HostAndPort dstNode = getNodeInfo(migrateItem.split(",")[1]);
				final int threadNum = Integer.parseInt(migrateItem.split(",")[2]);
				
				final Map<String,ReshardProcess> processMap = getProcessMap(appId);
				processThreadPool.execute(new Runnable() {
					@Override
					public void run() {

						RedisClusterReshard clusterReshard = new RedisClusterReshard(hosts);
						// 添加进度
						processMap.put(srcNode.toString(), clusterReshard.getReshardProcess());

						boolean migrateSuccess = false;
						
						try{
							synchronized (reshardLock) {
								migrateSuccess = clusterReshard.migrateNode(srcNode, dstNode, threadNum);
							}
						}catch(Exception e){
							clusterReshard.getReshardProcess().setStatus(2);
							logger.error("",e);
						}

						logger.warn("async:appId={} {}->{} migrate done result={} detail={}", appId,srcNode,dstNode,migrateSuccess,
								clusterReshard.getReshardProcess());
						
						//每个reshard进程执行完成后都检查，所有reshard过程都成功才更新审核状态
						if (checkReshardOK(appId)) {
							appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
						}
					}
				});
				
			}
		}catch(Exception e){
			logger.error("",e);
			return false;
		}
		
		return true;
	}
	
	@Override
	public int finishSlotMigrate(final Long appId,final Long appAuditId,final int threadNum){
		Assert.isTrue(appId != null && appId > 0L);
		final Map<String,ReshardProcess> processMap = appIdProcessMap.get(appId);
		if (isInProcess(appId)){
			logger.warn("appId {} is in process, no need to finish left slot migration.",appId);
			return -1;
		}
		
		final Set<HostAndPort> hosts = getHosts(appId);
		final RedisClusterReshard clusterReshard = new RedisClusterReshard(hosts);
		final List<ClusterNodeInformation> masterNodes = RedisClusterReshard.getMasterNodes(hosts);
		processThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				
				// 添加进度
				processMap.put("unFinishedMigratingSlots", clusterReshard.getReshardProcess());

				boolean migrateSuccess = false;
				
				try{
					synchronized (reshardLock) {
						migrateSuccess = clusterReshard.checkAndMovingSlot(threadNum,masterNodes);
						clusterReshard.getReshardResult();
					}					
				}catch(Exception e){
					clusterReshard.getReshardProcess().setStatus(2);
					logger.error("",e);
				}

				logger.warn("async:appId={} migrate left slot result={} detail={}", appId,migrateSuccess,
						clusterReshard.getReshardProcess());
				
				//每个reshard进程执行完成后都检查，所有reshard过程都成功才更新审核状态
				/*if (migrateSuccess) {
					appAuditDao.updateAppAudit(appAuditId, AppCheckEnum.APP_ALLOCATE_RESOURCE.value());
				}*/
			}
		});
		
		return 0;
	}
	
	private HostAndPort getNodeInfo(String hostportStr){
		String srcIp = hostportStr.split(":")[0];
		int srcPort = Integer.parseInt(hostportStr.split(":")[1]);
		return new HostAndPort(srcIp,srcPort);
	}
	

	public boolean checkReshardOK(Long appId) {
		Map<String,ReshardProcess> processMap = getProcessMap(appId);
		for (ReshardProcess reshard : processMap.values()) {
			if (reshard.getStatus() != 1) {
				return false;
			}
		}
		return true;
	}

	private boolean isInProcess(Long appId,String hostPort) {
		Map<String,ReshardProcess> processMap = getProcessMap(appId);
		ReshardProcess process = processMap.get(hostPort);
		if (process != null && process.getStatus() == 0) {
			logger.warn("appId={} isInProcess", hostPort, process.getStatus());
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isInProcess(Long appId) {
		Map<String,ReshardProcess> processMap = getProcessMap(appId);
		for (ReshardProcess process : processMap.values()){
			if (process.getStatus() == 0) {
				logger.warn("appId={} isInProcess", appId);
				return true;
			} 
		}
		return false;
	}

	@Override
	public Map<String, ReshardProcess> getHorizontalProcess(Long appId) {
		return getProcessMap(appId);
	}

	private InstanceInfo saveInstance(long appId, String host, int port, int maxMemory) {
		InstanceInfo instanceInfo = new InstanceInfo();
		instanceInfo.setAppId(appId);
		MachineInfo machineInfo = machineCenter.getMachineInfoByIp(host);
		instanceInfo.setHostId(machineInfo.getId());
		instanceInfo.setConn(0);
		instanceInfo.setMem(maxMemory);
		instanceInfo.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
		instanceInfo.setPort(port);
		instanceInfo.setType(ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
		instanceInfo.setCmd("");
		instanceInfo.setIp(host);
		instanceDao.saveInstance(instanceInfo);
		return instanceInfo;
	}

	@Override
	public void updateAuditType(long appAuditId, int redisType) {
		appService.updateAuditType(appAuditId, redisType);
	}

	@Override
	public void updateAppType(long appId, int redisType) {
		appDao.updateAppType(appId, redisType);
	}

	@Override
	public void updateAppTemplateId(long appId, int templateId) {
		appDao.updateAppTemplateId(appId, templateId);
	}

	public void setAppService(AppService appService) {
		this.appService = appService;
	}

	public void setRedisDeployCenter(RedisDeployCenter redisDeployCenter) {
		this.redisDeployCenter = redisDeployCenter;
	}

	public void setAppEmailUtil(AppEmailUtil appEmailUtil) {
		this.appEmailUtil = appEmailUtil;
	}

	public void setAppAuditDao(AppAuditDao appAuditDao) {
		this.appAuditDao = appAuditDao;
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

	public void setAppAuditLogDao(AppAuditLogDao appAuditLogDao) {
		this.appAuditLogDao = appAuditLogDao;
	}

	public void setAppDao(AppDao appDao) {
		this.appDao = appDao;
	}

}