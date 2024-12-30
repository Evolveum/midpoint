/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.caching.CacheConfiguration.getStatisticsLevel;

import static java.util.Objects.requireNonNullElse;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchQueryResult;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.*;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.util.PopulatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.caching.CacheUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Expression evaluator that is based on searching for an object of `O` type meeting specified criteria (like entitlement shadow),
 * and then converting it into "processed" form (like association value).
 *
 * @param <V> "Processed" value we are looking for (e.g. {@link PrismContainerValue} of {@link ShadowAssociationValueType})
 * @param <O> "Raw" object type we are searching for to get `V` (e.g. {@link ShadowType})
 * @param <D> Definition of `V`
 * @param <E> type of configuration bean
 * @author Radovan Semancik
 */
public abstract class AbstractSearchExpressionEvaluator<
        V extends PrismValue,
        O extends ObjectType,
        D extends ItemDefinition<?>,
        E extends SearchObjectExpressionEvaluatorType>
        extends AbstractValueTransformationExpressionEvaluator<V, D, E> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluator.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private final ObjectResolver objectResolver;
    private final ModelService modelService = ModelCommonBeans.get().modelService;
    /**
     * todo preview changes method calls in this class should be removed and everything should go through ModelService.executeChanges()
     */
    @Deprecated
    private final ModelInteractionService modelInteractionService = ModelCommonBeans.get().modelInteractionService;
    protected final CacheConfigurationManager cacheConfigurationManager = ModelCommonBeans.get().cacheConfigurationManager;

    AbstractSearchExpressionEvaluator(
            QName elementName,
            E expressionEvaluatorType,
            D outputDefinition,
            Protector protector,
            ObjectResolver objectResolver,
            LocalizationService localizationService) {
        super(
                elementName,
                expressionEvaluatorType,
                outputDefinition,
                protector,
                localizationService);
        this.objectResolver = objectResolver;
    }

    protected @NotNull List<V> transformSingleValue(@NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return createEvaluation(vtCtx, result)
                .execute();
    }

    /** Creates the instance of {@link Evaluation}, which is subclassed for each use of this (outer) class. */
    abstract Evaluation createEvaluation(@NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result)
            throws SchemaException;

    // Override the default in this case. It makes more sense like this.
    @Override
    protected boolean isIncludeNullInputs() {
        return BooleanUtils.isTrue(expressionEvaluatorBean.isIncludeNullInputs());
    }

    /** The actual evaluation process. It's a separate class to avoid moving all the parameters along. */
    protected abstract class Evaluation {

        /** The value transformation operation context. */
        @NotNull final ValueTransformationContext vtCtx;

        @NotNull private final OperationResult result;

        /** What objects are we looking for? E.g. {@link ShadowType} when association targets are to be found. Qualified. */
        @NotNull final QName targetTypeQName;

        /** Class corresponding to {@link #targetTypeQName}. */
        @NotNull final Class<O> targetTypeClass;

        protected Evaluation(
                @NotNull ValueTransformationContext vtCtx, @NotNull OperationResult result) throws SchemaException {
            this.vtCtx = vtCtx;
            this.result = result;

            this.targetTypeQName = determineTargetTypeQName();
            this.targetTypeClass = ObjectTypes.getObjectTypeClass(targetTypeQName);
        }

        private @NotNull QName determineTargetTypeQName() throws SchemaException {
            QName typeName = Objects.requireNonNullElseGet(expressionEvaluatorBean.getTargetType(), () -> getDefaultTargetType());
            if (typeName == null) {
                throw new SchemaException("Unknown target type in " + shortDebugDump());
            }
            return prismContext.getSchemaRegistry().qualifyTypeName(typeName);
        }

        protected QName getDefaultTargetType() {
            return null;
        }

        protected @NotNull List<V> execute()
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
                CommunicationException, ConfigurationException, SecurityViolationException {

            // Deltas to be applied on the newly-created value (assuming the value is an assignment value)
            List<ItemDelta<V, D>> newValueDeltas = createNewValueDeltas();

            var explicitTargetOid = expressionEvaluatorBean.getOid();
            if (explicitTargetOid != null) {
                // Shortcut: no searching, we already have OID
                log("explicit OID", 1);
                return List.of(createResultValue(explicitTargetOid, targetTypeQName, null, newValueDeltas));
            }

            var queries = createQueries();
            var searchResults = executeSearchUsingCache(queries, false, newValueDeltas);
            if (!searchResults.isEmpty()) {
                log("search operation (potentially cached)", searchResults.size());
                return searchResults;
            }

            String defaultTargetOid = Referencable.getOid(expressionEvaluatorBean.getDefaultTargetRef());
            if (defaultTargetOid != null) {
                log("default target OID", 1);
                return List.of(createResultValue(defaultTargetOid, targetTypeQName, null, newValueDeltas));
            }

            if (Boolean.TRUE.equals(expressionEvaluatorBean.isCreateOnDemand()) && vtCtx.isEvaluateNew()) {
                try {
                    PrismObject<O> createdObject = createOnDemand();
                    if (createdObject != null) {
                        log("create-on-demand", 1);
                        return List.of(createResultValue(
                                createdObject.getOid(),
                                getObjectTypeName(createdObject),
                                createdObject,
                                newValueDeltas));
                    }
                } catch (ObjectAlreadyExistsException ex) {
                    // object was created in the meantime, so we should try to search for it once more
                    var secondSearchResults = executeSearchUsingCache(queries, true, newValueDeltas);
                    log("create-on-demand (with conflict), followed by repeated search", secondSearchResults.size());
                    return secondSearchResults;
                }
            }

            log("search that found nothing", 0);
            return List.of();
        }

        private void log(String source, int values) {
            LOGGER.trace("Search expression {} (useNew: {}) resolved via {}: returning {} values",
                    vtCtx, vtCtx.isEvaluateNew(), source, values);
        }

        private @Nullable List<ItemDelta<V, D>> createNewValueDeltas()
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            PopulateType valuePopulationDef = expressionEvaluatorBean.getPopulate();
            if (valuePopulationDef != null) {
                if (outputDefinition instanceof PrismContainerDefinition<?> pcd) {
                    return PopulatorUtil.computePopulateItemDeltas(
                            valuePopulationDef, pcd, vtCtx.getVariablesMap(), vtCtx.getExpressionEvaluationContext(), result);
                } else {
                    LOGGER.warn("Search expression {} applied to non-container target, ignoring populate definition", vtCtx);
                    return null;
                }
            } else {
                return null;
            }
        }

        protected @NotNull List<ObjectQuery> createQueries()
                throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            var filterBeans = expressionEvaluatorBean.getFilter();
            configCheck(!filterBeans.isEmpty(), "No filters in %s", lazy(() -> shortDebugDump()));

            List<ObjectQuery> queries = new ArrayList<>();
            for (var filterBean : filterBeans) {
                ObjectQuery rawQuery = createRawQuery(filterBean);
                LOGGER.trace("XML query converted to: {}", rawQuery.debugDumpLazily());

                var eeCtx = vtCtx.getExpressionEvaluationContext();
                ObjectQuery evaluatedQuery = ExpressionUtil.evaluateQueryExpressions(
                        rawQuery,
                        vtCtx.getVariablesMap(),
                        eeCtx.getExpressionProfile(),
                        eeCtx.getExpressionFactory(),
                        vtCtx.getContextDescription(),
                        getTask(),
                        result);
                LOGGER.trace("Query after expressions evaluation: {}", evaluatedQuery.debugDumpLazily());

                ObjectQuery extendedQuery = extendQuery(evaluatedQuery);
                LOGGER.trace("Query after extension: {}", extendedQuery.debugDumpLazily());

                queries.add(extendedQuery);
            }

            return queries;
        }

        protected ObjectQuery createRawQuery(SearchFilterType filter) throws SchemaException, ExpressionEvaluationException {
            return prismContext.getQueryConverter().createObjectQuery(targetTypeClass, filter);
        }

        protected ObjectQuery extendQuery(ObjectQuery query) {
            return query;
        }

        CacheInfo getCacheInfo() {
            return null;
        }

        private List<V> executeSearchUsingCache(
                @NotNull Collection<ObjectQuery> queries,
                boolean createOnDemandRetry,
                List<ItemDelta<V, D>> newValueDeltas)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

            CacheInfo cacheInfo = getCacheInfo();
            ObjectSearchStrategyType searchStrategy = getSearchStrategy();
            var eeCtx = vtCtx.getExpressionEvaluationContext();

            if (cacheInfo == null) {
                return ObjectFound.unwrap(
                        executeSearch(queries, searchStrategy, createOnDemandRetry, newValueDeltas));
            }

            AbstractSearchExpressionEvaluatorCache<V, O, ?, ?> cache = cacheInfo.cache;
            if (cache == null) {
                cacheInfo.logNull(targetTypeClass, queries);
                return ObjectFound.unwrap(
                        executeSearch(queries, searchStrategy, createOnDemandRetry, newValueDeltas));
            }

            if (!cache.supportsObjectType(targetTypeClass)) {
                cacheInfo.logPass(targetTypeClass, queries);
                return ObjectFound.unwrap(
                        executeSearch(queries, searchStrategy, createOnDemandRetry, newValueDeltas));
            }

            List<V> cachedResult = cache.getSearchResult(targetTypeClass, queries, searchStrategy, eeCtx);
            if (cachedResult != null) {
                cacheInfo.logHit(targetTypeClass, queries);
                // TODO consider freezing/cloning objects in the cache or more shallow cloning here;
                //  but it's not crucial, as this kind of cache is not used globally (only as a thread-local one),
                //  nor probably the amount of data cached is quite low
                return CloneUtil.cloneCollectionMembers(cachedResult);
            }

            cacheInfo.logMiss(targetTypeClass, queries);
            var freshResult = executeSearch(queries, searchStrategy, createOnDemandRetry, newValueDeltas);
            if (!freshResult.isEmpty()) {
                // we don't want to cache negative results (e.g. if used with focal objects it might mean that they would
                // be attempted to create multiple times)
                cache.putSearchResult(targetTypeClass, queries, searchStrategy, eeCtx, freshResult);
            }
            return ObjectFound.unwrap(freshResult);
        }

        private @NotNull ObjectSearchStrategyType getSearchStrategy() {
            return requireNonNullElse(expressionEvaluatorBean.getSearchStrategy(), ObjectSearchStrategyType.IN_REPOSITORY);
        }

        private Collection<ObjectFound<O, V>> executeSearch(
                Collection<ObjectQuery> queries,
                ObjectSearchStrategyType searchStrategy,
                boolean createOnDemandRetry,
                List<ItemDelta<V, D>> newValueDeltas)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

            // TODO think about handling of CommunicationException | ConfigurationException | SecurityViolationException
            // Currently if tryAlsoRepository=true (for ON_RESOURCE strategy), such errors result in searching pure repo. And if there's no such
            // object in the repo, probably no exception is raised.
            // But if ON_RESOURCE_IF_NEEDED, and the object does not exist in repo, an exception WILL be raised.
            //
            // Probably we could create specific types of fetch strategies to reflect various error handling requirements.
            // (Or treat it via separate parameter.)

            // This is a set to filter potential duplicates stemming from multiple filters
            var objectsFound = new HashSet<ObjectFound<O, V>>();

            for (ObjectQuery query : queries) {
                switch (searchStrategy) {
                    case IN_REPOSITORY -> objectsFound.addAll(
                            executeSearchAttempt(
                                    query, false, false,
                                    createOnDemandRetry, newValueDeltas));
                    case ON_RESOURCE -> objectsFound.addAll(
                            executeSearchAttempt(
                                    query, true, true,
                                    createOnDemandRetry, newValueDeltas));
                    case ON_RESOURCE_IF_NEEDED -> {
                        Collection<ObjectFound<O, V>> inRepo = executeSearchAttempt(
                                query, false, false, createOnDemandRetry, newValueDeltas);
                        if (!inRepo.isEmpty()) {
                            objectsFound.addAll(inRepo);
                        } else {
                            objectsFound.addAll(
                                    executeSearchAttempt(
                                            query, true, false,
                                            createOnDemandRetry, newValueDeltas));
                        }
                    }
                    default -> throw new IllegalArgumentException("Unknown search strategy: " + searchStrategy);
                }
            }
            LOGGER.trace("Objects found (combined): {}", objectsFound.size());
            return objectsFound;
        }

        private @NotNull Collection<ObjectFound<O, V>> executeSearchAttempt(
                ObjectQuery query,
                boolean searchOnResource,
                boolean tryAlsoRepository,
                boolean createOnDemandRetry,
                List<ItemDelta<V, D>> additionalAttributeDeltas)
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

            var optionsBuilder = GetOperationOptionsBuilder.create()
                    .noFetch(!searchOnResource)
                    .readOnly()
                    .staleness(createOnDemandRetry ? 0L : null); // when retrying, we'd like to see fresh data
            extendOptions(optionsBuilder, searchOnResource);
            var options = optionsBuilder.build();

            try {
                return executeSearch(query, options, additionalAttributeDeltas);
            } catch (IllegalStateException e) { // this comes from checkConsistence methods
                throw new IllegalStateException(e.getMessage() + " in " + vtCtx, e);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + vtCtx, e);
            } catch (SystemException e) {
                throw new SystemException(e.getMessage() + " in " + vtCtx, e);
            } catch (CommunicationException | ConfigurationException | SecurityViolationException e) {
                if (searchOnResource && tryAlsoRepository) {
                    var retryOptions = createNoFetchReadOnlyCollection();
                    try {
                        return executeSearch(query, retryOptions, additionalAttributeDeltas);
                    } catch (SchemaException e1) {
                        throw new SchemaException(e1.getMessage() + " in " + vtCtx, e1);
                    } catch (CommunicationException | ConfigurationException | SecurityViolationException e1) {
                        // TODO improve handling of exception.. we do not want to
                        //  stop whole projection computation, but what to do if the
                        //  shadow for group doesn't exist? (MID-2107)
                        throw new ExpressionEvaluationException("Unexpected expression exception " + e + ": " + e.getMessage(), e);
                    }
                } else {
                    throw new ExpressionEvaluationException("Unexpected expression exception " + e + ": " + e.getMessage(), e);
                }
            }
        }

        private @NotNull Collection<ObjectFound<O, V>> executeSearch(
                ObjectQuery query,
                Collection<SelectorOptions<GetOperationOptions>> options,
                List<ItemDelta<V, D>> additionalAttributeDeltas)
                throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                SecurityViolationException, ExpressionEvaluationException {
            // TODO: perhaps we should limit query to some reasonably high number of results?
            LOGGER.trace("Looking for objects using query:\n{}", query.debugDumpLazily(1));
            var objects = objectResolver.searchObjects(targetTypeClass, query, options, getTask(), result);
            var itemsFound = new ArrayList<ObjectFound<O, V>>();
            for (var object : objects) {
                if (isAcceptable(object)) {
                    LOGGER.trace("Found {}", object);
                    itemsFound.add(
                            new ObjectFound<>(
                                    object,
                                    createResultValue(
                                            object.getOid(),
                                            getObjectTypeName(object),
                                            null, // We don't want to put full object into ref for historical reasons.
                                            additionalAttributeDeltas)));
                } else {
                    LOGGER.trace("Object {} was rejected by additional filtering", object);
                }
            }
            return itemsFound;
        }

        /** Provides additional filtering e.g. rejecting dead shadows as association targets. */
        protected boolean isAcceptable(@NotNull PrismObject<O> object) {
            return true;
        }

        protected void extendOptions(GetOperationOptionsBuilder builder, boolean searchOnResource) {
            // Nothing to do. To be overridden by subclasses
        }

        // e.g parameters, activation for assignment etc.

        /**
         * Converts the object found into a value to be returned (from the expression) - i.e. assignment, association, etc.
         *
         * If `object` is present, then `oid` = `object.oid` and `typeName` = object type name.
         */
        protected abstract @NotNull V createResultValue(
                String oid, @NotNull QName typeName, PrismObject<O> object, List<ItemDelta<V, D>> newValueDeltas)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException;

        /** Useful method called from subclasses producing references. */
        @NotNull PrismReferenceValue createReferenceValue(
                String oid, @NotNull QName objectTypeName, PrismObject<ObjectType> object, QName relation) {
            // Value deltas are ignored here (they cannot be applied to a reference, anyway).
            ObjectReferenceType ref;
            if (object != null) {
                ref = createObjectRefWithFullObject(object);
            } else {
                ref = new ObjectReferenceType()
                        .oid(oid)
                        .type(objectTypeName);
            }
            ref.setRelation(relation);
            return ref.asReferenceValue();
        }

        /** Used by reference-searching evaluators. */
        @Nullable
        QName determineRelation(QName relation, ExpressionType relationExpression)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            configCheck(relation == null || relationExpression == null,
                    "Both relation and relationExpression are present in %s", lazy(() -> shortDebugDump()));
            if (relationExpression != null) {
                var relationDef = PrismContext.get().definitionFactory()
                        .newPropertyDefinition(ObjectReferenceType.F_RELATION, DOMUtil.XSD_QNAME);
                var eeCtx = vtCtx.getExpressionEvaluationContext();
                // Variables are non-relativistic here, so the following call is adequate for both evaluateOld/evaluateNew cases.
                PrismValue value = ExpressionUtil.evaluateExpression(
                        vtCtx.getVariablesMap(), relationDef, relationExpression, eeCtx.getExpressionProfile(),
                        eeCtx.getExpressionFactory(), vtCtx.getContextDescription(), getTask(), result);
                return value != null ? (QName) value.getRealValue() : null;
            } else {
                return relation;
            }
        }

        private PrismObject<O> createOnDemand()
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

            var eeCtx = vtCtx.getExpressionEvaluationContext();
            var variables = vtCtx.getVariablesMap();

            LOGGER.trace("Going to create assignment targets on demand, variables:\n{}", lazy(variables::formatVariables));
            PrismObjectDefinition<O> objectDefinition =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(targetTypeClass);
            PrismObject<O> newObject = objectDefinition.instantiate();

            PopulateType populateObjectConfig = expressionEvaluatorBean.getPopulateObject();

            if (populateObjectConfig == null) {
                LOGGER.warn("No populateObject in search expression in {}, object created on demand will be empty. "
                        + "Subsequent operations will most likely fail", vtCtx);
            } else {
                List<ItemDelta<V, D>> populateDeltas =
                        PopulatorUtil.computePopulateItemDeltas(
                                populateObjectConfig, objectDefinition, variables, eeCtx, result);
                ItemDeltaCollectionsUtil.applyTo(populateDeltas, newObject);
            }

            LOGGER.debug("Creating object on demand from {}: {}", vtCtx, newObject);
            LOGGER.trace("Creating object on demand:\n{}", newObject.debugDumpLazily(1));

            Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges;
            try {
                ObjectDelta<O> addDelta = newObject.createAddDelta();
                // If running in the simulation mode, the executeChanges method below will not have any persistent effects.
                // If running in the legacy preview mode, the task execution mode is set to SIMULATION anyway.
                // So, we don't need to call previewChanges here. Execute is always OK.
                executedChanges = modelService.executeChanges(List.of(addDelta), null, getTask(), result);
            } catch (CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
                throw new ExpressionEvaluationException(e.getMessage(), e);
            }

            ObjectDeltaOperation<O> deltaOperation = ObjectDeltaOperation.findAddDelta(executedChanges, newObject);
            return deltaOperation != null ? deltaOperation.getObjectDelta().getObjectToAdd() : null;
        }

        private Task getTask() {
            return vtCtx.getExpressionEvaluationContext().getTask();
        }
    }

    /**
     * The result of the search: both source object, and the value that was created from it.
     *
     * The value is the actual result. The source object is currently used only for resource/kind determination
     * in {@link AssociationSearchQueryResult} (for cache invalidation).
     *
     * TODO reconsider whether the cache invalidation should not be done in a different way
     */
    public record ObjectFound<OT extends ObjectType, C> (@NotNull PrismObject<OT> sourceObject, C convertedValue) {

        public static <C> List<C> unwrap(Collection<? extends ObjectFound<?, C>> objectFounds) {
            return objectFounds.stream()
                    .map(i -> i.convertedValue())
                    .toList();
        }
    }

    /** Manages the information (logging, registering, ...) of caching aspects. */
    class CacheInfo {
        @Nullable final AbstractSearchExpressionEvaluatorCache<V, O, ?, ?> cache;
        @NotNull final Class<?> cacheClass;
        @NotNull final CacheType cacheType;
        CacheConfiguration.StatisticsLevel statisticsLevel;
        final boolean traceMiss;
        final boolean tracePass;

        CacheInfo(
                @Nullable AbstractSearchExpressionEvaluatorCache<V, O, ?, ?> cache,
                @NotNull Class<?> cacheClass,
                @NotNull CacheType cacheType,
                @NotNull Class<?> objectType) {
            this.cache = cache;
            this.cacheClass = cacheClass;
            this.cacheType = cacheType;

            CacheConfiguration cacheConfiguration = cacheConfigurationManager.getConfiguration(cacheType);
            CacheConfiguration.CacheObjectTypeConfiguration cacheObjectTypeConfiguration =
                    cacheConfiguration != null ? cacheConfiguration.getForObjectType(objectType) : null;
            statisticsLevel = getStatisticsLevel(cacheObjectTypeConfiguration, cacheConfiguration);
            traceMiss = CacheConfiguration.getTraceMiss(cacheObjectTypeConfiguration, cacheConfiguration);
            tracePass = CacheConfiguration.getTracePass(cacheObjectTypeConfiguration, cacheConfiguration);
        }

        void logNull(Class<?> objectType, Collection<ObjectQuery> queries) {
            log("Cache: NULL {} ({})", false, queries, objectType.getSimpleName());
            CachePerformanceCollector.INSTANCE.registerNotAvailable(cacheClass, objectType, statisticsLevel);
        }

        void logPass(Class<?> objectType, Collection<ObjectQuery> queries) {
            assert cache != null;
            cache.registerPass();
            log("Cache: PASS {} ({})", tracePass, queries, objectType.getSimpleName());
            CachePerformanceCollector.INSTANCE.registerPass(cacheClass, objectType, statisticsLevel);
        }

        void logHit(Class<O> objectType, Collection<ObjectQuery> queries) {
            assert cache != null;
            cache.registerHit();
            log("Cache: HIT {} ({})", false, queries, objectType.getSimpleName());
            CachePerformanceCollector.INSTANCE.registerHit(cacheClass, objectType, statisticsLevel);
        }

        void logMiss(Class<O> objectType, Collection<ObjectQuery> queries) {
            assert cache != null;
            cache.registerMiss();
            CachePerformanceCollector.INSTANCE.registerMiss(cacheClass, objectType, statisticsLevel);
            log("Cache: MISS {} ({})", traceMiss, queries, objectType.getSimpleName());
        }

        private void log(String message, boolean info, Object... params) {
            CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
        }
    }
}
