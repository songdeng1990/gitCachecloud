package com.sohu.cache.inspect.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.sohu.cache.alert.impl.BaseAlertService;
import com.sohu.cache.constant.InstanceStatusEnum;
import com.sohu.cache.dao.MachineStatsDao;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.exception.SSHException;
import com.sohu.cache.inspect.InspectParamEnum;
import com.sohu.cache.inspect.Inspector;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.machine.MachineDeployCenter;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.IdempotentConfirmer;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.service.AppService;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Created by yijunzhang on 15-1-30.
 */
public class RedisIsolationPersistenceInspector extends BaseAlertService implements Inspector {

	/**
	 * 应用相关dao
	 */
	private AppService appService;
	private RedisCenter redisCenter;
	public static final int REDIS_DEFAULT_TIME = 5000;
	public static Map<String, Integer> aofRewriteCounterMap = new ConcurrentHashMap<String, Integer>();

	@Override
	public boolean inspect(Map<InspectParamEnum, Object> paramMap) {

		final String host = MapUtils.getString(paramMap, InspectParamEnum.SPLIT_KEY);

		
		boolean getHostLock = false;
		synchronized(String.valueOf(host)){
			Integer counter = aofRewriteCounterMap.get(host);
			if (counter != null && counter >= 1) {
				aofRewriteCounterMap.put(host, 0);
				getHostLock = true;
			}
		}
		if (getHostLock) {
			aofRewriteAlways(paramMap);
			if (SSHUtil.isRemouteFileExist(host, ConstUtils.BAKCUP_DIR)) {
				logger.info(host + " redis  backup started.");
				redisbackup(paramMap);
			}
			
		} /*else if (reachDiskThreshhold(host)) {
			aofRewriteByincrease(paramMap);
		}*/
		else{
			aofRewriteByincrease(paramMap);
		}
		
		return true;
	}

	private boolean reachDiskThreshhold(String ip) {
		if (!SSHUtil.isRemouteFileExist(ip, ConstUtils.CACHECLOUD_BASE_DIR + "/bin")) {
			return false;
		}
		int ratio = SSHUtil.getPathDiskUsage(ip, ConstUtils.CACHECLOUD_BASE_DIR + "/data");

		return ratio >= ConstUtils.AOF_DISKUSAGE_THRESHOLD;
	}

	/**
	 * 直接执行aofrewrite操作
	 * 
	 * @param paramMap
	 */
	private void aofRewriteAlways(Map<InspectParamEnum, Object> paramMap) {

		final String host = MapUtils.getString(paramMap, InspectParamEnum.SPLIT_KEY);
		logger.warn("ip {} aof_rewrite started." , host);
		
	

		/**
		 * 非cachecloud管理的redis不做aof重写
		 */
		if (!SSHUtil.isRemouteFileExist(host, ConstUtils.CACHECLOUD_BASE_DIR + "/bin")) {
			return;
		}

		List<InstanceInfo> list = (List<InstanceInfo>) paramMap.get(InspectParamEnum.INSTANCE_LIST);
		for (InstanceInfo info : list) {
			final int port = info.getPort();
			final int type = info.getType();
			int status = info.getStatus();
			// 非正常节点
			if (status != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
				continue;
			}
			if (TypeUtil.isRedisDataType(type)) {
				Jedis jedis = redisCenter.getAuthJedis(host, port);
				try {
					Map<String, String> persistenceMap = parseMap(jedis);
					if (persistenceMap.isEmpty()) {
						logger.error("{}:{} get persistenceMap failed", host, port);
						continue;
					}

					if (isAofEnabled(persistenceMap)) {
						// rdbsave(persistenceMap,jedis);
						bgrewriteaof(jedis);
					}

				} finally {
					jedis.close();
				}
			}
		}
	}

	private void redisbackup(Map<InspectParamEnum, Object> paramMap) {

		/**
		 * ConstUtils.DEFALT_BAKCUP_DAYS 小于0代表全局禁用backup功能。
		 */
		if (ConstUtils.DEFALT_BAKCUP_DAYS <= 0) {
			return;
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = format.format(new Date());

		final String host = MapUtils.getString(paramMap, InspectParamEnum.SPLIT_KEY);

		List<InstanceInfo> list = (List<InstanceInfo>) paramMap.get(InspectParamEnum.INSTANCE_LIST);
		for (InstanceInfo info : list) {
			final int port = info.getPort();
			final int type = info.getType();
			final long appId = info.getAppId();
			int backupDays = appService.getByAppId(appId).getBackupDays();

			if (backupDays <= 0) {
				continue;
			}

			// 非正常节点
			if (!RedisUtil.isRun(host, port, null)) {
				continue;
			}

			if (TypeUtil.isRedisDataType(type)) {

				Jedis jedis = redisCenter.getAuthJedis(host, port);

				// only master need backup.
				if (StringUtils.isNotEmpty(jedis.configGet("slaveof").get(1))) {
					continue;
				}
				
				if (RedisUtil.isPersistenceDisabled(jedis)){
					continue;
				}

				try {

					rdbsave(jedis);

					createNewBackup(appId, host, port, jedis, dateString);

					delteOldBackup(appId, host, port, jedis, format);

				} catch (SSHException e) {
					// TODO Auto-generated catch block
					logger.error("", e);
				} finally {
					jedis.close();
				}
			}
		}

	}

	private void delteOldBackup(long appId, String host, int port, Jedis jedis, SimpleDateFormat format)
			throws SSHException {

		String backupdir = ConstUtils.BAKCUP_DIR + "/" + appId;
		String rs = SSHUtil.execute(host, "ls -t " + backupdir + " | wc -l");
		int fileNum = Integer.valueOf(rs);
		AppDesc appDesc = appService.getByAppId(appId);
		/*
		 * while (fileNum > appDesc.getBackupDays()) {
		 * 
		 * String dirNameToDelete = SSHUtil.execute(host, "ls -t " + backupdir +
		 * " | tail -1"); String fullPath = backupdir + "/" + dirNameToDelete;
		 * 
		 * if (SSHUtil.isRemouteFileExist(host, fullPath)) {
		 * SSHUtil.execute(host, "rm -rf " + fullPath); }
		 * 
		 * rs = SSHUtil.execute(host, "ls -t " + backupdir + " | wc -l");
		 * fileNum = Integer.valueOf(rs); }
		 */

		if (fileNum > appDesc.getBackupDays()) {
			Calendar ca = Calendar.getInstance();
			ca.setTime(new Date());
			ca.add(Calendar.DAY_OF_MONTH, -appDesc.getBackupDays());
			String date = format.format(ca.getTime());
			String fullPath = backupdir + "/" + date;

			if (SSHUtil.isRemouteFileExist(host, fullPath)) {
				logger.warn("delete old backup : " + fullPath);
				SSHUtil.execute(host, "rm -rf " + fullPath);
			}
		}

	}

	private void createNewBackup(long appId, String host, int port, Jedis jedis, String dateString)
			throws SSHException {
		String backupdir = ConstUtils.BAKCUP_DIR + "/" + appId + "/" + dateString;
		SSHUtil.createRemoteDir(host, backupdir);
		String dbFileName = jedis.configGet("dbfilename").get(1);
		String srcDumpFilePath = jedis.configGet("dir").get(1) + "/" + dbFileName;

		String dstDumpFilePath = backupdir + "/" + host + "-" + port + "-" + dbFileName;
		String cmd = "rsync -av --bwlimit=" + ConstUtils.COPY_SPEED_LIMIT + " " + srcDumpFilePath + " "
				+ dstDumpFilePath;
		logger.warn(cmd);
		SSHUtil.execute(host, cmd);
	}

	/**
	 * 只有当aof文件的增长量达到预先设定的阈值时，才执行aof重写。
	 * 
	 * @param paramMap
	 */
	private void aofRewriteByincrease(Map<InspectParamEnum, Object> paramMap) {
		final String host = MapUtils.getString(paramMap, InspectParamEnum.SPLIT_KEY);
		List<InstanceInfo> list = (List<InstanceInfo>) paramMap.get(InspectParamEnum.INSTANCE_LIST);
		for (InstanceInfo info : list) {
			final int port = info.getPort();
			final int type = info.getType();
			int status = info.getStatus();
			// 非正常节点
			if (status != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
				continue;
			}
			if (TypeUtil.isRedisDataType(type)) {
				Jedis jedis = redisCenter.getAuthJedis(host, port);
				try {
					Map<String, String> persistenceMap = parseMap(jedis);
					if (persistenceMap.isEmpty()) {
						logger.error("{}:{} get persistenceMap failed", host, port);
						continue;
					}
					if (!isAofEnabled(persistenceMap)) {
						// rdbsave(persistenceMap,jedis);
						continue;
					}
					long aofCurrentSize = MapUtils.getLongValue(persistenceMap, "aof_current_size");
					long aofBaseSize = MapUtils.getLongValue(persistenceMap, "aof_base_size");
					// 阀值大于60%
					long aofThresholdSize = (long) (aofBaseSize * 1.6);
					double percentage = getPercentage(aofCurrentSize, aofBaseSize);

					if (aofCurrentSize >= aofThresholdSize
							// 大于64Mb
							&& aofCurrentSize > (64 * 1024 * 1024)) {
						bgrewriteaof(jedis);
					} else {
						if (percentage > 50D) {
							long currentSize = getMb(aofCurrentSize);
							logger.info("checked {}:{} aof increase percentage:{}% currentSize:{}Mb", host, port,
									percentage, currentSize > 0 ? currentSize : "<1");
						}
					}
				} finally {
					jedis.close();
				}
			}
		}
	}

	private void waitForConsistencyFinish(Jedis jedis){
		String host = jedis.getClient().getHost();
		int port = jedis.getClient().getPort();
		while (true) {
			try {
				Map<String, String> loopMap = parseMap(jedis);
				Integer bgsaveInProgress = MapUtils.getInteger(loopMap, "rdb_bgsave_in_progress", null);
				Integer aof_rewrite_in_progress = MapUtils.getInteger(loopMap, "aof_rewrite_in_progress", null);

				if (bgsaveInProgress == null || aof_rewrite_in_progress == null) {
					logger.error("loop watch:{}:{} return info command failed", host, port);
					break;
				} else if (bgsaveInProgress <= 0 && aof_rewrite_in_progress<=0) {
					// bgrewriteaof Done
					logger.info("{}:{} bgsaveInProgress Done", host, port);
					break;
				} else {
					// wait 1s
					TimeUnit.SECONDS.sleep(1);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				break;
			}
		}
	}
	private void bgrewriteaof(Jedis jedis) {
		String host = jedis.getClient().getHost();
		int port = jedis.getClient().getPort();
		waitForConsistencyFinish(jedis);
		// bgRewriteAof
		boolean isInvoke = invokeBgRewriteAof(jedis);
		if (!isInvoke) {
			logger.error("{}:{} invokeBgRewriteAof failed", host, port);
			return;
		} else {
			logger.info("{}:{} invokeBgRewriteAof started percentage={}", host, port);
		}
		waitForConsistencyFinish(jedis);
	}

	private void rdbsave(Jedis jedis) {	
		String host = jedis.getClient().getHost();
		int port = jedis.getClient().getPort();

		waitForConsistencyFinish(jedis);
		boolean isInvoke = invokeBgsave(jedis);
		if (!isInvoke) {
			logger.error("{}:{} invokeBgsave failed", host, port);
			return;
		} else {
			logger.info("{}:{} invokeBgsave started ", host, port);
		}
		waitForConsistencyFinish(jedis);
	}

	private long getMb(long bytes) {
		return (long) (bytes / 1024 / 1024);
	}

	private boolean isAofEnabled(Map<String, String> infoMap) {
		Integer aofEnabled = MapUtils.getInteger(infoMap, "aof_enabled", null);
		return aofEnabled != null && aofEnabled == 1;
	}

	private double getPercentage(long aofCurrentSize, long aofBaseSize) {
		if (aofBaseSize == 0) {
			return 0.0D;
		}
		String format = String.format("%.2f", (Double.valueOf(aofCurrentSize - aofBaseSize) * 100 / aofBaseSize));
		return Double.parseDouble(format);
	}

	private Map<String, String> parseMap(final Jedis jedis) {
		final StringBuilder builder = new StringBuilder();
		boolean isInfo = new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String persistenceInfo = null;
				try {
					persistenceInfo = jedis.info("Persistence");
				} catch (Exception e) {
					logger.warn(e.getMessage() + "-{}:{}", jedis.getClient().getHost(), jedis.getClient().getPort(),
							e.getMessage());
				}
				boolean isOk = StringUtils.isNotBlank(persistenceInfo);
				if (isOk) {
					builder.append(persistenceInfo);
				}
				return isOk;
			}
		}.run();
		if (!isInfo) {
			logger.error("{}:{} info Persistence failed", jedis.getClient().getHost(), jedis.getClient().getPort());
			return Collections.emptyMap();
		}
		String persistenceInfo = builder.toString();
		if (StringUtils.isBlank(persistenceInfo)) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new LinkedHashMap<String, String>();
		String[] array = persistenceInfo.split("\r\n");
		for (String line : array) {
			String[] cells = line.split(":");
			if (cells.length > 1) {
				map.put(cells[0], cells[1]);
			}
		}

		return map;
	}

	public boolean invokeBgRewriteAof(final Jedis jedis) {
		return new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				try {
					String response = jedis.bgrewriteaof();
					if (response != null && response.contains("rewriting started")) {
						return true;
					}else{
						logger.error(jedis.getClient().getHostPort() + "invoke bgRewriteaof failed." + response);
					}
				} catch (Exception e) {
					String message = e.getMessage();
					if (message.contains("rewriting already")) {
						return true;
					}
					logger.error(message, e);
				}
				return false;
			}
		}.run();
	}

	public boolean invokeBgsave(final Jedis jedis) {
		return new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				try {
					String response = jedis.bgsave();
					if (response != null && response.contains("save started")) {
						return true;
					}
				} catch (Exception e) {
					String message = e.getMessage();
					if (message.contains("save already")) {
						return true;
					}
					logger.error(message, e);
				}
				return false;
			}
		}.run();
	}

	public AppService getAppService() {
		return appService;
	}

	public void setAppService(AppService appService) {
		this.appService = appService;
	}

	public RedisCenter getRedisCenter() {
		return redisCenter;
	}

	public void setRedisCenter(RedisCenter redisCenter) {
		this.redisCenter = redisCenter;
	}

}
