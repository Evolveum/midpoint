/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccountActivationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class AccountActivationNotifier extends ConfirmationNotifier<AccountActivationNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccountActivationNotifier.class);

    @Override
    public Class<AccountActivationNotifierType> getEventHandlerConfigurationType() {
        return AccountActivationNotifierType.class;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    @Override
    protected boolean checkApplicability(ModelEvent event, AccountActivationNotifierType configuration,
            OperationResult result) {
        if (!event.isSuccess()) {
            logNotApplicable(event, "operation was not successful");
            return false;
        }

        if (event.getFocusDeltas().isEmpty()) {
            logNotApplicable(event, "no user deltas in event");
            return false;
        }

        List<ShadowType> shadows = getShadowsToActivate(event);
        if (shadows.isEmpty()) {
            logNotApplicable(event, "no shadows to activate found in model context");
            return false;
        } else {
            LOGGER.trace("Found shadows to activate: {}. Processing notifications.", shadows);
            return true;
        }
    }

    @Override
    protected String getSubject(ModelEvent event, AccountActivationNotifierType configuration, String transport,
            Task task, OperationResult result) {
        return "Activate your accounts";
    }

    @Override
    protected String getBody(ModelEvent event, AccountActivationNotifierType configuration, String transport,
            Task task, OperationResult result) {

        String message = "Your accounts was successfully created. To activate your accounts, please click on the link bellow.";

        StringBuilder accountsToActivate = new StringBuilder("Shadow to be activated: \n");
        for (ShadowType shadow : getShadowsToActivate(event)) {
            accountsToActivate.append(shadow.asPrismObject().debugDump()).append("\n");
        }

        return message + "\n\n" + createConfirmationLink(getUser(event), configuration, result) + "\n\n" + accountsToActivate;
    }

    private List<ShadowType> getShadowsToActivate(ModelEvent modelEvent) {
        Collection<? extends ModelElementContext> projectionContexts = modelEvent.getProjectionContexts();
        return getMidpointFunctions().getShadowsToActivate(projectionContexts);
    }

    @Override
    public String getConfirmationLink(UserType userType) {
        return getMidpointFunctions().createAccountActivationLink(userType);
    }
}
