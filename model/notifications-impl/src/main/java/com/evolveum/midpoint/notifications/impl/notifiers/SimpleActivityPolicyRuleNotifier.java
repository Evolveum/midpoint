/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ActivityPolicyRuleEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleActivityPolicyRuleNotifierType;

@Component
public class SimpleActivityPolicyRuleNotifier
        extends AbstractGeneralNotifier<ActivityPolicyRuleEvent, SimpleActivityPolicyRuleNotifierType> {

    @Override
    public @NotNull Class<SimpleActivityPolicyRuleNotifierType> getEventHandlerConfigurationType() {
        return SimpleActivityPolicyRuleNotifierType.class;
    }

    @Override
    public @NotNull Class<ActivityPolicyRuleEvent> getEventType() {
        return ActivityPolicyRuleEvent.class;
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleActivityPolicyRuleNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ActivityPolicyRuleEvent> ctx,
            OperationResult result) {
        return "Activity policy rule '" + ctx.event().getRuleName() + "' triggering notification";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleActivityPolicyRuleNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ActivityPolicyRuleEvent> ctx,
            OperationResult result) throws SchemaException {
        return "Notification about policy rule-related event.\n\n"
                + ctx.event().getPolicyRule().debugDump();
    }
}

