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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class SimpleResourceObjectNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleResourceObjectNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleResourceObjectNotifierType.class);
    }

    private static final List<ItemPath> synchronizationPaths = Arrays.asList(
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_TIMESTAMP),
            new ItemPath(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP));


    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ResourceObjectEvent)) {
            LOGGER.trace("SimpleResourceObjectNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {

        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;
        ObjectDelta<ShadowType> delta = resourceObjectEvent.getShadowDelta();
        if (!delta.isModify()) {
            return true;
        }

        boolean otherThanSyncPresent = deltaContainsOtherPathsThan(delta, synchronizationPaths);
        boolean otherThanAuxPresent = deltaContainsOtherPathsThan(delta, auxiliaryPaths);
        boolean watchSync = isWatchSynchronizationAttributes((SimpleResourceObjectNotifierType) generalNotifierType);
        boolean watchAux = isWatchAuxiliaryAttributes(generalNotifierType);
        if ((watchSync || otherThanSyncPresent) && (watchAux || otherThanAuxPresent)) {
            return true;
        }

        LOGGER.trace("No relevant attributes in delta, skipping the notifier (watchSync = " + watchSync + ", otherThanSyncPresent = " + otherThanSyncPresent +
                ", watchAux = " + watchAux + ", otherThanAuxPresent = " + otherThanAuxPresent + ")");
        return false;
    }

    private boolean isWatchSynchronizationAttributes(SimpleResourceObjectNotifierType generalNotifierType) {
        return Boolean.TRUE.equals((generalNotifierType).isWatchSynchronizationAttributes());
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        ResourceOperationDescription rod = resourceObjectEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        String objectTypeDescription = resourceObjectEvent.isShadowKind(ShadowKindType.ACCOUNT) ? "Account" : "Resource object";

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
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();

        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        UserType owner = (UserType) notificationsUtil.getObjectType(resourceObjectEvent.getRequestee(), result);
        ResourceOperationDescription rod = resourceObjectEvent.getAccountOperationDescription();
        ObjectDelta<ShadowType> delta = (ObjectDelta<ShadowType>) rod.getObjectDelta();

        boolean isAccount = resourceObjectEvent.isShadowKind(ShadowKindType.ACCOUNT);
        String objectTypeDescription = isAccount ? "account" : "resource object";

        body.append("Notification about ").append(objectTypeDescription).append("-related operation\n\n");
        if (isAccount) {
            if (owner != null) {
                body.append("User: " + owner.getFullName() + " (" + owner.getName() + ", oid " + owner.getOid() + ")\n");
            } else {
                body.append("User: unknown\n");
            }
        }
        body.append("Notification created on: " + new Date() + "\n\n");
        body.append("Resource: " + rod.getResource().asObjectable().getName() + " (oid " + rod.getResource().getOid() + ")\n");
        boolean named;
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            if (isAccount) {
                body.append("Account: " + rod.getCurrentShadow().asObjectable().getName() + "\n");
            } else {
                body.append("Resource object: " + rod.getCurrentShadow().asObjectable().getName() + " (kind: " + rod.getCurrentShadow().asObjectable().getKind() + ")\n");
            }
            named = true;
        } else {
            named = false;
        }
        body.append("\n");

        if (isAccount) {
            body.append((named ? "The" : "An") + " account ");
        } else {
            body.append((named ? "The" : "A") + " resource object ");
        }
        switch (resourceObjectEvent.getOperationStatus()) {
            case SUCCESS: body.append("has been successfully "); break;
            case IN_PROGRESS: body.append("has been ATTEMPTED to be "); break;
            case FAILURE: body.append("FAILED to be "); break;
        }

        if (delta.isAdd()) {
            body.append("created on the resource with attributes:\n");
            body.append(textFormatter.formatAccountAttributes(delta.getObjectToAdd().asObjectable(), null, isWatchAuxiliaryAttributes(generalNotifierType)));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append("modified on the resource. Modified attributes are:\n");
            List<ItemPath> hiddenPaths = new ArrayList<ItemPath>();
            if (!isWatchSynchronizationAttributes((SimpleResourceObjectNotifierType) generalNotifierType)) {
                hiddenPaths.addAll(synchronizationPaths);
            }
            if (!isWatchAuxiliaryAttributes(generalNotifierType)) {
                hiddenPaths.addAll(auxiliaryPaths);
            }

            if (resourceObjectEvent.getOperationStatus() != OperationStatus.IN_PROGRESS) {
                // todo we do not have objectOld + objectNew, only the current status
                // it is used to explain modified containers with identifiers -- however, currently I don't know of use of such containers in shadows, which would be visible in notifications
                body.append(textFormatter.formatObjectModificationDelta(delta, hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType),
                        resourceObjectEvent.getAccountOperationDescription().getCurrentShadow(), null));
            } else {
                // special case - here the attributes are 'result', 'failedOperationType', 'objectChange', 'attemptNumber'
                // we have to unwrap attributes that are to be modified from the objectChange item
                Collection<PrismPropertyValue<ObjectDeltaType>> changes = null;
                if (delta.getModifications() != null) {
                    for (ItemDelta itemDelta : delta.getModifications()) {
                        if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_OBJECT_CHANGE))) {
                            changes = itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty() ?
                                    itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
                        }
                    }
                }

                if (changes != null && !changes.isEmpty()) {
                    try {
                        List<ObjectDelta<ShadowType>> deltas = new ArrayList<ObjectDelta<ShadowType>>(changes.size());
                        for (PrismPropertyValue<ObjectDeltaType> change : changes) {
                            deltas.add((ObjectDelta) DeltaConvertor.createObjectDelta(change.getValue(), prismContext));
                        }
                        ObjectDelta<ShadowType> shadowDelta = ObjectDelta.summarize(deltas);
                        body.append(textFormatter.formatObjectModificationDelta(shadowDelta, hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType),
                                resourceObjectEvent.getAccountOperationDescription().getCurrentShadow(), null));
                    } catch (SchemaException e) {
                        LoggingUtils.logException(LOGGER, "Unable to determine the shadow change; operation = {}", e, resourceObjectEvent.getAccountOperationDescription().debugDump());
                        body.append("(unable to determine the change because of schema exception: ").append(e.getMessage()).append(")\n");
                    }
                } else {
                    body.append("(unable to determine the change)\n");
                }
            }
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("removed from the resource.\n\n");
        }

        if (resourceObjectEvent.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("The operation will be retried.\n\n");
        } else if (resourceObjectEvent.getOperationStatus() == OperationStatus.FAILURE) {
            body.append("Error: " + resourceObjectEvent.getAccountOperationDescription().getResult().getMessage() + "\n\n");
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
