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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.wf.history.WfHistoryEventDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.Constants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;

/**
 * @author lazyman
 */
public class TaskDto extends Selectable {

    public static final String CLASS_DOT = TaskDto.class.getName() + ".";
    public static final String OPERATION_NEW = CLASS_DOT + "new";

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskDto.class);
    public static final String F_MODEL_OPERATION_STATUS = "modelOperationStatus";
    public static final String F_SUBTASKS = "subtasks";
    public static final String F_NAME = "name";
    public static final String F_CATEGORY = "category";
    public static final String F_PARENT_TASK_NAME = "parentTaskName";
    public static final String F_PARENT_TASK_OID = "parentTaskOid";
    public static final String F_WORKFLOW_LAST_DETAILS = "workflowLastDetails";
    public static final String F_WORKFLOW_DELTAS_IN = "workflowDeltasIn";
    public static final String F_WORKFLOW_DELTAS_OUT = "workflowDeltasOut";
    public static final String F_IDENTIFIER = "identifier";
    public static final String F_HANDLER_URI_LIST = "handlerUriList";
    public static final String F_WORKFLOW_HISTORY = "workflowHistory";
    public static final String F_TASK_OPERATION_RESULT = "taskOperationResult";

    private String oid;
    private String identifier;
    private String name;
    private String category;
    private List<String> handlerUriList;
    private String parentTaskName;
    private String parentTaskOid;

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
    private OperationResult taskOperationResult;
    private OperationResultStatus status;

    private ModelOperationStatusDto modelOperationStatusDto;

    private ObjectReferenceType objectRef;
    private ObjectTypes objectRefType;
    private String objectRefName;

    private List<TaskDto> subtasks = new ArrayList<TaskDto>();

    private Long lastRunStartTimestampLong;
    private Long lastRunFinishTimestampLong;
    private Long nextRunStartTimeLong;
    private Long completionTimestampLong;
    private TaskBinding binding;
    private TaskRecurrence recurrence;
    private boolean workflowShadowTask;
    private String workflowProcessInstanceId;
    private boolean workflowProcessInstanceFinished;
    private String workflowLastDetails;

    private List<DeltaDto> workflowDeltasIn, workflowDeltasOut;
    private List<WfHistoryEventDto> workflowHistory;

    public TaskDto(Task task, ClusterStatusInformation clusterStatusInfo, TaskManager taskManager, ModelInteractionService modelInteractionService, TaskDtoProviderOptions options) throws SchemaException, ObjectNotFoundException {
        Validate.notNull(task, "Task must not be null.");
        Validate.notNull(clusterStatusInfo, "Cluster status info must not be null.");
        Validate.notNull(taskManager, "Task manager must not be null.");

        OperationResult thisOpResult = new OperationResult(OPERATION_NEW);

        oid = task.getOid();
        identifier = task.getTaskIdentifier();
        name = WebMiscUtil.getOrigStringFromPoly(task.getName());
        category = task.getCategory();

        handlerUriList = new ArrayList<String>();
        if (task.getHandlerUri() != null) {
            handlerUriList.add(task.getHandlerUri());
        } else {
            handlerUriList.add("-");        // todo separate presentation from model
        }
        if (task.getOtherHandlersUriStack() != null) {
            List<UriStackEntry> stack = task.getOtherHandlersUriStack().getUriStackEntry();
            for (int i = stack.size()-1; i >= 0; i--) {
                handlerUriList.add(stack.get(i).getHandlerUri());
            }
        }

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

        Node n = null;
        if (options.isUseClusterInformation()) {
            n = clusterStatusInfo.findNodeInfoForTask(oid);
        }
        this.executingAt = n != null ? n.getNodeIdentifier() : null;

        rawExecutionStatus = task.getExecutionStatus();
        execution = TaskDtoExecutionStatus.fromTaskExecutionStatus(rawExecutionStatus, n != null);
        if (!options.isUseClusterInformation() && execution == TaskDtoExecutionStatus.RUNNABLE) {
            execution = TaskDtoExecutionStatus.RUNNING_OR_RUNNABLE;
        }

        lastRunFinishTimestampLong = task.getLastRunFinishTimestamp();
        lastRunStartTimestampLong = task.getLastRunStartTimestamp();
        completionTimestampLong = task.getCompletionTimestamp();
        if (options.isGetNextRunStartTime()) {
            nextRunStartTimeLong = task.getNextRunStartTime(new OperationResult("dummy"));
        }

        this.objectRef = task.getObjectRef();
        if (this.objectRef != null && task.getObjectRef().getType() != null) {
            this.objectRefType = ObjectTypes.getObjectTypeFromTypeQName(task.getObjectRef().getType());
        }

        if (options.isResolveObjectRef() && this.objectRef != null) {
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
        taskOperationResult = task.getResult();
        
        OperationResult result = task.getResult();
        if (result != null) {
            status = result.getStatus();
            
            opResult.add(result);
            opResult.addAll(result.getSubresults());
        }

        if (options.isRetrieveModelContext()) {
            PrismContext prismContext = task.getTaskPrismObject().getPrismContext();
            PrismContainer<LensContextType> modelContextContainer = (PrismContainer) task.getExtensionItem(SchemaConstants.MODEL_CONTEXT_NAME);
            if (modelContextContainer != null) {
                Object value = modelContextContainer.getValue().asContainerable();
                if (value != null) {
                    if (!(value instanceof LensContextType)) {
                        throw new SystemException("Model context information in task " + task + " is of wrong type: " + value.getClass());
                    }
                    try {
                        ModelContext modelContext = modelInteractionService.unwrapModelContext((LensContextType) value);
                        modelOperationStatusDto = new ModelOperationStatusDto(modelContext);
                    } catch (SchemaException e) {
                        LoggingUtils.logException(LOGGER, "Couldn't access model operation context in task {}", e, task);
                        // todo report to result
                    }
                }
            }
        }

        // todo do this through WfTaskUtil
        PrismProperty<String> wfProcessInstanceId = task.getExtension(Constants.WFPROCESSID_PROPERTY_NAME);
        if (wfProcessInstanceId != null) {

            workflowShadowTask = true;
            workflowProcessInstanceId = wfProcessInstanceId.getRealValue();
        } else {
            workflowShadowTask = false;
        }

        PrismProperty<Boolean> finished = task.getExtension(Constants.WFPROCESS_INSTANCE_FINISHED_PROPERTY_NAME);
        workflowProcessInstanceFinished = finished != null && finished.getRealValue() == Boolean.TRUE;

        PrismProperty<String> lastDetails = task.getExtension(Constants.WFLAST_DETAILS_PROPERTY_NAME);
        if (lastDetails != null) {
            workflowLastDetails = lastDetails.getRealValue();
        }

        workflowDeltasIn = retrieveDeltasToProcess(task);
        workflowDeltasOut = retrieveResultingDeltas(task);
        workflowHistory = prepareWorkflowHistory(task);

        if (options.isGetTaskParent()) {
            Task parent = task.getParentTask(result);
            if (parent != null) {
                parentTaskName = parent.getName() != null ? parent.getName().getOrig() : "(unnamed)";       // todo i18n
                parentTaskOid = parent.getOid();
            }
        }
    }

    private List<WfHistoryEventDto> prepareWorkflowHistory(Task task) {
        List<WfHistoryEventDto> retval = new ArrayList<WfHistoryEventDto>();
        PrismProperty<String> wfStatus = task.getExtension(Constants.WFSTATUS_PROPERTY_NAME);
        if (wfStatus != null) {
            for (String entry : wfStatus.getRealValues()) {
                retval.add(new WfHistoryEventDto(entry));
            }
            Collections.sort(retval);
        }
        return retval;
    }

    private List<DeltaDto> retrieveDeltasToProcess(Task task) throws SchemaException {

        List<DeltaDto> retval = new ArrayList<DeltaDto>();

        PrismProperty<ObjectDeltaType> deltaTypePrismProperty = task.getExtension(Constants.WFDELTA_TO_PROCESS_PROPERTY_NAME);
        if (deltaTypePrismProperty != null) {
            for (ObjectDeltaType objectDeltaType : deltaTypePrismProperty.getRealValues()) {
                retval.add(new DeltaDto(DeltaConvertor.createObjectDelta(objectDeltaType, task.getTaskPrismObject().getPrismContext())));
            }
        }
        return retval;
    }

    public List<DeltaDto> retrieveResultingDeltas(Task task) throws SchemaException {

        List<DeltaDto> retval = new ArrayList<DeltaDto>();

        PrismProperty<ObjectDeltaType> deltaTypePrismProperty = task.getExtension(Constants.WFRESULTING_DELTA_PROPERTY_NAME);
        if (deltaTypePrismProperty != null) {
            for (ObjectDeltaType objectDeltaType : deltaTypePrismProperty.getRealValues()) {
                retval.add(new DeltaDto(DeltaConvertor.createObjectDelta(objectDeltaType, task.getTaskPrismObject().getPrismContext())));
            }
        }
        return retval;
    }


    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getHandlerUriList() {
        return handlerUriList;
    }

    public void setHandlerUriList(List<String> handlerUriList) {
        this.handlerUriList = handlerUriList;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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

    public ModelOperationStatusDto getModelOperationStatus() {
        return modelOperationStatusDto;
    }

    public void addChildTaskDto(TaskDto taskDto) {
        subtasks.add(taskDto);
    }

    public List<TaskDto> getSubtasks() {
        return subtasks;
    }

    public boolean isWorkflowShadowTask() {
        return workflowShadowTask;
    }

    public void setWorkflowShadowTask(boolean workflowShadowTask) {
        this.workflowShadowTask = workflowShadowTask;
    }

    public String getWorkflowProcessInstanceId() {
        return workflowProcessInstanceId;
    }

    public boolean isWorkflowProcessInstanceFinished() {
        return workflowProcessInstanceFinished;
    }

    public void setWorkflowProcessInstanceId(String workflowProcessInstanceId) {
        this.workflowProcessInstanceId = workflowProcessInstanceId;
    }

    public void setWorkflowProcessInstanceFinished(boolean workflowProcessInstanceFinished) {
        this.workflowProcessInstanceFinished = workflowProcessInstanceFinished;
    }

    public String getWorkflowLastDetails() {
        return workflowLastDetails;
    }

    public List<DeltaDto> getWorkflowDeltasIn() {
        return workflowDeltasIn;
    }

    public List<DeltaDto> getWorkflowDeltasOut() {
        return workflowDeltasOut;
    }

    public String getParentTaskName() {
        return parentTaskName;
    }

    public String getParentTaskOid() {
        return parentTaskOid;
    }

    public OperationResult getTaskOperationResult() {
        return taskOperationResult;
    }
}
