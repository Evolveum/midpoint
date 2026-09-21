/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Date;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.notifications.impl.formatters.FormattingContext;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
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
    @Autowired private TextFormatter textFormatter;

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
    protected String getSubject(
            ConfigurationItem<? extends SimpleResourceObjectNotifierType> configuration,
            String transport,
            Locale locale,
            EventProcessingContext<? extends ResourceObjectEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        ResourceOperationDescription rod = event.getOperationDescription();
        //noinspection unchecked
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        boolean isAccount = event.isShadowKind(ShadowKindType.ACCOUNT);
        String objectType = isAccount ? "account" : "resourceObject";
        String objectTypeDescription = isAccount ? "Account" : "Resource object";
        String operation;
        String fallback;
        if (delta.isAdd()) {
            operation = "ADD";
            fallback = objectTypeDescription + " creation notification";
        } else if (delta.isModify()) {
            operation = "MODIFY";
            fallback = objectTypeDescription + " modification notification";
        } else if (delta.isDelete()) {
            operation = "DELETE";
            fallback = objectTypeDescription + " deletion notification";
        } else {
            return "(unknown resource object operation)";
        }
        return translate(
                "SimpleResourceObjectNotifier.subject." + objectType + "." + operation,
                new Object[0], locale, fallback);
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleResourceObjectNotifierType> configuration,
            String transport, Locale locale,
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

        body.append(translate(
                "SimpleResourceObjectNotifier.heading." + (isAccount ? "account" : "resourceObject"),
                new Object[0], locale,
                "Notification about " + objectTypeDescription + "-related operation"));
        body.append("\n\n");
        if (isAccount) {
            if (owner != null) {
                body.append(translate(
                        "SimpleResourceObjectNotifier." + userOrOwner.toLowerCase(Locale.ROOT),
                        new Object[] { event.getRequesteeDisplayName(), owner.getName(), owner.getOid() }, locale,
                        userOrOwner + ": " + event.getRequesteeDisplayName()
                                + " (" + owner.getName() + ", oid " + owner.getOid() + ")"));
                body.append("\n");
            } else {
                body.append(translate(
                        "SimpleResourceObjectNotifier." + userOrOwner.toLowerCase(Locale.ROOT) + "Unknown",
                        new Object[0], locale, userOrOwner + ": unknown"));
                body.append("\n");
            }
        }
        body.append(translate(
                "AbstractGeneralNotifier.notificationCreatedOn", new Object[0], locale, "Notification created on:"));
        body.append(" ").append(new Date()).append("\n\n");
        body.append(translate(
                "SimpleResourceObjectNotifier.resource",
                new Object[] { event.getResourceName(), event.getResourceOid() }, locale,
                "Resource: " + event.getResourceName() + " (oid " + event.getResourceOid() + ")"));
        body.append("\n");
        boolean named;
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            if (isAccount) {
                var accountName = rod.getCurrentShadow().asObjectable().getName();
                body.append(translate(
                        "SimpleResourceObjectNotifier.account",
                        new Object[] { accountName }, locale,
                        "Account: " + accountName));
                body.append("\n");
            } else {
                body.append("Resource object: ").append(rod.getCurrentShadow().asObjectable().getName()).append(" (kind: ").append(rod.getCurrentShadow().asObjectable().getKind()).append(")\n");
            }
            named = true;
        } else {
            named = false;
        }
        body.append("\n");

        boolean watchSynchronizationAttributes = isWatchSynchronizationAttributes(configuration.value());
        boolean watchAuxiliaryAttributes = isWatchAuxiliaryAttributes(configuration.value());
        final Task task = ctx.task();

        if (delta.isAdd()) {
            body.append(localizedOperationDescription(isAccount, named, "ADD", event.getOperationStatus(), locale));
            body.append("\n");
            body.append(textFormatter.formatResourceObjectDelta(
                    event.getShadowDelta(), watchSynchronizationAttributes, watchAuxiliaryAttributes,
                    task, result, new FormattingContext(locale)));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append(localizedOperationDescription(isAccount, named, "MODIFY", event.getOperationStatus(), locale));
            body.append("\n");
            body.append(textFormatter.formatResourceObjectDelta(
                    event.getShadowDelta(), watchSynchronizationAttributes, watchAuxiliaryAttributes,
                    task, result, new FormattingContext(locale)));
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append(localizedOperationDescription(isAccount, named, "DELETE", event.getOperationStatus(), locale));
            body.append("\n\n");
        }

        if (event.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("\n");
            body.append(translate(
                    "SimpleResourceObjectNotifier.operationWillBeRetried",
                    new Object[0], locale,
                    "The operation will be retried."));
        } else if (event.getOperationStatus() == OperationStatus.FAILURE) {
            body.append("\n");
            String errorMessage = event.getOperationDescription().getMessage();
            body.append(translate(
                    "SimpleResourceObjectNotifier.error",
                    new Object[] { errorMessage }, locale,
                    "Error: " + errorMessage));
        }

        body.append("\n\n");
        addRequesterAndChannelInformation(body, event, result, locale);

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(rod.debugDump(2));
        }

        return body.toString();
    }

    private String localizedOperationDescription(
            boolean isAccount, boolean named, String operation, OperationStatus status, Locale locale) {
        String objectType = isAccount ? "account" : "resourceObject";
        String naming = named ? "named" : "unnamed";
        return translate(
                "SimpleResourceObjectNotifier.operation."
                        + objectType + "." + operation + "." + status.name() + "." + naming,
                new Object[0], locale,
                operationDescriptionFallback(isAccount, named, operation, status));
    }

    private String operationDescriptionFallback(
            boolean isAccount, boolean named, String operation, OperationStatus status) {
        StringBuilder description = new StringBuilder();
        if (isAccount) {
            description.append(named ? "The" : "An").append(" account ");
        } else {
            description.append(named ? "The" : "A").append(" resource object ");
        }
        switch (status) {
            case SUCCESS -> description.append("has been successfully ");
            case IN_PROGRESS -> description.append("has been ATTEMPTED to be ");
            case FAILURE -> description.append("FAILED to be ");
        }
        switch (operation) {
            case "ADD" -> description.append("created on the resource with attributes:");
            case "MODIFY" -> description.append("modified on the resource. Modified attributes are:");
            case "DELETE" -> description.append("removed from the resource.");
            default -> throw new IllegalArgumentException("Unsupported resource object operation: " + operation);
        }
        return description.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
