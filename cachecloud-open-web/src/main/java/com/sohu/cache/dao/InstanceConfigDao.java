package com.sohu.cache.dao;

import java.util.List;

import com.sohu.cache.entity.ConfigTemplate;
import org.apache.ibatis.annotations.Param;

import com.sohu.cache.entity.InstanceConfig;

/**
 * 配置模板Dao
 * 
 * @author leifu
 * @Date 2016年6月22日
 * @Time 下午5:46:37
 */
public interface InstanceConfigDao {
    
    /**
     * 获取所有配置模板
     * @return
     */
    List<InstanceConfig> getAllInstanceConfig();

    /**
     * 根据type获取配置模板列表
     * 
     * @param type
     * @return
     */
    List<InstanceConfig> getByType(@Param("type") int type);

    /**
     * 根据templateId获取配置模板列表
     *
     * @param templateId
     * @return
     */
    List<InstanceConfig> getByTemplateId(@Param("templateId") int templateId);

    /**
     * 获取全部模板
     * @return
     */
    List<ConfigTemplate> getAllTemplate();

    /**
     * 获取type类型的模板
     * @return
     */
    List<ConfigTemplate> getTemplateByArchitecture(@Param("architecture")int architecture);

    /**
     * 根据id获取模板
     */
    ConfigTemplate getTemplateById(@Param("id") int id);

    /**
     * 新建模板
     * @return
     */
    int addTemplate(ConfigTemplate configTemplate);

    /**
     * 更新模板信息
     */
    int updateTemplateInfo(@Param("id") int id, @Param("name") String name, @Param("extraDesc") String extraDesc);

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
    InstanceConfig getById(@Param("id") long id);

    /**
     * 根据configKey和type获取配置
     * 
     * @param configKey
     * @param type
     * @return
     */
    InstanceConfig getByConfigKeyAndType(@Param("configKey") String configKey, @Param("type") int type);

    /**
     * 更改配置状态
     * @param id
     * @param status
     * @return
     */
    int updateStatus(@Param("id") long id, @Param("status") int status);

    /**
     * 删除配置
     * @param id
     * @return
     */
    int remove(@Param("id") long id);

    /**
     * 根据templateId删除配置模板
     * @param templateId
     * @return
     */
    void removeTemplateById(@Param("templateId") int templateId);

    /**
     * 根据templateId删除模板配置项
     *
     * @param templateId
     * @return
     */
    void removeTemplateConfigByTemplateId(@Param("templateId") int templateId);
}
