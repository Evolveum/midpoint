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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.Duration;
import java.util.*;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames.*;

/**
 * A generic instruction to start a background task; with or without a workflow process instance.
 * May be subclassed in order to add further functionality.
 *
 * @author mederly
 */
public class WfTaskCreationInstruction<PRC extends ProcessorSpecificContent, PCS extends ProcessSpecificContent> implements DebugDumpable {

	private static final Trace LOGGER = TraceManager.getTrace(WfTaskCreationInstruction.class);
	private static final Integer DEFAULT_PROCESS_CHECK_INTERVAL = 30;
	private static final String DEFAULT_EXECUTION_GROUP_PREFIX_FOR_SERIALIZATION = "$approval-task-group$:";
	private static final long DEFAULT_SERIALIZATION_RETRY_TIME = 10000L;

	protected final ChangeProcessor changeProcessor;

    protected final WfContextType wfContext = new WfContextType();    // workflow context to be put into the task
	private ModelContext taskModelContext;   						// model context to be put into the task

	protected final Date processCreationTimestamp = new Date();

	protected final PRC processorContent;
	protected final PCS processContent;

	protected PrismObject taskObject;          // object to be attached to the task; this object must have its definition available
	protected PrismObject<UserType> taskOwner; // if null, owner from parent task will be taken (if there's no parent task, exception will be thrown)
	protected PolyStringType taskName;         // name of task to be created/updated (applies only if the task has no name already) - e.g. "Approve adding role R to U"

	protected boolean executeModelOperationHandler;       // should the task contain model operation to be executed?
	protected boolean noProcess;                          // should the task provide no wf process (only direct execution of model operation)?

	protected boolean simple;                             // is workflow process simple? (i.e. such that requires periodic watching of its state)
	protected boolean sendStartConfirmation = true;       // should we send explicit "process started" event when the process was started by midPoint?
                                                        // for listener-enabled processes this can be misleading, because "process started" event could come
                                                        // after "process finished" one (for immediately-finishing processes)
                                                        //
                                                        // unfortunately, it seems we have to live with this (unless we define a "process started" listener)

	protected TaskExecutionStatus taskInitialState = TaskExecutionStatus.RUNNABLE;

    // what should be executed at a given occasion (in the order of being in this list)
    protected final List<UriStackEntry> handlersAfterModelOperation = new ArrayList<>();
	protected final List<UriStackEntry> handlersBeforeModelOperation = new ArrayList<>();
	protected final List<UriStackEntry> handlersAfterWfProcess = new ArrayList<>();

    //region Constructors
    protected WfTaskCreationInstruction(ChangeProcessor changeProcessor, PRC processorContent, PCS processContent) {
        Validate.notNull(changeProcessor);
        this.changeProcessor = changeProcessor;
		this.processorContent = processorContent;
		this.processContent = processContent;
    }

	@SuppressWarnings("unchecked")
    public static WfTaskCreationInstruction<?,?> createModelOnly(ChangeProcessor changeProcessor, ModelContext modelContext) throws SchemaException {
        WfTaskCreationInstruction<?,?> instruction = new WfTaskCreationInstruction(changeProcessor, null, null);
        instruction.setNoProcess(true);
        instruction.setTaskModelContext(modelContext);
        instruction.setExecuteModelOperationHandler(true);
        return instruction;
    }

	@SuppressWarnings("unchecked")
    public static WfTaskCreationInstruction<?,?> createWfOnly(ChangeProcessor changeProcessor,
			ProcessorSpecificContent processorSpecificContent, ProcessSpecificContent processSpecificContent) {
		return new WfTaskCreationInstruction(changeProcessor, processorSpecificContent, processSpecificContent);
    }

	@SuppressWarnings("unchecked")
	public static WfTaskCreationInstruction<?,?> createEmpty(ChangeProcessor changeProcessor) throws SchemaException {
		WfTaskCreationInstruction<?,?> instruction = new WfTaskCreationInstruction(changeProcessor, null, null);
		instruction.setNoProcess(true);
		return instruction;
	}
	//endregion

    // region Getters and setters
	public ChangeProcessor getChangeProcessor() {
		return changeProcessor;
	}

	protected PrismContext getPrismContext() { return changeProcessor.getPrismContext(); }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public boolean isSendStartConfirmation() {
        return sendStartConfirmation;
    }

    public void setSendStartConfirmation(boolean sendStartConfirmation) {
        this.sendStartConfirmation = sendStartConfirmation;
    }

	public String getProcessName() {
		return wfContext.getProcessName();
	}

	public void setProcessName(String name) {
        wfContext.setProcessName(name);
    }

    public String getProcessInstanceName() {
        return wfContext.getProcessInstanceName();
    }

	public void setProcessInstanceName(String name) {
		wfContext.setProcessInstanceName(name);
	}

	public void setLocalizableProcessInstanceName(LocalizableMessage name) {
    	if (name != null) {
		    wfContext.getLocalizableProcessInstanceName().add(LocalizationUtil.createLocalizableMessageType(name));
	    }
	}

	// use only for the root task
	public void setLocalizableTaskName(LocalizableMessage name) {
		wfContext.setLocalizableTaskName(name != null ? LocalizationUtil.createLocalizableMessageType(name) : null);
	}

    public void setTaskName(String taskName) {
        this.taskName = new PolyStringType(taskName);
    }

    public boolean isNoProcess() {
        return noProcess;
    }

    public boolean startsWorkflowProcess() {
        return !noProcess;
    }

    public void setNoProcess(boolean noProcess) {
        this.noProcess = noProcess;
    }

    public void setCreateTaskAsSuspended() {
        this.taskInitialState = TaskExecutionStatus.SUSPENDED;
    }

	public void setCreateTaskAsWaiting() {
		this.taskInitialState = TaskExecutionStatus.WAITING;
	}

	public List<UriStackEntry> getHandlersAfterModelOperation() {
        return handlersAfterModelOperation;
    }

    public List<UriStackEntry> getHandlersBeforeModelOperation() {
        return handlersBeforeModelOperation;
    }

    public List<UriStackEntry> getHandlersAfterWfProcess() {
        return handlersAfterWfProcess;
    }

	public void setHandlersBeforeModelOperation(String... handlerUri) {
		setHandlers(handlersBeforeModelOperation, createUriStackEntries(handlerUri));
	}

	public void setHandlersAfterModelOperation(String... handlerUri) {
		setHandlers(handlersAfterModelOperation, createUriStackEntries(handlerUri));
	}

	public void addHandlersAfterWfProcessAtEnd(String... handlerUriArray) {
		addHandlersAtEnd(handlersAfterWfProcess, createUriStackEntries(handlerUriArray));
	}

	private List<UriStackEntry> createUriStackEntries(String[] handlerUriArray) {
		List<UriStackEntry> retval = new ArrayList<>();
		for (String handlerUri : handlerUriArray) {
			retval.add(createUriStackEntry(handlerUri));
		}
		return retval;
	}

	private UriStackEntry createUriStackEntry(String handlerUri, TaskRecurrence recurrence, ScheduleType scheduleType, TaskBinding taskBinding) {
		UriStackEntry uriStackEntry = new UriStackEntry();
		uriStackEntry.setHandlerUri(handlerUri);
		uriStackEntry.setRecurrence(recurrence != null ? recurrence.toTaskType() : null);
		uriStackEntry.setSchedule(scheduleType);
		uriStackEntry.setBinding(taskBinding != null ? taskBinding.toTaskType() : null);
		return uriStackEntry;
	}

	private UriStackEntry createUriStackEntry(String handlerUri) {
		return createUriStackEntry(handlerUri, TaskRecurrence.SINGLE, new ScheduleType(), null);
	}

	private void setHandlers(List<UriStackEntry> list, List<UriStackEntry> uriStackEntry) {
		list.clear();
		list.addAll(uriStackEntry);
	}

	private void addHandlersAtEnd(List<UriStackEntry> list, List<UriStackEntry> uriStackEntry) {
		list.addAll(uriStackEntry);
	}

	public void setExecuteModelOperationHandler(boolean executeModelOperationHandler) {
        this.executeModelOperationHandler = executeModelOperationHandler;
    }

	public void setTaskObject(PrismObject taskObject) {
        this.taskObject = taskObject;
    }

    public void setTaskOwner(PrismObject<UserType> taskOwner) {
        this.taskOwner = taskOwner;
    }

    public void setTaskModelContext(ModelContext taskModelContext) {
        this.taskModelContext = taskModelContext;
    }

	public void setObjectRef(ObjectReferenceType ref, OperationResult result) {
		ref = getChangeProcessor().getMiscDataUtil().resolveObjectReferenceName(ref, result);
		wfContext.setObjectRef(ref);
	}

	public void setObjectRef(ModelContext<?> modelContext, OperationResult result) {
		ObjectType focus = MiscDataUtil.getFocusObjectNewOrOld(modelContext);
		ObjectDelta<?> primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
		if (primaryDelta != null && primaryDelta.isAdd()) {
			ObjectReferenceType ref = ObjectTypeUtil.createObjectRefWithFullObject(focus);
			wfContext.setObjectRef(ref);
		} else {
			setObjectRef(ObjectTypeUtil.createObjectRef(focus), result);
		}
	}

	public void setTargetRef(ObjectReferenceType ref, OperationResult result) {
		ref = getChangeProcessor().getMiscDataUtil().resolveObjectReferenceName(ref, result);
		wfContext.setTargetRef(ref);
	}

    public void setRequesterRef(PrismObject<UserType> requester) {
		wfContext.setRequesterRef(createObjectRef(requester));
    }

    public void setProcessInterfaceBean(ProcessMidPointInterface processInterfaceBean) {
		wfContext.setProcessInterface(processInterfaceBean.getBeanName());
    }

	public PRC getProcessorContent() {
		return processorContent;
	}

	public PCS getProcessContent() {
		return processContent;
	}

	public WfContextType getWfContext() {
		return wfContext;
	}

	//endregion

    //region Diagnostics
    public String toString() {
        return "WfTaskCreationInstruction: processDefinitionKey = " + getProcessName() + ", simple: " + simple;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

	@Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("WfTaskCreationInstruction: process: ").append(getProcessName()).append("/").append(getProcessInstanceName());
		sb.append(" ").append(simple ? "simple" : "smart").append(", ").append(noProcess ? "no-process" : "with-process").append(", model-context: ");
		sb.append(taskModelContext != null ? "YES" : "no").append(", task = ").append(taskName).append("\n");

        DebugUtil.indentDebugDump(sb, indent);
		sb.append("Workflow context:\n");
		sb.append(wfContext.asPrismContainerValue().debugDump(indent+2)).append("\n");

		DebugUtil.debugDumpWithLabelLn(sb, "Change processor", changeProcessor.getClass().getName(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Process creation timestamp", String.valueOf(processCreationTimestamp), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Task object", String.valueOf(taskObject), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Task owner", String.valueOf(taskOwner), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Task initial state", String.valueOf(taskInitialState), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Send start confirmation", String.valueOf(sendStartConfirmation), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Handlers after model operation", String.valueOf(handlersAfterModelOperation), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Handlers before model operation", String.valueOf(handlersBeforeModelOperation), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Handlers after wf process", String.valueOf(handlersAfterWfProcess), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Processor instruction", String.valueOf(processorContent), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "Process instruction", String.valueOf(processContent), indent+1);

        return sb.toString();

    }
    //endregion

	//region "Output" methods
	public Task createTask(WfTaskController taskController, Task parentTask, WfConfigurationType wfConfigurationType) throws SchemaException {

		LOGGER.trace("createTask starting; parent task = {}", parentTask);

		final WfTaskUtil wfTaskUtil = taskController.getWfTaskUtil();

		final Task task;
		if (parentTask != null) {
			task = parentTask.createSubtask();
		} else {
			task = taskController.getTaskManager().createTaskInstance();
			if (taskOwner == null) {
				throw new IllegalStateException("No task owner for " + task);
			}
			task.setOwner(taskOwner);
		}

		task.setInitialExecutionStatus(taskInitialState);
		task.setCategory(TaskCategory.WORKFLOW);

		if (taskObject != null) {
			task.setObjectRef(taskObject.getOid(), taskObject.getDefinition().getTypeName());
		} else if (parentTask != null && parentTask.getObjectRef() != null) {
			task.setObjectRef(parentTask.getObjectRef().clone());
		}
		if (task.getName() == null || task.getName().toPolyString().isEmpty()) {
			task.setName(taskName);
		}

		// push the handlers, beginning with these that should execute last
		wfTaskUtil.pushHandlers(task, getHandlersAfterModelOperation());
		if (executeModelOperationHandler) {
			task.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, null, null);
		}
		wfTaskUtil.pushHandlers(task, getHandlersBeforeModelOperation());
		wfTaskUtil.pushHandlers(task, getHandlersAfterWfProcess());
		if (!noProcess) {
			if (simple) {
				ScheduleType schedule = new ScheduleType();
				Integer processCheckInterval = wfConfigurationType != null ? wfConfigurationType.getProcessCheckInterval() : null;
				schedule.setInterval(processCheckInterval != null ? processCheckInterval : DEFAULT_PROCESS_CHECK_INTERVAL);
				schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(new Date(System.currentTimeMillis() + WfTaskController.TASK_START_DELAY)));
				task.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, schedule, TaskBinding.LOOSE);
			} else {
				task.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, new ScheduleType(), null);		// note that this handler will not be actively used (at least for now)
				task.makeWaiting();
			}
		}

		// model and workflow context
		if (taskModelContext != null) {
			boolean reduced = taskModelContext.getState() == ModelState.PRIMARY;
			task.setModelOperationContext(((LensContext) taskModelContext).toLensContextType(reduced));
		}
		wfContext.setChangeProcessor(changeProcessor.getClass().getName());
		wfContext.setStartTimestamp(createXMLGregorianCalendar(processCreationTimestamp));
		createProcessorContent();
		if (processContent != null) {
			wfContext.setProcessSpecificState(processContent.createProcessSpecificState());
		}
		task.setWorkflowContext(wfContext);

		WfExecutionTasksConfigurationType tasksConfig = wfConfigurationType != null ? wfConfigurationType.getExecutionTasks() : null;
		if (executeModelOperationHandler && tasksConfig != null) {
			TaskType taskBean = task.getTaskPrismObject().asObjectable();
			// execution constraints
			TaskExecutionConstraintsType constraints = tasksConfig.getExecutionConstraints();
			if (constraints != null) {
				taskBean.setExecutionConstraints(constraints.clone());
			}
			// serialization
			WfExecutionTasksSerializationType serialization = tasksConfig.getSerialization();
			if (serialization != null && !Boolean.FALSE.equals(serialization.isEnabled())) {
				List<WfExecutionTasksSerializationScopeType> scopes = new ArrayList<>(serialization.getScope());
				if (scopes.isEmpty()) {
					scopes.add(WfExecutionTasksSerializationScopeType.OBJECT);
				}
				List<String> groups = new ArrayList<>(scopes.size());
				for (WfExecutionTasksSerializationScopeType scope : scopes) {
					String groupPrefix = serialization.getGroupPrefix() != null
							? serialization.getGroupPrefix() : DEFAULT_EXECUTION_GROUP_PREFIX_FOR_SERIALIZATION;
					String groupSuffix = getGroupSuffix(scope, wfContext, parentTask, task);
					if (groupSuffix == null) {
						continue;
					}
					groups.add(groupPrefix + scope.value() + ":" + groupSuffix);
				}
				if (!groups.isEmpty()) {
					Duration retryAfter;
					if (serialization.getRetryAfter() != null) {
						if (constraints != null && constraints.getRetryAfter() != null && !constraints.getRetryAfter()
								.equals(serialization.getRetryAfter())) {
							LOGGER.warn(
									"Workflow configuration: task constraints retryAfter ({}) is different from serialization retryAfter ({}) -- using the latter",
									constraints.getRetryAfter(), serialization.getRetryAfter());
						}
						retryAfter = serialization.getRetryAfter();
					} else if (constraints != null && constraints.getRetryAfter() != null) {
						retryAfter = constraints.getRetryAfter();
					} else {
						retryAfter = XmlTypeConverter.createDuration(DEFAULT_SERIALIZATION_RETRY_TIME);
					}
					TaskExecutionConstraintsType executionConstraints = taskBean.getExecutionConstraints();
					if (executionConstraints == null) {
						executionConstraints = new TaskExecutionConstraintsType();
						taskBean.setExecutionConstraints(executionConstraints);
					}
					for (String group : groups) {
						executionConstraints
								.beginSecondaryGroup()
								.group(group)
								.groupTaskLimit(1);
					}
					executionConstraints.setRetryAfter(retryAfter);
					LOGGER.trace("Setting groups {} with a limit of 1 for task {}", groups, task);
				}
			}
		}
		return task;
	}

	private String getGroupSuffix(WfExecutionTasksSerializationScopeType scope, WfContextType wfContext, Task parentTask, Task task) {
		switch (scope) {
			case GLOBAL: return "";
			case OBJECT:
				String oid = wfContext.getObjectRef() != null ? wfContext.getObjectRef().getOid() : null;
				if (oid == null) {
					LOGGER.warn("No object OID present, synchronization with the scope of {} couldn't be set up for task {}", scope, task);
					return null;
				}
				return oid;
			case TARGET:
				return wfContext.getTargetRef() != null ? wfContext.getTargetRef().getOid() : null;     // null can occur so let's be silent then
			case OPERATION:
				return parentTask != null ? parentTask.getTaskIdentifier() : null;                      // null can occur so let's be silent then
			default:
				throw new AssertionError("Unknown scope: " + scope);
		}
	}

	// FIXME brutal hack because of objectDelta should be in wfContext when evaluating auto completion expression
	public void createProcessorContent() {
		if (processorContent != null) {
			wfContext.setProcessorSpecificState(null);			// ugly hack, see PrismForJaxbUtil:217
			wfContext.setProcessorSpecificState(processorContent.createProcessorSpecificState());
		}
	}

	public Map<String, Object> getAllProcessVariables() throws SchemaException {
		Map<String, Object> map = new HashMap<>();
		map.put(VARIABLE_PROCESS_INSTANCE_NAME, wfContext.getProcessInstanceName());
		map.put(VARIABLE_START_TIME, processCreationTimestamp);
		map.put(VARIABLE_OBJECT_REF, toLightweightObjectRef(wfContext.getObjectRef()));
		map.put(VARIABLE_TARGET_REF, toLightweightObjectRef(wfContext.getTargetRef()));
		map.put(VARIABLE_REQUESTER_REF, toLightweightObjectRef(wfContext.getRequesterRef()));
		map.put(VARIABLE_CHANGE_PROCESSOR, changeProcessor.getClass().getName());
		map.put(VARIABLE_PROCESS_INTERFACE_BEAN_NAME, wfContext.getProcessInterface());
		map.put(VARIABLE_UTIL, new ActivitiUtil());
		if (processorContent != null) {
			processorContent.createProcessVariables(map, getPrismContext());
		}
		if (processContent != null) {
			processContent.createProcessVariables(map, getPrismContext());
		}
		return map;
	}

	private LightweightObjectRef toLightweightObjectRef(ObjectReferenceType ref) {
		return ref != null ? new LightweightObjectRefImpl(ref) : null;
	}

	//endregion
}