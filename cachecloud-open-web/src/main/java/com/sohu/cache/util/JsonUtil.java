package com.sohu.cache.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sohu.cache.entity.DbOperate;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.InstanceStats;

import sun.misc.BASE64Encoder;

/**
 * jackson转换工具
 * @author leifu
 * @Date 2016年3月23日
 * @Time 上午10:47:57
 */
public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    // 采用jackson
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * 将对象转换为json
     * 
     * @param entity
     * @return
     */
    public static String toJson(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(entity);
        } catch (IOException e) {
            logger.error("parse entity=" + entity + " to json error!", e);
        }
        return null;
    }

    /**
     * 从json解析出对象
     * 
     * @param <T>
     * @param content
     * @param valueType
     * @return
     */
    public static <T> T fromJson(String content, Class<T> valueType) {
        if (content == null) {
            return null;
        }
        try {
            return mapper.readValue(content, valueType);
        } catch (IOException e) {
            logger.error("parse content=" + content + " error!", e);
        }
        return null;
    }
    
    public static <T> T fromJson(String content, TypeReference<T> valueType) {
        if (content == null) {
            return null;
        }
        try {
            return mapper.readValue(content, valueType);
        } catch (IOException e) {
            logger.error("parse content=" + content + " error!", e);
        }
        return null;
    }

    private static ObjectMapper getObjectMapper() {
        return mapper;
    }

    public static ObjectNode createObjectNode() {
        return getObjectMapper().createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return getObjectMapper().createArrayNode();
    }
    
   /* public static void main(String[] args) {
    	InstanceStats infor = new InstanceStats();
    	infor.setIp("192.1121");
    	DbOperate op = new DbOperate("1", "2", infor);
    	byte[] bytes = SearializeUtil.toByteArray(op);
    	DbOperate op2 = (DbOperate)SearializeUtil.toObject(bytes);
    	RedisQueueHelper.getInstance().push(op);
    	DbOperate op2 = (DbOperate) RedisQueueHelper.getInstance().pop();
    	InstanceStats infor2 = (InstanceStats) op2.getParameter();
    	
    	System.out.println(((InstanceStats)op2.getParameter()).getIp());
	}*/
}
