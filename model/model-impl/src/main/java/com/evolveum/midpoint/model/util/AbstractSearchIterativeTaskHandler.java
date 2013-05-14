/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeTaskHandler<O extends ObjectType> implements TaskHandler {
	
	private String taskName;
	private String taskOperationPrefix;
	private Class<O> type;
	
	// This is not ideal, TODO: refactor
	private Map<Task, AbstractSearchIterativeResultHandler> handlers = new HashMap<Task,AbstractSearchIterativeResultHandler>();
	
	@Autowired(required=true)
	protected ModelObjectResolver modelObjectResolver;
	
	@Autowired(required = true)
	protected PrismContext prismContext;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeTaskHandler.class);
	
	protected AbstractSearchIterativeTaskHandler(Class<O> type, String taskName, String taskOperationPrefix) {
		super();
		this.type = type;
		this.taskName = taskName;
		this.taskOperationPrefix = taskOperationPrefix;
	}

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("{} run starting (task {})", taskName, task);
		
		OperationResult opResult = new OperationResult(taskOperationPrefix + ".run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		runResult.setProgress(0);
		
		boolean cont = initialize(runResult, task, opResult);
		if (!cont) {
			// Initialization error. It should already be in runResult
			return runResult;
		}
		
		AbstractSearchIterativeResultHandler<O> handler = createHandler(runResult, task, opResult);
		if (handler == null) {
			// the error should already be in the runResult
			return runResult;
		}
		
		// TODO: error checking - already running
        handlers.put(task, handler);
        
		ObjectQuery query = createQuery(runResult, task, opResult);
		if (query == null) {
			// the error should already be in the runResult
			return runResult;
		}
		
		try {
		
			modelObjectResolver.searchIterative(type, query, handler, opResult);
			
		} catch (ObjectNotFoundException ex) {
            LOGGER.error("Import: Object not found: {}", ex.getMessage(), ex);
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError("Object not found " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (CommunicationException ex) {
            LOGGER.error("Import: Communication error: {}", ex.getMessage(), ex);
            // Error, but not critical. Just try later.
            opResult.recordPartialError("Communication error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (SchemaException ex) {
            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (RuntimeException ex) {
            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (ConfigurationException ex) {
        	LOGGER.error("Import: Configuration error: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Import: Security violation: {}", ex.getMessage(), ex);
            opResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
		}
		
		// TODO: check last handler status

        handlers.remove(task);
        opResult.computeStatus("Errors during import");
        runResult.setProgress(handler.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        String finishMessage = "Finished " + taskName + " (" + task + "). ";
        String statistics = "Processed " + handler.getProgress() + " objects, got " + handler.getErrors() + " errors.";

        opResult.createSubresult(taskOperationPrefix + ".statistics").recordStatus(OperationResultStatus.SUCCESS, statistics);

        LOGGER.info(finishMessage + statistics);
        
        finish(runResult, task, opResult);
        
        LOGGER.trace("{} run finished (task {}, run result {})", new Object[]{taskName, task, runResult});

        return runResult;
		
	}
	
	/**
	 * First method called. Can be used to initialize the task.
	 * 
	 * @return true if all OK, false if an error (error in runResult)
	 */
	protected boolean initialize(TaskRunResult runResult, Task task, OperationResult opResult) {
		return true;
	}
	
	protected void finish(TaskRunResult runResult, Task task, OperationResult opResult) {
	}

	private AbstractSearchIterativeResultHandler getHandler(Task task) {
        return handlers.get(task);
    }

    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        if (getHandler(task) != null) {
            return getHandler(task).heartbeat();
        } else {
            // most likely a race condition.
            return null;
        }
    }

    protected <T extends ObjectType> T resolveObjectRef(Class<T> type, TaskRunResult runResult, Task task, OperationResult opResult) {
    	String typeName = type.getClass().getSimpleName();
    	String objectOid = task.getObjectOid();
        if (objectOid == null) {
            LOGGER.error("Import: No {} OID specified in the task", typeName);
            opResult.recordFatalError("No "+typeName+" OID specified in the task");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        T objectType;
        try {

        	objectType = modelObjectResolver.getObject(type, objectOid, null, opResult);

        } catch (ObjectNotFoundException ex) {
            LOGGER.error("Import: {} {} not found: {}", new Object[]{typeName, objectOid, ex.getMessage(), ex});
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError(typeName+" not found " + objectOid, ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (SchemaException ex) {
            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
        } catch (RuntimeException ex) {
            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (CommunicationException ex) {
        	LOGGER.error("Import: Error getting {} {}: {}", new Object[]{typeName, objectOid, ex.getMessage(), ex});
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
		} catch (ConfigurationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", new Object[]{typeName, objectOid, ex.getMessage(), ex});
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", new Object[]{typeName, objectOid, ex.getMessage(), ex});
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		}

        if (objectType == null) {
            LOGGER.error("Import: No "+typeName+" specified");
            opResult.recordFatalError("No "+typeName+" specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }
        
        return objectType;
    }
    
    
	protected abstract ObjectQuery createQuery(TaskRunResult runResult, Task task, OperationResult opResult);

	protected abstract  AbstractSearchIterativeResultHandler<O> createHandler(TaskRunResult runResult, Task task,
			OperationResult opResult);

}
