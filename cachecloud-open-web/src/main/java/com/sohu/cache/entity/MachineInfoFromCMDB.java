/**
 * 
 */
package com.sohu.cache.entity;

import java.util.List;
import java.util.Map;

/**
 * @author dengsong
 *{
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
public class MachineInfoFromCMDB{
	private int total;
	private List<Map<String, String>> list;

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<Map<String, String>> getList() {
		return list;
	}

	public void setList(List<Map<String, String>> list) {
		this.list = list;
	}
}
