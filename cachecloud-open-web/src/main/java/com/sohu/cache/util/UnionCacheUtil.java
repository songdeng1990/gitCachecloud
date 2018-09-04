/**
 * 
 */
package com.sohu.cache.util;

import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.sohu.cache.entity.LoginResponse;
import com.sohu.cache.entity.UnionCacheRsp;
import com.sohu.cache.entity.User;
import com.sohu.cache.web.util.HttpRequestUtil;

/**
 * @author dengsong
 *
 */
public class UnionCacheUtil {
	private static final Logger logger = LoggerFactory.getLogger(UnionCacheUtil.class);
	public static boolean addNodeInfo(String urlPre,Map<String,String> parameters){
		try{
			String rst = HttpRequestUtil.doPost(urlPre + ConstUtils.UNION_CACHE_ADD_URL, parameters);
			UnionCacheRsp rsp = JsonUtil.fromJson(rst, UnionCacheRsp.class);
			if (rsp.getCode() != 0){
				return false;
			}
		}catch(Throwable e){
			logger.error("",e);
			return false;
		}
		logger.warn(String.format("Add node info  of appId %s %s to UnionCache successfully.",parameters.get("groupId"),parameters.get("group")));
		return true;
		
	}
	
	public static boolean syncUserInfo(String urlPre,Map<String,String> parameters){
		try{
			String rst = HttpRequestUtil.doPost(urlPre + ConstUtils.UNION_CACHE_USER_URL, parameters);
			UnionCacheRsp rsp = JsonUtil.fromJson(rst, UnionCacheRsp.class);
			if (rsp.getCode() != 0){
				return false;
			}
		}catch(Throwable e){
			logger.error("",e);
			return false;
		}
		logger.warn(String.format("Sync user info  of appId %s %s to UnionCache successfully.",parameters.get("groupId"),parameters.get("group")));
		return true;
	}
	
	public static boolean updateNodeInfo(String urlPre,Map<String,String> parameters){
		try{
			String rst = HttpRequestUtil.doPost(urlPre + ConstUtils.UNION_CACHE_UPDATE_URL, parameters);
			UnionCacheRsp rsp = JsonUtil.fromJson(rst, UnionCacheRsp.class);
			if (rsp.getCode() != 0){
				return false;
			}
		}catch(Throwable e){
			logger.error("",e);
			return false;
		}
		logger.warn(String.format("Update node info  of appId %s %s to UnionCache successfully.",parameters.get("groupId"),parameters.get("group")));
		return true;
	}
	
	public static boolean getNodeInfo(String urlPre,Map<String,String> parameters){
		try{
			String rst = HttpRequestUtil.doPost(urlPre + ConstUtils.UNION_CACHE_CHECK_URL, parameters);
			System.out.println(rst);
		}catch(Throwable e){
			logger.error("",e);
			return false;
		}
		logger.warn(String.format("check node info  of appId %s %s to UnionCache successfully.",parameters.get("groupId"),parameters.get("group")));
		return true;
	}
	
	public static boolean deleteNodeInfo(String urlPre,Map<String,String> parameters){
		try{
			String rst = HttpRequestUtil.doPost(urlPre + ConstUtils.UNION_CACHE_DELETE_URL, parameters);
			UnionCacheRsp rsp = JsonUtil.fromJson(rst, UnionCacheRsp.class);
			if (rsp.getCode() != 0){
				return false;
			}
		}catch(Throwable e){
			logger.error("",e);
			return false;
		}
		logger.warn(String.format("delete node info  of appId %s %s to UnionCache successfully.",parameters.get("groupId"),parameters.get("group")));
		return true;
	}
	
	
	
	public static void main(String[] args) {
		boolean success = true;
		try{
    		String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(ConstUtils.CACHECLOUD_MAIL, ConstUtils.CACHECLOUD_IPORTAL_PWD)));
            LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);
            Map<String,String> parameter = new Hashtable<String,String>();
            parameter.put("userId",loginResponse.getUser_id());
            parameter.put("token",loginResponse.getToken());
            parameter.put("groupId","10017");
            parameter.put("groupName","justForTest002");
            parameter.put("resourceType",String.valueOf(1));
            parameter.put("clusterType",String.valueOf(3));
            //parameter.put("bucket","defalut");
            parameter.put("userName","default");
            parameter.put("password","default");
            
            parameter.put("nodesList","192.168.13.35:16401,192.168.13.36:16381");
            String test="http://172.16.6.38:8080";
            //success = UnionCacheUtil.getNodeInfo(parameter);
            success = UnionCacheUtil.updateNodeInfo(test,parameter);
            
            
		}catch(Exception e){
			logger.error("",e);
			success = false;
		}
	}
}

	
	
