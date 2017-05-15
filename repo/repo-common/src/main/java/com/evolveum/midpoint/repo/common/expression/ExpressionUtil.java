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
package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QueryInterpretationOfNoValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public class ExpressionUtil {

	private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);
	
	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

	public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(
			PrismValueDeltaSetTriple<V> resultTriple, ItemDefinition outputDefinition,
			Function<Object, Object> additionalConvertor,
			final ItemPath residualPath, final Protector protector, final PrismContext prismContext) {
		
		PrismValueDeltaSetTriple<V> clonedTriple = resultTriple.clone();
		
		final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
		if (resultTripleValueClass == null) {
			// triple is empty. type does not matter.
			return clonedTriple;
		}
		Class<?> expectedJavaType = XsdTypeMapper.toJavaType(outputDefinition.getTypeName());
		if (expectedJavaType == null) {
			expectedJavaType = prismContext.getSchemaRegistry()
					.getCompileTimeClass(outputDefinition.getTypeName());
		}
		if (resultTripleValueClass == expectedJavaType) {
			return clonedTriple;
		}
		final Class<?> finalExpectedJavaType = expectedJavaType;

		clonedTriple.accept((Visitor) visitable -> {
			if (visitable instanceof PrismPropertyValue<?>) {
				PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>) visitable;
				Object realVal = pval.getValue();
				if (realVal != null) {
					if (Structured.class.isAssignableFrom(resultTripleValueClass)) {
						if (residualPath != null && !residualPath.isEmpty()) {
							realVal = ((Structured) realVal).resolve(residualPath);
						}
					}
					if (finalExpectedJavaType != null) {
						Object convertedVal = convertValue(finalExpectedJavaType, additionalConvertor, realVal, protector, prismContext);
						pval.setValue(convertedVal);
					}
				}
			}
		});
		return clonedTriple;
	}

	/**
	 * Slightly more powerful version of "convert" as compared to
	 * JavaTypeConverter. This version can also encrypt/decrypt and also handles
	 * polystrings.
	 */
	public static <I, O> O convertValue(Class<O> finalExpectedJavaType, Function<Object, Object> additionalConvertor, I inputVal,
			Protector protector,
			PrismContext prismContext) {
		if (inputVal == null) {
			return null;
		}
		if (finalExpectedJavaType.isInstance(inputVal)) {
			return (O) inputVal;
		}

		Object intermediateVal;
		if (finalExpectedJavaType == ProtectedStringType.class) {
			String valueToEncrypt;
			if (inputVal instanceof String) {
				valueToEncrypt = (String) inputVal;
			} else {
				valueToEncrypt = JavaTypeConverter.convert(String.class, inputVal);
			}
			try {
				intermediateVal = protector.encryptString(valueToEncrypt);
			} catch (EncryptionException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} else if (inputVal instanceof ProtectedStringType) {
			try {
				intermediateVal = protector.decryptString((ProtectedStringType) inputVal);
			} catch (EncryptionException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} else {
			intermediateVal = inputVal;
		}

		if (additionalConvertor != null) {
			intermediateVal = additionalConvertor.apply(intermediateVal);
		}

		O convertedVal = JavaTypeConverter.convert(finalExpectedJavaType, intermediateVal);
		PrismUtil.recomputeRealValue(convertedVal, prismContext);
		return convertedVal;
	}

	public static Object resolvePath(ItemPath path, ExpressionVariables variables, Object defaultContext,
			ObjectResolver objectResolver, String shortDesc, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {

		Object root = defaultContext;
		ItemPath relativePath = path;
		ItemPathSegment first = path.first();
		String varDesc = "default context";
		if (first.isVariable()) {
			QName varName = ((NameItemPathSegment) first).getName();
			varDesc = "variable " + PrettyPrinter.prettyPrint(varName);
			relativePath = path.rest();
			if (variables.containsKey(varName)) {
				root = variables.get(varName);
			} else {
				throw new SchemaException("No variable with name " + varName + " in " + shortDesc);
			}
		}
		if (root == null) {
			return null;
		}
		if (relativePath.isEmpty()) {
			return root;
		}

		if (root instanceof ObjectReferenceType) {
			root = resolveReference((ObjectReferenceType) root, objectResolver, varDesc, shortDesc, task,
					result);
		}

		if (root instanceof Objectable) {
			return (((Objectable) root).asPrismObject()).find(relativePath);
		}
		if (root instanceof PrismObject<?>) {
			return ((PrismObject<?>) root).find(relativePath);
		} else if (root instanceof PrismContainer<?>) {
			return ((PrismContainer<?>) root).find(relativePath);
		} else if (root instanceof PrismContainerValue<?>) {
			return ((PrismContainerValue<?>) root).find(relativePath);
		} else if (root instanceof Item<?, ?>) {
			// Except for container (which is handled above)
			throw new SchemaException(
					"Cannot apply path " + relativePath + " to " + root + " in " + shortDesc);
		} else if (root instanceof ObjectDeltaObject<?>) {
			return ((ObjectDeltaObject<?>) root).findIdi(relativePath);
		} else if (root instanceof ItemDeltaItem<?, ?>) {
			return ((ItemDeltaItem<?, ?>) root).findIdi(relativePath);
		} else {
			throw new IllegalArgumentException(
					"Unexpected root " + root + " (relative path:" + relativePath + ") in " + shortDesc);
		}
	}

	public static Object convertVariableValue(Object originalValue, String variableName, ObjectResolver objectResolver,
			String contextDescription, PrismContext prismContext, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		if (originalValue instanceof PrismValue) {
			((PrismValue) originalValue).setPrismContext(prismContext);			// TODO - or revive? Or make sure prismContext is set here?
		} else if (originalValue instanceof Item) {
			((Item) originalValue).setPrismContext(prismContext);				// TODO - or revive? Or make sure prismContext is set here?
		}
		if (originalValue instanceof ObjectReferenceType) {
			try {
				originalValue = resolveReference((ObjectReferenceType)originalValue, objectResolver, variableName,
						contextDescription, task, result);
			} catch (SchemaException e) {
				throw new ExpressionSyntaxException("Schema error during variable "+variableName+" resolution in "+contextDescription+": "+e.getMessage(), e);
			}
		}
		if (originalValue instanceof PrismObject<?>) {
			return ((PrismObject<?>)originalValue).asObjectable();
		}
		if (originalValue instanceof PrismContainerValue<?>) {
			return ((PrismContainerValue<?>)originalValue).asContainerable();
		}
		if (originalValue instanceof PrismPropertyValue<?>) {
			return ((PrismPropertyValue<?>)originalValue).getValue();
		}
		if (originalValue instanceof PrismReferenceValue) {
			if (((PrismReferenceValue) originalValue).getDefinition() != null) {
				return ((PrismReferenceValue) originalValue).asReferencable();
			}
		}
		if (originalValue instanceof PrismProperty<?>) {
			PrismProperty<?> prop = (PrismProperty<?>)originalValue;
			PrismPropertyDefinition<?> def = prop.getDefinition();
			if (def != null) {
				if (def.isSingleValue()) {
					return prop.getRealValue();
				} else {
					return prop.getRealValues();
				}
			} else {
				return prop.getValues();
			}
		}
		if (originalValue instanceof PrismReference) {
			PrismReference prop = (PrismReference)originalValue;
			PrismReferenceDefinition def = prop.getDefinition();
			if (def != null) {
				if (def.isSingleValue()) {
					return prop.getRealValue();
				} else {
					return prop.getRealValues();
				}
			} else {
				return prop.getValues();
			}
		}
		if (originalValue instanceof PrismContainer<?>) {
			PrismContainer<?> container = (PrismContainer<?>)originalValue;
			PrismContainerDefinition<?> def = container.getDefinition();
			if (def != null) {
				if (def.isSingleValue()) {
					return container.getRealValue();
				} else {
					return container.getRealValues();
					
				}
			} else {
				return container.getValues();
			}
		}
		
		return originalValue;
	}

	private static PrismObject<?> resolveReference(ObjectReferenceType ref, ObjectResolver objectResolver,
			String varDesc, String contextDescription, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {
		if (ref.getOid() == null) {
			throw new SchemaException(
					"Null OID in reference in variable " + varDesc + " in " + contextDescription);
		} else {
			try {

				ObjectType objectType = objectResolver.resolve(ref, ObjectType.class, null,
						contextDescription, task, result);
				if (objectType == null) {
					throw new IllegalArgumentException(
							"Resolve returned null for " + ref + " in " + contextDescription);
				}
				return objectType.asPrismObject();

			} catch (ObjectNotFoundException e) {
				throw new ObjectNotFoundException("Object not found during variable " + varDesc
						+ " resolution in " + contextDescription + ": " + e.getMessage(), e, ref.getOid());
			} catch (SchemaException e) {
				throw new SchemaException("Schema error during variable " + varDesc + " resolution in "
						+ contextDescription + ": " + e.getMessage(), e);
			}
		}
	}

	public static <ID extends ItemDefinition> ID resolveDefinitionPath(ItemPath path,
			ExpressionVariables variables, PrismContainerDefinition<?> defaultContext, String shortDesc)
					throws SchemaException {
		while (path != null && !path.isEmpty() && !(path.first() instanceof NameItemPathSegment)) {
			path = path.rest();
		}
		Object root = defaultContext;
		ItemPath relativePath = path;
		NameItemPathSegment first = (NameItemPathSegment) path.first();
		if (first.isVariable()) {
			relativePath = path.rest();
			QName varName = first.getName();
			if (variables.containsKey(varName)) {
				Object varValue = variables.get(varName);
				if (varValue instanceof ItemDeltaItem<?, ?>) {
					root = ((ItemDeltaItem<?, ?>) varValue).getDefinition();
				} else if (varValue instanceof Item<?, ?>) {
					root = ((Item<?, ?>) varValue).getDefinition();
				} else if (varValue instanceof Objectable) {
					root = ((Objectable) varValue).asPrismObject().getDefinition();
				} else if (varValue instanceof ItemDefinition) {
					root = varValue;
				} else {
					throw new IllegalStateException("Unexpected content of variable " + varName + ": "
							+ varValue + " (" + varValue.getClass() + ")");
				}
				if (root == null) {
					throw new IllegalStateException(
							"Null definition in content of variable " + varName + ": " + varValue);
				}
			} else {
				throw new SchemaException("No variable with name " + varName + " in " + shortDesc);
			}
		}
		if (root == null) {
			return null;
		}
		if (relativePath.isEmpty()) {
			return (ID) root;
		}
		ItemDefinition result = null;
		if (root instanceof PrismObjectDefinition<?>) {
			return ((PrismObjectDefinition<?>) root).findItemDefinition(relativePath);
		} else if (root instanceof PrismContainerDefinition<?>) {
			return ((PrismContainerDefinition<?>) root).findItemDefinition(relativePath);
		} else if (root instanceof ItemDefinition) {
			// Except for container (which is handled above)
			throw new SchemaException(
					"Cannot apply path " + relativePath + " to " + root + " in " + shortDesc);
		} else {
			throw new IllegalArgumentException("Unexpected root " + root + " in " + shortDesc);
		}
	}

	public static <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV, ID> toItemDeltaItem(
			Object object, ObjectResolver objectResolver, String string, OperationResult result) {
		if (object == null) {
			return null;
		}

		if (object instanceof ItemDeltaItem<?, ?>) {
			return (ItemDeltaItem<IV, ID>) object;
		}

		if (object instanceof PrismObject<?>) {
			return (ItemDeltaItem<IV, ID>) new ObjectDeltaObject((PrismObject<?>) object, null,
					(PrismObject<?>) object);
		} else if (object instanceof Item<?, ?>) {
			return new ItemDeltaItem<IV, ID>((Item<IV, ID>) object, null, (Item<IV, ID>) object);
		} else if (object instanceof ItemDelta<?, ?>) {
			return new ItemDeltaItem<IV, ID>(null, (ItemDelta<IV, ID>) object, null);
		} else {
			throw new IllegalArgumentException("Unexpected object " + object + " " + object.getClass());
		}

	}

	public static ObjectQuery evaluateQueryExpressions(ObjectQuery origQuery, ExpressionVariables variables,
			ExpressionFactory expressionFactory, PrismContext prismContext, String shortDesc, Task task,
			OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		if (origQuery == null) {
			return null;
		}
		ObjectQuery query = origQuery.clone();
		ObjectFilter evaluatedFilter = evaluateFilterExpressionsInternal(query.getFilter(), variables,
				expressionFactory, prismContext, shortDesc, task, result);
		query.setFilter(evaluatedFilter);
		return query;
	}

	public static ObjectFilter evaluateFilterExpressions(ObjectFilter origFilter,
			ExpressionVariables variables, ExpressionFactory expressionFactory, PrismContext prismContext,
			String shortDesc, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		if (origFilter == null) {
			return null;
		}

		return evaluateFilterExpressionsInternal(origFilter, variables, expressionFactory, prismContext,
				shortDesc, task, result);
	}

	public static boolean hasExpressions(@Nullable ObjectFilter filter) {
		if (filter == null) {
			return false;
		}
		Holder<Boolean> result = new Holder<>(false);
		filter.accept(f -> {
			if (f instanceof ValueFilter) {
				ValueFilter<?, ?> vf = (ValueFilter<?, ?>) f;
				if (vf.getExpression() != null) {
					result.setValue(true);
				}
			}
		});
		return result.getValue();
	}

	private static ObjectFilter evaluateFilterExpressionsInternal(ObjectFilter filter,
			ExpressionVariables variables, ExpressionFactory expressionFactory, PrismContext prismContext,
			String shortDesc, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		if (filter == null) {
			return null;
		}

		if (filter instanceof InOidFilter) {
			ExpressionWrapper expressionWrapper = ((InOidFilter) filter).getExpression();
			if (expressionWrapper == null || expressionWrapper.getExpression() == null) {
				LOGGER.warn("No valueExpression in filter in {}. Returning original filter", shortDesc);
				InOidFilter inOidFilter = (InOidFilter) filter;
				if (inOidFilter.getOids() != null && !inOidFilter.getOids().isEmpty()){
					return filter.clone();
				}
				return NoneFilter.createNone();
			}

			ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

			try {
				Collection<String> expressionResult = evaluateStringExpression(variables, prismContext,
						valueExpression, expressionFactory, shortDesc, task, result);

				if (expressionResult == null || expressionResult.isEmpty()) {
					LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
							valueExpression);
					return createFilterForNoValue(filter, valueExpression);
				}
				// TODO: log more context
				LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
						new Object[] { shortDesc, expressionResult });

				InOidFilter evaluatedFilter = (InOidFilter) filter.clone();
				evaluatedFilter.setOids(expressionResult);
				evaluatedFilter.setExpression(null);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
				}
				return evaluatedFilter;
			} catch (Exception ex) {
				throw new ExpressionEvaluationException(ex);
			}
		} else if (filter instanceof FullTextFilter) {
			ExpressionWrapper expressionWrapper = ((FullTextFilter) filter).getExpression();
			if (expressionMissing(expressionWrapper, shortDesc)) {
				return filter.clone();
			}
			ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

			try {
				Collection<String> expressionResult = evaluateStringExpression(variables, prismContext,
						valueExpression, expressionFactory, shortDesc, task, result);
				if (expressionResult == null || expressionResult.isEmpty()) {
					LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
							valueExpression);
					return createFilterForNoValue(filter, valueExpression);
				}
				// TODO: log more context
				LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
						shortDesc, expressionResult);

				FullTextFilter evaluatedFilter = (FullTextFilter) filter.clone();
				evaluatedFilter.setValues(expressionResult);
				evaluatedFilter.setExpression(null);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
				}
				return evaluatedFilter;
			} catch (Exception ex) {
				throw new ExpressionEvaluationException(ex);
			}
		} else if (filter instanceof LogicalFilter) {
			List<ObjectFilter> conditions = ((LogicalFilter) filter).getConditions();
			LogicalFilter evaluatedFilter = ((LogicalFilter) filter).cloneEmpty();
			for (ObjectFilter condition : conditions) {
				ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(condition, variables,
						expressionFactory, prismContext, shortDesc, task, result);
				evaluatedFilter.addCondition(evaluatedSubFilter);
			}
			return evaluatedFilter;

		} else if (filter instanceof ValueFilter) {
			ValueFilter valueFilter = (ValueFilter) filter;

			if (valueFilter.getValues() != null && !valueFilter.getValues().isEmpty()) {
				// We have value. Nothing to evaluate.
				return valueFilter.clone();
			}

			ExpressionWrapper expressionWrapper = valueFilter.getExpression();
			if (expressionMissing(expressionWrapper, shortDesc)) {
				return valueFilter.clone();
			}
			ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

			try {
				PrismValue expressionResult = evaluateExpression(variables, prismContext, valueExpression,
						filter, expressionFactory, shortDesc, task, result);

				if (expressionResult == null || expressionResult.isEmpty()) {
					LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
							valueExpression);

					return createFilterForNoValue(valueFilter, valueExpression);
				}
				// TODO: log more context
				LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
						new Object[] { shortDesc, expressionResult });

				ValueFilter evaluatedFilter = valueFilter.clone();
				evaluatedFilter.setValue(expressionResult);
				evaluatedFilter.setExpression(null);
				// }
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
				}
				return evaluatedFilter;

			} catch (RuntimeException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".",
						ex);
				throw new SystemException(
						"Couldn't evaluate expression" + valueExpression + ": " + ex.getMessage(), ex);

			} catch (SchemaException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".",
						ex);
				throw new SchemaException(
						"Couldn't evaluate expression" + valueExpression + ": " + ex.getMessage(), ex);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".",
						ex);
				throw new ObjectNotFoundException(
						"Couldn't evaluate expression" + valueExpression + ": " + ex.getMessage(), ex);
			} catch (ExpressionEvaluationException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + valueExpression + ".",
						ex);
				throw new ExpressionEvaluationException(
						"Couldn't evaluate expression" + valueExpression + ": " + ex.getMessage(), ex);
			}

		} else if (filter instanceof ExistsFilter) {
			ExistsFilter evaluatedFilter = ((ExistsFilter) filter).cloneEmpty();
			ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(((ExistsFilter) filter).getFilter(), variables,
					expressionFactory, prismContext, shortDesc, task, result);
			evaluatedFilter.setFilter(evaluatedSubFilter);
			return evaluatedFilter;
		} else if (filter instanceof TypeFilter) {
			TypeFilter evaluatedFilter = ((TypeFilter) filter).cloneEmpty();
			ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(((TypeFilter) filter).getFilter(), variables,
						expressionFactory, prismContext, shortDesc, task, result);
			evaluatedFilter.setFilter(evaluatedSubFilter);
			return evaluatedFilter;
		} else if (filter instanceof OrgFilter) {
			return filter;
		} else if (filter instanceof AllFilter || filter instanceof NoneFilter || filter instanceof UndefinedFilter) {
			return filter;
		} else {
			throw new IllegalStateException("Unsupported filter type: " + filter.getClass());
		}
	}

	private static boolean expressionMissing(ExpressionWrapper expressionWrapper, String shortDesc) {
		if (expressionWrapper == null || expressionWrapper.getExpression() == null) {
			LOGGER.warn("No valueExpression in filter in {}. Returning original filter", shortDesc);
			return true;
		}
		return false;
	}

	private static ExpressionType getExpression(ExpressionWrapper expressionWrapper, String shortDesc) throws SchemaException {
		if (!(expressionWrapper.getExpression() instanceof ExpressionType)) {
			throw new SchemaException("Unexpected expression type "
					+ expressionWrapper.getExpression().getClass() + " in filter in " + shortDesc);
		}
		return (ExpressionType) expressionWrapper.getExpression();
	}

	private static ObjectFilter createFilterForNoValue(ObjectFilter filter, ExpressionType valueExpression) throws ExpressionEvaluationException {
		QueryInterpretationOfNoValueType queryInterpretationOfNoValue = valueExpression.getQueryInterpretationOfNoValue();
		if (queryInterpretationOfNoValue == null) {
			queryInterpretationOfNoValue = QueryInterpretationOfNoValueType.FILTER_EQUAL_NULL;
		}
		
		switch (queryInterpretationOfNoValue) {
			
			case FILTER_UNDEFINED:
				return UndefinedFilter.createUndefined();
			
			case FILTER_NONE:
				return NoneFilter.createNone();
			
			case FILTER_ALL:
				return AllFilter.createAll();
			
			case FILTER_EQUAL_NULL:
				if (filter instanceof ValueFilter) {
					ValueFilter evaluatedFilter = (ValueFilter) filter.clone();
					evaluatedFilter.setExpression(null);
					return evaluatedFilter;
				} else if (filter instanceof InOidFilter) {
					return NoneFilter.createNone();
				} else if (filter instanceof FullTextFilter) {
					return NoneFilter.createNone(); // because full text search for 'no value' is meaningless
				} else {
					throw new IllegalArgumentException("Unknown filter to evaluate: " + filter);
				}
			
			case ERROR:
				throw new ExpressionEvaluationException("Expression "+valueExpression+" evaluated to no value");
				
			default:
				throw new IllegalArgumentException("Unknown value "+queryInterpretationOfNoValue+" in queryInterpretationOfNoValue in "+valueExpression);
				
		}
		
	}

	private static <V extends PrismValue> V evaluateExpression(ExpressionVariables variables,
			PrismContext prismContext, ExpressionType expressionType, ObjectFilter filter,
			ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		// TODO refactor after new query engine is implemented
		ItemDefinition outputDefinition = null;
		if (filter instanceof ValueFilter) {
			outputDefinition = ((ValueFilter) filter).getDefinition();
		}

		if (outputDefinition == null) {
			outputDefinition = new PrismPropertyDefinitionImpl(ExpressionConstants.OUTPUT_ELEMENT_NAME,
					DOMUtil.XSD_STRING, prismContext);
		}

		return (V) evaluateExpression(variables, outputDefinition, expressionType, expressionFactory, shortDesc,
				task, parentResult);

		// String expressionResult =
		// expressionHandler.evaluateExpression(currentShadow, valueExpression,
		// shortDesc, result);
	}

	public static <V extends PrismValue, D extends ItemDefinition> V evaluateExpression(
			ExpressionVariables variables, D outputDefinition, ExpressionType expressionType,
			ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition,
				shortDesc, task, parentResult);

		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task,
				parentResult);
		PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context);

		LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

		if (outputTriple == null) {
			return null;
		}
		Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}
		if (nonNegativeValues.size() > 1) {
			throw new ExpressionEvaluationException("Expression returned more than one value ("
					+ nonNegativeValues.size() + ") in " + shortDesc);
		}

		return nonNegativeValues.iterator().next();
	}

	private static Collection<String> evaluateStringExpression(ExpressionVariables variables,
			PrismContext prismContext, ExpressionType expressionType, ExpressionFactory expressionFactory,
			String shortDesc, Task task, OperationResult parentResult)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		PrismPropertyDefinitionImpl<String> outputDefinition = new PrismPropertyDefinitionImpl(
				ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING, prismContext);
		outputDefinition.setMaxOccurs(-1);
		Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
				.makeExpression(expressionType, outputDefinition, shortDesc, task, parentResult);

		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task,
				parentResult);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context);

		LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

		if (outputTriple == null) {
			return null;
		}
		Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}

		return PrismValue.getRealValuesOfCollection((Collection) nonNegativeValues);
		// return nonNegativeValues.iterator().next();
	}

	public static PrismPropertyValue<Boolean> evaluateCondition(ExpressionVariables variables,
			ExpressionType expressionType, ExpressionFactory expressionFactory, String shortDesc, Task task,
			OperationResult parentResult)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ItemDefinition outputDefinition = new PrismPropertyDefinitionImpl(
				ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN,
				expressionFactory.getPrismContext());
		return (PrismPropertyValue<Boolean>) evaluateExpression(variables, outputDefinition, expressionType,
				expressionFactory, shortDesc, task, parentResult);
	}
	
	public static boolean getBooleanConditionOutput(PrismPropertyValue<Boolean> conditionOutput) {
		if (conditionOutput == null) {
			return false;
		}
		Boolean value = conditionOutput.getValue();
		if (value == null) {
			return false;
		}
		return value;
	}

	public static Map<QName, Object> compileVariablesAndSources(ExpressionEvaluationContext params) {
		Map<QName, Object> variablesAndSources = new HashMap<QName, Object>();

		if (params.getVariables() != null) {
			for (Entry<QName, Object> entry : params.getVariables().entrySet()) {
				variablesAndSources.put(entry.getKey(), entry.getValue());
			}
		}

		if (params.getSources() != null) {
			for (Source<?, ?> source : params.getSources()) {
				variablesAndSources.put(source.getName(), source);
			}
		}

		return variablesAndSources;
	}

	public static boolean hasExplicitTarget(List<MappingType> mappingTypes) {
		for (MappingType mappingType : mappingTypes) {
			if (hasExplicitTarget(mappingType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasExplicitTarget(MappingType mappingType) {
		return mappingType.getTarget() != null;
	}

	public static boolean computeConditionResult(
			Collection<PrismPropertyValue<Boolean>> booleanPropertyValues) {
		if (booleanPropertyValues == null || booleanPropertyValues.isEmpty()) {
			// No value means false
			return false;
		}
		boolean hasFalse = false;
		for (PrismPropertyValue<Boolean> pval : booleanPropertyValues) {
			Boolean value = pval.getValue();
			if (Boolean.TRUE.equals(value)) {
				return true;
			}
			if (Boolean.FALSE.equals(value)) {
				hasFalse = true;
			}
		}
		if (hasFalse) {
			return false;
		}
		// No value or all values null. Return default.
		return true;
	}

	public static PlusMinusZero computeConditionResultMode(boolean condOld, boolean condNew) {
		if (condOld && condNew) {
			return PlusMinusZero.ZERO;
		}
		if (!condOld && !condNew) {
			return null;
		}
		if (condOld && !condNew) {
			return PlusMinusZero.MINUS;
		}
		if (!condOld && condNew) {
			return PlusMinusZero.PLUS;
		}
		throw new IllegalStateException("notreached");
	}

	public static void addActorVariable(ExpressionVariables scriptVariables,
			SecurityEnforcer securityEnforcer) {
		// There can already be a value, because for mappings, we create the
		// variable before parsing sources.
		// For other scripts we do it just before the execution, to catch all
		// possible places where scripts can be executed.

		UserType oldActor = (UserType) scriptVariables.get(ExpressionConstants.VAR_ACTOR);
		if (oldActor != null) {
			return;
		}

		UserType actor = null;
		try {
			if (securityEnforcer != null) {
				if (!securityEnforcer.isAuthenticated()) {
					// This is most likely evaluation of role
					// condition before
					// the authentication is complete.
					scriptVariables.addVariableDefinition(ExpressionConstants.VAR_ACTOR, null);
					return;
				}
				MidPointPrincipal principal = securityEnforcer.getPrincipal();
				if (principal != null) {
					actor = principal.getUser();
				}
			}
			if (actor == null) {
				LOGGER.debug("Couldn't get principal information - the 'actor' variable is set to null");
			}
		} catch (SecurityViolationException e) {
			LoggingUtils.logUnexpectedException(LOGGER,
					"Couldn't get principal information - the 'actor' variable is set to null", e);
		}
		scriptVariables.addVariableDefinition(ExpressionConstants.VAR_ACTOR, actor);
	}

	public static <D extends ItemDefinition> Object convertToOutputValue(Long longValue, D outputDefinition,
			Protector protector) throws ExpressionEvaluationException, SchemaException {
		if (longValue == null) {
			return null;
		}
		QName outputType = outputDefinition.getTypeName();
		if (outputType.equals(DOMUtil.XSD_INT)) {
			return longValue.intValue();
		} else if (outputType.equals(DOMUtil.XSD_LONG)) {
			return longValue;
		} else {
			return convertToOutputValue(longValue.toString(), outputDefinition, protector);
		}
	}

	public static <D extends ItemDefinition> Object convertToOutputValue(String stringValue,
			D outputDefinition, Protector protector) throws ExpressionEvaluationException, SchemaException {
		if (stringValue == null) {
			return null;
		}
		QName outputType = outputDefinition.getTypeName();
		if (outputType.equals(DOMUtil.XSD_STRING)) {
			return stringValue;
		} else if (outputType.equals(ProtectedStringType.COMPLEX_TYPE)) {
			try {
				return protector.encryptString(stringValue);
			} catch (EncryptionException e) {
				throw new ExpressionEvaluationException("Crypto error: " + e.getMessage(), e);
			}
		} else if (XmlTypeConverter.canConvert(outputType)) {
			Class<?> outputJavaType = XsdTypeMapper.toJavaType(outputType);
			try {
				return XmlTypeConverter.toJavaValue(stringValue, outputJavaType, true);
			} catch (NumberFormatException e) {
				throw new SchemaException("Cannot convert string '" + stringValue + "' to data type "
						+ outputType + ": invalid number format", e);
			} catch (IllegalArgumentException e) {
				throw new SchemaException("Cannot convert string '" + stringValue + "' to data type "
						+ outputType + ": " + e.getMessage(), e);
			}
		} else {
			throw new IllegalArgumentException(
					"Expression cannot generate values for properties of type " + outputType);
		}
	}

	public static <T> boolean isEmpty(T val) {
		if (val == null) {
			return true;
		}
		if (val instanceof String && ((String)val).isEmpty()) {
			return true;
		}
		if (val instanceof PolyString && ((PolyString)val).isEmpty()) {
			return true;
		}
		return false;
	}

	public static <T, V extends PrismValue> V convertToPrismValue(T value, ItemDefinition definition, String contextDescription, PrismContext prismContext) throws ExpressionEvaluationException {
		if (definition instanceof PrismReferenceDefinition) {
			return (V) ((ObjectReferenceType) value).asReferenceValue();
		} else if (definition instanceof PrismContainerDefinition) {
			try {
				prismContext.adopt((Containerable) value);
				((Containerable) value).asPrismContainerValue().applyDefinition(definition);
			} catch (SchemaException e) {
				throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
			}
			return (V) ((Containerable) value).asPrismContainerValue();
		} else {
			return (V) new PrismPropertyValue<>(value);
		}
	}
	
	public static Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> createCondition(ExpressionType conditionExpressionType, ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		PrismPropertyDefinition<Boolean> conditionOutputDef = new PrismPropertyDefinitionImpl<Boolean>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, expressionFactory.getPrismContext());
		return expressionFactory.makeExpression(conditionExpressionType, conditionOutputDef, shortDesc, task, result);	
	}

	public static Function<Object, Object> createRefConvertor(QName defaultType) {
		return (o) -> {
			if (o == null || o instanceof ObjectReferenceType) {
				return o;
			} else if (o instanceof PrismReferenceValue) {
				ObjectReferenceType rv = new ObjectReferenceType();
				rv.setupReferenceValue((PrismReferenceValue) o);
				return rv;
			} else if (o instanceof String) {
				return new ObjectReferenceType().oid((String) o).type(defaultType);
			} else {
				//throw new IllegalArgumentException("The value couldn't be converted to an object reference: " + o);
				return o;		// let someone else complain at this
			}
		};
	}
}
