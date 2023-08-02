/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface AutocompleteConfigurationMixin {

    Trace LOGGER = TraceManager.getTrace(AutocompleteConfigurationMixin.class);

    default <O extends ObjectType> String getDisplayNameFromExpression(
            String contextDesc, @Nullable ExpressionType expressionType,
            @NotNull Function<PrismObject<O>, String> defaultDisplayNameFunction,
            @NotNull PrismObject<O> object, @NotNull BasePanel<?> panel) {

        if (expressionType == null) {
            return defaultDisplayNameFunction.apply(object);
        }

        ModelServiceLocator locator = panel.getPageBase();

        Task task = panel.getPageBase().getPageTask();
        OperationResult result = task.getResult();

        try {
            ExpressionFactory factory = locator.getExpressionFactory();
            PrismContext ctx = object.getPrismContext();
            PrismPropertyDefinition<String> outputDefinition = ctx.definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);

            Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                    factory.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, result);

            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());

            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
            context.setExpressionFactory(factory);
            PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, result);
            if (outputTriple == null) {
                return null;
            }
            Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
            if (outputValues.isEmpty()) {
                return null;
            }
            if (outputValues.size() > 1) {
                return null;
            }
            return outputValues.iterator().next().getRealValue();
        } catch (Exception ex) {
            result.recordFatalError(ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate expression for group selection and user display name", ex);
        }

        return null;
    }
}
