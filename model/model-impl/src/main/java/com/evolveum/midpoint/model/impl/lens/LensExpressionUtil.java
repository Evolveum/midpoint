/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.getSingleValue;

/**
 * Helps with evaluating expressions during the "lens" execution.
 */
public class LensExpressionUtil {

    public static boolean evaluateBoolean(
            ExpressionType expressionBean,
            VariablesMap variablesMap,
            @Nullable LensContext<?> lensContext,
            String contextDesc,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return evaluateExpressionSingle(
                expressionBean, variablesMap, lensContext, contextDesc, task, result,
                DOMUtil.XSD_BOOLEAN, false, null);
    }

    private static String evaluateString(
            ExpressionType expressionBean,
            VariablesMap variablesMap,
            @Nullable LensContext<?> lensContext,
            String contextDesc,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return evaluateExpressionSingle(
                expressionBean, variablesMap, lensContext, contextDesc, task, result,
                DOMUtil.XSD_STRING, null, null);
    }

    public static LocalizableMessageType evaluateLocalizableMessageType(
            ExpressionType expressionBean,
            VariablesMap variablesMap,
            @Nullable LensContext<?> lensContext,
            String contextDesc,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Function<Object, Object> additionalConvertor = (o) -> {
            if (o == null || o instanceof LocalizableMessageType) {
                return o;
            } else if (o instanceof LocalizableMessage) {
                return LocalizationUtil.createLocalizableMessageType((LocalizableMessage) o);
            } else {
                return new SingleLocalizableMessageType().fallbackMessage(String.valueOf(o));
            }
        };
        return evaluateExpressionSingle(
                expressionBean, variablesMap, lensContext, contextDesc, task, result,
                LocalizableMessageType.COMPLEX_TYPE, null, additionalConvertor);
    }

    private static <T> T evaluateExpressionSingle(
            ExpressionType expressionBean,
            VariablesMap variablesMap,
            @Nullable LensContext<?> lensContext,
            String contextDesc,
            Task task,
            OperationResult result,
            QName typeName,
            T defaultValue,
            Function<Object, Object> additionalConvertor)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PrismPropertyDefinition<T> resultDef =
                PrismContext.get().definitionFactory().createPropertyDefinition(
                        new QName(SchemaConstants.NS_C, "result"), typeName);
        ExpressionFactory expressionFactory = ModelCommonBeans.get().expressionFactory;
        Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression =
                expressionFactory.makeExpression(
                        expressionBean, resultDef, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, contextDesc, task);
        ModelExpressionEnvironment<?,?,?> env = new ModelExpressionEnvironment<>(lensContext, null, task, result);
        eeContext.setExpressionFactory(expressionFactory);
        eeContext.setAdditionalConvertor(additionalConvertor);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, env, result);
        List<T> results = exprResultTriple.getZeroSet().stream()
                .map(ppv -> ppv.getRealValue())
                .collect(Collectors.toList());
        return getSingleValue(results, defaultValue, contextDesc);
    }

    public static @NotNull SingleLocalizableMessageType interpretLocalizableMessageTemplate(
            LocalizableMessageTemplateType template,
            VariablesMap variablesMap,
            @Nullable LensContext<?> lensContext,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        SingleLocalizableMessageType rv = new SingleLocalizableMessageType();
        if (template.getKey() != null) {
            rv.setKey(template.getKey());
        } else if (template.getKeyExpression() != null) {
            rv.setKey(
                    evaluateString(
                            template.getKeyExpression(),
                            variablesMap,
                            lensContext,
                            "localizable message key expression",
                            task, result));
        }
        if (!template.getArgument().isEmpty() && !template.getArgumentExpression().isEmpty()) {
            throw new IllegalArgumentException("Both argument and argumentExpression items are non empty");
        } else if (!template.getArgumentExpression().isEmpty()) {
            for (ExpressionType argumentExpression : template.getArgumentExpression()) {
                rv.getArgument().add(
                        new LocalizableMessageArgumentType().localizable(
                                evaluateLocalizableMessageType(
                                        argumentExpression,
                                        variablesMap,
                                        lensContext,
                                        "localizable message argument expression",
                                        task, result)));
            }
        } else {
            // TODO allow localizable messages templates here
            rv.getArgument().addAll(template.getArgument());
        }
        if (template.getFallbackMessage() != null) {
            rv.setFallbackMessage(template.getFallbackMessage());
        } else if (template.getFallbackMessageExpression() != null) {
            rv.setFallbackMessage(
                    evaluateString(
                            template.getFallbackMessageExpression(),
                            variablesMap,
                            lensContext,
                            "localizable message fallback expression",
                            task, result));
        }
        return rv;
    }
}
