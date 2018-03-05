/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
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
