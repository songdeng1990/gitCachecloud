package com.sohu.cache.entity;

/**
 * Created by jiguang on 2016/9/12.
 */
public class BusinessGroup {
    private long businessGroupId;
    private String businessGroupName;

    public String getBusinessGroupName() {
        return businessGroupName;
    }

    public void setBusinessGroupName(String businessGroupName) {
        this.businessGroupName = businessGroupName;
    }

    public long getBusinessGroupId() {
        return businessGroupId;
    }

    public void setBusinessGroupId(long businessGroupId) {
        this.businessGroupId = businessGroupId;
    }
}
