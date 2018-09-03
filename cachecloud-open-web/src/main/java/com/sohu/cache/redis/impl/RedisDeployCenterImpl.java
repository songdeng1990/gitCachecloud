package com.sohu.cache.redis.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.sohu.cache.alert.impl.BaseAlertService;
import com.sohu.cache.constant.InstanceStatusEnum;
import com.sohu.cache.constant.Result;
import com.sohu.cache.dao.AppDao;
import com.sohu.cache.dao.AppToUserDao;
import com.sohu.cache.dao.AppUserDao;
import com.sohu.cache.dao.InstanceDao;
import com.sohu.cache.dao.MachineDao;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppToUser;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.LoginResponse;
import com.sohu.cache.entity.MachineInfo;
import com.sohu.cache.entity.MachineStats;
import com.sohu.cache.entity.User;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.protocol.MachineProtocol;
import com.sohu.cache.protocol.RedisProtocol;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisClusterNode;
import com.sohu.cache.redis.RedisConfigTemplateService;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.HttpPostUtil;
import com.sohu.cache.util.IdempotentConfirmer;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.util.UnionCacheUtil;
import com.sohu.cache.web.enums.RedisOperateEnum;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

/**
 * Created by yijunzhang on 14-8-25.
 */
public class RedisDeployCenterImpl extends BaseAlertService implements RedisDeployCenter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private InstanceDao instanceDao;

    private MachineDao machineDao;

    private MachineCenter machineCenter;

    private RedisCenter redisCenter;
    
    private AppToUserDao appToUserDao;

    private AppUserDao appUserDao;
    
	private AppDao appDao;
    
    private RedisConfigTemplateService redisConfigTemplateService;

    @Override
    public boolean deployClusterInstance(long appId, List<RedisClusterNode> clusterNodes, int maxMemory, int templateId) {
        if (!isExist(appId)) {
            return false;
        }
        String host = null;
        Integer port = null;
        Map<Jedis, Jedis> clusterMap = new LinkedHashMap<Jedis, Jedis>();
        for (RedisClusterNode node : clusterNodes) {
            String masterHost = node.getMasterHost();
            String slaveHost = node.getSlaveHost();
            Integer masterPort = machineCenter.getAvailablePort(masterHost, ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
            if (masterPort == null) {
                logger.error("masterHost={} getAvailablePort is null", masterHost);
                return false;
            }

            if (host == null || port == null) {
                host = masterHost;
                port = masterPort;
            }
            boolean isMasterRun = runInstance(masterHost, masterPort, maxMemory, true, templateId);
            if (!isMasterRun) {
                return false;
            }
            if (StringUtils.isNotBlank(slaveHost)) {
                Integer slavePort = machineCenter.getAvailablePort(slaveHost, ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
                if (slavePort == null) {
                    logger.error("slavePort={} getAvailablePort is null", slavePort);
                    return false;
                }
                boolean isSlaveRun = runInstance(slaveHost, slavePort, maxMemory, true, templateId);
                if (!isSlaveRun) {
                    return false;
                }
                clusterMap.put(redisCenter.getAuthJedis(masterHost, masterPort), redisCenter.getAuthJedis(slaveHost, slavePort));
            } else {
                clusterMap.put(redisCenter.getAuthJedis(masterHost, masterPort), null);
            }
        }

        boolean isCluster;
        Set<String> ipSet = new HashSet<String>();
        try {
            isCluster = startCluster(clusterMap);
            if (!isCluster) {
                logger.error("startCluster create error!");
                return false;
            }
            
            
            for (Map.Entry<Jedis, Jedis> entry : clusterMap.entrySet()) {
                Jedis master = entry.getKey();
                Jedis slave = entry.getValue();
                ipSet.add(master.getClient().getHost());
                
                //保存实例信息 & 触发收集
                saveInstance(appId, master.getClient().getHost(),
                        master.getClient().getPort(), maxMemory, ConstUtils.CACHE_TYPE_REDIS_CLUSTER, "");
                redisCenter.deployRedisCollection(appId, master.getClient().getHost(), master.getClient().getPort());
                if (slave != null) {
                	ipSet.add(slave.getClient().getHost());
                    saveInstance(appId, slave.getClient().getHost(), slave.getClient().getPort(),
                            maxMemory, ConstUtils.CACHE_TYPE_REDIS_CLUSTER, "");
                    redisCenter.deployRedisCollection(appId, slave.getClient().getHost(), slave.getClient().getPort());
                }
            }
        } finally {
            //关闭jedis连接
            for (Jedis master : clusterMap.keySet()) {
                master.close();
                if (clusterMap.get(master) != null) {
                    clusterMap.get(master).close();
                }
            }
        }
        addToUnioncache(appId);
        return true;
    }
    
    /**
     * 1. 被forget的节点必须在线(这个条件有待验证) 
     * 2. 被forget的节点不能有从节点 
     * 3. 被forget的节点不能有slots
     */
    @Override
    public Result checkClusterForget(int forgetInstanceId) {
        // 0.各种验证
        Assert.isTrue(forgetInstanceId > 0);
        
        InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(forgetInstanceId);
        Assert.isTrue(instanceInfo != null);
        String forgetHost = instanceInfo.getIp();
        int forgetPort = instanceInfo.getPort();
        
        //find a master ip to check the forgetting redis instance status.
        String checkIp=forgetHost;
        int checkPort=forgetPort;
        long appId = instanceInfo.getAppId();
		List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
		for (InstanceInfo info : instanceList) {
			String clusterIp = info.getIp();
			int clusterPort = info.getPort();
			if (clusterIp.equals(forgetHost) && clusterPort == forgetPort) {
				continue;
			} else {
				if (RedisUtil.isRun(clusterIp, clusterPort, null) && RedisUtil.isMaster(clusterIp, clusterPort)) {
					checkIp=clusterIp;
					checkPort=clusterPort;
					break;
				}
			}
		}
		
		//find a master ip to check the forgetting redis instance end.
        
        // 2.被forget的节点不能有从节点
        /*Boolean hasSlaves = redisCenter.hasSlaves(forgetHost, forgetPort);
        if (hasSlaves == null || hasSlaves) {
            logger.warn("{}:{} has slave", forgetHost, forgetPort);
            return Result.fail(String.format("被forget的节点(%s:%s)不能有从节点", forgetHost, forgetPort));
        }*/

        // 3.被forget的节点不能有slots
        boolean hasSlots = RedisUtil.hasSlots(checkIp,checkPort,forgetHost,forgetPort);
        if (hasSlots) {
            logger.warn("{}:{} has slots", forgetHost, forgetPort);
            return Result.fail(String.format("被forget的节点(%s:%s)不能持有slot", forgetHost, forgetPort));
        }

        return Result.success("");
    }
    
    @Override
    public void addToUnioncache(long appId){
    	if ("true".equals(ConstUtils.SYNC_TO_JCACHE)){
    		boolean success = true;
    		try{
    			AppDesc app = appDao.getAppDescById(appId);
    			
    			if (StringUtils.isEmpty(app.getJcacheUrl())){
    				logger.warn(String.format("because url is empty ,no need to sync info of appId %s to UnionCache .",appId));
    				return;
    			}
        		
        		String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(ConstUtils.CACHECLOUD_MAIL, ConstUtils.CACHECLOUD_IPORTAL_PWD)));
                LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);
                
                Map<String,String> parameter = new Hashtable<String,String>();
                parameter.put("userId",loginResponse.getUser_id());
                parameter.put("token",loginResponse.getToken());
                parameter.put("groupId",String.valueOf(appId));
                parameter.put("groupName",app.getName());
                parameter.put("resourceType",String.valueOf(ConstUtils.REDIS_TYPE));
                parameter.put("clusterType",String.valueOf(getUnionCacheType(app.getType())));
                parameter.put("userName","default");
                parameter.put("password","default");
                
                List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
                parameter.put("nodesList",getNodeList(instanceList));
                
                success = UnionCacheUtil.addNodeInfo(app.getJcacheUrl(),parameter);
    		}catch(Exception e){
    			logger.error("",e);
    			success = false;
    		}
    		
    		if (!success){
    			mobileAlertComponent.sendPhone(String.format("Add node info of appId %s to UnionCache failed.",appId), null);
    		}
    	}else{
    		logger.warn(String.format("According to config SYNC_TO_UNION_CACHE=false ,no need to sync info of appId %s to UnionCache .",appId));
    	}
    	
    	syncUserPriToUnionCache(appId);
    }
    
    @Override
    public void syncUserPriToUnionCache(long appId){
    	if ("true".equals(ConstUtils.SYNC_TO_JCACHE)){
    		AppDesc app = appDao.getAppDescById(appId);
    		
    		if (StringUtils.isEmpty(app.getJcacheUrl())){
				logger.warn(String.format("because url is empty ,no need to sync info of appId %s to UnionCache .",appId));
				return;
			}
    		
    		boolean success = true;
    		try{
        		String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(ConstUtils.CACHECLOUD_MAIL, ConstUtils.CACHECLOUD_IPORTAL_PWD)));
                LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);
                
                Map<String,String> parameter = new Hashtable<String,String>();
                parameter.put("userId",loginResponse.getUser_id());
                parameter.put("token",loginResponse.getToken());
                parameter.put("groupId",String.valueOf(appId));
                
                
                List<AppToUser> userList = appToUserDao.getByAppId(appId);
                parameter.put("authUserInfo",getUserList(userList));
                
                success = UnionCacheUtil.syncUserInfo(app.getJcacheUrl(),parameter);
    		}catch(Exception e){
    			logger.error("",e);
    			success = false;
    		}
    		
    		if (!success){
    			mobileAlertComponent.sendPhone(String.format("sync user info of appId %s to UnionCache failed.",appId), null);
    		}
    	}else{
    		logger.warn(String.format("According to config SYNC_TO_UNION_CACHE=false ,no need to sync info of appId %s to UnionCache .",appId));
    	}
    }
    
    private String getUserList(List<AppToUser> userList){
    	StringBuffer sb = new StringBuffer();
    	for (AppToUser appToUser : userList){
    		AppUser user = appUserDao.get(appToUser.getUserId());
    		sb.append(user.getEmail()).append(",");
    	}
    	
    	return sb.substring(0, sb.length()-1).toString();
    }
    
    @Override
    public boolean updateToUnioncache(long appId){
    	if ("true".equals(ConstUtils.SYNC_TO_JCACHE)){
    		boolean rst = true;
    		try{
        		AppDesc app = appDao.getAppDescById(appId);
        		
        		if (StringUtils.isEmpty(app.getJcacheUrl())){
    				logger.warn(String.format("because url is empty ,no need to sync info of appId %s to UnionCache .",appId));
    				return false;
    			}
        		
        		String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(ConstUtils.CACHECLOUD_MAIL, ConstUtils.CACHECLOUD_IPORTAL_PWD)));
                LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);
                
                Map<String,String> parameter = new Hashtable<String,String>();
                parameter.put("userId",loginResponse.getUser_id());
                parameter.put("token",loginResponse.getToken());
                parameter.put("groupId",String.valueOf(appId));
                parameter.put("groupName",app.getName());
                parameter.put("resourceType",String.valueOf(ConstUtils.REDIS_TYPE));
                parameter.put("clusterType",String.valueOf(getUnionCacheType(app.getType())));
                parameter.put("userName","default");
                parameter.put("password","default");
                
                List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
                parameter.put("nodesList",getNodeList(instanceList));
                
                rst = UnionCacheUtil.updateNodeInfo(app.getJcacheUrl(),parameter);
    		}catch(Exception e){
    			logger.error("",e);
    			rst = false;
    		}
    		
    		if (!rst){
    			mobileAlertComponent.sendPhone(String.format("Update node info of appId %s to UnionCache failed.",appId), null);
    		}
    		return rst;
    	}else{
    		logger.warn(String.format("According to config SYNC_TO_UNION_CACHE=false ,no need to sync info of appId %s to UnionCache .",appId));
    		return false;
    	}
    	
    }
    
    @Override
    public void deleteToUnioncache(long appId){
    	if ("true".equals(ConstUtils.SYNC_TO_JCACHE)){
    		boolean success = true;
    		try{
        		AppDesc app = appDao.getAppDescById(appId);
        		if (StringUtils.isEmpty(app.getJcacheUrl())){
    				logger.warn(String.format("because url is empty ,no need to sync info of appId %s to UnionCache .",appId));
    				return;
    			}
        		
        		String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(ConstUtils.CACHECLOUD_MAIL, ConstUtils.CACHECLOUD_IPORTAL_PWD)));
                LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);
                
                Map<String,String> parameter = new Hashtable<String,String>();
                parameter.put("userId",loginResponse.getUser_id());
                parameter.put("token",loginResponse.getToken());
                parameter.put("groupId",String.valueOf(appId));
                
                success = UnionCacheUtil.deleteNodeInfo(app.getJcacheUrl(),parameter);
    		}catch(Exception e){
    			logger.error("",e);
    			success = false;
    		}
    		
    		if (!success){
    			mobileAlertComponent.sendPhone(String.format("Offline appId %s to UnionCache failed.",appId), null);
    		}
    	}else{
    		logger.warn(String.format("According to config SYNC_TO_UNION_CACHE=false ,no need to sync info of appId %s to UnionCache .",appId));
    	}
    }
    
    private int getUnionCacheType(int cachecloudType){
    	if (cachecloudType == ConstUtils.CACHE_TYPE_REDIS_CLUSTER){
    		return ConstUtils.CLUSTER_TYPE;
    	}else{
    		return ConstUtils.SINGLE_TYPE;
    	}
    }
    
    private String getNodeList(List<InstanceInfo> instanceList){
    	StringBuffer sb = new StringBuffer();
    	for (InstanceInfo info : instanceList){
    		String ip = info.getIp();
    		int port = info.getPort();
    		sb.append(ip).append(":").append(port).append(",");
    	}
    	return sb.substring(0, sb.length()-1).toString();
    }
    
    private boolean clusterMeet(Jedis jedis, String host, int port) {
        boolean isSingleNode = redisCenter.isSingleClusterNode(host, port);
        if (!isSingleNode) {
            logger.error("{}:{} isNotSingleNode", host, port);
            return false;
        } else {
            logger.warn("{}:{} isSingleNode", host, port);
        }

        String response = jedis.clusterMeet(host, port);
        boolean isMeet = response != null && response.equalsIgnoreCase("OK");
        if (!isMeet) {
            logger.error("{}:{} meet error", host, port);
            return false;
        }
        return true;
    }

    private boolean startCluster(Map<Jedis, Jedis> clusterMap) {
        final Jedis jedis = new ArrayList<Jedis>(clusterMap.keySet()).get(0);
        //meet集群节点
        for (final Jedis master : clusterMap.keySet()) {
            boolean isMeet = new IdempotentConfirmer() {

                @Override
                public boolean execute() {
                    boolean isMeet = clusterMeet(jedis, master.getClient().getHost(), master.getClient().getPort());
                    if (!isMeet) {
                        return false;
                    }
                    return true;
                }
            }.run();
            if (!isMeet) {
                return false;
            }
            final Jedis slave = clusterMap.get(master);
            if (slave != null) {
                isMeet = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        boolean isMeet = clusterMeet(jedis, slave.getClient().getHost(), slave.getClient().getPort());
                        if (!isMeet) {
                            return false;
                        }
                        return true;
                    }
                }.run();
                if (!isMeet) {
                    return false;
                }
            }
        }
        int masterSize = clusterMap.size();
        int perSize = (int) Math.ceil(16384 / masterSize);
        int index = 0;
        int masterIndex = 0;
        final ArrayList<Integer> slots = new ArrayList<Integer>();
        List<Jedis> masters = new ArrayList<Jedis>(clusterMap.keySet());
        //分配slot
        for (int slot = 0; slot <= 16383; slot++) {
            slots.add(slot);
            if (index++ >= perSize || slot == 16383) {
                final int[] slotArr = new int[slots.size()];
                for (int i = 0; i < slotArr.length; i++) {
                    slotArr[i] = slots.get(i);
                }
                final Jedis masterJedis = masters.get(masterIndex++);
                boolean isSlot = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        String response = masterJedis.clusterAddSlots(slotArr);
                        boolean isSlot = response != null && response.equalsIgnoreCase("OK");
                        if (!isSlot) {
                            return false;
                        }
                        return true;
                    }
                }.run();
                if (!isSlot) {
                    logger.error("{}:{} set slots:{}", masterJedis.getClient().getHost(),
                            masterJedis.getClient().getPort(), slots);
                    return false;
                }
                slots.clear();
                index = 0;
            }
        }
        //设置从节点
        for (Jedis masterJedis : clusterMap.keySet()) {
            final Jedis slaveJedis = clusterMap.get(masterJedis);
            if (slaveJedis == null) {
                continue;
            }
            final String nodeId = getClusterNodeId(masterJedis);
            boolean isReplicate = new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    try {
                        //等待广播节点
                        TimeUnit.SECONDS.sleep(2);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    String response = null;
                    try {
                        response = slaveJedis.clusterReplicate(nodeId);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    boolean isReplicate = response != null && response.equalsIgnoreCase("OK");
                    if (!isReplicate) {
                        try {
                            //等待广播节点
                            TimeUnit.SECONDS.sleep(2);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        return false;
                    }
                    return true;
                }
            }.run();

            if (!isReplicate) {
                logger.error("{}:{} set replicate:{}", slaveJedis.getClient().getHost(),
                        slaveJedis.getClient().getPort());
                return false;
            }
        }

        return true;
    }

    private String getClusterNodeId(Jedis jedis) {
        try {
            String infoOutput = jedis.clusterNodes();
            for (String infoLine : infoOutput.split("\n")) {
                if (infoLine.contains("myself")) {
                    return infoLine.split(" ")[0];
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean deploySentinelInstance(long appId, String masterHost, String slaveHost, int maxMemory, List<String> sentinelList, int templateId) {
        if (!isExist(appId)) {
            return false;
        }
        //获取端口
        Integer masterPort = machineCenter.getAvailablePort(masterHost, ConstUtils.CACHE_REDIS_STANDALONE);
        if (masterPort == null) {
            logger.error("masterHost={} getAvailablePort is null", masterHost);
            return false;
        }
        Integer slavePort = machineCenter.getAvailablePort(slaveHost, ConstUtils.CACHE_REDIS_STANDALONE);
        if (slavePort == null) {
            logger.error("slaveHost={} getAvailablePort is null", slavePort);
            return false;
        }
        //运行实例
        boolean isMasterRun = runInstance(masterHost, masterPort, maxMemory, false, templateId);
        if (!isMasterRun) {
            return false;
        }
        boolean isSlaveRun = runInstance(slaveHost, slavePort, maxMemory, false, templateId);
        if (!isSlaveRun) {
            return false;
        }
        //添加slaveof配置
        boolean isSlave = slaveOf(masterHost, masterPort, slaveHost, slavePort);
        if (!isSlave) {
            return false;
        }

        //运行sentinel实例组
        boolean isRunSentinel = runSentinelGroup(sentinelList, masterHost, masterPort, appId);
        if (!isRunSentinel) {
            return false;
        }

        //写入instanceInfo 信息
        saveInstance(appId, masterHost, masterPort, maxMemory, ConstUtils.CACHE_REDIS_STANDALONE, "");
        saveInstance(appId, slaveHost, slavePort, maxMemory, ConstUtils.CACHE_REDIS_STANDALONE, "");

        //启动监控trigger
        boolean isMasterDeploy = redisCenter.deployRedisCollection(appId, masterHost, masterPort);
        boolean isSlaveDeploy = redisCenter.deployRedisCollection(appId, slaveHost, slavePort);
        if (!isMasterDeploy) {
            logger.warn("host={},port={},isMasterDeploy=false", masterHost, masterPort);
        }
        if (!isSlaveDeploy) {
            logger.warn("host={},port={},isSlaveDeploy=false", slaveHost, slavePort);
        }
        
        addToUnioncache(appId);
        return true;
    }

    @Override
    public boolean deployStandaloneInstance(long appId, List<String[]> nodes, int templateId) {
        if (!isExist(appId)) {
            return false;
        }
        
        List<String> instanceList = new ArrayList<String>();
        for (String[] array : nodes) {
            String master = array[0];
            int memory = NumberUtils.createInteger(array[1]);
            String slave = null;
            if (array.length > 2) {
                slave = array[2];
            }
            
            //获取端口
            Integer masterPort = machineCenter.getAvailablePort(master, ConstUtils.CACHE_REDIS_STANDALONE);
            if (masterPort == null) {
                logger.error("masterHost={} getAvailablePort is null", master);
                return false;
            }
            
            //运行实例
            boolean isMasterRun = runInstance(master, masterPort, memory , false, templateId);
            if (!isMasterRun) {
                return false;
            }           
            
            instanceList.add(master + ":" + memory + ":" + masterPort );
            
            if (StringUtils.isNotBlank(slave)) {
                Integer slavePort = machineCenter.getAvailablePort(slave, ConstUtils.CACHE_REDIS_STANDALONE);
                if (slavePort == null) {
                    logger.error("slavePort={} getAvailablePort is null", slavePort);
                    return false;
                }
                boolean isSlaveRun = runInstance(slave, slavePort, memory, false, templateId);
                if (!isSlaveRun) {
                    return false;
                }
                
                //添加slaveof配置
                boolean isSlave = slaveOf(master, masterPort, slave, slavePort);
                if (!isSlave) {
                    return false;
                }
                
                instanceList.add(slave + ":" + memory + ":" + slavePort );
            }
        }
        
        Set<String> ipSet = new HashSet<String>();
        for (String instance : instanceList) {
        	String master = instance.split(":")[0];
        	int memory = Integer.valueOf(instance.split(":")[1]);
        	int port = Integer.valueOf(instance.split(":")[2]);
            ipSet.add(master);
        	//写入instanceInfo 信息
            saveInstance(appId, master, port, memory, ConstUtils.CACHE_REDIS_STANDALONE, "");
            //启动监控trigger
            boolean isMasterDeploy = redisCenter.deployRedisCollection(appId, master, port);
            if (!isMasterDeploy) {
                logger.warn("host={},port={},Deploy redis data collection failed。", master, port);
            }
        }
        
        addToUnioncache(appId);
        return true;
    }

    @Override
    public InstanceInfo saveInstance(long appId, String host, int port, int maxMemory, int type,
            String cmd) {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setAppId(appId);
        MachineInfo machineInfo = machineDao.getMachineInfoByIp(host);
        instanceInfo.setHostId(machineInfo.getId());
        instanceInfo.setConn(0);
        instanceInfo.setMem(maxMemory);
        instanceInfo.setStatus(InstanceStatusEnum.GOOD_STATUS.getStatus());
        instanceInfo.setPort(port);
        instanceInfo.setType(type);
        instanceInfo.setCmd(cmd);
        instanceInfo.setIp(host);
        instanceDao.saveInstance(instanceInfo);
        machineCenter.syncInstanceInfoFile(host);
        return instanceInfo;
    }

    private boolean runSentinelGroup(List<String> sentinelList, String masterHost, int masterPort, long appId) {
        for (String sentinelHost : sentinelList) {
            boolean isRun = runSentinel(sentinelHost, getMasterName(masterHost, masterPort), masterHost, masterPort, appId);
            if (!isRun) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean createRunNode(String host, Integer port, int maxMemory, boolean isCluster, int templateId) {
        boolean isRun = isRun(host, port);
        if (isRun) {
            return false;
        }
        boolean isCreate = runInstance(host, port, maxMemory, isCluster, templateId);
        
        return isCreate;
    }

    private boolean runInstance(String host, Integer port, int maxMemory, boolean isCluster, int templateId) {
        // 生成配置
        List<String> configs = handleConfig(host,port, maxMemory, templateId); //获取基本的配置项
        if (isCluster) {
            configs.addAll(handleClusterConfig(host,port, templateId));
        }
        printConfig(configs);
        String fileName;
        String runShell;
        if (isCluster) {
            runShell = RedisProtocol.getRunShell(port, true);
            fileName = RedisProtocol.getConfig(port, true);
        } else {
            runShell = RedisProtocol.getRunShell(port, false);
            fileName = RedisProtocol.getConfig(port, false);
        }
        String pathFile = machineCenter.createRemoteFile(host, fileName, configs);
        if (StringUtils.isBlank(pathFile)) {
            logger.error("createFile={} error", pathFile);
            return false;
        }
        if (isCluster) {
            //删除cluster节点配置
            String deleteNodeShell = String.format("rm -rf %s/nodes-%s.conf", MachineProtocol.DATA_DIR, port);
            String deleteNodeResult = machineCenter.executeShell(host, deleteNodeShell);
            if (!ConstUtils.INNER_ERROR.equals(deleteNodeResult)) {
                logger.warn("runDeleteNodeShell={} at host {} success", deleteNodeShell, host);
            }else{
            	 logger.warn("runDeleteNodeShell={} at host {} failed", deleteNodeShell, host);
            	return false;
            }
        }
        //启动实例
        logger.info("masterShell:host={};shell={}", host, runShell);
        boolean isMasterShell = machineCenter.startProcessAtPort(host, port, runShell);
        if (!isMasterShell) {
            logger.error("runShell={} error,{}:{}", runShell, host, port);
            return false;
        }
        //验证实例
        if (!isRun(host, port)) {
            logger.error("host:{};port:{} not run", host, port);
            return false;
        } else {
            logger.warn("runInstance-fallback : redis-cli -h {} -p {} shutdown", host, port);
        }
        return true;
    }

    private boolean slaveOf(final String masterHost, final int masterPort, final String slaveHost,
            final int slavePort) {
        final Jedis slave = redisCenter.getAuthJedis(slaveHost, slavePort, Protocol.DEFAULT_TIMEOUT * 3);
        final Jedis master = redisCenter.getAuthJedis(masterHost,masterPort);
        String password = master.configGet("requirepass").get(1);
        if (StringUtils.isNotEmpty(password)){
        	slave.auth(password);        	
        }
        try {
            boolean isSlave = new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    String result = slave.slaveof(masterHost, masterPort);
                    return result != null && result.equalsIgnoreCase("OK");
                }
            }.run();
            if (!isSlave) {
                logger.error(String.format("establishing master-slave link failed. master_ip=%s,master_port=%s,slave_ip=%s,slave_port=%s", masterHost,masterPort,slaveHost, slavePort));
                return false;
            }
            slave.configRewrite();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            if (slave != null)
                slave.close();
        }

        return true;
    }

    private boolean runSentinel(String sentinelHost, String masterName, String masterHost, Integer masterPort, long appId) {
        int templateId = appDao.getAppDescById(appId).getTemplateId();
        //启动sentinel实例
        Integer sentinelPort = machineCenter.getAvailablePort(sentinelHost, ConstUtils.CACHE_REDIS_SENTINEL);
        if (sentinelPort == null) {
            logger.error("host={} getAvailablePort is null", sentinelHost);
            return false;
        }
        List<String> masterSentinelConfigs = handleSentinelConfig(masterName, masterHost, masterPort, sentinelHost,sentinelPort, templateId);
        printConfig(masterSentinelConfigs);
        String masterSentinelFileName = RedisProtocol.getConfig(sentinelPort, false);
        String sentinelPathFile = machineCenter.createRemoteFile(sentinelHost, masterSentinelFileName, masterSentinelConfigs);
        if (StringUtils.isBlank(sentinelPathFile)) {
            return false;
        }
        String sentinelShell = RedisProtocol.getSentinelShell(sentinelPort);
        logger.info("sentinelMasterShell:{}", sentinelShell);
        boolean isSentinelMasterShell = machineCenter.startProcessAtPort(sentinelHost, sentinelPort, sentinelShell);
        if (!isSentinelMasterShell) {
            logger.error("sentinelMasterShell={} error", sentinelShell);
            return false;
        }
        //验证实例
        if (!isRun(sentinelHost, sentinelPort)) {
            logger.error("host:{};port:{} not run", sentinelHost, sentinelPort);
            return false;
        } else {
            logger.warn("runSentinel-fallback : redis-cli -h {} -p {} shutdown", sentinelHost, sentinelPort);
        }
        //save sentinel
        saveInstance(appId, sentinelHost, sentinelPort, 0, ConstUtils.CACHE_REDIS_SENTINEL,
                getMasterName(masterHost, masterPort));
        machineCenter.syncInstanceInfoFile(sentinelHost);
        return true;
    }

    /**
     * 获取redis配置模板
     *
     * @param port
     * @param maxMemory
     * @return
     */
    public List<String> handleConfig(String host,int port, int maxMemory, int templateId) {
        List<String> configs = null;
        try {
            configs = redisConfigTemplateService.handleConfig(host,port, maxMemory, templateId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (CollectionUtils.isEmpty(configs)) {
            configs = redisConfigTemplateService.handleCommonDefaultConfig(host,port, maxMemory);
        }
        return configs;
    }

    private List<String> handleSentinelConfig(String masterName, String host, int port, String sentinelHost,int sentinelPort, int templateId) {
        List<String> configs = null;
        try {
            configs = redisConfigTemplateService.handleSentinelConfig(masterName, host, port, sentinelHost,sentinelPort, templateId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (CollectionUtils.isEmpty(configs)) {
            configs = redisConfigTemplateService.handleSentinelDefaultConfig(masterName, host, port, sentinelPort);
        }
        return configs;
    }

    private List<String> handleClusterConfig(String host,int port, int templateId) {
        List<String> configs = null;
        try {
            configs = redisConfigTemplateService.handleClusterConfig(host,port, templateId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (CollectionUtils.isEmpty(configs)) {
            configs = redisConfigTemplateService.handleClusterDefaultConfig(port);
        }
        return configs;
    }

    private String getMasterName(String host, int port) {
        String masterSentinelName = String.format("sentinel-%s-%s", host, port);
        return masterSentinelName;
    }

    private boolean isRun(String host, int port) {
        final Jedis jedis = redisCenter.getAuthJedis(host, port);
        try {
            return new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    try{
                    	String pong = jedis.ping();
                        return pong != null && pong.equalsIgnoreCase("PONG");
                    }
                    catch (Exception e) {
						return false;
					}
                }
            }.run();
        } finally {
            jedis.close();
        }
    }

    private void printConfig(List<String> masterConfigs) {
        logger.info("==================redis-{}-config==================", masterConfigs);
        for (String line : masterConfigs) {
            logger.info(line);
        }
    }

    private boolean isExist(long appId) {
        List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
        if (instanceInfos != null && instanceInfos.size() > 0) {
            logger.error("appId={} instances is exist , instanceInfos={}", appId, instanceInfos);
            return false;
        }
        return true;
    }

    @Override
    public boolean modifyAppConfig(long appId, String parameter, String value) {
        List<InstanceInfo> list = instanceDao.getInstListByAppId(appId);
        if (list == null || list.isEmpty()) {
            logger.error(String.format("appId=%s no instances", appId));
            return false;
        }
        for (InstanceInfo instance : list) {
            int type = instance.getType();
            if (!TypeUtil.isRedisType(type)) {
                logger.error("appId={};type={};is not redisType", appId, type);
                return false;
            }
            //忽略sentinel
            if (TypeUtil.isRedisSentinel(type)) {
                continue;
            }
            //忽略下线
            if (instance.isOffline()) {
                continue;
            }
            String host = instance.getIp();
            int port = instance.getPort();
            if (!modifyInstanceConfig(host, port, parameter, value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean modifyInstanceConfig(final String host, final int port, final String parameter, final String value) {
        final Jedis jedis = redisCenter.getAuthJedis(host, port, 5000);
        try {
            boolean isConfig = new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    boolean isRun = redisCenter.isRun(host, port);
                    if (!isRun) {
                        logger.warn("modifyInstanceConfig{}:{} is shutdown", host, port);
                        return true;
                    }
                    String result = jedis.configSet(parameter, value);
                    if (parameter.equalsIgnoreCase("requirepass")){
                    	jedis.auth(value);
                    	jedis.configSet("masterauth",value);
                    }
                    boolean isConfig = result != null && result.equalsIgnoreCase("OK");
                    if (!isConfig) {
                        logger.error(String.format("modifyConfigError:ip=%s,port=%s,result=%s", host, port, result));
                        return false;
                    }
                    return isConfig;
                }
            }.run();
            String isRewrite = jedis.configRewrite();
            if (!"OK".equals(isRewrite)) {
                logger.error("configRewrite={}:{} failed", host, port);
            }
            return isConfig;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            if (jedis != null)
                jedis.close();
        }
    }
    
    public boolean modifySlaveConfig(final Jedis jedis, final String parameter, final String value) {
    	final String host = jedis.getClient().getHost();
    	final int port = jedis.getClient().getPort();
    	
        try {
            boolean isConfig = new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    boolean isRun = redisCenter.isRun(host, port);
                    if (!isRun) {
                        logger.warn("modifyInstanceConfig{}:{} is shutdown", host, port);
                        return true;
                    }
                    String result = jedis.configSet(parameter, value);
                    if (parameter.equalsIgnoreCase("requirepass")){
                    	jedis.auth(value);
                    	jedis.configSet("masterauth",value);
                    }
                    boolean isConfig = result != null && result.equalsIgnoreCase("OK");
                    if (!isConfig) {
                        logger.error(String.format("modifyConfigError:ip=%s,port=%s,result=%s", host, port, result));
                        return false;
                    }
                    return isConfig;
                }
            }.run();
            String isRewrite = jedis.configRewrite();
            if ("OK".equals(isRewrite)) {
                logger.error("configRewrite={}:{} failed", host, port);
            }
            return isConfig;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            if (jedis != null)
                jedis.close();
        }
    }

    @Override
    public boolean addSentinel(long appId, String sentinelHost) {
        AppDesc appDesc = appDao.getAppDescById(appId);
        JedisSentinelPool jedisSentinelPool = redisCenter.getJedisSentinelPool(appDesc);
        if (jedisSentinelPool == null) {
            return false;
        }
        List<InstanceInfo> instanceInfos = instanceDao.getInstListByAppId(appId);
        String masterName = null;
        for (Iterator<InstanceInfo> i = instanceInfos.iterator(); i.hasNext(); ) {
            InstanceInfo instanceInfo = i.next();
            if (instanceInfo.getType() != ConstUtils.CACHE_REDIS_SENTINEL) {
                i.remove();
                continue;
            }
            if (masterName == null && StringUtils.isNotBlank(instanceInfo.getCmd())) {
                masterName = instanceInfo.getCmd();
            }
        }
        Jedis jedis = null;
        String masterHost = null;
        Integer masterPort = null;
        try {
            jedis = jedisSentinelPool.getResource();
            masterHost = jedis.getClient().getHost();
            masterPort = jedis.getClient().getPort();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            jedis.close();
            jedisSentinelPool.destroy();
        }
        boolean isRun = runSentinel(sentinelHost, masterName, masterHost, masterPort, appId);
        if (!isRun) {
            return false;
        }
        return true;
    }
    
    @Override
    public RedisOperateEnum addSlotsFailMaster(long appId, int lossSlotsInstanceId, String newMasterHost) throws Exception {
        // 1.参数、应用、实例信息确认
        Assert.isTrue(appId > 0);
        Assert.isTrue(lossSlotsInstanceId > 0);
        Assert.isTrue(StringUtils.isNotBlank(newMasterHost));
        AppDesc appDesc = appDao.getAppDescById(appId);
        Assert.isTrue(appDesc != null);
        int type = appDesc.getType();
        if (!TypeUtil.isRedisCluster(type)) {
            logger.error("{} is not redis cluster type", appDesc);
            return RedisOperateEnum.FAIL;
        }
        //获取失联slots的实例信息
        InstanceInfo lossSlotsInstanceInfo = instanceDao.getInstanceInfoById(lossSlotsInstanceId);
        Assert.isTrue(lossSlotsInstanceInfo != null);

        // 2.获取集群中一个健康的master作为clusterInfo Nodes的数据源
        InstanceInfo sourceMasterInstance = redisCenter.getHealthyInstanceInfo(appId);
        // 并未找到一个合适的实例可以
        if (sourceMasterInstance == null) {
            logger.warn("appId {} does not have right instance", appId);
            return RedisOperateEnum.FAIL;
        }

        // 3. 找到丢失的slots，如果没找到就说明集群正常，直接返回
        String healthyMasterHost = sourceMasterInstance.getIp();
        int healthyMasterPort = sourceMasterInstance.getPort();
        int healthyMasterMem = sourceMasterInstance.getMem();
        // 3.1 查看整个集群中是否有丢失的slots
        List<Integer> allLossSlots = redisCenter.getClusterLossSlots(healthyMasterHost, healthyMasterPort);
        if (CollectionUtils.isEmpty(allLossSlots)) {
            logger.warn("appId {} all slots is regular and assigned", appId);
            return RedisOperateEnum.ALREADY_SUCCESS;
        }
        // 3.2 查看目标实例丢失slots 
        List<Integer> clusterLossSlots = redisCenter.getInstanceSlots(healthyMasterHost, healthyMasterPort, lossSlotsInstanceInfo.getIp(), lossSlotsInstanceInfo.getPort());
        // 4.开启新的节点
        // 4.1 从newMasterHost找到可用的端口newMasterPort
        final Integer newMasterPort = machineCenter.getAvailablePort(newMasterHost, ConstUtils.CACHE_TYPE_REDIS_CLUSTER);
        if (newMasterPort == null) {
            logger.error("host={} getAvailablePort is null", newMasterHost);
            return RedisOperateEnum.FAIL;
        }
        // 4.2 按照sourceMasterInstance的内存启动
        boolean isRun = runInstance(newMasterHost, newMasterPort, healthyMasterMem, true, appDesc.getTemplateId());
        if (!isRun) {
            logger.error("{}:{} is not run", newMasterHost, newMasterPort);
            return RedisOperateEnum.FAIL;
        }
        // 4.3 拷贝配置
        boolean isCopy = copyCommonConfig(healthyMasterHost, healthyMasterPort, newMasterHost, newMasterPort);
        if (!isCopy) {
            logger.error("{}:{} copy config {}:{} is error", healthyMasterHost, healthyMasterPort, newMasterHost, newMasterPort);
            return RedisOperateEnum.FAIL;
        }
        
        // 5. meet
        boolean isClusterMeet = false;
        Jedis sourceMasterJedis = null;
        try {
            sourceMasterJedis = redisCenter.getAuthJedis(healthyMasterHost, healthyMasterPort, 5000);
            isClusterMeet = clusterMeet(sourceMasterJedis, newMasterHost, newMasterPort);
            if (!isClusterMeet) {
                logger.error("{}:{} cluster is failed", newMasterHost, newMasterPort);
                return RedisOperateEnum.FAIL;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (sourceMasterJedis != null) {
                sourceMasterJedis.close();
            }
        }
        if (!isClusterMeet) {
            logger.warn("{}:{} meet {}:{} is fail", healthyMasterHost, healthyMasterPort, newMasterHost, newMasterPort);
            return RedisOperateEnum.FAIL;
        }
        
        // 6. 分配slots
        String addSlotsResult = "";
        Jedis newMasterJedis = null;
        Jedis healthyMasterJedis = null;
        try {
            newMasterJedis = redisCenter.getAuthJedis(newMasterHost, newMasterPort, 5000);
            healthyMasterJedis = redisCenter.getAuthJedis(healthyMasterHost, healthyMasterPort, 5000);
            //获取新的补救节点的nodid
            final String nodeId = getClusterNodeId(newMasterJedis);
            for (Integer slot : clusterLossSlots) {
                addSlotsResult = healthyMasterJedis.clusterSetSlotNode(slot, nodeId);
                logger.warn("set slot {}, result is {}", slot, addSlotsResult);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (newMasterJedis != null) {
                newMasterJedis.close();
            }
            if (healthyMasterJedis != null) {
                healthyMasterJedis.close();
            }
        }
        if (!"OK".equalsIgnoreCase(addSlotsResult)) {
            logger.warn("{}:{} set slots faily", newMasterHost, newMasterPort);
            return RedisOperateEnum.FAIL;
        }
        
        // 7.保存实例信息、并开启收集信息
        saveInstance(appId, newMasterHost, newMasterPort, healthyMasterMem, ConstUtils.CACHE_TYPE_REDIS_CLUSTER, "");
        redisCenter.deployRedisCollection(appId, newMasterHost, newMasterPort);
        
        // 休息一段时间，同步clusterNodes信息
        TimeUnit.SECONDS.sleep(2);
        
        // 8.最终打印出当前还没有补充的slots
        List<Integer> currentLossSlots = redisCenter.getClusterLossSlots(newMasterHost, newMasterPort);
        logger.warn("appId {} failslots assigned unsuccessfully, lossslots is {}", appId, currentLossSlots);
        updateToUnioncache(appId);
        return RedisOperateEnum.OP_SUCCESS;        
    }

    @Override
    public int checkAddSlave(int instanceId, final String slaveHost) {
        MachineStats machineStats = machineCenter.getMachineStatsByIp(slaveHost);
        if(machineStats == null) {
            return 2; //找不到该ip的机器！
        }
        int mem = (instanceDao.getInstanceInfoById(instanceId)).getMem();
        if(machineStats.getMemoryAllocated() + mem > (Long.parseLong(machineStats.getMemoryTotal())/(1024*1024))) {
            return 3; //机器剩余的内存不足，请选择其他机器进行分配!
        }
        return 0; // 成功
    }

    @Override
    public boolean addSlave(long appId, int instanceId, final String slaveHost) {
        Assert.isTrue(appId > 0);
        Assert.isTrue(instanceId > 0);
        Assert.isTrue(StringUtils.isNotBlank(slaveHost));
        AppDesc appDesc = appDao.getAppDescById(appId);
        Assert.isTrue(appDesc != null);
        int type = appDesc.getType();
        if (!TypeUtil.isRedisType(type)) {
            logger.error("{} is not redis type", appDesc);
            return false;
        }
        InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
        Assert.isTrue(instanceInfo != null);
        String masterHost = instanceInfo.getIp();
        int masterPort = instanceInfo.getPort();
        final Integer slavePort = machineCenter.getAvailablePort(slaveHost, ConstUtils.CACHE_REDIS_STANDALONE);
        if (slavePort == null) {
            logger.error("host={} getAvailablePort is null", slaveHost);
            return false;
        }
        boolean isRun;
        if (TypeUtil.isRedisCluster(type)) {
            isRun = runInstance(slaveHost, slavePort, instanceInfo.getMem(), true, appDesc.getTemplateId());
        } else {
            isRun = runInstance(slaveHost, slavePort, instanceInfo.getMem(), false, appDesc.getTemplateId());
        }

        if (!isRun) {
            logger.error("{}:{} is not run", slaveHost, slavePort);
            return false;
        }

        boolean isCopy = copyCommonConfig(masterHost, masterPort, slaveHost, slavePort);
        if (!isCopy) {
            logger.error("{}:{} copy config {}:{} is error", masterHost, masterPort, slaveHost, slavePort);
            return false;
        }
        if (TypeUtil.isRedisCluster(type)) {
            final Jedis masterJedis = redisCenter.getAuthJedis(masterHost, masterPort, Protocol.DEFAULT_TIMEOUT);
            final Jedis slaveJedis = redisCenter.getAuthJedis(slaveHost, slavePort, Protocol.DEFAULT_TIMEOUT);
            try {

                boolean isClusterMeet = clusterMeet(masterJedis, slaveHost, slavePort);
                if (!isClusterMeet) {
                    logger.error("{}:{} cluster is failed", slaveHost, slaveHost);
                    return isClusterMeet;
                }
                final String nodeId = getNodeId(masterJedis);
                if (StringUtils.isBlank(nodeId)) {
                    logger.error("{}:{} getNodeId failed", masterHost, masterPort);
                    return false;
                }
                boolean isClusterReplicate = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        try {
                            //等待广播节点
                            TimeUnit.SECONDS.sleep(2);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        String response = slaveJedis.clusterReplicate(nodeId);
                        logger.info("clusterReplicate-{}:{}={}", slaveHost, slavePort, response);
                        return response != null && response.equalsIgnoreCase("OK");
                    }
                }.run();
                if (!isClusterReplicate) {
                    logger.error("{}:{} clusterReplicate {} is failed ", slaveHost, slavePort, nodeId);
                    return false;
                }
                //保存配置
                masterJedis.clusterSaveConfig();
                slaveJedis.clusterSaveConfig();
                redisCenter.configRewrite(masterHost, masterPort);
                redisCenter.configRewrite(slaveHost, slavePort);
            } finally {
                masterJedis.close();
                slaveJedis.close();
            }
        } else {
            boolean isSlave = slaveOf(masterHost, masterPort, slaveHost, slavePort);
            if (!isSlave) {
                logger.error("{}:{} sync {}:{} is error", slaveHost, slavePort, masterHost, masterPort);
                return false;
            }
        }

        //写入instanceInfo 信息
        if (TypeUtil.isRedisCluster(type)) {
            saveInstance(appId, slaveHost, slavePort, instanceInfo.getMem(),
                    ConstUtils.CACHE_TYPE_REDIS_CLUSTER, "");
        } else {
            saveInstance(appId, slaveHost, slavePort, instanceInfo.getMem(),
                    ConstUtils.CACHE_REDIS_STANDALONE, "");
        }
        //启动监控trigger
        boolean isDeploy = redisCenter.deployRedisCollection(appId, slaveHost, slavePort);
        if (!isDeploy) {
            logger.warn("host={},port={},isMasterDeploy=false", slaveHost, slavePort);
        }

        updateToUnioncache(appId);
        return true;
    }
    
    @Override
    public boolean addSlaveAndBlock(long appId, int instanceId, final String slaveHost) {
        Assert.isTrue(appId > 0);
        Assert.isTrue(instanceId > 0);
        Assert.isTrue(StringUtils.isNotBlank(slaveHost));
        AppDesc appDesc = appDao.getAppDescById(appId);
        Assert.isTrue(appDesc != null);
        int type = appDesc.getType();
        if (!TypeUtil.isRedisType(type)) {
            logger.error("{} is not redis type", appDesc);
            return false;
        }
        InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(instanceId);
        Assert.isTrue(instanceInfo != null);
        String masterHost = instanceInfo.getIp();
        int masterPort = instanceInfo.getPort();
        final Integer slavePort = machineCenter.getAvailablePort(slaveHost, ConstUtils.CACHE_REDIS_STANDALONE);
        if (slavePort == null) {
            logger.error("host={} getAvailablePort is null", slaveHost);
            return false;
        }
        boolean isRun;
        if (TypeUtil.isRedisCluster(type)) {
            isRun = runInstance(slaveHost, slavePort, instanceInfo.getMem(), true, appDesc.getTemplateId());
        } else {
            isRun = runInstance(slaveHost, slavePort, instanceInfo.getMem(), false, appDesc.getTemplateId());
        }

        if (!isRun) {
            logger.error("{}:{} is not run", slaveHost, slavePort);
            return false;
        }

        boolean isCopy = copyCommonConfig(masterHost, masterPort, slaveHost, slavePort);
        if (!isCopy) {
            logger.error("{}:{} copy config {}:{} is error", masterHost, masterPort, slaveHost, slavePort);
            return false;
        }
        if (TypeUtil.isRedisCluster(type)) {
            final Jedis masterJedis = redisCenter.getAuthJedis(masterHost, masterPort, Protocol.DEFAULT_TIMEOUT);
            final Jedis slaveJedis = redisCenter.getAuthJedis(slaveHost, slavePort, Protocol.DEFAULT_TIMEOUT);
            try {

                boolean isClusterMeet = clusterMeet(masterJedis, slaveHost, slavePort);
                if (!isClusterMeet) {
                    logger.error("{}:{} cluster is failed", slaveHost, slaveHost);
                    return isClusterMeet;
                }
                final String nodeId = getNodeId(masterJedis);
                if (StringUtils.isBlank(nodeId)) {
                    logger.error("{}:{} getNodeId failed", masterHost, masterPort);
                    return false;
                }
                boolean isClusterReplicate = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        try {
                            //等待广播节点
                            TimeUnit.SECONDS.sleep(2);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        String response = slaveJedis.clusterReplicate(nodeId);
                        logger.info("clusterReplicate-{}:{}={}", slaveHost, slavePort, response);
                        return response != null && response.equalsIgnoreCase("OK");
                    }
                }.run();
                if (!isClusterReplicate) {
                    logger.error("{}:{} clusterReplicate {} is failed ", slaveHost, slavePort, nodeId);
                    return false;
                }
                //保存配置
                masterJedis.clusterSaveConfig();
                slaveJedis.clusterSaveConfig();
                redisCenter.configRewrite(masterHost, masterPort);
                redisCenter.configRewrite(slaveHost, slavePort);
            } finally {
                masterJedis.close();
                slaveJedis.close();
            }
        } else {
            boolean isSlave = slaveOf(masterHost, masterPort, slaveHost, slavePort);
            if (!isSlave) {
                logger.error("{}:{} sync {}:{} is error", slaveHost, slavePort, masterHost, masterPort);
                return false;
            }
        }

        //写入instanceInfo 信息
        if (TypeUtil.isRedisCluster(type)) {
            saveInstance(appId, slaveHost, slavePort, instanceInfo.getMem(),
                    ConstUtils.CACHE_TYPE_REDIS_CLUSTER, "");
        } else {
            saveInstance(appId, slaveHost, slavePort, instanceInfo.getMem(),
                    ConstUtils.CACHE_REDIS_STANDALONE, "");
        }
        //启动监控trigger
        boolean isDeploy = redisCenter.deployRedisCollection(appId, slaveHost, slavePort);
        if (!isDeploy) {
            logger.warn("host={},port={},isMasterDeploy=false", slaveHost, slavePort);
        }
        
        
        /**
         * block until slave and master relationship is established.
         */
        while(true){
        	
        	try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
        	Map<String, String> map = parseMap(slaveHost,slavePort);
        	String status = map.get("master_link_status");
        	
        	if (null == status){
        		throw new RuntimeException(String.format("Exception! master_link_status from  slave %s:%s is null",slaveHost,slavePort));
        	}
        	if ("up".equals(status)){
        		break;
        	}
        }

        updateToUnioncache(appId);
        return true;
    }
    
    private Map<String, String> parseMap(final String host,final int port) {
		final StringBuilder builder = new StringBuilder();
		boolean isInfo = new IdempotentConfirmer() {
			@Override
			public boolean execute() {
				String replicationInfo = null;
				Jedis jedis = redisCenter.getAuthJedis(host,port);
				try {
					replicationInfo = jedis.info("Replication");
				} catch (Exception e) {
					logger.warn(e.getMessage() + "-{}:{}", host, port,
							e.getMessage());
				}
				boolean isOk = StringUtils.isNotBlank(replicationInfo);
				if (isOk) {
					builder.append(replicationInfo);
				}
				return isOk;
			}
		}.run();
		if (!isInfo) {
			logger.error("{}:{} info Persistence failed", host, port);
			return Collections.emptyMap();
		}
		String replInfo = builder.toString();
		if (StringUtils.isBlank(replInfo)) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new LinkedHashMap<String, String>();
		String[] array = replInfo.split("\r\n");
		for (String line : array) {
			String[] cells = line.split(":");
			if (cells.length > 1) {
				map.put(cells[0], cells[1]);
			}
		}

		return map;
	}
    
    @Override
    public boolean sentinelFailover(long appId) throws Exception {
        Assert.isTrue(appId > 0);
        AppDesc appDesc = appDao.getAppDescById(appId);
        Assert.isTrue(appDesc != null);
        int type = appDesc.getType();
        if (!TypeUtil.isRedisSentinel(type)) {
            logger.warn("app={} is not sentinel", appDesc);
            return false;
        }
        final List<InstanceInfo> instanceList = instanceDao.getInstListByAppId(appId);
        if (instanceList == null || instanceList.isEmpty()) {
            logger.warn("app={} instances is empty");
            return false;
        }
        for (InstanceInfo instanceInfo : instanceList) {
            int instanceType = instanceInfo.getType();
            if (TypeUtil.isRedisSentinel(instanceType)) {
                final String host = instanceInfo.getIp();
                final int port = instanceInfo.getPort();
                final String masterName = instanceInfo.getCmd();
                if (StringUtils.isBlank(masterName)) {
                    logger.warn("{} cmd is null", instanceInfo);
                    continue;
                }
                boolean isRun = redisCenter.isRun(host, port);
                if (!isRun) {
                    logger.warn("{} is not run");
                    continue;
                }
                boolean isSentinelFailOver = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        Jedis jedis = redisCenter.getAuthJedis(host, port, Protocol.DEFAULT_TIMEOUT);
                        try {
                            String response = jedis.sentinelFailover(masterName);
                            return response != null && response.equalsIgnoreCase("OK");
                        } finally {
                            jedis.close();
                        }
                    }
                }.run();
                if (!isSentinelFailOver) {
                    logger.warn("{}:{} sentienl isSentinelFailOver error", host, port);
                    return false;
                } else {
                    logger.warn("SentinelFailOver done! ");
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean clusterFailover(long appId, int slaveInstanceId) throws Exception {
        Assert.isTrue(appId > 0);
        Assert.isTrue(slaveInstanceId > 0);
        AppDesc appDesc = appDao.getAppDescById(appId);
        Assert.isTrue(appDesc != null);
        int type = appDesc.getType();
        if (!TypeUtil.isRedisCluster(type)) {
            logger.error("{} is not redis type", appDesc);
            return false;
        }
        InstanceInfo instanceInfo = instanceDao.getInstanceInfoById(slaveInstanceId);
        Assert.isTrue(instanceInfo != null);
        String slaveHost = instanceInfo.getIp();
        int slavePort = instanceInfo.getPort();
        final Jedis slaveJedis = redisCenter.getAuthJedis(slaveHost, slavePort);
        boolean isClusterFailOver = new IdempotentConfirmer() {
            @Override
            public boolean execute() {

                String response = slaveJedis.clusterFailoverForce();
                return response != null && response.equalsIgnoreCase("OK");
            }
        }.run();
        if (!isClusterFailOver) {
            logger.error("{}:{} clusterFailover failed", slaveHost, slavePort);
            return false;
        } else {
            logger.warn("{}:{} clusterFailover Done! ", slaveHost, slavePort);
        }
        return true;
    }

    private String getNodeId(final Jedis jedis) {
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
                logger.error("{}:{} clusterNodes failed", jedis.getClient().getHost(), jedis.getClient().getPort());
                return null;
            }
            for (String infoLine : clusterNodes.toString().split("\n")) {
                if (infoLine.contains("myself")) {
                    String nodeId = infoLine.split(" ")[0];
                    return nodeId;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 拷贝redis配置
     *
     * @param sourceHost
     * @param sourcePort
     * @param targetHost
     * @param targetPort
     * @return
     */
    private boolean copyCommonConfig(String sourceHost, int sourcePort, String targetHost, int targetPort) {
        String[] compareConfigs = new String[] {"requirepass"
        		,"masterauth"
        		,"maxmemory"
        		,"maxmemory-samples"
        		,"timeout"
        		,"auto-aof-rewrite-percentage"
        		,"auto-aof-rewrite-min-size"
        		,"hash-max-ziplist-entries"
        		,"hash-max-ziplist-value"
        		,"list-max-ziplist-size"
        		,"list-compress-depth"
        		,"set-max-intset-entries"
        		,"zset-max-ziplist-entries"
        		,"zset-max-ziplist-value"
        		,"hll-sparse-max-bytes"
        		,"lua-time-limit"
        		,"slowlog-log-slower-than"
        		,"latency-monitor-threshold"
        		,"slowlog-max-len"
        		,"databases"
        		,"repl-ping-slave-period"
        		,"repl-timeout"
        		,"repl-backlog-size"
        		,"repl-backlog-ttl"
        		,"maxclients"
        		,"watchdog-period"
        		,"slave-priority"
        		,"min-slaves-to-write"
        		,"min-slaves-max-lag"
        		,"hz"
        		,"cluster-node-timeout"
        		,"cluster-migration-barrier"
        		,"cluster-slave-validity-factor"
        		,"repl-diskless-sync-delay"
        		,"tcp-keepalive"
        		,"cluster-require-full-coverage"
        		,"no-appendfsync-on-rewrite"
        		,"slave-serve-stale-data"
        		,"slave-read-only"
        		,"stop-writes-on-bgsave-error"
        		,"rdbcompression"
        		,"rdbchecksum"
        		,"activerehashing"
        		,"repl-disable-tcp-nodelay"
        		,"repl-diskless-sync"
        		,"aof-rewrite-incremental-fsync"
        		,"maxmemory-policy"
        		,"loglevel"
        		,"appendfsync"
        		,"appendonly"
        		,"save"
        		,"client-output-buffer-limit"};
        try {
        	
        	Jedis source = redisCenter.getAuthJedis(sourceHost, sourcePort, Protocol.DEFAULT_TIMEOUT * 3);
        	Jedis target =  redisCenter.getAuthJedis(targetHost, targetPort, Protocol.DEFAULT_TIMEOUT * 3);
            for (String config : compareConfigs) {
                String sourceValue = getConfigValue(source, config);
                if (StringUtils.isBlank(sourceValue)) {
                    continue;
                }
                String targetValue = getConfigValue(target, config);
                if (StringUtils.isNotBlank(targetHost)) {
                    if (!targetValue.equals(sourceValue)) {
                        this.modifySlaveConfig(target, config, sourceValue);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private String getConfigValue(Jedis jedis, String key) {
        try {
            List<String> values = jedis.configGet(key);
            if (values == null || values.size() < 1) {
                return null;
            }
            return values.get(1);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            jedis.close();
        }
    }

    public void setInstanceDao(InstanceDao instanceDao) {
        this.instanceDao = instanceDao;
    }

    public void setMachineCenter(MachineCenter machineCenter) {
        this.machineCenter = machineCenter;
    }

    public void setMachineDao(MachineDao machineDao) {
        this.machineDao = machineDao;
    }

    public void setRedisCenter(RedisCenter redisCenter) {
        this.redisCenter = redisCenter;
    }

    public void setAppDao(AppDao appDao) {
        this.appDao = appDao;
    }

    public void setRedisConfigTemplateService(RedisConfigTemplateService redisConfigTemplateService) {
        this.redisConfigTemplateService = redisConfigTemplateService;
    }
    
    public void setAppToUserDao(AppToUserDao appToUserDao) {
		this.appToUserDao = appToUserDao;
	}
    
    public void setAppUserDao(AppUserDao appUserDao) {
		this.appUserDao = appUserDao;
	}
}