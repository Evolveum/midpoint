package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.math.BigInteger;

import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

public class TaskItem implements Serializable {

	private static final long serialVersionUID = -8488920538885537525L;
	private String handlerUri;
	private String objectRef;
	private String oid;
	private String name;
	private Long lastRunStartTimestamp;
	private Long lastRunFinishTimestamp;
	private TaskItemExecutionStatus executionStatus;
	private TaskItemExclusivityStatus exclusivityStatus;
	private TaskItemRecurrenceStatus recurrenceStatus;
	private Long scheduleInterval;
	private String binding;

	public TaskItem() {

	}

	public TaskItem(Task task) {
		this.handlerUri = task.getHandlerUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		this.lastRunStartTimestamp = task.getLastRunStartTimestamp();
		this.lastRunFinishTimestamp = task.getLastRunFinishTimestamp();
		setTaskItemExecutionStatus(task.getExecutionStatus());
	}

	public TaskItem(TaskType task) {
		this.handlerUri = task.getHandlerUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		if (task.getLastRunStartTimestamp() != null) {
			this.lastRunStartTimestamp = new Long(
					XsdTypeConverter.toMillis(task.getLastRunStartTimestamp()));
		}
		if (task.getLastRunFinishTimestamp() != null) {
			this.lastRunFinishTimestamp = new Long(
					XsdTypeConverter.toMillis(task.getLastRunFinishTimestamp()));
		}
		setTaskItemExecutionStatus(TaskExecutionStatus.fromTaskType(task
				.getExecutionStatus()));
	}

	public void setTaskItemExecutionStatus(
			TaskExecutionStatus taskExecusionStatus) {
		if (taskExecusionStatus.equals(TaskExecutionStatus.RUNNING)) {
			executionStatus = TaskItemExecutionStatus.RUNNING;
			return;
		}
		if (taskExecusionStatus.equals(TaskExecutionStatus.WAITING)) {
			executionStatus = TaskItemExecutionStatus.WAITING;
			return;
		}
		if (taskExecusionStatus.equals(TaskExecutionStatus.CLOSED)) {
			executionStatus = TaskItemExecutionStatus.CLOSED;
			return;
		}
	}

	private TaskExclusivityStatusType getTaskTypeExclusivityStatusType() {
		if (getExclusivityStatus() == null
				|| getExclusivityStatus().equals(
						TaskItemExclusivityStatus.RELEASED)) {
			return TaskExclusivityStatusType.RELEASED;
		} else {
			return TaskExclusivityStatusType.CLAIMED;
		}

	}

	private TaskExecutionStatusType getTaskTypeExecutionStatusType() {
		if (getExecutionStatus() == null
				|| getExecutionStatus().equals(TaskItemExecutionStatus.RUNNING)) {
			return TaskExecutionStatusType.RUNNING;
		} else {
			if (getExecutionStatus().equals(TaskItemExecutionStatus.WAITING)) {
				return TaskExecutionStatusType.WAITING;
			} else {
				return TaskExecutionStatusType.CLOSED;
			}
		}

	}

	private TaskRecurrenceType getTaskTypeRecurrenceType() {
		if (getRecurrenceStatus() == null
				|| getRecurrenceStatus().equals(TaskRecurrenceType.RECURRING)) {
			return TaskRecurrenceType.RECURRING;
		} else {
			return TaskRecurrenceType.SINGLE;

		}

	}

	public TaskType toTaskType() {

		TaskType taskType = new TaskType();
		if (getOid() != null) {
			taskType.setOid(this.getOid());
		}
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setOid(getObjectRef());
		taskType.setObjectRef(ort);
		taskType.setHandlerUri(getHandlerUri());
		taskType.setName(getName());
		taskType.setExclusivityStatus(getTaskTypeExclusivityStatusType());
		taskType.setExecutionStatus(getTaskTypeExecutionStatusType());
		taskType.setRecurrence(getTaskTypeRecurrenceType());
		taskType.setBinding(TaskBindingType.TIGHT);
		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(BigInteger.valueOf(getScheduleInterval()));
		taskType.setSchedule(schedule);

		return taskType;

	}

	public void setExclusivityStatus(Task task) {
		if (task.getExclusivityStatus().equals(TaskExclusivityStatus.CLAIMED)) {
			exclusivityStatus = TaskItemExclusivityStatus.CLAIMED;
		}
		if (task.getExclusivityStatus().equals(TaskExclusivityStatus.RELEASED)) {
			exclusivityStatus = TaskItemExclusivityStatus.RELEASED;
		}
	}

	public TaskItemExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	public TaskItemExclusivityStatus getExclusivityStatus() {
		return exclusivityStatus;
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

	public TaskItemRecurrenceStatus getRecurrenceStatus() {
		return recurrenceStatus;
	}

	public void setRecurrenceStatus(TaskItemRecurrenceStatus recurrenceStatus) {
		this.recurrenceStatus = recurrenceStatus;
	}

	public Long getScheduleInterval() {
		return scheduleInterval;
	}

	public void setScheduleInterval(Long scheduleInterval) {
		this.scheduleInterval = scheduleInterval;
	}

	public String getBinding() {
		return binding;
	}

	public void setBinding(String binding) {
		this.binding = binding;
	}

}
