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

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingVariablesDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
public class VariablesUtil {

	private static final Trace LOGGER = TraceManager.getTrace(VariablesUtil.class);

	static class VariableResolutionContext {
		@NotNull final ExpressionFactory expressionFactory;
		@NotNull final ObjectResolver objectResolver;
		@NotNull final PrismContext prismContext;
		@NotNull final Task task;
		VariableResolutionContext(@NotNull ExpressionFactory expressionFactory,
				@NotNull ObjectResolver objectResolver, @NotNull PrismContext prismContext, @NotNull Task task) {
			this.expressionFactory = expressionFactory;
			this.objectResolver = objectResolver;
			this.prismContext = prismContext;
			this.task = task;
		}
	}

	// We create immutable versions of prism variables to avoid unnecessary downstream cloning
	@NotNull
	static Map<String, Object> initialPreparation(Map<String, Object> initialVariables,
			ScriptingVariablesDefinitionType derivedVariables, ExpressionFactory expressionFactory, ObjectResolver objectResolver,
			PrismContext prismContext, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		HashMap<String, Object> rv = new HashMap<>();
		addProvidedVariables(rv, initialVariables, task);
		addDerivedVariables(rv, derivedVariables,
				new VariableResolutionContext(expressionFactory, objectResolver, prismContext, task), result);
		return rv;
	}

	private static void addProvidedVariables(HashMap<String, Object> resultingVariables, Map<String, Object> initialVariables, Task task) {
		putImmutableValue(resultingVariables, ExpressionConstants.VAR_TASK.getLocalPart(), task.getTaskPrismObject().asObjectable());
		if (initialVariables != null) {
			initialVariables.forEach((key, value) -> putImmutableValue(resultingVariables, key, value));
		}
	}

	private static void addDerivedVariables(HashMap<String, Object> resultingVariables,
			ScriptingVariablesDefinitionType definitions, VariableResolutionContext ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (definitions == null) {
			return;
		}
		for (ScriptingVariableDefinitionType definition : definitions.getVariable()) {
			if (definition.getExpression() == null) {
				continue;       // todo or throw an exception?
			}
			String shortDesc = "scripting variable " + definition.getName();
			Object value;
			if (definition.getExpression().getExpressionEvaluator().size() == 1 &&
					QNameUtil.match(SchemaConstantsGenerated.C_PATH, definition.getExpression().getExpressionEvaluator().get(0).getName())) {
				value = variableFromPathExpression(resultingVariables, definition.getExpression().getExpressionEvaluator().get(0), ctx, shortDesc, result);
			} else {
				value = variableFromOtherExpression(resultingVariables, definition, ctx, shortDesc, result);
			}
			putImmutableValue(resultingVariables, definition.getName(), value);
		}
	}

	private static Object variableFromPathExpression(HashMap<String, Object> resultingVariables,
			JAXBElement<?> expressionEvaluator, VariableResolutionContext ctx, String shortDesc, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		if (!(expressionEvaluator.getValue() instanceof ItemPathType)) {
			throw new IllegalArgumentException("Path expression: expected ItemPathType but got " + expressionEvaluator.getValue());
		}
		ItemPath itemPath = ((ItemPathType) expressionEvaluator.getValue()).getItemPath();
		return ExpressionUtil.resolvePath(itemPath, createVariables(resultingVariables), null, ctx.objectResolver, shortDesc, ctx.task, result);
	}

	private static ExpressionVariables createVariables(HashMap<String, Object> variableMap) {
		ExpressionVariables rv = new ExpressionVariables();
		Map<String, Object> clonedVariableMap = cloneIfNecessary(variableMap);
		clonedVariableMap.forEach((name, value) -> rv.addVariableDefinition(new QName(SchemaConstants.NS_C, name), value));
		return rv;
	}

	private static Object variableFromOtherExpression(HashMap<String, Object> resultingVariables,
			ScriptingVariableDefinitionType definition, VariableResolutionContext ctx, String shortDesc,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		ItemDefinition<?> outputDefinition = determineOutputDefinition(definition, ctx, shortDesc);
		Expression<PrismValue, ItemDefinition<?>> expression = ctx.expressionFactory
				.makeExpression(definition.getExpression(), outputDefinition, shortDesc, ctx.task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, createVariables(resultingVariables), shortDesc, ctx.task, result);
		PrismValueDeltaSetTriple<?> triple = ModelExpressionThreadLocalHolder
				.evaluateAnyExpressionInContext(expression, context, ctx.task, result);
		Collection<?> resultingValues = triple.getNonNegativeValues();
		if (definition.getMaxOccurs() != null && outputDefinition.isSingleValue()           // cardinality of outputDefinition is derived solely from definition.maxOccurs (if specified)
				|| definition.getMaxOccurs() == null || resultingValues.size() <= 1) {
			return MiscUtil.getSingleValue(resultingValues, null, shortDesc);       // unwrapping will occur when the value is used
		} else {
			return unwrapPrismValues(resultingValues);
		}
	}

	// TODO shouldn't we unwrap collections of prism values in the same way as in ExpressionUtil.convertVariableValue ?
	private static Collection<?> unwrapPrismValues(Collection<?> prismValues) {
		Collection<Object> rv = new ArrayList<>(prismValues.size());
		for (Object value : prismValues) {
			if (value instanceof PrismValue) {
				rv.add(((PrismValue) value).getRealValue());
			} else {
				rv.add(value);
			}
		}
		return rv;
	}

	private static ItemDefinition<?> determineOutputDefinition(ScriptingVariableDefinitionType variableDefinition,
			VariableResolutionContext ctx, String shortDesc) throws SchemaException {
		List<JAXBElement<?>> evaluators = variableDefinition.getExpression().getExpressionEvaluator();
		boolean isValue = !evaluators.isEmpty() && QNameUtil.match(evaluators.get(0).getName(), SchemaConstants.C_VALUE);
		QName elementName = new QName(variableDefinition.getName());
		if (variableDefinition.getType() != null) {
			Integer maxOccurs;
			if (variableDefinition.getMaxOccurs() != null) {
				maxOccurs = XsdTypeMapper.multiplicityToInteger(variableDefinition.getMaxOccurs());
			} else if (isValue) {       // if we have constant values we can try to guess
				maxOccurs = evaluators.size() > 1 ? -1 : 1;
			} else {
				maxOccurs = null;           // no idea
			}
			if (maxOccurs == null) {
				maxOccurs = -1;             // to be safe
			}
			return ctx.prismContext.getSchemaRegistry().createAdHocDefinition(elementName, variableDefinition.getType(), 0, maxOccurs);
		}
		if (isValue) {
			return StaticExpressionUtil.deriveOutputDefinitionFromValueElements(elementName, evaluators, shortDesc, ctx.prismContext);
		} else {
			throw new SchemaException("The type of scripting variable " + variableDefinition.getName() + " is not defined");
		}
	}

	private static void putImmutableValue(HashMap<String, Object> map, String name, Object value) {
		map.put(name, makeImmutable(value));
	}

	@NotNull
	public static Map<String, Object> cloneIfNecessary(@NotNull Map<String, Object> variables) {
		HashMap<String, Object> rv = new HashMap<>();
		variables.forEach((key, value) -> rv.put(key, cloneIfNecessary(key, value)));
		return rv;
	}

	@Nullable
	public static Object cloneIfNecessary(String name, Object value) {
		if (value == null) {
			return null;
		}
		Object immutableOrNull = tryMakingImmutable(value);
		if (immutableOrNull != null) {
			return immutableOrNull;
		} else {
			try {
				return CloneUtil.clone(value);
			} catch (Throwable t) {
				LOGGER.warn("Scripting variable value {} of type {} couldn't be cloned. Using original.", name, value.getClass());
				return value;
			}
		}
	}

	public static <T> T makeImmutable(T value) {
		T rv = tryMakingImmutable(value);
		return rv != null ? rv : value;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T tryMakingImmutable(T value) {
		if (value instanceof Containerable) {
			PrismContainerValue<?> pcv = ((Containerable) value).asPrismContainerValue();
			if (!pcv.isImmutable()) {
				PrismContainerValue clone = pcv.clone();
				Containerable containerableClone = clone.asContainerable();
				clone.setImmutable(true);       // must be called after first asContainerable on clone
				return (T) containerableClone;
			} else {
				return value;
			}
		} else if (value instanceof Referencable) {
			PrismReferenceValue prv = ((Referencable) value).asReferenceValue();
			if (!prv.isImmutable()) {
				PrismReferenceValue clone = prv.clone();
				Referencable referencableClone = clone.asReferencable();
				clone.setImmutable(true);       // must be called after first asReferencable on clone
				return (T) referencableClone;
			} else {
				return value;
			}
		} else if (value instanceof PrismValue) {
			PrismValue pval = (PrismValue) value;
			if (!pval.isImmutable()) {
				PrismValue clone = pval.clone();
				clone.setImmutable(true);
				return (T) clone;
			} else {
				return (T) pval;
			}
		} else if (value instanceof Item) {
			Item item = (Item) value;
			if (!item.isImmutable()) {
				Item clone = item.clone();
				clone.setImmutable(true);
				return (T) clone;
			} else {
				return (T) item;
			}
		} else {
			return null;
		}
	}

}
