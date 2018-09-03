package com.sohu.cache.dao;

import java.util.List;

import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppSearch;

import com.sohu.cache.entity.BusinessGroup;
import org.apache.ibatis.annotations.Param;

/**
 * 基于app的dao操作
 * @author leifu
 * @Date 2014年5月15日
 * @Time 下午1:58:22
 */
public interface AppDao {
    /**
     * 通过appId获取对应的app
     * @param appId
     * @return
     */
    public AppDesc getAppDescById(@Param("appId") long appId);
    
    /**
     * 通过应用名获取对应app
     * @param appName
     * @return
     */
	public AppDesc getByAppName(@Param("appName") String appName);
	
	/**
	 * 保存app
	 * @param appDesc
	 * @return
	 */
	public int save(AppDesc appDesc);
	
	/**
	 * 更新app
	 * @param appDesc
	 * @return
	 */
	public int update(AppDesc appDesc);
	
	/**
	 * 删除app
	 * @param id
	 * @return
	 */
	public int delete(@Param("id") Long id);

	/**
	 * 获取用户拥有的应用
	 * @param userId
	 * @return
	 */
    public List<AppDesc> getAppDescList(@Param("userId") long userId);
    
    /**
     * 获取应用拥有的应用个数
     * @param userId
     * @return
     */
    public int getUserAppCount(@Param("userId") long userId);

    /**
     * 获取所有应用
     * @param userId
     * @return
     */
    public List<AppDesc> getAllAppDescList(AppSearch appSearch);

    /**
     * 获取应用个数
     * @param appSearch
     * @return
     */
    public int getAllAppCount(AppSearch appSearch);

    /**
     * 更新appKey
     * @param appId
     * @param appKey
     */
    public void updateAppKey(@Param("appId") long appId, @Param("appKey") String appKey);

	/**
	 * 更新type
	 */
	public void updateAppType(@Param("appId")long appId, @Param("type")int redisType);

	public void updateAppTemplateId(@Param("appId")long appId, @Param("templateId")int templateId);

	public List<BusinessGroup> getAllBusinessGroup();

	public String getGroupName(@Param("businessGroupId") long businessGroupId);
	
	public void updateAppPwd(@Param("appId")long appId, @Param("pwd")String pwd);
}
