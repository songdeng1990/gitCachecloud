package com.sohu.cache.redis;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.IdempotentConfirmer;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;

public class RedisUtil {
	public static Logger logger = LoggerFactory.getLogger(RedisUtil.class);

	public static boolean isRun(final String ip, final int port, final String password) {
		boolean isRun = new IdempotentConfirmer() {
			private int timeOutFactor = 1;

			@Override
			public boolean execute() {
				Jedis jedis = new Jedis(ip, port);
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
	
	public static int getClusterSize(String ip,int port) {
		Jedis jedis = null;
		try {
			HostAndPort addr = new HostAndPort(ip, port);
			jedis = new Jedis(ip, port);
			String clusterInfo = jedis.clusterInfo();
			for (String line : clusterInfo.split("\r\n")) {
				if (line.contains("cluster_known_nodes")) {
					String[] tmpArray = line.split(":");
					int size = Integer.parseInt(tmpArray[1]);
					return size;
				}
			}
		} catch (Exception e) {
			logger.error("exception happened for connecting to {} {} ", ip,port, e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return -1;
	}
	
	public static void main(String[] args) {
		System.out.println(hardKillRedis("192.168.52.116", 16381));
	}

	public static boolean isMaster(String ip, int port) {
		Jedis jedis = null;
		try {
			jedis = new Jedis(ip, port);
			return "".equals(jedis.configGet("slaveof").get(1));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public static boolean forgetNode(final String host, final int port, final String forgetIp, final int forgetPort) {
		final Jedis jedis = new Jedis(host, port);
		try {
			final StringBuilder clusterNode = new StringBuilder();
			boolean isGetNodes = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					String nodes = jedis.clusterNodes();
					if (nodes != null && nodes.length() > 0) {
						String[] array = nodes.split("\n");
						for (String node : array) {
							if (node.contains(forgetIp + ":" + forgetPort)) {
								clusterNode.append(node);
								return true;
							}
						}
						return false;
					}
					return true;
				}
			}.run();

			if (!isGetNodes) {
				logger.warn("Didn't find node {}:{} in {}:{} ", forgetIp, forgetPort, host, port);
				return true;
			}
			String nodeInfo = clusterNode.toString();
			ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();
			String nodeId = nodeInfoParser.getNodeId(nodeInfo);
			jedis.clusterForget(nodeId);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return true;
	}
	
	
	public static boolean hasSlots(String ip,int port,final String forgetIp, final int forgetPort) {
		final Jedis jedis = new Jedis(ip, port);
		try {
			final StringBuilder clusterNode = new StringBuilder();
			boolean isGetNodes = new IdempotentConfirmer() {
				@Override
				public boolean execute() {
					String nodes = jedis.clusterNodes();
					if(nodes.startsWith("LOADING")){
						clusterNode.append(nodes);
						return true;
					}
					if (nodes != null && nodes.length() > 0) {
						String[] array = nodes.split("\n");
						for (String node : array) {
							if (node.contains(forgetIp + ":" + forgetPort)) {
								clusterNode.append(node);
								return true;
							}
						}
						return false;
					}
					return true;
				}
			}.run();

			if (!isGetNodes) {
				throw new RuntimeException(String.format("%s:%s Execute 'cluster nodes' failed。",ip,port));
			}
			
			String nodeInfo = clusterNode.toString();
			
			if(nodeInfo.startsWith("LOADING")){
				throw new RuntimeException(String.format("%s:%s is loading dataset,please wait。",ip,port));
			}
			
			ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();
			ClusterNodeInformation info = nodeInfoParser.parse(nodeInfo, new HostAndPort(forgetIp,forgetPort));
			
			return !(info.getAvailableSlots().isEmpty() && info.getSlotsBeingImported().isEmpty() && info.getSlotsBeingMigrated().isEmpty());
			
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public static boolean hardKillRedis(String ip, int port) {
		try {
			String cmd = String.format("pid=`ps -elf | grep %s:%s | grep -v grep | awk '{print $4}'`;kill -9 $pid", ip, String.valueOf(port));
			String msg = SSHUtil.execute(ip, cmd);
			if(StringUtils.isNotEmpty(msg)){
				logger.warn(msg);
			}
			return true;
		} catch (Exception e){
			logger.error("", e);
		}
		return false;
	}
	
	public static boolean isPersistenceDisabled(Jedis jedis){
		String saveValue = jedis.configGet("save").get(1);
		String appendonly = jedis.configGet("appendonly").get(1);
		return saveValue.equals("") && appendonly.equals("no");
	}

}
