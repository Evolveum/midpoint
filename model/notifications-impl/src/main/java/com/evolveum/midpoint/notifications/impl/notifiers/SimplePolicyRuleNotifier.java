/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimplePolicyRuleNotifierType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class SimplePolicyRuleNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimplePolicyRuleNotifier.class);

    @PostConstruct
    public void init() {
        register(SimplePolicyRuleNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof PolicyRuleEvent)) {
            LOGGER.trace("{} is not applicable for this kind of event, continuing in the handler chain; event class = {}", getClass().getSimpleName(), event.getClass());
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
		PolicyRuleEvent ruleEvent = (PolicyRuleEvent) event;
		return "Policy rule '" + ruleEvent.getRuleName() + "' triggering notification";
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task opTask, OperationResult opResult) throws SchemaException {
		PolicyRuleEvent ruleEvent = (PolicyRuleEvent) event;

        StringBuilder body = new StringBuilder();

        body.append("Notification about policy rule-related event.\n\n");
		// TODO TODO TODO
		body.append(ruleEvent.getPolicyRule().debugDump());
        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
