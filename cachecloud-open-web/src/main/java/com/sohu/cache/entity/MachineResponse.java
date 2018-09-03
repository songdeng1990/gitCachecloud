package com.sohu.cache.entity;

import java.util.List;
import java.util.Map;

public class MachineResponse {
	private int code;
	private String message;
	private List<Map<String,String>> body;
	
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public List<Map<String, String>> getBody() {
		return body;
	}
	public void setBody(List<Map<String, String>> body) {
		this.body = body;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
