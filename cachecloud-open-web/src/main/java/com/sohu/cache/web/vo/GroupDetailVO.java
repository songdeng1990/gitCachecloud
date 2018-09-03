package com.sohu.cache.web.vo;

import com.sohu.cache.entity.AppDesc;

import java.util.List;

/**
 * Created by caijt on 2016/9/20.
 * 分组详情
 */
public class GroupDetailVO {

    private List<AppDesc> appDescList;

    /**
     * 内存空间
     */
    private long mem;

    /**
     * 当前内存
     */
    private long currentMem;

    /**
     * 机器数
     */
    private int machineNum;

    /**
     * 主节点数
     */
    private int masterNum;

    /**
     * 从节点数
     */
    private int slaveNum;

    /**
     * 当前对象数
     */
    private long currentObjNum;

    /**
     * 当前连接数
     */
    private int conn;

    /**
     * 内存使用报警
     */
    private double memUseThreshold;

    /**
     * 命中率使用报警
     */
    private double hitPercentThreshold;

    /**
     * 内存使用率
     */
    private double memUsePercent;

    /**
     * 命中率
     */
    private double hitPercent;

    public List<AppDesc> getAppDescList() {
        return appDescList;
    }

    public void setAppDescList(List<AppDesc> appDescList) {
        this.appDescList = appDescList;
    }

    public long getMem() {
        return mem;
    }

    public void setMem(long mem) {
        this.mem = mem;
    }

    public long getCurrentMem() {
        return currentMem;
    }

    public void setCurrentMem(long currentMem) {
        this.currentMem = currentMem;
    }

    public int getMachineNum() {
        return machineNum;
    }

    public void setMachineNum(int machineNum) {
        this.machineNum = machineNum;
    }

    public int getMasterNum() {
        return masterNum;
    }

    public void setMasterNum(int masterNum) {
        this.masterNum = masterNum;
    }

    public int getSlaveNum() {
        return slaveNum;
    }

    public void setSlaveNum(int slaveNum) {
        this.slaveNum = slaveNum;
    }

    public long getCurrentObjNum() {
        return currentObjNum;
    }

    public void setCurrentObjNum(long currentObjNum) {
        this.currentObjNum = currentObjNum;
    }

    public int getConn() {
        return conn;
    }

    public void setConn(int conn) {
        this.conn = conn;
    }

    public double getMemUseThreshold() {
        return memUseThreshold;
    }

    public void setMemUseThreshold(double memUseThreshold) {
        this.memUseThreshold = memUseThreshold;
    }

    public double getHitPercentThreshold() {
        return hitPercentThreshold;
    }

    public void setHitPercentThreshold(double hitPercentThreshold) {
        this.hitPercentThreshold = hitPercentThreshold;
    }

    public double getMemUsePercent() {
        return memUsePercent;
    }

    public void setMemUsePercent(double memUsePercent) {
        this.memUsePercent = memUsePercent;
    }

    public double getHitPercent() {
        return hitPercent;
    }

    public void setHitPercent(double hitPercent) {
        this.hitPercent = hitPercent;
    }

    @Override
    public String toString() {
        return "AppDetailVO{" +
                "appDescList=" + appDescList +
                ", mem=" + mem +
                ", currentMem=" + currentMem +
                ", machineNum=" + machineNum +
                ", masterNum=" + masterNum +
                ", slaveNum=" + slaveNum +
                ", currentObjNum=" + currentObjNum +
                ", conn=" + conn +
                ", memUsePercent=" + memUsePercent +
                ", hitPercent=" + hitPercent +
                '}';
    }
}
