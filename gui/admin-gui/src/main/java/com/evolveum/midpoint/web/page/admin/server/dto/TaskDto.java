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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
import com.evolveum.midpoint.web.component.wf.WfHistoryEventDto;
import com.evolveum.midpoint.web.page.admin.server.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Application;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

/**
 * @author lazyman
 */
public class TaskDto extends Selectable implements InlineMenuable {

    public static final String CLASS_DOT = TaskDto.class.getName() + ".";
    public static final String OPERATION_NEW = CLASS_DOT + "new";
    public static final String OPERATION_LOAD_RESOURCE = CLASS_DOT + "loadResource";

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
    public static final String F_WORKFLOW_DELTA_IN = "workflowDeltaIn";
    public static final String F_WORKFLOW_DELTAS_OUT = "workflowDeltasOut";
    public static final String F_IDENTIFIER = "identifier";
    public static final String F_HANDLER_URI_LIST = "handlerUriList";
    public static final String F_WORKFLOW_HISTORY = "workflowHistory";
    public static final String F_TASK_OPERATION_RESULT = "taskOperationResult";
    public static final String F_PROGRESS_DESCRIPTION = "progressDescription";
    public static final String F_DRY_RUN = "dryRun";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_WORKER_THREADS = "workerThreads";
    public static final String F_RESOURCE_REFERENCE = "resourceRef";
    public static final String F_OP_RESULT = "opResult";
	public static final String F_WORKFLOW_CONTEXT = "workflowContext";
	public static final String F_WORK_ITEMS = "workItems";
	public static final String F_WORKFLOW_REQUESTS = "workflowRequests";
	public static final String RECURRING = "recurring";
	public static final String BOUND = "bound";
	public static final String F_INTERVAL = "interval";
	public static final String CRON_SPECIFICATION = "cronSpecification";
	public static final String F_NOT_START_BEFORE = "notStartBefore";
	public static final String F_NOT_START_AFTER = "notStartAfter";
	public static final String F_MISFIRE_ACTION = "misfireAction";
	public static final String F_OBJECT_REF_NAME = "objectRefName";
	public static final String F_OBJECT_TYPE = "objectType";
	public static final String F_OBJECT_QUERY = "objectQuery";
	public static final String F_OBJECT_DELTA = "objectDelta";
	public static final String F_SCRIPT = "script";
	public static final String F_EXECUTE_IN_RAW_MODE = "executeInRawMode";
	public static final String F_PROCESS_INSTANCE_ID = "processInstanceId";
	public static final String F_CHANGES_1 = "changes1";

	private List<InlineMenuItem> menuItems;

    private List<String> handlerUriList;
	private TaskType parentTaskType;
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
    private List<TaskChangesDto> changes;

    private ObjectTypes objectRefType;
    private String objectRefName;

    private List<TaskDto> subtasks = new ArrayList<>();          // only persistent subtasks are here
    private List<TaskDto> transientSubtasks = new ArrayList<>();        // transient ones are here

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
	private List<ProcessInstanceDto> workflowRequests;
	private String requestedBy;
	private Date requestedOn;

	private List<SceneDto> workflowDeltasIn, workflowDeltasOut;
    private List<WfHistoryEventDto> workflowHistory;

	private SceneDto workflowDeltaIn;

    private boolean dryRun;
    private ShadowKindType kind;
    private String intent;
    private String objectClass;
    private List<QName> objectClassList;

    private Integer workerThreads;

    private TaskType taskType;

    //region Construction
    public TaskDto(TaskType taskType, ModelService modelService, TaskService taskService, ModelInteractionService modelInteractionService,
			TaskManager taskManager, WorkflowManager workflowManager, TaskDtoProviderOptions options,
			Task opTask, OperationResult parentResult, PageBase pageBase) throws SchemaException, ObjectNotFoundException {
        Validate.notNull(taskType, "Task must not be null.");
        Validate.notNull(modelService);
        Validate.notNull(taskService);
        Validate.notNull(modelInteractionService);
        Validate.notNull(taskManager);
        Validate.notNull(parentResult);
        Validate.notNull(pageBase);

        this.taskType = taskType;

        OperationResult thisOpResult = parentResult.createMinorSubresult(OPERATION_NEW);
        fillInTimestamps(taskType);
        fillInHandlerUriList(taskType);
        fillInScheduleAttributes(taskType);
        fillInResourceReference(taskType, taskManager, thisOpResult, modelService, pageBase);
        fillInObjectRefAttributes(taskType, modelService, taskManager, options, thisOpResult);
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

        //dryRun, intent, kind, objectCLass, workerThreads
        fillFromExtension(taskType);

        for (TaskType child : taskType.getSubtask()) {
            addChildTaskDto(new TaskDto(child, modelService, taskService, modelInteractionService, taskManager,
					workflowManager, options, opTask, parentResult, pageBase));
        }
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        return menuItems;
    }

    private void fillInResourceReference(TaskType task, TaskManager manager, OperationResult result, ModelService service, PageBase pageBase){
        ObjectReferenceType ref = task.getObjectRef();

        if(ref != null && ResourceType.COMPLEX_TYPE.equals(ref.getType())){
            resourceRef = new TaskAddResourcesDto(ref.getOid(), getTaskObjectName(task, manager, service, result));
        }

        updateObjectClassList(pageBase);
    }

    private void updateObjectClassList(PageBase pageBase){
    	Task task = pageBase.createSimpleTask(OPERATION_LOAD_RESOURCE);
        OperationResult result = task.getResult();
        List<QName> objectClassList = new ArrayList<>();

        if(resourceRef != null){
            PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceRef.getOid(), 
            		pageBase, task, result);

            try {
                ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, pageBase.getPrismContext());
                schema.getObjectClassDefinitions();

                for(Definition def: schema.getDefinitions()){
                    objectClassList.add(def.getTypeName());
                }

                setObjectClassList(objectClassList);
            } catch (Exception e){
                LoggingUtils.logException(LOGGER, "Couldn't load object class list from resource.", e);
            }
        }
    }

    private void fillFromExtension(TaskType taskType) {
        PrismObject<TaskType> task = taskType.asPrismObject();
        if (task.getExtension() == null) {
            dryRun = false;
            return;
        }

        PrismProperty<Boolean> item = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
        if (item == null || item.getRealValue() == null) {
            dryRun = false;
        } else {
            dryRun = item.getRealValue();
        }

        PrismProperty<ShadowKindType> kindItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_KIND);
        if(kindItem != null && kindItem.getRealValue() != null){
            kind = kindItem.getRealValue();
        }

        PrismProperty<String> intentItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
        if(intentItem != null && intentItem.getRealValue() != null){
            intent = intentItem.getRealValue();
        }

        PrismProperty<QName> objectClassItem = task.getExtension().findProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
        if(objectClassItem != null && objectClassItem.getRealValue() != null){
            objectClass = objectClassItem.getRealValue().getLocalPart();
        }

        PrismProperty<Integer> workerThreadsItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
        if(workerThreadsItem != null) {
            workerThreads = workerThreadsItem.getRealValue();
        }
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
                object = modelService.getObject(ObjectType.class, objectRef.getOid(), createCollection(GetOperationOptions.createRaw()), taskManager.createTaskInstance(), thisOpResult);
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
        return WebComponentUtil.getName(object);
    }

    private void fillInParentTaskAttributes(TaskType taskType, TaskService taskService, TaskDtoProviderOptions options, OperationResult thisOpResult) {
        if (options.isGetTaskParent() && taskType.getParent() != null) {
            try {
				Collection<SelectorOptions<GetOperationOptions>> getOptions =
						options.isRetrieveSiblings() ? createCollection(TaskType.F_SUBTASK, createRetrieve()) : null;
                parentTaskType = taskService.getTaskByIdentifier(taskType.getParent(), getOptions, thisOpResult).asObjectable();
                if (parentTaskType != null) {
                    parentTaskName = parentTaskType.getName() != null ? parentTaskType.getName().getOrig() : "(unnamed)";       // todo i18n
                    parentTaskOid = parentTaskType.getOid();
                }
            } catch (SchemaException|ObjectNotFoundException|SecurityViolationException|ConfigurationException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve parent task for task {}", e, taskType.getOid());
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
				return modelInteractionService.unwrapModelContext(lensContextType, result);
			} catch (SchemaException | CommunicationException | ConfigurationException e) {   // todo treat appropriately
				throw new SystemException("Couldn't access model operation context in task: " + e.getMessage(), e);
			}
		} else {
			return null;
		}
	}

    private void fillInWorkflowAttributes(TaskType taskType, ModelInteractionService modelInteractionService, WorkflowManager workflowManager,
			PrismContext prismContext, Task opTask,
			OperationResult thisOpResult) throws SchemaException, ObjectNotFoundException {

		WfContextType wfc = getWorkflowContext();

        workflowProcessInstanceId =
                taskType.getWorkflowContext() != null ? taskType.getWorkflowContext().getProcessInstanceId() : null;
        if (workflowProcessInstanceId != null) {
            workflowShadowTask = true;
        } else {
            workflowShadowTask = false;
        }

        workflowProcessInstanceFinished =
                taskType.getWorkflowContext() != null ?
                        taskType.getWorkflowContext().getEndTimestamp() != null : false;

//        PrismProperty<String> lastDetails = getExtensionProperty(taskType, WfTaskExtensionItemsNames.WFLAST_DETAILS_PROPERTY_NAME);
//        if (lastDetails != null) {
//            workflowLastDetails = lastDetails.getRealValue();
//        }

        workflowDeltasIn = retrieveDeltasToProcess(taskType, modelInteractionService, opTask, thisOpResult);
        workflowDeltaIn = retrieveDeltaToProcess(taskType, modelInteractionService, opTask, thisOpResult);
        workflowDeltasOut = retrieveResultingDeltas(taskType, modelInteractionService, opTask, thisOpResult);
        workflowHistory = prepareWorkflowHistory(taskType);

		final TaskType rootTask;
		if (parentTaskType == null) {
			rootTask = taskType;
		} else {
			rootTask = parentTaskType;
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

		if (wfc != null) {
			requestedBy = WebComponentUtil.getName(wfc.getRequesterRef());
			requestedOn = XmlTypeConverter.toDate(wfc.getStartTimestamp());
		}

		changes = new ArrayList<>();
		ChangesByState changesByState = workflowManager.getChangesByState(rootTask, modelInteractionService, prismContext, thisOpResult);
		if (!changesByState.getApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesApplied", changesByState.getApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getBeingApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesBeingApplied", changesByState.getBeingApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getWaitingToBeApplied().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesWaitingToBeApplied", changesByState.getWaitingToBeApplied(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getWaitingToBeApproved().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesWaitingToBeApproved", changesByState.getWaitingToBeApproved(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
		if (!changesByState.getRejected().isEmpty()) {
			changes.add(createTaskChangesDto("TaskDto.changesRejected", changesByState.getRejected(), modelInteractionService, prismContext, opTask, thisOpResult));
		}
	}

	private TaskChangesDto createTaskChangesDto(String titleKey, ObjectTreeDeltas deltas, ModelInteractionService modelInteractionService,
			PrismContext prismContext, Task opTask, OperationResult result) throws SchemaException {
		ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
		Scene scene = SceneUtil.visualizeObjectTreeDeltas(deltasType, titleKey, prismContext, modelInteractionService, opTask, result);
		return new TaskChangesDto(new SceneDto(scene));
	}

	private List<SceneDto> retrieveDeltasToProcess(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask,
			OperationResult thisOpResult) throws SchemaException {
        WfContextType wfc = taskType.getWorkflowContext();
        if (wfc == null || !(wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
            return null;
        }
        WfPrimaryChangeProcessorStateType pcps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
        return objectTreeDeltasToDeltaDtoList(pcps.getDeltasToProcess(), taskType.asPrismObject().getPrismContext(), modelInteractionService, opTask, thisOpResult);
    }

	private SceneDto retrieveDeltaToProcess(TaskType taskType, ModelInteractionService modelInteractionService, Task opTask,
			OperationResult thisOpResult) throws SchemaException {
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
			ModelInteractionService modelInteractionService, Task opTask, OperationResult thisOpResult) throws SchemaException {
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
			OperationResult thisOpResult) throws SchemaException {
        WfContextType wfc = taskType.getWorkflowContext();
        if (wfc == null || !(wfc.getProcessorSpecificState() instanceof WfPrimaryChangeProcessorStateType)) {
            return null;
        }
        WfPrimaryChangeProcessorStateType pcps = (WfPrimaryChangeProcessorStateType) wfc.getProcessorSpecificState();
        return objectTreeDeltasToDeltaDtoList(pcps.getResultingDeltas(), taskType.asPrismObject().getPrismContext(), modelInteractionService, opTask,
				thisOpResult);
    }

    private List<WfHistoryEventDto> prepareWorkflowHistory(TaskType taskType) {
        List<WfHistoryEventDto> retval = new ArrayList<WfHistoryEventDto>();
        // TODO remove whole method
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

	public TaskChangesDto getChangesForIndex(int index) {
		int realIndex = index-1;
		return realIndex < changes.size() ? changes.get(realIndex) : null;
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

    public List<SceneDto> getWorkflowDeltasIn() {
        return workflowDeltasIn;
    }

	public SceneDto getWorkflowDeltaIn() {
        return workflowDeltaIn;
    }

    public List<SceneDto> getWorkflowDeltasOut() {
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
        List<String> retval = new ArrayList<>();
        for (TaskDto taskDto : taskDtoList) {
            retval.add(taskDto.getOid());
        }
        return retval;
    }

    private PrismProperty getExtensionProperty(TaskType taskType, QName propertyName) {
        return taskType.asPrismObject().findProperty(new ItemPath(TaskType.F_EXTENSION, propertyName));
    }

	private <T> T getExtensionPropertyRealValue(QName propertyName, Class<T> clazz) {
		PrismProperty<T> property = taskType.asPrismObject().findProperty(new ItemPath(TaskType.F_EXTENSION, propertyName));
		return property != null ? property.getRealValue() : null;
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

    public String getIntent() {
        return intent;
    }

    public Integer getWorkerThreads() { return workerThreads; }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setWorkerThreads(Integer workerThreads) {
        this.workerThreads = workerThreads;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public List<QName> getObjectClassList() {
        if(objectClassList == null){
            objectClassList = new ArrayList<>();
        }

        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
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
			return prismContext.serializeAnyData(queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, PrismContext.LANG_XML);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize query: " + e.getMessage(), e);
		}
	}

	public String getObjectDelta() {
		ObjectDeltaType objectDeltaType = getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA, ObjectDeltaType.class);
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return prismContext.serializeAnyData(objectDeltaType, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA, PrismContext.LANG_XML);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize delta: " + e.getMessage(), e);
		}
	}

	public String getScript() {
		ExecuteScriptType script = getExtensionPropertyRealValue(SchemaConstants.SE_EXECUTE_SCRIPT, ExecuteScriptType.class);
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return prismContext.serializeAnyData(script, SchemaConstants.SE_EXECUTE_SCRIPT, PrismContext.LANG_XML);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize script: " + e.getMessage(), e);
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
		return requestedBy;
	}

	public Date getRequestedOn() {
		return requestedOn;
	}

	public Boolean getWorkflowOutcome() {
		WfContextType wfc = getWorkflowContext();
		return wfc != null ? wfc.isApproved() : null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaskDto taskDto = (TaskDto) o;

		if (workflowShadowTask != taskDto.workflowShadowTask)
			return false;
		if (workflowProcessInstanceFinished != taskDto.workflowProcessInstanceFinished)
			return false;
		if (dryRun != taskDto.dryRun)
			return false;
		if (menuItems != null ? !menuItems.equals(taskDto.menuItems) : taskDto.menuItems != null)
			return false;
		if (handlerUriList != null ? !handlerUriList.equals(taskDto.handlerUriList) : taskDto.handlerUriList != null)
			return false;
		if (parentTaskType != null ? !parentTaskType.equals(taskDto.parentTaskType) : taskDto.parentTaskType != null)
			return false;
		if (parentTaskName != null ? !parentTaskName.equals(taskDto.parentTaskName) : taskDto.parentTaskName != null)
			return false;
		if (parentTaskOid != null ? !parentTaskOid.equals(taskDto.parentTaskOid) : taskDto.parentTaskOid != null)
			return false;
		if (resourceRef != null ? !resourceRef.equals(taskDto.resourceRef) : taskDto.resourceRef != null)
			return false;
		if (interval != null ? !interval.equals(taskDto.interval) : taskDto.interval != null)
			return false;
		if (cronSpecification != null ? !cronSpecification.equals(taskDto.cronSpecification) : taskDto.cronSpecification != null)
			return false;
		if (notStartBefore != null ? !notStartBefore.equals(taskDto.notStartBefore) : taskDto.notStartBefore != null)
			return false;
		if (notStartAfter != null ? !notStartAfter.equals(taskDto.notStartAfter) : taskDto.notStartAfter != null)
			return false;
		if (misfireAction != taskDto.misfireAction)
			return false;
		if (opResult != null ? !opResult.equals(taskDto.opResult) : taskDto.opResult != null)
			return false;
		if (taskOperationResult != null ? !taskOperationResult.equals(taskDto.taskOperationResult) : taskDto.taskOperationResult != null)
			return false;
		if (modelOperationStatusDto != null ?
				!modelOperationStatusDto.equals(taskDto.modelOperationStatusDto) :
				taskDto.modelOperationStatusDto != null)
			return false;
		if (changes != null ? !changes.equals(taskDto.changes) : taskDto.changes != null)
			return false;
		if (objectRefType != taskDto.objectRefType)
			return false;
		if (objectRefName != null ? !objectRefName.equals(taskDto.objectRefName) : taskDto.objectRefName != null)
			return false;
		if (subtasks != null ? !subtasks.equals(taskDto.subtasks) : taskDto.subtasks != null)
			return false;
		if (transientSubtasks != null ? !transientSubtasks.equals(taskDto.transientSubtasks) : taskDto.transientSubtasks != null)
			return false;
		if (lastRunStartTimestampLong != null ?
				!lastRunStartTimestampLong.equals(taskDto.lastRunStartTimestampLong) :
				taskDto.lastRunStartTimestampLong != null)
			return false;
		if (lastRunFinishTimestampLong != null ?
				!lastRunFinishTimestampLong.equals(taskDto.lastRunFinishTimestampLong) :
				taskDto.lastRunFinishTimestampLong != null)
			return false;
		if (nextRunStartTimeLong != null ? !nextRunStartTimeLong.equals(taskDto.nextRunStartTimeLong) : taskDto.nextRunStartTimeLong != null)
			return false;
		if (completionTimestampLong != null ?
				!completionTimestampLong.equals(taskDto.completionTimestampLong) :
				taskDto.completionTimestampLong != null)
			return false;
		if (binding != taskDto.binding)
			return false;
		if (recurrence != taskDto.recurrence)
			return false;
		if (workflowProcessInstanceId != null ?
				!workflowProcessInstanceId.equals(taskDto.workflowProcessInstanceId) :
				taskDto.workflowProcessInstanceId != null)
			return false;
		if (workflowLastDetails != null ? !workflowLastDetails.equals(taskDto.workflowLastDetails) : taskDto.workflowLastDetails != null)
			return false;
		if (workflowRequests != null ? !workflowRequests.equals(taskDto.workflowRequests) : taskDto.workflowRequests != null)
			return false;
		if (requestedBy != null ? !requestedBy.equals(taskDto.requestedBy) : taskDto.requestedBy != null)
			return false;
		if (requestedOn != null ? !requestedOn.equals(taskDto.requestedOn) : taskDto.requestedOn != null)
			return false;
		if (workflowDeltasIn != null ? !workflowDeltasIn.equals(taskDto.workflowDeltasIn) : taskDto.workflowDeltasIn != null)
			return false;
		if (workflowDeltasOut != null ? !workflowDeltasOut.equals(taskDto.workflowDeltasOut) : taskDto.workflowDeltasOut != null)
			return false;
		if (workflowHistory != null ? !workflowHistory.equals(taskDto.workflowHistory) : taskDto.workflowHistory != null)
			return false;
		if (workflowDeltaIn != null ? !workflowDeltaIn.equals(taskDto.workflowDeltaIn) : taskDto.workflowDeltaIn != null)
			return false;
		if (kind != taskDto.kind)
			return false;
		if (intent != null ? !intent.equals(taskDto.intent) : taskDto.intent != null)
			return false;
		if (objectClass != null ? !objectClass.equals(taskDto.objectClass) : taskDto.objectClass != null)
			return false;
		if (objectClassList != null ? !objectClassList.equals(taskDto.objectClassList) : taskDto.objectClassList != null)
			return false;
		if (workerThreads != null ? !workerThreads.equals(taskDto.workerThreads) : taskDto.workerThreads != null)
			return false;
		return taskType != null ? taskType.equals(taskDto.taskType) : taskDto.taskType == null;

	}

	@Override
	public int hashCode() {
		int result = menuItems != null ? menuItems.hashCode() : 0;
		result = 31 * result + (handlerUriList != null ? handlerUriList.hashCode() : 0);
		result = 31 * result + (parentTaskType != null ? parentTaskType.hashCode() : 0);
		result = 31 * result + (parentTaskName != null ? parentTaskName.hashCode() : 0);
		result = 31 * result + (parentTaskOid != null ? parentTaskOid.hashCode() : 0);
		result = 31 * result + (resourceRef != null ? resourceRef.hashCode() : 0);
		result = 31 * result + (interval != null ? interval.hashCode() : 0);
		result = 31 * result + (cronSpecification != null ? cronSpecification.hashCode() : 0);
		result = 31 * result + (notStartBefore != null ? notStartBefore.hashCode() : 0);
		result = 31 * result + (notStartAfter != null ? notStartAfter.hashCode() : 0);
		result = 31 * result + (misfireAction != null ? misfireAction.hashCode() : 0);
		result = 31 * result + (opResult != null ? opResult.hashCode() : 0);
		result = 31 * result + (taskOperationResult != null ? taskOperationResult.hashCode() : 0);
		result = 31 * result + (modelOperationStatusDto != null ? modelOperationStatusDto.hashCode() : 0);
		result = 31 * result + (changes != null ? changes.hashCode() : 0);
		result = 31 * result + (objectRefType != null ? objectRefType.hashCode() : 0);
		result = 31 * result + (objectRefName != null ? objectRefName.hashCode() : 0);
		result = 31 * result + (subtasks != null ? subtasks.hashCode() : 0);
		result = 31 * result + (transientSubtasks != null ? transientSubtasks.hashCode() : 0);
		result = 31 * result + (lastRunStartTimestampLong != null ? lastRunStartTimestampLong.hashCode() : 0);
		result = 31 * result + (lastRunFinishTimestampLong != null ? lastRunFinishTimestampLong.hashCode() : 0);
		result = 31 * result + (nextRunStartTimeLong != null ? nextRunStartTimeLong.hashCode() : 0);
		result = 31 * result + (completionTimestampLong != null ? completionTimestampLong.hashCode() : 0);
		result = 31 * result + (binding != null ? binding.hashCode() : 0);
		result = 31 * result + (recurrence != null ? recurrence.hashCode() : 0);
		result = 31 * result + (workflowShadowTask ? 1 : 0);
		result = 31 * result + (workflowProcessInstanceId != null ? workflowProcessInstanceId.hashCode() : 0);
		result = 31 * result + (workflowProcessInstanceFinished ? 1 : 0);
		result = 31 * result + (workflowLastDetails != null ? workflowLastDetails.hashCode() : 0);
		result = 31 * result + (workflowRequests != null ? workflowRequests.hashCode() : 0);
		result = 31 * result + (requestedBy != null ? requestedBy.hashCode() : 0);
		result = 31 * result + (requestedOn != null ? requestedOn.hashCode() : 0);
		result = 31 * result + (workflowDeltasIn != null ? workflowDeltasIn.hashCode() : 0);
		result = 31 * result + (workflowDeltasOut != null ? workflowDeltasOut.hashCode() : 0);
		result = 31 * result + (workflowHistory != null ? workflowHistory.hashCode() : 0);
		result = 31 * result + (workflowDeltaIn != null ? workflowDeltaIn.hashCode() : 0);
		result = 31 * result + (dryRun ? 1 : 0);
		result = 31 * result + (kind != null ? kind.hashCode() : 0);
		result = 31 * result + (intent != null ? intent.hashCode() : 0);
		result = 31 * result + (objectClass != null ? objectClass.hashCode() : 0);
		result = 31 * result + (objectClassList != null ? objectClassList.hashCode() : 0);
		result = 31 * result + (workerThreads != null ? workerThreads.hashCode() : 0);
		result = 31 * result + (taskType != null ? taskType.hashCode() : 0);
		return result;
	}

	@Override
    public String toString() {
        return "TaskDto{" +
                "taskType=" + taskType +
                '}';
    }

}
