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
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.mutable.MutableInt;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingTargetDeclarationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PopulateItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PopulateObjectType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

/**
 * @author Radovan Semancik
 */
public class AssignmentExpressionEvaluator<V extends PrismValue> 
			extends AbstractValueTransformationExpressionEvaluator<V,AssignmentExpressionEvaluatorType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentExpressionEvaluator.class);
	
	private PrismContext prismContext;
	private ItemDefinition outputDefinition;
	private Protector protector;
	private ObjectResolver objectResolver;
	private ModelService modelService;

	public AssignmentExpressionEvaluator(AssignmentExpressionEvaluatorType expressionEvaluatorType, 
			ItemDefinition outputDefinition, Protector protector, ObjectResolver objectResolver, 
			ModelService modelService, PrismContext prismContext) {
		super(expressionEvaluatorType);
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
		this.objectResolver = objectResolver;
		this.modelService = modelService;
	}
	
	@Override
	protected List<V> transformSingleValue(ExpressionVariables variables, PlusMinusZero valueDestination, boolean useNew,
			ExpressionEvaluationContext params, String contextDescription, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		final QName targetTypeQName = getExpressionEvaluatorType().getTargetType();
		ObjectTypes targetType = ObjectTypes.getObjectTypeFromTypeQName(targetTypeQName);
		if (targetType == null) {
			throw new SchemaException("Unknown target type "+targetTypeQName+" in assignment expression");
		}
		Class<? extends ObjectType> targetTypeClass = targetType.getClassDefinition();
		
		QueryType queryType = getExpressionEvaluatorType().getQuery();
		if (queryType == null) {
			throw new SchemaException("No query in assignment expression");
		}
		ObjectQuery query = QueryConvertor.createObjectQuery(targetTypeClass, queryType, prismContext);

		ExpressionUtil.evaluateFilterExpressions(query.getFilter(), variables, params.getExpressionFactory(), 
				prismContext, params.getContextDescription(), task, result);
		
		List<PrismContainerValue<AssignmentType>> searchResults = executeSearch(targetTypeClass, targetTypeQName, query, params.getContextDescription(), params.getResult());
		
		if (searchResults.isEmpty() && getExpressionEvaluatorType().isCreateOnDemand() == Boolean.TRUE &&
				(valueDestination == PlusMinusZero.PLUS || valueDestination == PlusMinusZero.ZERO || useNew)) {
			String createdObjectOid = createOnDemand(targetTypeClass, variables, params, params.getContextDescription(), task, params.getResult());
			searchResults.add(createAssignmentCVal(createdObjectOid, targetTypeQName, contextDescription));
		}
		
		return (List<V>) searchResults;
	}

	private <O extends ObjectType> List<PrismContainerValue<AssignmentType>> executeSearch(Class<O> targetTypeClass,
			final QName targetTypeQName, ObjectQuery query, final String shortDesc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException {
		final List<PrismContainerValue<AssignmentType>> list = new ArrayList<PrismContainerValue<AssignmentType>>();
		
		ResultHandler<O> handler = new ResultHandler<O>() {
			@Override
			public boolean handle(PrismObject<O> object, OperationResult parentResult) {
				list.add(createAssignmentCVal(object.getOid(), targetTypeQName, shortDesc));

				// TODO: we should count results and stop after some reasonably high number?
				
				return true;
			}
		};
		
		try {
			objectResolver.searchIterative(targetTypeClass, query, handler, result);
		} catch (SchemaException | CommunicationException | ConfigurationException 
				| SecurityViolationException e) {
			throw new ExpressionEvaluationException("Unexpected expressione exception "+e+": "+e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			throw e;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment expression resulted in {} objects, using query:\n{}",
					list.size(), query.dump());
		}
		
		return list;
	}
	
	private PrismContainerValue<AssignmentType> createAssignmentCVal(String oid, QName targetTypeQName, String shortDesc) {
		AssignmentType assignmentType = new AssignmentType();
		PrismContainerValue<AssignmentType> assignmentCVal = assignmentType.asPrismContainerValue();
		
		ObjectReferenceType targetRef = new ObjectReferenceType();
		targetRef.setOid(oid);
		targetRef.setType(targetTypeQName);
		assignmentType.setTargetRef(targetRef);
		
		try {
			prismContext.adopt(assignmentCVal, FocusType.COMPLEX_TYPE, new ItemPath(FocusType.F_ASSIGNMENT));
			if (InternalsConfig.consistencyChecks) {
				assignmentCVal.assertDefinitions("assignmentCVal in assignment expression in "+shortDesc);
			}
		} catch (SchemaException e) {
			// Should not happen
			throw new SystemException(e);
		}
		
		return assignmentCVal;
	}
	
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
			LOGGER.trace("Creating object on demand:\n{}", newObject.dump());
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
		Element pathElement = targetType.getPath();
		if (pathElement == null) {
			throw new SchemaException("No path in target definition in "+contextDescription);
		}
		ItemPath targetPath = new XPathHolder(pathElement).toItemPath();
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
		LOGGER.trace("output triple: {}", outputTriple.dump());
		Collection<X> pvalues = outputTriple.getNonNegativeValues();
		
		// Maybe not really clean but it works. TODO: refactor later
		NameItemPathSegment first = (NameItemPathSegment)targetPath.first();
		if (first.isVariable()) {
			targetPath = targetPath.rest();
		}
		
		ItemDelta<X> itemDelta = propOutputDefinition.createEmptyDelta(targetPath);
		itemDelta.addValuesToAdd(PrismValue.cloneCollection(pvalues));
		
		LOGGER.trace("Item delta:\n{}", itemDelta.dump());
		
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
		return "assignmentExpression";
	}

}
