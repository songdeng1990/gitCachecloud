package com.sohu.cache.entity;

import java.io.Serializable;

/**
 * Created by renxin on 2017/8/1.
 */
public class DbOperate implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5759508007277539586L;

	private String mapperName;

    private String methodName;

    private Object parameter;

    public DbOperate(String mapperName, String methodName, Object parameter){
        this.mapperName = mapperName;
        this.methodName = methodName;
        this.parameter = parameter;
    }

    public String getMapperName() {
        return mapperName;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object getParameter() {
        return parameter;
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }
}
