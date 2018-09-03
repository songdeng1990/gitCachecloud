package com.sohu.cache.inspect.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.sohu.cache.alert.impl.BaseAlertService;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.inspect.InspectParamEnum;
import com.sohu.cache.inspect.Inspector;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.stats.app.AppStatsCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.vo.AppDetailVO;

/**
 * 曾经出现过集群存在非法节点，偶然加入两个集群，导致两个集群合并，数据大量丢失的问题，所以非法存在于redis集群的节点必须立即删除。
 * 
 * @author dengsong
 *
 */
public class AppClusterInspector extends BaseAlertService implements Inspector {

	/**
	 * app统计相关
	 */
	private AppStatsCenter appStatsCenter;

	@Override
	public boolean inspect(Map<InspectParamEnum, Object> paramMap) {
		Long appId = MapUtils.getLong(paramMap, InspectParamEnum.SPLIT_KEY);
		AppDetailVO appDetailVO = appStatsCenter.getAppDetail(appId);
		AppDesc appDesc = appDetailVO.getAppDesc();
		if (appDesc.getType() != ConstUtils.CACHE_TYPE_REDIS_CLUSTER) {
			return true;
		}
		List<InstanceInfo> appInstanceInfoList = (List<InstanceInfo>) paramMap.get(InspectParamEnum.INSTANCE_LIST);
		int globalClusterSize = -1;
		for (InstanceInfo info : appInstanceInfoList) {
			if (!info.isOffline()) {

				int localClusterSize = RedisUtil.getClusterSize(info.getIp(), info.getPort());
				if (localClusterSize > 0) {
					if (globalClusterSize == -1) {
						globalClusterSize = localClusterSize;
						logger.info("app {} {} instance {} {} 得到cluster size {}", appId, appDesc.getName(), info.getIp(), info.getPort(), globalClusterSize);
					} else if (globalClusterSize != localClusterSize) {
						String content = String.format("app %s %s instance %s %d 上执行 cluster info 得到的cluster_known_nodes的值 %d 与其它节点 %d不同，可能存在节点没有删除干净，请立即对比检查.", appId, appDesc.getName(), info.getIp(), info.getPort(),
								localClusterSize, globalClusterSize);
						logger.error(content);
						mobileAlertComponent.sendPhone(content, appDetailVO.getPhoneList());
						return true;
					}
				}
			}
		}

		if (globalClusterSize <= 0) {
			return true;
		}
		if (globalClusterSize != appInstanceInfoList.size()) {
			logger.error("app {} {} cluster size {}, is not the same as instanceNum in db {} ", appId, appDesc.getName(), globalClusterSize, appInstanceInfoList.size());
			String content = String.format("app %s %s redis 集群节点数 %d 与数据库登记的 实例数目 %d 不一致,请立即检查，如果存在数据未登记的节点，必须立即删除！！", appId, appDesc.getName(), globalClusterSize, appInstanceInfoList.size());
			mobileAlertComponent.sendPhone(content, appDetailVO.getPhoneList());
		} else {
			logger.info("app {} {} cluster size {}, is the same as instanceNum in db {} ", appId, appDesc.getName(), globalClusterSize, appInstanceInfoList.size());
		}
		if (CollectionUtils.isEmpty(appInstanceInfoList)) {
			logger.warn("appId {} instanceList is empty", appId);
			return true;
		}
		return true;
	}

	/**
	 * 应用clusterSize 与instance 不一致告警
	 * 
	 * @param appDetailVO
	 * @param appClientConnThreshold
	 * @param instanceCount
	 */
	private void alertAppClientConn(final AppDetailVO appDetailVO, final int appClientConnThreshold, final int instanceCount) {
		AppDesc appDesc = appDetailVO.getAppDesc();
		String content = String.format("应用(%s,%s)-客户端连接数报警-预设阀值每个分片为%s-现已达到%s(分片个数:%s)-请及时关注", appDesc.getAppId(), appDesc.getName(), appClientConnThreshold, appDetailVO.getConn(), instanceCount);
		String title = "CacheCloud系统-客户端连接数报警";
		logger.warn("app title {}", title);
		logger.warn("app content {}", content);
		/*
		 * emailComponent.sendMail(title, content, appDetailVO.getEmailList(),
		 * Arrays.asList(emailComponent.getAdminEmail().split(ConstUtils.COMMA))
		 * );
		 */
		mobileAlertComponent.sendPhone(content, appDetailVO.getPhoneList());
	}

	public void setAppStatsCenter(AppStatsCenter appStatsCenter) {
		this.appStatsCenter = appStatsCenter;
	}

}
