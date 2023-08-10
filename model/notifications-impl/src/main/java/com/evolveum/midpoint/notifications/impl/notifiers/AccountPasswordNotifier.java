/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccountPasswordNotifierType;

@Component
public class AccountPasswordNotifier extends AbstractGeneralNotifier<ResourceObjectEvent, AccountPasswordNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccountPasswordNotifier.class);

    @Override
    public @NotNull Class<ResourceObjectEvent> getEventType() {
        return ResourceObjectEvent.class;
    }

    @Override
    public @NotNull Class<AccountPasswordNotifierType> getEventHandlerConfigurationType() {
        return AccountPasswordNotifierType.class;
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends AccountPasswordNotifierType> configuration,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else if (event.getPlaintextPassword() == null) {
            LOGGER.trace("No password in delta, exiting.");
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends AccountPasswordNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        return "Account password notification";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends AccountPasswordNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        StringBuilder body = new StringBuilder();

        var event = ctx.event();
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
