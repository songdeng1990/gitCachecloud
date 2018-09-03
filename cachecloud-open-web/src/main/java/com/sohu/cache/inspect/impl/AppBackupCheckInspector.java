package com.sohu.cache.inspect.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.sohu.cache.alert.impl.BaseAlertService;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.inspect.InspectParamEnum;
import com.sohu.cache.inspect.Inspector;
import com.sohu.cache.redis.RedisUtil;
import com.sohu.cache.ssh.SSHUtil;
import com.sohu.cache.stats.app.AppStatsCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.vo.AppDetailVO;

import redis.clients.jedis.Jedis;

/**
 * 曾经出现过集群存在非法节点，偶然加入两个集群，导致两个集群合并，数据大量丢失的问题，所以非法存在于redis集群的节点必须立即删除。
 * 
 * @author dengsong
 *
 */
public class AppBackupCheckInspector extends BaseAlertService implements Inspector {

	/**
	 * app统计相关
	 */
	private AppStatsCenter appStatsCenter;

	@Override
	public boolean inspect(Map<InspectParamEnum, Object> paramMap) {
		Long appId = MapUtils.getLong(paramMap, InspectParamEnum.SPLIT_KEY);
		AppDetailVO appDetailVO = appStatsCenter.getAppDetail(appId);
		AppDesc appDesc = appDetailVO.getAppDesc();
		
		List<InstanceInfo> instanceListRaw = (List<InstanceInfo>) paramMap.get(InspectParamEnum.INSTANCE_LIST);
		List<InstanceInfo> instanceList = new ArrayList<InstanceInfo>();
		for (InstanceInfo info : instanceListRaw){
			if (TypeUtil.isRedisDataType(info.getType())){
				instanceList.add(info);
			}
		}
		boolean checkResult = true;
		if (isTimeNow(ConstUtils.BACKUP_CHECK_TIME) && appDesc.getBackupDays() > 0){
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			String dateString = format.format(new Date());
			List<String> backupList = new ArrayList<String>();
			InstanceInfo info = instanceList.get(0);
			String backupDir = ConstUtils.BAKCUP_DIR + "/" + appId + "/" + dateString;
			Jedis jedis = null;
			try {
				jedis = new Jedis(info.getIp(),info.getPort());
				if (!RedisUtil.isPersistenceDisabled(jedis)){
					String dir = SSHUtil.execute(info.getIp(), "ls -t " + backupDir);
					String[] array = dir.split("\\s");
					if (array.length > 0) {
						for (String backup : array) {
							backupList.add(backup);
						}
					}
					
					checkResult = (backupList.size() == appDetailVO.getMasterNum());
				}else{
					logger.warn("app {} {} {} redis 没有持久化，无需备份 ",appId, appDesc.getName(),backupDir);
					return true;
				}
				
			} catch (Exception e) {
				logger.error("", e);
				checkResult = false;
			} finally{
				jedis.close();
			}
			
			if (!checkResult){
				String content = String.format("app %s %s %s redis 备份异常 ，请检查。", appId, appDesc.getName(),backupDir);
				mobileAlertComponent.sendPhone(content, appDetailVO.getPhoneList());
			}else{
				logger.warn("app {} {} {} redis 备份完整 ",appId, appDesc.getName(),backupDir);
			}
				
			
		}
		
		return true;
	}
	
	private boolean isTimeNow(String time) {

		try {
			String start = time.split("-")[0];
			String end = time.split("-")[1];
			SimpleDateFormat sp = new SimpleDateFormat("HH:mm");
			Date startTime = sp.parse(start);
			Date endTime = sp.parse(end);
			Date now = sp.parse(sp.format(new Date()));
			return now.getTime() > startTime.getTime() && now.getTime() < endTime.getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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
