/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordResetNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class PasswordResetNotifier extends ConfirmationNotifier<PasswordResetNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Override
    public @NotNull Class<PasswordResetNotifierType> getEventHandlerConfigurationType() {
        return PasswordResetNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends PasswordResetNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        // TODO generalize to FocusType
        if (!ctx.event().hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "PasswordResetNotifier is not applicable for this kind of event, continuing in the handler chain; "
                            + "event class {}", ctx.getEventClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends PasswordResetNotifierType> notifierConfig,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else if (event.getFocusDeltas().isEmpty()) {
            LOGGER.trace("No user deltas in event, exiting.");
            return false;
        } else if (SchemaConstants.CHANNEL_RESET_PASSWORD_URI.equals(event.getChannel())) {
            LOGGER.trace("Found change from reset password channel.");
            return true;
        } else {
            LOGGER.trace("No password reset present in delta. Skip sending notifications.");
            return false;
        }
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends PasswordResetNotifierType> notifierConfig,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        return "Password reset";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends PasswordResetNotifierType> notifierConfig,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        UserType userType = getUser(ctx.event());
        return "Did you request password reset? If yes, click on the link below \n\n"
                + createConfirmationLink(userType, notifierConfig, result);
    }

    @Override
    public String getConfirmationLink(UserType user) {
        return getMidpointFunctions().createPasswordResetLink(user);
    }
}
