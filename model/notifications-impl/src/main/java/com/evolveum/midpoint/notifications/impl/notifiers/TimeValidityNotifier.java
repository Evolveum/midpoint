/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeValidityNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author katkav
 */
@Component
public class TimeValidityNotifier extends SimplePolicyRuleNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(TimeValidityNotifier.class);

    @PostConstruct
    public void init() {
        register(TimeValidityNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
       if (!(event instanceof PolicyRuleEvent)) {
           return false;
       }
       PolicyRuleEvent modelEvent = (PolicyRuleEvent) event;

        return UserType.class.isAssignableFrom(modelEvent.getRequesteeObject().getClass());
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        PolicyRuleEvent ruleEvent = (PolicyRuleEvent) event;
        PolicyConstraintsType policyConstraints = ruleEvent.getPolicyRule().getPolicyConstraints();
        return policyConstraints != null &&
                policyConstraints.getObjectTimeValidity() != null &&
                !policyConstraints.getObjectTimeValidity().isEmpty();
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task,
            OperationResult result) {


        return "Planned deactivation of user " + getUserName(event);
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task,
            OperationResult result) throws SchemaException {

        return "User " + getUserName(event) + " is going to be deactivated on " + getUser(event).getActivation().getValidTo();

    }

    private String getUserName(Event event){
        UserType user = getUser(event);
        PolyStringType username = user.getName();
        return username.getOrig();
    }

    private UserType getUser(Event event){
        PolicyRuleEvent taskEvent = (PolicyRuleEvent) event;
        UserType user = (UserType) taskEvent.getRequesteeObject();
        return user;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
