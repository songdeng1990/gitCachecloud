package com.sohu.cache.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jiguang on 2016/9/9.
 * {
    "msg": "",
    "data": {
        "total": 1,
        "list": [
            {
                "report_time": "2017-05-03 19:21:05",
                "hostname": "nfjd-cb-cbtoken08-103103.jpushoa.com",
                "python_version": "2.7.5",
                "relation_remark": "",
                "cpu_count": 16,
                "seat": 2,
                "use_status": "生产中",
                "create_time": "2017-03-27 14:21:53",
                "report_status": "不正常",
                "device_type": "虚拟机",
                "id": 6248,
                "cabinet_area": "205",
                "agency": "",
                "memory_total": 128942,
                "idc_id": 25,
                "os_version": "CentOS 7.2.1511",
                "vm_total": 0,
                "serial_number": "",
                "project_id": 0,
                "idrac_ip": "",
                "department_id": 0,
                "memory_total_except_vm": 128942,
                "lan_ip": "172.16.103.103",
                "update_time": "2017-05-03 19:21:05",
                "project_name": null,
                "physical_machine_id": 189,
                "memory_free": 49030,
                "brand": "Red Hat KVM",
                "idc_name": "广州南基",
                "cpu_model": "Intel Xeon E312xx (Sandy Bridge)",
                "subproject_id": 0,
                "static_ip": "",
                "physical_machine_ip": "172.16.100.50",
                "flag": 1,
                "cabinet_id": 43,
                "subproject_name": null,
                "device_id": "0021f6326701_172.16.103.103",
                "remark": "",
                "disk_size": "vda 500.00 GB",
                "service_code": "",
                "kernel_version": "3.10.0-327.el7.x86_64",
                "cabinet_num": "B0107",
                "wlan_ip": "",
                "physical_machine": "",
                "agent_version": "1020"
            }
        ]
    },
    "ret": 0
}
 */
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
