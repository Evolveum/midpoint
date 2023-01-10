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

import com.evolveum.midpoint.repo.common.expression.*;

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
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
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
    public boolean evaluateBooleanExpressionChecked(ExpressionType expressionType, VariablesMap variablesMap,
            String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateBooleanExpression(expressionType, variablesMap, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    public boolean evaluateBooleanExpression(
            ExpressionType expressionType, VariablesMap variablesMap, String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN);
        Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Filter expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    public List<String> evaluateExpressionChecked(ExpressionType expressionType, VariablesMap variablesMap,
            String shortDesc, Task task, OperationResult result) {

        try {
            return evaluateExpression(expressionType, variablesMap, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    private List<String> evaluateExpression(ExpressionType expressionType,
            VariablesMap variablesMap, String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        MutablePrismPropertyDefinition<String> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_STRING);
        resultDef.setMaxOccurs(-1);

        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
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

    public List<RecipientExpressionResultType> evaluateRecipientExpressionChecked(ExpressionType expressionType,
            VariablesMap variablesMap, String shortDesc, Task task, OperationResult result) {
        try {
            return evaluateRecipientExpression(expressionType, variablesMap, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    private List<RecipientExpressionResultType> evaluateRecipientExpression(ExpressionType expressionType,
            VariablesMap variablesMap, String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        MutablePrismPropertyDefinition<RecipientExpressionResultType> resultDef =
                prismContext.definitionFactory().createPropertyDefinition(
                        new QName(SchemaConstants.NS_C, "result"),
                        RecipientExpressionResultType.COMPLEX_TYPE);
        resultDef.setMaxOccurs(-1);

        Expression<PrismPropertyValue<RecipientExpressionResultType>, PrismPropertyDefinition<RecipientExpressionResultType>> expression =
                expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
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
            ExpressionType expressionType, VariablesMap variablesMap,
            String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateNotificationMessageAttachmentTypeExpression(expressionType, variablesMap, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    public List<NotificationMessageAttachmentType> evaluateNotificationMessageAttachmentTypeExpression(
            ExpressionType expressionType, VariablesMap variablesMap, String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<NotificationMessageAttachmentType> resultDef =
                prismContext.definitionFactory().createPropertyDefinition(resultName, NotificationMessageAttachmentType.COMPLEX_TYPE);
        Expression<PrismPropertyValue<NotificationMessageAttachmentType>, PrismPropertyDefinition<NotificationMessageAttachmentType>> expression =
                expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);

        PrismValueDeltaSetTriple<PrismPropertyValue<NotificationMessageAttachmentType>> exprResultTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);

        Collection<PrismPropertyValue<NotificationMessageAttachmentType>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return null;
        }

        List<NotificationMessageAttachmentType> retval = new ArrayList<>();
        for (PrismPropertyValue<NotificationMessageAttachmentType> item : exprResult) {
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
