/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeValidityNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author katkav
 */
@Component
public class TimeValidityNotifier extends AbstractPolicyRuleNotifier<TimeValidityNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(TimeValidityNotifier.class);

    @Override
    public @NotNull Class<TimeValidityNotifierType> getEventHandlerConfigurationType() {
        return TimeValidityNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends TimeValidityNotifierType> configuration,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) {
        return UserType.class.isAssignableFrom(ctx.event().getRequesteeObject().getClass());
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends TimeValidityNotifierType> configuration,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) {
        PolicyConstraintsType policyConstraints = ctx.event().getPolicyRule().getPolicyConstraints();
        return policyConstraints != null &&
                policyConstraints.getObjectTimeValidity() != null &&
                !policyConstraints.getObjectTimeValidity().isEmpty();
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends TimeValidityNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) {
        return "Planned deactivation of user " + getUserName(ctx.event());
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends TimeValidityNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends PolicyRuleEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        return "User " + getUserName(event) + " is going to be deactivated on " + getUser(event).getActivation().getValidTo();
    }

    private String getUserName(PolicyRuleEvent event) {
        UserType user = getUser(event);
        PolyStringType username = user.getName();
        return username.getOrig();
    }

    private UserType getUser(PolicyRuleEvent event) {
        return (UserType) event.getRequesteeObject();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
