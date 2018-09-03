package com.sohu.cache.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by caijt on 2016/9/21.
 */
public class GroupCommandStats implements Comparable<GroupCommandStats>,Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -7225961717392527914L;

	/**
     * 业务组id
     */
    private long businessGroupId;

    /**
     * 收集时间:格式yyyyMMddHHmm/yyyyMMdd/yyyyMMddHH
     */
    private long collectTime;

    /**
     * 命令名称
     */
    private String commandName;

    /**
     * 命令执行次数
     */
    private long commandCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date modifyTime;

    public long getBusinessGroupId() {
        return businessGroupId;
    }

    public void setBusinessGroupId(long businessGroupId) {
        this.businessGroupId = businessGroupId;
    }

    public long getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(long collectTime) {
        this.collectTime = collectTime;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public long getCommandCount() {
        return commandCount;
    }

    public void setCommandCount(long commandCount) {
        this.commandCount = commandCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public int compareTo(GroupCommandStats o) {
        if (o.commandCount > this.commandCount) {
            return 1;
        } else if (o.commandCount < this.commandCount) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "AppCommandStats{" +
                "businessGroupId=" + businessGroupId +
                ", collectTime=" + collectTime +
                ", commandName=" + commandName +
                ", commandCount=" + commandCount +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }
}
