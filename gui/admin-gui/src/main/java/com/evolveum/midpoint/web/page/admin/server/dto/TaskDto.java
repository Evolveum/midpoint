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

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ThreadStopActionType;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class TaskDto extends Selectable {

    public static final String CLASS_DOT = TaskDto.class.getName() + ".";
    public static final String OPERATION_NEW = CLASS_DOT + "new";

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskDto.class);

    private String oid;
    private String name;
    private String category;
    private String uri;
    private boolean recurring;
    private boolean bound;
    private Integer interval;
    private String cronSpecification;
    private Date notStartBefore;
    private Date notStartAfter;
    private MisfireActionType misfireAction;
    private boolean runUntilNodeDown;
    private ThreadStopActionType threadStop;
    
    private TaskExecutionStatus rawExecutionStatus;
    private TaskDtoExecutionStatus execution;
    private String executingAt;
    private List<OperationResult> opResult;
    private OperationResultStatus status;

    private ObjectReferenceType objectRef;
    private ObjectTypes objectRefType;
    private String objectRefName;

    //helpers, won't be probably shown
    private Long lastRunStartTimestampLong;
    private Long lastRunFinishTimestampLong;
    private Long nextRunStartTimeLong;
    private Long completionTimestampLong;
    private TaskBinding binding;
    private TaskRecurrence recurrence;

    public TaskDto(Task task, ClusterStatusInformation clusterStatusInfo, TaskManager taskManager) {
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(clusterStatusInfo, "Cluster status info must not be null.");
        Validate.notNull(taskManager, "Task manager must not be null.");

        OperationResult thisOpResult = new OperationResult(OPERATION_NEW);

        oid = task.getOid();
        name = WebMiscUtil.getOrigStringFromPoly(task.getName());
        category = task.getCategory();
        uri = task.getHandlerUri();
        recurring = task.isCycle();
        bound = task.isTightlyBound();
        
        // init Schedule
        if(task.getSchedule() != null){
        	interval = task.getSchedule().getInterval();
            cronSpecification = task.getSchedule().getCronLikePattern();
            if(task.getSchedule().getMisfireAction() == null){
            	misfireAction = MisfireActionType.EXECUTE_IMMEDIATELY;
            } else {
            	misfireAction = task.getSchedule().getMisfireAction();
            }
            notStartBefore = MiscUtil.asDate(task.getSchedule().getEarliestStartTime());
            notStartAfter = MiscUtil.asDate(task.getSchedule().getLatestStartTime());
        }
        
        if(task.getThreadStopAction() == null){
        	threadStop = ThreadStopActionType.RESTART;
        } else {
        	threadStop = task.getThreadStopAction();
        }
        
        if(ThreadStopActionType.CLOSE.equals(threadStop) || ThreadStopActionType.SUSPEND.equals(threadStop)){
        	runUntilNodeDown = true;
        } else {
        	runUntilNodeDown = false;
        }

        Node n = clusterStatusInfo.findNodeInfoForTask(oid);
        this.executingAt = n != null ? n.getNodeIdentifier() : null;

        rawExecutionStatus = task.getExecutionStatus();
        execution = TaskDtoExecutionStatus.fromTaskExecutionStatus(rawExecutionStatus, n != null);
        lastRunFinishTimestampLong = task.getLastRunFinishTimestamp();
        lastRunStartTimestampLong = task.getLastRunStartTimestamp();
        completionTimestampLong = task.getCompletionTimestamp();
        nextRunStartTimeLong = task.getNextRunStartTime(new OperationResult("dummy"));

        this.objectRef = task.getObjectRef();
        if (this.objectRef != null && task.getObjectRef().getType() != null) {
            this.objectRefType = ObjectTypes.getObjectTypeFromTypeQName(task.getObjectRef().getType());
        }

        if (this.objectRef != null) {
            try {
                PrismObject<? extends ObjectType> o = task.getObject(ObjectType.class, thisOpResult);
                this.objectRefName = WebMiscUtil.getName(o);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve task's object because it does not exist; oid = " + task.getObjectOid(), e);
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve task's object because of schema exception; oid = " + task.getObjectOid(), e);
            }
        }

        this.binding = task.getBinding();
        this.recurrence = task.getRecurrenceStatus();

        opResult = new ArrayList<OperationResult>();
        
        OperationResult result = task.getResult();
        if (result != null) {
            status = result.getStatus();
            
            opResult.add(result);
            opResult.addAll(result.getSubresults());
        }
    }

    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    public String getUri() {
		return uri;
	}
    
    public void setUri(String uri) {
		this.uri = uri;
	}

	public boolean getBound() {
		return bound;
	}
	
	public void setBound(boolean bound) {
		this.bound = bound;
	}

	public Integer getInterval() {
		return interval;
	}
	
	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	public String getCronSpecification() {
		return cronSpecification;
	}

	public Date getNotStartBefore() {
		return notStartBefore;
	}

	public Date getNotStartAfter() {
		return notStartAfter;
	}

	public MisfireActionType getMisfire() {
		return misfireAction;
	}

	public boolean getRunUntilNodeDown() {
		return runUntilNodeDown;
	}

	public ThreadStopActionType getThreadStop() {
		return threadStop;
	}

	public void setThreadStop(ThreadStopActionType threadStop) {
		this.threadStop = threadStop;
	}

	public boolean getRecurring() {
		return recurring;
	}
	
	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public Long getCurrentRuntime() {
        if (isRunNotFinished()) {
            if (isAliveClusterwide()) {
                return System.currentTimeMillis() - lastRunStartTimestampLong;
            }
        }

        return null;
    }

    public TaskDtoExecutionStatus getExecution() {
        return execution;
    }

    public String getExecutingAt() {
        //return executingAt != null ? executingAt.getNodeIdentifier() : null;
        return executingAt;
    }

    public List<OperationResult> getResult() {
		return opResult;
	}

	public String getName() {
        return name;
    }
	
	public void setName(String name) {
        this.name = name;
    }

    public String getObjectRefName() {
        return objectRefName;
    }

    public Long getLastRunStartTimestampLong() {
		return lastRunStartTimestampLong;
	}

	public Long getLastRunFinishTimestampLong() {
		return lastRunFinishTimestampLong;
	}

	public ObjectTypes getObjectRefType() {
        return objectRefType;
    }

    public ObjectReferenceType getObjectRef() {
        return objectRef;
    }

    public String getOid() {
        return oid;
    }
    
    public void setOid(String oid) {
        this.oid = oid;
    }

    public Long getNextRunStartTimeLong() {
        return nextRunStartTimeLong;
    }

    public Long getScheduledToStartAgain() {
        long current = System.currentTimeMillis();

        if (execution == TaskDtoExecutionStatus.RUNNING) {
            if (!recurring) {
                return null;
            } else if (bound) {
                return -1L;             // runs continually
            }
        }

        if (nextRunStartTimeLong == null || nextRunStartTimeLong == 0) {
            return null;
        }

        if (nextRunStartTimeLong > current + 1000) {
            return nextRunStartTimeLong - System.currentTimeMillis();
        } else if (nextRunStartTimeLong < current - 60000) {
            return -2L;             // already passed
        } else {
            return 0L;              // now
        }
    }

    public OperationResultStatus getStatus() {
        return status;
    }

    private boolean isRunNotFinished() {
        return lastRunStartTimestampLong != null &&
                (lastRunFinishTimestampLong == null || lastRunStartTimestampLong > lastRunFinishTimestampLong);
    }

    private boolean isAliveClusterwide() {
        return executingAt != null;
    }

	public MisfireActionType getMisfireAction() {
		return misfireAction;
	}

	public void setMisfireAction(MisfireActionType misfireAction) {
		this.misfireAction = misfireAction;
	}

	public TaskExecutionStatus getRawExecutionStatus() {
		return rawExecutionStatus;
	}

	public void setRawExecutionStatus(TaskExecutionStatus rawExecutionStatus) {
		this.rawExecutionStatus = rawExecutionStatus;
	}

	public List<OperationResult> getOpResult() {
		return opResult;
	}

	public void setOpResult(List<OperationResult> opResult) {
		this.opResult = opResult;
	}

	public TaskBinding getBinding() {
		return binding;
	}

	public void setBinding(TaskBinding binding) {
		this.binding = binding;
	}

	public TaskRecurrence getRecurrence() {
		return recurrence;
	}

	public void setRecurrence(TaskRecurrence recurrence) {
		this.recurrence = recurrence;
	}

	public void setCronSpecification(String cronSpecification) {
		this.cronSpecification = cronSpecification;
	}

	public void setNotStartBefore(Date notStartBefore) {
		this.notStartBefore = notStartBefore;
	}

	public void setNotStartAfter(Date notStartAfter) {
		this.notStartAfter = notStartAfter;
	}

	public void setRunUntilNodeDown(boolean runUntilNodeDown) {
		this.runUntilNodeDown = runUntilNodeDown;
	}

	public void setExecution(TaskDtoExecutionStatus execution) {
		this.execution = execution;
	}

	public void setExecutingAt(String executingAt) {
		this.executingAt = executingAt;
	}

	public void setStatus(OperationResultStatus status) {
		this.status = status;
	}

	public void setObjectRef(ObjectReferenceType objectRef) {
		this.objectRef = objectRef;
	}

	public void setObjectRefType(ObjectTypes objectRefType) {
		this.objectRefType = objectRefType;
	}

	public void setObjectRefName(String objectRefName) {
		this.objectRefName = objectRefName;
	}

	public void setLastRunStartTimestampLong(Long lastRunStartTimestampLong) {
		this.lastRunStartTimestampLong = lastRunStartTimestampLong;
	}

	public void setLastRunFinishTimestampLong(Long lastRunFinishTimestampLong) {
		this.lastRunFinishTimestampLong = lastRunFinishTimestampLong;
	}

	public void setNextRunStartTimeLong(Long nextRunStartTimeLong) {
		this.nextRunStartTimeLong = nextRunStartTimeLong;
	}

    public Long getCompletionTimestamp() {
        return completionTimestampLong;
    }

    public void setCompletionTimestampLong(Long completionTimestampLong) {
        this.completionTimestampLong = completionTimestampLong;
    }
}
