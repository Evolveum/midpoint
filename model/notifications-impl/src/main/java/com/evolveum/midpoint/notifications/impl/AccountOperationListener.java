/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class AccountOperationListener implements ResourceOperationListener {

    private static final Trace LOGGER = TraceManager.getTrace(AccountOperationListener.class);

    private static final String DOT_CLASS = AccountOperationListener.class.getName() + ".";

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired
    private ChangeNotificationDispatcher provisioningNotificationDispatcher;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

    @PostConstruct
    public void init() {
        provisioningNotificationDispatcher.registerNotificationListener(this);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registered account operation notification listener.");
        }
    }

    @Override
    public String getName() {
        return "user notification account change listener";
    }

    @Override
    public void notifySuccess(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
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
    public void notifyInProgress(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        if (notificationsEnabled()) {
            notifyAny(OperationStatus.IN_PROGRESS, operationDescription, task, parentResult.createMinorSubresult(DOT_CLASS + "notifyInProgress"));
        }
    }

    @Override
    public void notifyFailure(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
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
            LOGGER.trace("AccountOperationListener.notify (" + status + ") called with operationDescription = " + operationDescription.debugDump());
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
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object that was changed was not an account, exiting the operation listener (class = " +
                        operationDescription.getObjectDelta().getObjectTypeClass() + ")");
            }
            return;
        }

        ResourceObjectEvent request = createRequest(status, operationDescription, task, result);
        notificationManager.processEvent(request, task, result);
    }

    private ResourceObjectEvent createRequest(OperationStatus status,
                                                     ResourceOperationDescription operationDescription,
                                                     Task task,
                                                     OperationResult result) {

        ResourceObjectEvent event = new ResourceObjectEvent(lightweightIdentifierGenerator);
        event.setAccountOperationDescription(operationDescription);
        event.setOperationStatus(status);
        event.setChangeType(operationDescription.getObjectDelta().getChangeType());       // fortunately there's 1:1 mapping

        String accountOid = operationDescription.getObjectDelta().getOid();

        PrismObject<UserType> user = findRequestee(accountOid, task, result, operationDescription.getObjectDelta().isDelete());
        if (user != null) {
            event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, user.asObjectable()));
        }   // otherwise, appropriate messages were already logged

        if (task != null && task.getOwner() != null) {
            event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));
        } else {
            LOGGER.warn("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
        }

        if (task != null && task.getChannel() != null) {
            event.setChannel(task.getChannel());
        } else if (operationDescription.getSourceChannel() != null) {
			event.setChannel(operationDescription.getSourceChannel());
		}

        return event;
    }

//    private boolean isRequestApplicable(ResourceObjectEvent request, NotificationConfigurationEntryType entry) {
//
//        ResourceOperationDescription opDescr = request.getAccountOperationDescription();
//        OperationStatus status = request.getOperationStatus();
//        ChangeType type = opDescr.getObjectDelta().getChangeType();
//        return typeMatches(type, entry.getSituation(), opDescr) && statusMatches(status, entry.getSituation());
//    }

    private PrismObject<UserType> findRequestee(String accountOid, Task task, OperationResult result, boolean isDelete) {
        PrismObject<UserType> user;

        if (accountOid != null) {
            try {
                user = cacheRepositoryService.listAccountShadowOwner(accountOid, result);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("listAccountShadowOwner for account " + accountOid + " yields " + user);
                }
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("There's a problem finding account " + accountOid, e);
                return null;
            }

            if (user != null) {
                return user;
            }
        }

        PrismObject<UserType> requestee = task != null ? task.getRequestee() : null;
        if (requestee == null) {
            LOGGER.warn("There is no owner of account " + accountOid + " (in repo nor in task).");
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Task = " + (task != null ? task.debugDump() : "(null)"));
            }
            return null;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Requestee = " + requestee + " for account " + accountOid);
        }
        if (requestee.getOid() == null) {
            return requestee;
        }

        // let's try to get current value of requestee ... if it exists (it will NOT exist in case of delete operation)
        try {
            return cacheRepositoryService.getObject(UserType.class, requestee.getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            if (isDelete) {
                result.removeLastSubresult();           // get rid of this error - it's not an error
            }
            return requestee;           // returning last known value
//            if (!isDelete) {
//                LoggingUtils.logException(LOGGER, "Cannot find owner of account " + accountOid, e);
//            } else {
//                LOGGER.info("Owner of account " + accountOid + " (user oid " + userOid + ") was probably already deleted.");
//                result.removeLastSubresult();       // to suppress the error message (in GUI + in tests)
//            }
//            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot find owner of account " + accountOid, e);
            return null;
        }
    }

}
