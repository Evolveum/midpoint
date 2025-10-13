/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimplePolicyRuleNotifierType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractPolicyRuleNotifier<C extends SimplePolicyRuleNotifierType> extends AbstractGeneralNotifier<PolicyRuleEvent, C> {

    @Override
    public @NotNull Class<PolicyRuleEvent> getEventType() {
        return PolicyRuleEvent.class;
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends C> configuration,
            String transport,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) {
        return "Policy rule '" + ctx.event().getRuleName() + "' triggering notification";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends C> configuration,
            String transport,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) throws SchemaException {
        return "Notification about policy rule-related event.\n\n"
                // TODO TODO TODO
                + ctx.event().getPolicyRule().debugDump();
    }
}
