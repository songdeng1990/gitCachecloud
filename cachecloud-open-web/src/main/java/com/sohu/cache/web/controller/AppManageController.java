package com.sohu.cache.web.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sohu.cache.constant.AppCheckEnum;
import com.sohu.cache.constant.Result;
import com.sohu.cache.constant.ErrorMessageEnum;
import com.sohu.cache.entity.AppAudit;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.MachineStats;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.redis.RedisCenter;
import com.sohu.cache.redis.RedisClusterReshard;
import com.sohu.cache.redis.RedisConfigTemplateService;
import com.sohu.cache.redis.RedisDeployCenter;
import com.sohu.cache.redis.ReshardProcess;
import com.sohu.cache.stats.app.AppDailyDataCenter;
import com.sohu.cache.stats.app.AppDeployCenter;
import com.sohu.cache.stats.instance.InstanceDeployCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.enums.RedisOperateEnum;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.AppEmailUtil;
import com.sohu.cache.web.util.DateUtil;

import net.sf.json.JSONArray;
import redis.clients.jedis.HostAndPort;
import redis.clients.util.ClusterNodeInformation;

/**
 * 应用后台管理
 *
 * @author leifu
 * @Time 2014年7月3日
 */
@Controller
@RequestMapping("manage/app")
public class AppManageController extends BaseController {

	private Logger logger = LoggerFactory.getLogger(AppManageController.class);

	@Resource(name = "machineCenter")
	private MachineCenter machineCenter;

	@Resource(name = "appEmailUtil")
	private AppEmailUtil appEmailUtil;

	@Resource(name = "appDeployCenter")
	private AppDeployCenter appDeployCenter;

	@Resource(name = "redisCenter")
	private RedisCenter redisCenter;

	@Resource(name = "redisDeployCenter")
	private RedisDeployCenter redisDeployCenter;
	
	@Resource(name = "instanceDeployCenter")
	private InstanceDeployCenter instanceDeployCenter;

	@Resource(name = "appDailyDataCenter")
    private AppDailyDataCenter appDailyDataCenter;

	@Resource(name = "redisConfigTemplateService")
	private RedisConfigTemplateService redisConfigTemplateService;
	
	@RequestMapping("/appDaily")
    public ModelAndView appDaily(HttpServletRequest request, HttpServletResponse response, Model model) throws ParseException {
	    AppUser userInfo = getUserInfo(request);
        logger.warn("user {} want to send appdaily", userInfo.getName());
        if (ConstUtils.SUPER_MANAGER.contains(userInfo.getName())) {
            Date startDate;
            Date endDate;
            String startDateParam = request.getParameter("startDate");
            String endDateParam = request.getParameter("endDate");
            if (StringUtils.isBlank(startDateParam) || StringUtils.isBlank(endDateParam)) {
                endDate = new Date();
                startDate = DateUtils.addDays(endDate, -1);
            } else {
                startDate = DateUtil.parseYYYY_MM_dd(startDateParam);
                endDate = DateUtil.parseYYYY_MM_dd(endDateParam);
            }
            long appId = NumberUtils.toLong(request.getParameter("appId"));
            if (appId > 0) {
                appDailyDataCenter.sendAppDailyEmail(appId, startDate, endDate);
            } else {
                appDailyDataCenter.sendAppDailyEmail();
            }
            model.addAttribute("msg", "success!");
        } else {
            model.addAttribute("msg", "no power!");
        }
        return new ModelAndView("");
    }
	
	/**
	 * 审核列表
	 * 
	 * @param status 审核状态
	 * @param type 申请类型
	 */
	@RequestMapping(value = "/auditList")
	public ModelAndView doAppAuditList(HttpServletRequest request,HttpServletResponse response, Model model,
	        Integer status, Integer type) {
	    //获取审核列表
		List<AppAudit> list = appService.getAppAudits(status, type);

		model.addAttribute("list", list);
		model.addAttribute("status", status);
		model.addAttribute("type", type);
		model.addAttribute("checkActive", SuccessEnum.SUCCESS.value());

		return new ModelAndView("manage/appAudit/list");
	}

	/**
	 * 处理应用配置修改
	 * 
	 * @param appAuditId 审批id
	 */
	@RequestMapping(value = "/initAppConfigChange")
	public ModelAndView doInitAppConfigChange(HttpServletRequest request,
			HttpServletResponse response, Model model, Long appAuditId) {
		// 申请原因
		AppAudit appAudit = appService.getAppAuditById(appAuditId);
		model.addAttribute("appAudit", appAudit);

		// 用第一个参数存实例id
		Long instanceId = NumberUtils.toLong(appAudit.getParam1());
		Map<String, String> redisConfigList = redisCenter.getRedisConfigList(instanceId.intValue());
		model.addAttribute("redisConfigList", redisConfigList);
		model.addAttribute("instanceId", instanceId);

		// 实例列表
		List<InstanceInfo> instanceList = appService.getAppInstanceInfo(appAudit.getAppId());
		model.addAttribute("instanceList", instanceList);
		model.addAttribute("appId", appAudit.getAppId());
		model.addAttribute("appAuditId", appAuditId);

		// 修改配置的键值对
		model.addAttribute("appConfigKey", appAudit.getParam2());
		model.addAttribute("appConfigValue", appAudit.getParam3());

		return new ModelAndView("manage/appAudit/initAppConfigChange");
	}

	/**
	 * 添加应用配置修改
	 * 
	 * @param appId 应用id
	 * @param appConfigKey 配置项
	 * @param appConfigValue 配置值
	 * @param appAuditId 审批id
	 */
	@RequestMapping(value = "/addAppConfigChange")
	public ModelAndView doAddAppConfigChange(HttpServletRequest request,
			HttpServletResponse response, Model model, Long appId,
			String appConfigKey, String appConfigValue, Long appAuditId) {
	    AppUser appUser = getUserInfo(request);
        logger.warn("user {} change appConfig:appId={};key={};value={},appAuditId:{}", appUser.getName(), appId, appConfigKey, appConfigValue, appAuditId);
        boolean isModify = false;
        if (appId != null && appAuditId != null && StringUtils.isNotBlank(appConfigKey) && StringUtils.isNotBlank(appConfigValue)) {
			try {
				isModify = appDeployCenter.modifyAppConfig(appId, appAuditId, appConfigKey, appConfigValue);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
        logger.warn("user {} change appConfig:appId={};key={};value={},appAuditId:{},result is:{}", appUser.getName(), appId, appConfigKey, appConfigValue, appAuditId, isModify);
		return new ModelAndView("redirect:/manage/app/auditList");
	}

	/**
	 * 初始化水平扩容申请
	 */
	@RequestMapping(value = "/initHorizontalScaleApply")
	public ModelAndView doInitHorizontalScaleApply(HttpServletRequest request, HttpServletResponse response, Model model, Long appAuditId,String ipLike,String extraDesc) {
		// 1. 审批
		AppAudit appAudit = appService.getAppAuditById(appAuditId);
		model.addAttribute("appAudit", appAudit);
		model.addAttribute("appId", appAudit.getAppId());

		// 2. 从实例列表中获取应用的机器列表
		Set<String> appHostSet = new HashSet<String>();
		List<InstanceInfo> instanceList = appService.getAppInstanceInfo(appAudit.getAppId());
		for (InstanceInfo instanceInfo : instanceList) {
			if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
				continue;
			}
			appHostSet.add(instanceInfo.getIp());
		}

		// 3. 机器列表
		List<MachineStats> machineList = machineCenter.getAllMachineStats(ipLike,extraDesc);

		// 4. 排序
		machineList = sortByAppAndMachineTotalMem(appHostSet, machineList);

		model.addAttribute("machineList", machineList);
		model.addAttribute("appMachineSize", appHostSet.size());

		return new ModelAndView("manage/appAudit/initHorizontalScaleApply");
	}

	/**
	 * 添加分片
	 * 
	 * @return
	 */
	@RequestMapping(value = "/addAppClusterSharding")
	public ModelAndView doAddAppClusterSharding(HttpServletRequest request,
			HttpServletResponse response, Model model, String addNewNodeInputText,
			Long appAuditId,Long deployConfigId) {
		
		if (appDeployCenter.isDeployConfigDuplicate(deployConfigId)){
			model.addAttribute("status","0");
			model.addAttribute("msg", "你重复提交了安装配置，此次请求忽略。");
			return new ModelAndView("");
		}
		
	    AppUser appUser = getUserInfo(request);
	    String[] masterSizeSlaveArray = addNewNodeInputText.split(ConstUtils.NEXT_LINE);
	    boolean isAdd = false;
        for (String masterSizeSlave : masterSizeSlaveArray)       {        	
        	logger.warn("user {} addAppClusterSharding:{}", appUser.getName(), masterSizeSlave);
        	masterSizeSlave = masterSizeSlave.trim();
    		isAdd = false;
    		if (StringUtils.isNotBlank(masterSizeSlave) && appAuditId != null) {
    			AppAudit appAudit = appService.getAppAuditById(appAuditId);
    			// 解析配置
    			String[] configArr = masterSizeSlave.split(":");
    			if (configArr.length >= 2) {
    				String masterHost = configArr[0];
    				String memSize = configArr[1];
    				int memSizeInt = NumberUtils.toInt(memSize);
    				String slaveHost = null;
    				if (configArr.length >= 3) {
    					slaveHost = configArr[2];
    				}
    				if (memSizeInt > 0) {
    					try {
    						isAdd = appDeployCenter.addAppClusterSharding(appAudit.getAppId(), masterHost, slaveHost, memSizeInt);
    					} catch (Exception e) {
    						logger.error(e.getMessage(), e);
    					}
    				}
    			} else {
    				logger.error("addAppClusterSharding param size error, addAppClusterSharding:{}", masterSizeSlave);
    			}
    		}
    		
    		logger.warn("addAppClusterSharding:{}, result is {},terminated.", masterSizeSlave, isAdd);
    		if (!isAdd){
    			break;
    		}
        }
        
        if (isAdd){
        	model.addAttribute("status","1");
        	model.addAttribute("msg","新增节点部署成功，将跳转到数据均衡页面。");
        	model.addAttribute("auditId",appAuditId);
        	
        }else{
        	model.addAttribute("status","0");
        	model.addAttribute("msg","部署异常，请检查cachecloud日志~");
        }
        
        return new ModelAndView("");
	}

	/**
	 * 水平扩容初始化
	 * 
	 * @param appAuditId
	 */
	@RequestMapping(value = "/handleHorizontalScale")
	public ModelAndView doHandleHorizontalScale(HttpServletRequest request,
			HttpServletResponse response, Model model, Long appAuditId) {
		// 1. 审批
		AppAudit appAudit = appService.getAppAuditById(appAuditId);
		model.addAttribute("appAudit", appAudit);
		model.addAttribute("appId", appAudit.getAppId());

		// 2. 进度
		Map<String, ReshardProcess> appScaleProcessMap = appDeployCenter.getHorizontalProcess(appAudit.getAppId());
		model.addAttribute("appScaleProcessMap", appScaleProcessMap);

		// 3. 实例列表和统计
		fillAppInstanceStats(appAudit.getAppId(), model);
		// 4. 实例所在机器信息
		fillAppMachineStat(appAudit.getAppId(), model);

		return new ModelAndView("manage/appAudit/handleHorizontalScale");
	}

	/**
	 * 显示reshard进度
	 */
	@RequestMapping(value = "/showReshardProcess")
	public ModelAndView doShowReshardProcess(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		Map<String, ReshardProcess> appScaleProcessMap = appDeployCenter.getHorizontalProcess(appId);
		write(response, filterMapToJsonArray(appScaleProcessMap));
		return null;
	}

	/**
	 * 把Map组装成JsonArray
	 * 
	 * @param appScaleProcessMap
	 * @return
	 */
	private String filterMapToJsonArray(Map<String, ReshardProcess> appScaleProcessMap) {
		if (MapUtils.isEmpty(appScaleProcessMap)) {
			return "[]";
		}
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (Entry<String, ReshardProcess> entry : appScaleProcessMap.entrySet()) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("hostPort", entry.getKey());
			map.put("reshardSlot", entry.getValue().getReshardSlot());
			map.put("totalSlot", entry.getValue().getTotalSlot());
			map.put("status", entry.getValue().getStatus());
			list.add(map);
		}
		return JSONArray.fromObject(list).toString();
	}

	/**
	 * 添加水平扩容配置
	 * 
	 * @param ip
	 * @param port
	 * @param appId
	 * @param appAuditId
	 */
	@RequestMapping(value = "/addHorizontalScaleApply")
	public ModelAndView doAddHorizontalScaleApply(HttpServletRequest request,
			HttpServletResponse response, Model model, String onlineAddresses,
			Long appId, Long appAuditId) {
	    AppUser appUser = getUserInfo(request);
	    
	    if (StringUtils.isEmpty(onlineAddresses)){
	    	logger.error("addHorizontalScale param size error, onlineAddresses is empty");	    	
	    	return new ModelAndView("");
	    }

	    String[] ipPorts = onlineAddresses.split(ConstUtils.NEXT_LINE);
	    Set<HostAndPort> hosts = appDeployCenter.getHosts(appId);
	    for (int i=0;i<ipPorts.length;i++)
	    {	    	
	    	String ipPort = ipPorts[i].trim();
	    	String[] ipPortPair = ipPort.split(":");
	    	if (ipPortPair.length == 2){
	    		String ip = ipPortPair[0];
		    	int port = Integer.valueOf(ipPortPair[1]);
		    	logger.warn("user {} horizontalScaleApply param, ip:{}, port:{}, appId:{}", appUser.getName(), ip, port, appId);
		        boolean isSuccess = false;
				if (appId != null && StringUtils.isNotBlank(ip)) {
					try {
					    isSuccess = appDeployCenter.horizontalExpansion(appId, ip, port, appAuditId,hosts);
					} catch (Exception e) {
						logger.error("", e);
					}
				} else {
					logger.error("horizontalScaleApply error param, ip:{}, port:{}, appId:{}", ip, port, appId);
				}
		        logger.warn("user {} horizontalScaleApply param, ip:{}, port:{}, appId:{}, result is {}", appUser.getName(), ip, port, appId, isSuccess);
	    	}
	    	else{
	    		logger.error("addHorizontalScale param size error, addHorizontalScale:{}", ipPort);	    		
	    	}
	    }
        
		return new ModelAndView("redirect:/manage/app/handleHorizontalScale?appAuditId=" + appAuditId);
	}
	
	/**
	 * 迁移节点接口，将redis cluster中的一个节点的所有slot迁移到另外一个机器。
	 * @param request
	 * @param response
	 * @param model
	 * @param migrationInfo 节点迁移数据，每一行都是 srcip:srcport,dstip:dstport,threadNum(迁移并发线程数目)
	 * @param appId
	 * @param appAuditId
	 * @return
	 */
	@RequestMapping(value = "/addNodeMigrateApply")
	public ModelAndView doaddNodeMigrateApply(HttpServletRequest request,
			HttpServletResponse response, Model model, String migrationInfo,
			Long appId, Long appAuditId){
		AppUser appUser = getUserInfo(request);
		if (StringUtils.isEmpty(migrationInfo)){
	    	logger.error("doaddNodeMigrateApply error,migrationInfo is empty");	    	
	    	return new ModelAndView("");
	    }
		
		boolean isSuccess = false;
		try {
		    isSuccess = appDeployCenter.nodeMigrate(appId,appAuditId,migrationInfo);
		} catch (Exception e) {
			logger.error("",e);
		}
		
		logger.warn("user {} ,appId:{} doNodeMigrate migrationInfo {} , result is {}", appUser.getName(), appId, migrationInfo,isSuccess);
		return new ModelAndView("redirect:/manage/app/handleHorizontalScale?appAuditId=" + appAuditId);		
	}
	
	/**
	 * 完成因为意外被终端的slot迁移
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/finishSlotMigrate")
	public ModelAndView dofinishSlotMigrate(HttpServletRequest request,
			HttpServletResponse response, Model model,Long appId, Long appAuditId, String threadNum){
		try{
			appDeployCenter.finishSlotMigrate(appId, appAuditId, Integer.valueOf(threadNum));
		}catch(Exception e){
			logger.error("",e);
		}
		
		return new ModelAndView("redirect:/manage/app/handleHorizontalScale?appAuditId=" + appAuditId);
	}
	

	/**
	 * 下线分片
	 * 
	 * @param ip
	 * @param port
	 * @param appId
	 * @param appAuditId
	 * @return
	 */
	@RequestMapping(value = "/offLineHorizontalShard")
	public ModelAndView doOffLineHorizontalShard(HttpServletRequest request,
			HttpServletResponse response, Model model, String ip, Integer port,
			Long appId, Long appAuditId) {
	    AppUser appUser = getUserInfo(request);
		logger.warn("offLineHorizontalShard: user:{},ip:{},port:{},appId:{},appAuditId:{}", appUser.getName(), ip, port, appId, appAuditId);
		boolean isSuccess = false;
		if (appId != null && StringUtils.isNotBlank(ip) && port != null) {
			try {
				appDeployCenter.offLineClusterNode(appId, ip, port);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.error("offLineHorizontalShard error param, ip:{}, port:{}, appId:{}", ip, port, appId);
		}
	    logger.warn("offLineHorizontalShard: user:{},ip:{},port:{},appId:{},appAuditId:{},success is {}", appUser.getName(), ip, port, appId, appAuditId, isSuccess);
		return new ModelAndView("redirect:/manage/app/handleHorizontalScale?appAuditId=" + appAuditId);
	}

	/**
	 * 处理应用扩容
	 * 
	 * @param appAuditId 审批id
	 */
	@RequestMapping(value = "/initAppScaleApply")
	public ModelAndView doInitAppScaleApply(HttpServletRequest request, HttpServletResponse response, Model model, Long appAuditId) {
		// 申请原因
		AppAudit appAudit = appService.getAppAuditById(appAuditId);
		model.addAttribute("appAudit", appAudit);

		// 实例列表和统计
		fillAppInstanceStats(appAudit.getAppId(), model);
		// 实例所在机器信息
        fillAppMachineStat(appAudit.getAppId(), model);

		long appId = appAudit.getAppId();
		AppDesc appDesc = appService.getByAppId(appId);
        model.addAttribute("appAuditId", appAuditId);
		model.addAttribute("appId", appAudit.getAppId());
        model.addAttribute("appDesc", appDesc);
		
		return new ModelAndView("manage/appAudit/initAppScaleApply");
	}

	/**
	 * 添加扩容配置
	 * 
	 * @param appScaleText 扩容配置
	 * @param appAuditId 审批id
	 */
	@RequestMapping(value = "/addAppScaleApply")
	public ModelAndView doAddAppScaleApply(HttpServletRequest request,
			HttpServletResponse response, Model model, String appScaleText,
			Long appAuditId, Long appId) {
	    AppUser appUser = getUserInfo(request);
        logger.error("user {} appScaleApplay : appScaleText={},appAuditId:{}", appUser.getName(), appScaleText, appAuditId);
        boolean isSuccess = false;
		if (appAuditId != null && StringUtils.isNotBlank(appScaleText)) {
			int mem = NumberUtils.toInt(appScaleText, 0);
			try {
			    isSuccess = appDeployCenter.verticalExpansion(appId, appAuditId, mem);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.error("appScaleApplay error param: appScaleText={},appAuditId:{}", appScaleText, appAuditId);
		}
        logger.error("user {} appScaleApplay: appScaleText={},appAuditId:{}, result is {}", appUser.getName(), appScaleText, appAuditId, isSuccess);
		return new ModelAndView("redirect:/manage/app/auditList");
	}

	/**
	 * 初始化部署应用
	 * 
	 * @param appAuditId 审批id
	 * @return
	 */
	@RequestMapping(value = "/initAppDeploy")
	public ModelAndView doInitAppDeploy(HttpServletRequest request, HttpServletResponse response, Model model, Long appAuditId,String ipLike,String extraDesc) {
		// 申请原因
		AppAudit appAudit = appService.getAppAuditById(appAuditId);
		model.addAttribute("appAudit", appAudit);

		// 机器列表
		List<MachineStats> machineList = machineCenter.getAllMachineStats(ipLike, extraDesc);
		Set<String> groupSet = machineCenter.getGroups();
		model.addAttribute("machineList", machineList);
		model.addAttribute("appAuditId", appAuditId);
		model.addAttribute("appId", appAudit.getAppId());
		AppDesc appDesc = appService.getByAppId(appAudit.getAppId());
		model.addAttribute("appDesc", appDesc);
		model.addAttribute("ipLike",ipLike);
		model.addAttribute("extraDesc",extraDesc);
		model.addAttribute("groupSet",groupSet);
		model.addAttribute("templateList", redisConfigTemplateService.getTemplateByArchitecture(ConstUtils.CACHE_REDIS_STANDALONE));
		return new ModelAndView("manage/appAudit/initAppDeploy");
	}
	
	/**
     * 应用部署配置检查
     * @return
     */
    @RequestMapping(value = "/appDeployCheck")
    public ModelAndView doAppDeployCheck(HttpServletRequest request, HttpServletResponse response, Model model, String appDeployText,
            Long appAuditId, Long appId, int redisType, int templateId) {
        Result dataFormatCheckResult = null;
        try {
			//写入appAudit的存储类型
			appDeployCenter.updateAuditType(appAuditId, redisType);
			//写入appDesc的type
			appDeployCenter.updateAppType(appId, redisType);
			//写入appDesc的配置模板id
			appDeployCenter.updateAppTemplateId(appId, templateId);
			dataFormatCheckResult = appDeployCenter.checkAppDeployDetail(appAuditId, appDeployText);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataFormatCheckResult = Result.fail(ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
        }
        model.addAttribute("status", dataFormatCheckResult.getStatus());
        model.addAttribute("message", dataFormatCheckResult.getMessage());
        return new ModelAndView("");
    }
    
    /**
     * 水平扩展配置检查
     * @return
     */
    @RequestMapping(value = "/horizontalScaleCheck")
    public ModelAndView doAppHorizontalScaleCheck(HttpServletRequest request, HttpServletResponse response, Model model, String appDeployText,
            Long appAuditId) {
        Result dataFormatCheckResult = null;
        try {			
			dataFormatCheckResult = appDeployCenter.checkAppDeployDetail(appAuditId, appDeployText);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataFormatCheckResult = Result.fail(ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
        }
        model.addAttribute("status", dataFormatCheckResult.getStatus());
        model.addAttribute("message", dataFormatCheckResult.getMessage());
        return new ModelAndView("");
    }
    
    @RequestMapping("/batchAddSlaveCheck")
    public ModelAndView doBatchAddSlaveCheck(HttpServletRequest request, HttpServletResponse response, Model model, String slaveText,
            Long appId){
    	 Result dataFormatCheckResult = null;
         try {			
 			dataFormatCheckResult = appDeployCenter.batchAddSlaveCheck(appId, slaveText);
         } catch (Exception e) {
             logger.error(e.getMessage(), e);
             dataFormatCheckResult = Result.fail(ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
         }
         model.addAttribute("status", dataFormatCheckResult.getStatus());
         model.addAttribute("message", dataFormatCheckResult.getMessage());
    	return new ModelAndView("");
    }
    
    @RequestMapping("/batchAddSlaveDeploy")
    public ModelAndView doBatchAddSlaveDeploy(HttpServletRequest request, HttpServletResponse response, Model model, String slaveText,
            Long appId){
    	Result result = null;
        try {			
			result = appDeployCenter.batchAddSlaveDeploy(appId, slaveText);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = Result.fail(ErrorMessageEnum.INNER_ERROR_MSG.getMessage());
        }
        model.addAttribute("status", result.getStatus());
        model.addAttribute("message", result.getMessage());
   	return new ModelAndView("");
    }
    
    
    
    @RequestMapping("/batchAddSlave")
    public ModelAndView doBatchAddSlave(HttpServletRequest request, HttpServletResponse response, Model model,Long appId){
    	model.addAttribute("appId",appId);
    	return new ModelAndView("manage/appOps/appBatchAddSlave");
    }

	/**
	 * 添加应用部署
	 * 
	 * @param appDeployText 部署配置
	 * @param appAuditId 审批id
	 * @return
	 */
	@RequestMapping(value = "/addAppDeploy")
	public ModelAndView doAddAppDeploy(HttpServletRequest request,
			HttpServletResponse response, Model model, String appDeployText,
			Long appAuditId) {
	    AppUser appUser = getUserInfo(request);
        logger.warn("user {} appDeploy: appDeployText={},appAuditId:{}", appUser.getName(), appDeployText, appAuditId);
        boolean isSuccess = false;
	    if (appAuditId != null && StringUtils.isNotBlank(appDeployText)) {
			String[] appDetails = appDeployText.split("\n");
			// 部署service
			isSuccess = appDeployCenter.allocateResourceApp(appAuditId, Arrays.asList(appDetails), getUserInfo(request));
		} else {
			logger.error("appDeploy error param: appDeployText={},appAuditId:{}", appDeployText, appAuditId);
		}
        logger.warn("user {} appDeploy: appDeployText={},appAuditId:{}, result is {}", appUser.getName(), appDeployText, appAuditId, isSuccess);
        model.addAttribute("status", isSuccess ? 1 : 0);
        return new ModelAndView("");
	}

	/**
	 * 通过,获取驳回申请
	 * 
	 * @param status 审批状态
	 * @param appAuditId 审批id
	 * @param refuseReason 应用id
	 * @return
	 */
	@RequestMapping(value = "/addAuditStatus")
	public ModelAndView doAddAuditStatus(HttpServletRequest request, HttpServletResponse response, Model model, Integer status, Long appAuditId, String refuseReason) {
	    AppUser appUser = getUserInfo(request);
        logger.warn("user {} addAuditStatus: status={},appAuditId:{},refuseReason:{}", appUser.getName(), status, appAuditId, refuseReason);
	    AppAudit appAudit = appService.getAppAuditById(appAuditId);
		Long appId = appAudit.getAppId();
		// 通过或者驳回并记录日志
		appService.updateAppAuditStatus(appAuditId, appId, status, getUserInfo(request));

		// 记录驳回原因
		if (AppCheckEnum.APP_REJECT.value().equals(status)) {
			appAudit.setRefuseReason(refuseReason);
			appService.updateRefuseReason(appAudit, getUserInfo(request));
		}

		// 发邮件统计
		if (AppCheckEnum.APP_PASS.value().equals(status) || AppCheckEnum.APP_REJECT.value().equals(status)) {
			AppDesc appDesc = appService.getByAppId(appId);
			appEmailUtil.noticeAppResult(appDesc, appService.getAppAuditById(appAuditId));
		}

		// 批准成功直接跳转
		if (AppCheckEnum.APP_PASS.value().equals(status)) {
			return new ModelAndView("redirect:/manage/app/auditList");
		}

		write(response, String.valueOf(SuccessEnum.SUCCESS.value()));
		return null;
	}

	/**
	 * 下线应用
	 * 
	 * @param appId
	 * @return
	 */
	@RequestMapping(value = "/offLine")
	public ModelAndView offLineApp(HttpServletRequest request,
			HttpServletResponse response, Model model, Long appId) {
		AppUser userInfo = getUserInfo(request);
		logger.warn("user {} hope to offline appId: {}", userInfo.getName(), appId);
		if (ConstUtils.SUPER_MANAGER.contains(userInfo.getName())) {
			boolean result = appDeployCenter.offLineApp(appId);
			model.addAttribute("appId", appId);
			model.addAttribute("result", result);
			if (result) {
				model.addAttribute("msg", "操作成功");
			} else {
				model.addAttribute("msg", "操作失败");
			}
		    logger.warn("user {} offline appId: {}, result is {}", userInfo.getName(), appId, result);
		    appEmailUtil.noticeOfflineApp(userInfo, appId, result);
		} else {
		    logger.warn("user {} hope to offline appId: {}, hasn't provilege", userInfo.getName(), appId);
			model.addAttribute("result", false);
			model.addAttribute("msg", "权限不足");
	        appEmailUtil.noticeOfflineApp(userInfo, appId, false);
		}
		return new ModelAndView();
	}

	@RequestMapping("/syncToUnionCache")
	public ModelAndView syncToUnionCache(Model model,Long appId) {
		boolean success = redisDeployCenter.updateToUnioncache(appId);
		redisDeployCenter.syncUserPriToUnionCache(appId);
		model.addAttribute("msg", success?"同步成功":"同步失败");
		return new ModelAndView();
	}
	/**
	 * 实例机器信息
	 * @param appId
	 * @param model
	 */
	private void fillAppMachineStat(Long appId, Model model){
        List<InstanceInfo> instanceList = appService.getAppInstanceInfo(appId);
        
        Map<String, MachineStats> machineStatsMap = new HashMap<String, MachineStats>();
        Map<String, Long> machineCanUseMem = new HashMap<String, Long>();
        
        for (InstanceInfo instanceInfo : instanceList) {
            if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
                continue;
            }
            String ip = instanceInfo.getIp();
            if (machineStatsMap.containsKey(ip)) {
                continue;
            }
            MachineStats machineStats = machineCenter.getMachineMemoryDetail(ip);
            machineStatsMap.put(ip, machineStats);
            machineCanUseMem.put(ip, machineStats.getMachineMemInfo().getLockedMem());
        }
        model.addAttribute("machineCanUseMem", machineCanUseMem);
        model.addAttribute("machineStatsMap", machineStatsMap);
	}
	
	
	/**
	 * app最前，其余按照空闲内存倒排序
	 * 
	 * @param appHostSet
	 * @param machineList
	 * @return
	 */
	private List<MachineStats> sortByAppAndMachineTotalMem(
			Set<String> appHostSet, List<MachineStats> machineList) {
		if (CollectionUtils.isEmpty(machineList)) {
			return Collections.emptyList();
		}
		List<MachineStats> resultList = new ArrayList<MachineStats>();
		// 分为两组,当前app的一组其余的一组
		List<MachineStats> appList = new ArrayList<MachineStats>();
		List<MachineStats> otherList = new ArrayList<MachineStats>();
		for (MachineStats machineStat : machineList) {
			if (appHostSet.contains(machineStat.getIp())) {
				appList.add(machineStat);
			} else {
				otherList.add(machineStat);
			}
		}
		// 按照机器内存倒序
		Collections.sort(otherList, new Comparator<MachineStats>() {
			@Override
			public int compare(MachineStats m1, MachineStats m2) {
				long m1Total = NumberUtils.toLong(m1.getMemoryFree())
						- m1.getMachineMemInfo().getLockedMem();
				long m2Total = NumberUtils.toLong(m2.getMemoryFree())
						- m2.getMachineMemInfo().getLockedMem();
				return (int) ((m2Total - m1Total) / 1024 / 1024);
			}
		});
		// 添加
		resultList.addAll(appList);
		resultList.addAll(otherList);
		return resultList;
	}

	/**
	 * 应用运维
	 * @param appId
	 */
	@RequestMapping("/index")
	public ModelAndView index(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		model.addAttribute("appId", appId);
		return new ModelAndView("manage/appOps/appOpsIndex");
	}

	/**
	 * 应用机器运维
	 * @param appId
	 */
	@RequestMapping("/machine")
	public ModelAndView appMachine(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		if (appId != null && appId > 0) {
			List<MachineStats> appMachineList = appService.getAppMachineDetail(appId);
			model.addAttribute("appMachineList", appMachineList);
			AppDesc appDesc = appService.getByAppId(appId);
			model.addAttribute("appDesc", appDesc);
		}
		return new ModelAndView("manage/appOps/appMachine");
	}

	/**
	 * 应用实例运维
	 * @param appId
	 */
	@RequestMapping("/instance")
	public ModelAndView appInstance(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		logger.error("应用实例运维,appId = {}",appId);
		if (appId != null && appId > 0) {
			AppDesc appDesc = appService.getByAppId(appId);
			model.addAttribute("appDesc", appDesc);
			//实例信息和统计
			fillAppInstanceStatsAndSession(appId, model, request);
			
			//只有cluster类型才需要计算slot相关
            if (TypeUtil.isRedisCluster(appDesc.getType())) {
                // 计算丢失的slot区间
                Map<String,String> lossSlotsSegmentMap = redisCenter.getClusterLossSlots(appId);
                model.addAttribute("lossSlotsSegmentMap", lossSlotsSegmentMap);
            }
		}
		return new ModelAndView("manage/appOps/appInstance");
	}

	/**
	 * 应用详细信息和各种申请记录
	 * @param appId
	 */
	@RequestMapping("/detail")  
	public ModelAndView appInfoAndAudit(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		if (appId != null && appId > 0) {
			List<AppAudit> appAuditList = appService.getAppAuditListByAppId(appId);
			AppDesc appDesc = appService.getByAppId(appId);
			model.addAttribute("appAuditList", appAuditList);
			model.addAttribute("appDesc", appDesc);
		}
		return new ModelAndView("manage/appOps/appInfoAndAudit");
	}
	
	/**
	 * 应用详细信息和各种申请记录
	 * @param appId
	 */
	@RequestMapping("/backup")
	public ModelAndView appBackup(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		model.addAttribute("appId", appId);
		List<String> backupList = appService.getAppBackupList(appId);
		model.addAttribute("backupList",backupList); 
		return new ModelAndView("manage/appOps/appBackup");
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param appId
	 * @return
	 */
	@RequestMapping("/restoreBackup")
	public ModelAndView restoreBackup(HttpServletRequest request, HttpServletResponse response, Model model, Long appId,String date) {
		
		boolean check = appService.restoreBackup(appId, date);
		if (check){
			model.addAttribute("msg", "备份文件都存在，备份恢复已经开始，请稍后检查日志");
		}else{
			model.addAttribute("msg","备份检查出错，请查看日志");
		}
		return new ModelAndView();
	}
	
	@RequestMapping("/setPwd")
	public ModelAndView setPwd(HttpServletRequest request, HttpServletResponse response, Model model, Long appId,String pwd) {
		
		boolean check = appService.setPwd(appId,pwd);
		if (check){
			model.addAttribute("msg", "设置成功");
		}else{
			model.addAttribute("msg","设置失败，请查看日志");
		}
		return new ModelAndView();
	}
	
	/**
	 * 应用详细信息和各种申请记录
	 * @param appId
	 */
	@RequestMapping("/pwd")
	public ModelAndView setPwd(HttpServletRequest request, HttpServletResponse response, Model model, Long appId) {
		model.addAttribute("appId", appId);
		AppDesc appDesc = appService.getByAppId(appId);
		model.addAttribute("appDesc",appDesc);
		return new ModelAndView("manage/appOps/appPwd");
	}
	

	/**
	 * redisCluster从节点failover
	 * 
	 * @param appId 应用id
	 * @param slaveInstanceId 从节点instanceId
	 * @return
	 */
	@RequestMapping("/clusterSlaveFailOver")
	public void clusterSlaveFailOver(HttpServletRequest request, HttpServletResponse response, Model model, Long appId,
			int slaveInstanceId) {
		boolean success = false;
		logger.warn("clusterSlaveFailOver: appId:{}, slaveInstanceId:{}", appId, slaveInstanceId);
		if (appId != null && appId > 0 && slaveInstanceId > 0) {
			try {
				success = redisDeployCenter.clusterFailover(appId,slaveInstanceId);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.error("error param clusterSlaveFailOver: appId:{}, slaveInstanceId:{}", appId, slaveInstanceId);
		}
	    logger.warn("clusterSlaveFailOver: appId:{}, slaveInstanceId:{}, result is {}", appId, slaveInstanceId, success);
		write(response, String.valueOf(success == true ? SuccessEnum.SUCCESS.value() : SuccessEnum.FAIL.value()));
	}

	/**
	 * 添加slave节点
	 * 
	 * @param appId
	 * @param masterInstanceId
	 * @param slaveHost
	 * @return
	 */
    @RequestMapping(value = "/addSlave")
    public void addSlave(HttpServletRequest request, HttpServletResponse response, Model model, long appId,
            int masterInstanceId, String slaveHost) {
        AppUser appUser = getUserInfo(request);
        logger.warn("user {} addSlave: appId:{},masterInstanceId:{},slaveHost:{}", appUser.getName(), appId, masterInstanceId, slaveHost);
        boolean success = false;
		//int status = 0;
		if (appId > 0 && StringUtils.isNotBlank(slaveHost) && masterInstanceId > 0) {
			try {
				//status = redisDeployCenter.checkAddSlave(masterInstanceId, slaveHost);
				//if(status == 0) {
					success = redisDeployCenter.addSlave(appId, masterInstanceId, slaveHost);
				//}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.warn("user {} addSlave: appId:{},masterInstanceId:{},slaveHost:{} result is {}", appUser.getName(), appId, masterInstanceId, slaveHost, success);
		//write(response, String.valueOf(success == true ? SuccessEnum.SUCCESS.value() : status));
		write(response, String.valueOf(success == true ? SuccessEnum.SUCCESS.value() : SuccessEnum.FAIL.value()));
    }

    /**
     * 添加sentinel节点
     * @param appId
     * @param sentinelHost
     * @return
     */
	@RequestMapping(value = "/addSentinel")
	public void addSentinel(HttpServletRequest request, HttpServletResponse response, Model model, long appId, String sentinelHost) {
        AppUser appUser = getUserInfo(request);
		logger.warn("user {} addSentinel: appId:{}, sentinelHost:{}", appUser.getName(), appId, sentinelHost);
	    boolean success = false;
		if (appId > 0 && StringUtils.isNotBlank(sentinelHost)) {
			try {
				success = redisDeployCenter.addSentinel(appId, sentinelHost);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	    logger.warn("user {} addSentinel: appId:{}, sentinelHost:{} result is {}", appUser.getName(), appId, sentinelHost, success);
		write(response, String.valueOf(success == true ? SuccessEnum.SUCCESS.value() : SuccessEnum.FAIL.value()));
	}
	
	/**
	 * 为失联的slot添加master节点
	 * @param appId
	 * @param sentinelHost
	 */
	@RequestMapping(value = "/addFailSlotsMaster")
    public void addFailSlotsMaster(HttpServletRequest request, HttpServletResponse response, Model model, long appId, String failSlotsMasterHost, int instanceId) {
        AppUser appUser = getUserInfo(request);
        logger.warn("user {} addFailSlotsMaster: appId:{}, instanceId {}, newMasterHost:{}", appUser.getName(), appId, instanceId, failSlotsMasterHost);
        RedisOperateEnum redisOperateEnum = RedisOperateEnum.FAIL;
        if (appId > 0 && StringUtils.isNotBlank(failSlotsMasterHost)) {
            try {
                redisOperateEnum = redisDeployCenter.addSlotsFailMaster(appId, instanceId, failSlotsMasterHost);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.warn("user {} addFailSlotsMaster: appId:{}, instanceId {}, newMasterHost:{} result is {}", appUser.getName(), appId, instanceId, failSlotsMasterHost, redisOperateEnum.getValue());
        write(response, String.valueOf(redisOperateEnum.getValue()));
    }

	
	
	/**
	 * sentinelFailOver操作
	 * 
	 * @param appId
	 * @return
	 */
    @RequestMapping("/sentinelFailOver")
	public void sentinelFailOver(HttpServletRequest request, HttpServletResponse response, Model model, long appId) {
        AppUser appUser = getUserInfo(request);
		logger.warn("user {} sentinelFailOver, appId:{}", appUser.getName(), appId);
	    boolean success = false;
		if (appId > 0) {
			try {
				success = redisDeployCenter.sentinelFailover(appId);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.error("error param, sentinelFailOver: appId:{}", appId);
		}
	    logger.warn("user {} sentinelFailOver, appId:{}, result is {}", appUser.getName(), appId, success);
		write(response, String.valueOf(success == true ? SuccessEnum.SUCCESS.value() : SuccessEnum.FAIL.value()));
	}

}
