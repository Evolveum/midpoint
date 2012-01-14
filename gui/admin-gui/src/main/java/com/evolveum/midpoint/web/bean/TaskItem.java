/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.bean;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ExtensionProcessor;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

public class TaskItem extends SelectableBean {

    private static final long serialVersionUID = -8488920538885537525L;
    private String handlerUri;
    private String objectRef;
    private String oid;
    private String name;
    private String lastRunStartTimestamp;
    private String lastRunFinishTimestamp;
    private String nextRunStartTime;
	private TaskItemExecutionStatus executionStatus;
    private TaskItemExclusivityStatus exclusivityStatus;
    private TaskItemRecurrenceStatus recurrenceStatus;
    private Long scheduleInterval;
    private String scheduleCronLikePattern;
    private TaskItemBinding binding;
    private long progress;
    private OperationResult result;
    private PropertyContainer extension;
    
    private TaskManager taskManager;

    public TaskItem(TaskManager taskManager) {
    	this.taskManager = taskManager;
    }

    public TaskItem(Task task, TaskManager taskManager) {
    	this.taskManager = taskManager;
        this.handlerUri = task.getHandlerUri();
        if (task.getObjectRef() != null)
            this.objectRef = task.getObjectRef().getOid();
        else
            this.objectRef = null;
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
        if (task.getNextRunStartTime() != null) {
            calendar.setTimeInMillis(task.getNextRunStartTime());
            this.nextRunStartTime = calendar.toString();
        }
        this.executionStatus = TaskItemExecutionStatus.fromTask(task
                .getExecutionStatus());
        this.exclusivityStatus = TaskItemExclusivityStatus.fromTask(task
                .getExclusivityStatus());
        this.binding = TaskItemBinding.fromTask(task.getBinding());
        
        this.scheduleInterval = null;
        this.scheduleCronLikePattern = null;
        if (task.getSchedule() != null) {
        	if (task.getSchedule().getInterval() != null) 
               this.scheduleInterval = task.getSchedule().getInterval().longValue();
       		this.scheduleCronLikePattern = task.getSchedule().getCronLikePattern();
        }

        this.progress = task.getProgress();
        if (task.getResult() != null) {
            this.result = task.getResult();
        }
        if (task.getExtension() != null) {
            this.extension = task.getExtension();
        }
        // recurrenceStatus = TaskItemRecurrenceStatus.fromTask(task.get)
    }

    public TaskItem(TaskType task, TaskManager taskManager) {
    	this.taskManager = taskManager;
        this.handlerUri = task.getHandlerUri();
        if (task.getObjectRef() != null)
            this.objectRef = task.getObjectRef().getOid();
        else
            this.objectRef = null;
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
        if (task.getNextRunStartTime() != null) {
            this.nextRunStartTime = task.getNextRunStartTime().toString();
        }

        this.executionStatus = TaskItemExecutionStatus
                .fromTask(TaskExecutionStatus.fromTaskType(task
                        .getExecutionStatus()));
        this.exclusivityStatus = TaskItemExclusivityStatus
                .fromTask(TaskExclusivityStatus.fromTaskType(task
                        .getExclusivityStatus()));
        this.binding = TaskItemBinding.fromTask(TaskBinding.fromTaskType(task.getBinding()));

        this.scheduleInterval = null;
        this.scheduleCronLikePattern = null;
        if (task.getSchedule() != null) {
        	if (task.getSchedule().getInterval() != null) 
               this.scheduleInterval = task.getSchedule().getInterval().longValue();
       		this.scheduleCronLikePattern = task.getSchedule().getCronLikePattern();
        }

        if (task.getProgress() != null) {
            this.progress = task.getProgress().longValue();
        }
        if (task.getResult() != null) {
            this.result = OperationResult.createOperationResult(task
                    .getResult());
        }
        if (task.getExtension() != null) {
            try {
                this.extension = ExtensionProcessor.parseExtension(task
                        .getExtension());
            } catch (SchemaException e) {
                // FIXME: this is probably wrong
                throw new IllegalArgumentException(e.getMessage(), e);
            }
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
        taskType.setBinding(TaskItemBinding.toTask(getBinding()).toTaskType());

        taskType.setRecurrence(TaskItemRecurrenceStatus.toTask(
                getRecurrenceStatus()).toTaskType());
        
        ScheduleType schedule = new ScheduleType();
        schedule.setInterval(BigInteger.valueOf(getScheduleInterval()));
        schedule.setCronLikePattern(getScheduleCronLikePattern());
        taskType.setSchedule(schedule);

        // here we must compute when the task is to be run next (for recurring tasks);
        // beware that the schedule & recurring type must be known for this taskType
        long nextRunTime = taskManager.determineNextRunStartTime(taskType);		// for single-run tasks returns 0
        if (nextRunTime > 0)
        	taskType.setNextRunStartTime(XsdTypeConverter.toXMLGregorianCalendar(nextRunTime));
        
        if (getResult() != null) {
            taskType.setResult(getResult().createOperationResultType());
        }
        if (getExtension() != null) {
            try {
                Extension extension = new Extension();
                List<Object> extensionProperties = getExtension()
                        .serializePropertiesToJaxb(DOMUtil.getDocument());
                extension.getAny().addAll(extensionProperties);
                taskType.setExtension(extension);
            } catch (SchemaException ex) {
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

    public String getNextRunStartTime() {
		return nextRunStartTime;
	}

	public void setNextRunStartTime(String nextRunStartTime) {
		this.nextRunStartTime = nextRunStartTime;
	}

	public String getScheduleCronLikePattern() {
		return scheduleCronLikePattern;
	}

	public void setScheduleCronLikePattern(String scheduleCronLikePattern) {
		this.scheduleCronLikePattern = scheduleCronLikePattern;
	}

    public TaskItemRecurrenceStatus getRecurrenceStatus() {
        return recurrenceStatus;
    }

    public void setRecurrenceStatus(TaskItemRecurrenceStatus recurrenceStatus) {
        this.recurrenceStatus = recurrenceStatus;
    }

    public Long getScheduleInterval() {
//    	System.out.println("getScheduleInterval: " + scheduleInterval);
        return scheduleInterval;
    }

    public void setScheduleInterval(Long scheduleInterval) {
//    	System.out.println("setScheduleInterval to " + scheduleInterval);
        this.scheduleInterval = scheduleInterval;
    }

    public TaskItemBinding getBinding() {
        return binding;
    }

    public void setBinding(TaskItemBinding binding) {
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
