/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import static com.evolveum.midpoint.util.MiscUtil.*;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

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
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Single execution of a given search-iterative task part.
 *
 * Takes care of preparing and issuing the search query.
 *
 * *TODO finish cleanup*
 */
public abstract class AbstractSearchIterativeTaskPartExecution<O extends ObjectType,
        TH extends AbstractTaskHandler<TH, TE>,
        TE extends AbstractTaskExecution<TH, TE>,
        PE extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, RH>,
        RH extends AbstractSearchIterativeItemProcessor<O, TH, TE, PE, RH>>
    extends AbstractIterativeTaskPartExecution<PrismObject<O>, TH, TE, PE, RH> {

    /**
     * Current bucket that is being processed.
     * It is used to narrow the search query.
     */
    @Nullable protected final WorkBucketType workBucket;

    /**
     * Object type provided when counting and retrieving objects. Set up in {@link #prepareItemSource(OperationResult)}.
     *
     * Never null after initialization.
     */
    protected Class<O> objectType;

    /** Object query specifying what objects to process. Set up in {@link #prepareItemSource(OperationResult)}. */
    protected ObjectQuery query;

    /**
     * Additional filter to be applied to each object received (if not null).
     * Currently used to filter failed objects when FILTER_AFTER_RETRIEVAL mode is selected.
     *
     * See {@link AbstractSearchIterativeItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    protected ObjectFilter additionalFilter;

    /**
     * Pre-processes object received before it is passed to the task handler specific processing.
     * (Only for non-failed objects.) Currently used to fetch failed objects when FETCH_FAILED_OBJECTS mode is selected.
     *
     * See {@link AbstractSearchIterativeItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    protected ObjectPreprocessor<O> preprocessor;

    /**
     * Options to be used during counting and searching. Set up in {@link #prepareItemSource(OperationResult)}.
     *
     * Never null after initialization.
     */
    protected Collection<SelectorOptions<GetOperationOptions>> searchOptions;

    /**
     * Whether we want to use repository directly when counting/searching. Set up in {@link #prepareItemSource(OperationResult)}.
     * Can be "built-in" in the task (see {@link #requiresDirectRepositoryAccess}), or requested explicitly by the user.
     * In the latter case the raw authorization is checked.
     *
     * Note that this flag is really used only if {@link #modelProcessingAvailable()} is true.
     *
     * Never null after initialization.
     */
    protected Boolean useRepository;

    /**
     * In some situations (e.g. because provisioning service does not allow searches without specifying resource
     * or objectclass/kind) we need to use repository directly for some specific tasks or task parts.
     */
    private boolean requiresDirectRepositoryAccess;

    public AbstractSearchIterativeTaskPartExecution(TE taskExecution) {
        super(taskExecution);
        this.workBucket = taskExecution.workBucket;
    }

    @Override
    protected void prepareItemSource(OperationResult opResult) throws TaskException, CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException {
        objectType = determineObjectType();
        searchOptions = prepareSearchOptions(opResult);
        useRepository = prepareUseRepositoryFlag(opResult);
        query = prepareQuery(opResult);

        logger.trace("{}: searching for {} with options {} (use repo directly: {}) and query:\n{}",
                getTaskTypeName(), objectType, searchOptions, useRepository, DebugUtil.debugDumpLazily(query));
    }

    @Override
    protected void setProgressAndExpectedItems(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {
        Long expectedTotal = computeExpectedTotalIfApplicable(opResult);
        setProgressAndExpectedTotal(expectedTotal, opResult);
    }

    private void setProgressAndExpectedTotal(Long expectedTotal, OperationResult opResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        localCoordinatorTask.setProgress(runResult.getProgress());
        if (expectedTotal != null) {
            localCoordinatorTask.setExpectedTotal(expectedTotal);
        }
        localCoordinatorTask.flushPendingModifications(opResult);
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
        if (!getReportingOptions().isCountObjectsOnStart()) {
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

    private ObjectQuery prepareQuery(OperationResult opResult) throws TaskException {

        stateCheck(objectType != null, "uninitialized objectType");
        stateCheck(searchOptions != null, "uninitialized searchOptions");
        stateCheck(useRepository != null, "uninitialized useRepository");

        try {
            ObjectQuery queryFromHandler = createQuery(opResult); // TODO better name

            logger.trace("{}: query as defined by task handler:\n{}", getTaskTypeName(),
                    DebugUtil.debugDumpLazily(queryFromHandler));

            ObjectQuery failureNarrowedQuery = narrowQueryToProcessFailedObjectsOnly(queryFromHandler); // logging is inside

            ObjectQuery bucketNarrowedQuery = getTaskManager().narrowQueryForWorkBucket(failureNarrowedQuery, objectType,
                    createItemDefinitionProvider(), localCoordinatorTask,
                    workBucket, opResult);

            logger.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}", getTaskTypeName(),
                    DebugUtil.debugDumpLazily(bucketNarrowedQuery));

            return preProcessQuery(bucketNarrowedQuery, opResult);

        } catch (Throwable t) {
            // Most probably we have nothing more to do here.
            throw new TaskException("Couldn't create object query", FATAL_ERROR, PERMANENT_ERROR, t);
        }
    }

    @Nullable
    private ObjectQuery narrowQueryToProcessFailedObjectsOnly(ObjectQuery query) {
        FailedObjectsSelectorType selector = getTaskContainerRealValue(SchemaConstants.MODEL_EXTENSION_FAILED_OBJECTS_SELECTOR);
        if (selector == null) {
            return query;
        } else {
            ObjectFilter failedObjectsFilter =
                    new FailedObjectsFilterCreator(selector, this, getPrismContext())
                            .createFilter();

            FailedObjectsSelectionMethodType selectionMethod = getFailedObjectsSelectionMethod(selector);
            switch (selectionMethod) {
                case FETCH_FAILED_OBJECTS:
                    // We will use the narrowed query. The noFetch option ensures that search in provisioning will work.
                    // It would be nice to check if the query does not refer to attributes that are not cached.
                    // But it should be done in the provisioning module.
                    setNoFetchOption();
                    preprocessor = createShadowFetchingPreprocessor();
                    logger.trace("{}: shadow-fetching preprocessor was set up", getTaskTypeName());

                case NARROW_QUERY:
                    ObjectQuery failureNarrowedQuery = ObjectQueryUtil.addConjunctions(query, getPrismContext(), failedObjectsFilter);
                    logger.trace("{}: query narrowed to select failed objects only:\n{}", getTaskTypeName(),
                            DebugUtil.debugDumpLazily(failureNarrowedQuery));
                    return failureNarrowedQuery;

                case FILTER_AFTER_RETRIEVAL:
                    additionalFilter = failedObjectsFilter;
                    logger.trace("{}: will use additional filter to select failed objects only (but executes "
                                    + "the search with original query):\n{}", getTaskTypeName(),
                            DebugUtil.debugDumpLazily(failedObjectsFilter));
                    return query;

                default:
                    throw new AssertionError(selectionMethod);
            }
        }
    }

    @NotNull
    protected ObjectPreprocessor<O> createShadowFetchingPreprocessor() {
        throw new UnsupportedOperationException("FETCH_FAILED_OBJECTS is not available in this type of tasks. "
                + "Model processing is required.");
    }

    private void setNoFetchOption() {
        stateCheck(searchOptions != null, "uninitialized searchOptions");
        Collection<SelectorOptions<GetOperationOptions>> noFetch = getSchemaHelper().getOperationOptionsBuilder()
                .noFetch()
                .build();
        searchOptions = GetOperationOptions.merge(getPrismContext(), searchOptions, noFetch);
    }

    private @NotNull FailedObjectsSelectionMethodType getFailedObjectsSelectionMethod(FailedObjectsSelectorType selector) {
        FailedObjectsSelectionMethodType method = selector.getSelectionMethod();
        if (method != null && method != FailedObjectsSelectionMethodType.DEFAULT) {
            return method;
        } if (useRepository || !modelProcessingAvailable() || !ShadowType.class.equals(requireNonNull(objectType))) {
            return FailedObjectsSelectionMethodType.NARROW_QUERY;
        } else {
            return FailedObjectsSelectionMethodType.FETCH_FAILED_OBJECTS;
        }
    }

    /**
     * Returns a provider of definitions for runtime items (e.g. attributes) that are needed in bucket filters.
     * To be implemented in subclasses that work with resource objects.
     */
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return null;
    }

    protected final Function<ItemPath, ItemDefinition<?>> createItemDefinitionProviderForAttributes(
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

    @Override
    protected void processItems(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        searchIterative(opResult);
    }

    /**
     * Used to search using model or any similar higher-level interface. Defaults to search using repository.
     */
    protected void searchIterative(OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        searchIterativeInRepository(opResult);
    }

    protected final void searchIterativeInRepository(OperationResult opResult) throws SchemaException {
        getRepositoryService().searchObjectsIterative(objectType, query,
                createSearchResultHandler(), searchOptions, true, opResult);
    }

    protected boolean modelProcessingAvailable() {
        return false;
    }

    /**
     * Pre-processing query (e.g. evaluate expressions).
     */
    protected ObjectQuery preProcessQuery(ObjectQuery query, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return query;
    }

    /**
     * Handler parameter may be used to pass task instance state between the calls.
     */
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        return createQueryFromTask();
    }

    private Collection<SelectorOptions<GetOperationOptions>> prepareSearchOptions(OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromHandler = createSearchOptions(opResult);
        Collection<SelectorOptions<GetOperationOptions>> updatedOptions = updateSearchOptionsWithIterationMethod(optionsFromHandler);
        return emptyIfNull(updatedOptions);
    }

    // useful e.g. to specify noFetch options for shadow-related queries
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
        return createSearchOptionsFromTask();
    }

    /**
     * Returns true if the task processing requires direct access to the repository "by design". In such cases,
     * the "raw" authorization is not checked.
     *
     * The default implementation relies on the value of the corresponding field.
     * In theory, subclasses can plug their own "dynamic" implementation here.
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
        return requiresDirectRepositoryAccess;
    }

    protected void setRequiresDirectRepositoryAccess() {
        this.requiresDirectRepositoryAccess = true;
    }

    @NotNull
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

    private Boolean getUseRepositoryDirectlyFromTask() {
        return getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY);
    }

    protected final ObjectQuery createQueryFromTaskIfExists() throws SchemaException {
        Class<? extends ObjectType> objectType = determineObjectType();
        logger.trace("Object type = {}", objectType);

        QueryType queryFromTask = getObjectQueryTypeFromTask(localCoordinatorTask);
        if (queryFromTask != null) {
            ObjectQuery query = getPrismContext().getQueryConverter().createObjectQuery(objectType, queryFromTask);
            logger.trace("Using object query from the task:\n{}", query.debugDumpLazily(1));
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

    protected ExpressionProfile getExpressionProfile() {
        // TODO Determine from task object archetype
        return MiscSchemaUtil.getExpressionProfile();
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

    private <X> X getTaskPropertyRealValue(ItemName propertyName) {
        return taskExecution.getTaskPropertyRealValue(propertyName);
    }

    @SuppressWarnings("SameParameterValue")
    private <C extends Containerable> C getTaskContainerRealValue(ItemName containerName) {
        return taskExecution.getTaskContainerRealValue(containerName);
    }

    /**
     * Passes all objects found into the processing coordinator.
     * (Which processes them directly or queues them for the worker threads.)
     */
    protected final ResultHandler<O> createSearchResultHandler() {
        return (object, parentResult) -> {
            ItemProcessingRequest<PrismObject<O>> request = new ObjectProcessingRequest<>(object, itemProcessor);
            return coordinator.submit(request, parentResult);
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        // This is a temporary solution
        return !isNonScavengingWorker();
    }

    private boolean isNonScavengingWorker() {
        return localCoordinatorTask.getWorkManagement() != null &&
                localCoordinatorTask.getWorkManagement().getTaskKind() == TaskKindType.WORKER &&
                !Boolean.TRUE.equals(localCoordinatorTask.getWorkManagement().isScavenger());
    }

    @Override
    protected ErrorHandlingStrategyExecutor.@NotNull Action getDefaultErrorAction() {
        // This is the default for search-iterative tasks. It is a legacy behavior, and also the most logical:
        // we do not need to stop on error, because there's always possible to re-run the whole task.
        return ErrorHandlingStrategyExecutor.Action.CONTINUE;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return searchOptions;
    }
}
