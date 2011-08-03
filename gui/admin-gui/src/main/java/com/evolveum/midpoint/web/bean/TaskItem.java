package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.task.api.Task;

public class TaskItem {

	private String handlerUri;
	private String objectRef;
	private String oid;
	private String name;
	private Long lastRunStartTimestamp;
	private Long lastRunFinishTimestamp;

	public TaskItem(Task task){
		this.handlerUri = task.getHanderUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		this.lastRunStartTimestamp = task.getLastRunStartTimestamp();
		this.lastRunFinishTimestamp = task.getLastRunFinishTimestamp();
	}

	public String getHandlerUri() {
		return handlerUri;
	}

	public void setHandlerUri(String handlerUri) {
		this.handlerUri = handlerUri;
	}

	public String getObjectRef() {
		return objectRef;
	}

	public void setObjectRef(String objectRef) {
		this.objectRef = objectRef;
	}

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getLastRunStartTimestamp() {
		return lastRunStartTimestamp;
	}

	public void setLastRunStartTimestamp(Long lastRunStartTimestamp) {
		this.lastRunStartTimestamp = lastRunStartTimestamp;
	}

	public Long getLastRunFinishTimestamp() {
		return lastRunFinishTimestamp;
	}

	public void setLastRunFinishTimestamp(Long lastRunFinishTimestamp) {
		this.lastRunFinishTimestamp = lastRunFinishTimestamp;
	}
	
	
}
