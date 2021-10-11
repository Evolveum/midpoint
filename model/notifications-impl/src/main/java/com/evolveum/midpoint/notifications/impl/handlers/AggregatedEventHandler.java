/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.handlers;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.EventHandlerRegistry;
import com.evolveum.midpoint.notifications.impl.helpers.CategoryFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ChainHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ExpressionFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.FocusTypeFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ForkHelper;
import com.evolveum.midpoint.notifications.impl.helpers.KindIntentFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.OperationFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.StatusFilterHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * Handles now-aggregated event type (consisting of pointers to categories, operations, statuses, chained handlers,
 * notifiers, and so on). The original plug-in architecture had to be a bit abused because of the notifications
 * schema change.
 *
 * TODO should we really extend BaseHandler? E.g. should we register ourselves? (probably not)
 */
@Component
public class AggregatedEventHandler extends BaseHandler<Event, EventHandlerType> {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedEventHandler.class);

    @Autowired private CategoryFilterHelper categoryFilter;
    @Autowired private OperationFilterHelper operationFilter;
    @Autowired private StatusFilterHelper statusFilter;
    @Autowired private KindIntentFilterHelper kindIntentFilter;
    @Autowired private FocusTypeFilterHelper focusTypeFilterHelper;
    @Autowired private ExpressionFilterHelper expressionFilter;
    @Autowired private ChainHelper chainHelper;
    @Autowired private ForkHelper forkHelper;
    @Autowired private NotificationManager notificationManager;
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
    public boolean processEvent(Event event, EventHandlerType configuration, Task task, OperationResult result)
            throws SchemaException {

        logStart(LOGGER, event, configuration);

        boolean shouldContinue =
                categoryFilter.processEvent(event, configuration) &&
                operationFilter.processEvent(event, configuration) &&
                statusFilter.processEvent(event, configuration) &&
                kindIntentFilter.processEvent(event, configuration) &&
                focusTypeFilterHelper.processEvent(event, configuration) &&
                expressionFilter.processEvent(event, configuration, task, result) &&
                chainHelper.processEvent(event, configuration, notificationManager, task, result) &&
                forkHelper.processEvent(event, configuration, notificationManager, task, result);

        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleUserNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleFocalObjectNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleResourceObjectNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleWorkflowNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleCaseManagementNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getUserPasswordNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getUserRegistrationNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getPasswordResetNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getAccountActivationNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getAccountPasswordNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getGeneralNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getCustomNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleCampaignNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleCampaignStageNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleReviewerNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleTaskNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimpleReportNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getSimplePolicyRuleNotifier(), task, result);
        shouldContinue = shouldContinue && processNotifiers(event, configuration.getTimeValidityNotifier(), task, result);

        logEnd(LOGGER, event, shouldContinue);
        return shouldContinue;
    }

    private boolean processNotifiers(Event event, List<? extends EventHandlerType> notifiers, Task task, OperationResult result) throws SchemaException {
        for (EventHandlerType notifier : notifiers) {
            boolean shouldContinue = eventHandlerRegistry.forwardToHandler(event, notifier, task, result);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
}
