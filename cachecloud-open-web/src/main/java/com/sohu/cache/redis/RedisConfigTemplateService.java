package com.sohu.cache.redis;

import java.util.List;

import com.sohu.cache.entity.ConfigTemplate;
import com.sohu.cache.entity.InstanceConfig;

/**
 * redis配置模板服务
 * @author leifu
 * @Date 2016年6月23日
 * @Time 下午2:08:03
 */
public interface RedisConfigTemplateService {

    /**
     * 获取所有配置模板列表
     * @return
     */
    List<InstanceConfig> getAllInstanceConfig();

    /**
     * 根据type获取配置模板列表
     * 
     * @param type
     * @return
     */
    List<InstanceConfig> getByType(int type);

    /**
     * 根据templateId获取配置模板列表
     *
     * @param templateId
     * @return
     */
    List<InstanceConfig> getByTemplateId(int templateId);

    /**
     * 根据templateId删除模板
     *
     * @param templateId
     * @return
     */
    void removeByTemplateId(int templateId);

    /**
     * 保存或者更新配置模板
     * 
     * @param instanceConfig
     * @return
     */
    int saveOrUpdate(InstanceConfig instanceConfig);

    /**
     * 根据id获取配置模板
     * 
     * @param id
     * @return
     */
    InstanceConfig getById(long id);

    /**
     * 根据configKey和type获取配置
     * 
     * @param configKey
     * @param type
     * @return
     */
    InstanceConfig getByConfigKeyAndType(String configKey, int type);

    /**
     * 更改配置状态
     * @param id
     * @param status
     * @return
     */
    int updateStatus(long id, int status);
    
    /**
     * 删除配置
     * @param id
     */
    int remove(long id);

    /**
     * 普通节点配置
     * @param host
     * @param port
     * @param maxMemory
     * @param templateId
     * @return
     */
    List<String> handleConfig(String host,int port, int maxMemory, int templateId);

    /**
     * sentinel节点配置
     * @param masterName
     * @param host
     * @param port
     * @param sentinelPort
     * @param templateId
     * @return
     */
    List<String> handleSentinelConfig(String masterName, String host, int port,String sentinelHost, int sentinelPort, int templateId);

    /**
     * cluster节点配置
     * @param host
     * @param port
     * @return
     */
    List<String> handleClusterConfig(String host,int port, int templateId);


    /**
     * sentinel节点默认配置
     * @param masterName
     * @param host
     * @param port
     * @param sentinelPort
     * @param quorum
     * @return
     */
    List<String> handleSentinelDefaultConfig(String masterName, String host, int port, int sentinelPort);
    
    /**
     * cluster节点默认配置
     * @param port
     * @return
     */
    List<String> handleClusterDefaultConfig(int port);

    /**
     * 获取所有模板
     * @return
     */
    List<ConfigTemplate> getAllTemplate();

    /**
     * 获取architecture类型的模板
     * @return
     */
    List<ConfigTemplate> getTemplateByArchitecture(int architecture);

    /**
     * 根据id获取模板
     * @return
     */
    ConfigTemplate getTemplateById(int id);

    /**
     * 根据模板id获取模板，并过滤掉无效的配置项
     * @return
     */
    List<String> getByTemplateIdAndFilterUneffectiveConfig(int templateId);

    /**
     * 创建新模板
     */
    int addTemplate(ConfigTemplate configTemplate);

    /**
     * 更新模板信息
     */
    int updateTemplateInfo(int id, String name, String extraDesc);

    /**
     * 普通节点默认配置
     * @param port
     * @param maxMemory
     * @param host
     * @return
     */
	List<String> handleCommonDefaultConfig(String host, int port, int maxMemory);
}
