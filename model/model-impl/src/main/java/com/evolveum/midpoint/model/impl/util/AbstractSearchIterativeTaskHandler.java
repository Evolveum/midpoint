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
package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.sync.TaskHandlerUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.xml.namespace.QName;
import java.util.*;

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
	private boolean logFinishInfo = false;
    private boolean countObjectsOnStart = true;         // todo make configurable per task instance (if necessary)
    private boolean preserveStatistics = true;
    private boolean enableIterationStatistics = true;   // beware, this controls whether task stores these statistics; see also recordIterationStatistics in AbstractSearchIterativeResultHandler
    private boolean enableSynchronizationStatistics = false;
    private boolean enableActionsExecutedStatistics = true;

	// If you need to store fields specific to task instance or task run the ResultHandler is a good place to do that.
	
	// This is not ideal, TODO: refactor
	// Note: The key is task OID. Originally here was the whole task, which caused occasional ConcurrentModificationException
	// when computing hashCode of TaskQuartzImpl objects (not sure why). Anyway, OIDs are better; assuming we are dealing with
	// persistent tasks only - which is obviously true.
	private Map<String, H> handlers = Collections.synchronizedMap(new HashMap<String, H>());
	
	@Autowired
	protected TaskManager taskManager;
	
	@Autowired
	protected ModelObjectResolver modelObjectResolver;

    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
	protected PrismContext prismContext;

	@Autowired
	protected SecurityEnforcer securityEnforcer;

	@Autowired
	protected ExpressionFactory expressionFactory;

	@Autowired
	protected SystemObjectCache systemObjectCache;

	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeTaskHandler.class);
	
	protected AbstractSearchIterativeTaskHandler(String taskName, String taskOperationPrefix) {
		super();
		this.taskName = taskName;
		this.taskOperationPrefix = taskOperationPrefix;
	}

	public boolean isLogFinishInfo() {
		return logFinishInfo;
	}

    public boolean isPreserveStatistics() {
        return preserveStatistics;
    }

    public boolean isEnableIterationStatistics() {
        return enableIterationStatistics;
    }

    public void setEnableIterationStatistics(boolean enableIterationStatistics) {
        this.enableIterationStatistics = enableIterationStatistics;
    }

    public boolean isEnableSynchronizationStatistics() {
        return enableSynchronizationStatistics;
    }

    public void setEnableSynchronizationStatistics(boolean enableSynchronizationStatistics) {
        this.enableSynchronizationStatistics = enableSynchronizationStatistics;
    }

    public boolean isEnableActionsExecutedStatistics() {
        return enableActionsExecutedStatistics;
    }

    public void setEnableActionsExecutedStatistics(boolean enableActionsExecutedStatistics) {
        this.enableActionsExecutedStatistics = enableActionsExecutedStatistics;
    }

    public void setPreserveStatistics(boolean preserveStatistics) {
        this.preserveStatistics = preserveStatistics;
    }

    public void setLogFinishInfo(boolean logFinishInfo) {
		this.logFinishInfo = logFinishInfo;
	}

	@Override
	public TaskRunResult run(Task coordinatorTask) {
        LOGGER.trace("{} run starting (coordinator task {})", taskName, coordinatorTask);
        if (isPreserveStatistics()) {
            coordinatorTask.startCollectingOperationStatsFromStoredValues(isEnableIterationStatistics(), isEnableSynchronizationStatistics(),
                    isEnableActionsExecutedStatistics());
        } else {
            coordinatorTask.startCollectingOperationStatsFromZero(isEnableIterationStatistics(), isEnableSynchronizationStatistics(),
                    isEnableActionsExecutedStatistics());
        }
        try {
            return runInternal(coordinatorTask);
        } finally {
            coordinatorTask.storeOperationStats();
        }
    }

    public TaskRunResult runInternal(Task coordinatorTask) {
		OperationResult opResult = new OperationResult(taskOperationPrefix + ".run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		H resultHandler;
		try {
			resultHandler = createHandler(runResult, coordinatorTask, opResult);
		} catch (SecurityViolationException|SchemaException|RuntimeException e) {
			LOGGER.error("{}: Error while creating a result handler: {}", taskName, e.getMessage(), e);
			opResult.recordFatalError("Error while creating a result handler: " + e.getMessage(), e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(coordinatorTask.getProgress());
			return runResult;
		}
		if (resultHandler == null) {
			// the error should already be in the runResult
			return runResult;
		}
        // copying relevant configuration items from task to handler
        resultHandler.setEnableIterationStatistics(isEnableIterationStatistics());
        resultHandler.setEnableSynchronizationStatistics(isEnableSynchronizationStatistics());
        resultHandler.setEnableActionsExecutedStatistics(isEnableActionsExecutedStatistics());
		
		boolean cont = initializeRun(resultHandler, runResult, coordinatorTask, opResult);
		if (!cont) {
			return runResult;
		}
		
		// TODO: error checking - already running
		if (coordinatorTask.getOid() == null) {
			throw new IllegalArgumentException("Transient tasks cannot be run by " + AbstractSearchIterativeTaskHandler.class + ": " + coordinatorTask);
		}
        handlers.put(coordinatorTask.getOid(), resultHandler);

        ObjectQuery query;
        try {
        	query = createQuery(resultHandler, runResult, coordinatorTask, opResult);
        } catch (SchemaException ex) {
			logErrorAndSetResult(runResult, resultHandler, "Schema error while creating a search filter", ex,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
        }

        if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{}: using a query (before evaluating expressions):\n{}", taskName, DebugUtil.debugDump(query));
		}

		if (query == null) {
			// the error should already be in the runResult
			return runResult;
		}

		try {
			// TODO consider which variables should go here (there's no focus, shadow, resource - only configuration)
			if (ExpressionUtil.hasExpressions(query.getFilter())) {
				PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(opResult);
				ExpressionVariables variables = Utils.getDefaultExpressionVariables(null, null, null,
						configuration != null ? configuration.asObjectable() : null);
				try {
					ExpressionEnvironment<?> env = new ExpressionEnvironment<>(coordinatorTask, opResult);
					ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
					query = ExpressionUtil.evaluateQueryExpressions(query, variables, expressionFactory,
							prismContext, "evaluate query expressions", coordinatorTask, opResult);
				} finally {
					ModelExpressionThreadLocalHolder.popExpressionEnvironment();
				}
			}
		} catch (SchemaException|ObjectNotFoundException|ExpressionEvaluationException e) {
			logErrorAndSetResult(runResult, resultHandler, "Error while evaluating expressions in a search filter", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}

		Class<? extends ObjectType> type = getType(coordinatorTask);

        Collection<SelectorOptions<GetOperationOptions>> queryOptions = createQueryOptions(resultHandler, runResult, coordinatorTask, opResult);
        boolean useRepository = useRepositoryDirectly(resultHandler, runResult, coordinatorTask, opResult);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}: searching {} with options {}, using query:\n{}", taskName, type, queryOptions, query.debugDump());
        }

        try {

            // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative
            Long expectedTotal = null;
            if (countObjectsOnStart) {
                if (!useRepository) {
                    Integer expectedTotalInt = modelObjectResolver.countObjects(type, query, queryOptions, coordinatorTask, opResult);
                    if (expectedTotalInt != null) {
                        expectedTotal = (long) expectedTotalInt;        // conversion would fail on null
                    }
                } else {
                    expectedTotal = (long) repositoryService.countObjects(type, query, opResult);
                }
                LOGGER.trace("{}: expecting {} objects to be processed", taskName, expectedTotal);
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
            if (!useRepository) {
                modelObjectResolver.searchIterative((Class<O>) type, query, queryOptions, resultHandler, coordinatorTask, opResult);
            } else {
                repositoryService.searchObjectsIterative(type, query, (ResultHandler) resultHandler, null, false, opResult);    // TODO think about this
            }
            resultHandler.completeProcessing(coordinatorTask, opResult);

        } catch (ObjectNotFoundException e) {
            // This is bad. The resource does not exist. Permanent problem.
			logErrorAndSetResult(runResult, resultHandler, "Object not found", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (CommunicationException e) {
			// Error, but not critical. Just try later.
			logErrorAndSetResult(runResult, resultHandler, "Communication error", e,
					OperationResultStatus.PARTIAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
        } catch (SchemaException e) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
			logErrorAndSetResult(runResult, resultHandler, "Error dealing with schema", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
        } catch (RuntimeException e) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
			logErrorAndSetResult(runResult, resultHandler, "Internal error", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (ConfigurationException e) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
			logErrorAndSetResult(runResult, resultHandler, "Configuration error", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
		} catch (SecurityViolationException e) {
			logErrorAndSetResult(runResult, resultHandler, "Security violation", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
		} catch (ExpressionEvaluationException e) {
			logErrorAndSetResult(runResult, resultHandler, "Expression error", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
		}

        // TODO: check last handler status

        handlers.remove(coordinatorTask.getOid());

        runResult.setProgress(resultHandler.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        if (logFinishInfo) {
	        String finishMessage = "Finished " + taskName + " (" + coordinatorTask + "). ";
	        String statistics = "Processed " + resultHandler.getProgress() + " objects in " + resultHandler.getWallTime()/1000 + " seconds, got " + resultHandler.getErrors() + " errors.";
            if (resultHandler.getProgress() > 0) {
                statistics += " Average time for one object: " + resultHandler.getAverageTime() + " milliseconds" +
                    " (wall clock time average: " + resultHandler.getWallAverageTime() + " ms).";
            }
			if (!coordinatorTask.canRun()) {
				statistics += " Task was interrupted during processing.";
			}

			opResult.createSubresult(taskOperationPrefix + ".statistics").recordStatus(OperationResultStatus.SUCCESS, statistics);
			TaskHandlerUtil.appendLastFailuresInformation(taskOperationPrefix, coordinatorTask, opResult);

			LOGGER.info("{}", finishMessage + statistics);
        }
        
        try {
        	finish(resultHandler, runResult, coordinatorTask, opResult);
        } catch (SchemaException e) {
			logErrorAndSetResult(runResult, resultHandler, "Schema error while finishing the run", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
        
        LOGGER.trace("{} run finished (task {}, run result {})", taskName, coordinatorTask, runResult);
		return runResult;
	}

	private TaskRunResult logErrorAndSetResult(TaskRunResult runResult, H resultHandler, String message, Throwable e,
			OperationResultStatus opStatus, TaskRunResultStatus status) {
		LOGGER.error("{}: {}: {}", taskName, message, e.getMessage(), e);
		runResult.getOperationResult().recordStatus(opStatus, message + ": " + e.getMessage(), e);
		runResult.setRunResultStatus(status);
		runResult.setProgress(resultHandler.getProgress());
		return runResult;

	}

	protected void finish(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
	}

	private H getHandler(Task task) {
        return handlers.get(task.getOid());
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
            LOGGER.error("Import: {} {} not found: {}", typeName, objectOid, ex.getMessage(), ex);
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
        	LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
		} catch (ConfigurationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
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

    /**
     * Handler parameter may be used to pass task instance state between the calls. 
     */
	protected abstract ObjectQuery createQuery(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException;

    // useful e.g. to specify noFetch options for shadow-related queries
    protected Collection<SelectorOptions<GetOperationOptions>> createQueryOptions(H resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return null;
    }

    // as provisioning service does not allow searches without specifying resource or objectclass/kind, we need to be able to contact repository directly
    // for some specific tasks
    protected boolean useRepositoryDirectly(H resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return false;
    }

    protected abstract Class<? extends ObjectType> getType(Task task);

    protected abstract  H createHandler(TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult) throws SchemaException, SecurityViolationException;
	
	/**
	 * Used to properly initialize the "run", which is kind of task instance. The result handler is already created at this stage.
	 * Therefore this method may be used to "enrich" the result handler with some instance-specific data. 
	 */
	protected boolean initializeRun(H handler, TaskRunResult runResult, Task task, OperationResult opResult) {
		// Nothing to do by default
		return true;
	}

    /**
     * Ready-made implementation of createQuery - gets and parses objectQuery extension property.
     */
    protected ObjectQuery createQueryFromTask(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
        Class<? extends ObjectType> objectClass = getType(task);
        LOGGER.trace("Object class = {}", objectClass);

        QueryType queryFromTask = getObjectQueryTypeFromTask(task);
        if (queryFromTask != null) {
            ObjectQuery query = QueryJaxbConvertor.createObjectQuery(objectClass, queryFromTask, prismContext);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Using object query from the task: {}", query.debugDump());
            }
            return query;
        } else {
            // Search all objects
            return new ObjectQuery();
        }
    }

    protected ModelExecuteOptions getExecuteOptionsFromTask(Task task) {
		PrismProperty<ModelExecuteOptionsType> property = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
		return property != null ? ModelExecuteOptions.fromModelExecutionOptionsType(property.getRealValue()) : null;
	}

    protected QueryType getObjectQueryTypeFromTask(Task task) {
        PrismProperty<QueryType> objectQueryPrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        if (objectQueryPrismProperty != null && objectQueryPrismProperty.getRealValue() != null) {
            return objectQueryPrismProperty.getRealValue();
        } else {
            return null;
        }
    }

    protected Class<? extends ObjectType> getTypeFromTask(Task task, Class<? extends ObjectType> defaultType) {
        Class<? extends ObjectType> objectClass;
        PrismProperty<QName> objectTypePrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        if (objectTypePrismProperty != null && objectTypePrismProperty.getRealValue() != null) {
            objectClass = ObjectTypes.getObjectTypeFromTypeQName(objectTypePrismProperty.getRealValue()).getClassDefinition();
        } else {
            objectClass = defaultType;
        }
        return objectClass;
    }

}
