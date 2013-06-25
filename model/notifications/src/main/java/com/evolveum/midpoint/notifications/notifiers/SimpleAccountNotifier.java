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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.OperationStatus;
import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class SimpleAccountNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleAccountNotifier.class);
    private static final Integer LEVEL_TECH_INFO = 10;

    @PostConstruct
    public void init() {
        register(SimpleAccountNotifierType.class);
    }

    private static final List<ItemPath> synchronizationPaths = Arrays.asList(
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_TIMESTAMP));


    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof AccountEvent)) {
            LOGGER.trace("SimpleAccountNotifier was called with incompatible notification event; class = " + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {

        AccountEvent accountEvent = (AccountEvent) event;
        ObjectDelta<ShadowType> delta = accountEvent.getShadowDelta();
        if (!delta.isModify()) {
            return true;
        }

        boolean otherThanSyncPresent = deltaContainsOtherPathsThan(delta, synchronizationPaths);
        boolean otherThanAuxPresent = deltaContainsOtherPathsThan(delta, auxiliaryPaths);
        boolean watchSync = isWatchSynchronizationAttributes((SimpleAccountNotifierType) generalNotifierType);
        boolean watchAux = isWatchAuxiliaryAttributes(generalNotifierType);
        if ((watchSync || otherThanSyncPresent) && (watchAux || otherThanAuxPresent)) {
            return true;
        }

        LOGGER.trace("No relevant attributes in delta, skipping the notifier (watchSync = " + watchSync + ", otherThanSyncPresent = " + otherThanSyncPresent +
                ", watchAux = " + watchAux + ", otherThanAuxPresent = " + otherThanAuxPresent + ")");
        return false;
    }

    private boolean isWatchSynchronizationAttributes(SimpleAccountNotifierType generalNotifierType) {
        return Boolean.TRUE.equals((generalNotifierType).isWatchSynchronizationAttributes());
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        AccountEvent accountEvent = (AccountEvent) event;

        ResourceOperationDescription rod = accountEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        if (delta.isAdd()) {
            return "Account creation notification";
        } else if (delta.isModify()) {
            return "Account modification notification";
        } else if (delta.isDelete()) {
            return "Account deletion notification";
        } else {
            return "(unknown account operation)";
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();

        AccountEvent accountEvent = (AccountEvent) event;

        UserType owner = (UserType) notificationsUtil.getObjectType(accountEvent.getRequestee(), result);
        ResourceOperationDescription rod = accountEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        body.append("Notification about account-related operation\n\n");
        if (owner != null) {
            body.append("User: " + owner.getFullName() + " (" + owner.getName() + ", oid " + owner.getOid() + ")\n");
        } else {
            body.append("User: unknown\n");
        }
        body.append("Notification created on: " + new Date() + "\n\n");
        body.append("Resource: " + rod.getResource().asObjectable().getName() + " (oid " + rod.getResource().getOid() + ")\n");
        boolean named;
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            body.append("Account: " + rod.getCurrentShadow().asObjectable().getName() + "\n");
            named = true;
        } else {
            named = false;
        }
        body.append("\n");

        body.append((named ? "The" : "An") + " account ");
        switch (accountEvent.getOperationStatus()) {
            case SUCCESS: body.append("has been successfully "); break;
            case IN_PROGRESS: body.append("has been ATTEMPTED to be "); break;
            case FAILURE: body.append("FAILED to be "); break;
        }

        if (delta.isAdd()) {
            body.append("created on the resource with attributes:\n");
            body.append(textFormatter.formatAccountAttributes(delta.getObjectToAdd().asObjectable()));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append("modified on the resource. Modified attributes are:\n");
            List<ItemPath> hiddenPaths = new ArrayList<ItemPath>();
            if (!isWatchSynchronizationAttributes((SimpleAccountNotifierType) generalNotifierType)) {
                hiddenPaths.addAll(synchronizationPaths);
            }
            if (!isWatchAuxiliaryAttributes(generalNotifierType)) {
                hiddenPaths.addAll(auxiliaryPaths);
            }
            body.append(textFormatter.formatObjectModificationDelta(delta, hiddenPaths));
//            appendModifications(body, delta, hiddenPaths, generalNotifierType.isShowModifiedValues());
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("removed from the resource.\n\n");
        }

        if (accountEvent.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("The operation will be retried.\n\n");
        } else if (accountEvent.getOperationStatus() == OperationStatus.FAILURE) {
            body.append("Error: " + accountEvent.getAccountOperationDescription().getResult().getMessage() + "\n\n");
        }

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(rod.debugDump(2));
        }

        return body.toString();
    }


//    private String getLocalPart(QName name) {
//        if (name == null) {
//            return null;
//        } else {
//            return name.getLocalPart();
//        }
//    }
//
//    private String getResourceName(AccountShadowType account) {
//        String oid = null;
//        if (account.getResource() != null) {
//            if (account.getResource().getName() != null) {
//                return account.getResource().getName().getOrig();
//            }
//            oid = account.getResource().getOid();
//        } else {
//            if (account.getResourceRef() != null) {
//                oid = account.getResourceRef().getOid();
//            }
//        }
//        if (oid == null) {
//            return ("(unknown resource)");
//        }
//        return NotificationsUtil.getResourceNameFromRepo(cacheRepositoryService, oid, new OperationResult("dummy"));
//    }
//
//    private void listAccounts(StringBuilder messageText, List<String> lines) {
//        boolean first = true;
//        for (String line : lines) {
//            if (first) {
//                first = false;
//            } else {
//                messageText.append(",\n");
//            }
//            messageText.append(line);
//        }
//        messageText.append(".\n\n");
//    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
