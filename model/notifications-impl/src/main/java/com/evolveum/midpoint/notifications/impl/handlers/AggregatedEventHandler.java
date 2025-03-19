/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.handlers;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.EventHandlerRegistry;
import com.evolveum.midpoint.notifications.impl.helpers.*;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.EventHandlerConfigItem;
import com.evolveum.midpoint.schema.result.OperationResult;
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
    public @NotNull Class<Event> getEventType() {
        return Event.class;
    }

    @Override
    public @NotNull Class<EventHandlerType> getEventHandlerConfigurationType() {
        return EventHandlerType.class;
    }

    @Override
    public boolean processEvent(
            @NotNull ConfigurationItem<? extends BaseEventHandlerType> handlerConfig,
            @NotNull EventProcessingContext<?> ctx,
            @NotNull OperationResult result)
            throws SchemaException {

        logStart(LOGGER, handlerConfig, ctx);

        boolean shouldContinue = categoryFilter.processEvent(handlerConfig, ctx)
                && operationFilter.processEvent(handlerConfig, ctx)
                && statusFilter.processEvent(handlerConfig, ctx)
                && kindIntentFilter.processEvent(handlerConfig, ctx)
                && focusTypeFilterHelper.processEvent(handlerConfig, ctx)
                && expressionFilter.processEvent(handlerConfig, ctx, result);

        // We are too lazy to determine the exact origin here. It is not needed for profiles anyway, as the profile
        // is pre-determined and stored in the context.
        ConfigurationItemOrigin o = handlerConfig.origin().toApproximate();

        if (handlerConfig.value() instanceof EventHandlerType configBean) {
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleUserNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleFocalObjectNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleResourceObjectNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleWorkflowNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleCaseManagementNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getUserPasswordNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getUserRegistrationNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getPasswordResetNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getAccountActivationNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getAccountPasswordNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getGeneralNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getCustomNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleCampaignNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleCampaignStageNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleReviewerNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleTaskNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleReportNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimplePolicyRuleNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getTimeValidityNotifier(), o, ctx, result);
            shouldContinue = shouldContinue && processNotifiers(configBean.getSimpleActivityPolicyRuleNotifier(), o, ctx, result);
        }

        logEnd(LOGGER, ctx.event(), shouldContinue);
        return shouldContinue;
    }

    private boolean processNotifiers(
            List<? extends BaseEventHandlerType> notifiers,
            ConfigurationItemOrigin approximateOrigin,
            EventProcessingContext<?> ctx,
            OperationResult result)
            throws SchemaException {
        for (BaseEventHandlerType notifier : notifiers) {
            boolean shouldContinue = eventHandlerRegistry.forwardToHandler(
                    EventHandlerConfigItem.of(notifier, approximateOrigin),
                    ctx, result);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
}
