/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static java.util.Objects.requireNonNull;

/**
 * Single execution of a given search-iterative task part.
 *
 * Takes care of preparing and issuing the search query.
 *
 * *TODO finish cleanup*
 */
public abstract class AbstractSearchIterativeActivityExecution<O extends ObjectType,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        AE extends AbstractSearchIterativeActivityExecution<O, WD, AH, AE>>
    extends AbstractIterativeActivityExecution<PrismObject<O>, WD, AH> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeActivityExecution.class);

    private static final long FREE_BUCKET_WAIT_TIME = -1; // indefinitely

    /**
     * Object set specification (in the "modern" way), if present.
     * Set up in {@link #prepareItemSource(OperationResult)}.
     */
    private ObjectSetType objectSetSpecification;

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
     * Currently used to filter failed objects when `FILTER_AFTER_RETRIEVAL` mode is selected.
     *
     * See {@link AbstractSearchIterativeItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    ObjectFilter additionalFilter;

    /**
     * Pre-processes object received before it is passed to the task handler specific processing.
     * (Only for non-failed objects.) Currently used to fetch failed objects when `FETCH_FAILED_OBJECTS` mode is selected.
     *
     * See {@link AbstractSearchIterativeItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    ObjectPreprocessor<O> preprocessor;

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

    /**
     * OIDs of objects submitted to processing in current part execution (i.e. in the current bucket).
     */
    private final Set<String> oidsSeen = ConcurrentHashMap.newKeySet();

    /**
     * Current bucket that is being processed.
     * It is used to narrow the search query.
     */
    protected WorkBucketType bucket = new WorkBucketType(); // FIXME

    public AbstractSearchIterativeActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    /**
     * Bucketed version of the execution.
     */
    @Override
    protected void executeInitialized(OperationResult opResult)
            throws TaskException, PreconditionViolationException, CommonException {

        RunningTask task = taskExecution.getRunningTask();
        boolean initialExecution = true;

//        resetWorkStateAndStatisticsIfWorkComplete(result);
//        startCollectingStatistics(task, handler);

        for (; task.canRun(); initialExecution = false) {

            bucket = getWorkBucket(initialExecution, opResult);
            if (!task.canRun()) {
                break;
            }

            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
//                updateStructuredProgressOnNoMoreBuckets();
                break;
            }

            executeSingleBucket(opResult);

            if (!task.canRun()) {
                break;
            }

            completeWorkBucketAndUpdateStructuredProgress(opResult);
        }

//        task.updateAndStoreStatisticsIntoRepository(true, opResult);
    }

    private WorkBucketType getWorkBucket(boolean initialExecution, OperationResult result) {
        RunningTask task = taskExecution.getRunningTask();

        WorkBucketType bucket;
        try {
            bucket = beans.workStateManager.getWorkBucket(task, activity.getPath(),
                    activity.getDefinition().getDistributionDefinition(), FREE_BUCKET_WAIT_TIME, initialExecution, result);
        } catch (InterruptedException e) {
            LOGGER.trace("InterruptedExecution in getWorkBucket for {}", task);
            if (!task.canRun()) {
                return null;
            } else {
                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected InterruptedException in {}", e, task);
                throw new SystemException("Unexpected InterruptedException: " + e.getMessage(), e);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't allocate a work bucket for task {}", t, task);
            throw new SystemException("Couldn't allocate a work bucket for task: " + t.getMessage(), t);
        }
        return bucket;
    }

    private void completeWorkBucketAndUpdateStructuredProgress(OperationResult result) {
        RunningTask task = taskExecution.getRunningTask();
        try {
            beans.workStateManager.completeWorkBucket(task, getPath(), bucket, result);
//            task.changeStructuredProgressOnWorkBucketCompletion();
//            updateAndStoreStatisticsIntoRepository(task, result);
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RuntimeException e) {
            throw new SystemException("Couldn't complete work bucket for task: " + e.getMessage(), e);
        }
    }

    @Override
    protected void prepareItemSource(OperationResult opResult) throws TaskException, CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException {

        objectSetSpecification = getObjectSetSpecification();

        objectType = determineObjectType();
        searchOptions = prepareSearchOptions(opResult);
        useRepository = prepareUseRepositoryFlag(opResult);
        query = prepareQuery(opResult);

        LOGGER.trace("{}: searching for {} with options {} (use repo directly: {}) and query:\n{}",
                activityShortNameCapitalized, objectType, searchOptions, useRepository, DebugUtil.debugDumpLazily(query));
    }

    protected ObjectSetType getObjectSetSpecification() {
        return getObjectSetSpecificationFromWorkDefinition();
    }

    private ObjectSetType getObjectSetSpecificationFromWorkDefinition() {
        WD workDefinition = activity.getWorkDefinition();
        if (workDefinition instanceof ObjectSetSpecificationProvider) {
            return ((ObjectSetSpecificationProvider) workDefinition).getObjectSetSpecification();
        } else {
            return null;
        }
    }

    @Override
    protected void setExpectedTotal(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {
        Long expectedTotal = computeExpectedTotal(opResult);
        getTask().setExpectedTotal(expectedTotal);
        getTask().flushPendingModifications(opResult);
    }

    private boolean prepareUseRepositoryFlag(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        Boolean useRepositoryDirectlyExplicit = getUseRepositoryDirectlyFromTask();
        if (useRepositoryDirectlyExplicit != null) {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (useRepositoryDirectlyExplicit) {
                checkRawAuthorization(getTask(), opResult);
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
        IterationMethodType iterationMethod = getIterationMethod();
        Collection<SelectorOptions<GetOperationOptions>> rv;
        if (iterationMethod != null) {
            rv = CloneUtil.cloneCollectionMembers(searchOptions);
            return SelectorOptions.updateRootOptions(rv, o -> o.setIterationMethod(iterationMethod), GetOperationOptions::new);
        } else {
            return searchOptions;
        }
    }

    private IterationMethodType getIterationMethod() {
        if (objectSetSpecification != null && objectSetSpecification.getIterationMethod() != null) {
            return objectSetSpecification.getIterationMethod();
        } else {
            return getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_ITERATION_METHOD);
        }
    }

    @Nullable
    private Long computeExpectedTotal(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (!getReportingOptions().isDetermineExpectedTotal()) {
            return null;
        } else if (TaskWorkStateUtil.hasLimitations(bucket)) {
            // We avoid computing expected total if we are processing a bucket: actually we could do it,
            // but we should not display the result as 'task expected total'.
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

    private ObjectQuery prepareQuery(OperationResult opResult) throws TaskException {

        stateCheck(objectType != null, "uninitialized objectType");
        stateCheck(searchOptions != null, "uninitialized searchOptions");
        stateCheck(useRepository != null, "uninitialized useRepository");

        try {
            ObjectQuery queryFromHandler = createQuery(opResult); // TODO better name

            LOGGER.trace("{}: query as defined by task handler:\n{}", activityShortNameCapitalized,
                    DebugUtil.debugDumpLazily(queryFromHandler));

            ObjectQuery failureNarrowedQuery = narrowQueryToProcessFailedObjectsOnly(queryFromHandler); // logging is inside

            ObjectQuery bucketNarrowedQuery = beans.workStateManager.narrowQueryForWorkBucket(objectType, failureNarrowedQuery,
                    activity.getDefinition().getDistributionDefinition(), createItemDefinitionProvider(), bucket);

            LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}", activityShortNameCapitalized,
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
                    new FailedObjectsFilterCreator(selector, getTask(), getPrismContext())
                            .createFilter();

            FailedObjectsSelectionMethodType selectionMethod = getFailedObjectsSelectionMethod(selector);
            switch (selectionMethod) {
                case FETCH_FAILED_OBJECTS:
                    // We will use the narrowed query. The noFetch option ensures that search in provisioning will work.
                    // It would be nice to check if the query does not refer to attributes that are not cached.
                    // But it should be done in the provisioning module.
                    setNoFetchOption();
                    preprocessor = createShadowFetchingPreprocessor();
                    LOGGER.trace("{}: shadow-fetching preprocessor was set up", activityShortNameCapitalized);

                case NARROW_QUERY:
                    ObjectQuery failureNarrowedQuery = ObjectQueryUtil.addConjunctions(query, getPrismContext(), failedObjectsFilter);
                    LOGGER.trace("{}: query narrowed to select failed objects only:\n{}", activityShortNameCapitalized,
                            DebugUtil.debugDumpLazily(failureNarrowedQuery));
                    return failureNarrowedQuery;

                case FILTER_AFTER_RETRIEVAL:
                    additionalFilter = failedObjectsFilter;
                    LOGGER.trace("{}: will use additional filter to select failed objects only (but executes "
                                    + "the search with original query):\n{}", activityShortNameCapitalized,
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
        Collection<SelectorOptions<GetOperationOptions>> noFetch = getSchemaService().getOperationOptionsBuilder()
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

    /**
     * Used to count objects using model or any similar higher-level interface. Defaults to repository count.
     */
    protected Integer countObjects(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return countObjectsInRepository(opResult);
    }

    protected final int countObjectsInRepository(OperationResult opResult) throws SchemaException {
        return beans.repositoryService.countObjects(objectType, query, searchOptions, opResult);
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
        beans.repositoryService.searchObjectsIterative(objectType, query,
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
    protected ObjectQuery createQuery(OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        return createQueryFromTask();
    }

    private Collection<SelectorOptions<GetOperationOptions>> prepareSearchOptions(OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromHandler = createSearchOptions(opResult);
        Collection<SelectorOptions<GetOperationOptions>> updatedOptions = updateSearchOptionsWithIterationMethod(optionsFromHandler);
        return emptyIfNull(updatedOptions);
    }

    // useful e.g. to specify noFetch options for shadow-related queries
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(OperationResult opResult) {
        return createSearchOptionsFromTaskOrWorkDef();
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
        Class<O> typeFromObjectSetSpec = getTypeFromObjectSetSpecification();
        HandledObjectType handledObjectType = this.getClass().getAnnotation(HandledObjectType.class);

        if (handledObjectType != null) {
            //noinspection unchecked
            Class<O> typeFromAnnotation = (Class<O>) handledObjectType.value();
            if (typeFromObjectSetSpec != null && !typeFromAnnotation.equals(typeFromObjectSetSpec)) {
                LOGGER.warn("Object type from the object set specification ({}) is different from the hard-coded one ({}) "
                        + "and is therefore ignored.", typeFromObjectSetSpec, handledObjectType);
            }
            return typeFromAnnotation;
        }

        if (typeFromObjectSetSpec != null) {
            return typeFromObjectSetSpec;
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

    private Collection<SelectorOptions<GetOperationOptions>> createSearchOptionsFromTaskOrWorkDef() {
        SelectorQualifiedGetOptionsType optionsBean;

        if (objectSetSpecification != null && objectSetSpecification.getSearchOptions() != null) {
            optionsBean = objectSetSpecification.getSearchOptions();
        } else {
            optionsBean = getTaskContainerRealValue(SchemaConstants.MODEL_EXTENSION_SEARCH_OPTIONS);
        }

        return MiscSchemaUtil.optionsTypeToOptions(optionsBean, getPrismContext());
    }

    private Boolean getUseRepositoryDirectlyFromTask() {
        if (objectSetSpecification != null && objectSetSpecification.isUseRepositoryDirectly() != null) {
            return objectSetSpecification.isUseRepositoryDirectly();
        } else {
            return getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY);
        }
    }

    protected final ObjectQuery createQueryFromTaskIfExists() throws SchemaException {
        Class<? extends ObjectType> objectType = determineObjectType();
        LOGGER.trace("Object type = {}", objectType);

        QueryType queryFromTask = getObjectQueryTypeFromTask(getTask());
        if (queryFromTask != null) {
            ObjectQuery query = getPrismContext().getQueryConverter().createObjectQuery(objectType, queryFromTask);
            LOGGER.trace("Using object query from the task:\n{}", query.debugDumpLazily(1));
            return query;
        } else {
            return null;
        }
    }

    private QueryType getObjectQueryTypeFromTask(Task task) {
        if (objectSetSpecification != null && objectSetSpecification.getObjectQuery() != null) {
            return objectSetSpecification.getObjectQuery();
        }

        QueryType queryType = getObjectQueryTypeFromTaskObjectRef(task);
        if (queryType != null) {
            return queryType;
        }

        return getObjectQueryTypeFromTaskExtension(task);
    }

    private QueryType getObjectQueryTypeFromTaskExtension(Task task) {
        return getRealValue(task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
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

    private Class<O> getTypeFromObjectSetSpecification() {
        if (objectSetSpecification != null) {
            return getTypeFromName(objectSetSpecification.getObjectType());
        } else {
            return null;
        }
    }

    protected final Class<O> getTypeFromTask() {
        QName typeName = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        return getTypeFromName(typeName);
    }

    @Nullable
    private Class<O> getTypeFromName(QName typeName) {
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
        return beans.prismContext;
    }

    public SchemaService getSchemaService() {
        return beans.schemaService;
    }

    protected TaskManager getTaskManager() {
        return beans.taskManager;
    }

    // FIXME
    private <X> X getTaskPropertyRealValue(ItemName propertyName) {
        return getTask().getExtensionPropertyRealValue(propertyName);
    }

    @SuppressWarnings("SameParameterValue")
    private <C extends Containerable> C getTaskContainerRealValue(ItemName containerName) {
        return getTask().getExtensionContainerRealValueOrClone(containerName);
    }

    /**
     * Passes all objects found into the processing coordinator.
     * (Which processes them directly or queues them for the worker threads.)
     */
    protected final ResultHandler<O> createSearchResultHandler() {
        return (object, parentResult) -> {
            ObjectProcessingRequest<O> request = new ObjectProcessingRequest<>(object, this);
            return coordinator.submit(request, parentResult);
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        // This is a temporary solution
        return !isNonScavengingWorker();
    }

    private boolean isNonScavengingWorker() {
        return false;//FIXME
//        return localCoordinatorTask.getWorkManagement() != null &&
//                localCoordinatorTask.getWorkManagement().getTaskKind() == TaskKindType.WORKER &&
//                !Boolean.TRUE.equals(localCoordinatorTask.getWorkManagement().isScavenger());
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

    boolean checkAndRegisterOid(String oid) {
        return oidsSeen.add(oid);
    }

    @FunctionalInterface
    public interface SimpleItemProcessor<O extends ObjectType> {
        boolean processObject(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
                RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException;
    }

    protected ItemProcessor<PrismObject<O>> createDefaultItemProcessor(SimpleItemProcessor<O> simpleItemProcessor) {
        //noinspection unchecked
        return new AbstractSearchIterativeItemProcessor<>((AE) this) {
            @Override
            protected boolean processObject(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
                    RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
                return simpleItemProcessor.processObject(object, request, workerTask, result);
            }
        };
    }
}
