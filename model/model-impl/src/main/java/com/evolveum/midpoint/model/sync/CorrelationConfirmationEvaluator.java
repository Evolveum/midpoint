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

package com.evolveum.midpoint.model.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.camel.spi.Required;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.UnaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@Component
public class CorrelationConfirmationEvaluator {

	private static transient Trace LOGGER = TraceManager.getTrace(CorrelationConfirmationEvaluator.class);

	@Autowired(required = true)
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;
	
	public List<PrismObject<UserType>> findUsersByCorrelationRule(ShadowType currentShadow,
			List<QueryType> queries, ResourceType resourceType, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		if (queries == null || queries.isEmpty()) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceType);
			return null;
		}

		List<PrismObject<UserType>> users = null;
		if (queries.size() == 1){
			if (satisfyCondition(currentShadow, queries.get(0), resourceType, "Condition expression", result)){
				LOGGER.trace("Condition {} in correlation expression evaluated to true", queries.get(0).getCondition());
				users = findUsersByCorrelationRule(currentShadow, queries.get(0), resourceType, result);
			}
			
		} else {

			for (QueryType query : queries) {
				//TODO: better description
				if (satisfyCondition(currentShadow, query, resourceType, "Condition expression", result)) {
					LOGGER.trace("Condition {} in correlation expression evaluated to true", query.getCondition());
					List<PrismObject<UserType>> foundUsers = findUsersByCorrelationRule(
							currentShadow, query, resourceType, result);
					if (foundUsers == null && users == null) {
						continue;
					}
					if (foundUsers != null && foundUsers.isEmpty() && users == null) {
						users = new ArrayList<PrismObject<UserType>>();
					}

					if (users == null && foundUsers != null) {
						users = foundUsers;
					}
					if (users != null && !users.isEmpty() && foundUsers != null && !foundUsers.isEmpty()) {
						for (PrismObject<UserType> foundUser : foundUsers) {
							if (!contains(users, foundUser)) {
								users.add(foundUser);
							}
						}
					}
				}
			}
		}
		
		if (users != null) {
			LOGGER.debug(
					"SYNCHRONIZATION: CORRELATION: expression for {} returned {} users: {}",
					new Object[] { currentShadow, users.size(),
							PrettyPrinter.prettyPrint(users, 3) });
		}
		return users;
	}
	
	private boolean satisfyCondition(ShadowType currentShadow, QueryType query,
			ResourceType resourceType, String shortDesc,
			OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException {
		
		if (query.getCondition() == null){
			return true;
		}
		
		ExpressionType condition = createExpression((Element) query.getCondition());
		Map<QName, Object> variables = getDefaultXPathVariables(null,currentShadow, resourceType);
		ItemDefinition outputDefinition = new PrismPropertyDefinition(
				ExpressionConstants.OUTPUT_ELMENT_NAME,
				ExpressionConstants.OUTPUT_ELMENT_NAME, DOMUtil.XSD_BOOLEAN,
				prismContext);
		PrismPropertyValue<Boolean> satisfy = evaluate(variables,
				outputDefinition, condition, shortDesc, parentResult);
		if (satisfy.getValue() == null) {
			return false;
		}

		return satisfy.getValue();
	}

	private boolean contains(List<PrismObject<UserType>> users, PrismObject<UserType> foundUser){
		for (PrismObject<UserType> user : users){
			if (user.getOid().equals(foundUser.getOid())){
				return true;
			}
		}
		return false;
	}
	
	
		
		private List<PrismObject<UserType>> findUsersByCorrelationRule(ShadowType currentShadow, QueryType query, ResourceType resourceType, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
			Element element = query.getFilter();
			if (element == null) {
				LOGGER.warn("Correlation rule for resource '{}' doesn't contain query filter, "
						+ "returning empty list of users.", resourceType);
				return null;
			}
			
			ObjectQuery q = null;
			try {
				q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
				q = updateFilterWithAccountValues(currentShadow, resourceType, q, "Correlation expression", result);
				if (q == null) {
					// Null is OK here, it means that the value in the filter
					// evaluated
					// to null and the processing should be skipped
					return null;
				}
				
				
			} catch (SchemaException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(query));
				throw new SchemaException("Couldn't convert query.", ex);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(query));
				throw new ObjectNotFoundException("Couldn't convert query.", ex);
			} catch (ExpressionEvaluationException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(query));
				throw new ExpressionEvaluationException("Couldn't convert query.", ex);
			}
			
			List<PrismObject<UserType>> users = null;
			try {
				// query = new QueryType();
				// query.setFilter(filter);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}",
							new Object[] {q.dump() });
				}
				PagingType paging = new PagingType();
				// ObjectQuery q = QueryConvertor.createObjectQuery(UserType.class,
				// query, prismContext);
				users = repositoryService.searchObjects(UserType.class, q, null, result);

				if (users == null) {
					users = new ArrayList<PrismObject<UserType>>();
				}
			} catch (RuntimeException ex) {
				LoggingUtils.logException(LOGGER,
						"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.dump());
				throw new SystemException(
						"Couldn't search users in repository, based on filter (See logs).", ex);
			}
			
			return users;
		}


private <F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> currentShadow, 
		PrismObject<F> userType, ResourceType resourceType, QueryType query, OperationResult result){
	if (query == null) {
		LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
				+ "returning empty list of users.", resourceType);
		return false;
	}

	Element element = query.getFilter();
	if (element == null) {
		LOGGER.warn("Correlation rule for resource '{}' doesn't contain query filter, "
				+ "returning empty list of users.", resourceType);
		return false;
	}

	ObjectQuery q = null;
	try {
		q = QueryConvertor.createObjectQuery(UserType.class, query, prismContext);
		q = updateFilterWithAccountValues(currentShadow.asObjectable(), resourceType, q, "Correlation expression", result);
		LOGGER.debug("Start matching user {} with correlation eqpression {}", userType, q.dump());
		if (q == null) {
			// Null is OK here, it means that the value in the filter
			// evaluated
			// to null and the processing should be skipped
			return false;
		}
	} catch (Exception ex) {
		LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
				SchemaDebugUtil.prettyPrint(query));
		throw new SystemException("Couldn't convert query.", ex);
	}
//	List<PrismObject<UserType>> users = null;
//	try {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for {} results in filter\n{}",
					new Object[] { currentShadow, q});
		}

//		PagingType paging = new PagingType();
		return ObjectQuery.match(userType, q.getFilter(), matchingRuleRegistry);

//		if (users == null) {
//			users = new ArrayList<PrismObject<UserType>>();
//		}
//	} catch (Exception ex) {
//		LoggingUtils.logException(LOGGER,
//				"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.dump());
//		throw new SynchronizationException(
//				"Couldn't search users in repository, based on filter (See logs).", ex);
//	}

}
	public <F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> currentShadow, 
			PrismObject<F> userType, ResourceType resourceType, OperationResult result){

		ObjectSynchronizationType synchronization = ResourceTypeUtil.determineSynchronization(resourceType, UserType.class);
		
		if (synchronization == null){
			LOGGER.warn(
					"Resource does not support synchornization. Skipping evaluation correlation/confirmation for user {} and account {}",
					userType, currentShadow);
			return false;
		}
		
		List<QueryType> queries = synchronization.getCorrelation();
		
		for (QueryType query : queries){
			
			if (true && matchUserCorrelationRule(currentShadow, userType, resourceType, query, result)){
				LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} match user: {}", new Object[] {
						currentShadow, userType });
				return true;
			}
		}
		
		
		LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} does not match user: {}", new Object[] {
				currentShadow, userType });
		return false;
	}

		public List<PrismObject<UserType>> findUserByConfirmationRule(List<PrismObject<UserType>> users,
			ShadowType currentShadow, ResourceType resource, ExpressionType expression, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException
			 {

		List<PrismObject<UserType>> list = new ArrayList<PrismObject<UserType>>();
		for (PrismObject<UserType> user : users) {
			try {
				UserType userType = user.asObjectable();
				boolean confirmedUser = evaluateConfirmationExpression(userType,
						currentShadow, resource, expression, result);
				if (user != null && confirmedUser) {
					list.add(user);
				}
			} catch (RuntimeException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new SystemException("Couldn't confirm user " + user.getName(), ex);
			} catch (ExpressionEvaluationException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new ExpressionEvaluationException("Couldn't confirm user " + user.getName(), ex);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new ObjectNotFoundException("Couldn't confirm user " + user.getName(), ex);
			} catch (SchemaException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getName());
				throw new SchemaException("Couldn't confirm user " + user.getName(), ex);
			}
		}

		LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: expression for {} matched {} users.", new Object[] {
				currentShadow, list.size() });
		return list;
	}

	private ObjectQuery updateFilterWithAccountValues(ShadowType currentShadow, ResourceType resource,
			ObjectQuery query, String shortDesc, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		if (query.getFilter() == null) {
			LOGGER.trace("No filter provivided, skipping updating filter");
			return null;
		}
		
		ObjectFilter filter = query.getFilter();
		
		LOGGER.trace("updateFilterWithAccountValues::begin");

		evaluateFilterExpressions(filter, currentShadow, resource, shortDesc, result);
		
		LOGGER.trace("updateFilterWithAccountValues::end");
		return query;
	}

	
	private void evaluateFilterExpressions(ObjectFilter filter, ShadowType currentShadow, ResourceType resource, String shortDesc, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		if (filter instanceof LogicalFilter){
			List<ObjectFilter> conditions = ((LogicalFilter) filter).getCondition();
			
			for (ObjectFilter condition : conditions){
				evaluateFilterExpressions(condition, currentShadow, resource, shortDesc, result);
			}
			
			return;
		}
		
		Element valueExpressionElement = filter.getExpression();
		if (valueExpressionElement == null
				&& (((PropertyValueFilter) filter).getValues() == null || ((PropertyValueFilter) filter).getValues().isEmpty())) {
			LOGGER.warn("No valueExpression in rule for {}", currentShadow);
			return;
		}
		
		ExpressionType valueExpression = createExpression(valueExpressionElement);			
		
		try {
			PrismPropertyValue expressionResult = evaluateExpression(currentShadow, resource,
					valueExpression, filter, shortDesc, result);

			if (expressionResult == null || expressionResult.isEmpty()) {
				LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
						valueExpression);
				return;
			}
			// TODO: log more context
			LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.", new Object[] {
					currentShadow, expressionResult });
			if (filter instanceof EqualsFilter) {
				((EqualsFilter) filter).setValue(expressionResult);
				filter.setExpression(null);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Transforming filter to:\n{}", filter.dump());
			}
		} catch (RuntimeException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new SystemException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);

		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new SchemaException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new ObjectNotFoundException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		} catch (ExpressionEvaluationException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".", ex);
			throw new ExpressionEvaluationException("Couldn't evaluate expression" + valueExpression + ": "
					+ ex.getMessage(), ex);
		}

	}
	
	private ExpressionType createExpression(Element valueExpressionElement) throws SchemaException{
		ExpressionType valueExpression = null;
		try {
			valueExpression = prismContext.getPrismJaxbProcessor().toJavaValue(
					valueExpressionElement, ExpressionType.class);
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Filter transformed to expression\n{}", valueExpression);
			}
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Expression element couldn't be transformed.", ex);
			throw new SchemaException("Expression element couldn't be transformed: " + ex.getMessage(), ex);
		}
		
		return valueExpression;

	}

	public static Map<QName, Object> getDefaultXPathVariables(UserType user,
			ShadowType shadow, ResourceType resource) {
		
		Map<QName, Object> variables = new HashMap<QName, Object>();
		if (user != null) {
			variables.put(ExpressionConstants.VAR_USER, user.asPrismObject());
		}

		if (shadow != null) {
			variables.put(ExpressionConstants.VAR_ACCOUNT, shadow.asPrismObject());
		}

		if (resource != null) {
			variables.put(ExpressionConstants.VAR_RESOURCE, resource.asPrismObject());
		}

		return variables;
	}
	
	private PrismPropertyValue evaluateExpression(ShadowType currentShadow,
			ResourceType resource, ExpressionType valueExpression, ObjectFilter filter, String shortDesc,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
	Map<QName, Object> variables = getDefaultXPathVariables(null, currentShadow, resource);
		
		//TODO rafactor after new query engine is implemented
		ItemDefinition outputDefinition = null;
		if (filter instanceof ValueFilter){
			outputDefinition = ((ValueFilter)filter).getDefinition();
		}
		
		if (outputDefinition == null){
			outputDefinition =  new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
					DOMUtil.XSD_STRING, prismContext);
		}
		
		return evaluate(variables, outputDefinition, valueExpression, shortDesc, parentResult);
		
		
//		String expressionResult = expressionHandler.evaluateExpression(currentShadow, valueExpression,
//				shortDesc, result);
   	}
	
	private PrismPropertyValue evaluate(Map<QName, Object> variables,
			ItemDefinition outputDefinition, ExpressionType valueExpression,
			String shortDesc, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException{
		
		Expression<PrismPropertyValue> expression = expressionFactory.makeExpression(valueExpression,
				outputDefinition, shortDesc, parentResult);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, parentResult);
		PrismValueDeltaSetTriple<PrismPropertyValue> outputTriple = expression.evaluate(params);
		
		LOGGER.trace("Result of the expression evaluation: {}", outputTriple);
		
		if (outputTriple == null) {
			return null;
		}
		Collection<PrismPropertyValue> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }

        return nonNegativeValues.iterator().next();
	}
	
	public boolean evaluateConfirmationExpression(UserType user, ShadowType shadow, ResourceType resource,
			ExpressionType expressionType, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		Map<QName, Object> variables = getDefaultXPathVariables(user, shadow, resource);
		String shortDesc = "confirmation expression for "+resource.asPrismObject();
		
		PrismPropertyDefinition outputDefinition = new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, 
				outputDefinition, shortDesc, result);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(params);
		Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        PrismPropertyValue<Boolean> resultpval = nonNegativeValues.iterator().next();
        if (resultpval == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        Boolean resultVal = resultpval.getValue();
        if (resultVal == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
		return resultVal;
	}

}
