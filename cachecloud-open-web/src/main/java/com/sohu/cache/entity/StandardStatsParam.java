package com.sohu.cache.entity;

import java.io.Serializable;
import java.util.Map;

public class StandardStatsParam implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -942880283955810183L;
	private Map<String, Object> inforMap;
	private String host;
	private int port;
	private String type;
	
	public StandardStatsParam(Map<String, Object> inforMap,String host,int port ,String type){
		this.inforMap = inforMap;
		this.host = host;
		this.port = port;
		this.type = type;
	}
	
	public Map<String, Object> getInforMap() {
		return inforMap;
	}

	public void setInforMap(Map<String, Object> inforMap) {
		this.inforMap = inforMap;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
