/**
 * 
 */
package com.sohu.cache.entity;

import java.util.List;
import java.util.Map;

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
