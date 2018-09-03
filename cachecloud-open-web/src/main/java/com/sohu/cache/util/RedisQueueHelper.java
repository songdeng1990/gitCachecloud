package com.sohu.cache.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisQueueHelper {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private JedisPool pool;
	private JedisPool hashPool;
	private static final String lock = "lock";
	private static final String queueName = "dbInsert";
	
	
	public RedisQueueHelper(String ip,int port){
		try{
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(10);
			config.setMaxIdle(10);
			config.setMaxWaitMillis(5000);
			
			pool = new JedisPool(config,ip,port);
			hashPool = new JedisPool(config,ip,port);
		}catch(Throwable e){
			logger.error("",e);
		}
	}
	public void setLastInfoMap(String ip,int port,Map<String, Object> infoMap){
		Jedis jedis = hashPool.getResource();
		try{
			byte[] bytes = SearializeUtil.toByteArray(infoMap);
			jedis.hset("lastInfo".getBytes("utf8"), (ip+ ":" +port).getBytes("utf8"),bytes);
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
	}
	
	public Map<String, Object> getLastInfoMap(String ip,int port){
		Jedis jedis = hashPool.getResource();
		Map<String, Object> infoMap = null;
		try{
			byte[] bytes = jedis.hget("lastInfo".getBytes("utf8"), (ip+ ":" +port).getBytes("utf8"));
			if (bytes == null){
				return null;
			}
			
			Object ob =  SearializeUtil.toObject(bytes);
			infoMap = (Map<String, Object>)ob;
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
		
		return infoMap;
	}
	
	public void clearLastInfoMap(){
		Jedis jedis = hashPool.getResource();
		try{
			jedis.del("lastInfo");
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
	}
	
	
	public void push(Object object){
		Jedis jedis = pool.getResource();
		try{
			byte[] bytes = SearializeUtil.toByteArray(object);
			jedis.lpush(queueName.getBytes("UTF-8"), bytes);
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
	}
	
	public Object pop(){
		Jedis jedis = pool.getResource();
		Object rsp = null;
		try{
			byte[] bytes = jedis.rpop(queueName.getBytes("UTF-8"));
			if (bytes == null){
				return null;
			}
			rsp = SearializeUtil.toObject(bytes);
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
		return rsp;
	}
	
	public Object brpop(){
		Jedis jedis = pool.getResource();
		Object rsp = null;
		try{
			byte[] bytes = jedis.brpop(0,queueName.getBytes("UTF-8")).get(1);
			if (bytes == null){
				return null;
			}
			rsp = SearializeUtil.toObject(bytes);
		} catch (Exception e) {
			logger.error("",e);
		}finally{
			jedis.close();
		}
		return rsp;
	}
	
	public long length(){
		Jedis jedis = pool.getResource();
		Long rsp = 0L;
		try{
			rsp = jedis.llen(queueName);
		}finally{
			jedis.close();
		}
		return rsp;
	}
	
	public boolean isMaster(){
		Jedis jedis = pool.getResource();
		String rsp = "";
		try{
			rsp = jedis.get("masterIp");
		}finally{
			jedis.close();
		}
		return IPUtil.getInnerIp().equals(rsp);
	}
	
}
