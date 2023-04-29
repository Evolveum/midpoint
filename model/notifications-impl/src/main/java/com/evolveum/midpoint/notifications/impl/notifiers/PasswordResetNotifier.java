/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.ModelEvent;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordResetNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class PasswordResetNotifier extends ConfirmationNotifier<PasswordResetNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Override
    public Class<PasswordResetNotifierType> getEventHandlerConfigurationType() {
        return PasswordResetNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(ModelEvent event, PasswordResetNotifierType configuration,
            OperationResult result) {
        // TODO generalize to FocusType
        if (!event.hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "PasswordResetNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
                            + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(ModelEvent event, PasswordResetNotifierType configuration,
            OperationResult result) {
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
    protected String getSubject(ModelEvent event, PasswordResetNotifierType configuration, String transport,
            Task task, OperationResult result) {
        return "Password reset";
    }

    @Override
    protected String getBody(ModelEvent event, PasswordResetNotifierType generalNotifierType, String transport, Task task,
            OperationResult result) {
        UserType userType = getUser(event);
        return "Did you request password reset? If yes, click on the link below \n\n"
                + createConfirmationLink(userType, generalNotifierType, event.getChannel(), result);
    }

    @Override
    public String getConfirmationLink(UserType userType, String channel) {
        return getMidpointFunctions().createPasswordResetLink(userType);
    }
}
