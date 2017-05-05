/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractSearchExpressionEvaluator<V extends PrismValue,D extends ItemDefinition> 
			extends AbstractValueTransformationExpressionEvaluator<V,D,SearchObjectExpressionEvaluatorType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluator.class);
	
	private PrismContext prismContext;
	private D outputDefinition;
	private Protector protector;
	private ObjectResolver objectResolver;
	private ModelService modelService;

	protected AbstractSearchExpressionEvaluator(SearchObjectExpressionEvaluatorType expressionEvaluatorType,
												D outputDefinition, Protector protector, ObjectResolver objectResolver,
												ModelService modelService, PrismContext prismContext, SecurityEnforcer securityEnforcer) {
		super(expressionEvaluatorType, securityEnforcer);
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
		this.objectResolver = objectResolver;
		this.modelService = modelService;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public ItemDefinition getOutputDefinition() {
		return outputDefinition;
	}

	public Protector getProtector() {
		return protector;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public ModelService getModelService() {
		return modelService;
	}

	@Override
	protected List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination, boolean useNew,
			ExpressionEvaluationContext context, String contextDescription, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("transformSingleValue in {}\nvariables:\n{}\nvalueDestination: {}\nuseNew: {}",
//					new Object[]{contextDescription, variables.debugDump(1), valueDestination, useNew});
//		}
		
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
		
		List<V> resultValues = null;
		ObjectQuery query = null;
		
		List<ItemDelta<V, D>> additionalAttributeDeltas = null;
		PopulateType populateAssignmentType = getExpressionEvaluatorType().getPopulate();
		if (populateAssignmentType != null) {
			additionalAttributeDeltas = collectAdditionalAttributes(populateAssignmentType, outputDefinition, variables, context, contextDescription, task, result);
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
			query = QueryJaxbConvertor.createObjectQuery(targetTypeClass, filterType, prismContext);
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("XML query converted to: {}", query.debugDump());
			}
			query = ExpressionUtil.evaluateQueryExpressions(query, variables, context.getExpressionFactory(),
					prismContext, context.getContextDescription(), task, result);
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("Expression in query evaluated to: {}", query.debugDump());
			}
			query = extendQuery(query, context);
			
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("Query after extension: {}", query.debugDump());
			}
			
			resultValues = executeSearchUsingCache(targetTypeClass, targetTypeQName, query, additionalAttributeDeltas, context, contextDescription, task, context
					.getResult());
			
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
			String createdObjectOid = createOnDemand(targetTypeClass, variables, context, context.getContextDescription(), task, context
					.getResult());
			resultValues.add(createPrismValue(createdObjectOid, targetTypeQName, additionalAttributeDeltas, context));
		}
		
		LOGGER.trace("Search expression got {} results for query {}", resultValues==null?"null":resultValues.size(), query);
		
		return (List<V>) resultValues;
	}

	protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext params) throws SchemaException, ExpressionEvaluationException {
		return query;
	}

	protected QName getDefaultTargetType() {
		return null;
	}

	// subclasses may provide caching
	protected AbstractSearchExpressionEvaluatorCache getCache() {
		return null;
	}

	private <O extends ObjectType> List<V> executeSearchUsingCache(Class<O> targetTypeClass, final QName targetTypeQName, ObjectQuery query, List<ItemDelta<V, D>> additionalAttributeDeltas, 
																   final ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		ObjectSearchStrategyType searchStrategy = getSearchStrategy();

		AbstractSearchExpressionEvaluatorCache cache = getCache();
		if (cache == null) {
			return executeSearch(null, targetTypeClass, targetTypeQName, query, searchStrategy, additionalAttributeDeltas, params, contextDescription, task, result);
		}

		List<V> list = cache.getQueryResult(targetTypeClass, query, searchStrategy, params, prismContext);
		if (list != null) {
			LOGGER.trace("Cache: HIT {} ({})", query, targetTypeClass.getSimpleName());
			return CloneUtil.clone(list);
		}
		LOGGER.trace("Cache: MISS {} ({})", query, targetTypeClass.getSimpleName());
		List<PrismObject> rawResult = new ArrayList<>();
		list = executeSearch(rawResult, targetTypeClass, targetTypeQName, query, searchStrategy, additionalAttributeDeltas, params, contextDescription, task, result);
		if (list != null && !list.isEmpty()) {
			// we don't want to cache negative results (e.g. if used with focal objects it might mean that they would be attempted to create multiple times)
			cache.putQueryResult(targetTypeClass, query, searchStrategy, params, list, rawResult, prismContext);
		}
		return list;
	}

	private ObjectSearchStrategyType getSearchStrategy() {
		SearchObjectExpressionEvaluatorType evaluator = getExpressionEvaluatorType();
		if (evaluator.getSearchStrategy() != null) {
			return evaluator.getSearchStrategy();
		}
		if (BooleanUtils.isTrue(evaluator.isSearchOnResource())) {
			return ObjectSearchStrategyType.ON_RESOURCE;
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

	private <O extends ObjectType> List<V> executeSearchAttempt(final List<PrismObject> rawResult,
																Class<O> targetTypeClass, final QName targetTypeQName,
																ObjectQuery query, boolean searchOnResource, boolean tryAlsoRepository,
																final List<ItemDelta<V, D>> additionalAttributeDeltas, 
																final ExpressionEvaluationContext params, String contextDescription,
																Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		final List<V> list = new ArrayList<V>();
		
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
		if (!searchOnResource) {
			options.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
		}
		extendOptions(options, searchOnResource);
		
		ResultHandler<O> handler = new ResultHandler<O>() {
			@Override
			public boolean handle(PrismObject<O> object, OperationResult parentResult) {
				if (rawResult != null) {
					rawResult.add(object);
				}
				list.add(createPrismValue(object.getOid(), targetTypeQName, additionalAttributeDeltas, params));

				// TODO: we should count results and stop after some reasonably high number?
				
				return true;
			}
		};
		
		try {
			objectResolver.searchIterative(targetTypeClass, query, options, handler, task, result);
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
					objectResolver.searchIterative(targetTypeClass, query, options, handler, task, result);
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
		} catch (ObjectNotFoundException e) {
			throw e;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment expression resulted in {} objects, using query:\n{}",
					list.size(), query.debugDump());
		}
		
		return list;
	}
	
	protected void extendOptions(Collection<SelectorOptions<GetOperationOptions>> options, boolean searchOnResource) {
		// Nothing to do. To be overridden by subclasses
	}
	
	// e.g parameters, activation for assignment etc.
	protected <C extends Containerable> List<ItemDelta<V,D>> collectAdditionalAttributes(PopulateType fromPopulate,
			D outputDefinition, ExpressionVariables variables, ExpressionEvaluationContext params,
			String contextDescription, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		if (!(outputDefinition instanceof PrismContainerDefinition)) {
			return null;
		}
		
		List<ItemDelta<V,D>> deltas = new ArrayList<>();
		
		for (PopulateItemType populateItem: fromPopulate.getPopulateItem()) {
			
			ItemDelta<V,D> itemDelta = evaluatePopulateExpression(populateItem, variables, params, 
					(PrismContainerDefinition<C>) outputDefinition, contextDescription, false, task, result);
			if (itemDelta != null) {
				deltas.add(itemDelta);
			}
			
		}
		
		return deltas;
	}

	protected abstract V createPrismValue(String oid, QName targetTypeQName, List<ItemDelta<V, D>> additionalAttributeDeltas, ExpressionEvaluationContext params);
	
	private <O extends ObjectType> String createOnDemand(Class<O> targetTypeClass, ExpressionVariables variables, 
			ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
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
			for (PopulateItemType populateItem: populateObject.getPopulateItem()) {
				ItemDelta<?,?> itemDelta = evaluatePopulateExpression(populateItem, variables, params, 
						objectDefinition, contextDescription, true, task, result);
				if (itemDelta != null) {
					itemDelta.applyTo(newObject);
				}
			}
		}
		
		LOGGER.debug("Creating object on demand from {}: {}", contextDescription, newObject);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Creating object on demand:\n{}", newObject.debugDump());
		}
		
		ObjectDelta<O> addDelta = newObject.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);
		try {
			modelService.executeChanges(deltas, null, task, result);
		} catch (ObjectAlreadyExistsException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			throw new ExpressionEvaluationException(e.getMessage(), e);
		}
		
		return addDelta.getOid();
	}

	private <IV extends PrismValue, ID extends ItemDefinition, C extends Containerable> ItemDelta<IV,ID> evaluatePopulateExpression(PopulateItemType populateItem,
			ExpressionVariables variables, ExpressionEvaluationContext params, PrismContainerDefinition<C> objectDefinition,
			String contextDescription, boolean evaluateMinus, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ExpressionType expressionType = populateItem.getExpression();
		if (expressionType == null) {
			LOGGER.warn("No expression in populateObject in assignment expression in {}, "
					+ "skipping. Subsequent operations will most likely fail", contextDescription);
			return null;
		}
		
		VariableBindingDefinitionType targetType = populateItem.getTarget();
		if (targetType == null) {
			LOGGER.warn("No target in populateObject in assignment expression in {}, "
					+ "skipping. Subsequent operations will most likely fail", contextDescription);
			return null;
		}
        ItemPathType itemPathType = targetType.getPath();
		if (itemPathType == null) {
			throw new SchemaException("No path in target definition in "+contextDescription);
		}
		ItemPath targetPath = itemPathType.getItemPath();
		ID propOutputDefinition = ExpressionUtil.resolveDefinitionPath(targetPath, variables, 
				objectDefinition, "target definition in "+contextDescription);
		if (propOutputDefinition == null) {
			throw new SchemaException("No target item that would conform to the path "+targetPath+" in "+contextDescription);
		}
		
		String expressionDesc = "expression in assignment expression in "+contextDescription;
		ExpressionFactory expressionFactory = params.getExpressionFactory();
		Expression<IV,ID> expression = expressionFactory.makeExpression(expressionType, propOutputDefinition, 
				expressionDesc, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables,
				expressionDesc, task, result);
		context.setExpressionFactory(expressionFactory);
		context.setStringPolicyResolver(params.getStringPolicyResolver());
		context.setDefaultTargetContext(params.getDefaultTargetContext());
		context.setSkipEvaluationMinus(true);
		context.setSkipEvaluationPlus(false);
		PrismValueDeltaSetTriple<IV> outputTriple = expression.evaluate(context);
		LOGGER.trace("output triple: {}", outputTriple.debugDump());
		Collection<IV> pvalues = outputTriple.getNonNegativeValues();
		
		// Maybe not really clean but it works. TODO: refactor later
		NameItemPathSegment first = (NameItemPathSegment)targetPath.first();
		if (first.isVariable()) {
			targetPath = targetPath.rest();
		}
		
		ItemDelta<IV,ID> itemDelta = propOutputDefinition.createEmptyDelta(targetPath);
		itemDelta.addValuesToAdd(PrismValue.cloneCollection(pvalues));
		
		LOGGER.trace("Item delta:\n{}", itemDelta.debugDump());
		
		return itemDelta;
	}
	
	// Override the default in this case. It makes more sense like this.
	@Override
	protected Boolean isIncludeNullInputs() {
		Boolean superValue = super.isIncludeNullInputs();
		if (superValue != null) {
			return superValue;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "abstractSearchExpression";
	}

}
