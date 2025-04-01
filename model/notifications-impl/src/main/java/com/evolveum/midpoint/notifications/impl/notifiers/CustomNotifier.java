/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ExpressionConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class CustomNotifier extends BaseHandler<Event, CustomNotifierType> {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(CustomNotifier.class);

    @Autowired protected NotificationManager notificationManager;
    @Autowired protected AggregatedEventHandler aggregatedEventHandler;
    @Autowired private TransportService transportService;

    @Override
    public @NotNull Class<Event> getEventType() {
        return Event.class;
    }

    @Override
    public @NotNull Class<CustomNotifierType> getEventHandlerConfigurationType() {
        return CustomNotifierType.class;
    }

    @Override
    public boolean processEvent(
            @NotNull ConfigurationItem<? extends CustomNotifierType> handlerConfig,
            @NotNull EventProcessingContext<?> ctx,
            @NotNull OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(CustomNotifier.class.getName() + ".processEvent");

        Event event = ctx.event();

        logStart(getLogger(), handlerConfig, ctx);

        boolean applies = aggregatedEventHandler.processEvent(handlerConfig, ctx, result);

        if (applies) {
            VariablesMap variables = getDefaultVariables(event, result);

            reportNotificationStart(event);
            try {
                for (String transportName : handlerConfig.value().getTransport()) {
                    variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
                    Transport<?> transport = transportService.getTransport(transportName);

                    Message message = getMessageFromExpression(handlerConfig, variables, ctx, result);
                    if (message != null) {
                        getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
                        transport.send(message, transportName, ctx.sendingContext(), result);
                    } else {
                        getLogger().debug("No message for transport {}, won't send anything", transportName);
                    }
                }
            } finally {
                reportNotificationEnd(event, result);
            }
        }
        logEnd(getLogger(), event, applies);
        result.computeStatusIfUnknown();
        return true; // not-applicable notifiers do not stop processing of other notifiers
    }

    protected Trace getLogger() {
        return DEFAULT_LOGGER; // in case a subclass does not provide its own logger
    }

    private Message getMessageFromExpression(
            ConfigurationItem<? extends CustomNotifierType> config,
            VariablesMap variables,
            EventProcessingContext<?> ctx,
            OperationResult result) {
        var expressionBean = config.value().getExpression();
        if (expressionBean == null) {
            return null;
        }
        List<NotificationMessageType> messages;
        try {
            messages = evaluateExpression(
                    ExpressionConfigItem.of(
                            expressionBean,
                            config.origin().toApproximate()),
                    variables,
                    "message expression",
                    ctx, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            throw new SystemException("Couldn't evaluate custom notifier expression: " + e.getMessage(), e);
        }
        if (messages == null || messages.isEmpty()) {
            return null;
        } else if (messages.size() > 1) {
            getLogger().warn("Custom notifier returned more than one message: {}", messages);
        }
        return messages.get(0) != null ? new Message(messages.get(0)) : null;
    }

    @SuppressWarnings("SameParameterValue")
    private List<NotificationMessageType> evaluateExpression(
            ExpressionConfigItem expressionCI,
            VariablesMap VariablesMap,
            String shortDesc,
            EventProcessingContext<?> ctx,
            OperationResult result) throws ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        var task = ctx.task();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<NotificationMessageType> resultDef =
                prismContext.definitionFactory().newPropertyDefinition(resultName, NotificationMessageType.COMPLEX_TYPE);

        Expression<PrismPropertyValue<NotificationMessageType>, PrismPropertyDefinition<NotificationMessageType>> expression =
                expressionFactory.makeExpression(
                        expressionCI, resultDef, ctx.defaultExpressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, VariablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<NotificationMessageType>> exprResult =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
        return exprResult.getZeroSet().stream()
                .map(PrismPropertyValue::getValue)
                .collect(Collectors.toList());
    }
}
