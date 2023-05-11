/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */


package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.ModelEvent;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class RegistrationConfirmationNotifier extends ConfirmationNotifier<RegistrationConfirmationNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Override
    public Class<RegistrationConfirmationNotifierType> getEventHandlerConfigurationType() {
        return RegistrationConfirmationNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(ModelEvent event, RegistrationConfirmationNotifierType configuration,
            OperationResult result) {
        // TODO generalize to FocusType
        if (!event.hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "RegistrationConfirmationNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
                            + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(ModelEvent event, RegistrationConfirmationNotifierType configuration,
            OperationResult result) {
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        } else if (event.getFocusDeltas().isEmpty()) {
            LOGGER.trace("No user deltas in event, exiting.");
            return false;
        } else if (SchemaConstants.CHANNEL_SELF_REGISTRATION_URI.equals(event.getChannel())) {
            LOGGER.trace("Found change from registration channel.");
            return true;
        } else {
            LOGGER.trace("No registration present in delta. Skip sending notifications.");
            return false;
        }
    }

    @Override
    protected String getSubject(ModelEvent event, RegistrationConfirmationNotifierType configuration, String transport,
            Task task, OperationResult result) {
        return "Registration confirmation";
    }

    @Override
    protected String getBody(ModelEvent event, RegistrationConfirmationNotifierType configuration, String transport,
            Task task, OperationResult result) {

      UserType userType = getUser(event);

        String plainTextPassword = "IhopeYouRememberYourPassword";
        try {
            plainTextPassword = getMidpointFunctions().getPlaintextUserPassword(userType);
        } catch (EncryptionException e) {
            //ignore...????
        }

        StringBuilder messageBuilder = new StringBuilder("Dear ");
        messageBuilder.append(userType.getGivenName()).append(",\n")
                .append("your account was successfully created. To activate your account click on the following confirmation link. ")
                .append("\n")
                .append(createConfirmationLink(userType, configuration, result))
                .append("\n\n")
                .append("After your account is activated, use following credentials to log in: \n")
                .append("username: ")
                .append(userType.getName().getOrig())
                .append("password: ")
                .append(plainTextPassword);

        return messageBuilder.toString();
    }

    @Override
    public String getConfirmationLink(UserType userType) {
        return getMidpointFunctions().createRegistrationConfirmationLink(userType);
    }
}
