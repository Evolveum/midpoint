/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecification;
import com.evolveum.midpoint.repo.common.activity.definition.RepositoryObjectSetSpecificationImpl;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationImpl;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.processing.ContainerableProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemPreprocessor;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
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
 * A run of a given search-iterative activity.
 *
 * Responsibilities:
 *
 * 1. Bucketing, i.e. orchestrating getting and processing buckets. See {@link #doRun(OperationResult)}.
 *
 * 2. Item source preparation = preparation of search specification ({@link #prepareSearchSpecificationAndSearchableItemSource(OperationResult)}).
 * This includes
 *
 *   a. converting the configured object set to search spec (or obtaining the spec from
 *   the specifics object - {@link SearchBasedActivityRunSpecifics};
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
public abstract class SearchBasedActivityRun<
        C extends Containerable,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends IterativeActivityRun<C, WD, AH, WS>
        implements SearchBasedActivityRunSpecifics<C> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBasedActivityRun.class);

    private static final String OP_PREPROCESS_OBJECT = SearchBasedActivityRun.class.getName() + ".preprocessObject";

    /**
     * Specification of the search that is to be executed: container type, query, options, and "use repository" flag.
     */
    protected SearchSpecification<C> searchSpecification;

    /**
     * Source of items to be processed: repo or model, objects or audit event records, or any containerables.
     */
    private SearchableItemSource searchableItemSource;

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
    private ItemPreprocessor<C> preprocessor;

    /**
     * OIDs of objects submitted to processing in current activity run.
     * Used to filter duplicates. Not applicable to non-object items.
     */
    private final Set<String> oidsSeen = ConcurrentHashMap.newKeySet();

    /**
     * Provides numbers for {@link ItemProcessingRequest} objects.
     */
    @NotNull private final AtomicInteger sequentialNumberCounter = new AtomicInteger(0);

    public SearchBasedActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    protected final void prepareItemSourceForCurrentBucket(OperationResult result)
            throws ActivityRunException, CommonException {
        prepareSearchSpecificationAndSearchableItemSource(result);
    }

    private void prepareSearchSpecificationAndSearchableItemSource(OperationResult result)
            throws CommonException, ActivityRunException {
        searchSpecification = createCustomizedSearchSpecification(result);

        narrowQueryForBucketingAndErrorHandling();
        resolveExpressionsInQuery(result);
        applyDefinitionsToQuery(result);

        LOGGER.trace("{}: will do the following search (in bucket: {}):\n{}",
                shortName, bucket, DebugUtil.debugDumpLazily(searchSpecification));

        searchableItemSource = createSearchableItemSource();
    }

    private SearchableItemSource createSearchableItemSource() {
        if (searchSpecification.isUseRepository() || !advancedSupportAvailable()) {
            return beans.repositoryItemSourceFactory
                    .getItemSourceFor(searchSpecification.getType());
        } else {
            return beans.getAdvancedActivityRunSupport()
                    .getItemSourceFor(searchSpecification.getType());
        }
    }

    /**
     * Creates the search specification (type, query, options, use repo).
     *
     * Normally it is created from the configuration embodied in work definition
     * but there are activities that completely ignore it and provide their own search spec.
     *
     * After creation, it is customized by the activity run. (Even if it was created by activity run completely!)
     */
    private @NotNull SearchSpecification<C> createCustomizedSearchSpecification(OperationResult result)
            throws CommonException, ActivityRunException {

        @NotNull SearchSpecification<C> searchSpecification;
        SearchSpecification<C> customSpec = createCustomSearchSpecification(result);
        if (customSpec != null) {
            searchSpecification = customSpec;
        } else {
            searchSpecification = createSearchSpecificationFromObjectSetSpec(
                    ObjectSetSpecification.fromWorkDefinition(activity.getWorkDefinition()),
                    result);
        }

        customizeSearchSpecification(searchSpecification, result);
        return searchSpecification;
    }

    /**
     * Converts {@link ObjectSetSpecification} into {@link SearchSpecification}.
     */
    private @NotNull SearchSpecification<C> createSearchSpecificationFromObjectSetSpec(
            @NotNull ObjectSetSpecification objectSetSpecification, OperationResult result)
            throws SchemaException, ActivityRunException, ConfigurationException {
        if (objectSetSpecification instanceof ResourceObjectSetSpecificationImpl) {
            //noinspection unchecked
            return (SearchSpecification<C>)
                    beans.getAdvancedActivityRunSupport().createSearchSpecificationFromResourceObjectSetSpec(
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
    private void customizeSearchSpecification(@NotNull SearchSpecification<C> searchSpecification, OperationResult result)
            throws CommonException {
        customizeQuery(searchSpecification, result);
        customizeSearchOptions(searchSpecification, result);
        customizeUseRepository(searchSpecification, result);
    }

    private void customizeUseRepository(SearchSpecification<C> searchSpecification, OperationResult result)
            throws CommonException {
        Boolean configuredValue = searchSpecification.getUseRepository();
        if (doesRequireDirectRepositoryAccess()) {
            if (Boolean.FALSE.equals(configuredValue)) {
                LOGGER.warn("Ignoring 'useRepository' value of 'false' because the activity requires direct repository access");
            }
            searchSpecification.setUseRepository(true);
        } else if (configuredValue == null) {
            searchSpecification.setUseRepository(false); // this is the default
        } else {
            // if we requested this mode explicitly we need to have appropriate authorization
            if (configuredValue) {
                beans.getAdvancedActivityRunSupport()
                        .checkRawAuthorization(getRunningTask(), result);
            }
        }
    }

    private void narrowQueryForBucketingAndErrorHandling() throws ActivityRunException {

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
            throw new ActivityRunException("Couldn't create object query", FATAL_ERROR, PERMANENT_ERROR, t);
        }
    }

    private ObjectQuery narrowQueryToProcessBucket(ObjectQuery failureNarrowedQuery) throws SchemaException {
        ObjectQuery bucketNarrowedQuery;
        bucketNarrowedQuery = beans.bucketingManager.narrowQueryForWorkBucket(
                getItemType(),
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
                    new FailedObjectsFilterCreator(selector, getRunningTask())
                            .createFilter();

            FailedObjectsSelectionMethodType selectionMethod = getFailedObjectsSelectionMethod(selector);
            switch (selectionMethod) {
                case FETCH_FAILED_OBJECTS:
                    // We will use the narrowed query. The noFetch option ensures that search in provisioning will work.
                    // It would be nice to check if the query does not refer to attributes that are not cached.
                    // But it should be done in the provisioning module.
                    searchSpecification.setNoFetchOption();
                    stateCheck(beans.getAdvancedActivityRunSupport().isPresent(),
                            "FETCH_FAILED_OBJECTS is not available in this type of activities. "
                                    + "Model processing is required.");
                    argCheck(searchSpecification.concernsShadows(),
                            "FETCH_FAILED_OBJECTS processing is available only for shadows, not for %s",
                            searchSpecification.getType());
                    //noinspection unchecked
                    preprocessor = (ItemPreprocessor<C>) beans.getAdvancedActivityRunSupport()
                            .createShadowFetchingPreprocessor(this::getSearchOptions, getSchemaService());
                    LOGGER.trace("{}: shadow-fetching preprocessor was set up", shortName);

                case NARROW_QUERY:
                    ObjectQuery failureNarrowedQuery =
                            ObjectQueryUtil.addConjunctions(query, failedObjectsFilter);
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
    protected final boolean isInRepository(OperationResult result) throws ActivityRunException, CommonException {
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
                !ShadowType.class.equals(simpleSearchSpecification.getType());
    }

    private void resolveExpressionsInQuery(OperationResult result) throws CommonException {
        ObjectQuery query = searchSpecification.getQuery();
        if (query != null && ExpressionUtil.hasExpressions(query.getFilter())) {
            searchSpecification.setQuery(
                    beans.getAdvancedActivityRunSupport().evaluateQueryExpressions(
                            query, null, getRunningTask(), result));
        }
    }

    private void applyDefinitionsToQuery(OperationResult result) throws CommonException {
        beans.getAdvancedActivityRunSupport()
                .applyDefinitionsToQuery(searchSpecification, getRunningTask(), result);
    }

    @Override
    public final @Nullable Integer determineOverallSize(OperationResult result)
            throws CommonException, ActivityRunException {
        assert bucket == null;
        prepareSearchSpecificationAndSearchableItemSource(result);
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
        return searchableItemSource.count(searchSpecification, getRunningTask(), result);
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

    private boolean filteredOutByAdditionalFilter(ItemProcessingRequest<C> request)
            throws SchemaException {
        return additionalFilter != null &&
                !additionalFilter.match(request.getItem().asPrismContainerValue(), getBeans().matchingRuleRegistry);
    }

    @Override
    public final boolean processItem(@NotNull ItemProcessingRequest<C> request, @NotNull RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityRunException {

        C item = request.getItem();
        String oid = request.getItemOid();
        if (oid != null) {
            if (!checkAndRegisterOid(oid)) {
                LOGGER.trace("Skipping OID that has been already seen: {}", oid);
                result.recordStatus(NOT_APPLICABLE, "Object has been already seen");
                return true; // continue working
            }
        } else {
            LOGGER.trace("No OID for the item");
        }

        if (filteredOutByAdditionalFilter(request)) {
            LOGGER.trace("Request {} filtered out by additional filter", request);
            result.recordStatus(NOT_APPLICABLE, "Filtered out by additional filter");
            return true; // continue working
        }

        OperationResultType originalFetchResult = getFetchResult(item);
        if (originalFetchResult != null) {
            // We assume that if there's a fetch result, then the corresponding item (most probably shadow)
            // was not retrieved successfully. So instead of regular processing we process it as an error.
            // (Maybe we could check if the result is really an error. Will do that some day.)
            return processError(originalFetchResult, result);
        }

        // The item was retrieved OK. Let's process it.
        C preprocessedItem = preprocessItem(request, workerTask, result);
        return processItem(preprocessedItem, request, workerTask, result);
    }

    private OperationResultType getFetchResult(C item) {
        if (item instanceof ObjectType object) {
            return object.getFetchResult();
        } else {
            return null;
        }
    }

    private C preprocessItem(ItemProcessingRequest<C> request, RunningTask workerTask,
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

    private boolean processError(@NotNull OperationResultType errorFetchResult, OperationResult result) {
        result.recordFatalError("Error in preprocessing: " + errorFetchResult.getMessage());
        // We can return true here, as the "can continue" flag is updated by item processing gatekeeper
        // based on the error severity. Unfortunately, the exception as such is lost when the operation
        // result was converted from native form to a bean to be stored in the object.
        return true;
    }

    /**
     * Retrieves items from the source and passes them to search
     */
    private void searchIterative(OperationResult result) throws CommonException {
        ContainerableResultHandler<C> handler = (object, parentResult) -> {
            ItemProcessingRequest<C> request =
                    ContainerableProcessingRequest.create(sequentialNumberCounter.getAndIncrement(), object, this);
            return coordinator.submit(request, parentResult);
        };
        searchableItemSource.searchIterative(searchSpecification, handler, getRunningTask(), result);
    }

    private boolean advancedSupportAvailable() {
        return beans.getAdvancedActivityRunSupport().isPresent();
    }

    public final SchemaService getSchemaService() {
        return beans.schemaService;
    }

    protected final TaskManager getTaskManager() {
        return beans.taskManager;
    }

    @NotNull
    protected final SearchSpecification<C> getSearchSpecificationRequired() {
        return requireNonNull(searchSpecification, "no search specification");
    }

    /** Precondition: search specification must already exist. */
    protected final Class<C> getItemType() {
        return getSearchSpecificationRequired().getType();
    }

    public final ObjectQuery getQuery() {
        return getSearchSpecificationRequired().getQuery();
    }

    public final Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return getSearchSpecificationRequired().getSearchOptions();
    }

    private boolean checkAndRegisterOid(String oid) {
        return oidsSeen.add(oid);
    }
}
