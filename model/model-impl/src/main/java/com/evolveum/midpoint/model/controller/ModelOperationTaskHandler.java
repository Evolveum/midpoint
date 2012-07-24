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

package com.evolveum.midpoint.model.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.api.*;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ModelOperationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ModelOperationStateType;

/**
 * Handles a "ModelOperation task" - executes a given model operation in a context
 * of the task (i.e., in most cases, asynchronously).
 * 
 * The operation and its state is described in ModelOperationState element. When sent to this handler,
 * the current state of operation is unwrapped and processed by the ModelController.
 *
 * ModelOperationState consists of:
 *  - ModelOperationKindType: add, modify, delete,
 *  - ModelOperationStageType: primary, secondary, execute,
 *  - OperationData (base64-encoded serialized data structure, specific to MOKT/MOST)
 *
 * @author mederly
 */

public class ModelOperationTaskHandler implements TaskHandler {

	public static final String MODEL_OPERATION_TASK_URI = "http://evolveum.com/model-operation-task-uri";

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private ModelController model;
	
    @Autowired(required = false)
    private HookRegistry hookRegistry;

	private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = new OperationResult("ModelOperationTaskHandler.run");
		TaskRunResult runResult = new TaskRunResult();

//		/*
//		 * First, we delete ourselves from the handler stack. Normally
//		 * (asynchronous model operation will add us again, if necessary)
//		 */
//		try {
//			task.finishHandler(result);
//		} catch (ObjectNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SchemaException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		/*
		 * Now we will call the model operation body.
		 */

		ModelOperationStateType state = task.getModelOperationState();
		if (state == null) {
			String message = "The task " + task + " cannot be processed by ModelOperationTaskHandler, as it does not contain modelOperationState element."; 
			LOGGER.error(message);
			result.recordFatalError(message);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
		} else {
//			ModelOperationKindType kind = state.getKindOfOperation();
//			ModelOperationPhaseType phase = state.getPhase();
//			try {
//				switch (kind) {
//					case ADD:
//						model.addObjectWithContext(null, task, phase, result);
//						break;
//					case DELETE:
//						break;
//					case MODIFY:
//						break;
//					default:
//						break;
//				}
//				runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
//			} catch (Exception e) {
//				String message = "An exception occured within model operation " + kind + ", phase " + phase + ", in task " + task;
//				LoggingUtils.logException(LOGGER, message, e);
//				result.recordPartialError(message, e);
//				// TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
//				runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
//			}
		}

		runResult.setOperationResult(result);
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null; // null - as *not* to record progress
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @SuppressWarnings("unused")
	@PostConstruct
	private void initialize() throws Exception {
		LOGGER.info("Registering with taskManager as a handler for " + MODEL_OPERATION_TASK_URI);
		taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
	}
	
	
	
	/*
	 * ============ UTILITY METHODS ============
	 */
	
	/**
	 * Retrieves model operation context from the task.
	 * 
	 * @param methodName only for error reporting purposes
	 * @return null if context is not present or the operation is not to be executed (this is a bit of hack, should be put into separate method, perhaps)
	 */
	static Object getContext(Task task, OperationResult parentResult, String methodName) {
		
    	ModelOperationStateType state = task.getModelOperationState();

    	if (state == null) {
    		String message = "Model operation for task " + task + " does not carry ModelOperationState, exiting the " + methodName + " method.";
    		LOGGER.info(message);
    		parentResult.recordStatus(OperationResultStatus.FATAL_ERROR, message);
    		return null;
    	}
//    	if (!state.isShouldBeExecuted()) {
//    		String message = "Model operation for task " + task + " (phase " + state.getPhase() + ") is not to be continued. Exiting the " + methodName + " method.";
//    		LOGGER.info(message);
//    		parentResult.recordStatus(OperationResultStatus.SUCCESS, message);
//    		return null;
//    	}
    	
    	Object data;
    	try {
    		
        	String dataAsString = state.getOperationData();
        	LOGGER.info("DataAsString (from task new) = " + dataAsString);
        	if (dataAsString == null)
        		throw new Exception("Operation data is not present.");
    		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decodeBase64(dataAsString)));
    		data = ois.readObject();
    		if (data == null)
    			throw new Exception("Operation data is not present.");
    		
    	} catch(Exception e) {
//    		String message = "Model operation for task " + task + " (phase " + state.getPhase() + ") could not be executed: ";
//    		LoggingUtils.logException(LOGGER, message, e);
//    		parentResult.recordFatalError(message, e);
    		return null;
    	}

    	LOGGER.info("Operation data class = " + data.getClass().getName());
    	return data;
	}
	
	/*
	 * The following methods are not static, as they require the hookRegistry bean.
	 */
	
    /**
     * Executes preChangePrimary on all registered hooks.
     * Parameters (delta, task, result) are simply passed to these hooks.
     * 
     * As a bit of hack, changes current phase to SECONDARY. 
     * 
     * @return FOREGROUND, if all hooks returns FOREGROUND; BACKGROUND if not.
     * 
     * TODO in the future, maybe some error status returned from hooks should be considered here.
     * @throws ClassNotFoundException 
     * @throws java.io.IOException
     */
//    HookOperationMode executePreChangePrimary(Object context, Collection<ObjectDelta<?>> objectDeltas, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	// if we are not registered as a handler for this task, register!
//    	// (we could be registered from previous phases, therefore the check)
//    	if (!MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri()))
//    		task.pushHandlerUri(MODEL_OPERATION_TASK_URI, result);
//
//    	putContextIntoTask(context, ModelOperationPhaseType.SECONDARY, task, result);
//
//    	HookOperationMode resultMode = HookOperationMode.FOREGROUND;
//        if (hookRegistry != null) {
//        	for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
//        		HookOperationMode mode = hook.preChangePrimary(objectDeltas, task, result);
//        		if (mode == HookOperationMode.BACKGROUND)
//        			resultMode = HookOperationMode.BACKGROUND;
//        	}
//        }
//        putHookOperationModeIntoTask(resultMode, task, result);
//        return resultMode;
//    }
    
    /**
     * A convenience method when there is only one delta.
     * @throws ClassNotFoundException 
     * @throws java.io.IOException
     */
//    HookOperationMode executePreChangePrimary(Object context, ObjectDelta<?> objectDelta, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//    	Collection<ObjectDelta<?>> deltas = new ArrayList<ObjectDelta<?>>();
//    	deltas.add(objectDelta);
//    	return executePreChangePrimary(context, deltas, task, result);
//    }

    /**
     * Executes preChangeSecondary. See above for comments.
     * @throws ClassNotFoundException 
     * @throws java.io.IOException
     */
//    HookOperationMode executePreChangeSecondary(Object context, Collection<ObjectDelta<?>> objectDeltas, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	// if we are not registered as a handler for this task, register!
//    	// (we could be registered from previous phases, therefore the check)
//    	if (!MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri()))
//    		task.putHandlerAsFirst(MODEL_OPERATION_TASK_URI, result);
//
//    	putContextIntoTask(context, ModelOperationPhaseType.EXECUTE, task, result);
//
//    	HookOperationMode resultMode = HookOperationMode.FOREGROUND;
//        if (hookRegistry != null) {
//        	for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
//        		HookOperationMode mode = hook.preChangeSecondary(objectDeltas, task, result);
//        		if (mode == HookOperationMode.BACKGROUND)
//        			resultMode = HookOperationMode.BACKGROUND;
//        	}
//        }
//        putHookOperationModeIntoTask(resultMode, task, result);
//        return resultMode;
//    }
    
    /**
     * Executes postChange. See above for comments. 
     * @throws ClassNotFoundException 
     * @throws java.io.IOException
     */
//    HookOperationMode executePostChange(Object context, Collection<ObjectDelta<?>> objectDeltas, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	putContextIntoTask(context, ModelOperationPhaseType.DONE, task, result);
//
//    	HookOperationMode resultMode = HookOperationMode.FOREGROUND;
//        if (hookRegistry != null) {
//        	for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
//        		HookOperationMode mode = hook.postChange(objectDeltas, task, result);
//        		if (mode == HookOperationMode.BACKGROUND)
//        			resultMode = HookOperationMode.BACKGROUND;
//        	}
//        }
//        putHookOperationModeIntoTask(resultMode, task, result);	// actually, this call is not needed and should be considered for removal
//        return resultMode;
//    }

    /**
     * A convenience method when there is only one delta.
     * @throws ClassNotFoundException 
     * @throws java.io.IOException
     */
//    HookOperationMode executePostChange(Object context, ObjectDelta<?> objectDelta, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//    	Collection<ObjectDelta<?>> deltas = new ArrayList<ObjectDelta<?>>();
//    	deltas.add(objectDelta);
//    	return executePostChange(context, deltas, task, result);
//    }

//    private static PropertyDefinition syncContextPropertyDefinition;
//    {
//    	syncContextPropertyDefinition = new PropertyDefinition(SchemaConstants.C_TASK_SYNC_CONTEXT, DOMUtil.XSD_STRING);
//    	syncContextPropertyDefinition.setMaxOccurs(1);
//    	syncContextPropertyDefinition.setMinOccurs(0);
//    }
    
//    private void putContextIntoTask(Object context, ModelOperationPhaseType newphase, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    	ObjectOutputStream oos = new ObjectOutputStream(baos);
//    	oos.writeObject(context);
//    	String ctxAsString = Base64.encodeBase64String(baos.toByteArray());
//    	LOGGER.info("ContextAsString (serialized new) = " + ctxAsString);
////    	PropertyContainer ext = task.getExtension();
////    	Property ctx = new Property(SchemaConstants.C_TASK_SYNC_CONTEXT, syncContextPropertyDefinition);
////		ctx.setValue(new PropertyValue<Object>(ctxAsString));
////		ext.addReplaceExisting(ctx);
//
//    	ModelOperationStateType state = task.getModelOperationState();
//    	if (state == null)
//    		state = new ModelOperationStateType();
//    	state.setOperationData(ctxAsString);
//    	state.setPhase(newphase);
//    	task.setModelOperationStateWithPersist(state, result);
//    }

//    private void putHookOperationModeIntoTask(HookOperationMode mode, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	ModelOperationStateType state = task.getModelOperationState();
//    	if (state == null)
//    		state = new ModelOperationStateType();
//
//    	if (HookOperationMode.BACKGROUND.equals(mode))
//    		state.setResult(ModelOperationResultType.RUNNING_IN_BACKGROUND);
//    	else
//    		state.setResult(ModelOperationResultType.RUNNING_IN_FOREGROUND);
//
//    	task.setModelOperationStateWithPersist(state, result);
//    }
    
//    void putModelOperationResultIntoTask(ModelOperationResultType mode, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	ModelOperationStateType state = task.getModelOperationState();
//    	if (state == null)
//    		state = new ModelOperationStateType();
//
//    	state.setResult(mode);
//    	task.setModelOperationStateWithPersist(state, result);
//    }

//    void putModelOperationKindIntoTask(ModelOperationKindType kind, Task task, OperationResult result) throws IOException, ClassNotFoundException {
//
//    	ModelOperationStateType state = task.getModelOperationState();
//    	if (state == null)
//    		state = new ModelOperationStateType();
//
//    	state.setKindOfOperation(kind);
//    	task.setModelOperationStateWithPersist(state, result);
//    }

}
