package com.sohu.cache.entity;

/**
 * Created by renxin on 2017/7/27.
 */
public class MachineGroupStats {
    private int machineGroupId;
    private String groupName;
    private int machineNum;
    private long totalMem;
    private double memUseRatio;
    private int instanceNum;
    private int appNum;
    private long ops;

    public int getGroupId() {
        return machineGroupId;
    }

    public void setGroupId(int machineGroupId) {
        this.machineGroupId = machineGroupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getMachineNum() {
        return machineNum;
    }

    public void setMachineNum(int machineNum) {
        this.machineNum = machineNum;
    }

    public long getTotalMem() {
        return totalMem;
    }

    public void setTotalMem(long totalMem) {
        this.totalMem = totalMem;
    }

    public double getMemUseRatio() {
        return memUseRatio;
    }

    public void setMemUseRatio(double memUseRatio) {
        this.memUseRatio = memUseRatio;
    }

    public int getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(int instanceNum) {
        this.instanceNum = instanceNum;
    }

    public int getAppNum() {
        return appNum;
    }

    public void setAppNum(int appNum) {
        this.appNum = appNum;
    }

    public long getOps() {
        return ops;
    }

    public void setOps(long ops) {
        this.ops = ops;
    }
}
