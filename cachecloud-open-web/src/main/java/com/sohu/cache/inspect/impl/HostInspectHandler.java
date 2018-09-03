package com.sohu.cache.inspect.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.machine.MachineCenter;
import com.sohu.cache.util.ConstUtils;

/**
 * Created by yijunzhang on 15-1-20.
 */
public class HostInspectHandler extends AbstractInspectHandler{
    private final String inspectPoolKey="inspector-host-pool";
    private MachineCenter machineCenter;

    @Override
    public String getThreadPoolKey() {
        return inspectPoolKey;
    }

    @Override
    protected Map<String, List<InstanceInfo>> getSplitMap() {
        List<InstanceInfo> list = getAllInstanceList();
        Map<String, List<InstanceInfo>> hostMap = new TreeMap<String, List<InstanceInfo>>();
        for (InstanceInfo instanceInfo : list) {
            String host = instanceInfo.getIp();
            if (hostMap.containsKey(host)) {
                hostMap.get(host).add(instanceInfo);
            } else {
                List<InstanceInfo> hostInstances = new ArrayList<InstanceInfo>();
                hostInstances.add(instanceInfo);
                hostMap.put(host, hostInstances);
            }
        }
        if (isTimeNow(ConstUtils.AOF_TIME)){
        	Map<String,Integer> aofRewriteCounterMap = new ConcurrentHashMap<String,Integer>();
        	for(String host : hostMap.keySet()){
        		aofRewriteCounterMap.put(host, 1);
        	}
        	RedisIsolationPersistenceInspector.aofRewriteCounterMap = aofRewriteCounterMap;
        	for (String ip : hostMap.keySet()){
        		machineCenter.syncInstanceInfoFile(ip);
        	}
        }
        return hostMap;
    }
    
    private boolean isTimeNow(String time) {

		try {
			String start = time.split("-")[0];
			String end = time.split("-")[1];
			SimpleDateFormat sp = new SimpleDateFormat("HH:mm");
			Date startTime = sp.parse(start);
			Date endTime = sp.parse(end);
			Date now = sp.parse(sp.format(new Date()));
			return now.getTime() >= startTime.getTime() && now.getTime() <= endTime.getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
    
    public MachineCenter getMachineCenter() {
		return machineCenter;
	}

	public void setMachineCenter(MachineCenter machineCenter) {
		this.machineCenter = machineCenter;
	}
}
