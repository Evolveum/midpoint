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
package com.evolveum.midpoint.model.impl.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeTaskHandler<O extends ObjectType, H extends AbstractSearchIterativeResultHandler<O>> implements TaskHandler {
	
	// WARNING! This task handler is efficiently singleton!
	// It is a spring bean and it is supposed to handle all search task instances
	// Therefore it must not have task-specific fields. It can only contain fields specific to
	// all tasks of a specified type
	private String taskName;
	private String taskOperationPrefix;
	private Class<O> type;
	private boolean logFinishInfo = false;
    private boolean countObjectsOnStart = true;         // todo make configurable per task instance (if necessary)
	
	// If you need to store fields specific to task instance or task run the ResultHandler is a good place to do that.
	
	// This is not ideal, TODO: refactor
	private Map<Task, H> handlers = Collections.synchronizedMap(new HashMap<Task, H>());
	
	@Autowired(required=true)
	protected TaskManager taskManager;
	
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
	
	public boolean isLogFinishInfo() {
		return logFinishInfo;
	}

	public void setLogFinishInfo(boolean logFinishInfo) {
		this.logFinishInfo = logFinishInfo;
	}

	@Override
	public TaskRunResult run(Task coordinatorTask) {
		LOGGER.trace("{} run starting (coordinator task {})", taskName, coordinatorTask);
		
		OperationResult opResult = new OperationResult(taskOperationPrefix + ".run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		H resultHandler = createHandler(runResult, coordinatorTask, opResult);
		if (resultHandler == null) {
			// the error should already be in the runResult
			return runResult;
		}
		
		boolean cont = initializeRun(resultHandler, runResult, coordinatorTask, opResult);
		if (!cont) {
			return runResult;
		}
		
		// TODO: error checking - already running
        handlers.put(coordinatorTask, resultHandler);
        
        ObjectQuery query;
        try {
        	query = createQuery(resultHandler, runResult, coordinatorTask, opResult);
        } catch (SchemaException ex) {
        	LOGGER.error("{}: Schema error while creating a search filter: {}", new Object[]{taskName, ex.getMessage(), ex});
            opResult.recordFatalError("Schema error while creating a search filter: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        }
        
		if (query == null) {
			// the error should already be in the runResult
			return runResult;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{}: searching using query:\n{}", taskName, query.debugDump());
		}

		try {

            // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative
            Long expectedTotal = null;
            if (countObjectsOnStart) {
                Integer expectedTotalInt = modelObjectResolver.countObjects(type, query, opResult);
                LOGGER.trace("{}: expecting {} objects to be processed", taskName, expectedTotal);
                if (expectedTotalInt != null) {
                    expectedTotal = (long) expectedTotalInt;        // conversion would fail on null
                }
            }

            runResult.setProgress(0);
            coordinatorTask.setProgress(0);
            if (expectedTotal != null) {
                coordinatorTask.setExpectedTotal(expectedTotal);
            }
            try {
                coordinatorTask.savePendingModifications(opResult);
            } catch (ObjectAlreadyExistsException e) {      // other exceptions are handled in the outer try block
                throw new IllegalStateException("Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal", e);
            }

            resultHandler.createWorkerThreads(coordinatorTask, opResult);
            modelObjectResolver.searchIterative(type, query, null, resultHandler, opResult);
            resultHandler.completeProcessing(opResult);

        } catch (ObjectNotFoundException ex) {
            LOGGER.error("{}: Object not found: {}", new Object[]{taskName, ex.getMessage(), ex});
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError("Object not found " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        } catch (CommunicationException ex) {
            LOGGER.error("{}: Communication error: {}", new Object[]{taskName, ex.getMessage(), ex});
            // Error, but not critical. Just try later.
            opResult.recordPartialError("Communication error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        } catch (SchemaException ex) {
            LOGGER.error("{}: Error dealing with schema: {}", new Object[]{taskName, ex.getMessage(), ex});
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        } catch (RuntimeException ex) {
            LOGGER.error("{}: Internal Error: {}", new Object[]{taskName, ex.getMessage(), ex});
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        } catch (ConfigurationException ex) {
        	LOGGER.error("{}: Configuration error: {}", new Object[]{taskName, ex.getMessage(), ex});
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("{}: Security violation: {}", new Object[]{taskName, ex.getMessage(), ex});
            opResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
		}

        // TODO: check last handler status

        handlers.remove(coordinatorTask);

        runResult.setProgress(resultHandler.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        if (logFinishInfo) {
	        String finishMessage = "Finished " + taskName + " (" + coordinatorTask + "). ";
	        String statistics = "Processed " + resultHandler.getProgress() + " objects in " + resultHandler.getWallTime()/1000 + " seconds, got " + resultHandler.getErrors() + " errors.";
            if (resultHandler.getProgress() > 0) {
                statistics += " Average time for one object: " + resultHandler.getAverageTime() + " milliseconds" +
                    " (wall clock time average: " + resultHandler.getWallAverageTime() + " ms).";
            }
	
	        opResult.createSubresult(taskOperationPrefix + ".statistics").recordStatus(OperationResultStatus.SUCCESS, statistics);
	
	        LOGGER.info(finishMessage + statistics);
        }
        
        try {
        	finish(resultHandler, runResult, coordinatorTask, opResult);
        } catch (SchemaException ex) {
        	LOGGER.error("{}: Schema error while finishing the run: {}", new Object[]{taskName, ex.getMessage(), ex});
            opResult.recordFatalError("Schema error while finishing the run: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(resultHandler.getProgress());
            return runResult;
        }
        
        LOGGER.trace("{} run finished (task {}, run result {})", new Object[]{taskName, coordinatorTask, runResult});

        return runResult;
		
	}

    protected void finish(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
	}

	private H getHandler(Task task) {
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

        	objectType = modelObjectResolver.getObject(type, objectOid, null, task, opResult);

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
    
    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    // meant to be used in specific createQuery methods; it is not used here directly, because QueryType->ObjectQuery conversion
    // requires information on object class, and this is specific to individual subclasses (handlers)
    protected QueryType getObjectQueryTypeFromTask(Task task) {
        PrismProperty<QueryType> objectQueryPrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        if (objectQueryPrismProperty != null && objectQueryPrismProperty.getRealValue() != null) {
            return objectQueryPrismProperty.getRealValue();
        } else {
            return null;
        }
    }

    /**
     * Handler parameter may be used to pass task instance state between the calls. 
     */
	protected abstract ObjectQuery createQuery(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException;

	protected abstract  H createHandler(TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult);
	
	/**
	 * Used to properly initialize the "run", which is kind of task instance. The result handler is already created at this stage.
	 * Therefore this method may be used to "enrich" the result handler with some instance-specific data. 
	 */
	protected boolean initializeRun(H handler, TaskRunResult runResult, Task task, OperationResult opResult) {
		// Nothing to do by default
		return true;
	}

}
