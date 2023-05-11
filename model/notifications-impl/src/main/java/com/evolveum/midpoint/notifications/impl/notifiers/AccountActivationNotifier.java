/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class AccountActivationNotifier extends ConfirmationNotifier<AccountActivationNotifierType> {

    @Autowired Clock clock;
    @Autowired private ModelService modelService;

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

        StringBuilder body = new StringBuilder();
        String message = "Your accounts was successfully created. To activate your accounts, please click on the link below.";
        body.append(message).append("\n\n").append(createConfirmationLink(getUser(event), configuration,
                result)).append("\n\n");

        FocusType owner = (FocusType) event.getRequesteeObject();
        String userOrOwner = owner instanceof UserType ? "User" : "Owner";
        if (owner != null) {
            body.append(userOrOwner).append(": ").append(event.getRequesteeDisplayName());
            body.append(" (").append(owner.getName()).append(", oid ").append(owner.getOid()).append(")\n");
        } else {
            body.append(userOrOwner).append(": unknown\n");
        }
        body.append("Notification created on: ").append(new Date(clock.currentTimeMillis())).append("\n\n");

        body.append("Account to be activated: \n");
        for (ShadowType shadow : getShadowsToActivate(event)) {
            String resourceOid = shadow.getResourceRef() != null ? shadow.getResourceRef().getOid() : null;
            body.append(" Resource: ");
            if (StringUtils.isNotBlank(resourceOid)) {
                PrismObject<ResourceType> resource;
                try {
                    resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
                } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException
                        | ExpressionEvaluationException | SchemaException e) {
                    getLogger().error("Couldn't get Resource with oid " + resourceOid, e);
                    throw new SystemException("Couldn't get resource " + resourceOid, e);
                }
                body.append(StringUtils.isNotBlank(resource.getDisplayName()) ? resource.getDisplayName() : resource.getName());
                body.append(" (").append("oid ").append(resourceOid).append(")\n");

            } else {
                body.append("unknown\n");
            }
            for (Object att : shadow.getAttributes().asPrismContainerValue().getItems()) {
                if (att instanceof ResourceAttribute) {
                    ResourceAttribute<?> attribute = (ResourceAttribute<?>) att;
                    body.append(" - ").append(attribute.getDisplayName()).append(": ");
                    if (attribute.isSingleValue()) {
                        body.append(attribute.getRealValue()).append("\n");
                    } else {
                        body.append("\n");
                        for (Object value : attribute.getRealValues()) {
                            body.append("   - ").append(value).append("\n");
                        }
                    }
                }
            }
        }

        ObjectType requester = event.getRequester() != null ? event.getRequester().getObjectType() : null;
        if (requester != null) {
            body.append("\nRequester: ").append(getRequestorDisplayName(requester));
            body.append(" (").append(requester.getName()).append(", oid ").append(requester.getOid()).append(")\n");
        }
        return body.toString();
    }

    private String getRequestorDisplayName(ObjectType requester) {
        String name = requester.getName().getOrig();
        if (requester.asPrismObject().getDisplayName() != null) {
            name = requester.asPrismObject().getDisplayName();
        }
        if (requester instanceof UserType) {
            if (((UserType) requester).getFullName() != null) {
                name = ((UserType) requester).getFullName().getOrig();
            }
        }
        return name;
    }

    private List<ShadowType> getShadowsToActivate(ModelEvent modelEvent) {
        return getMidpointFunctions()
                .getShadowsToActivate(
                        modelEvent.getProjectionContexts());
    }

    @Override
    public String getConfirmationLink(UserType userType) {
        return getMidpointFunctions().createAccountActivationLink(userType);
    }
}
