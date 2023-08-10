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
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class RegistrationConfirmationNotifier extends ConfirmationNotifier<RegistrationConfirmationNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

    @Override
    public @NotNull Class<RegistrationConfirmationNotifierType> getEventHandlerConfigurationType() {
        return RegistrationConfirmationNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends RegistrationConfirmationNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        // TODO generalize to FocusType
        if (!ctx.event().hasFocusOfType(UserType.class)) {
            LOGGER.trace(
                    "RegistrationConfirmationNotifier is not applicable for this kind of event, continuing in the handler chain; "
                            + "event class = {}", ctx.getEventClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends RegistrationConfirmationNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
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
    protected String getSubject(
            ConfigurationItem<? extends RegistrationConfirmationNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        return "Registration confirmation";
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends RegistrationConfirmationNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {

      UserType userType = getUser(ctx.event());

        String plainTextPassword = "IhopeYouRememberYourPassword";
        try {
            plainTextPassword = getMidpointFunctions().getPlaintextUserPassword(userType);
        } catch (EncryptionException e) {
            //ignore...????
        }

        return "Dear " + userType.getGivenName() + ",\n"
                + "your account was successfully created. To activate your account click on the following confirmation link. "
                + "\n"
                + createConfirmationLink(userType, configuration, result)
                + "\n\n"
                + "After your account is activated, use following credentials to log in: \n"
                + "username: "
                + userType.getName().getOrig()
                + "password: "
                + plainTextPassword;
    }

    @Override
    public String getConfirmationLink(UserType user) {
        return getMidpointFunctions().createRegistrationConfirmationLink(user);
    }
}
