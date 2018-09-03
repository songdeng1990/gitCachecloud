package com.sohu.cache.entity;

/**
 * Created by jiguang on 2016/9/9.
 */
public class LoginResponse {
    private int ret;
    private String msg;
    private String user_id;
    private String token;
    
	public int getRet() {
		return ret;
	}
	public void setRet(int ret) {
		this.ret = ret;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}    
}
