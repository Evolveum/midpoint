/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.config.FunctionConfigItem;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Methods _internal_ to the processes of expression evaluation.
 * Should not be called from outside.
 *
 * Some of these methods are currently used only at a single place. However, this is just an intermediary state,
 * in order to consolidate the respective algorithms.
 */
public class ExpressionEvaluationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionEvaluationUtil.class);

    // waiting to be reused
    public static TypedValue<?> convertInput(
            @NotNull String argName, @Nullable Object argValue, @NotNull FunctionConfigItem function)
            throws ConfigurationException {

        LOGGER.trace("Converting argument: {} = {}", argName, argValue);

        var matchingParameter = function.getParameter(argName);

        QName paramType = matchingParameter.getType();
        Class<?> expressionParameterClass = PrismContext.get().getSchemaRegistry().determineClassForType(paramType);

        Object value;
        if (expressionParameterClass != null
                && !DOMUtil.XSD_ANYTYPE.equals(paramType)
                && XmlTypeConverter.canConvert(expressionParameterClass)) {
            value = ExpressionUtil.convertValue(
                    expressionParameterClass, null, argValue, PrismContext.get().getDefaultProtector());
        } else {
            value = argValue;
        }

        Class<?> valueClass;
        if (value == null) {
            valueClass = Objects.requireNonNullElse(expressionParameterClass, Object.class);
        } else {
            valueClass = value.getClass();
        }

        //ItemDefinition def = ExpressionUtil.determineDefinitionFromValueClass(prismContext, entry.getKey(), valueClass, paramType);

        // It is sometimes not possible to derive an item definition from the value class alone: more items can share the same
        // class (e.g. both objectStatePolicyConstraintType and assignmentStatePolicyConstraintType are of
        // StatePolicyConstraintType class). So let's provide valueClass here only.
        return new TypedValue<>(value, valueClass);
    }

    // TODO extract common parts
    public static @NotNull <D extends ItemDefinition<?>> D prepareFunctionOutputDefinition(@NotNull FunctionConfigItem function) {
        QName returnTypeName = function.getReturnTypeName();
        D outputDefinition;
        ItemDefinition<?> existingDefinition = PrismContext.get().getSchemaRegistry().findItemDefinitionByType(returnTypeName);
        if (existingDefinition != null) {
            //noinspection unchecked
            outputDefinition = (D) existingDefinition.clone();
        } else {
            //noinspection unchecked
            outputDefinition = (D) PrismContext.get().definitionFactory().createPropertyDefinition(
                    SchemaConstantsGenerated.C_VALUE, returnTypeName);
        }
        outputDefinition.toMutable().setMaxOccurs(function.isReturnMultiValue() ? -1 : 1);
        return outputDefinition;
    }

    // TODO rename & extract common parts
    // FIXME why the multiplicity is taken from functionToExecute?!
    public static <D extends ItemDefinition<?>> D determineVariableOutputDefinition(
            @NotNull FunctionConfigItem functionToExecute, @NotNull String paramName)
            throws SchemaException, ConfigurationException {
        var functionParameter = functionToExecute.getParameter(paramName);

        QName returnType = functionParameter.getType();

        ItemDefinition<?> returnTypeDef = PrismContext.get().getSchemaRegistry().findItemDefinitionByType(returnType);
        if (returnTypeDef != null) {
            //noinspection unchecked
            return (D) returnTypeDef;
        } else {
            MutablePrismPropertyDefinition<?> dynamicReturnTypeDef =
                    PrismContext.get().definitionFactory().createPropertyDefinition(SchemaConstantsGenerated.C_VALUE, returnType);
            dynamicReturnTypeDef.setMaxOccurs(functionToExecute.isReturnSingleValue() ? 1 : -1);
            //noinspection unchecked
            return (D) dynamicReturnTypeDef;
        }
    }

    @Nullable
    public static <V extends PrismValue, D extends ItemDefinition<?>> Object getSingleRealValue(
            PrismValueDeltaSetTriple<V> outputTriple, D outputDefinition, String contextDesc)
            throws ExpressionEvaluationException {
        if (outputTriple == null) {
            return null;
        }

        Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();

        if (nonNegativeValues.isEmpty()) {
            return null;
        }

        if (outputDefinition.isMultiValue()) {
            // This is a problem if the triple contains dynamically-typed PCVs. See MID-6775.
            return PrismValueCollectionsUtil.getRealValuesOfCollection(nonNegativeValues);
        }

        if (nonNegativeValues.size() > 1) {
            throw new ExpressionEvaluationException(
                    "Expression returned more than one value (%d) in %s".formatted(nonNegativeValues.size(), contextDesc));
        }

        // This is a problem if the triple contains dynamically-typed PCVs. See MID-6775.
        // Currently, it is hacked by wrapping these values to a PPV.
        return nonNegativeValues.iterator().next().getRealValue();
    }
}
