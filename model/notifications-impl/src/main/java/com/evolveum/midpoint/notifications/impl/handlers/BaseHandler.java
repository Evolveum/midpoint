/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.handlers;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.NOTIFICATIONS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

import java.util.List;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.EventHandlerRegistry;
import com.evolveum.midpoint.notifications.impl.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.impl.helpers.NotificationExpressionHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Common superclass for all event handlers. */
@Component
public abstract class BaseHandler<E extends Event, C extends BaseEventHandlerType> implements EventHandler<E, C> {

    private static final Trace LOGGER = TraceManager.getTrace(BaseHandler.class);

    @Autowired protected NotificationManagerImpl notificationManager;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected NotificationExpressionHelper expressionHelper;
    @Autowired protected EventHandlerRegistry eventHandlerRegistry;

    @PostConstruct
    protected void register() {
        eventHandlerRegistry.registerEventHandler(getEventHandlerConfigurationType(), this);
    }

    protected void logStart(
            Trace LOGGER, ConfigurationItem<? extends C> handlerConfig, EventProcessingContext<? extends E> ctx) {
        logStart(LOGGER, handlerConfig, ctx, null);
    }

    protected void logStart(
            Trace LOGGER,
            ConfigurationItem<? extends C> handlerConfiguration,
            EventProcessingContext<? extends E> ctx,
            Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event {} with handler {}\n{}",
                    ctx.event().shortDump(),
                    getHumanReadableHandlerDescription(handlerConfiguration.value()),
                    (additionalData != null ? ("\n  parameters: " + additionalData) :
                            ("\n  configuration: " + handlerConfiguration)));
        }
    }

    protected void logNotApplicable(E event, String reason) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "{} is not applicable for event {}, continuing in the handler chain; reason: {}",
                    this.getClass().getSimpleName(), event.shortDump(), reason);
        }
    }

    private String getHumanReadableHandlerDescription(C handlerConfiguration) {
        return Objects.requireNonNullElseGet(
                handlerConfiguration.getName(),
                () -> handlerConfiguration.getClass().getSimpleName());
    }

    public void logEnd(Trace logger, E event, boolean result) {
        logger.trace("Finishing processing event {}, result = {}", event, result);
    }

    protected List<String> evaluateExpressionChecked(
            @NotNull ExpressionType expressionBean,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variablesMap,
            String shortDesc, EventProcessingContext<?> ctx, OperationResult result) {
        return expressionHelper.evaluateExpressionChecked(expressionBean, origin, variablesMap, shortDesc, ctx, result);
    }

    protected VariablesMap getDefaultVariables(E event, OperationResult result) {
        return expressionHelper.getDefaultVariables(event, result);
    }

    protected void reportNotificationStart(E event) {
        if (event instanceof ModelEvent) {
            ((ModelEvent) event).getModelContext().reportProgress(new ProgressInformation(NOTIFICATIONS, ENTERING));
        }
    }

    protected void reportNotificationEnd(E event, OperationResult result) {
        if (event instanceof ModelEvent) {
            ((ModelEvent) event).getModelContext().reportProgress(new ProgressInformation(NOTIFICATIONS, result));
        }
    }
}
