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
package com.evolveum.midpoint.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.mutable.MutableInt;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.ExpressionVariables;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
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

	public AssignmentExpressionEvaluator(AssignmentExpressionEvaluatorType expressionEvaluatorType, 
			ItemDefinition outputDefinition, Protector protector, ObjectResolver objectResolver, 
			PrismContext prismContext) {
		super(expressionEvaluatorType);
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
		this.objectResolver = objectResolver;
	}
	
	@Override
	protected List<V> transformSingleValue(ExpressionVariables variables, boolean useNew,
			ExpressionEvaluationContext params, String contextDescription, OperationResult result) 
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

		ExpressionUtil.evaluateFilterExpressions(query.getFilter(), variables.getMap(), params.getExpressionFactory(), 
				prismContext, params.getContextDescription(), params.getResult());
		
		return (List<V>) constructTriple(targetTypeClass, targetTypeQName, query, params.getContextDescription(), params.getResult());		
	}

	private <O extends ObjectType> List<PrismContainerValue<AssignmentType>> constructTriple(Class<O> targetTypeClass,
			final QName targetTypeQName, ObjectQuery query, final String shortDesc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException {
		final List<PrismContainerValue<AssignmentType>> list = new ArrayList<PrismContainerValue<AssignmentType>>();
		
		ResultHandler<O> handler = new ResultHandler<O>() {
			@Override
			public boolean handle(PrismObject<O> object, OperationResult parentResult) {
				AssignmentType assignmentType = new AssignmentType();
				PrismContainerValue<AssignmentType> assignmentCVal = assignmentType.asPrismContainerValue();
				
				ObjectReferenceType targetRef = new ObjectReferenceType();
				targetRef.setOid(object.getOid());
				targetRef.setType(targetTypeQName);
				assignmentType.setTargetRef(targetRef);
				
				list.add(assignmentCVal);
				
				try {
					prismContext.adopt(assignmentCVal, FocusType.COMPLEX_TYPE, new ItemPath(FocusType.F_ASSIGNMENT));
					if (InternalsConfig.consistencyChecks) {
						assignmentCVal.assertDefinitions("assignmentCVal in assignment expression in "+shortDesc);
					}
				} catch (SchemaException e) {
					// Should not happen
					throw new SystemException(e);
				}
					
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "assignmentExpression";
	}

}
