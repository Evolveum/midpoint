/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccountPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class AccountPasswordNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(AccountPasswordNotifier.class);
    private static final Integer LEVEL_TECH_INFO = 10;

    @Autowired
    private MidpointFunctions midpointFunctions;

    @PostConstruct
    public void init() {
        register(AccountPasswordNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ResourceObjectEvent)) {
            LOGGER.trace("AccountPasswordNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        }

        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;
        ObjectDelta<? extends ShadowType> delta = resourceObjectEvent.getAccountOperationDescription().getObjectDelta();
        if (delta == null) {    // should not occur
            LOGGER.trace("Object delta is null, exiting. Event = " + event);
            return false;
        }
        return functions.getPlaintextPasswordFromDelta(delta) != null;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return "Account password notification";
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        StringBuilder body = new StringBuilder();
        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        body.append("Password for account ");
        String name = resourceObjectEvent.getShadowName();
        if (name != null) {
            body.append(name).append(" ");
        }
        body.append("on ").append(resourceObjectEvent.getResourceName());
        body.append(" is: ").append(resourceObjectEvent.getPlaintextPassword());
        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
