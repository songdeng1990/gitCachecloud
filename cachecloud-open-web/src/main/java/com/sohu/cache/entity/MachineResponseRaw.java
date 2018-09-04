package com.sohu.cache.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MachineResponseRaw {
	
	private int ret;
	private String msg;
	
	private MachineInfoFromCMDB data;
	
	public int getRet() {
		return ret;
	}
	public void setRet(int ret) {
		this.ret = ret;
	}

	public MachineInfoFromCMDB getData() {
		return data;
	}
	public void setData(MachineInfoFromCMDB data) {
		this.data = data;
	}
	
	public MachineResponse toMapchineResponse(){
		MachineResponse rsp = new MachineResponse();
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		for (int i=0;i < this.getData().getTotal(); i++){
			Map<String,String> rawMap = this.getData().getList().get(i);
			Map<String,String> map = new HashMap<String,String>();
			
			for (String key : rawMap.keySet()){
				if (key.equals("lan_ip")){
					map.put("ip",rawMap.get("lan_ip"));
				}else if(key.equals("idc_id")){
					map.put("idcId",rawMap.get("idc_id"));
				}else if(key.equals("memory_total")){
					map.put("memtotal",rawMap.get("memory_total"));
				}else if(key.equals("cpu_count")){
					map.put("cpu",rawMap.get("cpu_count"));
				}else if(key.equals("device_type")){
					map.put("virtual",rawMap.get("device_type").equals("虚拟机")?"1":"0");
				}else if(key.equals("physical_machine_ip")){
					map.put("realIp",rawMap.get("physical_machine_ip"));
				}
			}
			list.add(map);
		}
		rsp.setBody(list);
		rsp.setMessage(this.getMsg());
		rsp.setCode(1);
		return rsp;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
}
