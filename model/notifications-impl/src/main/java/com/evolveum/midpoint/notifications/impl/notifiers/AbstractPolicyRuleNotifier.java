/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimplePolicyRuleNotifierType;

/**
 *
 */
@Component
public abstract class AbstractPolicyRuleNotifier<C extends SimplePolicyRuleNotifierType> extends AbstractGeneralNotifier<PolicyRuleEvent, C> {

    @Override
    public Class<PolicyRuleEvent> getEventType() {
        return PolicyRuleEvent.class;
    }

    @Override
    protected String getSubject(PolicyRuleEvent event, SimplePolicyRuleNotifierType configuration, String transport, Task task, OperationResult result) {
        return "Policy rule '" + event.getRuleName() + "' triggering notification";
    }

    @Override
    protected String getBody(PolicyRuleEvent event, SimplePolicyRuleNotifierType configuration, String transport, Task opTask, OperationResult opResult) throws SchemaException {
        return "Notification about policy rule-related event.\n\n"
                // TODO TODO TODO
                + event.getPolicyRule().debugDump();
    }
}
