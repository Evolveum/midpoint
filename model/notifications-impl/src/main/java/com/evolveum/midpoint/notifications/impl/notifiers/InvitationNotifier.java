/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InvitationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class InvitationNotifier extends ConfirmationNotifier<InvitationNotifierType> {
    private static final Trace LOGGER = TraceManager.getTrace(InvitationNotifier.class);

    @Override
    public Class<InvitationNotifierType> getEventHandlerConfigurationType() {
        return InvitationNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(ModelEvent event, InvitationNotifierType configuration, OperationResult result) {
        if (!event.hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "InvitationNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
                            + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(ModelEvent event, InvitationNotifierType configuration, OperationResult result) {
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else if (SchemaConstants.CHANNEL_INVITATION_URI.equals(event.getChannel())) {
            LOGGER.trace("Found change from invitation channel.");
            return true;
        } else {
            LOGGER.trace("No invitation event is found. Skip sending notifications.");
            return false;
        }
    }

    @Override
    protected String getSubject(ModelEvent event, InvitationNotifierType configuration, String transport,
            Task task, OperationResult result) {
        return "Invitation link";
    }

    @Override
    protected String getBody(ModelEvent event, InvitationNotifierType configuration, String transport,
            Task task, OperationResult result) {

        UserType userType = getUser(event);

        StringBuilder messageBuilder = new StringBuilder("Dear ");
        messageBuilder.append(userType.getGivenName()).append(",\n")
                .append("To activate your account click on the following invitation link "
                        + "and follow the instructions on the opened page. ")
                .append("\n")
                .append(createConfirmationLink(userType, configuration, result))
                .append("\n\n");

        return messageBuilder.toString();
    }

    @Override
    public String getConfirmationLink(UserType userType) {
        return getMidpointFunctions().createInvitationLink(userType);
    }
}
