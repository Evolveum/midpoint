/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctions;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * TODO generalize to non-user principals
 */
@Component
public class UserPasswordNotifier extends AbstractGeneralNotifier<ModelEvent, UserPasswordNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(UserPasswordNotifier.class);

    @Autowired private NotificationFunctions notificationsUtil;

    @Override
    public @NotNull Class<ModelEvent> getEventType() {
        return ModelEvent.class;
    }

    @Override
    public @NotNull Class<UserPasswordNotifierType> getEventHandlerConfigurationType() {
        return UserPasswordNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends UserPasswordNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        if (!ctx.event().hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "UserPasswordNotifier is not applicable for this kind of event, continuing in the handler chain; "
                            + "event class = {}", ctx.getEventClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends UserPasswordNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        if (!event.isAlsoSuccess()) { // TODO
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else {
            return event.getFocusPassword() != null;    // logging is done in the called method
        }
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends UserPasswordNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        return "User password notification";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends UserPasswordNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        return "Password for user "
                + notificationsUtil.getObject(event.getRequestee(), false, result).getName()
                + " is: "
                + event.getFocusPassword();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
