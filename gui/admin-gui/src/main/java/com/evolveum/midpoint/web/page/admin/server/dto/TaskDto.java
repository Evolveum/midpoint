/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.wf.WfHistoryEventDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.wf.api.WfTaskExtensionItemsNames;
import com.evolveum.midpoint.wf.processors.primary.PcpTaskExtensionItemsNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.Validate;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
    public static final String F_DESCRIPTION = "description";
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
    public static final String F_PROGRESS_DESCRIPTION = "progressDescription";
    public static final String F_DRY_RUN = "dryRun";
    public static final String F_RESOURCE_REFERENCE = "resourceRef";

    private List<String> handlerUriList;
    private String parentTaskName;
    private String parentTaskOid;

    //resource reference
    private TaskAddResourcesDto resourceRef;

    // scheduling information
    private Integer interval;
    private String cronSpecification;
    private Date notStartBefore;
    private Date notStartAfter;
    private MisfireActionType misfireAction;

    private List<OperationResult> opResult;
    private OperationResult taskOperationResult;

    private ModelOperationStatusDto modelOperationStatusDto;

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

    private boolean dryRun;

    private TaskType taskType;

    //region Construction
    public TaskDto(TaskType taskType, ModelService modelService, TaskService taskService, ModelInteractionService modelInteractionService, TaskManager taskManager, TaskDtoProviderOptions options, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        Validate.notNull(taskType, "Task must not be null.");
        Validate.notNull(modelService);
        Validate.notNull(taskService);
        Validate.notNull(modelInteractionService);
        Validate.notNull(taskManager);
        Validate.notNull(parentResult);

        this.taskType = taskType;

        OperationResult thisOpResult = parentResult.createMinorSubresult(OPERATION_NEW);
        fillInTimestamps(taskType);
        fillInHandlerUriList(taskType);
        fillInScheduleAttributes(taskType);
        fillInResourceReference(taskType, taskManager, thisOpResult, modelService);
        fillInObjectRefAttributes(taskType, modelService, taskManager, options, thisOpResult);
        fillInParentTaskAttributes(taskType, taskService, options, thisOpResult);
        fillInOperationResultAttributes(taskType);
        if (options.isRetrieveModelContext()) {
            fillInModelContext(taskType, modelInteractionService, thisOpResult);
        }
        fillInWorkflowAttributes(taskType);
        thisOpResult.computeStatusIfUnknown();

        fillDryRun(taskType);
    }

    private void fillInResourceReference(TaskType task, TaskManager manager, OperationResult result, ModelService service){
        ObjectReferenceType ref = task.getObjectRef();

        if(ref != null && ResourceType.COMPLEX_TYPE.equals(ref.getType())){
            resourceRef = new TaskAddResourcesDto(ref.getOid(), getTaskObjectName(task, manager, service, result));
        }
    }

    private void fillDryRun(TaskType taskType) {
        PrismObject<TaskType> task = taskType.asPrismObject();
        if (task.getExtension() == null) {
            dryRun = false;
            return;
        }

        PrismProperty<Boolean> item = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
        if (item == null || item.getRealValue() == null) {
            dryRun = false;
            return;
        }

        dryRun = item.getRealValue();
    }

    private void fillInTimestamps(TaskType taskType) {
        lastRunFinishTimestampLong = xgc2long(taskType.getLastRunFinishTimestamp());
        lastRunStartTimestampLong = xgc2long(taskType.getLastRunStartTimestamp());
        completionTimestampLong = xgc2long(taskType.getCompletionTimestamp());
        nextRunStartTimeLong = xgc2long(taskType.getNextRunStartTimestamp());
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? new Long(XmlTypeConverter.toMillis(gc)) : null;
    }


    private void fillInHandlerUriList(TaskType taskType) {
        handlerUriList = new ArrayList<String>();
        if (taskType.getHandlerUri() != null) {
            handlerUriList.add(taskType.getHandlerUri());
        } else {
            handlerUriList.add("-");        // todo separate presentation from model
        }
        if (taskType.getOtherHandlersUriStack() != null) {
            List<UriStackEntry> stack = taskType.getOtherHandlersUriStack().getUriStackEntry();
            for (int i = stack.size()-1; i >= 0; i--) {
                handlerUriList.add(stack.get(i).getHandlerUri());
            }
        }
    }

    private void fillInScheduleAttributes(TaskType taskType) {
        if (taskType.getSchedule() != null){
            interval = taskType.getSchedule().getInterval();
            cronSpecification = taskType.getSchedule().getCronLikePattern();
            if (taskType.getSchedule().getMisfireAction() == null){
                misfireAction = MisfireActionType.EXECUTE_IMMEDIATELY;
            } else {
                misfireAction = taskType.getSchedule().getMisfireAction();
            }
            notStartBefore = MiscUtil.asDate(taskType.getSchedule().getEarliestStartTime());
            notStartAfter = MiscUtil.asDate(taskType.getSchedule().getLatestStartTime());
        }
    }

    private void fillInObjectRefAttributes(TaskType taskType, ModelService modelService, TaskManager taskManager, TaskDtoProviderOptions options, OperationResult thisOpResult) {
        if (taskType.getObjectRef() != null) {
            if (taskType.getObjectRef().getType() != null) {
                this.objectRefType = ObjectTypes.getObjectTypeFromTypeQName(taskType.getObjectRef().getType());
            }
            if (options.isResolveObjectRef()) {
                this.objectRefName = getTaskObjectName(taskType, taskManager, modelService, thisOpResult);
            }
        }
    }

    private String getTaskObjectName(TaskType taskType, TaskManager taskManager, ModelService modelService, OperationResult thisOpResult) {
        PrismReference objectRef = taskType.asPrismObject().findReference(TaskType.F_OBJECT_REF);
        if (objectRef == null) {
            return null;
        }
        PrismObject<ObjectType> object = null;
        if (objectRef.getValue().getObject() != null) {
            object = objectRef.getValue().getObject();
        } else {
            Throwable failReason = null;
            try {
//                Class compileTimeClass = objectRef.getValue().getTargetTypeCompileTimeClass();
//                if (compileTimeClass == null) {
//                    return "(" + objectRef.getOid() + ")";
//                }
                // todo optimize to retrieve name only (something like GetOperationOptions.createRetrieveNameOnlyOptions() if it would work)
                // raw is here because otherwise, if we would try to get a Resource in non-raw mode as ObjectType, we would get illegal state exception in model
                object = modelService.getObject(ObjectType.class, objectRef.getOid(), SelectorOptions.createCollection(GetOperationOptions.createRaw()), taskManager.createTaskInstance(), thisOpResult);
            } catch (ObjectNotFoundException e) {
                failReason = e;
            } catch (SchemaException e) {
                failReason = e;
            } catch (SecurityViolationException e) {
                failReason = e;
            } catch (CommunicationException e) {
                failReason = e;
            } catch (ConfigurationException e) {
                failReason = e;
            }
            if (failReason != null) {
                String message = "Couldn't get the name of referenced object with OID " + objectRef.getOid() + " for task " + taskType.getOid();
                LoggingUtils.logException(LOGGER, message, failReason);
                thisOpResult.recordWarning(message, failReason);
                return null;
            }
        }
        return WebMiscUtil.getName(object);
    }

    private void fillInParentTaskAttributes(TaskType taskType, TaskService taskService, TaskDtoProviderOptions options, OperationResult thisOpResult) {
        if (options.isGetTaskParent() && taskType.getParent() != null) {
            Throwable failReason = null;
            try {
                //TaskType parentTaskType = taskService.getTaskByIdentifier(taskType.getParent(), GetOperationOptions.createRetrieveNameOnlyOptions(), thisOpResult).asObjectable();
                TaskType parentTaskType = taskService.getTaskByIdentifier(taskType.getParent(), null, thisOpResult).asObjectable();
                if (parentTaskType != null) {
                    parentTaskName = parentTaskType.getName() != null ? parentTaskType.getName().getOrig() : "(unnamed)";       // todo i18n
                    parentTaskOid = parentTaskType.getOid();
                }
            } catch (SchemaException e) {
                failReason = e;
            } catch (ObjectNotFoundException e) {
                failReason = e;
            }
            if (failReason != null) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve parent task for task {}", failReason, taskType.getOid());
            }
        }
    }

    private void fillInOperationResultAttributes(TaskType taskType) {
        opResult = new ArrayList<OperationResult>();
        if (taskType.getResult() != null) {
            taskOperationResult = OperationResult.createOperationResult(taskType.getResult());
            opResult.add(taskOperationResult);
            opResult.addAll(taskOperationResult.getSubresults());
        }
    }

    private void fillInModelContext(TaskType taskType, ModelInteractionService modelInteractionService, OperationResult result) throws ObjectNotFoundException {
        PrismContainer<LensContextType> modelContextContainer =
                (PrismContainer) taskType.asPrismObject().findItem(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_CONTEXT_NAME));
        if (modelContextContainer != null) {
            Object value = modelContextContainer.getValue().asContainerable();
            if (value != null) {
                if (!(value instanceof LensContextType)) {
                    throw new SystemException("Model context information in task " + taskType + " is of wrong type: " + value.getClass());
                }
                try {
                    ModelContext modelContext = modelInteractionService.unwrapModelContext((LensContextType) value, result);
                    modelOperationStatusDto = new ModelOperationStatusDto(modelContext);
                } catch (SchemaException e) {   // todo report to result
                    LoggingUtils.logException(LOGGER, "Couldn't access model operation context in task {}", e, WebMiscUtil.getIdentification(taskType));
                } catch (CommunicationException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't access model operation context in task {}", e, WebMiscUtil.getIdentification(taskType));
                } catch (ConfigurationException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't access model operation context in task {}", e, WebMiscUtil.getIdentification(taskType));
                }
            }
        }
    }

    private void fillInWorkflowAttributes(TaskType taskType) throws SchemaException {
        // todo do this through WfTaskUtil
        PrismProperty<String> wfProcessInstanceId = getExtensionProperty(taskType, WfTaskExtensionItemsNames.WFPROCESSID_PROPERTY_NAME);
        if (wfProcessInstanceId != null) {
            workflowShadowTask = true;
            workflowProcessInstanceId = wfProcessInstanceId.getRealValue();
        } else {
            workflowShadowTask = false;
        }

        PrismProperty<Boolean> finished = getExtensionProperty(taskType, WfTaskExtensionItemsNames.WFPROCESS_INSTANCE_FINISHED_PROPERTY_NAME);
        workflowProcessInstanceFinished = finished != null && Boolean.TRUE.equals(finished.getRealValue());

        PrismProperty<String> lastDetails = getExtensionProperty(taskType, WfTaskExtensionItemsNames.WFLAST_DETAILS_PROPERTY_NAME);
        if (lastDetails != null) {
            workflowLastDetails = lastDetails.getRealValue();
        }

        workflowDeltasIn = retrieveDeltasToProcess(taskType);
        workflowDeltasOut = retrieveResultingDeltas(taskType);
        workflowHistory = prepareWorkflowHistory(taskType);
    }

    private List<DeltaDto> retrieveDeltasToProcess(TaskType taskType) throws SchemaException {
        List<DeltaDto> retval = new ArrayList<DeltaDto>();
        PrismProperty<ObjectDeltaType> deltaTypePrismProperty = getExtensionProperty(taskType, PcpTaskExtensionItemsNames.WFDELTA_TO_PROCESS_PROPERTY_NAME);
        if (deltaTypePrismProperty != null) {
            for (ObjectDeltaType objectDeltaType : deltaTypePrismProperty.getRealValues()) {
                retval.add(new DeltaDto(DeltaConvertor.createObjectDelta(objectDeltaType, taskType.asPrismObject().getPrismContext())));
            }
        }
        return retval;
    }

    public List<DeltaDto> retrieveResultingDeltas(TaskType taskType) throws SchemaException {
        List<DeltaDto> retval = new ArrayList<DeltaDto>();
        PrismProperty<ObjectDeltaType> deltaTypePrismProperty = getExtensionProperty(taskType, PcpTaskExtensionItemsNames.WFRESULTING_DELTA_PROPERTY_NAME);
        if (deltaTypePrismProperty != null) {
            for (ObjectDeltaType objectDeltaType : deltaTypePrismProperty.getRealValues()) {
                retval.add(new DeltaDto(DeltaConvertor.createObjectDelta(objectDeltaType, taskType.asPrismObject().getPrismContext())));
            }
        }
        return retval;
    }

    private List<WfHistoryEventDto> prepareWorkflowHistory(TaskType taskType) {
        List<WfHistoryEventDto> retval = new ArrayList<WfHistoryEventDto>();
        PrismProperty<String> wfStatus = getExtensionProperty(taskType, WfTaskExtensionItemsNames.WFSTATUS_PROPERTY_NAME);
        if (wfStatus != null) {
            for (String entry : wfStatus.getRealValues()) {
                retval.add(new WfHistoryEventDto(entry));
            }
            Collections.sort(retval);
        }
        return retval;
    }
    //endregion

    //region Getters
    public String getCategory() {
        return taskType.getCategory();
    }
    
    public List<String> getHandlerUriList() {
        return handlerUriList;
    }

    public boolean getBound() {
		return taskType.getBinding() == TaskBindingType.TIGHT;
	}

    public void setBound(boolean value) {
        if (value) {
            taskType.setBinding(TaskBindingType.TIGHT);
        } else {
            taskType.setBinding(TaskBindingType.LOOSE);
        }
    }
	
	public Integer getInterval() {
		return interval;
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
        return ThreadStopActionType.CLOSE.equals(taskType.getThreadStopAction()) || ThreadStopActionType.SUSPEND.equals(taskType.getThreadStopAction());
    }

	public ThreadStopActionType getThreadStop() {
        if (taskType.getThreadStopAction() == null){
            return ThreadStopActionType.RESTART;
        } else {
            return taskType.getThreadStopAction();
        }
    }

	public boolean getRecurring() {
		return taskType.getRecurrence() == TaskRecurrenceType.RECURRING;
	}

    public void setRecurring(boolean value) {
        if (value) {
            taskType.setRecurrence(TaskRecurrenceType.RECURRING);
        } else {
            taskType.setRecurrence(TaskRecurrenceType.SINGLE);
        }
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
        return TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
    }

    public String getExecutingAt() {
        return taskType.getNodeAsObserved();
    }

    public String getProgressDescription() {
        if (taskType.getProgress() == null && taskType.getExpectedTotal() == null) {
            return "";      // the task handler probably does not report progress at all
        } else {
            StringBuilder sb = new StringBuilder();
            if (taskType.getProgress() != null){
                sb.append(taskType.getProgress());
            } else {
                sb.append("0");
            }
            if (taskType.getExpectedTotal() != null) {
                sb.append("/" + taskType.getExpectedTotal());
            }
            return sb.toString();
        }
    }

    public List<OperationResult> getResult() {
		return opResult;
	}

	public String getName() {
        return taskType.getName() != null ? taskType.getName().getOrig() : null;
    }
	
	public void setName(String name) {
        taskType.setName(new PolyStringType(name));
    }

    public String getDescription() {
        return taskType.getDescription();
    }

    public void setDescription(String description) {
        taskType.setDescription(description);
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
        return taskType.getObjectRef();
    }

    public String getOid() {
        return taskType.getOid();
    }
    
    public String getIdentifier() {
        return taskType.getTaskIdentifier();
    }

    public Long getNextRunStartTimeLong() {
        return nextRunStartTimeLong;
    }

    public Long getScheduledToStartAgain() {
        long current = System.currentTimeMillis();

        if (getExecution() == TaskDtoExecutionStatus.RUNNING) {
            if (!getRecurring()) {
                return null;
            } else if (getBound()) {
                return -1L;             // runs continually; todo provide some information also in this case
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
        return taskOperationResult != null ? taskOperationResult.getStatus() : null;
    }

    private boolean isRunNotFinished() {
        return lastRunStartTimestampLong != null &&
                (lastRunFinishTimestampLong == null || lastRunStartTimestampLong > lastRunFinishTimestampLong);
    }

    private boolean isAliveClusterwide() {
        return getExecutingAt() != null;
    }

	public MisfireActionType getMisfireAction() {
		return misfireAction;
	}

	public void setMisfireAction(MisfireActionType misfireAction) {
		this.misfireAction = misfireAction;
	}

	public TaskExecutionStatus getRawExecutionStatus() {
		return TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
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

    public Long getCompletionTimestamp() {
        return completionTimestampLong;
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

    public String getWorkflowProcessInstanceId() {
        return workflowProcessInstanceId;
    }

    public boolean isWorkflowProcessInstanceFinished() {
        return workflowProcessInstanceFinished;
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
    //endregion

    public static List<String> getOids(List<TaskDto> taskDtoList) {
        List<String> retval = new ArrayList<String>();
        for (TaskDto taskDto : taskDtoList) {
            retval.add(taskDto.getOid());
        }
        return retval;
    }

    private PrismProperty getExtensionProperty(TaskType taskType, QName propertyName) {
        return taskType.asPrismObject().findProperty(new ItemPath(TaskType.F_EXTENSION, propertyName));
    }

    public void setThreadStop(ThreadStopActionType value) {
        taskType.setThreadStopAction(value);
    }

    public Long getStalledSince() {
        return xgc2long(taskType.getStalledSince());
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public TaskAddResourcesDto getResource() {
        return resourceRef;
    }

    public void setResource(TaskAddResourcesDto resource) {
        this.resourceRef = resource;
    }
}
