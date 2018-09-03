package com.sohu.cache.entity;

/**
 * Created by jiguang on 2016/9/18.
 */
public class ConfigTemplate {
    private int id;
    private String name;
    private int architecture;
    private String extraDesc;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getArchitecture() {
        return architecture;
    }

    public void setArchitecture(int architecture) {
        this.architecture = architecture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtraDesc() {
        return extraDesc;
    }

    public void setExtraDesc(String extraDesc) {
        this.extraDesc = extraDesc;
    }
}
