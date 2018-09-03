package com.sohu.cache.stats.app;

import com.alibaba.fastjson.JSONObject;
import com.sohu.cache.constant.Result;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.redis.ReshardProcess;
import org.springframework.ui.Model;
import redis.clients.jedis.HostAndPort;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * app相关发布操作
 * Created by yijunzhang on 14-10-20.
 */
public interface AppDeployCenter {

    /**
     * 新建应用
     *
     * @param appDesc
     * @param appUser
     * @param memSize
     */
    public boolean createApp(AppDesc appDesc, AppUser appUser, String memSize);

    /**
     * 为应用分配资源
     *
     * @param appAuditId
     * @param nodeInfoList <br/>格式=masterIp:空间:slaveIp
     * @param auditUser
     * @return
     */
    public boolean allocateResourceApp(Long appAuditId, List<String> nodeInfoList, AppUser auditUser);


    /**
     * 为应用分配的资源格式检测
     * @param appAuditId
     * @param appDeployText
     * @return
     */
    public Result checkAppDeployDetail(Long appAuditId, String appDeployText);

    /**
     * 下线应用
     *
     * @param appId
     * @return
     */
    public boolean offLineApp(Long appId);

    /**
     * 修改应用下节点配置
     *
     * @param appId
     * @param appAuditId
     * @param key
     * @param value
     * @return
     */
    public boolean modifyAppConfig(Long appId, Long appAuditId, String key, String value);

    /**
     * 垂直扩展
     *
     * @param appId
     * @param appAuditId
     * @param memory 单位MB
     * @return
     */
    public boolean verticalExpansion(Long appId, Long appAuditId, int memory);

    /**
     * 添加cluster一个主(从)节点
     *
     * @param appId
     * @param masterHost
     * @param slaveHost 从节点可为空
     * @param memory
     * @return
     */
    public boolean addAppClusterSharding(Long appId, String masterHost, String slaveHost, int memory);

    /**
     * 下线集群节点
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public boolean offLineClusterNode(Long appId, String host, int port);
    
    /**
     * 清理应用数据
     * @param appId
     * @param appUser
     * @return
     */
    public boolean cleanAppData(long appId, AppUser appUser, JSONObject errMsg);

    /**
     * 更新audit的param2字段
     */
    public void updateAuditType(long appAuditId, int redisType);

    /**
     * 更新appDesc的存储类型
     */
    public void updateAppType(long appId, int redisType);

    /**
     * 更新appDesc的模板id
     */
    public void updateAppTemplateId(long appId, int templateId);

    /**
     * 添加cluster一个主(从)节点
     *
     * @param appId
     * @param masterHost
     * @param slaveHost 从节点可为空
     * @param memory
     * @param 本次分配的大小
     * @return
     */
	boolean horizontalExpansion(Long appId, String host, int port, Long appAuditId, Set<HostAndPort> clusterHosts);
	
	Set<HostAndPort> getHosts(Long appId);

	/**
	 * 节点迁移
	 * @param appId
	 * @param appAuditId
	 * @param migrationInfo 
	 * @return
	 */
	boolean nodeMigrate(Long appId, Long appAuditId, String migrationInfo);

	/**
	 * 获取当前应用水平扩展进度列表
	 * @param appId
	 * @return
	 */
	Map<String, ReshardProcess> getHorizontalProcess(Long appId);

	
	/**
	 * 完成尚未迁移的slot,通常这些未迁移的slot是因为其他上线，下线，或者迁移节点意外中断导致的。
	 * @param appId
	 * @param appAuditId
	 * @param 并发线程数
	 * @return
	 */
	int finishSlotMigrate(Long appId, Long appAuditId, int threadNum);

	/**
	 * 批量添加slave文本格式检查
	 * @param appId
	 * @param appDeployText
	 * @return
	 */
	Result batchAddSlaveCheck(Long appId, String appDeployText);

	/**
	 * 批量添加slave部署
	 * @param appId
	 * @param slaveText
	 * @return
	 * @throws Exception 
	 */
	Result batchAddSlaveDeploy(Long appId, String slaveText) throws Exception;

	/**
	 * 检测当前部署配置是否是重复提交的
	 * @param deployConfigId
	 * @return
	 */
	public boolean isDeployConfigDuplicate(long deployConfigId);
	
}
