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

import com.evolveum.midpoint.notifications.NotificationsUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SimpleAccountNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

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
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION));

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof AccountEvent)) {
            LOGGER.trace("SimpleAccountNotifier was called with incompatible notification event; class = " + event.getClass());
            return false;
        } else {

            AccountEvent accountEvent = (AccountEvent) event;
            ObjectDelta<ShadowType> delta = accountEvent.getShadowDelta();
            if (!delta.isModify() || ((SimpleAccountNotifierType) generalNotifierType).isWatchSynchronizationAttributes() == Boolean.TRUE) {
                return true;
            }

            for (ItemDelta itemDelta : delta.getModifications()) {
                if (!synchronizationPaths.contains(itemDelta.getPath())) {
                    return true;
                }
            }
            LOGGER.trace("Only synchronization-related attributes in delta, skipping the notifier.");
            return false;
        }
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

        boolean techInfo = generalNotifierType.getLevelOfDetail() != null && generalNotifierType.getLevelOfDetail() >= LEVEL_TECH_INFO;

        StringBuilder body = new StringBuilder();

        AccountEvent accountEvent = (AccountEvent) event;

        UserType owner = accountEvent.getAccountOwner();
        ResourceOperationDescription rod = accountEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        body.append("Notification about account-related operation\n\n");
        body.append("User: " + owner.getFullName() + " (" + owner.getName() + ", oid " + owner.getOid() + ")\n");
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
        switch (event.getOperationStatus()) {
            case SUCCESS: body.append("has been successfully "); break;
            case IN_PROGRESS: body.append("has been ATTEMPTED to be "); break;
            case FAILURE: body.append("FAILED to be "); break;
        }

        if (delta.isAdd()) {
            body.append("created on the resource.\n\n");
        } else if (delta.isModify()) {
            body.append("modified on the resource. Modified attributes are:\n");
            for (ItemDelta itemDelta : delta.getModifications()) {
                body.append(" - " + itemDelta.getName().getLocalPart() + "\n");
            }
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("removed from the resource.\n\n");
        }

        if (event.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("The operation will be retried.\n\n");
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

}
