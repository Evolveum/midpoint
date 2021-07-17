/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static java.util.Objects.requireNonNull;

/**
 * Single execution of a given search-iterative task part.
 *
 * Takes care of preparing and issuing the search query.
 *
 * *TODO finish cleanup*
 */
public abstract class AbstractSearchIterativeActivityExecution<
        O extends ObjectType,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        AE extends AbstractSearchIterativeActivityExecution<O, WD, AH, AE, BS>,
        BS extends AbstractActivityWorkStateType>
    extends AbstractIterativeActivityExecution<PrismObject<O>, WD, AH, BS> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeActivityExecution.class);

    private static final long FREE_BUCKET_WAIT_TIME = -1; // indefinitely

    /**
     * Specification of the search that is to be executed: object type, query, options, and "use repository" flag.
     */
    protected SearchSpecification<O> searchSpecification;

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
    protected WorkBucketType bucket;

    @NotNull private final AtomicInteger sequentialNumberCounter = new AtomicInteger(0);

    public AbstractSearchIterativeActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    /**
     * Bucketed version of the execution.
     */
    @Override
    protected void executeInitialized(OperationResult opResult)
            throws ActivityExecutionException, CommonException {

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

            if (!task.canRun() || errorState.wasStoppingExceptionEncountered()) {
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
            bucket = beans.bucketingManager.getWorkBucket(task, activity.getPath(),
                    activity.getDefinition().getDistributionDefinition(), FREE_BUCKET_WAIT_TIME, initialExecution,
                    getLiveBucketManagementStatistics(), result);
            task.refresh(result); // We want to have the most current state of the running task.
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
            beans.bucketingManager.completeWorkBucket(task, getActivityPath(), bucket,
                    getLiveBucketManagementStatistics(), result);
            activityState.getLiveProgress().onCommitPoint();
            // TODO update in repository
//            task.changeStructuredProgressOnWorkBucketCompletion();
//            updateAndStoreStatisticsIntoRepository(task, result);
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RuntimeException e) {
            throw new SystemException("Couldn't complete work bucket for task: " + e.getMessage(), e);
        }
    }

    @Override
    protected void prepareItemSource(OperationResult opResult) throws ActivityExecutionException, CommonException {
        prepareSearchSpecification(opResult);
    }

    private void prepareSearchSpecification(OperationResult opResult) throws CommonException, ActivityExecutionException {
        searchSpecification = createSearchSpecification(opResult);
        customizeSearchSpecification(searchSpecification, opResult);

        narrowQueryForBucketingAndErrorHandling();
        searchSpecification.setQuery(
                evaluateQueryExpressions(searchSpecification.getQuery(), opResult));

        applyDefinitionsToQuery(opResult);

        LOGGER.trace("{}: will do the following search:\n{}",
                activityShortNameCapitalized, DebugUtil.debugDumpLazily(searchSpecification));
    }

    /**
     * Creates the search specification (type, query, options, use repo). Normally it is created from the configuration
     * embodied in work definition, but it can be fully overridden for the least configurable activities.
     *
     * Note that at repo-common level we do not support {@link ResourceObjectSetSpecificationImpl}.
     *
     * TODO Clean this up a little bit. It is quite awkward.
     */
    protected @NotNull SearchSpecification<O> createSearchSpecification(OperationResult opResult) throws SchemaException, ActivityExecutionException {
        ObjectSetSpecification objectSetSpecification = ObjectSetSpecification.fromWorkDefinition(activity.getWorkDefinition());
        if (objectSetSpecification instanceof RepositoryObjectSetSpecificationImpl) {
            return SearchSpecification.fromRepositoryObjectSetSpecification(
                    (RepositoryObjectSetSpecificationImpl) objectSetSpecification);
        } else {
            throw new UnsupportedOperationException("Non-repository object set specification is not supported: " +
                    objectSetSpecification);
        }
    }

    /**
     * Customizes the configured search specification. Almost fully delegated to the subclasses;
     * except for the "use repository" flag, which has some default behavior here.
     *
     * *BEWARE* Be careful when overriding this, because of killing use repository flag check - FIXME
     */
    @SuppressWarnings("WeakerAccess") // To allow overriding in subclasses.
    protected void customizeSearchSpecification(@NotNull SearchSpecification<O> searchSpecification, OperationResult opResult)
            throws CommonException, ActivityExecutionException {
        searchSpecification.setQuery(customizeQuery(searchSpecification.getQuery(), opResult));
        searchSpecification.setSearchOptions(customizeSearchOptions(searchSpecification.getSearchOptions(), opResult));
        searchSpecification.setUseRepository(customizeUseRepository(searchSpecification.getUseRepository(), opResult));
    }

    protected ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult opResult) throws CommonException {
        return configuredQuery;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult)
            throws CommonException {
        return configuredOptions;
    }

    /**
     * Customizes the direct use of repository. The default implementation makes use
     * of requiresDirectRepositoryAccess and also checks the authorization of direct access.
     *
     * FIXME this implementation is not good, as it allows unintentional overriding of the autz check
     */
    @SuppressWarnings("WeakerAccess") // To allow overriding in subclasses.
    protected Boolean customizeUseRepository(Boolean configuredValue, OperationResult opResult)
            throws CommonException {
        if (configuredValue != null) {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (configuredValue) {
                checkRawAuthorization(getRunningTask(), opResult);
            }
            return configuredValue;
        } else {
            return requiresDirectRepositoryAccess;
        }
    }

    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // nothing to do here as we are in repo-common
    }

    private void narrowQueryForBucketingAndErrorHandling() throws ActivityExecutionException {

        checkSearchSpecificationPresent();

        try {
            ObjectQuery queryFromHandler = searchSpecification.getQuery(); // TODO better name

            LOGGER.trace("{}: query as defined by activity:\n{}", activityShortNameCapitalized,
                    DebugUtil.debugDumpLazily(queryFromHandler));

            ObjectQuery failureNarrowedQuery = narrowQueryToProcessFailedObjectsOnly(queryFromHandler); // logging is inside

            ObjectQuery bucketNarrowedQuery = beans.bucketingManager.narrowQueryForWorkBucket(
                    getObjectType(),
                    failureNarrowedQuery,
                    activity.getDefinition().getDistributionDefinition(),
                    createItemDefinitionProvider(),
                    bucket);

            LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}",
                    activityShortNameCapitalized, DebugUtil.debugDumpLazily(bucketNarrowedQuery));

            searchSpecification.setQuery(bucketNarrowedQuery);

        } catch (Throwable t) {
            // This is most probably a permanent error. (The real communication with a resource is carried out when the
            // query is issued. With an exception of untested resource. But it is generally advised to do the connection test
            // before running any tasks.)
            throw new ActivityExecutionException("Couldn't create object query", FATAL_ERROR, PERMANENT_ERROR, t);
        }
    }

    /**
     * Besides updating the query this method influences the search process:
     *
     * In case of FETCH_FAILED_OBJECTS it
     *
     * - sets up a pre-processor that implements fetching individual shadows (that are to be re-tried) from a resource,
     * - turns on the "no fetch" option.
     *
     * In case of FILTER_AFTER_RETRIEVAL it
     *
     * - installs additional filter that passes through only selected objects (the ones that were failed).
     */
    @Nullable
    private ObjectQuery narrowQueryToProcessFailedObjectsOnly(ObjectQuery query) {
        FailedObjectsSelectorType selector = getRunningTask().getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_FAILED_OBJECTS_SELECTOR);
        if (selector == null) {
            return query;
        } else {
            ObjectFilter failedObjectsFilter =
                    new FailedObjectsFilterCreator(selector, getRunningTask(), getPrismContext())
                            .createFilter();

            FailedObjectsSelectionMethodType selectionMethod = getFailedObjectsSelectionMethod(selector);
            switch (selectionMethod) {
                case FETCH_FAILED_OBJECTS:
                    // We will use the narrowed query. The noFetch option ensures that search in provisioning will work.
                    // It would be nice to check if the query does not refer to attributes that are not cached.
                    // But it should be done in the provisioning module.
                    searchSpecification.setNoFetchOption();
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

    private @NotNull FailedObjectsSelectionMethodType getFailedObjectsSelectionMethod(FailedObjectsSelectorType selector) {
        FailedObjectsSelectionMethodType method = selector.getSelectionMethod();
        if (method != null && method != FailedObjectsSelectionMethodType.DEFAULT) {
            return method;
        } else if (searchesResourceObjects()) {
            return FailedObjectsSelectionMethodType.FETCH_FAILED_OBJECTS;
        } else {
            return FailedObjectsSelectionMethodType.NARROW_QUERY;
        }
    }

    private boolean searchesResourceObjects() {
        return searchSpecification.concernsShadows() &&
                !isUseRepository() && modelProcessingAvailable() &&
                !searchSpecification.isNoFetch() && !searchSpecification.isRaw();
    }

    @NotNull
    protected ObjectPreprocessor<O> createShadowFetchingPreprocessor() {
        throw new UnsupportedOperationException("FETCH_FAILED_OBJECTS is not available in this type of tasks. "
                + "Model processing is required.");
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
     * Evaluates expressions in query. Currently implemented only in model-impl. TODO why?
     */
    protected ObjectQuery evaluateQueryExpressions(ObjectQuery query, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return query;
    }

    /**
     * Applies definitions to query. Currently related only to provisioning-level definitions.
     * Therefore not available in repo-common.
     */
    protected void applyDefinitionsToQuery(OperationResult opResult) throws CommonException {
    }

    /**
     * TODO reconsider
     */
    @Override
    protected void setExpectedTotal(OperationResult opResult) throws CommonException {
        Long expectedTotal = computeExpectedTotal(opResult);
        getRunningTask().setExpectedTotal(expectedTotal);
        getRunningTask().flushPendingModifications(opResult);
    }

    @Nullable
    private Long computeExpectedTotal(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (!getReportingOptions().isDetermineExpectedTotal()) {
            return null;
        } else if (BucketingUtil.hasLimitations(bucket)) {
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

    /**
     * Used to count objects using model or any similar higher-level interface. Defaults to repository count.
     */
    protected Integer countObjects(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return countObjectsInRepository(opResult);
    }

    protected final int countObjectsInRepository(OperationResult opResult) throws SchemaException {
        return beans.repositoryService.countObjects(
                getObjectType(),
                getQuery(),
                getSearchOptions(),
                opResult);
    }

    @Override
    protected void processItems(OperationResult opResult) throws CommonException {
        searchIterative(opResult);
    }

    /**
     * Used to search using model or any similar higher-level interface. Defaults to search using repository.
     */
    protected void searchIterative(OperationResult opResult) throws CommonException {
        searchIterativeInRepository(opResult);
    }

    protected final void searchIterativeInRepository(OperationResult opResult) throws SchemaException {
        beans.repositoryService.searchObjectsIterative(
                getObjectType(),
                getQuery(),
                createSearchResultHandler(),
                getSearchOptions(),
                true,
                opResult);
    }

    protected boolean modelProcessingAvailable() {
        return false;
    }

    protected void setRequiresDirectRepositoryAccess() {
        this.requiresDirectRepositoryAccess = true;
    }

    protected ExpressionProfile getExpressionProfile() {
        // TODO Determine from task object archetype
        return MiscSchemaUtil.getExpressionProfile();
    }

    public SchemaService getSchemaService() {
        return beans.schemaService;
    }

    protected TaskManager getTaskManager() {
        return beans.taskManager;
    }

    /**
     * Passes all objects found into the processing coordinator.
     * (Which processes them directly or queues them for the worker threads.)
     */
    protected final ResultHandler<O> createSearchResultHandler() {
        return (object, parentResult) -> {
            ObjectProcessingRequest<O> request =
                    new ObjectProcessingRequest<>(sequentialNumberCounter.getAndIncrement(), object, this);
            return coordinator.submit(request, parentResult);
        };
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        // This is a temporary solution
        return !isNonScavengingWorker();
    }

    private boolean isNonScavengingWorker() {
        return activityState.isWorker() && !activityState.isScavenger();
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        // This is the default for search-iterative tasks. It is a legacy behavior, and also the most logical:
        // we do not need to stop on error, because there's always possible to re-run the whole task.
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    boolean checkAndRegisterOid(String oid) {
        return oidsSeen.add(oid);
    }

    @FunctionalInterface
    public interface SimpleItemProcessor<O extends ObjectType> {
        boolean processObject(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
                RunningTask workerTask, OperationResult result) throws CommonException, ActivityExecutionException;
    }

    protected final ItemProcessor<PrismObject<O>> createDefaultItemProcessor(SimpleItemProcessor<O> simpleItemProcessor) {
        //noinspection unchecked
        return new AbstractSearchIterativeItemProcessor<>((AE) this) {
            @Override
            protected boolean processObject(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
                    RunningTask workerTask, OperationResult result) throws CommonException, ActivityExecutionException {
                return simpleItemProcessor.processObject(object, request, workerTask, result);
            }
        };
    }

    @Override
    protected boolean hasProgressCommitPoints() {
        return true;
    }

    private ActivityBucketManagementStatistics getLiveBucketManagementStatistics() {
        return activityState.getLiveStatistics().getLiveBucketManagement();
    }

    public @NotNull SearchSpecification<O> getSearchSpecificationRequired() {
        return requireNonNull(searchSpecification, "no search specification");
    }

    private void checkSearchSpecificationPresent() {
        stateCheck(searchSpecification != null, "no search specification");
    }

    public final Class<O> getObjectType() {
        return getSearchSpecificationRequired().getObjectType();
    }

    public final ObjectQuery getQuery() {
        return getSearchSpecificationRequired().getQuery();
    }

    public final Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return getSearchSpecificationRequired().getSearchOptions();
    }

    public final boolean isUseRepository() {
        return Boolean.TRUE.equals(searchSpecification.getUseRepository());
    }
}
