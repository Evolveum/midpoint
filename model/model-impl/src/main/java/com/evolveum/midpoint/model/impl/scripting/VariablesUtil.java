/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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
        final ExpressionProfile expressionProfile;
        @NotNull final Task task;
        VariableResolutionContext(@NotNull ExpressionFactory expressionFactory,
                @NotNull ObjectResolver objectResolver, @NotNull PrismContext prismContext, ExpressionProfile expressionProfile, @NotNull Task task) {
            this.expressionFactory = expressionFactory;
            this.objectResolver = objectResolver;
            this.prismContext = prismContext;
            this.expressionProfile = expressionProfile;
            this.task = task;
        }
    }

    // We create immutable versions of prism variables to avoid unnecessary downstream cloning
    @NotNull
    static VariablesMap initialPreparation(VariablesMap initialVariables,
            ScriptingVariablesDefinitionType derivedVariables, ExpressionFactory expressionFactory, ObjectResolver objectResolver,
            PrismContext prismContext, ExpressionProfile expressionProfile, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap rv = new VariablesMap();
        addProvidedVariables(rv, initialVariables, task);
        addDerivedVariables(rv, derivedVariables,
                new VariableResolutionContext(expressionFactory, objectResolver, prismContext, expressionProfile, task), result);
        return rv;
    }

    private static void addProvidedVariables(VariablesMap resultingVariables, VariablesMap initialVariables, Task task) {
        TypedValue<TaskType> taskValAndDef = new TypedValue<>(task.getUpdatedOrClonedTaskObject().asObjectable(), task.getUpdatedOrClonedTaskObject().getDefinition());
        putImmutableValue(resultingVariables, ExpressionConstants.VAR_TASK, taskValAndDef);
        if (initialVariables != null) {
            initialVariables.forEach((key, value) -> putImmutableValue(resultingVariables, key, value));
        }
    }

    private static void addDerivedVariables(VariablesMap resultingVariables,
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
            TypedValue valueAndDef;
            if (definition.getExpression().getExpressionEvaluator().size() == 1 &&
                    QNameUtil.match(SchemaConstantsGenerated.C_PATH, definition.getExpression().getExpressionEvaluator().get(0).getName())) {
                valueAndDef = variableFromPathExpression(resultingVariables, definition.getExpression().getExpressionEvaluator().get(0), ctx, shortDesc, result);
            } else {
                valueAndDef = variableFromOtherExpression(resultingVariables, definition, ctx, shortDesc, result);
            }
            putImmutableValue(resultingVariables, definition.getName(), valueAndDef);
        }
    }

    private static TypedValue variableFromPathExpression(VariablesMap resultingVariables,
            JAXBElement<?> expressionEvaluator, VariableResolutionContext ctx, String shortDesc, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (!(expressionEvaluator.getValue() instanceof ItemPathType)) {
            throw new IllegalArgumentException("Path expression: expected ItemPathType but got " + expressionEvaluator.getValue());
        }
        ItemPath itemPath = ctx.prismContext.toPath((ItemPathType) expressionEvaluator.getValue());
        return ExpressionUtil.resolvePathGetTypedValue(itemPath, createVariables(resultingVariables), false, null, ctx.objectResolver, ctx.prismContext, shortDesc, ctx.task, result);
    }

    private static ExpressionVariables createVariables(VariablesMap variableMap) {
        ExpressionVariables rv = new ExpressionVariables();
        VariablesMap clonedVariableMap = cloneIfNecessary(variableMap);
        clonedVariableMap.forEach((name, value) -> rv.put(name, value));
        return rv;
    }

    private static TypedValue variableFromOtherExpression(VariablesMap resultingVariables,
            ScriptingVariableDefinitionType definition, VariableResolutionContext ctx, String shortDesc,
            OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        ItemDefinition<?> outputDefinition = determineOutputDefinition(definition, ctx, shortDesc);
        Expression<PrismValue, ItemDefinition<?>> expression = ctx.expressionFactory
                .makeExpression(definition.getExpression(), outputDefinition, ctx.expressionProfile, shortDesc, ctx.task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, createVariables(resultingVariables), shortDesc, ctx.task);
        PrismValueDeltaSetTriple<?> triple = ModelExpressionThreadLocalHolder
                .evaluateAnyExpressionInContext(expression, context, ctx.task, result);
        Collection<?> resultingValues = triple.getNonNegativeValues();
        Object value;
        if (definition.getMaxOccurs() != null && outputDefinition.isSingleValue()           // cardinality of outputDefinition is derived solely from definition.maxOccurs (if specified)
                || definition.getMaxOccurs() == null || resultingValues.size() <= 1) {
            value = MiscUtil.getSingleValue(resultingValues, null, shortDesc);       // unwrapping will occur when the value is used
        } else {
            value = unwrapPrismValues(resultingValues);
        }
        return new TypedValue(value, outputDefinition);
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

    private static void putImmutableValue(VariablesMap map, String name, TypedValue valueAndDef) {
        map.put(name, makeImmutable(valueAndDef));
    }

    @NotNull
    public static VariablesMap cloneIfNecessary(@NotNull VariablesMap variables) {
        VariablesMap rv = new VariablesMap();
        variables.forEach((key, value) -> rv.put(key, cloneIfNecessary(key, value)));
        return rv;
    }

    @Nullable
    public static <T> TypedValue<T> cloneIfNecessary(String name, TypedValue<T> valueAndDef) {
        T valueClone = (T) cloneIfNecessary(name, valueAndDef.getValue());
        if (valueClone == valueAndDef.getValue()) {
            return valueAndDef;
        } else {
            return valueAndDef.createTransformed(valueClone);
        }
    }

    @Nullable
    public static <T> T cloneIfNecessary(String name, T value) {
        if (value == null) {
            return null;
        }
        T immutableOrNull = tryMakingImmutable(value);
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


    public static <T> TypedValue<T> makeImmutable(TypedValue<T> valueAndDef) {
        T immutableValue = (T) makeImmutableValue(valueAndDef.getValue());
        if (immutableValue == valueAndDef.getValue()) {
            return valueAndDef;
        } else {
            valueAndDef.setValue(immutableValue);
        }
        return valueAndDef;
    }

    public static <T> T makeImmutableValue(T value) {
        T rv = tryMakingImmutable(value);
        return rv != null ? rv : value;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T tryMakingImmutable(T value) {
        if (value instanceof Containerable) {
            PrismContainerValue<?> pcv = ((Containerable) value).asPrismContainerValue();
            if (!pcv.isImmutable()) {
                return (T) pcv.createImmutableClone().asContainerable();
            } else {
                return value;
            }
        } else if (value instanceof Referencable) {
            PrismReferenceValue prv = ((Referencable) value).asReferenceValue();
            if (!prv.isImmutable()) {
                return (T) prv.createImmutableClone().asReferencable();
            } else {
                return value;
            }
        } else if (value instanceof PrismValue) {
            PrismValue pval = (PrismValue) value;
            if (!pval.isImmutable()) {
                return (T) pval.createImmutableClone();
            } else {
                return (T) pval;
            }
        } else if (value instanceof Item) {
            Item item = (Item) value;
            if (!item.isImmutable()) {
                return (T) item.createImmutableClone();
            } else {
                return (T) item;
            }
        } else {
            return null;
        }
    }

}
