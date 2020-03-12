/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccountPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.stereotype.Component;

@Component
public class AccountPasswordNotifier extends AbstractGeneralNotifier<ResourceObjectEvent, AccountPasswordNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccountPasswordNotifier.class);

    @Override
    public Class<ResourceObjectEvent> getEventType() {
        return ResourceObjectEvent.class;
    }

    @Override
    public Class<AccountPasswordNotifierType> getEventHandlerConfigurationType() {
        return AccountPasswordNotifierType.class;
    }

    @Override
    protected boolean checkApplicability(ResourceObjectEvent event, AccountPasswordNotifierType configuration, OperationResult result) {
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else {
            ObjectDelta<? extends ShadowType> delta = event.getAccountOperationDescription().getObjectDelta();
            return delta != null && functions.getPlaintextPasswordFromDelta(delta) != null;
        }
    }

    @Override
    protected String getSubject(ResourceObjectEvent event, AccountPasswordNotifierType configuration, String transport, Task task, OperationResult result) {
        return "Account password notification";
    }

    @Override
    protected String getBody(ResourceObjectEvent event, AccountPasswordNotifierType configuration, String transport, Task task, OperationResult result) {
        StringBuilder body = new StringBuilder();

        body.append("Password for account ");
        String name = event.getShadowName();
        if (name != null) {
            body.append(name).append(" ");
        }
        body.append("on ").append(event.getResourceName());
        body.append(" is: ").append(event.getPlaintextPassword());
        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
