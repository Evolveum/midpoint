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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.OperationStatus;
import com.evolveum.midpoint.notifications.events.AccountEvent;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class SimpleUserNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleUserNotifier.class);
    private static final Integer LEVEL_TECH_INFO = 10;

    @PostConstruct
    public void init() {
        register(SimpleUserNotifierType.class);
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ModelEvent)) {
            LOGGER.trace("SimpleUserNotifier was called with incompatible notification event; class = " + event.getClass());
            return false;
        }
        ModelContext modelContext = ((ModelEvent) event).getModelContext();
        if (modelContext == null || modelContext.getFocusContext() == null || modelContext.getFocusContext().getPrimaryDelta() == null) {
            LOGGER.trace("No primary focus delta in model context, exiting.");
            return false;
        }
        ObjectDelta primaryDelta = modelContext.getFocusContext().getPrimaryDelta();
        if (primaryDelta.getObjectTypeClass() == null || !UserType.class.isAssignableFrom(primaryDelta.getObjectTypeClass())) {
            LOGGER.trace("Not a UserType operation; object type class in primary delta = " + primaryDelta.getObjectTypeClass());
            return false;
        }
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        ObjectDelta delta = ((ModelEvent) event).getModelContext().getFocusContext().getPrimaryDelta();

        if (delta.isAdd()) {
            return "User creation notification";
        } else if (delta.isModify()) {
            return "User modification notification";
        } else if (delta.isDelete()) {
            return "User deletion notification";
        } else {
            return "(unknown user operation)";
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        boolean techInfo = generalNotifierType.getLevelOfDetail() != null && generalNotifierType.getLevelOfDetail() >= LEVEL_TECH_INFO;

        ModelContext<UserType,? extends ObjectType> modelContext = (ModelContext) ((ModelEvent) event).getModelContext();
        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        PrismObject<UserType> user = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        UserType userType = user.asObjectable();
        ObjectDelta<UserType> delta = focusContext.getPrimaryDelta();

        StringBuilder body = new StringBuilder();

        body.append("Notification about user-related operation\n\n");
        body.append("User: " + userType.getFullName() + " (" + userType.getName() + ", oid " + userType.getOid() + ")\n");
        body.append("Notification created on: " + new Date() + "\n\n");

        if (delta.isAdd()) {
            body.append("The user record was created.\n\n");
        } else if (delta.isModify()) {
            body.append("The user record was modified. Modified attributes are:\n");
            for (ItemDelta itemDelta : delta.getModifications()) {
                body.append(" - " + itemDelta.getName().getLocalPart() + "\n");
            }
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("The user record was removed.\n\n");
        }

        // todo what about the status?

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(modelContext.debugDump(2));
        }

        return body.toString();
    }

}
