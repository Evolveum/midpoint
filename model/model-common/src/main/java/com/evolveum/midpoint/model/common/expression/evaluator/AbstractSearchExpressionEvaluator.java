/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.util.caching.CacheConfiguration;
import com.evolveum.midpoint.util.caching.CacheConfiguration.StatisticsLevel;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.caching.CacheUtil;
import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.util.PopulatorUtil;
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
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import static com.evolveum.midpoint.util.caching.CacheConfiguration.getStatisticsLevel;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractSearchExpressionEvaluator<V extends PrismValue,D extends ItemDefinition>
            extends AbstractValueTransformationExpressionEvaluator<V,D,SearchObjectExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluator.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private ObjectResolver objectResolver;
    private ModelService modelService;
    protected CacheConfigurationManager cacheConfigurationManager;

    AbstractSearchExpressionEvaluator(QName elementName, SearchObjectExpressionEvaluatorType expressionEvaluatorType,
            D outputDefinition, Protector protector, PrismContext prismContext,
            ObjectResolver objectResolver, ModelService modelService, SecurityContextManager securityContextManager,
            LocalizationService localizationService,
            CacheConfigurationManager cacheConfigurationManager) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext, securityContextManager, localizationService);
        this.objectResolver = objectResolver;
        this.modelService = modelService;
        this.cacheConfigurationManager = cacheConfigurationManager;
    }

    protected ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    protected ModelService getModelService() {
        return modelService;
    }

    @Override
    protected List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination, boolean useNew,
            ExpressionEvaluationContext context, String contextDescription, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

//        // Too loud
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("transformSingleValue in {}\nvalueDestination: {}\nuseNew: {}",
//                    contextDescription, valueDestination, useNew);
//            // Very very loud
////            LOGGER.trace("variables:\n{}", variables.debugDump(1));
//        }

        QName targetTypeQName = getExpressionEvaluatorType().getTargetType();
        if (targetTypeQName == null) {
            targetTypeQName = getDefaultTargetType();
        }
        if (targetTypeQName != null && QNameUtil.isUnqualified(targetTypeQName)) {
            targetTypeQName = getPrismContext().getSchemaRegistry().resolveUnqualifiedTypeName(targetTypeQName);
        }

        ObjectTypes targetType = ObjectTypes.getObjectTypeFromTypeQName(targetTypeQName);
        if (targetType == null) {
            throw new SchemaException("Unknown target type "+targetTypeQName+" in "+shortDebugDump());
        }
        Class<? extends ObjectType> targetTypeClass = targetType.getClassDefinition();

        List<V> resultValues;
        ObjectQuery query = null;

        List<ItemDelta<V, D>> additionalAttributeDeltas = null;
        PopulateType populateAssignmentType = getExpressionEvaluatorType().getPopulate();
        if (populateAssignmentType != null) {
            if (outputDefinition instanceof PrismContainerDefinition) {
                additionalAttributeDeltas = PopulatorUtil.computePopulateItemDeltas(populateAssignmentType,
                        (PrismContainerDefinition<?>)outputDefinition, variables, context, contextDescription, task, result);
            } else {
                LOGGER.warn("Search expression {} applied to non-container target, ignoring populate definition", contextDescription);
            }
        }

        if (getExpressionEvaluatorType().getOid() != null) {
            resultValues = new ArrayList<>(1);
            resultValues.add(createPrismValue(getExpressionEvaluatorType().getOid(), targetTypeQName, additionalAttributeDeltas,
                    context));
        } else {

            SearchFilterType filterType = getExpressionEvaluatorType().getFilter();
            if (filterType == null) {
                throw new SchemaException("No filter in "+shortDebugDump());
            }
            query = prismContext.getQueryConverter().createObjectQuery(targetTypeClass, filterType);
            if (LOGGER.isTraceEnabled()){
                LOGGER.trace("XML query converted to: {}", query.debugDump());
            }
            query = ExpressionUtil.evaluateQueryExpressions(query, variables, context.getExpressionProfile(), context.getExpressionFactory(),
                    prismContext, context.getContextDescription(), task, result);
            if (LOGGER.isTraceEnabled()){
                LOGGER.trace("Expression in query evaluated to: {}", query.debugDump());
            }
            query = extendQuery(query, context);

            if (LOGGER.isTraceEnabled()){
                LOGGER.trace("Query after extension: {}", query.debugDump());
            }

            resultValues = executeSearchUsingCache(targetTypeClass, targetTypeQName, query, additionalAttributeDeltas, context,
                    contextDescription, task, result);

            if (resultValues.isEmpty()) {
                ObjectReferenceType defaultTargetRef = getExpressionEvaluatorType().getDefaultTargetRef();
                if (defaultTargetRef != null) {
                    resultValues.add(createPrismValue(defaultTargetRef.getOid(), targetTypeQName, additionalAttributeDeltas,
                            context));
                }
            }
        }

        if (resultValues.isEmpty() && getExpressionEvaluatorType().isCreateOnDemand() == Boolean.TRUE &&
                (valueDestination == PlusMinusZero.PLUS || valueDestination == PlusMinusZero.ZERO || useNew)) {
            String createdObjectOid = createOnDemand(targetTypeClass, variables, context, context.getContextDescription(), task, result);
            resultValues.add(createPrismValue(createdObjectOid, targetTypeQName, additionalAttributeDeltas, context));
        }

        LOGGER.trace("Search expression {} (valueDestination={}) got {} results for query {}", contextDescription, valueDestination,
                resultValues.size(), query);

        return resultValues;
    }

    protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext params) throws SchemaException, ExpressionEvaluationException {
        return query;
    }

    protected QName getDefaultTargetType() {
        return null;
    }

    protected AbstractSearchExpressionEvaluatorCache getCache() {
        return null;
    }

    protected Class<?> getCacheClass() {
        return null;
    }

    protected CacheType getCacheType() {
        return null;
    }

    private <O extends ObjectType> List<V> executeSearchUsingCache(Class<O> targetTypeClass, final QName targetTypeQName, ObjectQuery query, List<ItemDelta<V, D>> additionalAttributeDeltas,
                                                                   final ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

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

        AbstractSearchExpressionEvaluatorCache cache = getCache();
        if (cache == null) {
            if (cacheClass != null) {
                log("Cache: NULL {} ({})", false, query, targetTypeClass.getSimpleName());
                collector.registerNotAvailable(cacheClass, targetTypeClass, statisticsLevel);
            }
            return executeSearch(null, targetTypeClass, targetTypeQName, query, searchStrategy, additionalAttributeDeltas, params, contextDescription, task, result);
        }

        assert cacheClass != null && cacheType != null;

        if (!cache.supportsObjectType(targetTypeClass)) {
            log("Cache: PASS {} ({})", tracePass, query, targetTypeClass.getSimpleName());
            cache.registerPass();
            collector.registerPass(cacheClass, targetTypeClass, statisticsLevel);
            return executeSearch(null, targetTypeClass, targetTypeQName, query, searchStrategy, additionalAttributeDeltas, params, contextDescription, task, result);
        }

        //noinspection unchecked
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
        List<PrismObject> rawResult = new ArrayList<>();
        list = executeSearch(rawResult, targetTypeClass, targetTypeQName, query, searchStrategy, additionalAttributeDeltas, params, contextDescription, task, result);
        if (list != null && !list.isEmpty()) {
            // we don't want to cache negative results (e.g. if used with focal objects it might mean that they would be attempted to create multiple times)
            //noinspection unchecked
            cache.putQueryResult(targetTypeClass, query, searchStrategy, params, list, rawResult, prismContext);
        }
        return list;
    }

    private ObjectSearchStrategyType getSearchStrategy() {
        SearchObjectExpressionEvaluatorType evaluator = getExpressionEvaluatorType();
        if (evaluator.getSearchStrategy() != null) {
            return evaluator.getSearchStrategy();
        }
        return ObjectSearchStrategyType.IN_REPOSITORY;
    }

    private <O extends ObjectType> List<V> executeSearch(List<PrismObject> rawResult,
                                                         Class<O> targetTypeClass, final QName targetTypeQName, ObjectQuery query,
                                                         ObjectSearchStrategyType searchStrategy,
                                                         List<ItemDelta<V, D>> additionalAttributeDeltas,
                                                         ExpressionEvaluationContext params, String contextDescription,
                                                         Task task, OperationResult result)
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
                return executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, false, false, additionalAttributeDeltas, params, contextDescription, task, result);
            case ON_RESOURCE:
                return executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, true, true, additionalAttributeDeltas, params, contextDescription, task, result);
            case ON_RESOURCE_IF_NEEDED:
                List<V> inRepo = executeSearchAttempt(rawResult, targetTypeClass, targetTypeQName, query, false, false, additionalAttributeDeltas, params, contextDescription, task, result);
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

    private <O extends ObjectType> List<V> executeSearchAttempt(List<PrismObject> rawResults, Class<O> targetTypeClass,
            QName targetTypeQName, ObjectQuery query, boolean searchOnResource, boolean tryAlsoRepository,
            List<ItemDelta<V, D>> additionalAttributeDeltas, ExpressionEvaluationContext params, String contextDescription,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        final List<V> valueResults = new ArrayList<>();

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        if (!searchOnResource) {
            options.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
        }
        extendOptions(options, searchOnResource);

        try {
            executeSearch(valueResults, rawResults, targetTypeClass, targetTypeQName, query, options, task, result, params, additionalAttributeDeltas);
        } catch (IllegalStateException e) { // this comes from checkConsistence methods
            throw new IllegalStateException(e.getMessage()+" in "+contextDescription, e);
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage()+" in "+contextDescription, e);
        } catch (SystemException e) {
            throw new SystemException(e.getMessage()+" in "+contextDescription, e);
        } catch (CommunicationException | ConfigurationException
                | SecurityViolationException e) {
            if (searchOnResource && tryAlsoRepository) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
                try {
                    executeSearch(valueResults, rawResults, targetTypeClass, targetTypeQName, query, options, task, result, params,
                            additionalAttributeDeltas);
                } catch (SchemaException e1) {
                    throw new SchemaException(e1.getMessage()+" in "+contextDescription, e1);
                } catch (CommunicationException | ConfigurationException
                        | SecurityViolationException e1) {
                    // TODO improve handling of exception.. we do not want to
                    // stop whole projection computation, but what to do if the
                    // shadow for group doesn't exist? (MID-2107)
                    throw new ExpressionEvaluationException("Unexpected expression exception "+e+": "+e.getMessage(), e);
                }
            } else {
                throw new ExpressionEvaluationException("Unexpected expression exception "+e+": "+e.getMessage(), e);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Assignment expression resulted in {} objects, using query:\n{}",
                    valueResults.size(), query.debugDump());
        }

        return valueResults;
    }

    private <O extends ObjectType> void executeSearch(List<V> valueResults, List<PrismObject> rawResults, Class<O> targetTypeClass,
            QName targetTypeQName, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task,
            OperationResult opResult, ExpressionEvaluationContext params, List<ItemDelta<V, D>> additionalAttributeDeltas)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        // TODO: perhaps we should limit query to some reasonably high number of results?
        SearchResultList<PrismObject<O>> objects = objectResolver.searchObjects(targetTypeClass, query, options, task, opResult);
        for (PrismObject<O> object : objects) {
            if (rawResults != null) {
                rawResults.add(object);
            }
            valueResults.add(createPrismValue(object.getOid(), targetTypeQName, additionalAttributeDeltas, params));
        }
    }

    protected void extendOptions(Collection<SelectorOptions<GetOperationOptions>> options, boolean searchOnResource) {
        // Nothing to do. To be overridden by subclasses
    }

    // e.g parameters, activation for assignment etc.


    protected abstract V createPrismValue(String oid, QName targetTypeQName, List<ItemDelta<V, D>> additionalAttributeDeltas, ExpressionEvaluationContext params);

    private <O extends ObjectType> String createOnDemand(Class<O> targetTypeClass, ExpressionVariables variables,
            ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Going to create assignment targets on demand, variables:\n{}", variables.formatVariables());
        }
        PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(targetTypeClass);
        PrismObject<O> newObject = objectDefinition.instantiate();

        PopulateType populateObject = getExpressionEvaluatorType().getPopulateObject();

        if (populateObject == null) {
            LOGGER.warn("No populateObject in assignment expression in {}, "
                    + "object created on demand will be empty. Subsequent operations will most likely fail", contextDescription);
        } else {

            List<ItemDelta<V, D>> populateDeltas = PopulatorUtil.computePopulateItemDeltas(populateObject,
                    objectDefinition, variables, params, contextDescription, task, result);
            ItemDeltaCollectionsUtil.applyTo(populateDeltas, newObject);

        }

        LOGGER.debug("Creating object on demand from {}: {}", contextDescription, newObject);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating object on demand:\n{}", newObject.debugDump(1));
        }

        ObjectDelta<O> addDelta = newObject.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges;
        try {
            executedChanges = modelService.executeChanges(deltas, null, task, result);
        } catch (ObjectAlreadyExistsException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            throw new ExpressionEvaluationException(e.getMessage(), e);
        }

        return ObjectDeltaOperation.findAddDeltaOid(executedChanges, newObject);
    }


    // Override the default in this case. It makes more sense like this.
    @Override
    protected boolean isIncludeNullInputs() {
        Boolean includeNullInputs = getExpressionEvaluatorType().isIncludeNullInputs();
        return includeNullInputs != null && includeNullInputs;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "abstractSearchExpression";
    }

    private void log(String message, boolean info, Object... params) {
        CacheUtil.log(LOGGER, PERFORMANCE_ADVISOR, message, info, params);
    }
}
