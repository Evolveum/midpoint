/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.impl.events.ResourceObjectEventImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Converts provisioning events into notification events.
 */
@Component
public class AccountOperationListener implements ResourceOperationListener {

    private static final Trace LOGGER = TraceManager.getTrace(AccountOperationListener.class);

    private static final String DOT_CLASS = AccountOperationListener.class.getName() + ".";

    @Autowired private PrismContext prismContext;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private EventDispatcher provisioningEventDispatcher;
    @Autowired private NotificationManager notificationManager;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired private NotificationFunctions notificationsUtil;

    @PostConstruct
    public void init() {
        provisioningEventDispatcher.registerListener(this);
        LOGGER.trace("Registered account operation notification listener.");
    }

    @PreDestroy
    public void preDestroy() {
        provisioningEventDispatcher.unregisterListener(this);
    }

    @Override
    public String getName() {
        return "user notification account change listener";
    }

    @Override
    public void notifySuccess(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        if (notificationsEnabled()) {
            notifyAny(OperationStatus.SUCCESS, operationDescription, task, parentResult.createMinorSubresult(DOT_CLASS + "notifySuccess"));
        }
    }

    private boolean notificationsEnabled() {
        if (notificationManager.isDisabled()) {
            LOGGER.trace("Notifications are temporarily disabled, exiting the hook.");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void notifyInProgress(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        if (notificationsEnabled()) {
            notifyAny(OperationStatus.IN_PROGRESS, operationDescription, task, parentResult.createMinorSubresult(DOT_CLASS + "notifyInProgress"));
        }
    }

    @Override
    public void notifyFailure(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        if (notificationsEnabled()) {
            notifyAny(OperationStatus.FAILURE, operationDescription, task, parentResult.createMinorSubresult(DOT_CLASS + "notifyFailure"));
        }
    }

    private void notifyAny(OperationStatus status, ResourceOperationDescription operationDescription, Task task, OperationResult result) {
        try {
            executeNotifyAny(status, operationDescription, task, result);
        } catch (RuntimeException e) {
            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
            LoggingUtils.logException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
        }

        // todo work correctly with operationResult (in whole notification module)
        if (result.isUnknown()) {
            result.computeStatus();
        }
        result.recordSuccessIfUnknown();
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Returning operation result: " + result.dump());
//        }
    }

    private void executeNotifyAny(OperationStatus status, ResourceOperationDescription operationDescription, Task task, OperationResult result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("AccountOperationListener.notify ({}) called with operationDescription = {}", status, operationDescription.debugDump());
        }

        if (operationDescription.getObjectDelta() == null) {
            LOGGER.warn("Object delta is null, exiting the change listener.");
            return;
        }

        if (operationDescription.getCurrentShadow() == null) {
            LOGGER.warn("Current shadow is null, exiting the change listener.");
            return;
        }

        // for the time being, we deal only with accounts here
        if (operationDescription.getObjectDelta().getObjectTypeClass() == null ||
                !ShadowType.class.isAssignableFrom(operationDescription.getObjectDelta().getObjectTypeClass())) {
            LOGGER.trace("Object that was changed was not an account, exiting the operation listener (class = {})",
                    operationDescription.getObjectDelta().getObjectTypeClass());
            return;
        }

        ResourceObjectEventImpl request = createRequest(status, operationDescription, task, result);
        notificationManager.processEvent(request, task, result);
    }

    @NotNull
    private ResourceObjectEventImpl createRequest(
            OperationStatus status,
            ResourceOperationDescription operationDescription,
            Task task,
            OperationResult result) {

        ResourceObjectEventImpl event = new ResourceObjectEventImpl(
                lightweightIdentifierGenerator, operationDescription, status);

        String accountOid = operationDescription.getObjectDelta().getOid();

        PrismObject<UserType> user = findRequestee(accountOid, task, result);
        if (user != null) {
            event.setRequestee(new SimpleObjectRefImpl(user.asObjectable()));
        }   // otherwise, appropriate messages were already logged

        PrismObject<? extends FocusType> taskOwner = task != null ? task.getOwner(result) : null;
        if (taskOwner != null) {
            event.setRequester(new SimpleObjectRefImpl(taskOwner));
        } else {
            LOGGER.warn("No owner for task {}, therefore no requester will be set for event {}", task, event.getId());
        }

        if (task != null && task.getChannel() != null) {
            event.setChannel(task.getChannel());
        } else if (operationDescription.getSourceChannel() != null) {
            event.setChannel(operationDescription.getSourceChannel());
        }

        return event;
    }

    private PrismObject<UserType> findRequestee(String shadowOid, Task task, OperationResult result) {
        // This is (still) a temporary solution. We need to rework it eventually.
        if (task != null && task.getRequestee() != null) {
            return task.getRequestee();
        } else if (shadowOid != null) {
            try {
                ObjectQuery query = prismContext.queryFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .ref(shadowOid)
                        .build();
                SearchResultList<PrismObject<UserType>> prismObjects =
                        cacheRepositoryService.searchObjects(UserType.class, query, null, result);
                PrismObject<UserType> user = MiscUtil.extractSingleton(prismObjects);

                LOGGER.trace("listAccountShadowOwner for shadow {} yields {}", shadowOid, user);
                return user;
            } catch (SchemaException e) {
                LOGGER.trace("There's a problem finding account {}", shadowOid, e);
                return null;
            }
        } else {
            LOGGER.debug("There is no owner of account {} (in repo nor in task).", shadowOid);
            return null;
        }
    }
}
