/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
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
    private ChangeNotificationDispatcher provisioningNotificationDispatcher;

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

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
        notifyAny(OperationStatus.SUCCESS, operationDescription, task, parentResult.createSubresult(DOT_CLASS + "notifySuccess"));
    }

    @Override
    public void notifyInProgress(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        notifyAny(OperationStatus.IN_PROGRESS, operationDescription, task, parentResult.createSubresult(DOT_CLASS + "notifyInProgress"));
    }

    @Override
    public void notifyFailure(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        notifyAny(OperationStatus.FAILURE, operationDescription, task, parentResult.createSubresult(DOT_CLASS + "notifyFailure"));
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

        AccountEvent request = createRequest(status, operationDescription, task, result);
        if (request != null) {
            notificationManager.processEvent(request, result);
        }
    }

    private AccountEvent createRequest(OperationStatus status,
                                                     ResourceOperationDescription operationDescription,
                                                     Task task,
                                                     OperationResult result) {

        AccountEvent event = new AccountEvent();
        event.setAccountOperationDescription(operationDescription);
        event.setOperationStatus(status);
        event.setChangeType(operationDescription.getObjectDelta().getChangeType());       // fortunately there's 1:1 mapping

        String accountOid = operationDescription.getObjectDelta().getOid();

        PrismObject<UserType> user = findRequestee(accountOid, task, result, operationDescription.getObjectDelta().isDelete());
        if (user == null) {
            return null;        // appropriate message is already logged
        }

        if (task.getOwner() != null) {
            event.setRequester(task.getOwner().asObjectable());
        } else {
            LOGGER.warn("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
        }
        event.setRequestee(user.asObjectable());
        event.setAccountOwner(user.asObjectable());
        return event;
    }

//    private boolean isRequestApplicable(AccountEvent request, NotificationConfigurationEntryType entry) {
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
                LOGGER.trace("There's a problem finding account " + accountOid + ", no notification will be sent", e);
                return null;
            }

            if (user != null) {
                return user;
            }
        }

        String userOid = task.getRequesteeOid();
        if (userOid == null) {
            LOGGER.warn("There is no owner of account " + accountOid + " (in repo nor in task), cannot send the notification.");
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Task = " + task.dump());
            }
            return null;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Requestee OID = " + userOid + " for account " + accountOid);
        }
        try {
            return cacheRepositoryService.getObject(UserType.class, userOid, result);
        } catch (ObjectNotFoundException e) {
            if (!isDelete) {
                LoggingUtils.logException(LOGGER, "Cannot find owner of account " + accountOid + ", no notification will be sent", e);
            } else {
                LOGGER.info("Owner of account " + accountOid + " (user oid " + userOid + ") was probably already deleted, no notification will be sent");
                result.removeLastSubresult();       // to suppress the error message (in GUI + in tests)
            }
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot find owner of account " + accountOid + ", no notification will be sent", e);
            return null;
        }
    }

//    private boolean typeMatches(ChangeType type, List<QName> filter, ResourceOperationDescription operationDescription) {
//        if (type == ChangeType.ADD) {
//            return filter.contains(NotificationConstants.ACCOUNT_CREATION_QNAME);
//        } else if (type == ChangeType.MODIFY) {
//            return filter.contains(NotificationConstants.ACCOUNT_MODIFICATION_QNAME) && changedNotOnlySyncSituation(operationDescription);
//        } else if (type == ChangeType.DELETE) {
//            return filter.contains(NotificationConstants.ACCOUNT_DELETION_QNAME);
//        } else {
//            LOGGER.warn("Unknown account change type: " + type + ", no notification will be sent.");
//            return false;
//        }
//    }
//
//    // assuming: change type is MODIFY
//    private boolean changedNotOnlySyncSituation(ResourceOperationDescription operationDescription) {
//        if (operationDescription.getObjectDelta() == null) {
//            return true;        // dubious, but let's have it this way
//        } else {
//            for (ItemDelta id : operationDescription.getObjectDelta().getModifications()) {
//                if (!ShadowType.F_SYNCHRONIZATION_SITUATION.equals(id.getName()) &&
//                        !ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION.equals(id.getName())) {
//                    return true;
//                }
//            }
//            return false;
//        }
//    }
//
//    private boolean statusMatches(OperationStatus status, List<QName> filter) {
//        boolean allowSuccess = filter.contains(NotificationConstants.SUCCESS_QNAME);
//        boolean allowInProgress = filter.contains(NotificationConstants.IN_PROGRESS_QNAME);
//        boolean allowFailure = filter.contains(NotificationConstants.FAILURE_QNAME);
//
//        if (!allowSuccess && !allowInProgress && !allowFailure) {
//            return true;
//        }
//
//        switch (status) {
//            case SUCCESS: return allowSuccess;
//            case IN_PROGRESS: return allowInProgress;
//            case FAILURE: return allowFailure;
//            default:
//                LOGGER.warn("Unknown operation status type: " + status + ", no notification will be sent.");
//                return false;
//        }
//    }
}
