/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.RunningTask;
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

import static com.evolveum.midpoint.util.MiscUtil.*;

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
 * 3. "Expected total" determination - see {@link #setExpectedTotal(OperationResult)} and
 * {@link #setExpectedInCurrentBucket(OperationResult)}.
 *
 * 4. Pre-processing of items found - see {@link #processItem(ItemProcessingRequest, RunningTask, OperationResult)}:
 *
 *   a. checking for already-processed objects (OIDs seen),
 *   b. applying additional filter (currently used for retrying failed objects),
 *   c. applying additional pre-processing to objects (currently used for retrying failed objects),
 */
public abstract class SearchBasedActivityExecution<
        C extends Containerable,
        PC extends PrismContainer<C>,
        RH extends ObjectResultHandler,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends IterativeActivityExecution<PC, WD, AH, WS>
        implements SearchBasedActivityExecutionSpecifics<C, PC> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBasedActivityExecution.class);

    private static final String OP_PREPROCESS_OBJECT = SearchBasedActivityExecution.class.getName() + ".preprocessObject";

    /**
     * Specification of the search that is to be executed: container type, query, options, and "use repository" flag.
     */
    protected SearchSpecification<C> searchSpecification;

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
    private ObjectPreprocessor<?> preprocessor;

    /**
     * OIDs of objects submitted to processing in current part execution (i.e. in the current bucket).
     * Used to filter duplicates.
     */
    private final Set<String> oidsSeen = ConcurrentHashMap.newKeySet();

    /**
     * Provides numbers for {@link ItemProcessingRequest} objects.
     */
    @NotNull private final AtomicInteger sequentialNumberCounter = new AtomicInteger(0);

    public SearchBasedActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    protected final void prepareItemSourceForCurrentBucket(OperationResult result)
            throws ActivityExecutionException, CommonException {
        prepareSearchSpecification(result);
    }

    private void prepareSearchSpecification(OperationResult result) throws CommonException, ActivityExecutionException {
        searchSpecification = createCustomizedSearchSpecification(result);

        narrowQueryForBucketingAndErrorHandling();
        resolveExpressionsInQuery(result);
        applyDefinitionsToQuery(result);

        LOGGER.trace("{}: will do the following search (in bucket: {}):\n{}",
                shortName, bucket, DebugUtil.debugDumpLazily(searchSpecification));
    }

    private @NotNull SearchSpecification<C> createCustomizedSearchSpecification(OperationResult result)
            throws CommonException, ActivityExecutionException {
        SearchSpecification<C> searchSpecification = doCreateSearchSpecification(result);
        customizeSearchSpecification(searchSpecification, result);
        return searchSpecification;
    }

    /**
     * Creates the search specification (type, query, options, use repo). Normally it is created from the configuration
     * embodied in work definition, but it can be fully overridden for the least configurable activities.
     */
    private @NotNull SearchSpecification<C> doCreateSearchSpecification(OperationResult result)
            throws SchemaException, ActivityExecutionException {
        SearchSpecification<C> customSpec = createCustomSearchSpecification(result);
        if (customSpec != null) {
            return customSpec;
        }

        return createSearchSpecificationFromObjectSetSpec(
                ObjectSetSpecification.fromWorkDefinition(activity.getWorkDefinition()),
                result);
    }

    /**
     * Converts {@link ObjectSetSpecification} into {@link SearchSpecification}.
     */
    private @NotNull SearchSpecification<C> createSearchSpecificationFromObjectSetSpec(
            @NotNull ObjectSetSpecification objectSetSpecification, OperationResult result)
            throws SchemaException, ActivityExecutionException {
        if (objectSetSpecification instanceof ResourceObjectSetSpecificationImpl) {
            //noinspection unchecked
            return (SearchSpecification<C>)
                    beans.getAdvancedActivityExecutionSupport().createSearchSpecificationFromResourceObjectSetSpec(
                            (ResourceObjectSetSpecificationImpl) objectSetSpecification, getRunningTask(), result);
        } else if (objectSetSpecification instanceof RepositoryObjectSetSpecificationImpl) {
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
    private void customizeSearchSpecification(SearchSpecification<C> searchSpecification, OperationResult result)
            throws CommonException {

        searchSpecification.setQuery(
                customizeQuery(searchSpecification.getQuery(), result));

        searchSpecification.setSearchOptions(
                customizeSearchOptions(searchSpecification.getSearchOptions(), result));

        searchSpecification.setUseRepository(
                customizeUseRepository(searchSpecification.getUseRepository(), result));
    }

    private Boolean customizeUseRepository(Boolean configuredValue, OperationResult result)
            throws CommonException {
        if (doesRequireDirectRepositoryAccess()) {
            if (Boolean.FALSE.equals(configuredValue)) {
                LOGGER.warn("Ignoring 'useRepository' value of 'false' because the activity requires direct repository access");
            }
            return true;
        } else if (configuredValue != null) {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (configuredValue) {
                beans.getAdvancedActivityExecutionSupport()
                        .checkRawAuthorization(getRunningTask(), result);
            }
            return configuredValue;
        } else {
            return false;
        }
    }

    private void narrowQueryForBucketingAndErrorHandling() throws ActivityExecutionException {

        try {
            ObjectQuery queryFromActivity = searchSpecification.getQuery();
            LOGGER.trace("{}: query as defined by activity:\n{}", shortName,
                    DebugUtil.debugDumpLazily(queryFromActivity));

            ObjectQuery failureNarrowedQuery = narrowQueryToProcessFailedObjectsOnly(queryFromActivity); // logging is inside
            ObjectQuery bucketNarrowedQuery =
                    bucket != null ?
                            narrowQueryToProcessBucket(failureNarrowedQuery) : // logging is inside
                            failureNarrowedQuery;

            searchSpecification.setQuery(bucketNarrowedQuery);

        } catch (Throwable t) {
            // This is most probably a permanent error. (The real communication with a resource is carried out when the
            // query is issued. With an exception of untested resource. But it is generally advised to do the connection test
            // before running any tasks.)
            throw new ActivityExecutionException("Couldn't create object query", FATAL_ERROR, PERMANENT_ERROR, t);
        }
    }

    private ObjectQuery narrowQueryToProcessBucket(ObjectQuery failureNarrowedQuery) throws SchemaException {
        ObjectQuery bucketNarrowedQuery;
        bucketNarrowedQuery = beans.bucketingManager.narrowQueryForWorkBucket(
                getContainerType(),
                failureNarrowedQuery,
                activity.getDefinition().getDistributionDefinition(),
                createItemDefinitionProvider(),
                bucket);

        LOGGER.trace("{}: using a query (after applying work bucket, before evaluating expressions):\n{}",
                shortName, DebugUtil.debugDumpLazily(bucketNarrowedQuery));
        return bucketNarrowedQuery;
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
                    stateCheck(beans.getAdvancedActivityExecutionSupport().isPresent(),
                            "FETCH_FAILED_OBJECTS is not available in this type of activities. "
                                    + "Model processing is required.");
                    argCheck(searchSpecification.concernsShadows(),
                            "FETCH_FAILED_OBJECTS processing is available only for shadows, not for %s",
                            searchSpecification.getContainerType());
                    //noinspection unchecked
                    preprocessor = beans.getAdvancedActivityExecutionSupport()
                            .createShadowFetchingPreprocessor(this::getSearchOptions, getSchemaService());
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
                advancedSupportAvailable() &&
                !searchSpecification.isNoFetch() && !searchSpecification.isRaw();
    }

    @Override
    protected final boolean isInRepository(OperationResult result) throws ActivityExecutionException, CommonException {
        SearchSpecification<C> simpleSearchSpecification;
        if (searchSpecification != null) {
            simpleSearchSpecification = searchSpecification;
        } else {
            // We don't care about narrowing the query via buckets, error handling, etc.
            // We only need "use repo" and object type.
            simpleSearchSpecification = createCustomizedSearchSpecification(result);
        }
        return simpleSearchSpecification.isUseRepository() ||
                simpleSearchSpecification.isRaw() ||
                simpleSearchSpecification.isNoFetch() ||
                !ShadowType.class.equals(simpleSearchSpecification.getContainerType());
    }

    private void resolveExpressionsInQuery(OperationResult result) throws CommonException {
        if (ExpressionUtil.hasExpressions(searchSpecification.getQuery().getFilter())) {
            searchSpecification.setQuery(
                    beans.getAdvancedActivityExecutionSupport().evaluateQueryExpressions(
                            searchSpecification.getQuery(), null, getRunningTask(), result));
        }
    }

    private void applyDefinitionsToQuery(OperationResult result) throws CommonException {
        beans.getAdvancedActivityExecutionSupport()
                .applyDefinitionsToQuery(searchSpecification, getRunningTask(), result);
    }

    @Override
    public final @Nullable Integer determineOverallSize(OperationResult result)
            throws CommonException, ActivityExecutionException {
        assert bucket == null;
        prepareSearchSpecification(result);
        return countObjects(result);
    }

    @Override
    public final @Nullable Integer determineCurrentBucketSize(OperationResult result) throws CommonException {
        assert bucket != null;
        return countObjects(result);
    }

    /**
     * Used to count objects using model or any similar higher-level interface. Defaults to repository count.
     */
    protected Integer countObjects(OperationResult result) throws CommonException {
        if (searchSpecification.isUseRepository() || !advancedSupportAvailable()) {
            return countObjectsInRepository(result);
        } else {
            return beans.getAdvancedActivityExecutionSupport()
                    .countObjects(searchSpecification, getRunningTask(), result);
        }
    }

    private int countObjectsInRepository(OperationResult result) throws SchemaException {
        return getSearchSupport().countObjectsInRepository(
                getContainerType(), getQuery(), getSearchOptions(), this, result);
    }

    @Override
    protected final void iterateOverItemsInBucket(OperationResult result) throws CommonException {
        searchIterative(result);
    }

    @Override
    protected final @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        // This is the default for search-iterative tasks. It is a legacy behavior, and also the most logical:
        // we do not need to stop on error, because there's always possible to re-run the whole task.
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    final boolean filteredOutByAdditionalFilter(ItemProcessingRequest<PC> request)
            throws SchemaException {
        return additionalFilter != null &&
                !additionalFilter.match(request.getItem().getValue(), getBeans().matchingRuleRegistry);
    }

    @Override
    public final boolean processItem(@NotNull ItemProcessingRequest<PC> request, @NotNull RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityExecutionException {

        PC object = request.getItem();
        String oid = request.getItemOid();
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

        OperationResultType originalFetchResult = getFetchResult(object);
        if (originalFetchResult == null) {
            return processWithPreprocessing(request, workerTask, result);
        } else {
            return processError(object, originalFetchResult, workerTask, result);
        }
    }

    private OperationResultType getFetchResult(PC object){
        if (object instanceof PrismObject) {
            ((PrismObject<? extends ObjectType>)object).asObjectable().getFetchResult();
        }
        return null;
    }

    boolean processWithPreprocessing(ItemProcessingRequest<PC> request, RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityExecutionException {
        PC objectToProcess = preprocessObject(request, workerTask, result);
        return processObject(objectToProcess, request, workerTask, result);
    }

    private PC preprocessObject(ItemProcessingRequest<PC> request, RunningTask workerTask,
            OperationResult parentResult) throws CommonException {
        if (preprocessor == null || !(request.getItem() instanceof PrismObject)) {
            return request.getItem();
        }
        OperationResult result = parentResult.createMinorSubresult(OP_PREPROCESS_OBJECT);
        try {
            return (PC) preprocessor.preprocess((PrismObject)request.getItem(), workerTask, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t; // any exceptions thrown are treated in the gatekeeper
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected final boolean processError(PrismContainer<C> object, @NotNull OperationResultType errorFetchResult, RunningTask workerTask,
            OperationResult result)
            throws CommonException, ActivityExecutionException {
        result.recordFatalError("Error in preprocessing: " + errorFetchResult.getMessage());
        return true; // "Can continue" flag is updated by item processing gatekeeper (unfortunately, the exception is lost)
    }

    /**
     * Used to search using model or any similar higher-level interface. Defaults to search using repository.
     */
    private void searchIterative(OperationResult result) throws CommonException {
        if (searchSpecification.isUseRepository() || !advancedSupportAvailable()) {
            searchIterativeInRepository(result);
        } else {
            beans.getAdvancedActivityExecutionSupport()
                    .searchIterative(searchSpecification, createSearchResultHandler(), getRunningTask(), result);
        }
    }

    /**
     * Passes all objects found into the processing coordinator.
     * (Which processes them directly or queues them for the worker threads.)
     */
    private RH createSearchResultHandler() {
        return getSearchSupport().createSearchResultHandler(getSequentialNumberCounter(), coordinator, this);
    }

    private void searchIterativeInRepository(OperationResult result) throws SchemaException {
        getSearchSupport().searchIterativeInRepository(
                getContainerType(),
                getQuery(),
                getSearchOptions(),
                getSequentialNumberCounter(),
                this,
                result);
    }

    private boolean advancedSupportAvailable() {
        return beans.getAdvancedActivityExecutionSupport().isPresent();
    }

    protected ExpressionProfile getExpressionProfile() {
        // TODO Determine from task object archetype
        return MiscSchemaUtil.getExpressionProfile();
    }

    public final SchemaService getSchemaService() {
        return beans.schemaService;
    }

    protected final TaskManager getTaskManager() {
        return beans.taskManager;
    }

    @Override
    protected final boolean hasProgressCommitPoints() {
        return true;
    }

    @NotNull
    protected final SearchSpecification<C> getSearchSpecificationRequired() {
        return requireNonNull(searchSpecification, "no search specification");
    }

    public final Class<C> getContainerType() {
        return getSearchSpecificationRequired().getContainerType();
    }

    public final ObjectQuery getQuery() {
        return getSearchSpecificationRequired().getQuery();
    }

    public final Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return getSearchSpecificationRequired().getSearchOptions();
    }

    final AtomicInteger getSequentialNumberCounter() {
        return sequentialNumberCounter;
    }

    final boolean checkAndRegisterOid(String oid) {
        return oidsSeen.add(oid);
    }

    abstract SearchActivityExecutionSupport<C, PC, RH> getSearchSupport();
}
