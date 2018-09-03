package com.sohu.cache.web.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sohu.cache.constant.MachineInfoEnum;
import com.sohu.cache.entity.*;
import com.sohu.cache.machine.MachineDeployCenter;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.util.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 机器管理
 * 
 * @author leifu
 * @Time 2014年10月14日
 */
@Controller
@RequestMapping("manage/machine")
public class MachineManageController extends BaseController {

	@Resource
	private MachineDeployCenter machineDeployCenter;

	@RequestMapping(value = "/list")
	public ModelAndView doMachineList(HttpServletRequest request, HttpServletResponse response, Model model,
			String ipLike,String extraDesc,String groupName) {
		int pageNo = NumberUtils.toInt(request.getParameter("pageNo"), 1);
		int pageSize = NumberUtils.toInt(request.getParameter("pageSize"), 50);

		logger.info("machine list接收到参数,pageNo = {}, pageSize = {}",request.getParameter("pageNo"),request.getParameter("pageSize"));
		int start = (pageNo - 1) * pageSize;
		List<MachineStats> machineList = machineCenter.getMachineStats(ipLike,extraDesc,groupName,start,pageSize);
		int total = machineCenter.countMachine(ipLike,extraDesc,groupName);
		Set<String> groupSet = machineCenter.getGroups();
		model.addAttribute("list", machineList);
		model.addAttribute("ipLike", ipLike);
		model.addAttribute("extraDesc", extraDesc);
		model.addAttribute("groupName",groupName);
		model.addAttribute("machineActive", SuccessEnum.SUCCESS.value());
		model.addAttribute("collectAlert", "(请等待" + ConstUtils.MACHINE_STATS_CRON_MINUTE + "分钟)");
		model.addAttribute("roomList", machineCenter.getAllRoom());
		model.addAttribute("groupSet", groupSet);
		model.addAttribute("machineGroup",machineCenter.getMachineGroups());

		Page page = new Page(pageNo,pageSize,total);
		model.addAttribute("page",page);
		return new ModelAndView("manage/machine/list");
	}

	@RequestMapping(value = "/installRedis")
	public ModelAndView doInstallRedis(HttpServletRequest request, HttpServletResponse response, Model model,
			String ip) {

		MachineResponse rsp = new MachineResponse();
		List<InstanceInfo> instanceList = machineCenter.getMachineInstanceInfo(ip);

		if (instanceList.size() >= 1) {
			rsp.setCode(3);
			rsp.setMessage("You can't execute install-redis script,because there are some redis instance on it.");
		} else {
			if (machineDeployCenter.installRedis(ip)) {
				rsp.setCode(1);
				rsp.setMessage("install redis succeeded.");
			} else {
				rsp.setCode(2);
				rsp.setMessage("install redis failed.");
			}
		}
		model.addAttribute("rsp", rsp);
		return new ModelAndView("");
	}

	@RequestMapping(value = "/updateMonitor")
	public ModelAndView doUpdateMonitor(HttpServletRequest request, HttpServletResponse response, Model model) {
		boolean result = machineCenter.updateAllMonitorScript();
		MachineResponse rsp = new MachineResponse();
		if (result) {
			rsp.setCode(1);
			rsp.setMessage("所有机器监控脚本和信息成功");
		} else {
			rsp.setCode(2);
			rsp.setMessage("跟新监控新脚本和信息遭遇异常，请查看服务器日志");
		}
		model.addAttribute("rsp", rsp);
		return new ModelAndView("");
	}
	
	@RequestMapping(value = "/updateMachineInfo")
	public ModelAndView doUpdateMachineInfo (HttpServletRequest request, HttpServletResponse response, Model model) {
		boolean result = machineCenter.updateMachineInfo();
		MachineResponse rsp = new MachineResponse();
		if (result) {
			rsp.setCode(1);
			rsp.setMessage("更新所有机器信息成功");
		} else {
			rsp.setCode(2);
			rsp.setMessage("更新部分机器信息遭遇异常，请查看server日志");
		}
		model.addAttribute("rsp", rsp);
		return new ModelAndView("");
	}

	/**
	 * 机器实例展示
	 * 
	 * @param ip
	 * @return
	 */
	@RequestMapping(value = "/machineInstances")
	public ModelAndView doMachineInstances(HttpServletRequest request, HttpServletResponse response, Model model,
			String ip) {
		// 机器以及机器下面的实例信息
		MachineInfo machineInfo = machineCenter.getMachineInfoByIp(ip);
		List<InstanceInfo> instanceList = machineCenter.getMachineInstanceInfo(ip);
		List<InstanceStats> instanceStatList = machineCenter.getMachineInstanceStatsByIp(ip);
		// 统计信息
		fillInstanceModel(instanceList, instanceStatList, model);

		model.addAttribute("machineInfo", machineInfo);
		model.addAttribute("machineActive", SuccessEnum.SUCCESS.value());
		return new ModelAndView("manage/machine/machineInstances");
	}

	/**
	 * 检查机器下是否有存活的实例
	 * 
	 * @param ip
	 * @return
	 */
	@RequestMapping(value = "/checkMachineInstances")
	public ModelAndView doCheckMachineInstances(HttpServletRequest request, HttpServletResponse response, Model model,
			String ip) {
		List<InstanceInfo> instanceList = machineCenter.getMachineInstanceInfo(ip);
		model.addAttribute("machineHasInstance", CollectionUtils.isNotEmpty(instanceList));
		return new ModelAndView("");
	}

	@RequestMapping(value = "/get", method = { RequestMethod.POST })
	public ModelAndView doGet(HttpServletRequest request, HttpServletResponse response, Model model) {
		String resp = machineDeployCenter.getMachineInfo(request.getParameter("room"),
				Lists.newArrayList(request.getParameter("ip")));
		MachineResponseRaw machineResponseRaw = JSON.parseObject(resp, MachineResponseRaw.class);
		model.addAttribute("info", machineResponseRaw.toMapchineResponse());
		return new ModelAndView("");
	}

	@RequestMapping(value = "/addAll", method = { RequestMethod.POST })
	public ModelAndView doAddAll(HttpServletRequest request, HttpServletResponse response, Model model) {
		String machineInfoText = request.getParameter("machineInfoText");
		String group = request.getParameter("group");
		logger.info("机器批量添加获取到组名,group = {}",group);
		long groupId = machineCenter.getGroupIdByName(group);
		// 分离ip和额外描述
		String[] machineInfoList = machineInfoText.split(ConstUtils.NEXT_LINE);
		List<String> ipList = new ArrayList<>();
		Map<String, String> info = new HashMap<>();
		boolean check = true;
		for (String machineInfo : machineInfoList) {
			int index = machineInfo.indexOf(':');
			// 格式有误
			if (index == -1) {
				check = false;
				break;
			}
			String ip = machineInfo.substring(0, index);
			String extraDesc = machineInfo.substring(index + 1);
			info.put(ip, extraDesc);
			ipList.add(ip);
		}
		model.addAttribute("check", check);
		// 格式无误
		if (check) {
			boolean success = true;
			String resp = machineDeployCenter.getMachineInfo("", ipList);
			MachineResponseRaw machineResponseRaw = JSON.parseObject(resp, MachineResponseRaw.class);
			model.addAttribute("info", machineResponseRaw.toMapchineResponse());
			if (machineResponseRaw.getRet() == 0) { // success
				List<Map<String, String>> body = machineResponseRaw.getData().getList();
				for (Map<String, String> machine : body) {
					MachineInfo machineInfo = new MachineInfo();
					machineInfo.setIp(machine.get("lan_ip"));
					if (StringUtils.isEmpty(machine.get("idc_id"))) {
						machineInfo.setRoom(ConstUtils.DEFAULT_IDC_ID);
					} else {
						machineInfo.setRoom(machine.get("idc_id"));
					}
					machineInfo.setMem(NumberUtils.toInt(machine.get("memory_total"), 0));
					machineInfo.setCpu(NumberUtils.toInt(machine.get("cpu_count"), 0));
					machineInfo.setVirtual(machine.get("device_type").equals("虚拟机")?1:0);
					machineInfo.setRealIp(machine.get("physical_machine_ip"));
					machineInfo.setType(0);
					machineInfo.setExtraDesc(info.get(machine.get("lan_ip")));
					machineInfo.setCollect(NumberUtils.toInt(request.getParameter("collect"), 1));

					Date date = new Date();
					machineInfo.setSshUser(ConstUtils.USERNAME);
					machineInfo.setSshPasswd(ConstUtils.PASSWORD);
					machineInfo.setServiceTime(date);
					machineInfo.setModifyTime(date);
					machineInfo.setAvailable(MachineInfoEnum.AvailableEnum.YES.getValue());
					machineInfo.setMachineGroupId(groupId);
					machineDeployCenter.addMachine(machineInfo);
				}
			
			}else{
				success = false;
				logger.error("Request for machine info of {} failed",ipList.toString());
			}
			model.addAttribute("success", success);
		}
		return new ModelAndView("");
	}

	@RequestMapping(value = "/add", method = { RequestMethod.POST })
	public ModelAndView doAdd(HttpServletRequest request, HttpServletResponse response, Model model) {
		MachineInfo machineInfo = new MachineInfo();
		String group = request.getParameter("group");
		logger.info("机器批量添加获取到组名,group = {}",group);
		long groupId = machineCenter.getGroupIdByName(group);
		machineInfo.setIp(request.getParameter("ip"));
		machineInfo.setRoom(request.getParameter("room"));
		machineInfo.setMem(NumberUtils.toInt(request.getParameter("mem"), 0));
		machineInfo.setCpu(NumberUtils.toInt(request.getParameter("cpu"), 0));
		machineInfo.setVirtual(NumberUtils.toInt(request.getParameter("virtual"), 0));
		machineInfo.setRealIp(request.getParameter("realIp"));
		machineInfo.setType(NumberUtils.toInt(request.getParameter("machineType"), 0));
		machineInfo.setExtraDesc(request.getParameter("extraDesc"));
		machineInfo.setCollect(NumberUtils.toInt(request.getParameter("collect"), 1));

		Date date = new Date();
		machineInfo.setSshUser(request.getParameter("sshUser"));
		machineInfo.setSshPasswd(request.getParameter("sshPasswd"));
		machineInfo.setServiceTime(date);
		machineInfo.setModifyTime(date);
		machineInfo.setAvailable(MachineInfoEnum.AvailableEnum.YES.getValue());
		machineInfo.setMachineGroupId(groupId);
		boolean isSuccess = machineDeployCenter.addMachine(machineInfo);
		model.addAttribute("result", isSuccess);
		return new ModelAndView("");
	}

	@RequestMapping(value = "/delete")
	public ModelAndView doDelete(HttpServletRequest request, HttpServletResponse response, Model model) {
		String machineIp = request.getParameter("machineIp");
		if (StringUtils.isNotBlank(machineIp)) {
			MachineInfo machineInfo = machineCenter.getMachineInfoByIp(machineIp);
			boolean success = machineDeployCenter.removeMachine(machineInfo);
			logger.warn("delete machine {}, result is {}", machineIp, success);
		} else {
			logger.warn("machineIp is empty!");
		}
		return new ModelAndView("redirect:/manage/machine/list");
	}

	/**
	 * 实例统计信息
	 * 
	 * @param
	 * @param model
	 */
	protected void fillInstanceModel(List<InstanceInfo> instanceList, List<InstanceStats> appInstanceStats,
			Model model) {
		Map<String, MachineStats> machineStatsMap = new HashMap<String, MachineStats>();
		Map<String, Long> machineCanUseMem = new HashMap<String, Long>();
		Map<String, InstanceStats> instanceStatsMap = new HashMap<String, InstanceStats>();
		Map<Long, AppDesc> appInfoMap = new HashMap<Long, AppDesc>();

		for (InstanceStats instanceStats : appInstanceStats) {
			instanceStatsMap.put(instanceStats.getIp() + ":" + instanceStats.getPort(), instanceStats);
			appInfoMap.put(instanceStats.getAppId(), appService.getByAppId(instanceStats.getAppId()));
		}

		for (InstanceInfo instanceInfo : instanceList) {
			if (TypeUtil.isRedisSentinel(instanceInfo.getType())) {
				continue;
			}
			String ip = instanceInfo.getIp();
			if (machineStatsMap.containsKey(ip)) {
				continue;
			}
			List<MachineStats> machineStatsList = machineCenter.getMachineStats(ip,"",null,0,100);
			MachineStats machineStats = null;
			for (MachineStats stats : machineStatsList) {
				if (stats.getIp().equals(ip)) {
					machineStats = stats;
					machineStatsMap.put(ip, machineStats);
					break;
				}
			}
			MachineStats ms = machineCenter.getMachineMemoryDetail(ip);
			machineCanUseMem.put(ip, ms.getMachineMemInfo().getLockedMem());
		}


		model.addAttribute("machineCanUseMem", machineCanUseMem);
		model.addAttribute("machineStatsMap", machineStatsMap);

		model.addAttribute("instanceList", instanceList);
		model.addAttribute("instanceStatsMap", instanceStatsMap);
	}


	@RequestMapping("/machineGroupStat")
	public ModelAndView getMachineGroupStatInfo(HttpServletRequest request, HttpServletResponse response, Model model){
		logger.info("machineGroupStat接收到请求");
		MachineGroupStats totalStat = new MachineGroupStats();
		int machineNum = 0;
		long totalMem = 0;
		double totalUsedMem = 0;
		int instanceNum = 0;
		int appNum = 0;
		long totalOps = 0L;
		List<MachineGroupStats> machineGroupStat = machineCenter.getMachineStatsByGroup();
		for (MachineGroupStats mgs: machineGroupStat){
			if (StringUtils.isEmpty(mgs.getGroupName())){
				mgs.setGroupName("未分组");
			}
			MachineInstanceStats machineInstanceStats = machineCenter.getMachineInstanceStatsByGroup(mgs.getGroupId());
			Long ops = machineCenter.getMachineGroupOps(mgs.getGroupId());
			mgs.setOps(ops);
			mgs.setAppNum(machineInstanceStats.getAppNum());
			mgs.setInstanceNum(machineInstanceStats.getInstanceNum());
			machineNum += mgs.getMachineNum();
			totalMem += mgs.getTotalMem();
			totalUsedMem += (mgs.getTotalMem() * mgs.getMemUseRatio());
			instanceNum += mgs.getInstanceNum();
			appNum += mgs.getAppNum();
			totalOps += ops;
		}
		totalStat.setInstanceNum(instanceNum);
		totalStat.setAppNum(appNum);
		totalStat.setMachineNum(machineNum);
		totalStat.setMemUseRatio(totalUsedMem/totalMem);
		totalStat.setTotalMem(totalMem);
		totalStat.setOps(totalOps);

		model.addAttribute("totalStat",totalStat);
		model.addAttribute("machineGroupStat",machineGroupStat);
		return new ModelAndView("manage/machine/groupList");
	}

}