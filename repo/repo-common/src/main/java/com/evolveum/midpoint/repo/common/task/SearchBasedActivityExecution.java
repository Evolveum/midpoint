/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.NOT_APPLICABLE;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import static java.util.Objects.requireNonNull;

/**
 * Execution of a given search-iterative task part.
 *
 * Responsibilities:
 *
 * 1. Bucketing, i.e. orchestrating getting and processing buckets. See {@link #doExecute(OperationResult)}.
 *
 * 2. Item source preparation = preparation of search specification ({@link #prepareSearchSpecification(OperationResult)}).
 * This includes
 *
 *   a. converting the configured object set to search spec (or obtaining the spec from
 *   the specifics object - {@link SearchBasedActivityExecutionSpecifics};
 *   b. customizing the spec by calling `customizeXXX` methods in the specifics object;
 *   c. narrowing the query for bucketing and error handling.
 *
 * 3. "Expected total" determination - see {@link #setExpectedTotal(OperationResult)}.
 *
 * 4. Pre-processing of items found - see {@link #processItem(ItemProcessingRequest, RunningTask, OperationResult)}:
 *
 *   a. checking for already-processed objects (OIDs seen),
 *   b. applying additional filter (currently used for retrying failed objects),
 *   c. applying additional pre-processing to objects (currently used for retrying failed objects),
 */
public class SearchBasedActivityExecution<
        O extends ObjectType,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends IterativeActivityExecution<
            PrismObject<O>,
            WD,
            AH,
            WS,
            SearchBasedActivityExecution<O, WD, AH, ?>,
            SearchBasedActivityExecutionSpecifics<O>> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBasedActivityExecution.class);

    private static final String OP_PREPROCESS_OBJECT = SearchBasedActivityExecution.class.getName() + ".preprocessObject";


    /**
     * Specification of the search that is to be executed: object type, query, options, and "use repository" flag.
     */
    protected SearchSpecification<O> searchSpecification;

    /**
     * Additional filter to be applied to each object received (if not null).
     * Currently used to filter failed objects when `FILTER_AFTER_RETRIEVAL` mode is selected.
     *
     * See {@link #processItem(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    private ObjectFilter additionalFilter;

    /**
     * Pre-processes object received before it is passed to the task handler specific processing.
     * (Only for non-failed objects.) Currently used to fetch failed objects when `FETCH_FAILED_OBJECTS`
     * failed selector mode is selected.
     *
     * See {@link #processItem(ItemProcessingRequest, RunningTask, OperationResult)}.
     */
    private ObjectPreprocessor<O> preprocessor;

    /**
     * OIDs of objects submitted to processing in current part execution (i.e. in the current bucket).
     */
    private final Set<String> oidsSeen = ConcurrentHashMap.newKeySet();

    @NotNull private final AtomicInteger sequentialNumberCounter = new AtomicInteger(0);

    public SearchBasedActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized,
            @NotNull SearchBasedActivityExecution.SearchBasedSpecificsSupplier<O, WD, AH> specificsSupplier) {
        super(context, shortNameCapitalized, specificsSupplier);
    }

    @Override
    protected void prepareItemSource(OperationResult result) throws ActivityExecutionException, CommonException {
        prepareSearchSpecification(result);
    }

    private void prepareSearchSpecification(OperationResult result) throws CommonException, ActivityExecutionException {
        searchSpecification = createSearchSpecification(result);
        customizeSearchSpecification(result);

        narrowQueryForBucketingAndErrorHandling();
        searchSpecification.setQuery(
                evaluateQueryExpressions(searchSpecification.getQuery(), result));

        applyDefinitionsToQuery(result);

        LOGGER.trace("{}: will do the following search:\n{}",
                shortName, DebugUtil.debugDumpLazily(searchSpecification));
    }

    /**
     * Creates the search specification (type, query, options, use repo). Normally it is created from the configuration
     * embodied in work definition, but it can be fully overridden for the least configurable activities.
     */
    private @NotNull SearchSpecification<O> createSearchSpecification(OperationResult result)
            throws SchemaException, ActivityExecutionException {
        SearchSpecification<O> fromPlugin = executionSpecifics.createSearchSpecification(result);
        if (fromPlugin != null) {
            return fromPlugin;
        }

        return createSearchSpecificationFromObjectSetSpec(
                ObjectSetSpecification.fromWorkDefinition(activity.getWorkDefinition()),
                result);
    }

    /**
     * Converts {@link ObjectSetSpecification} into {@link SearchSpecification}.
     *
     * (Note that at repo-common level we do not support {@link ResourceObjectSetSpecificationImpl}.
     * This is implemented in the model-level subclass.)
     */
    protected @NotNull SearchSpecification<O> createSearchSpecificationFromObjectSetSpec(
            @NotNull ObjectSetSpecification objectSetSpecification, OperationResult result)
            throws SchemaException, ActivityExecutionException {
        if (objectSetSpecification instanceof RepositoryObjectSetSpecificationImpl) {
            return SearchSpecification.fromRepositoryObjectSetSpecification(
                    (RepositoryObjectSetSpecificationImpl) objectSetSpecification);
        } else {
            throw new UnsupportedOperationException("Non-repository object set specification is not supported: " +
                    objectSetSpecification);
        }
    }

    /**
     * Customizes the configured search specification. Almost fully delegated to the plugin.
     */
    private void customizeSearchSpecification(OperationResult opResult) throws CommonException {
        searchSpecification.setQuery(
                executionSpecifics.customizeQuery(searchSpecification.getQuery(), opResult));

        searchSpecification.setSearchOptions(
                executionSpecifics.customizeSearchOptions(searchSpecification.getSearchOptions(), opResult));

        searchSpecification.setUseRepository(
                customizeUseRepository(searchSpecification.getUseRepository(), opResult));
    }

    private Boolean customizeUseRepository(Boolean configuredValue, OperationResult opResult)
            throws CommonException {
        if (executionSpecifics.doesRequireDirectRepositoryAccess()) {
            if (Boolean.FALSE.equals(configuredValue)) {
                LOGGER.warn("Ignoring 'useRepository' value of 'false' because the activity requires direct repository access");
            }
            return true;
        } else if (configuredValue != null) {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (configuredValue) {
                checkRawAuthorization(getRunningTask(), opResult);
            }
            return configuredValue;
        } else {
            return false;
        }
    }

    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // nothing to do here as we are in repo-common
    }

    private void narrowQueryForBucketingAndErrorHandling() throws ActivityExecutionException {

        try {
            ObjectQuery queryFromActivity = searchSpecification.getQuery();

            LOGGER.trace("{}: query as defined by activity:\n{}", shortName,
                    DebugUtil.debugDumpLazily(queryFromActivity));

            ObjectQuery failureNarrowedQuery = narrowQueryToProcessFailedObjectsOnly(queryFromActivity); // logging is inside

            ObjectQuery bucketNarrowedQuery = beans.bucketingManager.narrowQueryForWorkBucket(
                    getObjectType(),
                    failureNarrowedQuery,
                    activity.getDefinition().getDistributionDefinition(),
                    executionSpecifics.createItemDefinitionProvider(),
                    bucket);

            LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}",
                    shortName, DebugUtil.debugDumpLazily(bucketNarrowedQuery));

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
     * 1. In case of `FETCH_FAILED_OBJECTS` it
     *
     * a. sets up a pre-processor that implements fetching individual shadows (that are to be re-tried) from a resource,
     * b. turns on the "no fetch" option.
     *
     * 2. In case of `FILTER_AFTER_RETRIEVAL` it
     *
     * a. installs additional filter that passes through only selected objects (the ones that were failed).
     */
    @Nullable
    private ObjectQuery narrowQueryToProcessFailedObjectsOnly(ObjectQuery query) {
        FailedObjectsSelectorType selector =
                getActivity().getDefinition().getFailedObjectsSelector();

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
                    LOGGER.trace("{}: shadow-fetching preprocessor was set up", shortName);

                case NARROW_QUERY:
                    ObjectQuery failureNarrowedQuery =
                            ObjectQueryUtil.addConjunctions(query, getPrismContext(), failedObjectsFilter);
                    LOGGER.trace("{}: query narrowed to select failed objects only:\n{}", shortName,
                            DebugUtil.debugDumpLazily(failureNarrowedQuery));
                    return failureNarrowedQuery;

                case FILTER_AFTER_RETRIEVAL:
                    additionalFilter = failedObjectsFilter;
                    LOGGER.trace("{}: will use additional filter to select failed objects only (but executes "
                                    + "the search with original query):\n{}", shortName,
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
                !searchSpecification.isUseRepository() &&
                modelProcessingAvailable() &&
                !searchSpecification.isNoFetch() && !searchSpecification.isRaw();
    }

    @NotNull
    protected ObjectPreprocessor<O> createShadowFetchingPreprocessor() {
        throw new UnsupportedOperationException("FETCH_FAILED_OBJECTS is not available in this type of activities. "
                + "Model processing is required.");
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

    @Override
    protected @Nullable Integer determineExpectedTotal(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (isBucketsAnalysis()) {
            return countObjects(opResult);
        } else if (!getReportingOptions().isDetermineExpectedTotal()) {
            return null;
        } else if (BucketingUtil.hasLimitations(bucket)) {
            // We avoid computing expected total if we are processing a bucket: actually we could do it,
            // but we should not display the result as 'task expected total'.
            return null;
        } else {
            return countObjects(opResult);
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
    protected void iterateOverItemsInBucket(OperationResult result) throws CommonException {
        searchIterative(result);
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        // This is the default for search-iterative tasks. It is a legacy behavior, and also the most logical:
        // we do not need to stop on error, because there's always possible to re-run the whole task.
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<PrismObject<O>> request, @NotNull RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityExecutionException {

        PrismObject<O> object = request.getItem();
        String oid = object.getOid();
        if (oid != null) {
            if (!checkAndRegisterOid(oid)) {
                LOGGER.trace("Skipping OID that has been already seen: {}", oid);
                result.recordStatus(NOT_APPLICABLE, "Object has been already seen");
                return true; // continue working
            }
        } else {
            LOGGER.trace("OID is null; can be in case of malformed objects");
        }

        if (filteredOutByAdditionalFilter(request)) {
            LOGGER.trace("Request {} filtered out by additional filter", request);
            result.recordStatus(NOT_APPLICABLE, "Filtered out by additional filter");
            return true; // continue working
        }

        OperationResultType originalFetchResult = object.asObjectable().getFetchResult();
        if (originalFetchResult == null) {
            return processWithPreprocessing(request, workerTask, result);
        } else {
            return processError(object, originalFetchResult, workerTask, result);
        }
    }

    private boolean filteredOutByAdditionalFilter(ItemProcessingRequest<PrismObject<O>> request)
            throws SchemaException {
        return additionalFilter != null &&
                !additionalFilter.match(request.getItem().getValue(), getBeans().matchingRuleRegistry);
    }

    private boolean processWithPreprocessing(ItemProcessingRequest<PrismObject<O>> request, RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityExecutionException {
        PrismObject<O> objectToProcess = preprocessObject(request, workerTask, result);
        return executionSpecifics.processObject(objectToProcess, request, workerTask, result);
    }

    private PrismObject<O> preprocessObject(ItemProcessingRequest<PrismObject<O>> request, RunningTask workerTask,
            OperationResult parentResult) throws CommonException {
        if (preprocessor == null) {
            return request.getItem();
        }
        OperationResult result = parentResult.createMinorSubresult(OP_PREPROCESS_OBJECT);
        try {
            return preprocessor.preprocess(request.getItem(), workerTask, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t; // any exceptions thrown are treated in the gatekeeper
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected boolean processError(PrismObject<O> object, @NotNull OperationResultType errorFetchResult, RunningTask workerTask,
            OperationResult result)
            throws CommonException, ActivityExecutionException {
        result.recordFatalError("Error in preprocessing: " + errorFetchResult.getMessage());
        return true; // "Can continue" flag is updated by item processing gatekeeper (unfortunately, the exception is lost)
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
    protected ResultHandler<O> createSearchResultHandler() {
        return (object, parentResult) -> {
            ObjectProcessingRequest<O> request =
                    new ObjectProcessingRequest<>(sequentialNumberCounter.getAndIncrement(), object, this);
            return coordinator.submit(request, parentResult);
        };
    }

    private boolean checkAndRegisterOid(String oid) {
        return oidsSeen.add(oid);
    }

    @Override
    protected boolean hasProgressCommitPoints() {
        return true;
    }

    @NotNull
    protected SearchSpecification<O> getSearchSpecificationRequired() {
        return requireNonNull(searchSpecification, "no search specification");
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

    @FunctionalInterface
    public interface SearchBasedSpecificsSupplier<O extends ObjectType, WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
            extends IterativeActivityExecution.SpecificsSupplier<
            SearchBasedActivityExecution<O, WD, AH, ?>,
            SearchBasedActivityExecutionSpecifics<O>> {
    }
}
