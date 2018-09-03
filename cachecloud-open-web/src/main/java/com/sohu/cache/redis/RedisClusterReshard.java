package com.sohu.cache.redis;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.cache.async.NamedThreadFactory;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.IdempotentConfirmer;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformation.ImportingSlot;
import redis.clients.util.ClusterNodeInformation.MigratingSlot;
import redis.clients.util.ClusterNodeInformationParser;

/**
 *
 * Created by yijunzhang on 14-9-4.
 */
public class RedisClusterReshard {
	private static final Logger logger = LoggerFactory.getLogger(RedisClusterReshard.class);

	private final Map<String, HostAndPort> nodeMap = new LinkedHashMap<String, HostAndPort>();

	private int migrateTimeout = 10000;

	private static int defaultTimeout = Protocol.DEFAULT_TIMEOUT * 5;

	private final ReshardProcess reshardProcess;

	private int migrateBatch = 100;

	private final Set<HostAndPort> hosts;

	private static final int allSlots = 16384;

	static {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
	}

	public RedisClusterReshard(Set<HostAndPort> hosts) {
		for (HostAndPort host : hosts) {
			String key = JedisClusterInfoCache.getNodeKey(host);
			nodeMap.put(key, host);
		}
		this.hosts = hosts;
		reshardProcess = new ReshardProcess();
	}

	private boolean isInCluster(Jedis jedis, List<ClusterNodeInformation> masterNodes) {
		for (ClusterNodeInformation info : masterNodes) {
			String nodeKey = getNodeKey(info.getNode());
			String jedisKey = getNodeKey(jedis);
			if (nodeKey.equals(jedisKey)) {
				return true;
			}
		}
		return false;
	}

	protected static String getNodeKey(HostAndPort hnp) {
		return hnp.getHost() + ":" + hnp.getPort();
	}

	protected String getNodeKey(Jedis jedis) {
		return jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
	}

	public static List<ClusterNodeInformation> getMasterNodes(Set<HostAndPort> hosts) {
		Map<String, ClusterNodeInformation> masterNodeMap = new LinkedHashMap<String, ClusterNodeInformation>();
		JedisCluster jedisCluster = new JedisCluster(hosts, defaultTimeout);
		// 所有节点
		Collection<JedisPool> allNodes = jedisCluster.getConnectionHandler().getNodes().values();
		try {
			for (JedisPool jedisPool : allNodes) {
				final String host = jedisPool.getHost();
				final int port = jedisPool.getPort();
				final Jedis jedis = getJedis(host, port, defaultTimeout);
				if (!isMaster(jedis)) {
					continue;
				}
				try {
					final StringBuilder clusterNodes = new StringBuilder();
					boolean isGetNodes = new IdempotentConfirmer() {
						@Override
						public boolean execute() {
							String nodes = jedis.clusterNodes();
							if (nodes != null && nodes.length() > 0) {
								String[] array = nodes.split("\n");
								for (String node : array) {
									if (node.contains(host + ":" + port)) {
										clusterNodes.append(node);
									}
								}
								return true;
							}
							return false;
						}
					}.run();
					if (!isGetNodes) {
						logger.error("clusterNodes" + failedInfo(jedis, -1));
						continue;
					}
					String nodeInfo = clusterNodes.toString();
					if (StringUtils.isNotBlank(nodeInfo)) {
						ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();
						ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(nodeInfo, new HostAndPort(jedis.getClient().getHost(), jedis.getClient().getPort()));
						masterNodeMap.put(getNodeKey(clusterNodeInfo.getNode()), clusterNodeInfo);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				} finally {
					jedis.close();
				}
			}
		} finally {
			jedisCluster.close();
		}
		List<ClusterNodeInformation> resultList = new ArrayList<ClusterNodeInformation>(masterNodeMap.values());
		// 按slot大小排序
		Collections.sort(resultList, new Comparator<ClusterNodeInformation>() {
			@Override
			public int compare(ClusterNodeInformation node1, ClusterNodeInformation node2) {
				if (node1 == node2) {
					return 0;
				}
				int slotNum1 = 0;
				int slotNum2 = 0;
				List<Integer> slots1 = node1.getAvailableSlots();
				for (Integer slot : slots1) {
					slotNum1 += slot;
				}
				List<Integer> slots2 = node2.getAvailableSlots();
				for (Integer slot : slots2) {
					slotNum2 += slot;
				}
				if (slots2.isEmpty()) {
					if (slots1.isEmpty()) {
						return 0;
					} else {
						return 1;
					}
				}
				if (slots1.isEmpty()) {
					if (slots2.isEmpty()) {
						return 0;
					} else {
						return -1;
					}
				}
				slotNum1 = slotNum1 / slots1.size();
				slotNum2 = slotNum2 / slots2.size();
				if (slotNum1 == slotNum2) {
					return 0;
				} else if (slotNum1 > slotNum2) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		return resultList;
	}

	private static boolean isMaster(final Jedis jedis) {
		return new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String replications = jedis.info("Replication");
				if (StringUtils.isNotBlank(replications)) {
					String[] data = replications.split("\r\n");
					for (String line : data) {
						String[] arr = line.split(":");
						if (arr.length > 1) {
							String value = arr[1];
							if (value.equalsIgnoreCase("master")) {
								return true;
							}
						}
					}
				}
				return false;
			}
		}.run();
	}

	public boolean offLineMaster(String host, int port) {
		long begin = System.currentTimeMillis();
		reshardProcess.setType(1);

		ClusterNodeInformation offNode = null;
		List<ClusterNodeInformation> masterNodes = getMasterNodes(hosts);
		checkNodeHealth(host, port, masterNodes);
		for (ClusterNodeInformation nodeInfo : masterNodes) {
			if (nodeInfo.getNode().getHost().equals(host) && nodeInfo.getNode().getPort() == port) {
				offNode = nodeInfo;
				break;
			}
		}

		if (offNode == null) {
			throw new JedisException(String.format("clusterReshard:host=%s,port=%s not find in masters", host, port));
		}
		List<Integer> slots = new ArrayList<Integer>(offNode.getAvailableSlots());
		if (slots.isEmpty()) {
			logger.warn(String.format("clusterReshard:host=%s,port=%s slots is null", host, port));
			reshardProcess.setStatus(1);
			return true;
		}
		// 设置slots数量
		reshardProcess.setTotalSlot(slots.size());
		List<ClusterNodeInformation> allocatNodes = new ArrayList<ClusterNodeInformation>(masterNodes);
		for (Iterator<ClusterNodeInformation> i = allocatNodes.iterator(); i.hasNext();) {
			ClusterNodeInformation nodeInfo = i.next();
			if (nodeInfo.getNode().getHost().equals(host) && nodeInfo.getNode().getPort() == port) {
				// 移除自身
				i.remove();
			}
		}

		Map<String, Integer> balanceSlotMap = getBalanceSlotMap(allocatNodes, false);

		JedisPool srcJedisPool = getJedisPool(new HostAndPort(host, port), ConstUtils.RESHARD_THREAD_NUM + 1);

		for (ClusterNodeInformation nodeInfo : allocatNodes) {
			ThreadPoolExecutor pool = getBlockingThreadPool(ConstUtils.RESHARD_THREAD_NUM);
			String nodeKey = getNodeKey(nodeInfo.getNode());
			Integer thresholdSize = balanceSlotMap.get(nodeKey);
			if (thresholdSize == null || thresholdSize == 0) {
				continue;
			}
			JedisPool dstJedisPool = null;
			try {
				dstJedisPool = getJedisPool(nodeInfo.getNode(), ConstUtils.RESHARD_THREAD_NUM + 1);
				int moveSize = 0;
				for (Iterator<Integer> i = slots.iterator(); i.hasNext();) {
					final Integer slot = i.next();
					i.remove();
					logger.info("startMigrateSlot={}", slot);
					if (moveSize++ >= thresholdSize) {
						break;
					}
					oneSlotMoveByThreadPool(pool, srcJedisPool, dstJedisPool, slot);
				}
			} finally {

				waitAlltaskToFinish(pool);
				if (dstJedisPool != null) {
					dstJedisPool.close();
				}
			}
		}
		srcJedisPool.close();
		long end = System.currentTimeMillis();
		logger.warn("{}:{} joinNewMaster cost:{} ms", host, port, (end - begin));

		return getReshardResult();
	}

	private static Jedis getJedis(String host, int port, int timeout) {
		return new Jedis(host, port, timeout);
	}

	/**
	 * 返回平衡的迁移slot数量
	 *
	 */
	private Map<String, Integer> getBalanceSlotMap(List<ClusterNodeInformation> allocatNodes, boolean isAdd) {
		return getBalanceSlotMap(allocatNodes, isAdd, 1);
	}

	public static Map<String, Integer> getBalanceSlotMap(List<ClusterNodeInformation> allocatNodes, boolean isAdd, int addNodeCount) {
		int nodeSize = allocatNodes.size();
		int perSize = (int) Math.ceil((double) allSlots / nodeSize);
		Map<String, Integer> moveSlotMap = new HashMap<String, Integer>();
		Map<String, Integer> finalSlotMap = new HashMap<String, Integer>();

		for (int i = 0; i < allocatNodes.size(); i++) {
			if (i == (allocatNodes.size() - 1)) {
				finalSlotMap.put(getNodeKey(allocatNodes.get(i).getNode()), perSize + allSlots % nodeSize);
			}
			finalSlotMap.put(getNodeKey(allocatNodes.get(i).getNode()), perSize);
		}

		for (ClusterNodeInformation node : allocatNodes) {
			String key = getNodeKey(node.getNode());
			int balanceSize;
			int slotSize = finalSlotMap.get(key);
			if (isAdd) {
				balanceSize = node.getAvailableSlots().size() - slotSize;
			} else {
				balanceSize = slotSize - node.getAvailableSlots().size();
			}
			moveSlotMap.put(key, balanceSize);
		}

		return moveSlotMap;
	}

	/**
	 * 把cluster中一个节点的数据全部迁移到另外一个节点，采用slot迁移的方式
	 * 
	 * @param srcNode
	 * @param dstNode
	 * @param concurrencyNum
	 * @return
	 */
	public boolean migrateNode(HostAndPort srcNode, HostAndPort dstNode, int concurrencyNum) {
		long begin = System.currentTimeMillis();
		reshardProcess.setType(2);
		List<ClusterNodeInformation> masterNodes = getMasterNodes(hosts);
		checkNodeHealth(srcNode.getHost(), srcNode.getPort(), masterNodes);
		checkNodeHealth(dstNode.getHost(), dstNode.getPort(), masterNodes);
		ClusterNodeInformation srcNodeInfo = getNodeInfo(srcNode.getHost(), srcNode.getPort(), masterNodes);

		List<Integer> slots = srcNodeInfo.getAvailableSlots();
		if (slots.isEmpty()) {
			logger.warn(String.format("clusterReshard:host=%s,port=%s slots is null", srcNode.getHost(), srcNode.getPort()));
			reshardProcess.setStatus(1);
			return true;
		}

		reshardProcess.setTotalSlot(slots.size());

		ThreadPoolExecutor pool = getBlockingThreadPool(concurrencyNum);

		// 因为当前运行县城也算一个worker，所以连接数加1
		final JedisPool srcJedisPool = getJedisPool(srcNode, concurrencyNum + 1);
		final JedisPool dstJeidsPool = getJedisPool(dstNode, concurrencyNum + 1);

		for (final Integer slot : slots) {
			oneSlotMoveByThreadPool(pool, srcJedisPool, dstJeidsPool, slot);
		}

		waitAlltaskToFinish(pool);
		long end = System.currentTimeMillis();
		logger.warn("{}->{} migrate cost:{} ms", srcNode.toString(), dstNode.toString(), (end - begin));

		return getReshardResult();
	}

	private void oneSlotMoveByThreadPool(ThreadPoolExecutor pool, final JedisPool srcJedisPool, final JedisPool dstJedisPool, final int slot) {
		pool.execute(new MoveSlotTask(srcJedisPool, dstJedisPool, slot));
	}

	class MoveSlotTask implements Runnable {

		private JedisPool srcJedisPool;
		private JedisPool dstJedisPool;
		private int slot;

		public MoveSlotTask(JedisPool srcJedisPool, JedisPool dstJedisPool, int slot) {
			this.srcJedisPool = srcJedisPool;
			this.dstJedisPool = dstJedisPool;
			this.slot = slot;
		}

		@Override
		public void run() {
			Jedis src = null;
			Jedis dst = null;
			try {
				src = srcJedisPool.getResource();
				dst = dstJedisPool.getResource();
				int num = migrateSlotData(src, dst, slot);
				reshardProcess.addReshardSlot(slot, num);
			} catch (Exception e) {
				logger.error("", e);
				reshardProcess.setStatus(2);
			} finally {
				if (src != null) {
					src.close();
				}
				if (dst != null) {
					dst.close();
				}
			}
		}

	}

	private JedisPool getJedisPool(HostAndPort node, int size) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxIdle(size);
		config.setMaxTotal(size);
		return new JedisPool(config, node.getHost(), node.getPort());
	}

	/**
	 * 如果提交的任务时发现当前线程池所有线程都在运行，则当前线程阻塞。
	 * 
	 * @param concurrencyNum
	 * @return
	 */
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

	private void waitAlltaskToFinish(ThreadPoolExecutor pool) {
		pool.shutdown();
		try {
			pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("", e);
		}
	}

	private void checkNodeHealth(String host, int port, List<ClusterNodeInformation> masterNodes) {
		final Jedis jedis = getJedis(host, port, defaultTimeout);
		boolean isRun = isRun(jedis);
		if (!isRun) {
			reshardProcess.setStatus(2);
			throw new JedisException(String.format("clusterReshard:host=%s,port=%s is not run", host, port));
		}
		if (!isInCluster(jedis, masterNodes)) {
			reshardProcess.setStatus(2);
			throw new JedisException(String.format("clusterReshard:host=%s,port=%s not inCluster", host, port));
		}

		checkAndMovingSlot(ConstUtils.RESHARD_THREAD_NUM, masterNodes);
		if (!checkMigrationgFinished()) {
			throw new JedisException(String.format("Redis Cluster Migration is not finished,this migration aborted for data safety.", host, port));
		}
	}

	private ClusterNodeInformation getNodeInfo(String host, int port, List<ClusterNodeInformation> masterNodes) {
		ClusterNodeInformation node = null;
		for (ClusterNodeInformation nodeInfo : masterNodes) {
			if (nodeInfo.getNode().getHost().equals(host) && nodeInfo.getNode().getPort() == port) {
				node = nodeInfo;
			}
		}

		return node;
	}

	/**
	 * 加入主从分片
	 */
	public boolean joinCluster(String masterHost, int masterPort, final String slaveHost, final int slavePort) {
		final Jedis masterJedis = getJedis(masterHost, masterPort, defaultTimeout);
		boolean isRun = isRun(masterJedis);
		if (!isRun) {
			logger.error(String.format("joinCluster:host=%s,port=%s is not run", masterHost, masterPort));
			return false;
		}
		boolean hasSlave = StringUtils.isNotBlank(slaveHost) && slavePort > 0;
		final Jedis slaveJedis = hasSlave ? getJedis(slaveHost, slavePort, defaultTimeout) : null;
		if (hasSlave) {
			isRun = isRun(slaveJedis);
			if (!isRun) {
				logger.error(String.format("joinCluster:host=%s,port=%s is not run", slaveHost, slavePort));
				return false;
			}
		}

		List<ClusterNodeInformation> masterNodes = getMasterNodes(this.hosts);
		if (!isInCluster(masterJedis, masterNodes)) {
			boolean isClusterMeet = clusterMeet(masterNodes, masterHost, masterPort);
			if (!isClusterMeet) {
				logger.error("isClusterMeet failed {}:{}", masterHost, masterPort);
				return false;
			}
		}
		if (hasSlave) {
			if (!isInCluster(slaveJedis, masterNodes)) {
				boolean isClusterMeet = clusterMeetMaster(masterHost,masterPort, slaveHost, slavePort);
				if (!isClusterMeet) {
					logger.error("isClusterMeet failed {}:{}", slaveHost, slavePort);
					return false;
				}
			}
		}
		if (hasSlave) {
			final String masterNodeId = getNodeId(masterJedis);
			if (masterNodeId == null) {
				logger.error(String.format("joinCluster :host=%s,port=%s nodeId is null", masterHost, masterPort));
				return false;
			}
			return new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					try {
						// 等待广播节点
						TimeUnit.SECONDS.sleep(2);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					String response = slaveJedis.clusterReplicate(masterNodeId);
					logger.info("clusterReplicate-{}:{}={}", slaveHost, slavePort, response);
					return response != null && response.equalsIgnoreCase("OK");
				}
			}.run();
		} else {
			return true;
		}
	}

	//slave 必须meetmaster，以为后续slave replicate 的时候必须知道master的nodeId，如果meet的其它节点，有一定概率slave还没来得及知道master nodeId，导致失败。
	private boolean clusterMeetMaster(final String masterHost, final int masterPort, final String host, final int port) {
		if (!isSingleClusterNode(host, port)) {
			logger.error(host + port + " This is not  a single redis node , can't join in a new cluster.");
			return false;
		}

		final Jedis jedis = new Jedis(masterHost, masterPort, defaultTimeout);
		try {
			boolean isClusterMeet = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					// 将新节点添加到集群当中,成为集群中已知新节点
					String meet = jedis.clusterMeet(host, port);
					return meet != null && meet.equalsIgnoreCase("OK");
				}
			}.run();
			if (isClusterMeet) {
				return true;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}finally {
			if (jedis != null){
				jedis.close();
			}
		}

		return false;
	}

	private boolean clusterMeet(List<ClusterNodeInformation> masterNodes, final String host, final int port) {
		if (!isSingleClusterNode(host, port)) {
			logger.error(host + port + " This is not  a single redis node , can't join in a new cluster.");
			return false;
		}
		for (ClusterNodeInformation info : masterNodes) {
			String clusterHost = info.getNode().getHost();
			int clusterPort = info.getNode().getPort();
			final Jedis jedis = new Jedis(clusterHost, clusterPort, defaultTimeout);
			try {
				boolean isClusterMeet = new IdempotentConfirmer() {
					@Override
					public boolean execute() {
						// 将新节点添加到集群当中,成为集群中已知新节点
						String meet = jedis.clusterMeet(host, port);
						return meet != null && meet.equalsIgnoreCase("OK");
					}
				}.run();
				if (isClusterMeet) {
					return true;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return false;
	}

	private boolean isSingleClusterNode(String host, int port) {
		final Jedis jedis = new Jedis(host, port);
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

	private boolean isRun(final Jedis jedis) {
		return new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String pong = jedis.ping();
				return pong != null && pong.equalsIgnoreCase("PONG");
			}
		}.run();
	}

	/**
	 * 对新加入的节点 尽心solt重分配
	 * 
	 * @param machines
	 *            泛型string的格式为ip:port
	 */
	public boolean joinNewMaster(final String host, final int port) {
		logger.warn("{}:{} joinNewMaster start:{} ms", host, port, new Date());
		long begin = System.currentTimeMillis();
		reshardProcess.setType(0);
		List<ClusterNodeInformation> masterNodes = getMasterNodes(hosts);
		checkNodeHealth(host, port, masterNodes);

		final Map<String, Integer> balanceSlotMap = getBalanceSlotMap(masterNodes, true, 1);
		// 设置总slots数量
		int totalSlot = balanceSlotMap.get(host + ":" + port);

		if (totalSlot >= 0) {
			logger.warn("The slot num of {} {} isn't less than average ,no need to add slot.", host, port);
			reshardProcess.setStatus(1);
			return true;
		}
		reshardProcess.setTotalSlot(Math.abs(totalSlot));
		final AtomicInteger counter = new AtomicInteger(totalSlot);
		ThreadPoolExecutor globalPool = getBlockingThreadPool(masterNodes.size());

		for (final ClusterNodeInformation nodeInfo : masterNodes) {
			globalPool.execute(new Runnable() {

				@Override
				public void run() {

					if (getNodeKey(nodeInfo.getNode()).equals(host + ":" + port)) {
						return;
					}
					Integer moveSlot = balanceSlotMap.get(getNodeKey(nodeInfo.getNode()));
					if (moveSlot == null || moveSlot <= 0) {
						return;
					}

					ThreadPoolExecutor pool = getBlockingThreadPool(ConstUtils.RESHARD_THREAD_NUM);
					final JedisPool dstJedisPool = getJedisPool(new HostAndPort(host, port), ConstUtils.RESHARD_THREAD_NUM + 1);
					JedisPool srcJedisPool = null;
					int index = 0;
					try {
						srcJedisPool = getJedisPool(nodeInfo.getNode(), ConstUtils.RESHARD_THREAD_NUM + 1);
						List<Integer> slots = nodeInfo.getAvailableSlots();
						for (Integer slot : slots) {
							if (index++ >= moveSlot || counter.incrementAndGet() > 0) {
								break;
							}
							oneSlotMoveByThreadPool(pool, srcJedisPool, dstJedisPool, slot);
						}

					} catch (Exception e) {
						logger.error("{} -> {} migrate error", nodeInfo.getNode(), host + ":" + port, e);
					} finally {
						waitAlltaskToFinish(pool);
						if (srcJedisPool != null) {
							srcJedisPool.close();
						}

						if (dstJedisPool != null) {
							dstJedisPool.close();
						}
					}

				}

			});

		}

		waitAlltaskToFinish(globalPool);

		long end = System.currentTimeMillis();
		logger.warn("{}:{} joinNewMaster end:{} ms", host, port, new Date());
		logger.warn("{}:{} joinNewMaster cost:{} ms", host, port, (end - begin));

		return getReshardResult();
	}

	public boolean getReshardResult() {
		if (!checkMigrationgFinished()) {
			reshardProcess.setStatus(2);
		}

		if (reshardProcess.getStatus() != 2) {
			reshardProcess.setStatus(1);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取还需要秦迁移的slot数目
	 * 
	 * @return
	 */
	public int getUnMigatedSlotsNum(List<ClusterNodeInformation> masterNodes) {
		int count = 0;
		for (ClusterNodeInformation node : masterNodes) {
			count += node.getSlotsBeingMigrated().size();
		}
		return count;
	}

	/**
	 * 判断迁移是否完成，
	 * 
	 * @return
	 */
	public boolean checkMigrationgFinished() {
		List<ClusterNodeInformation> masterNodes = getMasterNodes(hosts);
		for (ClusterNodeInformation node : masterNodes) {
			if (node.getSlotsBeingImported().size() > 0) {
				return false;
			}
			if (node.getSlotsBeingMigrated().size() > 0) {
				return false;
			}
		}
		return true;
	}

	// 检查&导入 moveslot
	/*
	 * public boolean checkAndMovingSlot(int
	 * threadNum,List<ClusterNodeInformation> masterNodes) {
	 * reshardProcess.setType(3); int count = 0; Map<HostAndPort,
	 * List<ImportingSlot>> importedMap = new LinkedHashMap<HostAndPort,
	 * List<ImportingSlot>>(); Map<HostAndPort, List<MigratingSlot>> migratedMap
	 * = new LinkedHashMap<HostAndPort, List<MigratingSlot>>(); for
	 * (ClusterNodeInformation node : masterNodes) { if
	 * (node.getSlotsBeingImported().size() > 0) {
	 * importedMap.put(node.getNode(), node.getSlotsBeingImported()); } if
	 * (node.getSlotsBeingMigrated().size() > 0) {
	 * migratedMap.put(node.getNode(), node.getSlotsBeingMigrated()); count++; }
	 * }
	 * 
	 * if (importedMap.isEmpty() && migratedMap.isEmpty()) { return true; }
	 * 
	 * 
	 * reshardProcess.setTotalSlot(count);
	 * 
	 * ThreadPoolExecutor pool = getBlockingThreadPool(threadNum);
	 * 
	 * for (final HostAndPort hostPort : importedMap.keySet()) {
	 * List<ImportingSlot> importedSlots = importedMap.get(hostPort);
	 * 
	 * for (final ImportingSlot slot : importedSlots) { for (final HostAndPort
	 * subHostPort : migratedMap.keySet()) { List<MigratingSlot> migratedSlots =
	 * migratedMap.get(subHostPort); if (migratedSlots.contains(slot)) {
	 * pool.execute(new Runnable(){
	 * 
	 * @Override public void run() { Jedis source = null; Jedis target = null;
	 * 
	 * try { source = new Jedis(subHostPort.getHost(), subHostPort.getPort(),
	 * defaultTimeout); target = new Jedis(hostPort.getHost(),
	 * hostPort.getPort(), defaultTimeout); int num = moveSlotData(source,
	 * target, slot); reshardProcess.addReshardSlot(slot, num);
	 * logger.warn("beingMoveSlotData:{} -> {} slot={} num={}", subHostPort,
	 * hostPort, slot, num); } catch (Exception e) {
	 * reshardProcess.setStatus(2); logger.error(e.getMessage(), e); }finally{
	 * if (source != null){ source.close(); } if (target != null){
	 * target.close(); } } }
	 * 
	 * });
	 * 
	 * break; }
	 * 
	 * }
	 * 
	 * }
	 * 
	 * }
	 * 
	 * waitAlltaskToFinish(pool);
	 * 
	 * return getReshardResult(); }
	 */

	public boolean checkAndMovingSlot(int threadNum, List<ClusterNodeInformation> masterNodes) {
		logger.warn("start checkAndMovingSlot by handle migrating.");
		reshardProcess.setType(3);
		int count = 0;
		Map<HostAndPort, List<MigratingSlot>> migratedMap = new LinkedHashMap<HostAndPort, List<MigratingSlot>>();
		for (ClusterNodeInformation node : masterNodes) {
			if (node.getSlotsBeingMigrated().size() > 0) {
				migratedMap.put(node.getNode(), node.getSlotsBeingMigrated());
				count = count + node.getSlotsBeingMigrated().size();
			}
		}

		reshardProcess.setTotalSlot(count);
		ThreadPoolExecutor pool = getBlockingThreadPool(threadNum);
		for (final HostAndPort hostPort : migratedMap.keySet()) {
			List<MigratingSlot> migratedSlots = migratedMap.get(hostPort);
			for (final MigratingSlot slot : migratedSlots) {
				final HostAndPort dstHostPort = getAddressByNodeId(masterNodes, slot.getDstNodeId());
				JedisPool srcJedisPool = getJedisPool(hostPort, threadNum);
				JedisPool dstJedisPool = getJedisPool(dstHostPort, threadNum);
				logger.warn("beingMoveSlotData:{} -> {} slot={} ", hostPort, dstHostPort, slot);
				oneSlotMoveByThreadPool(pool, srcJedisPool, dstJedisPool, slot.getSlot());
			}
		}

		waitAlltaskToFinish(pool);
		logger.warn("start checkAndMovingSlot by handle importing.");
		clearOnlyImportingSlot();
		return true;
	}

	private void clearOnlyImportingSlot() {
		List<ClusterNodeInformation> masterNodes = getMasterNodes(hosts);
		for (final ClusterNodeInformation node : masterNodes) {
			if (node.getSlotsBeingImported().size() > 0) {
				HostAndPort dstHostPort = node.getNode();
				for (final ImportingSlot slot : node.getSlotsBeingImported()) {
					HostAndPort srcHostPort = getAddressByNodeId(masterNodes, slot.getSrcNodeId());
					final Jedis target = new Jedis(dstHostPort.getHost(), dstHostPort.getPort(), defaultTimeout);
					final Jedis source = new Jedis(srcHostPort.getHost(), srcHostPort.getPort(), defaultTimeout);

					logger.warn("halfClusterSetSlotNode:{} -> {} slot={} ", srcHostPort, dstHostPort, slot.getSlot());
					// 设置 slot新归属节点
					boolean isClusterSetSlotNode = new IdempotentConfirmer() {
						@Override
						public boolean execute() {
							int slotNum = slot.getSlot();
							String targetNodeId = node.getNodeId();
							List<String> soureHasKey = source.clusterGetKeysInSlot(slotNum, 1);
							List<String> dstHasKey = target.clusterGetKeysInSlot(slotNum, 1);
							boolean isOk = false;
							if (CollectionUtils.isEmpty(soureHasKey) && CollectionUtils.isNotEmpty(dstHasKey)) {
								String response = source.clusterSetSlotNode(slotNum, targetNodeId);
								isOk = response != null && response.equalsIgnoreCase("OK");
								if (isOk) {
									response = target.clusterSetSlotNode(slotNum, targetNodeId);
									isOk = response != null && response.equalsIgnoreCase("OK");
								} else {
									logger.error("clusterSetSlotNode-{}={}", getNodeId(target), response);
								}
								if (!isOk) {
									logger.error("clusterSetSlotNode-{}={}", getNodeId(source), response);
								}
							}

							return isOk;
						}
					}.run();
					if (!isClusterSetSlotNode) {
						throw new RuntimeException("halfClusterSetSlotNode:src " + srcHostPort + " -> dst" + dstHostPort + " slot: " + slot.getSlot());
					}
				}
			}
		}

	}

	private HostAndPort getAddressByNodeId(List<ClusterNodeInformation> masterNodes, String nodeId) {
		for (ClusterNodeInformation info : masterNodes) {
			if (info.getNodeId().equals(nodeId)) {
				return info.getNode();
			}
		}

		return null;
	}

	/**
	 * 迁移slot数据，并稳定slot配置
	 * 
	 * @throws Exception
	 */
	private int moveSlotData(final Jedis source, final Jedis target, final int slot) throws Exception {
		int num = 0;
		while (true) {
			final Set<byte[]> keys = new HashSet<byte[]>();
			boolean isGetKeysInSlot = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					List<byte[]> perKeys = source.clusterGetKeysInSlotByte(slot, migrateBatch);
					if (perKeys != null && perKeys.size() > 0) {
						keys.addAll(perKeys);
					}
					return true;
				}
			}.run();
			if (!isGetKeysInSlot) {
				throw new RuntimeException(String.format("get keys failed slot=%d num=%d", slot, num));
			}
			if (keys.isEmpty()) {
				break;
			}
			for (final byte[] key : keys) {
				boolean isKeyMigrate = new IdempotentConfirmer() {
					// 失败后，迁移时限加倍
					private int migrateTimeOutFactor = 1;

					@Override
					public boolean execute() {
						String response = null;
						try {
							response = source.migrate(target.getClient().getHost().getBytes(Protocol.CHARSET), target.getClient().getPort(), key, 0, migrateTimeout * (migrateTimeOutFactor++));
						} catch (UnsupportedEncodingException e) {
							logger.error("", e);
						}
						return response != null && (response.equalsIgnoreCase("OK")
								|| /* TODO 确认 */ response.equalsIgnoreCase("NOKEY"));
					}
				}.run();
				if (!isKeyMigrate) {
					throw new RuntimeException("migrate key=" + key + failedInfo(source, slot));
				} else {
					num++;
					logger.info("migrate key={};response=OK", key);
				}
			}
		}

		/*
		 * boolean isDelSlots = new IdempotentConfirmer() {
		 * 
		 * @Override public boolean execute() { String response =
		 * source.clusterDelSlots(slot); logger.info("clusterDelSlots-{}:{}={}",
		 * source.getClient().getHost(), source.getClient().getPort(),
		 * response); return response != null &&
		 * response.equalsIgnoreCase("OK"); } }.run(); if (!isDelSlots) { throw
		 * new RuntimeException("clusterDelSlots:" + failedInfo(source, slot));
		 * }
		 */

		final String targetNodeId = getNodeId(target);
		boolean isClusterSetSlotNode;
		// 设置 slot新归属节点
		isClusterSetSlotNode = new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String response = source.clusterSetSlotNode(slot, targetNodeId);
				boolean isOk = response != null && response.equalsIgnoreCase("OK");
				if (isOk) {
					response = target.clusterSetSlotNode(slot, targetNodeId);
					isOk = response != null && response.equalsIgnoreCase("OK");
				} else {
					logger.error("clusterSetSlotNode-{}={}", getNodeId(target), response);
				}
				if (!isOk) {
					logger.error("clusterSetSlotNode-{}={}", getNodeId(source), response);
				}
				return isOk;
			}
		}.run();
		if (!isClusterSetSlotNode) {
			throw new RuntimeException("clusterSetSlotNode:" + failedInfo(target, slot));
		}
		return num;
	}

	/**
	 * 指派迁移节点数据 CLUSTER SETSLOT <slot> IMPORTING <node_id> 从 node_id 指定的节点中导入槽
	 * slot 到本节点。 CLUSTER SETSLOT <slot> MIGRATING <node_id> 将本节点的槽 slot 迁移到
	 * node_id 指定的节点中。 CLUSTER GETKEYSINSLOT <slot> <count> 返回 count 个 slot
	 * 槽中的键。 MIGRATE host port key destination-db timeout [COPY] [REPLACE]
	 * CLUSTER SETSLOT <slot> NODE <node_id> 将槽 slot 指派给 node_id
	 * 指定的节点，如果槽已经指派给另一个节点，那么先让另一个节点删除该槽>，然后再进行指派。
	 */
	private int migrateSlotData(final Jedis source, final Jedis target, final int slot) {
		int num = 0;
		final String sourceNodeId = getNodeId(source);
		final String targetNodeId = getNodeId(target);
		boolean isError = false;
		if (sourceNodeId == null || targetNodeId == null) {
			throw new JedisException(String.format("sourceNodeId = %s || targetNodeId = %s", sourceNodeId, targetNodeId));
		}

		boolean isMigrate = new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String migrating = source.clusterSetSlotMigrating(slot, targetNodeId);
				logger.info("slot={},clusterSetSlotMigrating={}", slot, migrating);
				return migrating != null && migrating.equalsIgnoreCase("OK");
			}
		}.run();

		if (!isMigrate) {
			isError = true;
			logger.error("clusterSetSlotMigrating" + failedInfo(source, slot));
			String errorMessage = "source=%s target=%s slot=%d num=%d reShard failed";
			throw new RuntimeException(String.format(errorMessage, getNodeKey(source), getNodeKey(target), slot, num));
		}

		boolean isImport = new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String importing = target.clusterSetSlotImporting(slot, sourceNodeId);
				logger.info("slot={},clusterSetSlotImporting={}", slot, importing);
				return importing != null && importing.equalsIgnoreCase("OK");
			}
		}.run();
		if (!isImport) {
			isError = true;
			logger.error("clusterSetSlotImporting" + failedInfo(target, slot));
			String errorMessage = "source=%s target=%s slot=%d num=%d reShard failed";
			throw new RuntimeException(String.format(errorMessage, getNodeKey(source), getNodeKey(target), slot, num));
		}

		try {
			num = moveSlotData(source, target, slot);
		} catch (Exception e) {
			isError = true;
			logger.error(e.getMessage(), e);
		}
		if (!isError) {
			return num;
		} else {
			String errorMessage = "source=%s target=%s slot=%d num=%d reShard failed";
			throw new RuntimeException(String.format(errorMessage, getNodeKey(source), getNodeKey(target), slot, num));
		}
	}

	private static String failedInfo(Jedis jedis, int slot) {
		return String.format(" failed %s:%d slot=%d", jedis.getClient().getHost(), jedis.getClient().getPort(), slot);
	}

	private final Map<String, String> nodeIdCachedMap = new HashMap<String, String>();

	public String getNodeId(final Jedis jedis) {
		String nodeKey = getNodeKey(jedis);
		if (nodeIdCachedMap.get(nodeKey) != null) {
			return nodeIdCachedMap.get(nodeKey);
		}
		try {
			final StringBuilder clusterNodes = new StringBuilder();
			boolean isGetNodes = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					String nodes = jedis.clusterNodes();
					if (nodes != null && nodes.length() > 0) {
						clusterNodes.append(nodes);
						return true;
					}
					return false;
				}
			}.run();
			if (!isGetNodes) {
				logger.error("clusterNodes" + failedInfo(jedis, -1));
				return null;
			}
			for (String infoLine : clusterNodes.toString().split("\n")) {
				if (infoLine.contains("myself")) {
					String nodeId = infoLine.split(" ")[0];
					nodeIdCachedMap.put(nodeKey, nodeId);
					return nodeId;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public void setMigrateTimeout(int migrateTimeout) {
		this.migrateTimeout = migrateTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public ReshardProcess getReshardProcess() {
		return reshardProcess;
	}

}
