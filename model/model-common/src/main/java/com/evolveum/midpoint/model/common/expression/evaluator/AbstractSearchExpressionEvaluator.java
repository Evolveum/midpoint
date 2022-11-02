/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;
import static com.evolveum.midpoint.util.caching.CacheConfiguration.getStatisticsLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.util.PopulatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CacheConfiguration.StatisticsLevel;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.caching.CacheUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Expression evaluator that is based on searching for an object of `O` type meeting specified criteria (like entitlement shadow),
 * and then converting it into "processed" form (like association value).
 *
 * @param <V> "Processed" value we are looking for (e.g. {@link PrismContainerValue} of {@link ShadowAssociationType})
 * @param <O> "Raw" object type we are searching for to get `V` (e.g. {@link ShadowType})
 * @param <D> Definition of `V`
 * @param <E> type of configuration bean
 *
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
    private final ModelService modelService;
    /**
     * todo preview changes method calls in this class should be removed and everything should go through ModelService.executeChanges()
     */
    @Deprecated
    private final ModelInteractionService modelInteractionService;
    protected CacheConfigurationManager cacheConfigurationManager;

    AbstractSearchExpressionEvaluator(QName elementName, E expressionEvaluatorType,
            D outputDefinition, Protector protector, PrismContext prismContext,
            ObjectResolver objectResolver, ModelService modelService, ModelInteractionService modelInteractionService, SecurityContextManager securityContextManager,
            LocalizationService localizationService,
            CacheConfigurationManager cacheConfigurationManager) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext, securityContextManager, localizationService);
        this.objectResolver = objectResolver;
        this.modelService = modelService;
        this.modelInteractionService = modelInteractionService;
        this.cacheConfigurationManager = cacheConfigurationManager;
    }

    @NotNull
    @Override
    protected List<V> transformSingleValue(
            VariablesMap variables,
            PlusMinusZero valueDestination,
            boolean useNew,
            ExpressionEvaluationContext context,
            String contextDescription,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        QName targetTypeQName = expressionEvaluatorBean.getTargetType();
        if (targetTypeQName == null) {
            targetTypeQName = getDefaultTargetType();
        }
        if (targetTypeQName != null && QNameUtil.isUnqualified(targetTypeQName)) {
            targetTypeQName = prismContext.getSchemaRegistry().resolveUnqualifiedTypeName(targetTypeQName);
        }

        ObjectTypes targetType = ObjectTypes.getObjectTypeFromTypeQName(targetTypeQName);
        if (targetType == null) {
            throw new SchemaException("Unknown target type " + targetTypeQName + " in " + shortDebugDump());
        }
        Class<O> targetTypeClass = targetType.getClassDefinition();

        List<V> resultValues;
        ObjectQuery query = null;

        List<ItemDelta<V, D>> additionalAttributeDeltas = null;
        PopulateType populateAssignmentType = expressionEvaluatorBean.getPopulate();
        if (populateAssignmentType != null) {
            if (outputDefinition instanceof PrismContainerDefinition) {
                additionalAttributeDeltas = PopulatorUtil.computePopulateItemDeltas(populateAssignmentType,
                        (PrismContainerDefinition<?>) outputDefinition, variables, context, contextDescription, task, result);
            } else {
                LOGGER.warn("Search expression {} applied to non-container target, ignoring populate definition", contextDescription);
            }
        }

        if (expressionEvaluatorBean.getOid() != null) {

            // Shortcut: no searching, we already have OID
            resultValues = new ArrayList<>(1);
            resultValues.add(
                    createPrismValue(
                            expressionEvaluatorBean.getOid(), null, targetTypeQName, additionalAttributeDeltas, context));

        } else {

            SearchFilterType filterType = expressionEvaluatorBean.getFilter();
            if (filterType == null) {
                throw new SchemaException("No filter in " + shortDebugDump());
            }

            query = createQuery(targetTypeClass, filterType, variables, context, result, task);

            resultValues = executeSearchUsingCache(
                    targetTypeClass,
                    targetTypeQName,
                    query,
                    additionalAttributeDeltas,
                    context,
                    contextDescription,
                    task,
                    result);

            if (resultValues.isEmpty()) {
                ObjectReferenceType defaultTargetRef = expressionEvaluatorBean.getDefaultTargetRef();
                if (defaultTargetRef != null) {
                    resultValues.add(
                            createPrismValue(
                                    defaultTargetRef.getOid(), null, targetTypeQName, additionalAttributeDeltas, context));
                }
            }
        }

        if (resultValues.isEmpty()
                && Boolean.TRUE.equals(expressionEvaluatorBean.isCreateOnDemand())
                && (valueDestination == PlusMinusZero.PLUS || valueDestination == PlusMinusZero.ZERO || useNew)) {
//            String createdObjectOid =
//                    createOnDemand(targetTypeClass, variables, context, context.getContextDescription(), task, result);
//            resultValues.add(
//                    createPrismValue(createdObjectOid, targetTypeQName, additionalAttributeDeltas, context));

            PrismObject createdObject = createOnDemand(targetTypeClass, variables, context, context.getContextDescription(), task, result);
            resultValues.add(
                    createPrismValue(createdObject.getOid(), createdObject, targetTypeQName, additionalAttributeDeltas, context));
        }

        LOGGER.trace("Search expression {} (valueDestination={}) got {} results for query {}",
                contextDescription, valueDestination, resultValues.size(), query);

        return resultValues;
    }

    @NotNull
    private ObjectQuery createQuery(
            Class<O> targetTypeClass,
            SearchFilterType filterType,
            VariablesMap variables,
            ExpressionEvaluationContext context,
            OperationResult result,
            Task task) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        ObjectQuery rawQuery = prismContext.getQueryConverter().createObjectQuery(targetTypeClass, filterType);
        LOGGER.trace("XML query converted to: {}", rawQuery.debugDumpLazily());

        ObjectQuery evaluatedQuery = ExpressionUtil.evaluateQueryExpressions(
                rawQuery,
                variables,
                context.getExpressionProfile(),
                context.getExpressionFactory(),
                prismContext,
                context.getContextDescription(),
                task,
                result);
        LOGGER.trace("Expression in query evaluated to: {}", evaluatedQuery.debugDumpLazily());

        ObjectQuery extendedQuery = extendQuery(evaluatedQuery, context);
        LOGGER.trace("Query after extension: {}", extendedQuery.debugDumpLazily());
        return extendedQuery;
    }

    protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext params)
            throws ExpressionEvaluationException {
        return query;
    }

    protected QName getDefaultTargetType() {
        return null;
    }

    protected AbstractSearchExpressionEvaluatorCache<V, O, ?, ?> getCache() {
        return null;
    }

    protected Class<?> getCacheClass() {
        return null;
    }

    protected CacheType getCacheType() {
        return null;
    }

    private List<V> executeSearchUsingCache(
            Class<O> targetTypeClass,
            final QName targetTypeQName,
            ObjectQuery query,
            List<ItemDelta<V, D>> additionalAttributeDeltas,
            final ExpressionEvaluationContext params,
            String contextDescription,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;
        Class<?> cacheClass = getCacheClass();
        CacheType cacheType = getCacheType();
        CacheConfiguration cacheConfiguration = cacheType != null ? cacheConfigurationManager.getConfiguration(cacheType) : null;
        CacheConfiguration.CacheObjectTypeConfiguration cacheObjectTypeConfiguration =
                cacheConfiguration != null ? cacheConfiguration.getForObjectType(targetTypeClass) : null;
        StatisticsLevel statisticsLevel = getStatisticsLevel(cacheObjectTypeConfiguration, cacheConfiguration);
        boolean traceMiss = CacheConfiguration.getTraceMiss(cacheObjectTypeConfiguration, cacheConfiguration);
        boolean tracePass = CacheConfiguration.getTracePass(cacheObjectTypeConfiguration, cacheConfiguration);

        ObjectSearchStrategyType searchStrategy = getSearchStrategy();

        AbstractSearchExpressionEvaluatorCache<V, O, ?, ?> cache = getCache();
        if (cache == null) {
            if (cacheClass != null) {
                log("Cache: NULL {} ({})", false, query, targetTypeClass.getSimpleName());
                collector.registerNotAvailable(cacheClass, targetTypeClass, statisticsLevel);
            }
            return executeSearch(null, targetTypeClass, targetTypeQName, query, searchStrategy,
                    additionalAttributeDeltas, params, contextDescription, task, result);
        }

        assert cacheClass != null && cacheType != null;

        if (!cache.supportsObjectType(targetTypeClass)) {
            log("Cache: PASS {} ({})", tracePass, query, targetTypeClass.getSimpleName());
            cache.registerPass();
            collector.registerPass(cacheClass, targetTypeClass, statisticsLevel);
            return executeSearch(null, targetTypeClass, targetTypeQName, query, searchStrategy,
                    additionalAttributeDeltas, params, contextDescription, task, result);
        }

        List<V> list = cache.getQueryResult(targetTypeClass, query, searchStrategy, params, prismContext);
        if (list != null) {
            cache.registerHit();
            collector.registerHit(cacheClass, targetTypeClass, statisticsLevel);
            log("Cache: HIT {} ({})", false, query, targetTypeClass.getSimpleName());
            return CloneUtil.clone(list);
        }
        cache.registerMiss();
        collector.registerMiss(cacheClass, targetTypeClass, statisticsLevel);
        log("Cache: MISS {} ({})", traceMiss, query, targetTypeClass.getSimpleName());
        List<PrismObject<O>> rawResult = new ArrayList<>();
        List<V> freshList = executeSearch(
                rawResult,
                targetTypeClass,
                targetTypeQName,
                query,
                searchStrategy,
                additionalAttributeDeltas,
                params,
                contextDescription,
                task,
                result);
        if (!freshList.isEmpty()) {
            // we don't want to cache negative results (e.g. if used with focal objects it might mean that they would be attempted to create multiple times)
            cache.putQueryResult(
                    targetTypeClass,
                    query,
                    searchStrategy,
                    params,
                    freshList,
                    rawResult,
                    prismContext);
        }
        return freshList;
    }

    private ObjectSearchStrategyType getSearchStrategy() {
        if (expressionEvaluatorBean.getSearchStrategy() != null) {
            return expressionEvaluatorBean.getSearchStrategy();
        }
        return ObjectSearchStrategyType.IN_REPOSITORY;
    }

    private List<V> executeSearch(
            List<PrismObject<O>> rawResult,
            Class<O> targetTypeClass,
            final QName targetTypeQName,
            ObjectQuery query,
            ObjectSearchStrategyType searchStrategy,
            List<ItemDelta<V, D>> additionalAttributeDeltas,
            ExpressionEvaluationContext params,
            String contextDescription,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        // TODO think about handling of CommunicationException | ConfigurationException | SecurityViolationException
        // Currently if tryAlsoRepository=true (for ON_RESOURCE strategy), such errors result in searching pure repo. And if there's no such
        // object in the repo, probably no exception is raised.
        // But if ON_RESOURCE_IF_NEEDED, and the object does not exist in repo, an exception WILL be raised.
        //
        // Probably we could create specific types of fetch strategies to reflect various error handling requirements.
        // (Or treat it via separate parameter.)

        switch (searchStrategy) {
            case IN_REPOSITORY:
                return executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, false, false,
                        additionalAttributeDeltas, params, contextDescription, task, result);
            case ON_RESOURCE:
                return executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, true, true,
                        additionalAttributeDeltas, params, contextDescription, task, result);
            case ON_RESOURCE_IF_NEEDED:
                List<V> inRepo = executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, false, false,
                        additionalAttributeDeltas, params, contextDescription, task, result);
                if (!inRepo.isEmpty()) {
                    return inRepo;
                }
                if (rawResult != null) {
                    rawResult.clear();
                }
                return executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, true, false, additionalAttributeDeltas, params, contextDescription, task, result);
            default:
                throw new IllegalArgumentException("Unknown search strategy: " + searchStrategy);
        }
    }

    private List<V> executeSearchAttempt(
            List<PrismObject<O>> rawResults,
            Class<O> targetTypeClass,
            QName targetTypeQName,
            ObjectQuery query,
            boolean searchOnResource,
            boolean tryAlsoRepository,
            List<ItemDelta<V, D>> additionalAttributeDeltas,
            ExpressionEvaluationContext params,
            String contextDescription,
            Task task,
            OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        final List<V> valueResults = new ArrayList<>();

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        if (!searchOnResource) {
            options.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
        }
        options = GetOperationOptions.updateToReadOnly(options);
        extendOptions(options, searchOnResource);

        try {
            executeSearch(valueResults, rawResults, targetTypeClass, targetTypeQName, query, options, task, result, params, additionalAttributeDeltas);
        } catch (IllegalStateException e) { // this comes from checkConsistence methods
            throw new IllegalStateException(e.getMessage() + " in " + contextDescription, e);
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage() + " in " + contextDescription, e);
        } catch (SystemException e) {
            throw new SystemException(e.getMessage() + " in " + contextDescription, e);
        } catch (CommunicationException | ConfigurationException | SecurityViolationException e) {
            if (searchOnResource && tryAlsoRepository) {
                options = createNoFetchReadOnlyCollection();
                try {
                    executeSearch(valueResults, rawResults, targetTypeClass, targetTypeQName, query, options, task, result, params,
                            additionalAttributeDeltas);
                } catch (SchemaException e1) {
                    throw new SchemaException(e1.getMessage() + " in " + contextDescription, e1);
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

        LOGGER.trace("Assignment expression resulted in {} objects, using query:\n{}",
                valueResults.size(), query.debugDumpLazily());

        return valueResults;
    }

    private void executeSearch(
            List<V> valueResults,
            List<PrismObject<O>> rawResults,
            Class<O> targetTypeClass,
            QName targetTypeQName,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult opResult,
            ExpressionEvaluationContext params,
            List<ItemDelta<V, D>> additionalAttributeDeltas)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        // TODO: perhaps we should limit query to some reasonably high number of results?
        SearchResultList<PrismObject<O>> objects = objectResolver.searchObjects(targetTypeClass, query, options, task, opResult);
        for (PrismObject<O> object : objects) {
            if (!isAcceptable(object)) {
                LOGGER.trace("Object {} was rejected by additional filtering", object);
                continue;
            }
            if (rawResults != null) {
                rawResults.add(object);
            }
            valueResults.add(createPrismValue(object.getOid(), null, targetTypeQName, additionalAttributeDeltas, params));
        }
    }

    /** Provides additional filtering e.g. rejecting dead shadows as association targets. */
    protected boolean isAcceptable(@NotNull PrismObject<O> object) {
        return true;
    }

    protected void extendOptions(Collection<SelectorOptions<GetOperationOptions>> options, boolean searchOnResource) {
        // Nothing to do. To be overridden by subclasses
    }

    // e.g parameters, activation for assignment etc.

    protected abstract V createPrismValue(
            String oid,
            PrismObject object,
            QName targetTypeQName,
            List<ItemDelta<V, D>> additionalAttributeDeltas,
            ExpressionEvaluationContext params);

    private PrismObject createOnDemand(Class<O> targetTypeClass, VariablesMap variables,
            ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Going to create assignment targets on demand, variables:\n{}", variables.formatVariables());
        }
        PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(targetTypeClass);
        PrismObject<O> newObject = objectDefinition.instantiate();

        PopulateType populateObject = expressionEvaluatorBean.getPopulateObject();

        if (populateObject == null) {
            LOGGER.warn("No populateObject in assignment expression in {}, "
                    + "object created on demand will be empty. Subsequent operations will most likely fail", contextDescription);
        } else {

            List<ItemDelta<V, D>> populateDeltas = PopulatorUtil.computePopulateItemDeltas(populateObject,
                    objectDefinition, variables, params, contextDescription, task, result);
            ItemDeltaCollectionsUtil.applyTo(populateDeltas, newObject);

        }

        LOGGER.debug("Creating object on demand from {}: {}", contextDescription, newObject);
        LOGGER.trace("Creating object on demand:\n{}", newObject.debugDumpLazily(1));

        ObjectDelta<O> addDelta = newObject.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);

        boolean isCreateOnDemandSafe = isCreateOnDemandSafe();
        if (isCreateOnDemandSafe) {
            try {
                ModelContext<O> context = modelInteractionService.previewChanges(deltas, null, task, result);
                ModelElementContext focusContext = context.getFocusContext();
                return focusContext.getObjectNew();
            } catch (Exception ex) {
                throw new ExpressionEvaluationException(ex.getMessage(), ex);
            }
        }

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges;
        try {
            executedChanges = modelService.executeChanges(deltas, null, task, result);
        } catch (ObjectAlreadyExistsException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            throw new ExpressionEvaluationException(e.getMessage(), e);
        }

        ObjectDeltaOperation deltaOperation = ObjectDeltaOperation.findAddDelta(executedChanges, newObject);
        return deltaOperation != null ? deltaOperation.getObjectDelta().getObjectToAdd() : null;
    }

    protected boolean isCreateOnDemandSafe() {
        boolean isCreateOnDemandSafe = true;   // todo default value should be true later on;

        ModelExecuteOptions options = ModelExpressionThreadLocalHolder.getLensContextRequired().getOptions();
        if (options == null || options.getSimulationOptions() == null) {
            return isCreateOnDemandSafe;
        }

        SimulationOptionsType simulation = options.getSimulationOptions();
        if (simulation.getCreateOnDemand() == null) {
            return isCreateOnDemandSafe;
        }

        return SimulationOptionType.SAFE.equals(simulation.getCreateOnDemand());
    }

    // Override the default in this case. It makes more sense like this.
    @Override
    protected boolean isIncludeNullInputs() {
        return BooleanUtils.isTrue(expressionEvaluatorBean.isIncludeNullInputs());
    }

    @Override
    public String shortDebugDump() {
        return "abstractSearchExpression";
    }

    private void log(String message, boolean info, Object... params) {
        CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
    }
}
