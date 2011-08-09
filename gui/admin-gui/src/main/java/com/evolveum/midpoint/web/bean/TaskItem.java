package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ExtensionProcessor;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
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
	private String lastRunStartTimestamp;
	private String lastRunFinishTimestamp;
	private TaskItemExecutionStatus executionStatus;
	private TaskItemExclusivityStatus exclusivityStatus;
	private TaskItemRecurrenceStatus recurrenceStatus;
	private Long scheduleInterval;
	private String binding;
	private long progress;
	private OperationResult result;
	private PropertyContainer extension;

	public TaskItem() {

	}

	public TaskItem(Task task) {
		this.handlerUri = task.getHandlerUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		Calendar calendar = GregorianCalendar.getInstance();
		if (task.getLastRunStartTimestamp() != null) {
			calendar.setTimeInMillis(task.getLastRunStartTimestamp());
			this.lastRunStartTimestamp = calendar.toString();
		}
		if (task.getLastRunFinishTimestamp() != null) {
			calendar.setTimeInMillis(task.getLastRunFinishTimestamp());
			this.lastRunFinishTimestamp = calendar.toString();
		}
		this.executionStatus = TaskItemExecutionStatus.fromTask(task
				.getExecutionStatus());
		this.exclusivityStatus = TaskItemExclusivityStatus.fromTask(task
				.getExclusivityStatus());
		this.scheduleInterval = task.getSchedule().getInterval().longValue();

		this.progress = task.getProgress();
		if (task.getResult() != null) {
			this.result = task.getResult();
		}
		if (task.getExtension() != null) {
			this.extension = task.getExtension();
		}
		// recurrenceStatus = TaskItemRecurrenceStatus.fromTask(task.get)
	}

	public TaskItem(TaskType task) {
		this.handlerUri = task.getHandlerUri();
		this.objectRef = task.getObjectRef().getOid();
		this.oid = task.getOid();
		this.name = task.getName();
		if (task.getLastRunStartTimestamp() != null) {
			this.lastRunStartTimestamp = task.getLastRunStartTimestamp()
					.toString();
		}
		if (task.getLastRunFinishTimestamp() != null) {
			this.lastRunFinishTimestamp = task.getLastRunFinishTimestamp()
					.toString();
		}
		this.executionStatus = TaskItemExecutionStatus
				.fromTask(TaskExecutionStatus.fromTaskType(task
						.getExecutionStatus()));
		this.exclusivityStatus = TaskItemExclusivityStatus
				.fromTask(TaskExclusivityStatus.fromTaskType(task
						.getExclusivityStatus()));
		this.scheduleInterval = task.getSchedule().getInterval().longValue();
		if (task.getProgress() != null) {
			this.progress = task.getProgress().longValue();
		}
		if (task.getResult() != null) {
			this.result = OperationResult.createOperationResult(task
					.getResult());
		}
		if (task.getExtension() != null) {
			this.extension = ExtensionProcessor.parseExtension(task
					.getExtension());
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
		taskType.setExclusivityStatus(TaskItemExclusivityStatus.toTask(
				getExclusivityStatus()).toTaskType());
		taskType.setExecutionStatus(TaskItemExecutionStatus.toTask(
				getExecutionStatus()).toTaskType());

		taskType.setRecurrence(TaskItemRecurrenceStatus.toTask(
				getRecurrenceStatus()).toTaskType());

		taskType.setBinding(TaskBindingType.TIGHT);
		ScheduleType schedule = new ScheduleType();
		schedule.setInterval(BigInteger.valueOf(getScheduleInterval()));
		taskType.setSchedule(schedule);
		if (getResult() != null) {
			taskType.setResult(getResult().createOperationResultType());
		}
		if (getExtension() != null) {
			try {
				Extension extension = new Extension();
				List<Element> extensionProperties = getExtension()
						.serializePropertiesToDom(DOMUtil.getDocument());
				extension.getAny().addAll(extensionProperties);
				taskType.setExtension(extension);
			} catch (SchemaProcessorException ex) {
				// TODO: error handling
			}
		}
	
			taskType.setProgress(BigInteger.valueOf(getProgress()));
	
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

	public String getLastRunStartTimestamp() {
		return lastRunStartTimestamp;
	}

	public void setLastRunStartTimestamp(String lastRunStartTimestamp) {
		this.lastRunStartTimestamp = lastRunStartTimestamp;
	}

	public String getLastRunFinishTimestamp() {
		return lastRunFinishTimestamp;
	}

	public void setLastRunFinishTimestamp(String lastRunFinishTimestamp) {
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

	public void setExecutionStatus(TaskItemExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	public void setExclusivityStatus(TaskItemExclusivityStatus exclusivityStatus) {
		this.exclusivityStatus = exclusivityStatus;
	}

	public long getProgress() {
		return progress;
	}

	public void setProgress(long progress) {
		this.progress = progress;
	}

	public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
	}

	public PropertyContainer getExtension() {
		return extension;
	}

	public void setExtension(PropertyContainer extension) {
		this.extension = extension;
	}

}
