/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.SceneUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.server.handlers.HandlerDtoFactory;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.HandlerDto;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ResourceRelatedHandlerDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto.computeTriggers;

/**
 * @author lazyman
 * @author mederly
 */
public class TaskDto extends Selectable implements InlineMenuable {

    public static final String CLASS_DOT = TaskDto.class.getName() + ".";
    public static final String OPERATION_NEW = CLASS_DOT + "new";

	private static final transient Trace LOGGER = TraceManager.getTrace(TaskDto.class);
    public static final String F_MODEL_OPERATION_STATUS = "modelOperationStatus";
    public static final String F_SUBTASKS = "subtasks";
    public static final String F_NAME = "name";
    public static final String F_OID = "oid";
    public static final String F_DESCRIPTION = "description";
    public static final String F_CATEGORY = "category";
    public static final String F_PARENT_TASK_NAME = "parentTaskName";
    public static final String F_PARENT_TASK_OID = "parentTaskOid";
	public static final String F_OWNER_NAME = "ownerName";
	public static final String F_OWNER_OID = "ownerOid";
    @Deprecated public static final String F_WORKFLOW_DELTAS_IN = "workflowDeltasIn";
	@Deprecated public static final String F_WORKFLOW_DELTAS_OUT = "workflowDeltasOut";
    public static final String F_CHANGE_BEING_APPROVED = "changesBeingApproved";
    public static final String F_IDENTIFIER = "identifier";
    public static final String F_HANDLER_URI_LIST = "handlerUriList";
    public static final String F_TASK_OPERATION_RESULT = "taskOperationResult";
    public static final String F_PROGRESS_DESCRIPTION = "progressDescription";
    public static final String F_WORKER_THREADS = "workerThreads";
    public static final String F_OP_RESULT = "opResult";
	public static final String F_WORKFLOW_CONTEXT = "workflowContext";
	public static final String F_WORK_ITEMS = "workItems";
	public static final String F_WORKFLOW_REQUESTS = "workflowRequests";
	public static final String F_RECURRING = "recurring";
	public static final String F_BOUND = "bound";
	public static final String F_INTERVAL = "interval";
	public static final String F_CRON_SPECIFICATION = "cronSpecification";
	public static final String F_NOT_START_BEFORE = "notStartBefore";
	public static final String F_NOT_START_AFTER = "notStartAfter";
	public static final String F_MISFIRE_ACTION = "misfireActionType";
	public static final String F_EXECUTION_GROUP = "executionGroup";
	public static final String F_GROUP_TASK_LIMIT = "groupTaskLimit";
	public static final String F_OBJECT_REF_NAME = "objectRefName";
	public static final String F_OBJECT_TYPE = "objectType";
	public static final String F_OBJECT_QUERY = "objectQuery";
	public static final String F_OBJECT_DELTA = "objectDelta";
	public static final String F_SCRIPT = "script";
	public static final String F_EXECUTE_IN_RAW_MODE = "executeInRawMode";
	public static final String F_PROCESS_INSTANCE_ID = "processInstanceId";
	public static final String F_HANDLER_DTO = "handlerDto";
	public static final String F_IN_STAGE_BEFORE_LAST_ONE = "isInStageBeforeLastOne";
	public static final long RUNS_CONTINUALLY = -1L;
	public static final long ALREADY_PASSED = -2L;
	public static final long NOW = 0L;
	public static final String F_TRIGGERS = "triggers";

	@NotNull private final TaskType taskType;

	private TaskEditableState currentEditableState = new TaskEditableState();
	private TaskEditableState originalEditableState;

	// simple computed properties (optimization)
	private List<String> handlerUriList;
	private List<OperationResult> opResult;
	private OperationResult taskOperationResult;
	private ModelOperationStatusDto modelOperationStatusDto;

	private List<TaskChangesDto> changesCategorizationList;
	private List<ProcessInstanceDto> workflowRequests;
	private List<SceneDto> workflowDeltasIn, workflowDeltasOut;
	private TaskChangesDto changesBeingApproved;

	// related objects
	private TaskType parentTaskType;
	private ObjectTypes objectRefType;
	private String objectRefName;
	private ObjectReferenceType objectRef;

	private List<TaskDto> subtasks = new ArrayList<>();          // only persistent subtasks are here
	private List<TaskDto> transientSubtasks = new ArrayList<>();        // transient ones are here

	// other
	private List<InlineMenuItem> menuItems;
	private HandlerDto handlerDto;
	private List<EvaluatedTriggerGroupDto> triggers;            // initialized on demand

    //region Construction
    public TaskDto(@NotNull TaskType taskType, ModelService modelService, TaskService taskService, ModelInteractionService modelInteractionService,
			TaskManager taskManager, WorkflowManager workflowManager, TaskDtoProviderOptions options,
			Task opTask, OperationResult parentResult, PageBase pageBase) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
        Validate.notNull(modelService);
        Validate.notNull(taskService);
        Validate.notNull(modelInteractionService);
        Validate.notNull(taskManager);
        Validate.notNull(parentResult);
        Validate.notNull(pageBase);

        this.taskType = taskType;
		this.currentEditableState.name = taskType.getName() != null ? taskType.getName().getOrig() : null;
		this.currentEditableState.description = taskType.getDescription();

		this.currentEditableState.executionGroup = taskType.getExecutionConstraints() != null ? taskType.getExecutionConstraints().getGroup() : null;
		this.currentEditableState.groupTaskLimit = taskType.getExecutionConstraints() != null ? taskType.getExecutionConstraints().getGroupTaskLimit() : null;
		fillInScheduleAttributes(taskType);

        OperationResult thisOpResult = parentResult.createMinorSubresult(OPERATION_NEW);
        fillInHandlerUriList(taskType);
        fillInObjectRefAttributes(taskType, options, pageBase, opTask, thisOpResult);
        fillInParentTaskAttributes(taskType, taskService, options, thisOpResult);
        fillInOperationResultAttributes(taskType);
        if (options.isRetrieveModelContext()) {
            fillInModelContext(taskType, modelInteractionService, opTask, thisOpResult);
        }
		if (options.isRetrieveWorkflowContext()) {
			// TODO fill-in "cheap" wf attributes not only when this option is set
			fillInWorkflowAttributes(taskType, modelInteractionService, workflowManager, pageBase.getPrismContext(), opTask, thisOpResult);
		}
        thisOpResult.computeStatusIfUnknown();

        fillFromExtension();

        for (TaskType child : taskType.getSubtask()) {
            addChildTaskDto(new TaskDto(child, modelService, taskService, modelInteractionService, taskManager,
					workflowManager, options, opTask, parentResult, pageBase));
        }

		if (options.isCreateHandlerDto()) {
			handlerDto = HandlerDtoFactory.instance().createDtoForTask(this, pageBase, opTask, thisOpResult);
		} else {
			handlerDto = new HandlerDto(this);		// just to avoid NPEs
		}
		currentEditableState.handlerSpecificState = handlerDto.getEditableState();

		originalEditableState = currentEditableState.clone();
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        return menuItems;
    }

    private void fillFromExtension() {
        PrismProperty<Integer> workerThreadsItem = getExtensionProperty(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
        if (workerThreadsItem != null) {
            currentEditableState.workerThreads = workerThreadsItem.getRealValue();
        }
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    private void fillInHandlerUriList(TaskType taskType) {
        handlerUriList = new ArrayList<>();
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
		this.currentEditableState.recurring = taskType.getRecurrence() == TaskRecurrenceType.RECURRING;
		this.currentEditableState.bound = taskType.getBinding() == TaskBindingType.TIGHT;
		this.currentEditableState.threadStopActionType = taskType.getThreadStopAction();
		if (taskType.getSchedule() != null) {
			currentEditableState.interval = taskType.getSchedule().getInterval();
			currentEditableState.cronSpecification = taskType.getSchedule().getCronLikePattern();
            if (taskType.getSchedule().getMisfireAction() == null){
				currentEditableState.misfireActionType = MisfireActionType.EXECUTE_IMMEDIATELY;
            } else {
				currentEditableState.misfireActionType = taskType.getSchedule().getMisfireAction();
            }
			currentEditableState.notStartBefore = MiscUtil.asDate(taskType.getSchedule().getEarliestStartTime());
			currentEditableState.notStartAfter = MiscUtil.asDate(taskType.getSchedule().getLatestStartTime());
        }
    }

    private void fillInObjectRefAttributes(TaskType taskType, TaskDtoProviderOptions options, PageBase pageBase, Task opTask, OperationResult thisOpResult) {
        if (taskType.getObjectRef() != null) {
			if (taskType.getObjectRef().getType() != null) {
				this.objectRefType = ObjectTypes.getObjectTypeFromTypeQName(taskType.getObjectRef().getType());
			}
			if (options.isResolveObjectRef()) {
				this.objectRefName = getTaskObjectName(taskType, pageBase, opTask, thisOpResult);
			}
			this.objectRef = taskType.getObjectRef();
		}
    }

    public String getTaskObjectName(TaskType taskType, PageBase pageBase, Task opTask, OperationResult thisOpResult) {
		return WebModelServiceUtils.resolveReferenceName(taskType.getObjectRef(), pageBase, opTask, thisOpResult);
    }

    private void fillInParentTaskAttributes(TaskType taskType, TaskService taskService, TaskDtoProviderOptions options, OperationResult thisOpResult) {
        if (options.isGetTaskParent() && taskType.getParent() != null) {
            try {
				Collection<SelectorOptions<GetOperationOptions>> getOptions =
						options.isRetrieveSiblings() ? createCollection(TaskType.F_SUBTASK, createRetrieve()) : null;
                parentTaskType = taskService.getTaskByIdentifier(taskType.getParent(), getOptions, thisOpResult).asObjectable();
            } catch (SchemaException|ObjectNotFoundException|SecurityViolationException|ConfigurationException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve parent task for task {}", e, taskType.getOid());
            }
        }
    }

    private void fillInOperationResultAttributes(TaskType taskType) throws SchemaException {
        opResult = new ArrayList<OperationResult>();
        if (taskType.getResult() != null) {
            taskOperationResult = OperationResult.createOperationResult(taskType.getResult());
            opResult.add(taskOperationResult);
            opResult.addAll(taskOperationResult.getSubresults());
        }
    }

    private void fillInModelContext(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask, OperationResult result) throws ObjectNotFoundException {
        ModelContext ctx = unwrapModelContext(taskType, modelInteractionService, opTask, result);
		if (ctx != null) {
			modelOperationStatusDto = new ModelOperationStatusDto(ctx, modelInteractionService, opTask, result);
		}
    }

	private ModelContext unwrapModelContext(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask, OperationResult result) throws ObjectNotFoundException {
		LensContextType lensContextType = taskType.getModelOperationContext();
		if (lensContextType != null) {
			try {
				return modelInteractionService.unwrapModelContext(lensContextType, opTask, result);
			} catch (SchemaException | CommunicationException | ConfigurationException|ExpressionEvaluationException e) {   // todo treat appropriately
				throw new SystemException("Couldn't access model operation context in task: " + e.getMessage(), e);
			}
		} else {
			return null;
		}
	}

    private void fillInWorkflowAttributes(TaskType taskType, ModelInteractionService modelInteractionService, WorkflowManager workflowManager,
			PrismContext prismContext, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

        workflowDeltasIn = retrieveDeltasToProcess(taskType, modelInteractionService, opTask, thisOpResult);
		workflowDeltasOut = retrieveResultingDeltas(taskType, modelInteractionService, opTask, thisOpResult);

		final TaskType rootTask;
		if (parentTaskType == null) {
			rootTask = taskType;
		} else {
			rootTask = parentTaskType;
		}

		WfContextType wfc = taskType.getWorkflowContext();
		if (wfc != null && parentTaskType != null && (wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
			ChangesByState changesByState = workflowManager.getChangesByState(taskType, rootTask, modelInteractionService, prismContext, thisOpResult);
			List<TaskChangesDto> changeCategories = computeChangesCategorizationList(changesByState, modelInteractionService, prismContext, opTask, thisOpResult);
			if (changeCategories.size() > 1) {
				throw new IllegalStateException("More than one task change category for task " + taskType + ": " + changeCategories);
			} else if (changeCategories.size() == 1) {
				changesBeingApproved = changeCategories.get(0);
			}
		}

		workflowRequests = new ArrayList<>();
		for (TaskType wfSubtask : rootTask.getSubtask()) {
			final WfContextType subWfc = wfSubtask.getWorkflowContext();
			if (subWfc != null && subWfc.getProcessInstanceId() != null) {
				if (this.getOid() == null || !this.getOid().equals(wfSubtask.getOid())) {
					workflowRequests.add(new ProcessInstanceDto(wfSubtask));
				}
			}
		}

		ChangesByState changesByState = workflowManager.getChangesByState(rootTask, modelInteractionService, prismContext, opTask, thisOpResult);
		this.changesCategorizationList = computeChangesCategorizationList(changesByState, modelInteractionService, prismContext, opTask, thisOpResult);
	}

	@NotNull
	private List<TaskChangesDto> computeChangesCategorizationList(ChangesByState changesByState, ModelInteractionService modelInteractionService,
			PrismContext prismContext, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
		List<TaskChangesDto> changes = new ArrayList<>();
		if (!changesByState.getApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesApplied", "box-solid box-success", changesByState.getApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getBeingApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesBeingApplied", "box-solid box-info", changesByState.getBeingApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getWaitingToBeApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesWaitingToBeApplied", "box-solid box-warning", changesByState.getWaitingToBeApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getWaitingToBeApproved().isEmpty()) {
			changes.add(createChangesToBeApproved(changesByState.getWaitingToBeApproved(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getRejected().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesRejected", "box-solid box-danger", changesByState.getRejected(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getCanceled().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesCanceled", "box-solid box-danger", changesByState.getCanceled(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		return changes;
	}

	public static TaskChangesDto createChangesToBeApproved(ObjectTreeDeltas<?> deltas, ModelInteractionService modelInteractionService,
			PrismContext prismContext, Task opTask, OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
		return createTaskChangesDto("TaskDto.changesWaitingToBeApproved", "box-solid box-primary", deltas, modelInteractionService, prismContext, opTask, thisOpResult);
	}

	private static TaskChangesDto createTaskChangesDto(String titleKey, String boxClassOverride, ObjectTreeDeltas deltas, ModelInteractionService modelInteractionService,
			PrismContext prismContext, Task opTask, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
		Scene scene = SceneUtil.visualizeObjectTreeDeltas(deltasType, titleKey, prismContext, modelInteractionService, opTask, result);
		SceneDto sceneDto = new SceneDto(scene);
		sceneDto.setBoxClassOverride(boxClassOverride);
		return new TaskChangesDto(sceneDto);
	}

	private List<SceneDto> retrieveDeltasToProcess(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
        WfContextType wfc = taskType.getWorkflowContext();
        if (wfc == null || !(wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
            return null;
        }
        WfPrimaryChangeProcessorStateType pcps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
        return objectTreeDeltasToDeltaDtoList(pcps.getDeltasToProcess(), taskType.asPrismObject().getPrismContext(), modelInteractionService, opTask, thisOpResult);
    }

	private SceneDto retrieveDeltaToProcess(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
		WfContextType wfc = taskType.getWorkflowContext();
		if (wfc == null || !(wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
			return null;
		}
		WfPrimaryChangeProcessorStateType pcps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
		Scene scene = SceneUtil.visualizeObjectTreeDeltas(pcps.getDeltasToProcess(), "", taskType.asPrismObject().getPrismContext(),
				modelInteractionService, opTask, thisOpResult);
		return new SceneDto(scene);
	}

    private List<SceneDto> objectTreeDeltasToDeltaDtoList(ObjectTreeDeltasType deltas, PrismContext prismContext,
			ModelInteractionService modelInteractionService, Task opTask, OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
        List<SceneDto> retval = new ArrayList<>();
		if (deltas == null) {
			return retval;
		}
		Scene wrapperScene = SceneUtil.visualizeObjectTreeDeltas(deltas, "", prismContext, modelInteractionService, opTask, thisOpResult);
		for (Scene scene : wrapperScene.getPartialScenes()) {
			retval.add(new SceneDto(scene));
		}
        return retval;
    }

    private List<SceneDto> retrieveResultingDeltas(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ExpressionEvaluationException {
        WfContextType wfc = taskType.getWorkflowContext();
        if (wfc == null || !(wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
            return null;
        }
        WfPrimaryChangeProcessorStateType pcps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
        return objectTreeDeltasToDeltaDtoList(pcps.getResultingDeltas(), taskType.asPrismObject().getPrismContext(), modelInteractionService, opTask,
				thisOpResult);
    }

    //endregion

	//region Getters and setters for read-write properties

	public String getName() {
		return currentEditableState.name;
	}

	public void setName(String name) {
		this.currentEditableState.name = name;
	}

	public String getDescription() {
		return currentEditableState.description;
	}

	public void setDescription(String description) {
		this.currentEditableState.description = description;
	}

	public boolean isRecurring() {
		return currentEditableState.recurring;
	}

	public void setRecurring(boolean recurring) {
		this.currentEditableState.recurring = recurring;
	}

	public boolean isBound() {
		return currentEditableState.bound;
	}

	public void setBound(boolean bound) {
		this.currentEditableState.bound = bound;
	}

	public Integer getInterval() {
		return currentEditableState.interval;
	}

	public void setInterval(Integer interval) {
		this.currentEditableState.interval = interval;
	}

	public String getCronSpecification() {
		return currentEditableState.cronSpecification;
	}

	public void setCronSpecification(String cronSpecification) {
		this.currentEditableState.cronSpecification = cronSpecification;
	}

	public Date getNotStartBefore() {
		return currentEditableState.notStartBefore;
	}

	public void setNotStartBefore(Date notStartBefore) {
		this.currentEditableState.notStartBefore = notStartBefore;
	}

	public Date getNotStartAfter() {
		return currentEditableState.notStartAfter;
	}

	public void setNotStartAfter(Date notStartAfter) {
		this.currentEditableState.notStartAfter = notStartAfter;
	}

	public MisfireActionType getMisfireActionType() {
		return currentEditableState.misfireActionType;
	}

	public void setMisfireActionType(MisfireActionType misfireActionType) {
		this.currentEditableState.misfireActionType = misfireActionType;
	}

	public String getExecutionGroup() {
    	return currentEditableState.executionGroup;
	}

	public void setExecutionGroup(String value) {
    	this.currentEditableState.executionGroup = value;
	}

	public Integer getGroupTaskLimit() {
    	return currentEditableState.groupTaskLimit;
	}

	public void setGroupTaskLimit(Integer value) {
    	this.currentEditableState.groupTaskLimit = value;
	}

	public ThreadStopActionType getThreadStopActionType() {
		return currentEditableState.threadStopActionType;
	}

	public void setThreadStopActionType(ThreadStopActionType threadStopActionType) {
		this.currentEditableState.threadStopActionType = threadStopActionType;
	}

	public String getObjectRefName() {
		return objectRefName;
	}

	public ObjectTypes getObjectRefType() {
		return objectRefType;
	}

	public ObjectReferenceType getObjectRef() {
		return objectRef;
	}

	// should contain the name
	public void setObjectRef(@Nullable ObjectReferenceType objectRef) {
		this.objectRef = objectRef;
		if (objectRef != null) {
			this.objectRefName = PolyString.getOrig(objectRef.getTargetName());
			this.objectRefType = ObjectTypes.getObjectTypeFromTypeQName(objectRef.getType());
		} else {
			this.objectRefName = null;
			this.objectRefType = null;
		}
	}

	public Integer getWorkerThreads() { return currentEditableState.workerThreads; }

	public void setWorkerThreads(Integer workerThreads) {
		this.currentEditableState.workerThreads = workerThreads;
	}

	//endregion

    //region Getters for read-only properties
    public String getCategory() {
        return taskType.getCategory();
    }

    public List<String> getHandlerUriList() {
        return handlerUriList;
    }

	public Long getCurrentRuntime() {
        if (isRunNotFinished()) {
            if (isAliveClusterwide()) {
                return System.currentTimeMillis() - getLastRunStartTimestampLong();
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

    public Long getProgress() {
        return taskType.getProgress();
    }

    public Long getExpectedTotal() {
        return taskType.getExpectedTotal();
    }

    public String getProgressDescription() {
        return getProgressDescription(taskType.getProgress());
    }

    public String getProgressDescription(Long currentProgress) {
        if (currentProgress == null && taskType.getExpectedTotal() == null) {
            return "";      // the task handler probably does not report progress at all
        } else {
            StringBuilder sb = new StringBuilder();
            if (currentProgress != null){
                sb.append(currentProgress);
            } else {
                sb.append("0");
            }
            if (taskType.getExpectedTotal() != null) {
                sb.append("/").append(taskType.getExpectedTotal());
            }
            return sb.toString();
        }
    }

    public List<OperationResult> getResult() {
		return opResult;
	}

    public Long getLastRunStartTimestampLong() {
		return xgc2long(taskType.getLastRunStartTimestamp());
	}

	public Long getLastRunFinishTimestampLong() {
		return xgc2long(taskType.getLastRunFinishTimestamp());
	}

    public String getOid() {
        return taskType.getOid();
    }

    public String getIdentifier() {
        return taskType.getTaskIdentifier();
    }

    public Long getNextRunStartTimeLong() {
        return xgc2long(taskType.getNextRunStartTimestamp());
    }

	public Long getNextRetryTimeLong() {
		return xgc2long(taskType.getNextRetryTimestamp());
	}

	public Long getRetryAfter() {
    	Long retryAt = getNextRetryTimeLong();
    	return retryAt != null ? retryAt - System.currentTimeMillis() : null;
    }

    public Long getScheduledToStartAgain() {
        long current = System.currentTimeMillis();

        if (getExecution() == TaskDtoExecutionStatus.RUNNING) {
            if (!currentEditableState.recurring) {
                return null;
            } else if (currentEditableState.bound) {
                return RUNS_CONTINUALLY;             // runs continually; todo provide some information also in this case
            }
        }

		Long nextRunStartTimeLong = getNextRunStartTimeLong();
        if (nextRunStartTimeLong == null || nextRunStartTimeLong == 0) {
            return null;
        }

        if (nextRunStartTimeLong > current + 1000) {
            return nextRunStartTimeLong - System.currentTimeMillis();
        } else if (nextRunStartTimeLong < current - 60000) {
            return ALREADY_PASSED;
        } else {
            return NOW;
        }
    }

    public OperationResultStatus getStatus() {
        return taskOperationResult != null ? taskOperationResult.getStatus() : null;
    }

    private boolean isRunNotFinished() {
		final Long lastRunStartTimestampLong = getLastRunStartTimestampLong();
		final Long lastRunFinishTimestampLong = getLastRunFinishTimestampLong();
		return lastRunStartTimestampLong != null &&
                (lastRunFinishTimestampLong == null || lastRunStartTimestampLong > lastRunFinishTimestampLong);
    }

    private boolean isAliveClusterwide() {
        return getExecutingAt() != null;
    }

	public TaskExecutionStatus getRawExecutionStatus() {
		return TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
	}

	public List<OperationResult> getOpResult() {
		return opResult;
	}

    public Long getCompletionTimestamp() {
        return xgc2long(taskType.getCompletionTimestamp());
    }

    public ModelOperationStatusDto getModelOperationStatus() {
        return modelOperationStatusDto;
    }

	public TaskChangesDto getChangesForIndex(int index) {
		int realIndex = index-1;
		return realIndex < changesCategorizationList.size() ? changesCategorizationList.get(realIndex) : null;
	}

	public List<TaskChangesDto> getChangesCategorizationList() {
		return changesCategorizationList;
	}

	public void addChildTaskDto(TaskDto taskDto) {
        if (taskDto.getOid() != null) {
            subtasks.add(taskDto);
        } else {
            transientSubtasks.add(taskDto);
        }
    }

    public List<TaskDto> getSubtasks() {
        return subtasks;
    }

    public List<TaskDto> getTransientSubtasks() {
        return transientSubtasks;
    }

    public String getWorkflowProcessInstanceId() {
        return taskType.getWorkflowContext() != null ? taskType.getWorkflowContext().getProcessInstanceId() : null;
    }

    public boolean isWorkflowProcessInstanceFinished() {
        return taskType.getWorkflowContext() != null ?
				taskType.getWorkflowContext().getEndTimestamp() != null : false;
    }

    @Deprecated
	public List<SceneDto> getWorkflowDeltasIn() {
        return workflowDeltasIn;
    }

	public TaskChangesDto getChangesBeingApproved() {
        return changesBeingApproved;
    }

	@Deprecated
    public List<SceneDto> getWorkflowDeltasOut() {
        return workflowDeltasOut;
    }

    public String getParentTaskName() {
        return parentTaskType != null ? WebComponentUtil.getName(parentTaskType.asPrismObject()) : null;
    }

    public String getParentTaskOid() {
        return parentTaskType != null ? parentTaskType.getOid() : null;
    }

    public OperationResult getTaskOperationResult() {
        return taskOperationResult;
    }

	public PrismProperty getExtensionProperty(QName propertyName) {
		return taskType.asPrismObject().findProperty(new ItemPath(TaskType.F_EXTENSION, propertyName));
	}

	public <T> T getExtensionPropertyRealValue(QName propertyName, Class<T> clazz) {
		PrismProperty<T> property = taskType.asPrismObject().findProperty(new ItemPath(TaskType.F_EXTENSION, propertyName));
		return property != null ? property.getRealValue() : null;
	}

	public Long getStalledSince() {
		return xgc2long(taskType.getStalledSince());
	}

	@NotNull
	public TaskType getTaskType() {
		return taskType;
	}

	public WfContextType getWorkflowContext() {
		return taskType.getWorkflowContext();
	}

	public List<WorkItemDto> getWorkItems() {
		List<WorkItemDto> rv = new ArrayList<>();
		if (taskType.getWorkflowContext() != null) {
			for (WorkItemType workItemType : taskType.getWorkflowContext().getWorkItem()) {
				rv.add(new WorkItemDto(workItemType));
			}
		}
		return rv;
	}

	public List<ProcessInstanceDto> getWorkflowRequests() {
		return workflowRequests;
	}

	public String getObjectType() {
		QName type = getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, QName.class);
		return type != null ? type.getLocalPart() : null;
	}

	public String getObjectQuery() {
		QueryType queryType = getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.class);
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return prismContext.xmlSerializer().serializeAnyData(queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize query: " + e.getMessage(), e);
		}
	}

	public String getObjectDelta() {
		ObjectDeltaType objectDeltaType = getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA, ObjectDeltaType.class);
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return prismContext.xmlSerializer().serializeAnyData(objectDeltaType, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize delta: " + e.getMessage(), e);
		}
	}

	public String getProcessInstanceId() {
		WfContextType wfc = getWorkflowContext();
		return wfc != null ? wfc.getProcessInstanceId() : null;
	}

	public Boolean isExecuteInRawMode() {
		return getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OPTION_RAW, Boolean.class);
	}

	public String getRequestedBy() {
		WfContextType wfc = getWorkflowContext();
		return wfc != null ? WebComponentUtil.getName(wfc.getRequesterRef()) : null;
	}

	public Date getRequestedOn() {
		WfContextType wfc = getWorkflowContext();
		return wfc != null ? XmlTypeConverter.toDate(wfc.getStartTimestamp()) : null;
	}

	public Boolean getWorkflowOutcome() {
		WfContextType wfc = getWorkflowContext();
		return wfc != null ? ApprovalUtils.approvalBooleanValueFromUri(wfc.getOutcome()) : null;
	}

	public String getOwnerOid() {
		return taskType.getOwnerRef() != null ? taskType.getOwnerRef().getOid() : null;
	}

	public String getOwnerName() {
		return WebComponentUtil.getName(taskType.getOwnerRef());
	}

	public HandlerDto getHandlerDto() {
		return handlerDto;
	}

	//endregion

    public static List<String> getOids(List<TaskDto> taskDtoList) {
        List<String> retval = new ArrayList<>();
        for (TaskDto taskDto : taskDtoList) {
            retval.add(taskDto.getOid());
        }
        return retval;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaskDto taskDto = (TaskDto) o;

		if (taskType != null ? !taskType.equals(taskDto.taskType) : taskDto.taskType != null)
			return false;
		if (currentEditableState != null ? !currentEditableState.equals(taskDto.currentEditableState) : taskDto.currentEditableState != null)
			return false;
		if (originalEditableState != null ? !originalEditableState.equals(taskDto.originalEditableState) : taskDto.originalEditableState != null)
			return false;
		if (handlerUriList != null ? !handlerUriList.equals(taskDto.handlerUriList) : taskDto.handlerUriList != null)
			return false;
		if (opResult != null ? !opResult.equals(taskDto.opResult) : taskDto.opResult != null)
			return false;
		if (taskOperationResult != null ? !taskOperationResult.equals(taskDto.taskOperationResult) : taskDto.taskOperationResult != null)
			return false;
		if (modelOperationStatusDto != null ?
				!modelOperationStatusDto.equals(taskDto.modelOperationStatusDto) :
				taskDto.modelOperationStatusDto != null)
			return false;
		if (changesCategorizationList != null ?
				!changesCategorizationList.equals(taskDto.changesCategorizationList) :
				taskDto.changesCategorizationList != null)
			return false;
		if (workflowRequests != null ? !workflowRequests.equals(taskDto.workflowRequests) : taskDto.workflowRequests != null)
			return false;
		if (workflowDeltasIn != null ? !workflowDeltasIn.equals(taskDto.workflowDeltasIn) : taskDto.workflowDeltasIn != null)
			return false;
		if (workflowDeltasOut != null ? !workflowDeltasOut.equals(taskDto.workflowDeltasOut) : taskDto.workflowDeltasOut != null)
			return false;
		if (changesBeingApproved != null ? !changesBeingApproved.equals(taskDto.changesBeingApproved) : taskDto.changesBeingApproved != null)
			return false;
		if (parentTaskType != null ? !parentTaskType.equals(taskDto.parentTaskType) : taskDto.parentTaskType != null)
			return false;
		if (objectRefType != taskDto.objectRefType)
			return false;
		if (objectRefName != null ? !objectRefName.equals(taskDto.objectRefName) : taskDto.objectRefName != null)
			return false;
		if (objectRef != null ? !objectRef.equals(taskDto.objectRef) : taskDto.objectRef != null)
			return false;
		if (subtasks != null ? !subtasks.equals(taskDto.subtasks) : taskDto.subtasks != null)
			return false;
		if (transientSubtasks != null ? !transientSubtasks.equals(taskDto.transientSubtasks) : taskDto.transientSubtasks != null)
			return false;
		if (menuItems != null ? !menuItems.equals(taskDto.menuItems) : taskDto.menuItems != null)
			return false;
		return handlerDto != null ? handlerDto.equals(taskDto.handlerDto) : taskDto.handlerDto == null;

	}

	@Override
	public int hashCode() {
		int result = taskType != null ? taskType.hashCode() : 0;
		result = 31 * result + (currentEditableState != null ? currentEditableState.hashCode() : 0);
		result = 31 * result + (originalEditableState != null ? originalEditableState.hashCode() : 0);
		result = 31 * result + (handlerUriList != null ? handlerUriList.hashCode() : 0);
		result = 31 * result + (opResult != null ? opResult.hashCode() : 0);
		result = 31 * result + (taskOperationResult != null ? taskOperationResult.hashCode() : 0);
		result = 31 * result + (modelOperationStatusDto != null ? modelOperationStatusDto.hashCode() : 0);
		result = 31 * result + (changesCategorizationList != null ? changesCategorizationList.hashCode() : 0);
		result = 31 * result + (workflowRequests != null ? workflowRequests.hashCode() : 0);
		result = 31 * result + (workflowDeltasIn != null ? workflowDeltasIn.hashCode() : 0);
		result = 31 * result + (workflowDeltasOut != null ? workflowDeltasOut.hashCode() : 0);
		result = 31 * result + (changesBeingApproved != null ? changesBeingApproved.hashCode() : 0);
		result = 31 * result + (parentTaskType != null ? parentTaskType.hashCode() : 0);
		result = 31 * result + (objectRefType != null ? objectRefType.hashCode() : 0);
		result = 31 * result + (objectRefName != null ? objectRefName.hashCode() : 0);
		result = 31 * result + (objectRef != null ? objectRef.hashCode() : 0);
		result = 31 * result + (subtasks != null ? subtasks.hashCode() : 0);
		result = 31 * result + (transientSubtasks != null ? transientSubtasks.hashCode() : 0);
		result = 31 * result + (menuItems != null ? menuItems.hashCode() : 0);
		result = 31 * result + (handlerDto != null ? handlerDto.hashCode() : 0);
		return result;
	}

	@Override
    public String toString() {
        return "TaskDto{" +
                "taskType=" + taskType +
                '}';
    }

	public boolean isRunnableOrRunning() {
		TaskDtoExecutionStatus exec = getExecution();
		return exec == TaskDtoExecutionStatus.RUNNABLE || exec == TaskDtoExecutionStatus.RUNNING;
	}

	public boolean isRunnable() {
		return getExecution() == TaskDtoExecutionStatus.RUNNABLE;
	}

	public boolean isRunning() {
		return getExecution() == TaskDtoExecutionStatus.RUNNING;
	}

	public boolean isClosed() {
		return getExecution() == TaskDtoExecutionStatus.CLOSED;
	}

	public boolean isWaiting() {
		return getExecution() == TaskDtoExecutionStatus.WAITING;
	}

	public boolean isSuspended() {
		return getExecution() == TaskDtoExecutionStatus.SUSPENDED;
	}

	public boolean isReconciliation() {
		return TaskCategory.RECONCILIATION.equals(getCategory());
	}

	public boolean isImportAccounts() {
		return TaskCategory.IMPORTING_ACCOUNTS.equals(getCategory());
	}

	public boolean isRecomputation() {
		return TaskCategory.RECOMPUTATION.equals(getCategory());
	}

	public boolean isExecuteChanges() {
		return TaskCategory.EXECUTE_CHANGES.equals(getCategory());
	}

	public boolean isWorkflowCategory() {
		return TaskCategory.WORKFLOW.equals(getCategory());
	}

	public boolean isWorkflowChild() {
		return isWorkflowCategory() && getWorkflowContext() != null && getWorkflowContext().getProcessInstanceId() != null;
	}

	public boolean isWorkflowParent() {
		return isWorkflowCategory() && getParentTaskOid() == null;
	}

	public boolean isWorkflow() {
		return isWorkflowChild() || isWorkflowParent();		// "task0" is not among these
	}

	public boolean isLiveSync() {
		return TaskCategory.LIVE_SYNCHRONIZATION.equals(getCategory());
	}

	public boolean isShadowIntegrityCheck() {
		return getHandlerUriList().contains(ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI);
	}

	public boolean isFocusValidityScanner() {
		return getHandlerUriList().contains(ModelPublicConstants.FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI);
	}

	public boolean isJdbcPing() {
		return getHandlerUriList().contains(TaskConstants.JDBC_PING_HANDLER_URI);
	}

	public boolean isTriggerScanner() {
		return getHandlerUriList().contains(ModelPublicConstants.TRIGGER_SCANNER_TASK_HANDLER_URI);
	}

	public boolean isDelete() {
		return getHandlerUriList().contains(ModelPublicConstants.DELETE_TASK_HANDLER_URI);
	}

	public boolean isBulkAction() {
		return TaskCategory.BULK_ACTIONS.equals(getCategory());
	}

	public boolean isReportCreate() {
		return TaskCategory.REPORT.equals(getCategory());
	}

	public boolean configuresWorkerThreads() {
		return isReconciliation() || isImportAccounts() || isRecomputation() || isExecuteChanges() || isShadowIntegrityCheck() || isFocusValidityScanner() || isTriggerScanner();
	}

	public boolean configuresWorkToDo() {
		return isLiveSync() || isReconciliation() || isImportAccounts() || isRecomputation() || isExecuteChanges() || isBulkAction() || isDelete() || isShadowIntegrityCheck();
	}

	public boolean configuresResourceCoordinates() {
		return isLiveSync() || isReconciliation() || isImportAccounts();
	}

	public boolean configuresObjectType() {
		return isRecomputation() || isExecuteChanges() || isDelete();
	}

	public boolean configuresObjectQuery() {
		return isRecomputation() || isExecuteChanges() || isDelete() || isShadowIntegrityCheck();
	}

	public boolean configuresObjectDelta() {
		return isExecuteChanges();
	}

	public boolean configuresScript() {
		return isBulkAction();
	}

	public boolean configuresDryRun() {
		return isLiveSync() || isReconciliation() || isImportAccounts() || isShadowIntegrityCheck();
	}

	public boolean configuresExecuteInRawMode() {
		return isExecuteChanges();
	}

	// quite a hack (TODO think about this)
	public boolean isDryRun() {
		if (handlerDto instanceof ResourceRelatedHandlerDto) {
			return ((ResourceRelatedHandlerDto) handlerDto).isDryRun();
		} else {
			return false;
		}
	}

	public boolean isCleanup() {
		return ModelPublicConstants.CLEANUP_TASK_HANDLER_URI.equals(taskType.getHandlerUri());
	}

	public boolean isNoOp() {		// temporary implementation
		return TaskCategory.DEMO.equals(getCategory());
	}

	public TaskEditableState getCurrentEditableState() {
		return currentEditableState;
	}

	public TaskEditableState getOriginalEditableState() {
		return originalEditableState;
	}

	public boolean isInStageBeforeLastOne() {
		return WfContextUtil.isInStageBeforeLastOne(getWorkflowContext());
	}

	public String getAllowedNodes(List<NodeType> nodes) {
		Map<String, Integer> restrictions = TaskManagerUtil.getNodeRestrictions(getExecutionGroup(), nodes);
		String n = restrictions.entrySet().stream()
				.filter(e -> e.getValue() == null || e.getValue() > 0)
				.map(e -> e.getKey() + (e.getValue() != null ? " (" + e.getValue() + ")" : ""))
				.collect(Collectors.joining(", "));
		return n.isEmpty() ? "-" : n;
	}

	public List<EvaluatedTriggerGroupDto> getTriggers() {
		if (triggers == null) {
			triggers = computeTriggers(getWorkflowContext());
		}
		return triggers;
	}

}
