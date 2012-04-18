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

import java.util.Date;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.time.DurationFormatUtils;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
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
    private Long lastRunStartTimestampLong;
    private Long lastRunFinishTimestampLong;
    private Long nextRunStartTimeLong;
	private TaskItemExecutionStatus executionStatus;
    private TaskItemRecurrenceStatus recurrenceStatus;
    private Integer scheduleInterval;
    private String scheduleCronLikePattern;
    private TaskItemBinding binding;
    private long progress;
    private OperationResult result;
    private PrismContainer extension;
    private String taskIdentifier;

    private String executesAt;

    private Task task;

    public TaskItem(TaskManager taskManager) {
    }

    // used to convert task currently executing at this node (then runningTaskInfo is null!) or in cluster
    public TaskItem(Task task, TaskManager taskManager, RunningTasksInfo runningTasksInfo) {
        initializeFromTask(task, taskManager, runningTasksInfo);
    }

    private void initializeFromTask(Task task, TaskManager taskManager, RunningTasksInfo runningTasksInfo) {

        this.task = task;

        this.handlerUri = task.getHandlerUri();
        this.taskIdentifier = task.getTaskIdentifier();
        if (task.getObjectRef() != null)
            this.objectRef = task.getObjectRef().getOid();
        else
            this.objectRef = null;
        this.oid = task.getOid();
        this.name = task.getName();
        this.owner = task.getOwner() != null ? task.getOwner().asObjectable() : null;
        this.lastRunStartTimestampLong = task.getLastRunStartTimestamp();
        this.lastRunFinishTimestampLong = task.getLastRunFinishTimestamp();
        this.nextRunStartTimeLong = task.getNextRunStartTime();
        //this.exclusivityStatus = TaskItemExclusivityStatus.fromTask(task.getExclusivityStatus());
        this.binding = TaskItemBinding.fromTask(task.getBinding());
        
        this.scheduleInterval = null;
        this.scheduleCronLikePattern = null;
        this.recurrenceStatus = TaskItemRecurrenceStatus.SINGLE;
        if (task.getSchedule() != null) {
        	if (task.getSchedule().getInterval() != null) {
        		this.scheduleInterval = task.getSchedule().getInterval();
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

        if (runningTasksInfo == null) {         // when listing nodes currently executing at this node
            this.executesAt = taskManager.getNodeId();
            this.executionStatus = TaskItemExecutionStatus.RUNNING;
        } else {
            RunningTasksInfo.NodeInfo nodeInfo = runningTasksInfo.findNodeInfoForTask(this.getOid());
            if (nodeInfo != null) {
                this.executesAt = nodeInfo.getNodeType().asObjectable().getNodeIdentifier();
                this.executionStatus = TaskItemExecutionStatus.RUNNING;
            } else {
                this.executesAt = null;
                this.executionStatus = TaskItemExecutionStatus.fromTask(task.getExecutionStatus());
            }
        }
    }


    public TaskItem(PrismObject<TaskType> taskPrism, TaskManager taskManager, RunningTasksInfo runningTasksInfo) {

        OperationResult result = createOperationResult("TaskItem");

        try {
            Task task = taskManager.createTaskInstance(taskPrism, result);
            initializeFromTask(task, taskManager, runningTasksInfo);
        } catch (SchemaException e) {
            // should not occur
            throw new SystemException("Cannot initialize task instance due to unexpected schema exception", e);
        }

    }

    /**
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
//        taskType.setExclusivityStatus(TaskItemExclusivityStatus.toTask(
//                getExclusivityStatus()).toTaskType());
        taskType.setExecutionStatus(TaskItemExecutionStatus.toTask(
                getExecutionStatus()).toTaskType());
        taskType.setBinding(TaskItemBinding.toTask(getBinding()).toTaskType());
        
        recurrenceStatus = TaskItemRecurrenceStatus.SINGLE;
        ScheduleType schedule = new ScheduleType();
        if (getScheduleInterval() != null && getScheduleInterval() > 0) {
        	schedule.setInterval(getScheduleInterval());
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
        {
//        	Long nextRunTime = taskManager.determineNextRunStartTime(taskType);		// for single-run tasks returns 0
//        	if (nextRunTime != null && nextRunTime > 0)
//        		taskType.setNextRunStartTime(XmlTypeConverter.createXMLGregorianCalendar(nextRunTime));
        }
        
        if (getResult() != null) {
            taskType.setResult(getResult().createOperationResultType());
        }
        if (getExtension() != null) {
            taskType.setExtension((ExtensionType) getExtension().getValue().asContainerable());
        }

        taskType.setProgress(getProgress());

		if (owner != null) {
			taskType.setOwnerRef(ObjectTypeUtil.createObjectRef(owner));
		}
        
        return taskType;
    }

    public TaskItemExecutionStatus getExecutionStatus() {
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
    
    public UserType getOwner() {
        return owner;
    }

    public void setOwner(UserType owner) {
        this.owner = owner;
    }

    public String getLastRunStartTimestamp() {
        return formatDateTime(lastRunStartTimestampLong);  
    }

    public String getLastRunFinishTimestamp() {
    	return formatDateTime(lastRunFinishTimestampLong);
    }

    public String getNextRunStartTime() {
    	return formatDateTime(nextRunStartTimeLong);
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

    public Integer getScheduleInterval() {
        return scheduleInterval;
    }

    public void setScheduleInterval(Integer scheduleInterval) {
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
    
    public boolean isAliveClusterwide() {
    	return executesAt != null;
    }

//    public String getAliveAsText() {
//        if (isAlive()) {
//            return "true";
//        } else {
//            return "-";
//        }
//    }

    public String getExecutesAt() {
        return executesAt;
    }

    public String getExecutesAtFormatted() {
        return executesAt == null ? "-" : executesAt;
    }

    public void setExecutesAt(String executesAt) {
        this.executesAt = executesAt;
    }

    public String getCurrentRunTime() {
    	if (taskRunNotFinished()) {
    		if (isAliveClusterwide()) {
    			return formatTimeInterval(System.currentTimeMillis() - lastRunStartTimestampLong);
    		} else {
    			return "-"; //was: "finish time unknown, task thread dead";
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
        else if (TaskItemBinding.TIGHT.equals(binding)) {
            return "runs continually";
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

    public String getCategory() {
        return task.getCategory();
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(TaskItem.class.getName() + "." + methodName);
    }

}
