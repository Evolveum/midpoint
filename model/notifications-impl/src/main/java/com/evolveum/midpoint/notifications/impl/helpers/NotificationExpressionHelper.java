/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.repo.common.expression.*;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.evolveum.midpoint.schema.config.ExpressionConfigItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.NotificationFunctions;
import com.evolveum.midpoint.notifications.impl.events.BaseEventImpl;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class NotificationExpressionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationExpressionHelper.class);

    @Autowired private NotificationFunctions notificationFunctions;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private TextFormatter textFormatter;
    @Autowired private SystemObjectCache systemObjectCache;

    // shortDesc = what is to be evaluated e.g. "event filter expression"
    boolean evaluateBooleanExpressionChecked(
            @NotNull ExpressionConfigItem expressionCI,
            VariablesMap variablesMap,
            @SuppressWarnings("SameParameterValue") String shortDesc,
            EventProcessingContext<?> ctx,
            OperationResult result) {

        try {
            return evaluateBooleanExpression(expressionCI, variablesMap, shortDesc, ctx, result);
        } catch (CommonException e) {
            throw handleCommonException(shortDesc, result, e);
        }
    }

    // FIXME we should throw correct exception class
    private static RuntimeException handleCommonException(String shortDesc, OperationResult result, CommonException e) {
        String msg = "Couldn't evaluate " + shortDesc + ": " + e.getMessage();
        result.recordException(msg, e);
        throw new SystemException(msg, e);
    }

    private boolean evaluateBooleanExpression(
            @NotNull ExpressionConfigItem expressionCI,
            VariablesMap variablesMap, String shortDesc,
            EventProcessingContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        var task = ctx.task();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef =
                prismContext.definitionFactory().newPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN);
        Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                expressionFactory.makeExpression(
                        expressionCI,
                        resultDef, ctx.defaultExpressionProfile(),
                        shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.isEmpty()) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException(
                    "Filter expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    public List<String> evaluateExpressionChecked(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap,
            String shortDesc,
            EventProcessingContext<?> ctx, OperationResult result) {

        try {
            return evaluateExpression(expressionBean, origin, variablesMap, shortDesc, ctx, result);
        } catch (CommonException e) {
            throw handleCommonException(shortDesc, result, e);
        }
    }

    private List<String> evaluateExpression(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap, String shortDesc, EventProcessingContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef =
                prismContext.definitionFactory().newPropertyDefinition(resultName, DOMUtil.XSD_STRING, 0, -1);

        var task = ctx.task();
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(
                        ExpressionConfigItem.of(expressionBean, origin),
                        resultDef, ctx.defaultExpressionProfile(),
                        shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);

        List<String> retval = new ArrayList<>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }

    public List<RecipientExpressionResultType> evaluateRecipientExpressionChecked(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap, String shortDesc,
            EventProcessingContext<?> ctx, OperationResult result) {
        try {
            return evaluateRecipientExpression(expressionBean, origin, variablesMap, shortDesc, ctx, result);
        } catch (CommonException e) {
            throw handleCommonException(shortDesc, result, e);
        }
    }

    private List<RecipientExpressionResultType> evaluateRecipientExpression(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap, String shortDesc, EventProcessingContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        PrismPropertyDefinition<RecipientExpressionResultType> resultDef =
                prismContext.definitionFactory().newPropertyDefinition(
                        new QName(SchemaConstants.NS_C, "result"),
                        RecipientExpressionResultType.COMPLEX_TYPE,
                        0, -1);

        var task = ctx.task();
        Expression<PrismPropertyValue<RecipientExpressionResultType>, PrismPropertyDefinition<RecipientExpressionResultType>> expression =
                expressionFactory.makeExpression(
                        ExpressionConfigItem.of(expressionBean, origin),
                        resultDef,
                        ctx.defaultExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        context.setExpressionFactory(expressionFactory);
        context.setAdditionalConvertor(this::recipientConverter);
        PrismValueDeltaSetTriple<PrismPropertyValue<RecipientExpressionResultType>> exprResult =
                ExpressionUtil.evaluateExpressionInContext(expression, context, task, result);

        List<RecipientExpressionResultType> retval = new ArrayList<>();
        for (PrismPropertyValue<RecipientExpressionResultType> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }


    private Object recipientConverter(Object resultValue) {
        if (resultValue == null) {
            return null;
        }

        RecipientExpressionResultType result = new RecipientExpressionResultType();
        if (resultValue instanceof PrismObject) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.asReferenceValue().setObject((PrismObject<?>) resultValue); // it better be focus
            result.setRecipientRef(ref);
        } else if (resultValue instanceof FocusType) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.asReferenceValue().setOriginObject((FocusType) resultValue);
            result.setRecipientRef(ref);
        } else if (resultValue instanceof String) {
            // TODO OID check, if it's OID, just change it to ref
            result.setAddress((String) resultValue);
        } else {
            return resultValue; // we don't know what to do with it, let it fail with original value
        }

        return result;
    }

    public List<NotificationMessageAttachmentType> evaluateAttachmentExpressionChecked(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap,
            String shortDesc,
            EventProcessingContext<?> ctx, OperationResult result) {

        try {
            return evaluateNotificationMessageAttachmentTypeExpression(
                    expressionBean, origin, variablesMap, shortDesc, ctx, result);
        } catch (CommonException e) {
            throw handleCommonException(shortDesc, result, e);
        }
    }

    private List<NotificationMessageAttachmentType> evaluateNotificationMessageAttachmentTypeExpression(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap, String shortDesc, EventProcessingContext<?> ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        var task = ctx.task();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        ComplexTypeDefinition ctd = prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(NotificationMessageAttachmentType.COMPLEX_TYPE);

        PrismContainerDefinition<NotificationMessageAttachmentType> resultDef =
                prismContext.definitionFactory().newContainerDefinition(resultName, ctd);
        Expression<PrismContainerValue<NotificationMessageAttachmentType>, PrismContainerDefinition<NotificationMessageAttachmentType>> expression =
                expressionFactory.makeExpression(
                        ExpressionConfigItem.of(expressionBean, origin),
                        resultDef, ctx.defaultExpressionProfile(),
                        shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);

        PrismValueDeltaSetTriple<PrismContainerValue<NotificationMessageAttachmentType>> exprResultTriple =
                (PrismValueDeltaSetTriple) ExpressionUtil.evaluateAnyExpressionInContext(expression, eeContext, task, result);

        Collection<PrismContainerValue<NotificationMessageAttachmentType>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.isEmpty()) {
            return null;
        }

        List<NotificationMessageAttachmentType> retval = new ArrayList<>();
        for (PrismContainerValue<NotificationMessageAttachmentType> item : exprResult) {
            retval.add(item.getValue());
        }
        return retval;
    }

    public VariablesMap getDefaultVariables(Event event, OperationResult result) {
        VariablesMap variables = new VariablesMap();
        ((BaseEventImpl) event).createVariablesMap(variables, result);
        variables.put(ExpressionConstants.VAR_TEXT_FORMATTER, textFormatter, TextFormatter.class);
        variables.put(ExpressionConstants.VAR_NOTIFICATION_FUNCTIONS, notificationFunctions, NotificationFunctions.class);
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
        if (systemConfiguration != null) {
            variables.put(ExpressionConstants.VAR_CONFIGURATION, systemConfiguration, systemConfiguration.getDefinition());
        } else {
            variables.put(ExpressionConstants.VAR_CONFIGURATION, null, SystemConfigurationType.class);
        }
        return variables;
    }

    @Nullable
    private PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) {
        try {
            return systemObjectCache.getSystemConfiguration(result);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get system configuration", e);
            return null;
        }
    }
}
