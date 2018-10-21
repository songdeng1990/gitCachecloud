package com.sohu.cache.redis.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.catalina.startup.VersionLoggerListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.crsh.cli.impl.bootstrap.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.cache.dao.InstanceConfigDao;
import com.sohu.cache.entity.ConfigTemplate;
import com.sohu.cache.entity.InstanceConfig;
import com.sohu.cache.protocol.MachineProtocol;
import com.sohu.cache.redis.RedisConfigTemplateService;
import com.sohu.cache.redis.enums.RedisClusterConfigEnum;
import com.sohu.cache.redis.enums.RedisConfigEnum;
import com.sohu.cache.redis.enums.RedisSentinelConfigEnum;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.util.ConstUtils;

/**
 * redis配置模板服务
 * 
 * @author leifu
 * @Date 2016年6月23日
 * @Time 下午2:08:03
 */
public class RedisConfigTemplateServiceImpl implements RedisConfigTemplateService {

    private Logger logger = LoggerFactory.getLogger(RedisConfigTemplateServiceImpl.class);
    
    private final static String SPECIAL_EMPTY_STR = "\"\"";

    private InstanceConfigDao instanceConfigDao;
    
    @Override
    public List<InstanceConfig> getAllInstanceConfig() {
        try {
            return instanceConfigDao.getAllInstanceConfig();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<InstanceConfig> getByType(int type) {
        try {
            return instanceConfigDao.getByType(type);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<InstanceConfig> getByTemplateId(int templateId) {
        try {
            logger.info("loading configs template, templateId is {}", templateId);
            return instanceConfigDao.getByTemplateId(templateId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getByTemplateIdAndFilterUneffectiveConfig(int templateId) {
        try {
            logger.info("loading configs template, templateId is {}", templateId);
            List<InstanceConfig> instanceConfigList = getByTemplateId(templateId);
            List<String> configs = new ArrayList<>();
            for (InstanceConfig instanceConfig : instanceConfigList) {
                // 无效配置过滤
                if (!instanceConfig.isEffective()) {
                    continue;
                }
                String configKey = instanceConfig.getConfigKey();
                String configValue = instanceConfig.getConfigValue();
                if (StringUtils.isBlank(configValue)) {
                    configValue = SPECIAL_EMPTY_STR;
                }
                configs.add(combineConfigKeyValue(configKey, configValue));
            }
            return configs;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public int saveOrUpdate(InstanceConfig instanceConfig) {
        return instanceConfigDao.saveOrUpdate(instanceConfig);
    }

    @Override
    public InstanceConfig getById(long id) {
        try {
            return instanceConfigDao.getById(id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public InstanceConfig getByConfigKeyAndType(String configKey, int type) {
        try {
            return instanceConfigDao.getByConfigKeyAndType(configKey, type);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public int remove(long id) {
        return instanceConfigDao.remove(id);
    }

    @Override
    public int updateStatus(long id, int status) {
        return instanceConfigDao.updateStatus(id, status);
    }

    @Override
    public List<String> handleConfig(String host,int port, int maxMemory, int templateId) {
        List<InstanceConfig> instanceConfigList = getByTemplateId(templateId);
        if (CollectionUtils.isEmpty(instanceConfigList)) {
            return Collections.emptyList();
        }
        List<String> configs = new ArrayList<>();
        for (InstanceConfig instanceConfig : instanceConfigList) {
            // 无效配置过滤
            if (!instanceConfig.isEffective()) {
                continue;
            }
            if(ConstUtils.CACHE_REDIS_STANDALONE != instanceConfig.getType()) { //忽略sentinel配置
                continue;
            }
            String configKey = instanceConfig.getConfigKey();
            String configValue = instanceConfig.getConfigValue();
            if (StringUtils.isBlank(configValue)) {
                configValue = SPECIAL_EMPTY_STR;
            }
            if (RedisConfigEnum.MAXMEMORY.getKey().equals(configKey)) {
                configValue = String.format(configValue, maxMemory);
            } else if (RedisConfigEnum.DBFILENAME.getKey().equals(configKey)
                    || RedisConfigEnum.APPENDFILENAME.getKey().equals(configKey) || RedisConfigEnum.PORT.getKey().equals(configKey)) {
                configValue = String.format(configValue, port);
            } else if (RedisConfigEnum.DIR.getKey().equals(configKey)) {
                configValue = MachineProtocol.DATA_DIR;
            } else if (RedisConfigEnum.AUTO_AOF_REWRITE_PERCENTAGE.getKey().equals(configKey)) {
                //随机比例 auto-aof-rewrite-percentage
                int percent = 69 + new Random().nextInt(30);
                configValue = String.format(configValue, percent);
            }
            configs.add(combineConfigKeyValue(configKey, configValue));
        }
        handleSpecialConfig(host, configs);
        
        return configs;
    }

    @Override
    public List<String> handleSentinelConfig(String masterName, String masterHost, int masterPort, String sentinelHost,int sentinelPort, int templateId) {
        List<InstanceConfig> instanceConfigList = instanceConfigDao.getByTemplateId(templateId);
        if (CollectionUtils.isEmpty(instanceConfigList)) {
            return Collections.emptyList();
        }
        List<String> configs = new ArrayList<String>();
        for (InstanceConfig instanceConfig : instanceConfigList) {
            if (!instanceConfig.isEffective()) {
                continue;
            }
            if(instanceConfig.getType() != ConstUtils.CACHE_REDIS_SENTINEL) {
                continue;
            }
            String configKey = instanceConfig.getConfigKey();
            String configValue = instanceConfig.getConfigValue();
            if (StringUtils.isBlank(configValue)) {
                configValue = SPECIAL_EMPTY_STR;
            }
            if (RedisSentinelConfigEnum.PORT.getKey().equals(configKey)) {
                configValue = String.format(configValue, sentinelPort);
            } else if(RedisSentinelConfigEnum.MONITOR.getKey().equals(configKey)) {
                configValue = String.format(configValue, masterName, masterHost, masterPort);
            } else if(RedisSentinelConfigEnum.DOWN_AFTER_MILLISECONDS.getKey().equals(configKey) || RedisSentinelConfigEnum.FAILOVER_TIMEOUT.getKey().equals(configKey) || RedisSentinelConfigEnum.PARALLEL_SYNCS.getKey().equals(configKey)) {
                configValue = String.format(configValue, masterName);
            } else if (RedisConfigEnum.DIR.getKey().equals(configKey)) {
                configValue = MachineProtocol.DATA_DIR;
            }
            configs.add(combineConfigKeyValue(configKey, configValue));
        }
        handleSpecialConfig(sentinelHost, configs);
        return configs;
    }

    @Override
    public List<String> handleClusterConfig(String host,int port, int templateId) {
        List<InstanceConfig> instanceConfigList = getByTemplateId(templateId);
        if (CollectionUtils.isEmpty(instanceConfigList)) {
            return Collections.emptyList();
        }
        List<String> configs = new ArrayList<String>();
        for (InstanceConfig instanceConfig : instanceConfigList) {
            if (!instanceConfig.isEffective()) {
                continue;
            }
            if(instanceConfig.getType() != ConstUtils.CACHE_TYPE_REDIS_CLUSTER) {
                continue;
            }
            String configKey = instanceConfig.getConfigKey();
            String configValue = instanceConfig.getConfigValue();
            if (StringUtils.isBlank(configValue)) {
                configValue = SPECIAL_EMPTY_STR;
            }
            if (RedisClusterConfigEnum.CLUSTER_CONFIG_FILE.getKey().equals(configKey)) {
                configValue = String.format(configValue, port);
            }
            configs.add(combineConfigKeyValue(configKey, configValue));

        }
        
        //addConstantConfig(host, configs);
        return configs;
    }
    
    @Override
    public List<String> handleCommonDefaultConfig(String host,int port, int maxMemory) {
        List<String> configs = new ArrayList<String>();
        for (RedisConfigEnum config : RedisConfigEnum.values()) {
            if (RedisConfigEnum.MAXMEMORY.equals(config)) {
                configs.add(config.getKey() + " " + String.format(config.getValue(), maxMemory));
            } else if (RedisConfigEnum.DBFILENAME.equals(config) ||
                    RedisConfigEnum.APPENDFILENAME.equals(config) || RedisConfigEnum.PORT.equals(config)) {
                configs.add(config.getKey() + " " + String.format(config.getValue(), port));
            } else if (RedisConfigEnum.DIR.equals(config)) {
                configs.add(config.getKey() + " " + MachineProtocol.DATA_DIR);
            } else if (RedisConfigEnum.AUTO_AOF_REWRITE_PERCENTAGE.equals(config)) {
                //随机比例 auto-aof-rewrite-percentage
                int percent = 69 + new Random().nextInt(30);
                configs.add(config.getKey() + " " + String.format(RedisConfigEnum.AUTO_AOF_REWRITE_PERCENTAGE.getValue(), percent));
            } else {
                configs.add(config.getKey() + " " + config.getValue());
            }
        }
        
        handleSpecialConfig(host, configs);
        return configs;
    }

    @Override
    public List<String> handleSentinelDefaultConfig(String masterName, String host, int port, int sentinelPort) {
        List<String> configs = new ArrayList<String>();
        configs.add(RedisSentinelConfigEnum.PORT.getKey() + " " + String.format(RedisSentinelConfigEnum.PORT.getValue(), sentinelPort));
        configs.add(RedisSentinelConfigEnum.DIR.getKey() + " " + RedisSentinelConfigEnum.DIR.getValue());
        configs.add(RedisSentinelConfigEnum.MONITOR.getKey() + " " + String.format(RedisSentinelConfigEnum.MONITOR.getValue(), masterName, host, port, 1));
        configs.add(RedisSentinelConfigEnum.DOWN_AFTER_MILLISECONDS.getKey() + " " + String
                .format(RedisSentinelConfigEnum.DOWN_AFTER_MILLISECONDS.getValue(), masterName));
        configs.add(RedisSentinelConfigEnum.FAILOVER_TIMEOUT.getKey() + " " + String
                .format(RedisSentinelConfigEnum.FAILOVER_TIMEOUT.getValue(), masterName));
        configs.add(RedisSentinelConfigEnum.PARALLEL_SYNCS.getKey() + " " + String
                .format(RedisSentinelConfigEnum.PARALLEL_SYNCS.getValue(), masterName));
        return configs;
    }

    @Override
    public List<String> handleClusterDefaultConfig(int port) {
        List<String> configs = new ArrayList<String>();
        for (RedisClusterConfigEnum config : RedisClusterConfigEnum.values()) {
            if (config.equals(RedisClusterConfigEnum.CLUSTER_CONFIG_FILE)) {
                configs.add(RedisClusterConfigEnum.CLUSTER_CONFIG_FILE.getKey() + " "
                        + String.format(RedisClusterConfigEnum.CLUSTER_CONFIG_FILE.getValue(), port));
            } else {
                configs.add(config.getKey() + " "
                        + config.getValue());
            }
        }
        return configs;
    }

    @Override
    public int addTemplate(ConfigTemplate configTemplate) {
        return instanceConfigDao.addTemplate(configTemplate);
    }
    
    /**
     * 添加固定配置项，这些配置项是不能够被页面配置的
     * @param host
     * @param configList
     */
    private void handleSpecialConfig(String host,List<String> configList){
    	//remove old bind value
    	Iterator<String> configItrator = configList.iterator();
    	while (configItrator.hasNext())
    	{
    		
    		String keyValuePair = configItrator.next();
    		String keyName = keyValuePair.split(ConstUtils.SPACE)[0];
    		if (keyName.equals(RedisConfigEnum.BIND.getKey()) || keyName.equals(RedisConfigEnum.DAEMONIZE.getKey()))
    		{
    			configItrator.remove();   			
    		}
    		
    		
    		//为了兼容3.0.x 版本加入已有的redis cluster集群，对procted_mode 做如下特殊配置
    		if (keyName.equals(RedisConfigEnum.PROCTED_MODE.getKey())){
    			String version = SSHUtil.getRedisVersion(host);
    			if (versionLessThan(version, "3.2.0")){
    				configItrator.remove();
    			}
    		}
    	}
    	
    	configList.add(combineConfigKeyValue(RedisConfigEnum.BIND.getKey(), host + " 127.0.0.1"));
    	configList.add(combineConfigKeyValue(RedisConfigEnum.DAEMONIZE.getKey(), "no"));
    }
    
    /**
     * return v1<v2
     * @param v1
     * @param v2
     * @return
     */
    private boolean versionLessThan(String v1,String v2){
    	
            if (v1.trim().equals(v2)) {
                return false;
            }
            String[] version1Array = v1.split("[._]");
            String[] version2Array = v2.split("[._]");
            int minLen = Math.min(version1Array.length, version2Array.length);
            
            for (int i=0;i<minLen;i++){
            	int v1Num = Integer.parseInt(version1Array[i]);
            	int v2Num = Integer.parseInt(version2Array[i]);
            	if (v1Num == v2Num){
            		continue;
            	}else{
            		return v1Num < v2Num;          		
            	}
            }
            
            return false;
    }
    
    

    @Override
    public int updateTemplateInfo(int id, String name, String extraDesc) {
        return instanceConfigDao.updateTemplateInfo(id, name, extraDesc);
    }

    @Override
    public void removeByTemplateId(int templateId) {
        try {
            instanceConfigDao.removeTemplateById(templateId);
            instanceConfigDao.removeTemplateConfigByTemplateId(templateId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 组合
     * @param configKey
     * @param configValue
     * @return
     */
    private String combineConfigKeyValue(String configKey, String configValue) {
        return configKey + ConstUtils.SPACE + configValue;
    }

    public void setInstanceConfigDao(InstanceConfigDao instanceConfigDao) {
        this.instanceConfigDao = instanceConfigDao;
    }

    public List<ConfigTemplate> getAllTemplate() {
        return instanceConfigDao.getAllTemplate();
    }

    public List<ConfigTemplate> getTemplateByArchitecture(int architecture) {
        return instanceConfigDao.getTemplateByArchitecture(architecture);
    }

    public ConfigTemplate getTemplateById(int id) {
        return instanceConfigDao.getTemplateById(id);
    }
}
