/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.ExitWorkBucketHandlerException;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.task.api.WorkBucketAwareTaskHandler;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeTaskHandler<O extends ObjectType, H extends AbstractSearchIterativeResultHandler<O>>
        implements WorkBucketAwareTaskHandler {

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

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeTaskHandler.class);

    protected AbstractSearchIterativeTaskHandler(String taskName, String taskOperationPrefix) {
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
    public TaskWorkBucketProcessingResult run(RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partition, TaskWorkBucketProcessingResult previousRunResult) {
        LOGGER.trace("{} run starting: local coordinator task {}, bucket {}, previous run result {}", taskName,
                localCoordinatorTask, workBucket, previousRunResult);

//        counterManager.registerCounter(localCoordinatorTask, false);

        if (localCoordinatorTask.getOid() == null) {
            throw new IllegalArgumentException(
                    "Transient tasks cannot be run by " + AbstractSearchIterativeTaskHandler.class + ": "
                            + localCoordinatorTask);
        }

        TaskWorkBucketProcessingResult runResult = new TaskWorkBucketProcessingResult();
        runResult.setShouldContinue(false);         // overridden later if the processing is successful
        runResult.setBucketComplete(false);         // overridden later if the processing is successful
        if (previousRunResult != null) {
            runResult.setProgress(previousRunResult.getProgress());
            logPreviousResultIfNeeded(localCoordinatorTask, previousRunResult, LOGGER); // temporary
            //runResult.setOperationResult(previousRunResult.getOperationResult());
        } else {
            runResult.setProgress(0L);
            //runResult.setOperationResult(new OperationResult(taskOperationPrefix + ".run"));
        }
        // temporary measure: to prevent uncontrolled growth of operation results in tasks - we'll keep only the last one
        runResult.setOperationResult(new OperationResult(taskOperationPrefix + ".run"));
        OperationResult opResult = runResult.getOperationResult();
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);

        if (localCoordinatorTask.getChannel() == null) {
            String channel = getDefaultChannel();
            if (channel != null) {
                localCoordinatorTask.setChannel(channel);
            }
        }

        try {
            H resultHandler = setupHandler(partition, runResult, localCoordinatorTask, opResult);

            boolean cont = initializeRun(resultHandler, runResult, localCoordinatorTask, opResult);
            if (!cont) {
                return runResult;
            }

            // TODO: error checking - already running
            handlers.put(localCoordinatorTask.getOid(), resultHandler);

            Class<? extends ObjectType> type = getType(localCoordinatorTask);
            ObjectQuery query = prepareQuery(resultHandler, type, workBucket, localCoordinatorTask, runResult, opResult);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = createSearchOptions(resultHandler, runResult, localCoordinatorTask, opResult);

            LOGGER.trace("{}: searching {} with options {}, using query:\n{}", taskName, type, searchOptions, query.debugDumpLazily());

            try {
                boolean useRepository;
                Boolean useRepositoryDirectlyExplicit = getUseRepositoryDirectlyFromTask(resultHandler, runResult, localCoordinatorTask, opResult);
                if (useRepositoryDirectlyExplicit != null) {
                    // if we requested this mode explicitly we need to have appropriate authorization
                    if (useRepositoryDirectlyExplicit) {
                        checkRawAuthorization(localCoordinatorTask, opResult);
                    }
                    useRepository = useRepositoryDirectlyExplicit;
                } else {
                    useRepository = requiresDirectRepositoryAccess(resultHandler, runResult, localCoordinatorTask, opResult);
                }

                // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative
                Long expectedTotal = computeExpectedTotalIfApplicable(type, query, searchOptions, useRepository, workBucket, localCoordinatorTask, opResult);

                localCoordinatorTask.setProgress(runResult.getProgress());
                if (expectedTotal != null) {
                    localCoordinatorTask.setExpectedTotal(expectedTotal);
                }
                try {
                    localCoordinatorTask.flushPendingModifications(opResult);
                } catch (ObjectAlreadyExistsException e) {      // other exceptions are handled in the outer try block
                    throw new IllegalStateException(
                            "Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal", e);
                }

                searchOptions = updateSearchOptionsWithIterationMethod(searchOptions, localCoordinatorTask);

                resultHandler.createWorkerThreads(localCoordinatorTask);
                try {
                    if (!useRepository) {   // todo consider honoring useRepository=true within searchIterative itself
                        searchIterative((Class<O>) type, query, searchOptions, resultHandler, localCoordinatorTask, opResult);
                    } else {
                        repositoryService
                                .searchObjectsIterative((Class<O>) type, query, resultHandler, searchOptions, true, opResult);
                    }
                } finally {
                    resultHandler.completeProcessing(localCoordinatorTask, opResult);
                }

            } catch (ObjectNotFoundException e) {
                // This is bad. The resource does not exist. Permanent problem.
                return logErrorAndSetResult(runResult, resultHandler, "Object not found", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            } catch (CommunicationException e) {
                // Error, but not critical. Just try later.
                return logErrorAndSetResult(runResult, resultHandler, "Communication error", e,
                        OperationResultStatus.PARTIAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            } catch (SchemaException e) {
                // Not sure about this. But most likely it is a misconfigured resource or connector
                // It may be worth to retry. Error is fatal, but may not be permanent.
                return logErrorAndSetResult(runResult, resultHandler, "Error dealing with schema", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            } catch (RuntimeException e) {
                // Can be anything ... but we can't recover from that.
                // It is most likely a programming error. Does not make much sense to retry.
                return logErrorAndSetResult(runResult, resultHandler, "Internal error", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            } catch (ConfigurationException e) {
                // Not sure about this. But most likely it is a misconfigured resource or connector
                // It may be worth to retry. Error is fatal, but may not be permanent.
                return logErrorAndSetResult(runResult, resultHandler, "Configuration error", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            } catch (SecurityViolationException e) {
                return logErrorAndSetResult(runResult, resultHandler, "Security violation", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            } catch (ExpressionEvaluationException e) {
                return logErrorAndSetResult(runResult, resultHandler, "Expression error", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            }

            if (resultHandler.getExceptionEncountered() != null) {
                return logErrorAndSetResult(runResult, resultHandler, resultHandler.getExceptionEncountered().getMessage(), resultHandler.getExceptionEncountered(),
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            }
            // TODO: check last handler status

            handlers.remove(localCoordinatorTask.getOid());

            runResult.setProgress(runResult.getProgress() + resultHandler.getProgress());     // TODO ?
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

            if (logFinishInfo) {
                String finishMessage = "Finished " + taskName + " (" + localCoordinatorTask + "). ";
                String statistics =
                        "Processed " + resultHandler.getProgress() + " objects in " + resultHandler.getWallTime() / 1000
                                + " seconds, got " + resultHandler.getErrors() + " errors.";
                if (resultHandler.getProgress() > 0) {
                    statistics += " Average time for one object: " + resultHandler.getAverageTime() + " milliseconds" +
                            " (wall clock time average: " + resultHandler.getWallAverageTime() + " ms).";
                }
                if (!localCoordinatorTask.canRun()) {
                    statistics += " Task was interrupted during processing.";
                }

                opResult.createSubresult(taskOperationPrefix + ".statistics")
                        .recordStatus(OperationResultStatus.SUCCESS, statistics);
                TaskHandlerUtil.appendLastFailuresInformation(taskOperationPrefix, localCoordinatorTask, opResult);

                LOGGER.info("{}", finishMessage + statistics);
            }

            try {
                finish(resultHandler, runResult, localCoordinatorTask, opResult);
            } catch (SchemaException e) {
                logErrorAndSetResult(runResult, resultHandler, "Schema error while finishing the run", e,
                        OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }

            LOGGER.trace("{} run finished (task {}, run result {})", taskName, localCoordinatorTask, runResult);
            runResult.setBucketComplete(localCoordinatorTask.canRun());     // TODO
            runResult.setShouldContinue(localCoordinatorTask.canRun());     // TODO
            return runResult;
        } catch (ExitWorkBucketHandlerException e) {
            return e.getRunResult();
        }
    }

    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // nothing to do here as we are in repo-common
    }

    protected String getDefaultChannel() {
        return null;
    }

    private Collection<SelectorOptions<GetOperationOptions>> updateSearchOptionsWithIterationMethod(
            Collection<SelectorOptions<GetOperationOptions>> searchOptions, RunningTask localCoordinatorTask) {
        Collection<SelectorOptions<GetOperationOptions>> rv;
        IterationMethodType iterationMethod = getIterationMethodFromTask(localCoordinatorTask);
        if (iterationMethod != null) {
            rv = CloneUtil.cloneCollectionMembers(searchOptions);
            return SelectorOptions.updateRootOptions(rv, o -> o.setIterationMethod(iterationMethod), GetOperationOptions::new);
        } else {
            return searchOptions;
        }
    }

    @Nullable
    private Long computeExpectedTotalIfApplicable(Class<? extends ObjectType> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> queryOptions, boolean useRepository,
            WorkBucketType workBucket, Task localCoordinatorTask,
            OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (!countObjectsOnStart) {
            return null;
        } else if (TaskWorkStateTypeUtil.hasLimitations(workBucket)) {
            // We avoid computing expected total if we are processing a bucket -- actually we could but we should
            // not display it as 'task expected total'
            return null;
        } else {
            Long expectedTotal;
            if (!useRepository) {
                Integer expectedTotalInt = countObjects(type, query, queryOptions, localCoordinatorTask, opResult);
                if (expectedTotalInt != null) {
                    expectedTotal = (long) expectedTotalInt;        // conversion would fail on null
                } else {
                    expectedTotal = null;
                }
            } else {
                expectedTotal = (long) repositoryService.countObjects(type, query, queryOptions, opResult);
            }
            LOGGER.trace("{}: expecting {} objects to be processed", taskName, expectedTotal);
            return expectedTotal;
        }
    }

    private ObjectQuery prepareQuery(H resultHandler,
            Class<? extends ObjectType> type,
            WorkBucketType workBucket, Task localCoordinatorTask, TaskWorkBucketProcessingResult runResult,
            OperationResult opResult) throws ExitWorkBucketHandlerException {

        ObjectQuery queryFromHandler;
        try {
            queryFromHandler = createQuery(resultHandler, runResult, localCoordinatorTask, opResult);
        } catch (SchemaException ex) {
            throw new ExitWorkBucketHandlerException(
                    logErrorAndSetResult(runResult, resultHandler, "Schema error while creating a search filter", ex,
                            OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR));
        }

        LOGGER.trace("{}: using a query (before applying work bucket and evaluating expressions):\n{}", taskName,
                DebugUtil.debugDumpLazily(queryFromHandler));

        if (queryFromHandler == null) {
            // the error should already be in the runResult
            throw new ExitWorkBucketHandlerException(runResult);
        }

        ObjectQuery bucketNarrowedQuery;
        try {
            bucketNarrowedQuery = taskManager.narrowQueryForWorkBucket(queryFromHandler, type,
                    getIdentifierDefinitionProvider(localCoordinatorTask, opResult), localCoordinatorTask,
                    workBucket, opResult);
        } catch (SchemaException | ObjectNotFoundException e) {
            throw new ExitWorkBucketHandlerException(
                    logErrorAndSetResult(runResult, resultHandler, "Exception while narrowing a search query", e,
                            OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR));
        }

        LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}", taskName,
                DebugUtil.debugDumpLazily(bucketNarrowedQuery));

        ObjectQuery preprocessedQuery;
        try {
            preprocessedQuery = preProcessQuery(bucketNarrowedQuery, localCoordinatorTask, opResult);
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new ExitWorkBucketHandlerException(
                    logErrorAndSetResult(runResult, resultHandler, "Error while pre-processing search filter", e,
                            OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR));
        }

        return preprocessedQuery;
    }

    private H setupHandler(TaskPartitionDefinitionType partition, TaskWorkBucketProcessingResult runResult, RunningTask localCoordinatorTask, OperationResult opResult)
            throws ExitWorkBucketHandlerException {
        try {
            H resultHandler = createHandler(partition, runResult, localCoordinatorTask, opResult);
            if (resultHandler == null) {
                throw new ExitWorkBucketHandlerException(runResult);       // the error should already be in the runResult
            }
            // copying relevant configuration items from task to handler
            resultHandler.setEnableIterationStatistics(isEnableIterationStatistics());
            resultHandler.setEnableSynchronizationStatistics(isEnableSynchronizationStatistics());
            resultHandler.setEnableActionsExecutedStatistics(isEnableActionsExecutedStatistics());
            return resultHandler;
        } catch (Throwable e) {
            throw new ExitWorkBucketHandlerException(
                    logErrorAndSetResult(runResult, null, "Error while creating a result handler", e,
                    OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR));
        }
    }

    protected Function<ItemPath,ItemDefinition<?>> getIdentifierDefinitionProvider(Task localCoordinatorTask,
            OperationResult opResult) {
        return null;
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
    protected <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> searchOptions, ResultHandler<O> resultHandler, Task coordinatorTask, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        repositoryService.searchObjectsIterative(type, query, resultHandler, searchOptions, true, opResult);
    }

    /**
     * Pre-processing query (e.g. evaluate expressions).
     */
    protected ObjectQuery preProcessQuery(ObjectQuery query, Task coordinatorTask, OperationResult opResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return query;
    }

    private TaskWorkBucketProcessingResult logErrorAndSetResult(TaskWorkBucketProcessingResult runResult, H resultHandler, String message, Throwable e,
            OperationResultStatus opStatus, TaskRunResultStatus status) {
        LOGGER.error("{}: {}: {}", taskName, message, e.getMessage(), e);
        runResult.getOperationResult().recordStatus(opStatus, message + ": " + e.getMessage(), e);
        runResult.setRunResultStatus(status);
        if (resultHandler != null) {
            runResult.setProgress(resultHandler.getProgress());     // TODO ???
        }
        return runResult;

    }

    protected void finish(H handler, TaskRunResult runResult, RunningTask task, OperationResult opResult) throws SchemaException {
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
    protected ObjectQuery createQuery(H handler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) throws SchemaException {
        return createQueryFromTask(handler, runResult, coordinatorTask, opResult);
    }

    // useful e.g. to specify noFetch options for shadow-related queries
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(H resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return createSearchOptionsFromTask(resultHandler, runResult, coordinatorTask, opResult);
    }

    // as provisioning service does not allow searches without specifying resource or objectclass/kind, we need to be able to contact repository directly
    // for some specific tasks
    protected boolean requiresDirectRepositoryAccess(H resultHandler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return false;
    }

    protected abstract Class<? extends ObjectType> getType(Task task);

    protected abstract  H createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, RunningTask coordinatorTask,
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
        return query != null ? query : prismContext.queryFactory().createQuery();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptionsFromTask(H resultHandler, TaskRunResult runResult,
            Task coordinatorTask, OperationResult opResult) {
        SelectorQualifiedGetOptionsType opts = getRealValue(coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_SEARCH_OPTIONS));
        return MiscSchemaUtil.optionsTypeToOptions(opts, prismContext);
    }

    protected Boolean getUseRepositoryDirectlyFromTask(H resultHandler, TaskRunResult runResult,
            Task coordinatorTask, OperationResult opResult) {
        return getRealValue(coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY));
    }

    protected ObjectQuery createQueryFromTaskIfExists(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
        Class<? extends ObjectType> objectType = getType(task);
        LOGGER.trace("Object type = {}", objectType);

        QueryType queryFromTask = getObjectQueryTypeFromTask(task);
        if (queryFromTask != null) {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(objectType, queryFromTask);
            LOGGER.trace("Using object query from the task:\n{}", query.debugDumpLazily(1));
            return query;
        } else {
            return null;
        }
    }

    private QueryType getObjectQueryTypeFromTask(Task task) {
        QueryType queryType = getObjectQueryTypeFromTaskObjectRef(task);
        if (queryType != null) {
            return queryType;
        } else {
            return getObjectQueryTypeFromTaskExtension(task);
        }
    }

    private QueryType getObjectQueryTypeFromTaskObjectRef(Task task) {
        ObjectReferenceType objectRef = task.getObjectRefOrClone();
        if (objectRef == null) {
            return null;
        }
        SearchFilterType filterType = objectRef.getFilter();
        if (filterType == null || FilterUtil.isFilterEmpty(filterType)) {
            return null;
        }
        QueryType queryType = new QueryType();
        queryType.setFilter(filterType);
        return queryType;
    }

    protected IterationMethodType getIterationMethodFromTask(Task task) {
        return getRealValue(task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_ITERATION_METHOD));
    }

    protected Class<? extends ObjectType> getTypeFromTask(Task task, Class<? extends ObjectType> defaultType) {
        Class<? extends ObjectType> objectClass;
        PrismProperty<QName> objectTypePrismProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        if (objectTypePrismProperty != null && objectTypePrismProperty.getRealValue() != null) {
            objectClass = ObjectTypes.getObjectTypeFromTypeQName(objectTypePrismProperty.getRealValue()).getClassDefinition();
        } else {
            objectClass = defaultType;
        }
        return objectClass;
    }

    public static void logPreviousResultIfNeeded(Task task, TaskWorkBucketProcessingResult previousRunResult, Trace logger) {
        OperationResult previousOpResult = previousRunResult.getOperationResult();
        if (previousOpResult != null) {
            previousOpResult.computeStatusIfUnknown();
            if (!previousOpResult.isSuccess()) {
                logger.warn("Last work bucket finished with status other than SUCCESS in {}:\n{}", task, previousOpResult.debugDump());
            }
        }
    }

    protected ExpressionProfile getExpressionProfile() {
        // TODO Determine from task object archetype
        return MiscSchemaUtil.getExpressionProfile();
    }
}
