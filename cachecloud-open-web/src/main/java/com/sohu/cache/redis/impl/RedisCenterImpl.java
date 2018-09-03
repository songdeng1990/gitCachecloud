
package com.sohu.cache.redis.impl;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sohu.cache.alert.impl.BaseAlertService;
import com.sohu.cache.async.AsyncService;
import com.sohu.cache.async.AsyncThreadPoolFactory;
import com.sohu.cache.async.KeyCallable;
import com.sohu.cache.async.NamedThreadFactory;
import com.sohu.cache.constant.AppAuditLogTypeEnum;
import com.sohu.cache.constant.AppDescEnum;
import com.sohu.cache.constant.InstanceStatusEnum;
import com.sohu.cache.constant.RWSpliter;
import com.sohu.cache.constant.RedisAccumulation;
import com.sohu.cache.constant.RedisConstant;
import com.sohu.cache.constant.RedisExcludeCommand;
import com.sohu.cache.dao.AppAuditLogDao;
import com.sohu.cache.dao.AppDao;
import com.sohu.cache.dao.AppStatsDao;
import com.sohu.cache.dao.GroupDao;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.dao.InstanceFaultDao;
import com.sohu.cache.dao.InstanceSlowLogDao;
import com.sohu.cache.dao.InstanceStatsDao;
import com.sohu.cache.entity.AppAuditLog;
import com.sohu.cache.entity.AppCommandStats;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppSearch;
import com.sohu.cache.entity.AppStats;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.DbOperate;
import com.sohu.cache.entity.GroupCommandStats;
import com.sohu.cache.entity.GroupStats;
import com.sohu.cache.entity.InstanceFault;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.InstanceSlowLog;
import com.sohu.cache.entity.InstanceStats;
import com.sohu.cache.entity.QueneSizeStatus;
import com.sohu.cache.entity.StandardStats;
import com.sohu.cache.entity.StandardStatsParam;
import com.sohu.cache.exception.SSHException;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.protocol.RedisProtocol;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.redis.enums.RedisReadOnlyCommandEnum;
import com.sohu.cache.schedule.SchedulerCenter;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.stats.instance.InstanceStatsCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.IdempotentConfirmer;
import com.sohu.cache.util.ObjectConvert;
import com.sohu.cache.util.RedisQueueHelper;
import com.sohu.cache.util.ScheduleUtil;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.component.MobileAlertComponent;
import com.sohu.cache.web.util.DateUtil;
import com.sohu.cache.web.vo.RedisSlowLog;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Slowlog;

/**
 * Created by yijunzhang on 14-6-10.
 */
public class RedisCenterImpl extends BaseAlertService implements RedisCenter {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final BlockingDeque<DbOperate> DB_OPERATE_DEQUE = new LinkedBlockingDeque<>(1000000);

	@Resource
	private SqlSessionFactory mysqlSessionFactory;

	public static final int REDIS_DEFAULT_TIME = 4000;

	private SchedulerCenter schedulerCenter;

	private AppStatsDao appStatsDao;

	private AsyncService asyncService;

	private InstanceDao instanceDao;

	private InstanceFaultDao instanceFaultDao;

	private InstanceStatsDao instanceStatsDao;

	private InstanceStatsCenter instanceStatsCenter;

	private MachineCenter machineCenter;

	private volatile Map<String, JedisPool> jedisPoolMap = new HashMap<String, JedisPool>();

	private final Lock lock = new ReentrantLock();

	private AppDao appDao;

	private GroupDao groupDao;

	private AppAuditLogDao appAuditLogDao;

	private RedisQueueHelper redisQueueHelper;

	private MobileAlertComponent mobileAlert;

	public static final String REDIS_SLOWLOG_POOL = "redis-slowlog-pool";

	public void init() {
		asyncService.assemblePool(getThreadPoolKey(), AsyncThreadPoolFactory.REDIS_SLOWLOG_THREAD_POOL);

		try {
			if (redisQueueHelper.isMaster()) {
				redisQueueHelper.clearLastInfoMap();
				logger.warn("This a master node of cachecloud cluster.");
				asyncService.submitFuture(new consumDbOper());
			} else {
				logger.warn("This a standby node of cachecloud cluster.");
			}
			asyncService.submitFuture(new collectQueneInfo());
		} catch (Exception e) {
			logger.error("Init cachecloud inner redis queue error.", e);
		}
	}

	private InstanceSlowLogDao instanceSlowLogDao;

	@Override
	public boolean deployRedisCollection(long appId, String host, int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put(ConstUtils.HOST_KEY, host);
		dataMap.put(ConstUtils.PORT_KEY, port);
		dataMap.put(ConstUtils.APP_KEY, appId);
		JobKey jobKey = JobKey.jobKey(ConstUtils.REDIS_JOB_NAME, ConstUtils.REDIS_JOB_GROUP);
		TriggerKey triggerKey = TriggerKey.triggerKey(ObjectConvert.linkIpAndPort(host, port), ConstUtils.REDIS_TRIGGER_GROUP + appId);
		return schedulerCenter.deployJobByCron(jobKey, triggerKey, dataMap, ScheduleUtil.getMinuteCronByAppId(appId), false);
	}

	@Override
	public boolean unDeployRedisCollection(long appId, String host, int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);
		TriggerKey triggerKey = TriggerKey.triggerKey(ObjectConvert.linkIpAndPort(host, port), ConstUtils.REDIS_TRIGGER_GROUP + appId);
		Trigger trigger = schedulerCenter.getTrigger(triggerKey);
		if (trigger == null) {
			return true;
		}
		return schedulerCenter.unscheduleJob(triggerKey);
	}

	private String buildFutureKey(long appId, long collectTime, String host, int port) {
		StringBuilder keyBuffer = new StringBuilder("redis-");
		keyBuffer.append(collectTime);
		keyBuffer.append("-");
		keyBuffer.append(appId);
		keyBuffer.append("-");
		keyBuffer.append(host + ":" + port);
		return keyBuffer.toString();
	}

	private class RedisKeyCallable extends KeyCallable<Boolean> {
		private final long appId;
		private long collectTime;
		private final String host;
		private final int port;

		private RedisKeyCallable(long appId, long collectTime, String host, int port) {
			super(buildFutureKey(appId, collectTime, host, port));
			this.appId = appId;
			this.collectTime = collectTime;
			this.host = host;
			this.port = port;
		}

		@Override
		public Boolean execute() {
			long start = System.currentTimeMillis();

			InstanceInfo instanceInfo = instanceDao.getInstByIpAndPort(host, port);

			// 不存在实例/实例异常/下线
			if (instanceInfo == null) {
				return null;
			}

			if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
				// 忽略sentinel redis实例
				return null;
			}

			Map<RedisConstant, Map<String, Object>> infoMap = getInfoStats(host, port);

			checkAndAlert(instanceInfo, MapUtils.isNotEmpty(infoMap));
			if (MapUtils.isEmpty(infoMap)) {
				logger.error("appId:{},collectTime:{},host:{},ip:{} cost={} ms redis infoMap is null", new Object[] { appId, collectTime, host, port, (System.currentTimeMillis() - start) });
				return null;
			}
			// 比对currentInfoMap和lastInfoMap,计算差值
			// long lastCollectTime =
			// ScheduleUtil.getLastCollectTime(collectTime);

			// Map<String, Object> lastInfoMap =
			// instanceStatsCenter.queryStandardInfoMap(lastCollectTime, host,
			// port, ConstUtils.REDIS);
			Map<String, Object> lastInfoMap = redisQueueHelper.getLastInfoMap(host, port);
			if (lastInfoMap == null || lastInfoMap.isEmpty()) {
				logger.warn("[redis-lastInfoMap] : appId={} host:port = {}:{} is null", appId, host, port);
			}

			// 基本统计累加差值
			Table<RedisConstant, String, Long> baseDiffTable = getAccumulationDiff(infoMap, lastInfoMap);
			fillAccumulationMap(infoMap, baseDiffTable);

			// 命令累加差值
			Table<RedisConstant, String, Long> commandDiffTable = getCommandsDiff(infoMap, lastInfoMap);
			fillAccumulationMap(infoMap, commandDiffTable);

			Map<String, Object> currentInfoMap = new LinkedHashMap<String, Object>();
			for (RedisConstant constant : infoMap.keySet()) {
				currentInfoMap.put(constant.getValue(), infoMap.get(constant));
			}
			currentInfoMap.put(ConstUtils.COLLECT_TIME, collectTime);
			redisQueueHelper.setLastInfoMap(host, port, currentInfoMap);
			/*
			 * instanceStatsCenter.saveStandardStats(currentInfoMap, host, port,
			 * ConstUtils.REDIS);
			 */

			/*
			 * DbOperate standardStats = new DbOperate("instanceStatsCenter",
			 * "saveStandardStats", new StandardStatsParam(currentInfoMap, host,
			 * port, ConstUtils.REDIS)); redisQueueHelper.push(standardStats);
			 */

			// 更新实例在db中的状态
			InstanceStats instanceStats = getInstanceStats(appId, host, port, infoMap);

			if (instanceStats != null) {
				instanceStatsDao.updateInstanceStats(instanceStats);
				// DbOperate dbOperate = new DbOperate("instanceStatsDao",
				// "updateInstanceStats", instanceStats);
				// redisQueueHelper.push(dbOperate);
			}

			boolean isMaster = isMaster(infoMap);

			Table<RedisConstant, String, Long> diffTable = HashBasedTable.create();
			diffTable.putAll(baseDiffTable);
			diffTable.putAll(commandDiffTable);

			long allCommandCount = 0L;
			// 更新命令统计
			List<AppCommandStats> commandStatsList = getCommandStatsList(appId, collectTime, diffTable);
			for (AppCommandStats commandStats : commandStatsList) {
				// 排除无效命令且存储有累加的数据
				if (RedisExcludeCommand.isExcludeCommand(commandStats.getCommandName()) || commandStats.getCommandCount() <= 0L || (!isMaster && RWSpliter.isWriteCommand(commandStats.getCommandName()))) {
					continue;
				}
				allCommandCount += commandStats.getCommandCount();

				// appStatsDao.mergeMinuteCommandStatus(commandStats);
				// appStatsDao.mergeHourCommandStatus(commandStats);

				DbOperate minuteDbOperate = new DbOperate("appStatsDao", "mergeMinuteCommandStatus", commandStats);
				DbOperate hourDboperate = new DbOperate("appStatsDao", "mergeHourCommandStatus", commandStats);
				redisQueueHelper.push(minuteDbOperate);
				redisQueueHelper.push(hourDboperate);

			}
			long allGroupCommandCount = 0L;
			List<GroupCommandStats> groupCommandStatsList = getGroupCommandStatsList((appDao.getAppDescById(appId)).getBusinessGroupId(), collectTime, diffTable);
			for (GroupCommandStats commandStats : groupCommandStatsList) {
				// 排除无效命令且存储有累加的数据
				if (RedisExcludeCommand.isExcludeCommand(commandStats.getCommandName()) || commandStats.getCommandCount() <= 0L || (!isMaster && RWSpliter.isWriteCommand(commandStats.getCommandName()))) {
					continue;
				}
				allGroupCommandCount += commandStats.getCommandCount();

				// DbOperate minuteDbOperate = new DbOperate("groupDao",
				// "mergeMinuteCommandStatus", commandStats);
				// DbOperate hourDboperate = new DbOperate("groupDao",
				// "mergeHourCommandStatus", commandStats);
				// redisQueueHelper.push(minuteDbOperate);
				// redisQueueHelper.push(hourDboperate);
			}

			// 写入app分钟统计
			AppStats appStats = getAppStats(isMaster, appId, collectTime, diffTable, infoMap);
			appStats.setCommandCount(allCommandCount);

			DbOperate appMinuteOper = new DbOperate("appStatsDao", "mergeMinuteAppStats", appStats);
			DbOperate appHourOper = new DbOperate("appStatsDao", "mergeHourAppStats", appStats);
			redisQueueHelper.push(appMinuteOper);
			redisQueueHelper.push(appHourOper);

			// GroupStats groupStats = getGroupStats(appStats);
			// groupStats.setCommandCount(allGroupCommandCount);
			// DbOperate groupMinuteOper = new DbOperate("groupDao",
			// "mergeMinuteGroupStats", groupStats);
			// DbOperate groupHourOper = new DbOperate("groupDao",
			// "mergeHourGroupStats", groupStats);
			// redisQueueHelper.push(groupMinuteOper);
			// redisQueueHelper.push(groupHourOper);

			logger.info("collect redis info done, appId: {}, instance: {}:{}, time: {}", appId, host, port, collectTime);

			return true;
		}
	}

	private class consumDbOper implements Runnable {
		private ExecutorService appStatsPool;
		private ExecutorService instanceStatsPool;
		private ExecutorService groupDaoPool;

		private Map<String, List<Object>> batchListMap;

		public consumDbOper() {
			appStatsPool = getBlockingThreadPool(1);
			instanceStatsPool = getBlockingThreadPool(20);
			groupDaoPool = getBlockingThreadPool(1);
			batchListMap = new HashMap<String, List<Object>>();
		}

		private ThreadPoolExecutor getBlockingThreadPool(int concurrencyNum) {
			ThreadPoolExecutor pool = new ThreadPoolExecutor(concurrencyNum, concurrencyNum, Integer.MAX_VALUE, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("redis-cluster-migrate ", true));
			RejectedExecutionHandler block = new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					try {
						executor.getQueue().put(r);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			pool.setRejectedExecutionHandler(block);
			return pool;
		}

		@Override
		public void run() {
			while (true) {

				SqlSession session = null;
				try {
					long s1 = System.currentTimeMillis();
					// session =
					// mysqlSessionFactory.openSession(ExecutorType.BATCH,
					// false);
					// java.sql.Connection connection = session.getConnection();
					// connection.setAutoCommit(false);
					//
					// AppStatsDao appStatsDao =
					// session.getMapper(AppStatsDao.class);
					// InstanceStatsDao instanceStatsDao =
					// session.getMapper(InstanceStatsDao.class);
					// GroupDao groupDao = session.getMapper(GroupDao.class);

					int size = (int) redisQueueHelper.length();
					logger.info("DB queue size = {}", size);

					int counter = 0;
					Object raw;
					while ((raw = redisQueueHelper.pop()) != null) {
						DbOperate operate = (DbOperate) raw;
						String mapper = operate.getMapperName();
						String method = operate.getMethodName();

						if ("instanceStatsCenter".equals(mapper)) {
							// if ("saveStandardStats".equals(method)) {
							// StandardStatsParam param = (StandardStatsParam)
							// operate.getParameter();
							// instanceStatsCenter.saveStandardStats(param.getInforMap(),
							// param.getHost(), param.getPort(),
							// param.getType());
							// }
						}
						if ("instanceStatsDao".equals(mapper)) {
							// if ("updateInstanceStats".equals(method)) {
							// InstanceStats instanceStats = (InstanceStats)
							// operate.getParameter();
							// instanceStatsDao.updateInstanceStats(instanceStats);
							// }
						} else if ("appStatsDao".equals(mapper)) {
							if ("mergeMinuteCommandStatus".equals(method)) {
								if (isBatchLimitReached(method, operate.getParameter())) {
									appStatsDao.batchMergeMinuteCommandStatus(getList(batchListMap.get(method), AppCommandStats.class));
									batchListMap.get(method).clear();
								}
								// appStatsDao.mergeMinuteCommandStatus((AppCommandStats)
								// operate.getParameter());
							} else if ("mergeHourCommandStatus".equals(method)) {
								if (isBatchLimitReached(method, operate.getParameter())) {
									appStatsDao.batchMergeHourCommandStatus(getList(batchListMap.get(method), AppCommandStats.class));
									batchListMap.get(method).clear();
								}
								// appStatsDao.mergeHourCommandStatus((AppCommandStats)
								// operate.getParameter());
							} else if ("mergeMinuteAppStats".equals(method)) {
								if (isBatchLimitReached(method, operate.getParameter())) {
									appStatsDao.batchMergeMinuteAppStats(getList(batchListMap.get(method), AppStats.class));
									batchListMap.get(method).clear();
								}
								// appStatsDao.mergeMinuteAppStats((AppStats)operate.getParameter());
							} else if ("mergeHourAppStats".equals(method)) {
								if (isBatchLimitReached(method, operate.getParameter())) {
									appStatsDao.batchMergeHourAppStats(getList(batchListMap.get(method), AppStats.class));
									batchListMap.get(method).clear();
								}
								// appStatsDao.mergeHourAppStats((AppStats)
								// operate.getParameter());
							}
						} else if ("groupDao".equals(mapper)) {
							// if ("mergeMinuteCommandStatus".equals(method)) {
							// groupDao.mergeMinuteCommandStatus((GroupCommandStats)
							// operate.getParameter());
							// } else if
							// ("mergeHourCommandStatus".equals(method)) {
							// groupDao.mergeHourCommandStatus((GroupCommandStats)
							// operate.getParameter());
							// } else if
							// ("mergeMinuteGroupStats".equals(method)) {
							// groupDao.mergeMinuteGroupStats((GroupStats)
							// operate.getParameter());
							// } else if ("mergeHourGroupStats".equals(method))
							// {
							// groupDao.mergeHourGroupStats((GroupStats)
							// operate.getParameter());
							// }
						}
					}
					long s2 = System.currentTimeMillis();
					logger.info("采集信息批量插入DB耗时,cost = {}", s2 - s1);
				} catch (Exception e) {
					logger.error("collectQueneInfo error!, e = {}", e);
				} finally {
					// if (session != null) {
					// session.close();
					// }
				}

			}
		}

		private int checkAndCommit(final SqlSession session, ExecutorService pool) throws SQLException {
			pool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						session.commit();
						session.clearCache();
					} catch (Exception e) {
						logger.error("", e);
					} finally {
						session.close();
					}
				}
			});
			;
			return 0;
		}

		public boolean saveStandardStats(Map<String, Object> infoMap, String ip, int port, String dbType, InstanceStatsDao instanceStatsDao) {
			Assert.isTrue(infoMap != null && infoMap.size() > 0);
			Assert.isTrue(StringUtils.isNotBlank(ip));
			Assert.isTrue(port > 0);
			Assert.isTrue(infoMap.containsKey(ConstUtils.COLLECT_TIME), ConstUtils.COLLECT_TIME + " not in infoMap");
			long collectTime = MapUtils.getLong(infoMap, ConstUtils.COLLECT_TIME);
			StandardStats ss = new StandardStats();
			ss.setCollectTime(collectTime);
			ss.setIp(ip);
			ss.setPort(port);
			ss.setDbType(dbType);
			if (infoMap.containsKey(RedisConstant.DIFF.getValue())) {
				Map<String, Object> diffMap = (Map<String, Object>) infoMap.get(RedisConstant.DIFF.getValue());
				ss.setDiffMap(diffMap);
				infoMap.remove(RedisConstant.DIFF.getValue());
			} else {
				ss.setDiffMap(new HashMap<String, Object>(0));
			}
			ss.setInfoMap(infoMap);

			int mergeCount = instanceStatsDao.mergeStandardStats(ss);
			return mergeCount > 0;
		}

		private boolean isBatchLimitReached(String method, Object object) {
			List<Object> list = batchListMap.get(method);
			if (list == null) {
				list = new ArrayList<Object>();
				batchListMap.put(method, list);
			}

			list.add(object);

			return list.size() > ConstUtils.SQL_COMMIT_LIMIT;
		}

		private <T> List<T> getList(List<Object> list, Class<T> tClass) {
			List<T> rs = new ArrayList<T>();
			for (Object ob : list) {
				rs.add((T) ob);
			}

			return rs;
		}
	}

	private class collectQueneInfo implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					long collectTime = ScheduleUtil.getCollectTime(new Date());
					int size = (int) redisQueueHelper.length();
					if (size > (100000 * ConstUtils.QUEUE_SIZE_ALARM)) {
						mobileAlertComponent.sendPhoneToAdmin("采集数据队列大小已超过100000x0.8");
					}
					QueneSizeStatus queneSizeStatus = new QueneSizeStatus();
					queneSizeStatus.setCollectTime(collectTime);
					queneSizeStatus.setQueneSize(size);
					appStatsDao.mergeQueneSizeStatus(queneSizeStatus);
					Thread.sleep(1000 * 60);
				} catch (Exception e) {
					logger.error("collectQueneInfo error!, e = {}", e);
				}
			}
		}
	}

	/*
	 * private class mergeStats implements Runnable { public void run() { while
	 * (true){ List<AppDesc> appDescList = appDao.getAllAliveAppDescList();
	 * 
	 * for (AppDesc appDesc : appDescList){ List<InstanceInfo> infoList =
	 * instanceDao.getInstListByAppId(appDesc.getAppId());
	 * 
	 * } } } }
	 */

	@Override
	public void collectRedisSlowLog(long appId, long collectTime, final String host, final int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);

		// 处理
		String key = getThreadPoolKey() + "_" + host + "_" + port;
		boolean isOk = asyncService.submitFuture(getThreadPoolKey(), new KeyCallable<Boolean>(key) {
			@Override
			public Boolean execute() {
				try {
					InstanceInfo instanceInfo = instanceDao.getLiveInstByIpAndPort(host, port);

					// 不存在实例/实例异常/下线
					if (instanceInfo == null) {
						return false;
					}
					if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
						// 忽略sentinel redis实例
						return false;
					}

					String password = appDao.getAppDescById(instanceInfo.getAppId()).getPassword();
					// 从redis中获取慢查询日志
					List<RedisSlowLog> redisLowLogList = getRedisSlowLogs(host, port, 100, password);
					if (CollectionUtils.isEmpty(redisLowLogList)) {
						return false;
					}

					// transfer
					final List<InstanceSlowLog> instanceSlowLogList = new ArrayList<InstanceSlowLog>();
					for (RedisSlowLog redisSlowLog : redisLowLogList) {
						InstanceSlowLog instanceSlowLog = transferRedisSlowLogToInstance(redisSlowLog, instanceInfo);
						if (instanceSlowLog == null) {
							continue;
						}
						instanceSlowLogList.add(instanceSlowLog);
					}

					if (CollectionUtils.isEmpty(instanceSlowLogList)) {
						return false;
					}
					instanceSlowLogDao.batchSave(instanceSlowLogList);
					return true;
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return false;
				}
			}
		});
		if (!isOk) {
			logger.error("slowlog submitFuture failed,appId:{},collectTime:{},host:{},ip:{}", appId, collectTime, host, port);
		}
	}

	private InstanceSlowLog transferRedisSlowLogToInstance(RedisSlowLog redisSlowLog, InstanceInfo instanceInfo) {
		if (redisSlowLog == null) {
			return null;
		}

		String command = redisSlowLog.getCommand();
		int maxLength = 100;
		if (StringUtils.isNotBlank(command) && command.length() > maxLength) {
			command = command.substring(0, maxLength) + "...";
		}

		long executionTime = redisSlowLog.getExecutionTime();
		// 如果command=BGREWRITEAOF并且小于50毫秒,则忽略
		if (command.equalsIgnoreCase("BGREWRITEAOF") && executionTime < 50000) {
			return null;
		}

		InstanceSlowLog instanceSlowLog = new InstanceSlowLog();
		instanceSlowLog.setAppId(instanceInfo.getAppId());
		instanceSlowLog.setCommand(redisSlowLog.getCommand());
		instanceSlowLog.setCostTime((int) redisSlowLog.getExecutionTime());
		instanceSlowLog.setCreateTime(new Timestamp(System.currentTimeMillis()));
		instanceSlowLog.setExecuteTime(new Timestamp(redisSlowLog.getDate().getTime()));
		instanceSlowLog.setInstanceId(instanceInfo.getId());
		instanceSlowLog.setIp(instanceInfo.getIp());
		instanceSlowLog.setPort(instanceInfo.getPort());
		instanceSlowLog.setSlowLogId(redisSlowLog.getId());

		return instanceSlowLog;
	}

	private String getThreadPoolKey() {
		return REDIS_SLOWLOG_POOL;
	}

	/**
	 * 检查redis状态，并判断是否告警
	 *
	 * @param info
	 * @param isRun
	 */
	private void checkAndAlert(InstanceInfo info, boolean isRun) {

		Boolean isUpdate = updateInstanceByRun(isRun, info);
		if (isUpdate != null) {
			sendPhoneAlert(info);
		}
	}

	/**
	 * 发送短信报警
	 *
	 * @param info
	 */
	private void sendPhoneAlert(InstanceInfo info) {
		if (info == null) {
			return;
		}
		String message = generateMessage(info, false);
		mobileAlertComponent.sendPhone(message, null);
	}

	/**
	 * 返回示例消息
	 *
	 * @param info
	 * @return
	 */
	private String generateMessage(InstanceInfo info, boolean isEmail) {
		StringBuffer message = new StringBuffer();
		long appId = info.getAppId();
		AppDesc appDesc = appDao.getAppDescById(appId);
		message.append("CacheCloud系统-实例(" + info.getIp() + ":" + info.getPort() + ")-");
		if (info.getStatus() == InstanceStatusEnum.ERROR_STATUS.getStatus()) {
			message.append("由运行中变为心跳停止");
		} else if (info.getStatus() == InstanceStatusEnum.GOOD_STATUS.getStatus()) {
			message.append("由心跳停止变为运行中");
		}
		if (isEmail) {
			message.append(", appId:");
			message.append(appId + "-" + appDesc.getName());
		} else {
			message.append("-appId(" + appId + "-" + appDesc.getName() + ")");
		}
		return message.toString();
	}

	@Override
	public void collectRedisInfo(long appId, long collectTime, String host, int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);

		boolean isOk = asyncService.submitCollect(new RedisKeyCallable(appId, collectTime, host, port));
		if (!isOk) {
			logger.error("submitFuture failed,appId:{},collectTime:{},host:{},ip:{} cost={} ms", new Object[] { appId, collectTime, host, port, });
		}
	}

	private Boolean updateInstanceByRun(boolean isRun, InstanceInfo info) {
		if (info.getStatus() == InstanceStatusEnum.DELETE_STATUS.getStatus()) {
			return null;
		}
		if (isRun) {
			if (info.getStatus() != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
				info.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
				instanceDao.update(info);
				logger.warn("instance:{} instance is run", info);
				saveFault(info, isRun);
				return true;
			}
		} else {
			if (info.getStatus() != InstanceStatusEnum.ERROR_STATUS.getStatus()) {
				info.setStatus(InstanceStatusEnum.ERROR_STATUS.getStatus());
				instanceDao.update(info);
				logger.error("instance:{} instance failed", info);
				saveFault(info, isRun);
				return false;
			}
		}
		return null;
	}

	private void saveFault(InstanceInfo info, boolean isRun) {
		InstanceFault instanceFault = new InstanceFault();
		instanceFault.setAppId((int) info.getAppId());
		instanceFault.setInstId(info.getId());
		instanceFault.setIp(info.getIp());
		instanceFault.setPort(info.getPort());
		instanceFault.setType(info.getType());
		instanceFault.setCreateTime(new Date());
		if (isRun) {
			instanceFault.setReason("恢复运行");
		} else {
			instanceFault.setReason("心跳停止");
		}
		instanceFaultDao.insert(instanceFault);
	}

	@Override
	public Map<RedisConstant, Map<String, Object>> getInfoStats(final String host, final int port) {
		Map<RedisConstant, Map<String, Object>> infoMap = null;
		final StringBuilder infoBuilder = new StringBuilder();
		try {
			boolean isOk = new IdempotentConfirmer() {
				private int timeOutFactor = 1;

				@Override
				public boolean execute() {
					Jedis jedis = null;
					try {
						jedis = getAuthJedis(host, port);
						jedis.getClient().setConnectionTimeout(REDIS_DEFAULT_TIME * (timeOutFactor++));
						jedis.getClient().setSoTimeout(REDIS_DEFAULT_TIME * (timeOutFactor++));
						String info = jedis.info("all");
						infoBuilder.append(info);
						return StringUtils.isNotBlank(info);
					} catch (Exception e) {
						logger.warn("{}:{}, redis-getInfoStats errorMsg:{}", host, port, e.getMessage());
						return false;
					} finally {
						if (jedis != null)
							jedis.close();
					}
				}
			}.run();
			if (!isOk) {
				return infoMap;
			}
			infoMap = processRedisStats(infoBuilder.toString());
		} catch (Exception e) {
			logger.error(e.getMessage() + " {}:{}", host, port, e);
		}
		if (infoMap == null || infoMap.isEmpty()) {
			logger.error("host:{},ip:{} redis infoMap is null", host, port);
			return infoMap;
		}
		return infoMap;
	}

	private void fillAccumulationMap(Map<RedisConstant, Map<String, Object>> infoMap, Table<RedisConstant, String, Long> table) {
		Map<String, Object> accMap = infoMap.get(RedisConstant.DIFF);
		if (table == null || table.isEmpty()) {
			return;
		}
		if (accMap == null) {
			accMap = new LinkedHashMap<String, Object>();
			infoMap.put(RedisConstant.DIFF, accMap);
		}
		for (RedisConstant constant : table.rowKeySet()) {
			Map<String, Long> rowMap = table.row(constant);
			accMap.putAll(rowMap);
		}
	}

	/**
	 * 获取累加参数值
	 *
	 * @param currentInfoMap
	 * @return 累加差值map
	 */
	private Table<RedisConstant, String, Long> getAccumulationDiff(Map<RedisConstant, Map<String, Object>> currentInfoMap, Map<String, Object> lastInfoMap) {
		// 没有上一次统计快照，忽略差值统计
		if (lastInfoMap == null || lastInfoMap.isEmpty()) {
			return HashBasedTable.create();
		}
		Map<RedisAccumulation, Long> currentMap = new LinkedHashMap<RedisAccumulation, Long>();
		for (RedisAccumulation acc : RedisAccumulation.values()) {
			Long count = getCommonCount(currentInfoMap, acc.getConstant(), acc.getValue());
			if (count != null) {
				currentMap.put(acc, count);
			}
		}
		Map<RedisAccumulation, Long> lastMap = new LinkedHashMap<RedisAccumulation, Long>();
		for (RedisAccumulation acc : RedisAccumulation.values()) {
			if (lastInfoMap != null) {
				Long lastCount = getCommonCount(lastInfoMap, acc.getConstant(), acc.getValue());
				if (lastCount != null) {
					lastMap.put(acc, lastCount);
				}
			}
		}
		Table<RedisConstant, String, Long> resultTable = HashBasedTable.create();
		for (RedisAccumulation key : currentMap.keySet()) {
			Long value = MapUtils.getLong(currentMap, key, null);
			Long lastValue = MapUtils.getLong(lastMap, key, null);
			if (value == null || lastValue == null) {
				// 忽略
				continue;
			}
			long diff = 0L;
			if (value > lastValue) {
				diff = value - lastValue;
			}
			resultTable.put(key.getConstant(), key.getValue(), diff);
		}
		return resultTable;
	}

	/**
	 * 获取命令差值统计
	 *
	 * @param currentInfoMap
	 * @param lastInfoMap
	 * @return 命令统计
	 */
	private Table<RedisConstant, String, Long> getCommandsDiff(Map<RedisConstant, Map<String, Object>> currentInfoMap, Map<String, Object> lastInfoMap) {
		// 没有上一次统计快照，忽略差值统计
		if (lastInfoMap == null || lastInfoMap.isEmpty()) {
			return HashBasedTable.create();
		}
		Map<String, Object> map = currentInfoMap.get(RedisConstant.Commandstats);
		Map<String, Long> currentMap = transferLongMap(map);
		Map<String, Object> lastObjectMap;
		if (lastInfoMap.get(RedisConstant.Commandstats.getValue()) == null) {
			lastObjectMap = new HashMap<String, Object>();
		} else {
			lastObjectMap = (Map<String, Object>) lastInfoMap.get(RedisConstant.Commandstats.getValue());
		}
		Map<String, Long> lastMap = transferLongMap(lastObjectMap);

		Table<RedisConstant, String, Long> resultTable = HashBasedTable.create();
		for (String command : currentMap.keySet()) {
			long lastCount = MapUtils.getLong(lastMap, command, 0L);
			long currentCount = MapUtils.getLong(currentMap, command, 0L);
			if (currentCount > lastCount) {
				resultTable.put(RedisConstant.Commandstats, command, (currentCount - lastCount));
			}
		}
		return resultTable;
	}

	private AppStats getAppStats(boolean isMaster, long appId, long collectTime, Table<RedisConstant, String, Long> table, Map<RedisConstant, Map<String, Object>> infoMap) {
		AppStats appStats = new AppStats();
		appStats.setAppId(appId);
		appStats.setCollectTime(collectTime);
		appStats.setModifyTime(new Date());
		if (isMaster) {
			appStats.setUsedMemory(MapUtils.getLong(infoMap.get(RedisConstant.Memory), "used_memory", 0L));
		} else {
			appStats.setUsedMemory(0L);
		}
		;
		appStats.setHits(MapUtils.getLong(table.row(RedisConstant.Stats), "keyspace_hits", 0L));
		appStats.setMisses(MapUtils.getLong(table.row(RedisConstant.Stats), "keyspace_misses", 0L));
		appStats.setEvictedKeys(MapUtils.getLong(table.row(RedisConstant.Stats), "evicted_keys", 0L));
		appStats.setExpiredKeys(MapUtils.getLong(table.row(RedisConstant.Stats), "expired_keys", 0L));
		appStats.setNetInputByte(MapUtils.getLong(table.row(RedisConstant.Stats), "total_net_input_bytes", 0L));
		appStats.setNetOutputByte(MapUtils.getLong(table.row(RedisConstant.Stats), "total_net_output_bytes", 0L));
		appStats.setConnectedClients(MapUtils.getIntValue(infoMap.get(RedisConstant.Clients), "connected_clients", 0));
		appStats.setObjectSize(getObjectSize(infoMap));
		return appStats;
	}

	private GroupStats getGroupStats(AppStats appStats) {
		GroupStats groupStats = new GroupStats();
		groupStats.setBusinessGroupId((appDao.getAppDescById(appStats.getAppId())).getBusinessGroupId());
		groupStats.setCollectTime(appStats.getCollectTime());
		groupStats.setHits(appStats.getHits());
		groupStats.setMisses(appStats.getMisses());
		groupStats.setCommandCount(appStats.getCommandCount());
		groupStats.setUsedMemory(appStats.getUsedMemory());
		groupStats.setExpiredKeys(appStats.getExpiredKeys());
		groupStats.setEvictedKeys(appStats.getEvictedKeys());
		groupStats.setNetInputByte(appStats.getNetInputByte());
		groupStats.setNetOutputByte(appStats.getNetOutputByte());
		groupStats.setConnectedClients(appStats.getConnectedClients());
		groupStats.setObjectSize(appStats.getObjectSize());
		groupStats.setAccumulation(appStats.getAccumulation());
		groupStats.setCreateTime(appStats.getCreateTime());
		groupStats.setModifyTime(appStats.getModifyTime());
		return groupStats;
	}

	private long getObjectSize(Map<RedisConstant, Map<String, Object>> currentInfoMap) {
		Map<String, Object> sizeMap = currentInfoMap.get(RedisConstant.Keyspace);
		if (sizeMap == null || sizeMap.isEmpty()) {
			return 0L;
		}
		long result = 0L;
		Map<String, Long> longSizeMap = transferLongMap(sizeMap);
		for (String key : longSizeMap.keySet()) {
			result += longSizeMap.get(key);
		}
		return result;
	}

	private Long getCommonCount(Map<?, ?> infoMap, RedisConstant redisConstant, String commond) {
		Object constantObject = infoMap.get(redisConstant) == null ? infoMap.get(redisConstant.getValue()) : infoMap.get(redisConstant);
		if (constantObject != null && (constantObject instanceof Map)) {
			Map constantMap = (Map) constantObject;
			if (constantMap == null || constantMap.get(commond) == null) {
				return null;
			}
			return MapUtils.getLongValue(constantMap, commond);
		}
		return null;
	}

	/**
	 * 转换redis 命令行统计结果
	 *
	 * @param commandMap
	 * @return
	 */
	private Map<String, Long> transferLongMap(Map<String, Object> commandMap) {
		Map<String, Long> resultMap = new HashMap<String, Long>();
		if (commandMap == null || commandMap.isEmpty()) {
			return resultMap;
		}
		for (String key : commandMap.keySet()) {
			if (commandMap.get(key) == null) {
				continue;
			}
			String value = commandMap.get(key).toString();
			String[] stats = value.split(",");
			if (stats.length == 0) {
				continue;
			}
			String[] calls = stats[0].split("=");
			if (calls == null || calls.length < 2) {
				continue;
			}
			long callCount = Long.valueOf(calls[1]);
			resultMap.put(key, callCount);
		}
		return resultMap;
	}

	private List<AppCommandStats> getCommandStatsList(long appId, long collectTime, Table<RedisConstant, String, Long> table) {
		Map<String, Long> commandMap = table.row(RedisConstant.Commandstats);
		List<AppCommandStats> list = new ArrayList<AppCommandStats>();
		if (commandMap == null) {
			return list;
		}
		for (String key : commandMap.keySet()) {
			String commandName = key.replace("cmdstat_", "");
			long callCount = MapUtils.getLong(commandMap, key, 0L);
			if (callCount == 0L) {
				continue;
			}
			AppCommandStats commandStats = new AppCommandStats();
			commandStats.setAppId(appId);
			commandStats.setCollectTime(collectTime);
			commandStats.setCommandName(commandName);
			commandStats.setCommandCount(callCount);
			commandStats.setModifyTime(new Date());
			list.add(commandStats);
		}
		return list;
	}

	private List<GroupCommandStats> getGroupCommandStatsList(long groupId, long collectTime, Table<RedisConstant, String, Long> table) {
		Map<String, Long> commandMap = table.row(RedisConstant.Commandstats);
		List<GroupCommandStats> list = new ArrayList<>();
		if (commandMap == null) {
			return list;
		}
		for (String key : commandMap.keySet()) {
			String commandName = key.replace("cmdstat_", "");
			long callCount = MapUtils.getLong(commandMap, key, 0L);
			if (callCount == 0L) {
				continue;
			}
			GroupCommandStats commandStats = new GroupCommandStats();
			commandStats.setBusinessGroupId(groupId);
			commandStats.setCollectTime(collectTime);
			commandStats.setCommandName(commandName);
			commandStats.setCommandCount(callCount);
			commandStats.setModifyTime(new Date());
			list.add(commandStats);
		}
		return list;
	}

	/**
	 * 处理redis统计信息
	 *
	 * @param statResult
	 *            统计结果串
	 */
	private Map<RedisConstant, Map<String, Object>> processRedisStats(String statResult) {
		Map<RedisConstant, Map<String, Object>> redisStatMap = new HashMap<RedisConstant, Map<String, Object>>();
		String[] data = statResult.split("\r\n");
		String key;
		int i = 0;
		int length = data.length;
		while (i < length) {
			if (data[i].contains("#")) {
				int index = data[i].indexOf('#');
				key = data[i].substring(index + 1);
				++i;
				RedisConstant redisConstant = RedisConstant.value(key.trim());
				if (redisConstant == null) {
					continue;
				}
				Map<String, Object> sectionMap = new LinkedHashMap<String, Object>();
				while (i < length && data[i].contains(":")) {
					String[] pair = data[i].split(":");
					sectionMap.put(pair[0], pair[1]);
					i++;
				}
				redisStatMap.put(redisConstant, sectionMap);
			} else {
				i++;
			}
		}
		return redisStatMap;
	}

	/**
	 * 根据infoMap的结果判断实例的主从
	 *
	 * @param infoMap
	 * @return
	 */
	private Boolean isMaster(Map<RedisConstant, Map<String, Object>> infoMap) {
		Map<String, Object> map = infoMap.get(RedisConstant.Replication);
		if (map == null || map.get("role") == null) {
			return null;
		}
		if (String.valueOf(map.get("role")).equals("master")) {
			return true;
		}
		return false;
	}

	/**
	 * 根据ip和port判断某一个实例当前是主还是从
	 *
	 * @param ip
	 *            ip
	 * @param port
	 *            port
	 * @return 主返回true， 从返回false；
	 */
	@Override
	public Boolean isMaster(String ip, int port) {
		Jedis jedis = null;

		try {
			jedis = getAuthJedis(ip, port);
			String info = jedis.info("all");
			Map<RedisConstant, Map<String, Object>> infoMap = processRedisStats(info);
			return isMaster(infoMap);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		} finally {
			jedis.close();
		}
	}

	@Override
	public HostAndPort getMaster(String ip, int port) {
		Jedis jedis = null;
		try {
			jedis = getAuthJedis(ip, port);
			String info = jedis.info(RedisConstant.Replication.getValue());
			Map<RedisConstant, Map<String, Object>> infoMap = processRedisStats(info);
			Map<String, Object> map = infoMap.get(RedisConstant.Replication);
			if (map == null) {
				return null;
			}
			String masterHost = MapUtils.getString(map, "master_host", null);
			int masterPort = MapUtils.getInteger(map, "master_port", 0);
			if (StringUtils.isNotBlank(masterHost) && masterPort > 0) {
				return new HostAndPort(masterHost, masterPort);
			}
			return null;
		} catch (Exception e) {
			logger.error("{}:{} getMaster failed {}", ip, port, e.getMessage(), e);
			return null;
		} finally {
			if (jedis != null)
				jedis.close();
		}
	}

	@Override
	public boolean isRun(final String ip, final int port, final String password) {
		boolean isRun = new IdempotentConfirmer() {
			private int timeOutFactor = 1;

			@Override
			public boolean execute() {
				Jedis jedis = getAuthJedis(ip, port);
				try {
					jedis.getClient().setConnectionTimeout(Protocol.DEFAULT_TIMEOUT * (timeOutFactor++));
					jedis.getClient().setSoTimeout(Protocol.DEFAULT_TIMEOUT * (timeOutFactor++));
					if (StringUtils.isNotBlank(password)) {
						jedis.auth(password);
					}
					String pong = jedis.ping();
					return pong != null && pong.equalsIgnoreCase("PONG");
				} catch (JedisDataException e) {
					String message = e.getMessage();
					logger.warn(e.getMessage());
					if (StringUtils.isNotBlank(message) && message.startsWith("LOADING")) {
						return true;
					}
					return false;
				} catch (Exception e) {
					logger.warn("{}:{} error message is {} ", ip, port, e.getMessage());
					return false;
				} finally {
					jedis.close();
				}
			}
		}.run();
		return isRun;
	}

	@Override
	public boolean isRun(final String ip, final int port) {
		return isRun(ip, port, null);
	}

	@Override
	public boolean shutdown(String ip, int port) {
		boolean isRun = isRun(ip, port);
		if (!isRun) {
			return true;
		}
		final Jedis jedis = getAuthJedis(ip, port);
		try {
			// 关闭实例节点
			boolean isShutdown = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					jedis.shutdown();
					return true;
				}
			}.run();

			if (isPortExist(ip, port)) {
				logger.warn("{}:{} redis executing shutdown failed,start hard kill redis.!", ip, port);
				isShutdown = false;
				isShutdown = RedisUtil.hardKillRedis(ip, port);
			} else {
				logger.warn("{}:{} redis executing shutdown succeeded!", ip, port);
			}

			if (isPortExist(ip, port)) {
				isShutdown = false;
				mobileAlert.sendPhoneToAdmin(String.format("%s %d redis shutdown failed.", ip, port));
				logger.error("{}:{} redis not shutdown!", ip, port);
			}
			return isShutdown;
		} finally {
			jedis.close();
		}
	}

	private boolean isPortExist(String ip, int port) {
		try {
			return SSHUtil.isPortUsed(ip, port);
		} catch (SSHException e) {
			logger.error("", e);
		}
		return true;
	}

	/**
	 * 根据infoMap的结果判断实例的主从
	 *
	 * @param infoMap
	 * @return
	 */
	private Boolean hasSlaves(Map<RedisConstant, Map<String, Object>> infoMap) {
		Map<String, Object> replicationMap = infoMap.get(RedisConstant.Replication);
		if (MapUtils.isEmpty(replicationMap)) {
			return null;
		}
		for (Entry<String, Object> entry : replicationMap.entrySet()) {
			String key = entry.getKey();
			// 判断一个即可
			if (key != null && key.contains("slave0")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据ip和port判断redis实例当前是否有从节点
	 * 
	 * @param ip
	 *            ip
	 * @param port
	 *            port
	 * @return 主返回true，从返回false；
	 */
	@Override
	public Boolean hasSlaves(String ip, int port) {
		Jedis jedis = getAuthJedis(ip, port);
		try {
			String info = jedis.info("all");
			Map<RedisConstant, Map<String, Object>> infoMap = processRedisStats(info);
			return hasSlaves(infoMap);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		} finally {
			jedis.close();
		}
	}

	/**
	 * 返回当前实例的一些关键指标
	 *
	 * @param appId
	 * @param ip
	 * @param port
	 * @param infoMap
	 * @return
	 */
	public InstanceStats getInstanceStats(long appId, String ip, int port, Map<RedisConstant, Map<String, Object>> infoMap) {
		if (infoMap == null) {
			return null;
		}
		// 查询最大内存限制
		Long maxMemory = this.getRedisMaxMemory(ip, port);
		/**
		 * 将实例的一些关键指标返回
		 */
		InstanceStats instanceStats = new InstanceStats();
		instanceStats.setAppId(appId);
		InstanceInfo curInst = instanceDao.getLiveInstByIpAndPort(ip, port);
		if (curInst != null) {
			instanceStats.setHostId(curInst.getHostId());
			instanceStats.setInstId(curInst.getId());
		} else {
			logger.error("redis={}:{} not found", ip, port);
			return null;
		}
		instanceStats.setIp(ip);
		instanceStats.setPort(port);
		if (maxMemory != null) {
			instanceStats.setMaxMemory(maxMemory);
		}
		instanceStats.setUsedMemory(MapUtils.getLongValue(infoMap.get(RedisConstant.Memory), "used_memory", 0));
		instanceStats.setHits(MapUtils.getLongValue(infoMap.get(RedisConstant.Stats), "keyspace_hits", 0));
		instanceStats.setMisses(MapUtils.getLongValue(infoMap.get(RedisConstant.Stats), "keyspace_misses", 0));
		instanceStats.setCurrConnections(MapUtils.getIntValue(infoMap.get(RedisConstant.Clients), "connected_clients", 0));
		instanceStats.setCurrItems(getObjectSize(infoMap));
		instanceStats.setRole((byte) 1);
		if (MapUtils.getString(infoMap.get(RedisConstant.Replication), "role").equals("slave")) {
			instanceStats.setRole((byte) 2);
		}
		instanceStats.setModifyTime(new Timestamp(System.currentTimeMillis()));
		instanceStats.setMemFragmentationRatio(MapUtils.getDoubleValue(infoMap.get(RedisConstant.Memory), "mem_fragmentation_ratio", 0.0));
		instanceStats.setAofDelayedFsync(MapUtils.getIntValue(infoMap.get(RedisConstant.Persistence), "aof_delayed_fsync", 0));
		return instanceStats;
	}

	@Override
	public Long getRedisMaxMemory(final String ip, final int port) {
		final String key = "maxmemory";
		final Map<String, Long> resultMap = new HashMap<String, Long>();
		boolean isSuccess = new IdempotentConfirmer() {
			private int timeOutFactor = 1;

			@Override
			public boolean execute() {
				Jedis jedis = null;
				try {
					jedis = getAuthJedis(ip, port);
					jedis.getClient().setConnectionTimeout(REDIS_DEFAULT_TIME * (timeOutFactor++));
					jedis.getClient().setSoTimeout(REDIS_DEFAULT_TIME * (timeOutFactor++));
					List<String> maxMemoryList = jedis.configGet(key); // 返回结果：list中是2个字符串，如："maxmemory",
					// "4096000000"
					if (maxMemoryList != null && maxMemoryList.size() >= 2) {
						resultMap.put(key, Long.valueOf(maxMemoryList.get(1)));
					}
					return MapUtils.isNotEmpty(resultMap);
				} catch (Exception e) {
					logger.warn("{}:{} errorMsg: {}", ip, port, e.getMessage());
					return false;
				} finally {
					if (jedis != null) {
						jedis.close();
					}
				}
			}
		}.run();
		if (isSuccess) {
			return MapUtils.getLong(resultMap, key);
		} else {
			logger.error("{}:{} getMaxMemory failed!", ip, port);
			return null;
		}
	}

	@Override
	public Jedis getAuthJedis(String ip, int port) {
		return getAuthJedis(ip, port, REDIS_DEFAULT_TIME);
	}
	
	@Override
	public Jedis getAuthJedis(String ip, int port,int timeout) {
		Jedis jedis = new Jedis(ip, port, timeout);
		InstanceInfo instance = instanceDao.getAllInstByIpAndPort(ip, port);
		if (instance == null){
			return jedis;
		}
		AppDesc app = appDao.getAppDescById(instance.getAppId());
		String password = app.getPassword();

		if (StringUtils.isNotEmpty(password)) {
			jedis.auth(password);
		}
		return jedis;
	}

	@Override
	public String executeCommand(AppDesc appDesc, String command) {
		// 非测试应用只能执行白名单里面的命令
		if (AppDescEnum.AppTest.NOT_TEST.getValue() == appDesc.getIsTest()) {
			if (!RedisReadOnlyCommandEnum.contains(command)) {
				return "online app only support read-only and safe command";
			}
		}
		int type = appDesc.getType();
		long appId = appDesc.getAppId();
		if (type == ConstUtils.CACHE_REDIS_SENTINEL) {
			JedisSentinelPool jedisSentinelPool = getJedisSentinelPool(appDesc);
			if (jedisSentinelPool == null) {
				return "sentinel can not execute ";
			}
			Jedis jedis = null;
			try {
				jedis = jedisSentinelPool.getResource();
				String host = jedis.getClient().getHost();
				int port = jedis.getClient().getPort();
				return executeCommand(appId, host, port, command);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return "运行出错:" + e.getMessage();
			} finally {
				if (jedis != null)
					jedis.close();
				jedisSentinelPool.destroy();
			}
		} else if (type == ConstUtils.CACHE_REDIS_STANDALONE) {
			List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
			if (instanceList == null || instanceList.isEmpty()) {
				return "应用没有运行的实例";
			}
			String host = null;
			int port = 0;
			for (InstanceInfo instanceInfo : instanceList) {
				host = instanceInfo.getIp();
				port = instanceInfo.getPort();
				break;
			}
			try {
				return executeCommand(appId, host, port, command);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return "运行出错:" + e.getMessage();
			}
		} else if (type == ConstUtils.CACHE_TYPE_REDIS_CLUSTER) {
			List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
			if (instanceList == null || instanceList.isEmpty()) {
				return "应用没有运行的实例";
			}
			Set<HostAndPort> clusterHosts = new LinkedHashSet<HostAndPort>();
			for (InstanceInfo instance : instanceList) {
				if (instance == null || instance.getStatus() == InstanceStatusEnum.OFFLINE_STATUS.getStatus()) {
					continue;
				}
				clusterHosts.add(new HostAndPort(instance.getIp(), instance.getPort()));
			}
			if (clusterHosts.isEmpty()) {
				return "no run instance";
			}
			String host = null;
			int port = 0;
			JedisCluster jedisCluster = new JedisCluster(clusterHosts, REDIS_DEFAULT_TIME);
			try {
				String commandKey = getCommandKey(command);
				int slot;
				if (StringUtils.isBlank(commandKey)) {
					slot = 0;
				} else {
					slot = JedisClusterCRC16.getSlot(commandKey);
				}
				JedisPool jedisPool = jedisCluster.getConnectionHandler().getJedisPoolFromSlot(slot);
				host = jedisPool.getHost();
				port = jedisPool.getPort();
			} finally {
				jedisCluster.close();
			}

			try {
				return executeCommand(appId, host, port, command);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return "运行出错:" + e.getMessage();
			}
		}
		return "不支持应用类型";
	}

	private String getCommandKey(String command) {
		String[] array = StringUtils.trim(command).split("\\s+");
		if (array.length > 1) {
			return array[1];
		} else {
			return null;
		}
	}

	@Override
	public String executeCommand(long appId, String host, int port, String command) {
		AppDesc appDesc = appDao.getAppDescById(appId);
		if (appDesc == null) {
			return "not exist appId";
		}
		// 非测试应用只能执行白名单里面的命令
		if (AppDescEnum.AppTest.NOT_TEST.getValue() == appDesc.getIsTest()) {
			if (!RedisReadOnlyCommandEnum.contains(command)) {
				return "online app only support read-only and safe command ";
			}
		}
		String shell = RedisProtocol.getExecuteCommandShell(host, port, command);
		// 记录客户端发送日志
		logger.warn("executeRedisShell={}", shell);
		return machineCenter.executeShell(host, shell);
	}

	@Override
	public JedisSentinelPool getJedisSentinelPool(AppDesc appDesc) {
		if (appDesc == null) {
			logger.error("appDes is null");
			return null;
		}
		if (appDesc.getType() != ConstUtils.CACHE_REDIS_SENTINEL) {
			logger.error("type={} is not sentinel", appDesc.getType());
			return null;
		}
		long appId = appDesc.getAppId();
		List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);

		String masterName = null;
		for (Iterator<InstanceInfo> i = instanceInfos.iterator(); i.hasNext();) {
			InstanceInfo instanceInfo = i.next();
			if (instanceInfo.getType() != ConstUtils.CACHE_REDIS_SENTINEL) {
				i.remove();
				continue;
			}
			if (masterName == null && StringUtils.isNotBlank(instanceInfo.getCmd())) {
				masterName = instanceInfo.getCmd();
			}
		}
		Set<String> sentinels = new HashSet<String>();
		for (InstanceInfo instanceInfo : instanceInfos) {
			sentinels.add(instanceInfo.getIp() + ":" + instanceInfo.getPort());
		}
		JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(masterName, sentinels);
		return jedisSentinelPool;
	}

	@Override
	public Map<String, String> getRedisConfigList(int instanceId) {
		if (instanceId <= 0) {
			return Collections.emptyMap();
		}
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		if (instanceInfo == null) {
			return Collections.emptyMap();
		}
		if (TypeUtil.isRedisType(instanceInfo.getType())) {
			Jedis jedis = null;
			try {
				jedis = getAuthJedis(instanceInfo.getIp(), instanceInfo.getPort());
				List<String> configs = jedis.configGet("*");
				Map<String, String> configMap = new LinkedHashMap<String, String>();
				for (int i = 0; i < configs.size(); i += 2) {
					if (i < configs.size()) {
						String key = configs.get(i);
						String value = configs.get(i + 1);
						if (StringUtils.isBlank(value)) {
							continue;
						}
						configMap.put(key, value);
					}
				}
				return configMap;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}

		return Collections.emptyMap();
	}

	@Override
	public List<RedisSlowLog> getRedisSlowLogs(int instanceId, int maxCount) {
		if (instanceId <= 0) {
			return Collections.emptyList();
		}
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		if (instanceInfo == null) {
			return Collections.emptyList();
		}

		if (TypeUtil.isRedisType(instanceInfo.getType())) {
			String password = appDao.getAppDescById(instanceInfo.getAppId()).getPassword();
			return getRedisSlowLogs(instanceInfo.getIp(), instanceInfo.getPort(), maxCount, password);
		}
		return Collections.emptyList();
	}

	private List<RedisSlowLog> getRedisSlowLogs(String host, int port, int maxCount, String password) {
		Jedis jedis = null;
		try {
			jedis = getAuthJedis(host, port);
			if (StringUtils.isNotEmpty(password)) {
				jedis.auth(password);
			}
			List<RedisSlowLog> resultList = new ArrayList<RedisSlowLog>();
			List<Slowlog> slowlogs = null;
			if (maxCount > 0) {
				slowlogs = jedis.slowlogGet(maxCount);
			} else {
				slowlogs = jedis.slowlogGet();
			}
			if (slowlogs != null && slowlogs.size() > 0) {
				for (Slowlog sl : slowlogs) {
					RedisSlowLog rs = new RedisSlowLog();
					rs.setId(sl.getId());
					rs.setExecutionTime(sl.getExecutionTime());
					long time = sl.getTimeStamp() * 1000L;
					rs.setDate(new Date(time));
					rs.setTimeStamp(DateUtil.formatYYYYMMddHHMMSS(new Date(time)));
					rs.setCommand(StringUtils.join(sl.getArgs(), " "));
					resultList.add(rs);
				}
			}
			return resultList;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public boolean configRewrite(final String host, final int port) {
		return new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				Jedis jedis = getAuthJedis(host, port);
				try {
					String response = jedis.configRewrite();
					return response != null && response.equalsIgnoreCase("OK");
				} finally {
					jedis.close();
				}
			}
		}.run();
	}
	
	@Override
	public boolean cleanAppData(AppDesc appDesc, AppUser appUser, JSONObject errMsg) {
		String msg = "";
		boolean cleanResult = true;
		if (appDesc == null) {
			return false;
		}

		long appId = appDesc.getAppId();

		// 线上应用不能清理数据
		if (AppDescEnum.AppTest.IS_TEST.getValue() != appDesc.getIsTest()) {
			logger.error("appId {} profile must be test", appId);
			return false;
		}

		// 必须是redis应用
		if (!TypeUtil.isRedisType(appDesc.getType())) {
			logger.error("appId {} type must be redis", appId);
			return false;
		}

		// 实例验证
		List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
		if (CollectionUtils.isEmpty(instanceList)) {
			logger.error("appId {} instanceList is empty", appId);
			return false;
		}

		// 开始清除
		for (InstanceInfo instance : instanceList) {
			if (instance.getStatus() != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
				continue;
			}
			String host = instance.getIp();
			// master + 非sentinel节点
			int port = instance.getPort();
			Boolean isMater = isMaster(host, port);
			if (isMater != null && isMater.equals(true) && !TypeUtil.isRedisSentinel(instance.getType())) {
				Jedis jedis = getAuthJedis(host, port, 30000);
				// 检测redis节点是否可用
				try {
					String resp = jedis.echo("success");
					if (!"success".equals(resp)) {
						msg = msg + host + ":" + port + "异常,清除数据失败;";
						cleanResult = false;
						continue;
					}
				} catch (Exception e) {
					msg = msg + host + ":" + port + "异常,清除数据失败,msg=" + e.getMessage() + ";";
					cleanResult = false;
					continue;
				}
				// 执行清空操作
				try {
					logger.warn("{}:{} start clear data", host, port);
					long start = System.currentTimeMillis();
					// flushAll命令不会失败,抛出异常不做处理
					jedis.flushAll();
					logger.warn("{}:{} finish clear data, cost time:{} ms", host, port, (System.currentTimeMillis() - start));
				} catch (Exception e) {
					logger.error("clear redis: " + e.getMessage(), e);
				} finally {
					jedis.close();
				}
			}
		}

		// 记录日志
		AppAuditLog appAuditLog = AppAuditLog.generate(appDesc, appUser, 0L, AppAuditLogTypeEnum.APP_CLEAN_DATA);
		appAuditLogDao.save(appAuditLog);

		errMsg.put("errMsg", msg);
		return cleanResult;
	}

	@Override
	public boolean isSingleClusterNode(String host, int port) {
		final Jedis jedis = getAuthJedis(host, port);
		try {
			String clusterNodes = jedis.clusterNodes();
			if (StringUtils.isBlank(clusterNodes)) {
				throw new RuntimeException(host + ":" + port + "clusterNodes is null");
			}
			String[] nodeInfos = clusterNodes.split("\n");
			if (nodeInfos.length == 1) {
				return true;
			}
			return false;
		} finally {
			jedis.close();
		}
	}

	@Override
	public List<String> getClientList(int instanceId) {
		if (instanceId <= 0) {
			return Collections.emptyList();
		}
		InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
		if (instanceInfo == null) {
			return Collections.emptyList();
		}
		if (TypeUtil.isRedisType(instanceInfo.getType())) {
			Jedis jedis = null;
			try {
				jedis = getAuthJedis(instanceInfo.getIp(), instanceInfo.getPort(), REDIS_DEFAULT_TIME);
				jedis.clientList();
				List<String> resultList = new ArrayList<String>();
				String clientList = jedis.clientList();
				if (StringUtils.isNotBlank(clientList)) {
					String[] array = clientList.split("\n");
					resultList.addAll(Arrays.asList(array));
				}
				return resultList;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Map<String, String> getClusterLossSlots(long appId) {
		// 1.从应用中获取一个健康的主节点
		InstanceInfo sourceMasterInstance = getHealthyInstanceInfo(appId);
		if (sourceMasterInstance == null) {
			return Collections.emptyMap();
		}
		// 2. 获取所有slot和节点的对应关系
		Map<Integer, String> slotHostPortMap = getSlotsHostPortMap(sourceMasterInstance.getIp(), sourceMasterInstance.getPort());
		// 3. 获取集群中失联的slot
		List<Integer> lossSlotList = getClusterLossSlots(sourceMasterInstance.getIp(), sourceMasterInstance.getPort());
		// 3.1 将失联的slot列表组装成Map<String host:port,List<Integer> lossSlotList>
		Map<String, List<Integer>> hostPortSlotMap = new HashMap<String, List<Integer>>();
		if (CollectionUtils.isNotEmpty(lossSlotList)) {
			for (Integer lossSlot : lossSlotList) {
				String key = slotHostPortMap.get(lossSlot);
				if (hostPortSlotMap.containsKey(key)) {
					hostPortSlotMap.get(key).add(lossSlot);
				} else {
					List<Integer> list = new ArrayList<Integer>();
					list.add(lossSlot);
					hostPortSlotMap.put(key, list);
				}
			}
		}
		// 3.2 hostPortSlotMap组装成Map<String host:port,String startSlot-endSlot>
		Map<String, String> slotSegmentsMap = new HashMap<String, String>();
		for (Entry<String, List<Integer>> entry : hostPortSlotMap.entrySet()) {
			List<Integer> list = entry.getValue();
			List<String> slotSegments = new ArrayList<String>();
			int min = list.get(0);
			int max = min;
			for (int i = 1; i < list.size(); i++) {
				int temp = list.get(i);
				if (temp == max + 1) {
					max = temp;
				} else {
					slotSegments.add(String.valueOf(min) + "-" + String.valueOf(max));
					min = temp;
					max = temp;
				}
			}
			slotSegments.add(String.valueOf(min) + "-" + String.valueOf(max));
			slotSegmentsMap.put(entry.getKey(), slotSegments.toString());
		}
		return slotSegmentsMap;
	}

	/**
	 * 从一个应用中获取一个健康的主节点
	 *
	 * @param appId
	 * @return
	 */
	public InstanceInfo getHealthyInstanceInfo(long appId) {
		InstanceInfo sourceMasterInstance = null;
		List<InstanceInfo> appInstanceInfoList = instanceDao.getInstListByAppId(appId);
		if (CollectionUtils.isEmpty(appInstanceInfoList)) {
			logger.error("appId {} has not instances", appId);
			return null;
		}
		for (InstanceInfo instanceInfo : appInstanceInfoList) {
			int instanceType = instanceInfo.getType();
			if (!TypeUtil.isRedisCluster(instanceType)) {
				continue;
			}
			final String host = instanceInfo.getIp();
			final int port = instanceInfo.getPort();
			if (instanceInfo.getStatus() != InstanceStatusEnum.GOOD_STATUS.getStatus()) {
				continue;
			}
			boolean isRun = isRun(host, port);
			if (!isRun) {
				logger.warn("{}:{} is not run", host, port);
				continue;
			}
			boolean isMaster = isMaster(host, port);
			if (!isMaster) {
				logger.warn("{}:{} is not master", host, port);
				continue;
			}
			sourceMasterInstance = instanceInfo;
			break;
		}
		return sourceMasterInstance;
	}

	/**
	 * clusterslots命令拼接成Map<Integer slot, String host:port>
	 *
	 * @param host
	 * @param port
	 * @return
	 */
	private Map<Integer, String> getSlotsHostPortMap(String host, int port) {
		Map<Integer, String> slotHostPortMap = new HashMap<Integer, String>();
		Jedis jedis = null;
		try {
			jedis = getAuthJedis(host, port);
			List<Object> slots = jedis.clusterSlots();
			for (Object slotInfoObj : slots) {
				List<Object> slotInfo = (List<Object>) slotInfoObj;
				if (slotInfo.size() <= 2) {
					continue;
				}
				List<Integer> slotNums = getAssignedSlotArray(slotInfo);

				// hostInfos
				List<Object> hostInfos = (List<Object>) slotInfo.get(2);
				if (hostInfos.size() <= 0) {
					continue;
				}
				HostAndPort targetNode = generateHostAndPort(hostInfos);

				for (Integer slot : slotNums) {
					slotHostPortMap.put(slot, targetNode.getHost() + ":" + targetNode.getPort());
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return slotHostPortMap;
	}

	private HostAndPort generateHostAndPort(List<Object> hostInfos) {
		return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)), ((Long) hostInfos.get(1)).intValue());
	}

	private List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
		List<Integer> slotNums = new ArrayList<Integer>();
		for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1)).intValue(); slot++) {
			slotNums.add(slot);
		}
		return slotNums;
	}

	@Override
	public List<Integer> getClusterLossSlots(String host, int port) {
		InstanceInfo instanceInfo = instanceDao.getAllInstByIpAndPort(host, port);
		if (instanceInfo == null) {
			logger.warn("{}:{} instanceInfo is null", host, port);
			return Collections.emptyList();
		}
		if (!TypeUtil.isRedisCluster(instanceInfo.getType())) {
			logger.warn("{}:{} is not rediscluster type", host, port);
			return Collections.emptyList();
		}
		List<Integer> clusterLossSlots = new ArrayList<Integer>();
		Jedis jedis = null;
		try {
			jedis = getAuthJedis(host, port, 5000);
			String clusterNodes = jedis.clusterNodes();
			if (StringUtils.isBlank(clusterNodes)) {
				throw new RuntimeException(host + ":" + port + "clusterNodes is null");
			}
			Set<Integer> allSlots = new LinkedHashSet<Integer>();
			for (int i = 0; i <= 16383; i++) {
				allSlots.add(i);
			}

			// 解析
			ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();
			for (String nodeInfo : clusterNodes.split("\n")) {
				if (StringUtils.isNotBlank(nodeInfo) && !nodeInfo.contains("disconnected")) {
					ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(nodeInfo, new HostAndPort(host, port));
					List<Integer> availableSlots = clusterNodeInfo.getAvailableSlots();
					for (Integer slot : availableSlots) {
						allSlots.remove(slot);
					}
				}
			}
			clusterLossSlots = new ArrayList<Integer>(allSlots);
		} catch (Exception e) {
			logger.error("getClusterLossSlots: " + e.getMessage(), e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return clusterLossSlots;
	}

	@Override
	public List<Integer> getInstanceSlots(String healthHost, int healthPort, String lossSlotsHost, int lossSlotsPort) {
		InstanceInfo instanceInfo = instanceDao.getAllInstByIpAndPort(healthHost, healthPort);
		if (instanceInfo == null) {
			logger.warn("{}:{} instanceInfo is null", healthHost, healthPort);
			return Collections.emptyList();
		}
		if (!TypeUtil.isRedisCluster(instanceInfo.getType())) {
			logger.warn("{}:{} is not rediscluster type", healthHost, healthPort);
			return Collections.emptyList();
		}
		List<Integer> clusterLossSlots = new ArrayList<Integer>();
		Jedis jedis = null;
		try {
			jedis = getAuthJedis(healthHost, healthPort, 5000);
			String clusterNodes = jedis.clusterNodes();
			if (StringUtils.isBlank(clusterNodes)) {
				throw new RuntimeException(healthHost + ":" + healthPort + "clusterNodes is null");
			}
			// 解析
			ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();
			for (String nodeInfo : clusterNodes.split("\n")) {
				if (StringUtils.isNotBlank(nodeInfo) && nodeInfo.contains("disconnected") && nodeInfo.contains(lossSlotsHost + ":" + lossSlotsPort)) {
					ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(nodeInfo, new HostAndPort(healthHost, healthPort));
					clusterLossSlots = clusterNodeInfo.getAvailableSlots();
				}
			}
		} catch (Exception e) {
			logger.error("getClusterLossSlots: " + e.getMessage(), e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return clusterLossSlots;
	}

	public void destory() {
		for (JedisPool jedisPool : jedisPoolMap.values()) {
			jedisPool.destroy();
		}
	}

	@Override
	public boolean deployRedisSlowLogCollection(long appId, String host, int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put(ConstUtils.HOST_KEY, host);
		dataMap.put(ConstUtils.PORT_KEY, port);
		dataMap.put(ConstUtils.APP_KEY, appId);
		JobKey jobKey = JobKey.jobKey(ConstUtils.REDIS_SLOWLOG_JOB_NAME, ConstUtils.REDIS_SLOWLOG_JOB_GROUP);
		TriggerKey triggerKey = TriggerKey.triggerKey(ObjectConvert.linkIpAndPort(host, port), ConstUtils.REDIS_SLOWLOG_TRIGGER_GROUP + appId);
		boolean result = schedulerCenter.deployJobByCron(jobKey, triggerKey, dataMap, ScheduleUtil.getRandomHourCron(appId), false);
		return result;
	}

	@Override
	public boolean unDeployRedisSlowLogCollection(long appId, String host, int port) {
		Assert.isTrue(appId > 0);
		Assert.hasText(host);
		Assert.isTrue(port > 0);
		TriggerKey triggerKey = TriggerKey.triggerKey(ObjectConvert.linkIpAndPort(host, port), ConstUtils.REDIS_SLOWLOG_TRIGGER_GROUP + appId);
		Trigger trigger = schedulerCenter.getTrigger(triggerKey);
		if (trigger == null) {
			return true;
		}
		return schedulerCenter.unscheduleJob(triggerKey);
	}

	@Override
	public List<InstanceSlowLog> getInstanceSlowLogByAppId(long appId) {
		try {
			return instanceSlowLogDao.getByAppId(appId);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<InstanceSlowLog> getInstanceSlowLogByAppId(long appId, Date startDate, Date endDate) {
		try {
			return instanceSlowLogDao.search(appId, startDate, endDate);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, Long> getInstanceSlowLogCountMapByAppId(Long appId, Date startDate, Date endDate) {
		try {
			List<Map<String, Object>> list = instanceSlowLogDao.getInstanceSlowLogCountMapByAppId(appId, startDate, endDate);
			if (CollectionUtils.isEmpty(list)) {
				return Collections.emptyMap();
			}
			Map<String, Long> resultMap = new LinkedHashMap<String, Long>();
			for (Map<String, Object> map : list) {
				long count = MapUtils.getLongValue(map, "count");
				String hostPort = MapUtils.getString(map, "hostPort");
				if (StringUtils.isNotBlank(hostPort)) {
					resultMap.put(hostPort, count);
				}
			}
			return resultMap;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	@Override
	public boolean isSentinelNode(final String ip, final int port) {
		boolean isRun = new IdempotentConfirmer() {
			private int timeOutFactor = 1;

			@Override
			public boolean execute() {
				Jedis jedis = getAuthJedis(ip, port);
				try {
					jedis.getClient().setConnectionTimeout(Protocol.DEFAULT_TIMEOUT * (timeOutFactor++));
					jedis.getClient().setSoTimeout(Protocol.DEFAULT_TIMEOUT * (timeOutFactor++));
					String info = jedis.info(RedisConstant.Server.getValue());
					Map<RedisConstant, Map<String, Object>> infoMap = processRedisStats(info);
					Map<String, Object> map = infoMap.get(RedisConstant.Server);
					String redisMode = MapUtils.getString(map, "redis_mode", null);
					return redisMode != null && redisMode.equalsIgnoreCase("sentinel");
				} catch (Exception e) {
					logger.warn("{}:{} error message is {} ", ip, port, e.getMessage());
					return false;
				} finally {
					jedis.close();
				}
			}
		}.run();
		return isRun;
	}

	public void setSchedulerCenter(SchedulerCenter schedulerCenter) {
		this.schedulerCenter = schedulerCenter;
	}

	public void setInstanceStatsCenter(InstanceStatsCenter instanceStatsCenter) {
		this.instanceStatsCenter = instanceStatsCenter;
	}

	public void setAppStatsDao(AppStatsDao appStatsDao) {
		this.appStatsDao = appStatsDao;
	}

	public void setAsyncService(AsyncService asyncService) {
		this.asyncService = asyncService;
	}

	public void setInstanceDao(InstanceDao instanceDao) {
		this.instanceDao = instanceDao;
	}

	public void setMachineCenter(MachineCenter machineCenter) {
		this.machineCenter = machineCenter;
	}

	public void setAppDao(AppDao appDao) {
		this.appDao = appDao;
	}

	public void setAppAuditLogDao(AppAuditLogDao appAuditLogDao) {
		this.appAuditLogDao = appAuditLogDao;
	}

	public void setInstanceSlowLogDao(InstanceSlowLogDao instanceSlowLogDao) {
		this.instanceSlowLogDao = instanceSlowLogDao;
	}

	public void setGroupDao(GroupDao groupDao) {
		this.groupDao = groupDao;
	}

	public void setInstanceFaultDao(InstanceFaultDao instanceFaultDao) {
		this.instanceFaultDao = instanceFaultDao;
	}

	public SqlSessionFactory getMysqlSessionFactory() {
		return mysqlSessionFactory;
	}

	public void setMysqlSessionFactory(SqlSessionFactory mysqlSessionFactory) {
		this.mysqlSessionFactory = mysqlSessionFactory;
	}

	public void setRedisQueueHelper(RedisQueueHelper redisQueueHelper) {
		this.redisQueueHelper = redisQueueHelper;
	}

	public MobileAlertComponent getMobileAlert() {
		return mobileAlert;
	}

	public void setMobileAlert(MobileAlertComponent mobileAlert) {
		this.mobileAlert = mobileAlert;
	}

	public void setInstanceStatsDao(InstanceStatsDao instanceStatsDao) {
		this.instanceStatsDao = instanceStatsDao;
	}

}