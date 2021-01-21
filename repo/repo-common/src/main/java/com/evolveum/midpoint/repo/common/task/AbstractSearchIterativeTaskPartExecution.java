/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.util.Collection;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Single execution of a given search-iterative task part.
 *
 * TODO
 */
public abstract class AbstractSearchIterativeTaskPartExecution<O extends ObjectType,
        TH extends AbstractSearchIterativeTaskHandler<TH, TE>,
        TE extends AbstractSearchIterativeTaskExecution<TH, TE>,
        PE extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, RH>,
        RH extends AbstractSearchIterativeResultHandler<O, TH, TE, PE, RH>> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeTaskPartExecution.class);

    /**
     * The task handler. Used to access e.g. necessary Spring beans.
     */
    @NotNull protected final TH taskHandler;

    /**
     * Task execution. Maintains objects (e.g. resource definition, scan timestamps, and so on) common to all task parts.
     */
    @NotNull protected final TE taskExecution;

    /**
     * The persistent task that carries out the work.
     * It can have lightweight asynchronous worker tasks (threads), hence the name.
     *
     * TODO better name
     */
    @NotNull protected final RunningTask localCoordinatorTask;

    /**
     * Current bucket that is being processed.
     */
    @Nullable protected final WorkBucketType workBucket;

    /**
     * Definition of the task part that is being processed.
     */
    @Nullable protected final TaskPartitionDefinitionType partDefinition;

    /**
     * Result of the processing of previous work bucket. TODO what's the reason for this?
     */
    @Nullable protected final TaskWorkBucketProcessingResult previousRunResult;

    /**
     * Result of the processing of the current bucket in the current task part.
     */
    @NotNull protected final TaskWorkBucketProcessingResult runResult;

    /**
     * Result handler responsible for processing individual objects found.
     */
    protected RH resultHandler;

    protected Class<O> objectType;

    protected ObjectQuery query;

    protected Collection<SelectorOptions<GetOperationOptions>> searchOptions;

    protected boolean useRepository;

    public AbstractSearchIterativeTaskPartExecution(TE taskExecution) {
        this.taskHandler = taskExecution.taskHandler;
        this.taskExecution = taskExecution;
        this.localCoordinatorTask = taskExecution.localCoordinatorTask;
        this.workBucket = taskExecution.workBucket;
        this.partDefinition = taskExecution.partDefinition;
        this.previousRunResult = taskExecution.previousRunResult;
        this.runResult = taskExecution.getCurrentRunResult();
    }

    public @NotNull TaskWorkBucketProcessingResult run(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException,
            TaskException, ObjectAlreadyExistsException, ThresholdPolicyViolationException {
        LOGGER.trace("{} run starting: local coordinator task {}, bucket {}, previous run result {}",
                taskHandler.taskTypeName, localCoordinatorTask, workBucket, previousRunResult);

        checkTaskPersistence();

        initialize(opResult);

        objectType = determineObjectType();

        resultHandler = setupHandler(opResult);

        query = prepareQuery(opResult);
        searchOptions = prepareSearchOptions(opResult);
        useRepository = prepareUseRepositoryFlag(opResult);

        LOGGER.trace("{}: searching for {} with options {} (use repo directly: {}) and query:\n{}",
                getTaskTypeName(), objectType, searchOptions, useRepository, DebugUtil.debugDumpLazily(query));

        // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative
        Long expectedTotal = computeExpectedTotalIfApplicable(opResult);
        setExpectedTotal(expectedTotal, opResult);

        resultHandler.createWorkerThreads();
        try {
            searchIterative(opResult);
        } finally {
            resultHandler.completeProcessing(localCoordinatorTask, opResult);
        }

        // TODO
        Throwable exceptionEncountered = resultHandler.getExceptionEncountered();
        if (exceptionEncountered != null) {
            // FIXME this is a temporary hack
            if (exceptionEncountered instanceof ThresholdPolicyViolationException) {
                throw (ThresholdPolicyViolationException) exceptionEncountered;
            } else {
                return logErrorAndSetResult(runResult, opResult, resultHandler, exceptionEncountered.getMessage(), exceptionEncountered,
                        FATAL_ERROR, PERMANENT_ERROR);
            }
        }

        if (resultHandler.getErrors() > 0) {
            opResult.setStatus(PARTIAL_ERROR);
        } else {
            opResult.setStatus(SUCCESS);
        }
        // TODO: check last handler status

        runResult.setProgress(runResult.getProgress() + resultHandler.getProgress());     // TODO ?
        runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);

        if (taskHandler.getReportingOptions().isLogFinishInfo()) {
            logFinishInfo(opResult);
        }

        finish(opResult);

        LOGGER.trace("{} run finished (task {}, run result {})", getTaskTypeName(), localCoordinatorTask, runResult);
        runResult.setBucketComplete(localCoordinatorTask.canRun()); // TODO
        runResult.setShouldContinue(localCoordinatorTask.canRun()); // TODO
        return runResult;
    }

    private void setExpectedTotal(Long expectedTotal, OperationResult opResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        localCoordinatorTask.setProgress(runResult.getProgress());
        if (expectedTotal != null) {
            localCoordinatorTask.setExpectedTotal(expectedTotal);
        }
        localCoordinatorTask.flushPendingModifications(opResult);
    }

    private void logFinishInfo(OperationResult opResult) {
        String finishMessage = "Finished " + getTaskTypeName() + " (" + localCoordinatorTask + "). ";
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

        opResult.createSubresult(getTaskOperationPrefix() + ".statistics")
                .recordStatus(OperationResultStatus.SUCCESS, statistics);
        TaskHandlerUtil.appendLastFailuresInformation(getTaskOperationPrefix(), localCoordinatorTask, opResult);

        LOGGER.info("{}", finishMessage + statistics);
    }

    private void checkTaskPersistence() {
        if (localCoordinatorTask.getOid() == null) {
            throw new IllegalArgumentException(
                    "Transient tasks cannot be run by " + AbstractSearchIterativeTaskHandler.class + ": "
                            + localCoordinatorTask);
        }
    }

    private RH setupHandler(OperationResult opResult) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return createHandler(opResult);
    }

    private boolean prepareUseRepositoryFlag(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        Boolean useRepositoryDirectlyExplicit = getUseRepositoryDirectlyFromTask();
        if (useRepositoryDirectlyExplicit != null) {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (useRepositoryDirectlyExplicit) {
                checkRawAuthorization(localCoordinatorTask, opResult);
            }
            return useRepositoryDirectlyExplicit;
        } else {
            return requiresDirectRepositoryAccess(opResult);
        }
    }

    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // nothing to do here as we are in repo-common
    }

    private Collection<SelectorOptions<GetOperationOptions>> updateSearchOptionsWithIterationMethod(
            Collection<SelectorOptions<GetOperationOptions>> searchOptions) {
        Collection<SelectorOptions<GetOperationOptions>> rv;
        IterationMethodType iterationMethod = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_ITERATION_METHOD);
        if (iterationMethod != null) {
            rv = CloneUtil.cloneCollectionMembers(searchOptions);
            return SelectorOptions.updateRootOptions(rv, o -> o.setIterationMethod(iterationMethod), GetOperationOptions::new);
        } else {
            return searchOptions;
        }
    }

    @Nullable
    private Long computeExpectedTotalIfApplicable(OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (!taskHandler.getReportingOptions().isCountObjectsOnStart()) {
            return null;
        } else if (TaskWorkStateTypeUtil.hasLimitations(workBucket)) {
            // We avoid computing expected total if we are processing a bucket -- actually we could but we should
            // not display it as 'task expected total'
            return null;
        } else {
            Integer expectedTotal = countObjects(opResult);
            if (expectedTotal != null) {
                return (long) expectedTotal;
            } else {
                return null;
            }
        }
    }

    private String getTaskTypeName() {
        return taskHandler.getTaskTypeName();
    }

    private String getTaskOperationPrefix() {
        return taskHandler.getTaskOperationPrefix();
    }

    private ObjectQuery prepareQuery(OperationResult opResult) throws TaskException {

        try {
            ObjectQuery queryFromHandler = createQuery(opResult); // TODO better name

            LOGGER.trace("{}: using a query (before applying work bucket and evaluating expressions):\n{}", getTaskTypeName(),
                    DebugUtil.debugDumpLazily(queryFromHandler));

            ObjectQuery bucketNarrowedQuery = getTaskManager().narrowQueryForWorkBucket(queryFromHandler, objectType,
                    createItemDefinitionProvider(), localCoordinatorTask,
                    workBucket, opResult);

            LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}", getTaskTypeName(),
                    DebugUtil.debugDumpLazily(bucketNarrowedQuery));

            return preProcessQuery(bucketNarrowedQuery, opResult);

        } catch (Throwable t) {
            // Most probably we have nothing more to do here.
            throw new TaskException("Couldn't create object query", FATAL_ERROR, PERMANENT_ERROR, t);
        }
    }

    /**
     * Returns a provider of definitions for runtime items (e.g. attributes) that are needed in bucket filters.
     * To be implemented in subclasses that work with resource objects.
     */
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return null;
    }

    public static Function<ItemPath, ItemDefinition<?>> createItemDefinitionProviderForAttributes(
            ObjectClassComplexTypeDefinition objectClass) {
        return itemPath -> {
            if (itemPath.startsWithName(ShadowType.F_ATTRIBUTES)) {
                return objectClass.findAttributeDefinition(itemPath.rest().asSingleName());
            } else {
                return null;
            }
        };
    }

    private RepositoryService getRepositoryService() {
        return taskHandler.getRepositoryService();
    }

    /**
     * Used to count objects using model or any similar higher-level interface. Defaults to repository count.
     */
    protected Integer countObjects(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return countObjectsInRepository(opResult);
    }

    protected final int countObjectsInRepository(OperationResult opResult) throws SchemaException {
        return getRepositoryService().countObjects(objectType, query, searchOptions, opResult);
    }

    /**
     * Used to search using model or any similar higher-level interface. Defaults to search using repository.
     */
    protected void searchIterative(OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        searchIterativeInRepository(opResult);
    }

    protected final void searchIterativeInRepository(OperationResult opResult) throws SchemaException {
        getRepositoryService().searchObjectsIterative(objectType, query, resultHandler, searchOptions, true, opResult);
    }

    /**
     * Pre-processing query (e.g. evaluate expressions).
     */
    protected ObjectQuery preProcessQuery(ObjectQuery query, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return query;
    }

    // TODO fix/remove
    private TaskWorkBucketProcessingResult logErrorAndSetResult(TaskWorkBucketProcessingResult runResult, OperationResult opResult, RH resultHandler, String message, Throwable e,
            OperationResultStatus opStatus, TaskRunResult.TaskRunResultStatus status) {
        LOGGER.error("{}: {}: {}", getTaskTypeName(), message, e.getMessage(), e);
        opResult.recordStatus(opStatus, message + ": " + e.getMessage(), e);
        runResult.setRunResultStatus(status);
        if (resultHandler != null) {
            runResult.setProgress(resultHandler.getProgress());     // TODO ???
        }
        return runResult;
    }

    protected void finish(OperationResult opResult) throws SchemaException {
    }

    /**
     * Handler parameter may be used to pass task instance state between the calls.
     */
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        return createQueryFromTask();
    }

    private Collection<SelectorOptions<GetOperationOptions>> prepareSearchOptions(OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromTask = createSearchOptions(opResult);
        return updateSearchOptionsWithIterationMethod(optionsFromTask);
    }

    // useful e.g. to specify noFetch options for shadow-related queries
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
        return createSearchOptionsFromTask();
    }

    // as provisioning service does not allow searches without specifying resource or objectclass/kind, we need to be able to contact repository directly
    // for some specific tasks
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return false;
    }

    protected Class<O> determineObjectType() {
        HandledObjectType handledObjectType = this.getClass().getAnnotation(HandledObjectType.class);
        if (handledObjectType != null) {
            //noinspection unchecked
            return (Class<O>) handledObjectType.value();
        }

        Class<O> typeFromTask = getTypeFromTask();
        if (typeFromTask != null) {
            return typeFromTask;
        }

        DefaultHandledObjectType defaultHandledObjectType = this.getClass().getAnnotation(DefaultHandledObjectType.class);
        if (defaultHandledObjectType != null) {
            //noinspection unchecked
            return (Class<O>) defaultHandledObjectType.value();
        }

        throw new IllegalStateException("Type of objects to be processed is not specified");
    }

    /**
     * Creates result handler. This method should not do anything more. For initialization,
     * there are other methods.
     */
    protected @NotNull RH createHandler(OperationResult opResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return createHandlerFromAnnotation();
    }

    private @NotNull RH createHandlerFromAnnotation() {
        try {
            ResultHandlerClass annotation =
                    java.util.Objects.requireNonNull(this.getClass().getAnnotation(ResultHandlerClass.class),
                            "The @ResultHandlerClass annotation is missing.");

            //noinspection unchecked
            return (RH) InstantiationUtil.instantiate(annotation.value(), this, this);
        } catch (Throwable t) {
            throw new SystemException("Cannot create result handler from annotation of " +
                    this.getClass() + ": " + t.getMessage(), t);
        }
    }

    /**
     * Ready-made implementation of createQuery - gets and parses objectQuery extension property.
     */
    @NotNull
    protected ObjectQuery createQueryFromTask() throws SchemaException {
        ObjectQuery query = createQueryFromTaskIfExists();
        return query != null ? query : getPrismContext().queryFactory().createQuery();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptionsFromTask() {
        return MiscSchemaUtil.optionsTypeToOptions(
                getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_SEARCH_OPTIONS),
                getPrismContext());
    }

    protected Boolean getUseRepositoryDirectlyFromTask() {
        return getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY);
    }

    protected final ObjectQuery createQueryFromTaskIfExists() throws SchemaException {
        Class<? extends ObjectType> objectType = determineObjectType();
        LOGGER.trace("Object type = {}", objectType);

        QueryType queryFromTask = getObjectQueryTypeFromTask(localCoordinatorTask);
        if (queryFromTask != null) {
            ObjectQuery query = getPrismContext().getQueryConverter().createObjectQuery(objectType, queryFromTask);
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
            return taskHandler.getObjectQueryTypeFromTaskExtension(task);
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

    protected final Class<O> getTypeFromTask() {
        QName typeName = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        //noinspection unchecked
        return typeName != null
                ? (Class<O>) ObjectTypes.getObjectTypeFromTypeQName(typeName).getClassDefinition()
                : null;
    }

//    public static void logPreviousResultIfNeeded(Task task, TaskWorkBucketProcessingResult previousRunResult, Trace logger) {
//        OperationResult previousOpResult = previousRunResult.getOperationResult();
//        if (previousOpResult != null) {
//            previousOpResult.computeStatusIfUnknown();
//            if (!previousOpResult.isSuccess()) {
//                logger.warn("Last work bucket finished with status other than SUCCESS in {}:\n{}", task, previousOpResult.debugDump());
//            }
//        }
//    }

    protected ExpressionProfile getExpressionProfile() {
        // TODO Determine from task object archetype
        return MiscSchemaUtil.getExpressionProfile();
    }

    public Long heartbeat() {
        return resultHandler != null ? resultHandler.heartbeat() : null;
    }

    public PrismContext getPrismContext() {
        return taskHandler.prismContext;
    }

    public SchemaHelper getSchemaHelper() {
        return taskHandler.schemaHelper;
    }

    protected TaskManager getTaskManager() {
        return taskHandler.taskManager;
    }

    public @NotNull TH getTaskHandler() {
        return taskHandler;
    }

    protected void initialize(OperationResult opResult) throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException, TaskException {
    }

    protected void processPermanentError(OperationResult opResult, Throwable t) {
        opResult.recordFatalError(t);
        runResult.setRunResultStatus(PERMANENT_ERROR);
    }

    protected <X> X getTaskPropertyRealValue(ItemName propertyName) {
        return taskExecution.getTaskPropertyRealValue(propertyName);
    }

    public @NotNull TE getTaskExecution() {
        return taskExecution;
    }
}
