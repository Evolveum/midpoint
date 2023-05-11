/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.handlers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.EventHandlerRegistry;
import com.evolveum.midpoint.notifications.impl.helpers.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

/**
 * Handles now-aggregated event type (consisting of pointers to categories, operations, statuses, chained handlers,
 * notifiers, and so on). The original plug-in architecture had to be a bit abused because of the notifications
 * schema change.
 *
 * TODO should we really extend BaseHandler? E.g. should we register ourselves? (probably not)
 */
@Component
public class AggregatedEventHandler extends BaseHandler<Event, BaseEventHandlerType> {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedEventHandler.class);

    @Autowired private CategoryFilterHelper categoryFilter;
    @Autowired private OperationFilterHelper operationFilter;
    @Autowired private StatusFilterHelper statusFilter;
    @Autowired private KindIntentFilterHelper kindIntentFilter;
    @Autowired private FocusTypeFilterHelper focusTypeFilterHelper;
    @Autowired private ExpressionFilterHelper expressionFilter;
    @Autowired private EventHandlerRegistry eventHandlerRegistry;

    @Override
    public Class<Event> getEventType() {
        return Event.class;
    }

    @Override
    public Class<EventHandlerType> getEventHandlerConfigurationType() {
        return EventHandlerType.class;
    }

    @Override
    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig, Task task, OperationResult result)
            throws SchemaException {

        logStart(LOGGER, event, eventHandlerConfig);

        boolean shouldContinue = categoryFilter.processEvent(event, eventHandlerConfig)
                && operationFilter.processEvent(event, eventHandlerConfig)
                && statusFilter.processEvent(event, eventHandlerConfig)
                && kindIntentFilter.processEvent(event, eventHandlerConfig)
                && focusTypeFilterHelper.processEvent(event, eventHandlerConfig)
                && expressionFilter.processEvent(event, eventHandlerConfig, task, result);

        if (eventHandlerConfig instanceof EventHandlerType) {
            EventHandlerType handlerConfig = (EventHandlerType) eventHandlerConfig;
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleUserNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleFocalObjectNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleResourceObjectNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleWorkflowNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleCaseManagementNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getUserPasswordNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getUserRegistrationNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getUserInvitationNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getPasswordResetNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getAccountActivationNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getAccountPasswordNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getGeneralNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getCustomNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleCampaignNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleCampaignStageNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleReviewerNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleTaskNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimpleReportNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getSimplePolicyRuleNotifier(), task, result);
            shouldContinue = shouldContinue && processNotifiers(event, handlerConfig.getTimeValidityNotifier(), task, result);
        }

        logEnd(LOGGER, event, shouldContinue);
        return shouldContinue;
    }

    private boolean processNotifiers(Event event, List<? extends BaseEventHandlerType> notifiers, Task task, OperationResult result)
            throws SchemaException {
        for (BaseEventHandlerType notifier : notifiers) {
            boolean shouldContinue = eventHandlerRegistry.forwardToHandler(event, notifier, task, result);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
}
