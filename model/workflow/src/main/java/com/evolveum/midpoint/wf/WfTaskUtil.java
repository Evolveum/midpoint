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

package com.evolveum.midpoint.wf;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.messages.QueryProcessResponse;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.opends.server.admin.PropertyDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessVariable;
import org.springframework.stereotype.Component;

/**
 * 
 * @author mederly
 *
 */

@Component
public class WfTaskUtil {

//	@Autowired(required = true)
//	private TaskManager taskManager;

	@Autowired(required = true)
	private RepositoryService repositoryService;

    @Autowired(required = true)
    private WorkflowManager workflowManager;

//    @Autowired(required = true)
//    private ModelController modelController;

    @Autowired(required = true)
    private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(WfTaskUtil.class);

	public static final String WORKFLOW_EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/public/model/workflow-1.xsd";
//	public static final String WORKFLOW_COMMUNICATION_NS = "http://midpoint.evolveum.com/xml/ns/public/communication/workflow-1.xsd";

	// wfStatus - records information about process execution at WfMS

	private static final boolean USE_WFSTATUS = true;
	public static final QName WFSTATUS_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfStatus");
	private PrismPropertyDefinition wfStatusPropertyDefinition;

	// wfLastEvent - stores last event received from WfMS

    public static final QName WFLASTVARIABLES1_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfLastVariables1");
	public static final QName WFLASTVARIABLES_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfLastVariables");
	private PrismPropertyDefinition wfLastVariablesPropertyDefinition;

	// wfProcessWrapper - class that serves as an interface for starting and finishing wf process
	
	public static final QName WFPROCESS_WRAPPER_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfProcessWrapper");
	private PrismPropertyDefinition wfProcessWrapperPropertyDefinition;
	
	// wfProcessId - id of the workflow process instance
	
	public static final QName WFPROCESSID_PROPERTY_NAME = new QName(WORKFLOW_EXTENSION_NS, "wfProcessId");
	private PrismPropertyDefinition wfProcessIdPropertyDefinition;
    private static final long TASK_START_DELAY = 5000L;

    @PostConstruct
    public void prepareDefinitions() {

//        try {
//            prismContext.getSchemaRegistry().loadPrismSchemaResource("schema/workflows-in-task.xsd");
//        } catch (SchemaException e) {
//            throw new SystemException("Cannot load workflow-related task extensions due schema exception", e);
//        }

        wfStatusPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(WFSTATUS_PROPERTY_NAME);
		wfLastVariablesPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(WFLASTVARIABLES_PROPERTY_NAME);
		wfProcessWrapperPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(WFPROCESS_WRAPPER_PROPERTY_NAME);
		wfProcessIdPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(WFPROCESSID_PROPERTY_NAME);

        if (wfLastVariablesPropertyDefinition.isIndexed() != Boolean.FALSE) {
            throw new SystemException("wfLastVariables property definition was not found or is not correct (its isIndexed should be FALSE)");
        }
	}


	/**
	 * Makes a task active, i.e. a task that actively queries wf process instance about its status.
	 */
	void prepareActiveTask(Task t, String taskName, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
//		// shutdown the task if it is already running (i.e. a run from previous phase)
//		t.shutdown();

        if (StringUtils.isEmpty(t.getName())) {
		    t.setName(taskName);
        }

        t.pushHandlerUri(ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, new ScheduleType(), null);

        ScheduleType schedule = new ScheduleType();
        schedule.setInterval(workflowManager.getWfConfiguration().getProcessCheckInterval());
        schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(new Date(System.currentTimeMillis() + TASK_START_DELAY)));
		t.pushHandlerUri(WfTaskHandler.WF_SHADOW_TASK_URI, schedule, null);

        t.setCategory(TaskCategory.WORKFLOW);
        t.setBinding(TaskBinding.LOOSE);


        t.makeRunnable();
        // TODO fix this ugly hack
        t.setOwner(repositoryService.getObject(UserType.class, "00000000-0000-0000-0000-000000000002", parentResult));
        t.savePendingModifications(parentResult);
	}
	
	/**
	 * Creates a passive task, i.e. a task that stores information received from WfMS about a process instance.
	 * 
\	 * @param taskName
	 * @return
	 */
	
	void preparePassiveTask(Task t, String taskName, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
//		// shutdown the task if it is already running (i.e. a run from previous phase)
//		t.shutdown();

        if (StringUtils.isEmpty(t.getName())) {
            t.setName(taskName);
        }
        t.pushHandlerUri(WfTaskHandler.WF_SHADOW_TASK_URI, new ScheduleType(), null);		// note that this handler will not be used (at least for now)
        t.setCategory(TaskCategory.WORKFLOW);
		t.makeSingle();
        t.makeWaiting();
        t.savePendingModifications(parentResult);
	}

//	// stores a task persistently (releases it)
//	boolean persistTask(Task task, OperationResult parentResult) {
//		try {
//			taskManager.switchToBackground(task, parentResult);
//			LOGGER.trace("Task switch to background; oid = " + task.getOid());
//			return true;
//		} catch (Exception ex) {
//			LoggingUtils.logException(LOGGER, "Couldn't switch to background a workflow-process-watching task {}", ex, task);
//			parentResult.recordFatalError("Couldn't switch to background a workflow-process-watching task " + task, ex);
//			return false;
//		}
//	}

	void setWfProcessId(Task task, String pid, OperationResult parentResult) {
		String oid = task.getOid();
		Validate.notEmpty(oid, "Task oid must not be null or empty (task must be persistent).");
		LOGGER.info("Setting wf process id = " + pid + " for task " + oid);
		setExtensionProperty(task, wfProcessIdPropertyDefinition, pid, parentResult);
	}

	String getLastVariables(Task task) {
		PrismProperty<?> p = task.getExtension(WFLASTVARIABLES_PROPERTY_NAME);
		if (p == null) {
			return null;
        } else {
			return p.getValue(String.class).getValue();
        }
	}
	
	/**
	 * Sets an extension property.
	 */
	private void setExtensionProperty(Task task, PrismPropertyDefinition propertyDef, String propertyValue, OperationResult parentResult)
	{
		if (parentResult == null)
			parentResult = new OperationResult("setExtensionProperty");
		
		PrismProperty prop = propertyDef.instantiate();
        prop.setValue(new PrismPropertyValue<String>(propertyValue));

		try {
            task.setExtensionPropertyImmediate(prop, parentResult);
        } catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Object not found", ex);
            LoggingUtils.logException(LOGGER, "Cannot set process ID for task {}", ex, task);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error", ex);
            LoggingUtils.logException(LOGGER, "Cannot set process ID for task {}", ex, task);
		} catch (RuntimeException ex) {
			parentResult.recordFatalError("Internal error", ex);
            LoggingUtils.logException(LOGGER, "Cannot set process ID for task {}", ex, task);
		}
		
	}

	
	/**
	 * Sets a task description to a given text. 
	 * 
	 * param oid OID of the task. Must not be null, i.e. task should be already persisted.
	 * @param description Description to be set.
	 * @param parentResult
	 */
	void recordProcessState(Task task, String description, String details, ProcessEvent event,
			OperationResult parentResult) {
		
		LOGGER.info("recordProcessState starting.");

		String oid = task.getOid();

		Validate.notEmpty(oid, "Task oid must not be null or empty (task must be persistent).");
		if (parentResult == null)
			parentResult = new OperationResult("recordProcessState");

		// descriptionTsDt (for wfStatus): [<timestamp>: <formatted datetime>] <status description>
		// (timestamp is to enable easy sorting, [] to easy parsing)
		// descriptionDt (for OperationResult): only formatted datetime + status description

		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		String descriptionTsDt = "[" + d.getTime() + ": " + df.format(d) + "] " + description;
		String descriptionDt = "[" + df.format(d) + "] " + description;

		/*
		 * Let us construct and execute the modification request.
		 */
		try {

			ObjectModificationType modification = new ObjectModificationType();
			modification.setOid(oid);

			/*
			 * (1) Change the task description.
			 */
            task.setDescription(description);

			/*
			 * The following is executed only if the process variables have changed (for active tasks i.e. answers to queries)
			 */
			boolean recordStatus = true;
			if (event != null && event instanceof QueryProcessResponse) {
				String lastVariables = getLastVariables(task);
				if (lastVariables != null) {
					String currentVariables = dumpVariables(event);
					if (lastVariables.equals(currentVariables)) {
						LOGGER.info("skipping inserting record, as current variables == last variables: " + currentVariables);
						recordStatus = false;
					}
				}
			}

			if (recordStatus) {

				/*
				 * (2) Add a record to wfStatus extension element
				 */
				if (USE_WFSTATUS) {
					PrismProperty wfStatusProperty = wfStatusPropertyDefinition.instantiate();
                    PrismPropertyValue<String> newValue = new PrismPropertyValue<String>(descriptionTsDt);
                    wfStatusProperty.addValue(newValue);
                    task.addExtensionProperty(wfStatusProperty);
				}

				/*
				 * (3) Create an operationResult sub-result with the status line
				 */
				OperationResult or = task.getResult();

				String ordump = or == null ? null : or.dump();

				LOGGER.info("-------------------------------------------------------------------");
				LOGGER.info("Original operation result: " + ordump);

				if (or == null)
					or = new OperationResult("Task");

				OperationResult sub = or.createSubresult(descriptionDt);
				sub.recordStatus(OperationResultStatus.SUCCESS, details);

				LOGGER.info("-------------------------------------------------------------------");
				LOGGER.info("Setting new operation result: " + or.dump());

                task.setResult(or);
			}

			/*
			 * (4) Store the variables from the event.
			 */
			if (event != null) {
				PrismProperty wfLastVariablesProperty = wfLastVariablesPropertyDefinition.instantiate();
				String variables = dumpVariables(event);
//                if (variables.length() > 255) {             // todo fix this hack
//                    variables = variables.substring(0, 252) + "...";
//                }
                wfLastVariablesProperty.setValue(new PrismPropertyValue<String>(variables));
                LOGGER.info("WfLastVariable INDEXED = " + wfLastVariablesProperty.getDefinition().isIndexed());
                task.setExtensionProperty(wfLastVariablesProperty);
			}

			/*
			 * Now execute the modification
			 */
            task.savePendingModifications(parentResult);

		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't record information from WfMS into task " + oid, ex);
			parentResult.recordFatalError("Couldn't record information from WfMS into task " + oid, ex);
		}

		LOGGER.info("recordProcessState ending.");
	}

    // todo: fix this brutal hack (task closing should not go through repo)
	void markTaskAsClosed(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        String oid = task.getOid();
		try {
            ItemDelta delta = PropertyDelta.createReplaceDelta(
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class),
                    TaskType.F_EXECUTION_STATUS,
                    TaskExecutionStatusType.CLOSED);
            List<ItemDelta> deltas = new ArrayList<ItemDelta>();
            deltas.add(delta);
			repositoryService.modifyObject(TaskType.class, oid, deltas, parentResult);
		} catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot mark task " + task + " as closed.", e);
            throw e;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot mark task " + task + " as closed.", e);
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logException(LOGGER, "Cannot mark task " + task + " as closed.", e);
            throw new SystemException(e);
        }

    }

//	void returnToAsyncCaller(Task task, OperationResult parentResult) throws Exception {
//		try {
//			task.finishHandler(parentResult);
////			if (task.getExclusivityStatus() == TaskExclusivityStatus.CLAIMED) {
////				taskManager.releaseTask(task, parentResult);	// necessary for active tasks
////				task.shutdown();								// this works when this java object is the same as is being executed by CycleRunner
////			}
////			if (task.getHandlerUri() != null)
////				task.setExecutionStatusWithPersist(TaskExecutionStatus.RUNNING, parentResult);
//		}
//		catch (Exception e) {
//			throw new Exception("Couldn't mark task " + task + " as running.", e);
//		}
//	}

//	void setTaskResult(String oid, OperationResult result) throws Exception {
//		try {
//			OperationResultType ort = result.createOperationResultType();
//			repositoryService.modifyObject(TaskType.class, ObjectTypeUtil.createModificationReplaceProperty(oid, 
//				SchemaConstants.C_RESULT,
//				ort), result);
//		}
//		catch (Exception e) {
//			throw new Exception("Couldn't set result for the task " + oid, e);
//		}
//
//	}

	/*
	 * Returns the content of process variables in more-or-less human readable format.
	 * Sorts variables according to their names, in order to be able to decide whether
	 * anything has changed since last event coming from the process. 
	 */
	String dumpVariables(ProcessEvent event)
	{
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		
		if (event.getAnswer() != null)
			sb.append("[answer from workflow: " + event.getAnswer() + "] ");
		
		TreeMap<String,Object> variables = new TreeMap<String,Object>();
        if (event.getVariables() != null) {
            variables.putAll(event.getVariables());
        }
//		for (WfProcessVariable var : event.getVariables())
//			if (!var.getName().startsWith("midpoint"))
//				variables.put(var.getName(), var.getValue());
		
		for (Map.Entry<String,Object> entry: variables.entrySet()) {
			if (!first)
				sb.append("; ");
			sb.append(entry.getKey() + "=" + entry.getValue());
			first = false;
		}

        return sb.toString();
	}

//    void putWorkflowAnswerIntoTask(boolean doit, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	ModelOperationStateType state = task.getModelOperationState();
//    	if (state == null)
//    		state = new ModelOperationStateType();
//
////    	state.setShouldBeExecuted(doit);
////    	task.setModelOperationStateWithPersist(state, result);
//    }

    public void setProcessWrapper(Task task, ProcessWrapper wrapper) throws SchemaException {
        PrismProperty<String> w = wfProcessWrapperPropertyDefinition.instantiate();
        w.setRealValue(wrapper.getClass().getName());
        task.setExtensionProperty(w);
    }

    public ProcessWrapper getProcessWrapper(Task task, List<ProcessWrapper> wrappers) {
        String wrapperClassName = task.getExtension(WFPROCESS_WRAPPER_PROPERTY_NAME).getRealValue(String.class);
        if (wrapperClassName == null) {
            throw new IllegalStateException("No wf process wrapper defined in task " + task);
        }

        for (ProcessWrapper w : wrappers) {
            if (wrapperClassName.equals(w.getClass().getName())) {
                return w;
            }
        }

        throw new IllegalStateException("Wrapper " + wrapperClassName + " is not registered.");
    }

    public Map<String, String> unwrapWfVariables(WfProcessInstanceEventType event) {
        Map<String,String> retval = new HashMap<String,String>();
        for (WfProcessVariable var : event.getWfProcessVariable()) {
            retval.put(var.getName(), var.getValue());
        }
        return retval;
    }

    public String getWfVariable(WfProcessInstanceEventType event, String name) {
        for (WfProcessVariable v : event.getWfProcessVariable())
            if (name.equals(v.getName()))
                return v.getValue();
        return null;
    }


    public void markRejection(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {

        ModelOperationStateType state = task.getModelOperationState();
        state.setStage(null);
        task.setModelOperationState(state);
        try {
            task.savePendingModifications(result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }

        markTaskAsClosed(task, result);         // brutal hack
    }


    public void markAcceptation(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {

        ModelOperationStateType state = task.getModelOperationState();
        ModelOperationStageType stage = state.getStage();
        switch (state.getStage()) {
            case PRIMARY: stage = ModelOperationStageType.SECONDARY; break;
            case SECONDARY: stage = ModelOperationStageType.EXECUTE; break;
            case EXECUTE: stage = null; break;      // should not occur
            default: throw new SystemException("Unknown model operation stage: " + state.getStage());
        }
        state.setStage(stage);
        task.setModelOperationState(state);
        try {
            task.savePendingModifications(result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }

        // todo continue with model operation

        markTaskAsClosed(task, result);         // brutal hack

//        switch (state.getKindOfOperation()) {
//            case ADD: modelController.addObjectContinuing(task, result); break;
//            case MODIFY: modelController.modifyObjectContinuing(task, result); break;
//            case DELETE: modelController.deleteObjectContinuing(task, result); break;
//            default: throw new SystemException("Unknown kind of model operation: " + state.getKindOfOperation());
//        }

    }

    public String getProcessId(Task task) {
        // let us request the current task status
        // todo make this property single-valued in schema to be able to use getRealValue
        PrismProperty idProp = task.getExtension(WfTaskUtil.WFPROCESSID_PROPERTY_NAME);
        Collection<String> values = null;
        if (idProp != null) {
            values = idProp.getRealValues(String.class);
        }
        if (values == null || values.isEmpty()) {
            LOGGER.error("Process ID is not known for task " + task.getName());
            return null;
        } else {
            return values.iterator().next();
            //String id = (String) idProp.getRealValue(String.class);
        }
    }
}
