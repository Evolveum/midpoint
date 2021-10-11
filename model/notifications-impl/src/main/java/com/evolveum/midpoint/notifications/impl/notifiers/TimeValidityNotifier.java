/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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
    public Class<TimeValidityNotifierType> getEventHandlerConfigurationType() {
        return TimeValidityNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(PolicyRuleEvent event, TimeValidityNotifierType configuration, OperationResult result) {
        return UserType.class.isAssignableFrom(event.getRequesteeObject().getClass());
    }

    @Override
    protected boolean checkApplicability(PolicyRuleEvent event, TimeValidityNotifierType configuration, OperationResult result) {
        PolicyConstraintsType policyConstraints = event.getPolicyRule().getPolicyConstraints();
        return policyConstraints != null &&
                policyConstraints.getObjectTimeValidity() != null &&
                !policyConstraints.getObjectTimeValidity().isEmpty();
    }

    @Override
    protected String getSubject(PolicyRuleEvent event, TimeValidityNotifierType configuration, String transport, Task task,
            OperationResult result) {
        return "Planned deactivation of user " + getUserName(event);
    }

    @Override
    protected String getBody(PolicyRuleEvent event, TimeValidityNotifierType configuration, String transport, Task task,
            OperationResult result) {
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
