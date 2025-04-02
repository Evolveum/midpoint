/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class SimpleResourceObjectNotifier extends AbstractGeneralNotifier<ResourceObjectEvent, SimpleResourceObjectNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleResourceObjectNotifier.class);

    @Override
    public @NotNull Class<ResourceObjectEvent> getEventType() {
        return ResourceObjectEvent.class;
    }

    @Override
    public @NotNull Class<SimpleResourceObjectNotifierType> getEventHandlerConfigurationType() {
        return SimpleResourceObjectNotifierType.class;
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends SimpleResourceObjectNotifierType> configuration,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        return ctx.event().hasContentToShow(
                isWatchSynchronizationAttributes(configuration.value()),
                isWatchAuxiliaryAttributes(configuration.value()));
    }

    private boolean isWatchSynchronizationAttributes(SimpleResourceObjectNotifierType configuration) {
        return Boolean.TRUE.equals((configuration).isWatchSynchronizationAttributes());
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleResourceObjectNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        ResourceOperationDescription rod = event.getOperationDescription();
        //noinspection unchecked
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        String objectTypeDescription = event.isShadowKind(ShadowKindType.ACCOUNT) ? "Account" : "Resource object";

        if (delta.isAdd()) {
            return objectTypeDescription + " creation notification";
        } else if (delta.isModify()) {
            return objectTypeDescription + " modification notification";
        } else if (delta.isDelete()) {
            return objectTypeDescription + " deletion notification";
        } else {
            return "(unknown resource object operation)";
        }
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleResourceObjectNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {

        boolean techInfo = Boolean.TRUE.equals(configuration.value().isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();

        var event = ctx.event();
        FocusType owner = (FocusType) event.getRequesteeObject();
        ResourceOperationDescription rod = event.getOperationDescription();
        //noinspection unchecked
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        boolean isAccount = event.isShadowKind(ShadowKindType.ACCOUNT);
        String objectTypeDescription = isAccount ? "account" : "resource object";
        String userOrOwner = owner instanceof UserType ? "User" : "Owner";

        body.append("Notification about ").append(objectTypeDescription).append("-related operation\n\n");
        if (isAccount) {
            if (owner != null) {
                body.append(userOrOwner).append(": ").append(event.getRequesteeDisplayName());
                body.append(" (").append(owner.getName()).append(", oid ").append(owner.getOid()).append(")\n");
            } else {
                body.append(userOrOwner).append(": unknown\n");
            }
        }
        body.append("Notification created on: ").append(new Date()).append("\n\n");
        body.append("Resource: ").append(event.getResourceName()).append(" (oid ").append(event.getResourceOid()).append(")\n");
        boolean named;
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            if (isAccount) {
                body.append("Account: ").append(rod.getCurrentShadow().asObjectable().getName()).append("\n");
            } else {
                body.append("Resource object: ").append(rod.getCurrentShadow().asObjectable().getName()).append(" (kind: ").append(rod.getCurrentShadow().asObjectable().getKind()).append(")\n");
            }
            named = true;
        } else {
            named = false;
        }
        body.append("\n");

        if (isAccount) {
            body.append(named ? "The" : "An").append(" account ");
        } else {
            body.append(named ? "The" : "A").append(" resource object ");
        }
        switch (event.getOperationStatus()) {
            case SUCCESS: body.append("has been successfully "); break;
            case IN_PROGRESS: body.append("has been ATTEMPTED to be "); break;
            case FAILURE: body.append("FAILED to be "); break;
        }

        boolean watchSynchronizationAttributes = isWatchSynchronizationAttributes(configuration.value());
        boolean watchAuxiliaryAttributes = isWatchAuxiliaryAttributes(configuration.value());
        final Task task = ctx.task();

        if (delta.isAdd()) {
            body.append("created on the resource with attributes:\n");
            body.append(event.getContentAsFormattedList(watchSynchronizationAttributes, watchAuxiliaryAttributes, task,
                    result));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append("modified on the resource. Modified attributes are:\n");
            body.append(event.getContentAsFormattedList(watchSynchronizationAttributes, watchAuxiliaryAttributes, task,
                    result));
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("removed from the resource.\n\n");
        }

        if (event.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("The operation will be retried.\n\n");
        } else if (event.getOperationStatus() == OperationStatus.FAILURE) {
            body.append("Error: ").append(event.getOperationDescription().getMessage()).append("\n\n");
        }

        body.append("\n\n");
        addRequesterAndChannelInformation(body, event, result);

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(rod.debugDump(2));
        }

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
