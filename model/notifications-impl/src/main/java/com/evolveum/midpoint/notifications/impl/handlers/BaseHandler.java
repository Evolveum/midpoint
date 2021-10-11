/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.handlers;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.EventHandlerRegistry;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.helpers.NotificationExpressionHelper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.NOTIFICATIONS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

/**
 *
 */
@Component
public abstract class BaseHandler<E extends Event, C extends EventHandlerType> implements EventHandler<E, C> {

    private static final Trace LOGGER = TraceManager.getTrace(BaseHandler.class);

    @Autowired protected NotificationManagerImpl notificationManager;
    @Autowired protected NotificationFunctionsImpl notificationsUtil;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected TextFormatter textFormatter;
    @Autowired protected NotificationExpressionHelper expressionHelper;
    @Autowired protected EventHandlerRegistry eventHandlerRegistry;

    @PostConstruct
    protected void register() {
        eventHandlerRegistry.registerEventHandler(getEventHandlerConfigurationType(), this);
    }

    protected void logStart(Trace LOGGER, E event, C handlerConfiguration) {
        logStart(LOGGER, event, handlerConfiguration, null);
    }

    protected void logStart(Trace LOGGER, E event, C handlerConfiguration, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event.shortDump() + " with handler " +
                    getHumanReadableHandlerDescription(handlerConfiguration) + "\n  parameters: " +
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

    protected String getHumanReadableHandlerDescription(C handlerConfiguration) {
        if (handlerConfiguration.getName() != null) {
            return handlerConfiguration.getName();
        } else {
            return handlerConfiguration.getClass().getSimpleName();
        }
    }

    public void logEnd(Trace logger, E event, boolean result) {
        logger.trace("Finishing processing event {}, result = {}", event, result);
    }

    protected List<String> evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
            String shortDesc, Task task, OperationResult result) {
        return expressionHelper.evaluateExpressionChecked(expressionType, expressionVariables, shortDesc, task, result);
    }

    protected List<NotificationMessageAttachmentType> evaluateNotificationMessageAttachmentTypeExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
            String shortDesc, Task task, OperationResult result) {
        return expressionHelper.evaluateNotificationMessageAttachmentTypeExpressionChecked(expressionType, expressionVariables, shortDesc, task, result);
    }

    protected ExpressionVariables getDefaultVariables(E event, OperationResult result) {
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
