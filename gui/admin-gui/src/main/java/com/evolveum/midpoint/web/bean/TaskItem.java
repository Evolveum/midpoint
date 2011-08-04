package com.evolveum.midpoint.web.bean;

import java.io.Serializable;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;

public class TaskItem implements Serializable{

	
	private static final long serialVersionUID = -8488920538885537525L;
	private String handlerUri;
	private String objectRef;
	private String oid;
	private String name;
	private Long lastRunStartTimestamp;
	private Long lastRunFinishTimestamp;
	private TaskItemExecutionStatus executionStatus;
	

	public TaskItem(Task task){
		this.handlerUri = task.getHandlerUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		this.lastRunStartTimestamp = task.getLastRunStartTimestamp();
		this.lastRunFinishTimestamp = task.getLastRunFinishTimestamp();
		setTaskItemExecutionStatus(task);
	}
	
	public void setTaskItemExecutionStatus(Task task){
		if (task.getExecutionStatus().equals(TaskExecutionStatus.RUNNING)){
			executionStatus = TaskItemExecutionStatus.RUNNING;
			return;
		}
		if (task.getExecutionStatus().equals(TaskExecutionStatus.WAITING)){
			executionStatus = TaskItemExecutionStatus.WAITING;
			return;
		}
		if (task.getExecutionStatus().equals(TaskExecutionStatus.CLOSED)){
			executionStatus = TaskItemExecutionStatus.CLOSED;
			return;
		}
	}

	public TaskItemExecutionStatus getExecutionStatus(){
		return executionStatus;
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
