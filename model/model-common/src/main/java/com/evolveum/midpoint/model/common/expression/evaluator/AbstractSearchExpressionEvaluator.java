/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang.BooleanUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTargetDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractSearchExpressionEvaluator<V extends PrismValue> 
			extends AbstractValueTransformationExpressionEvaluator<V,SearchObjectExpressionEvaluatorType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluator.class);
	
	private PrismContext prismContext;
	private ItemDefinition outputDefinition;
	private Protector protector;
	private ObjectResolver objectResolver;
	private ModelService modelService;

	protected AbstractSearchExpressionEvaluator(SearchObjectExpressionEvaluatorType expressionEvaluatorType, 
			ItemDefinition outputDefinition, Protector protector, ObjectResolver objectResolver, 
			ModelService modelService, PrismContext prismContext) {
		super(expressionEvaluatorType);
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
			ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("transformSingleValue in {}\nvariables:\n{}\nvalueDestination: {}\nuseNew: {}",
//					new Object[]{contextDescription, variables.debugDump(1), valueDestination, useNew});
//		}
		
		QName targetTypeQName = getExpressionEvaluatorType().getTargetType();
		if (targetTypeQName == null) {
			targetTypeQName = getDefaultTargetType();
		}
		ObjectTypes targetType = ObjectTypes.getObjectTypeFromTypeQName(targetTypeQName);
		if (targetType == null) {
			throw new SchemaException("Unknown target type "+targetTypeQName+" in "+shortDebugDump());
		}
		Class<? extends ObjectType> targetTypeClass = targetType.getClassDefinition();
		
		List<V> resultValues = null;
		ObjectQuery query = null;
		
		if (getExpressionEvaluatorType().getOid() != null) {
			resultValues = new ArrayList<>(1);
			resultValues.add(createPrismValue(getExpressionEvaluatorType().getOid(), targetTypeQName, params));
		} else {
		
			SearchFilterType filterType = getExpressionEvaluatorType().getFilter();
			if (filterType == null) {
				throw new SchemaException("No filter in "+shortDebugDump());
			}
			query = QueryJaxbConvertor.createObjectQuery(targetTypeClass, filterType, prismContext);
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("XML query converted to: {}", query.debugDump());
			}
			query = ExpressionUtil.evaluateQueryExpressions(query, variables, params.getExpressionFactory(), 
					prismContext, params.getContextDescription(), task, result);
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("Expression in query evalueated to: {}", query.debugDump());
			}
			query = extendQuery(query, params);
			
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("Query after extension: {}", query.debugDump());
			}
			
			resultValues = executeSearch(targetTypeClass, targetTypeQName, query, params, params.getResult());
		}
			
		if (resultValues.isEmpty() && getExpressionEvaluatorType().isCreateOnDemand() == Boolean.TRUE &&
				(valueDestination == PlusMinusZero.PLUS || valueDestination == PlusMinusZero.ZERO || useNew)) {
			String createdObjectOid = createOnDemand(targetTypeClass, variables, params, params.getContextDescription(), task, params.getResult());
			resultValues.add(createPrismValue(createdObjectOid, targetTypeQName, params));
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

	private <O extends ObjectType> List<V> executeSearch(Class<O> targetTypeClass,
			final QName targetTypeQName, ObjectQuery query, final ExpressionEvaluationContext params, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException {
		final List<V> list = new ArrayList<V>();
		
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		if (!BooleanUtils.isTrue(getExpressionEvaluatorType().isSearchOnResource())) {
			options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		}
		
		ResultHandler<O> handler = new ResultHandler<O>() {
			@Override
			public boolean handle(PrismObject<O> object, OperationResult parentResult) {
				list.add(createPrismValue(object.getOid(), targetTypeQName, params));

				// TODO: we should count results and stop after some reasonably high number?
				
				return true;
			}
		};
		
		try {
			objectResolver.searchIterative(targetTypeClass, query, options, handler, result);
		} catch (SchemaException | CommunicationException | ConfigurationException 
				| SecurityViolationException e) {
			if (BooleanUtils.isTrue(getExpressionEvaluatorType().isSearchOnResource())) {
				options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
				try {
					objectResolver.searchIterative(targetTypeClass, query, options, handler, result);
				} catch (SchemaException | CommunicationException | ConfigurationException
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
	
	protected abstract V createPrismValue(String oid, QName targetTypeQName, ExpressionEvaluationContext params);
	
	private <O extends ObjectType> String createOnDemand(Class<O> targetTypeClass, ExpressionVariables variables, 
			ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Going to create assignment targets on demand, variables:\n{}", variables.formatVariables());
		}
		PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(targetTypeClass);
		PrismObject<O> newObject = objectDefinition.instantiate();
		
		PopulateObjectType populateObject = getExpressionEvaluatorType().getPopulateObject();
		if (populateObject == null) {
			LOGGER.warn("No populateObject in assignment expression in {}, "
					+ "object created on demand will be empty. Subsequent operations will most likely fail", contextDescription);
		} else {			
			for (PopulateItemType populateItem: populateObject.getPopulateItem()) {
				
				ItemDelta<PrismValue> itemDelta = evaluatePopulateExpression(populateItem, variables, params, 
						objectDefinition, contextDescription, task, result);
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

	private <X extends PrismValue, O extends ObjectType> ItemDelta<X> evaluatePopulateExpression(PopulateItemType populateItem,
			ExpressionVariables variables, ExpressionEvaluationContext params, PrismObjectDefinition<O> objectDefinition,
			String contextDescription, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ExpressionType expressionType = populateItem.getExpression();
		if (expressionType == null) {
			LOGGER.warn("No expression in populateObject in assignment expression in {}, "
					+ "skipping. Subsequent operations will most likely fail", contextDescription);
			return null;
		}
		
		MappingTargetDeclarationType targetType = populateItem.getTarget();
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
		ItemDefinition propOutputDefinition = ExpressionUtil.resolveDefinitionPath(targetPath, variables, 
				objectDefinition, "target definition in "+contextDescription);
		if (propOutputDefinition == null) {
			throw new SchemaException("No target item that would conform to the path "+targetPath+" in "+contextDescription);
		}
		
		String expressionDesc = "expression in assignment expression in "+contextDescription;
		ExpressionFactory expressionFactory = params.getExpressionFactory();
		Expression<X> expression = expressionFactory.makeExpression(expressionType, propOutputDefinition, 
				expressionDesc, result);
		ExpressionEvaluationContext expressionParams = new ExpressionEvaluationContext(null, variables, 
				expressionDesc, task, result);
		expressionParams.setExpressionFactory(expressionFactory);
		expressionParams.setStringPolicyResolver(params.getStringPolicyResolver());
		expressionParams.setDefaultTargetContext(params.getDefaultTargetContext());
		expressionParams.setSkipEvaluationMinus(true);
		expressionParams.setSkipEvaluationPlus(false);
		PrismValueDeltaSetTriple<X> outputTriple = expression.evaluate(expressionParams);
		LOGGER.trace("output triple: {}", outputTriple.debugDump());
		Collection<X> pvalues = outputTriple.getNonNegativeValues();
		
		// Maybe not really clean but it works. TODO: refactor later
		NameItemPathSegment first = (NameItemPathSegment)targetPath.first();
		if (first.isVariable()) {
			targetPath = targetPath.rest();
		}
		
		ItemDelta<X> itemDelta = propOutputDefinition.createEmptyDelta(targetPath);
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
