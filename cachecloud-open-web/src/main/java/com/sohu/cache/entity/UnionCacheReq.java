package com.sohu.cache.entity;

public class UnionCacheReq {
	private String userId;
	private String token;
	private String groupId;
	private String groupName;
	private int resourceType;
	private int clusterType;
	private String bucket;
	private String nodeList;
	private String userName;
	private String password;
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getResourceType() {
		return resourceType;
	}

	public void setResourceType(int resourceType) {
		this.resourceType = resourceType;
	}

	public int getClusterType() {
		return clusterType;
	}

	public void setClusterType(int clusterType) {
		this.clusterType = clusterType;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getNodeList() {
		return nodeList;
	}

	public void setNodeList(String nodeList) {
		this.nodeList = nodeList;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
