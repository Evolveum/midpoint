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

import it.sauronsoftware.cron4j.InvalidPatternException;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DurationFormatUtils;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ExtensionProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public class TaskItem extends SelectableBean {

    private static final long serialVersionUID = -8488920538885537525L;
    private String handlerUri;
    private String objectRef;
    private String oid;
    private String name;
    private UserType owner;
//    private String lastRunStartTimestamp;
    private Long lastRunStartTimestampLong;
//    private String lastRunFinishTimestamp;
    private Long lastRunFinishTimestampLong;
//    private String nextRunStartTime;
    private Long nextRunStartTimeLong;
	private TaskItemExecutionStatus executionStatus;
    private TaskItemExclusivityStatus exclusivityStatus;
    private TaskItemRecurrenceStatus recurrenceStatus;
    private Long scheduleInterval;
    private String scheduleCronLikePattern;
    private TaskItemBinding binding;
    private long progress;
    private OperationResult result;
    private PrismContainer extension;
    private String taskIdentifier;
    
    private TaskManager taskManager;

    public TaskItem(TaskManager taskManager) {
    	this.taskManager = taskManager;
    }

    public TaskItem(Task task, TaskManager taskManager) {
    	this.taskManager = taskManager;
        this.handlerUri = task.getHandlerUri();
        this.taskIdentifier = task.getTaskIdentifier();
        if (task.getObjectRef() != null)
            this.objectRef = task.getObjectRef().getOid();
        else
            this.objectRef = null;
        this.oid = task.getOid();
        this.name = task.getName();
        this.owner = task.getOwner();
        this.lastRunStartTimestampLong = task.getLastRunStartTimestamp();
        this.lastRunFinishTimestampLong = task.getLastRunFinishTimestamp();
        this.nextRunStartTimeLong = task.getNextRunStartTime();
        this.executionStatus = TaskItemExecutionStatus.fromTask(task
                .getExecutionStatus());
        this.exclusivityStatus = TaskItemExclusivityStatus.fromTask(task
                .getExclusivityStatus());
        this.binding = TaskItemBinding.fromTask(task.getBinding());
        
        this.scheduleInterval = null;
        this.scheduleCronLikePattern = null;
        this.recurrenceStatus = TaskItemRecurrenceStatus.SINGLE;
        if (task.getSchedule() != null) {
        	if (task.getSchedule().getInterval() != null) {
        		this.scheduleInterval = task.getSchedule().getInterval().longValue();
        		this.recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        	}
        	if (task.getSchedule().getCronLikePattern() != null) {
        		this.scheduleCronLikePattern = task.getSchedule().getCronLikePattern();
        		this.recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        	}
        }

        this.progress = task.getProgress();
        if (task.getResult() != null) {
            this.result = task.getResult();
        }
        if (task.getExtension() != null) {
            this.extension = task.getExtension();
        }

    }

    public TaskItem(TaskType taskType, TaskManager taskManager) {
    	this.taskManager = taskManager;
        this.handlerUri = taskType.getHandlerUri();
        this.taskIdentifier = taskType.getTaskIdentifier();
        if (taskType.getObjectRef() != null)
            this.objectRef = taskType.getObjectRef().getOid();
        else
            this.objectRef = null;
        this.oid = taskType.getOid();
        this.name = taskType.getName();
        if (taskType.getLastRunStartTimestamp() != null) {
            lastRunStartTimestampLong = XmlTypeConverter.toMillis(taskType.getLastRunStartTimestamp());
        }
        if (taskType.getLastRunFinishTimestamp() != null) {
            lastRunFinishTimestampLong = XmlTypeConverter.toMillis(taskType.getLastRunFinishTimestamp());
        }
        if (taskType.getNextRunStartTime() != null) {
            nextRunStartTimeLong = XmlTypeConverter.toMillis(taskType.getNextRunStartTime());
        }

        this.executionStatus = TaskItemExecutionStatus
                .fromTask(TaskExecutionStatus.fromTaskType(taskType
                        .getExecutionStatus()));
        this.exclusivityStatus = TaskItemExclusivityStatus
                .fromTask(TaskExclusivityStatus.fromTaskType(taskType
                        .getExclusivityStatus()));
        this.binding = TaskItemBinding.fromTask(TaskBinding.fromTaskType(taskType.getBinding()));

        this.scheduleInterval = null;
        this.scheduleCronLikePattern = null;
        this.recurrenceStatus = TaskItemRecurrenceStatus.SINGLE;
        if (taskType.getSchedule() != null) {
        	if (taskType.getSchedule().getInterval() != null) {
        		this.scheduleInterval = taskType.getSchedule().getInterval().longValue();
        		this.recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        	}
        	if (taskType.getSchedule().getCronLikePattern() != null) {
        		this.scheduleCronLikePattern = taskType.getSchedule().getCronLikePattern();
        		this.recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        	}
        }

        if (taskType.getProgress() != null) {
            this.progress = taskType.getProgress().longValue();
        }
        if (taskType.getResult() != null) {
            this.result = OperationResult.createOperationResult(taskType
                    .getResult());
        }
        if (taskType.getExtension() != null) {
            try {
                this.extension = ExtensionProcessor.parseExtension(taskType
                        .getExtension());
            } catch (SchemaException e) {
                // FIXME: this is probably wrong
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    /**
     * @throws InvalidPatternException (a cron4j-generated subtype of RuntimeException) if cron-like pattern is not valid.
     * @return
     */
    public TaskType toTaskType() {

        TaskType taskType = new TaskType();
        if (getOid() != null) {
            taskType.setOid(this.getOid());
        }
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(getObjectRef());
        taskType.setObjectRef(ort);
        taskType.setTaskIdentifier(taskIdentifier);
        taskType.setHandlerUri(getHandlerUri());
        taskType.setName(getName());
        taskType.setExclusivityStatus(TaskItemExclusivityStatus.toTask(
                getExclusivityStatus()).toTaskType());
        taskType.setExecutionStatus(TaskItemExecutionStatus.toTask(
                getExecutionStatus()).toTaskType());
        taskType.setBinding(TaskItemBinding.toTask(getBinding()).toTaskType());
        
        recurrenceStatus = TaskItemRecurrenceStatus.SINGLE;
        ScheduleType schedule = new ScheduleType();
        if (getScheduleInterval() != null && getScheduleInterval() > 0) {
        	schedule.setInterval(BigInteger.valueOf(getScheduleInterval()));
        	recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        }
        if (getScheduleCronLikePattern() != null && !getScheduleCronLikePattern().isEmpty()) {
        	schedule.setCronLikePattern(getScheduleCronLikePattern());
        	recurrenceStatus = TaskItemRecurrenceStatus.RECURRING;
        } 
        taskType.setSchedule(schedule);

        taskType.setRecurrence(TaskItemRecurrenceStatus.toTask(
                getRecurrenceStatus()).toTaskType());

        // here we must compute when the task is to be run next (for recurring tasks);
        // beware that the schedule & recurring type must be known for this taskType
        long nextRunTime = taskManager.determineNextRunStartTime(taskType);		// for single-run tasks returns 0
        if (nextRunTime > 0)
        	taskType.setNextRunStartTime(XmlTypeConverter.toXMLGregorianCalendar(nextRunTime));
        
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

		if (owner != null) {
			taskType.setOwnerRef(ObjectTypeUtil.createObjectRef(owner));
		}
        
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
    
    public UserType getOwner() {
        return owner;
    }

    public void setOwner(UserType owner) {
        this.owner = owner;
    }

    public String getLastRunStartTimestamp() {
        return formatDateTime(lastRunStartTimestampLong);  
    }

//    public void setLastRunStartTimestamp(String lastRunStartTimestamp) {
//        this.lastRunStartTimestamp = lastRunStartTimestamp;
//    }

    public String getLastRunFinishTimestamp() {
    	return formatDateTime(lastRunFinishTimestampLong);
    }

//    public void setLastRunFinishTimestamp(String lastRunFinishTimestamp) {
//        this.lastRunFinishTimestamp = lastRunFinishTimestamp;
//    }

    public String getNextRunStartTime() {
    	return formatDateTime(nextRunStartTimeLong);
	}

//	public void setNextRunStartTime(String nextRunStartTime) {
//		this.nextRunStartTime = nextRunStartTime;
//	}

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

    public PrismContainer getExtension() {
        return extension;
    }

    public void setExtension(PrismContainer extension) {
        this.extension = extension;
    }

    private String formatDateTime(Long millis) {
    	if (millis == null || millis == 0) {
    		return "-";
    	} else {
    		return new Date(millis).toString();
//    		return FastDateFormat.getInstance().format(millis);
//    		return XsdTypeConverter.toXMLGregorianCalendar(millis).toXMLFormat();
    	}
    }

    private String formatTimeInterval(Long interval) {
    	return DurationFormatUtils.formatDurationWords(interval, true, true);
    }
    
    public boolean taskRunNotFinished() {
    	return lastRunStartTimestampLong != null && 
			(lastRunFinishTimestampLong == null || lastRunStartTimestampLong > lastRunFinishTimestampLong);
    }
    
    public boolean isAlive() {
    	return taskManager.isTaskThreadActive(taskIdentifier);
    }
    
    public String getCurrentRunTime() {
    	if (taskRunNotFinished()) {
    		if (isAlive()) {
    			return formatTimeInterval(System.currentTimeMillis() - lastRunStartTimestampLong);
    		} else {
    			return "finish time unknown, task thread dead";
    		}
    	} else {
    		return "-";
    	}
    }

    public String getStartsAgainAfter() {
    	long current = System.currentTimeMillis();
    	if (!TaskItemRecurrenceStatus.RECURRING.equals(recurrenceStatus)) {
    		return "-";
    	} 
//    	else if (taskRunNotFinished()) {
//    		return "to be computed";		
    	else if (nextRunStartTimeLong != null && nextRunStartTimeLong > 0) {
    			if (nextRunStartTimeLong > current+1000) {
    				return "in " + formatTimeInterval(nextRunStartTimeLong - System.currentTimeMillis());
    			} else {
    				return "now";
    			}
    	} else {
    		return "-";		// either a task is not recurring, or the next run start time has not been determined yet 
    						// TODO: if necessary, this could be made more clear in the future
    	}
    }
}
