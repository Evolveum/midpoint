/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
	protected PrismContext prismContext;

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

	protected String getTaskName() {
		return taskName;
	}

	protected String getTaskOperationPrefix() {
		return taskOperationPrefix;
	}

	protected TaskManager getTaskManager() {
		return taskManager;
	}

	protected RepositoryService getRepositoryService() {
		return repositoryService;
	}

	protected PrismContext getPrismContext() {
		return prismContext;
	}

	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy(!isPreserveStatistics(), isEnableIterationStatistics(),
				isEnableSynchronizationStatistics(), isEnableActionsExecutedStatistics());
	}

	@Override
    public TaskRunResult run(Task coordinatorTask) {
	    LOGGER.trace("{} run starting (coordinator task {})", taskName, coordinatorTask);
		OperationResult opResult = new OperationResult(taskOperationPrefix + ".run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		H resultHandler;
		try {
			resultHandler = createHandler(runResult, coordinatorTask, opResult);
		} catch (Throwable e) {
			LOGGER.error("{}: Error while creating a result handler: {}", taskName, e.getMessage(), e);
			opResult.recordFatalError("Error while creating a result handler: " + e.getMessage(), e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
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
			
			query = preProcessQuery(query, coordinatorTask, opResult);
			
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			logErrorAndSetResult(runResult, resultHandler, "Error while pre-processing search filter", e,
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
                    Integer expectedTotalInt = countObjects(type, query, queryOptions, coordinatorTask, opResult);
                    if (expectedTotalInt != null) {
                        expectedTotal = (long) expectedTotalInt;        // conversion would fail on null
                    }
                } else {
                    expectedTotal = (long) repositoryService.countObjects(type, query, queryOptions, opResult);
                }
                LOGGER.trace("{}: expecting {} objects to be processed", taskName, expectedTotal);
            }

            coordinatorTask.setProgress(0);
            if (expectedTotal != null) {
                coordinatorTask.setExpectedTotal(expectedTotal);
            }
            try {
                coordinatorTask.savePendingModifications(opResult);
            } catch (ObjectAlreadyExistsException e) {      // other exceptions are handled in the outer try block
                throw new IllegalStateException("Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal", e);
            }

            Collection<SelectorOptions<GetOperationOptions>> searchOptions;
	        IterationMethodType iterationMethod = getIterationMethodFromTask(coordinatorTask);
            if (iterationMethod != null) {
            	searchOptions = CloneUtil.cloneCollectionMembers(queryOptions);
            	searchOptions = SelectorOptions.updateRootOptions(searchOptions, o -> o.setIterationMethod(iterationMethod), GetOperationOptions::new);
            } else {
            	searchOptions = queryOptions;
            }

	        resultHandler.createWorkerThreads(coordinatorTask, opResult);
            if (!useRepository) {
                searchIterative((Class<O>) type, query, searchOptions, resultHandler, coordinatorTask, opResult);
            } else {
                repositoryService.searchObjectsIterative((Class<O>) type, query, resultHandler, searchOptions, false, opResult);    // TODO think about this
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

        runResult.setProgress(resultHandler.getProgress());     // TODO ?
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
	
	/**
	 * Used to count objects using model or any similar higher-level interface. Defaults to repository count.
	 */
	protected <O extends ObjectType> Integer countObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> queryOptions, Task coordinatorTask, OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		return repositoryService.countObjects(type, query, queryOptions, opResult);
	}
	
	/**
	 * Used to search using model or any similar higher-level interface. Defaults to search using repository.
	 */
	protected <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> searchOptions, ResultHandler<O> resultHandler, Object coordinatorTask, OperationResult opResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		repositoryService.searchObjectsIterative((Class<O>) type, query, resultHandler, searchOptions, false, opResult);    // TODO think about this
	}

	/**
	 * Pre-processing query (e.g. evaluate expressions).
	 */
	protected ObjectQuery preProcessQuery(ObjectQuery query, Task coordinatorTask, OperationResult opResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return query;
	}

	private TaskRunResult logErrorAndSetResult(TaskRunResult runResult, H resultHandler, String message, Throwable e,
			OperationResultStatus opStatus, TaskRunResultStatus status) {
		LOGGER.error("{}: {}: {}", taskName, message, e.getMessage(), e);
		runResult.getOperationResult().recordStatus(opStatus, message + ": " + e.getMessage(), e);
		runResult.setRunResultStatus(status);
		runResult.setProgress(resultHandler.getProgress());     // TODO ???
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


    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    /**
     * Handler parameter may be used to pass task instance state between the calls.
     */
	protected abstract ObjectQuery createQuery(H handler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) throws SchemaException;

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
			OperationResult opResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

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
    @NotNull
    protected ObjectQuery createQueryFromTask(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
	    ObjectQuery query = createQueryFromTaskIfExists(handler, runResult, task, opResult);
	    return query != null ? query : new ObjectQuery();
    }

    protected ObjectQuery createQueryFromTaskIfExists(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
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
            return null;
        }
    }

    protected QueryType getObjectQueryTypeFromTask(Task task) {
    	QueryType queryType = getObjectQueryTypeFromTaskObjectRef(task);
    	if (queryType != null) {
    		return queryType;
    	}
    	return getObjectQueryTypeFromTaskExtension(task);
    }
    
    protected QueryType getObjectQueryTypeFromTaskObjectRef(Task task) {
    	ObjectReferenceType objectRef = task.getObjectRef();
    	if (objectRef == null) {
    		return null;
    	}
    	SearchFilterType filterType = objectRef.getFilter();
    	if (filterType == null) {
    		return null;
    	}
    	QueryType queryType = new QueryType();
    	queryType.setFilter(filterType);
    	return queryType;
    }
    
    protected QueryType getObjectQueryTypeFromTaskExtension(Task task) {
    	return getRealValue(task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
    }

    protected IterationMethodType getIterationMethodFromTask(Task task) {
    	return getRealValue(task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_ITERATION_METHOD));
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
