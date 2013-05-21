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
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
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
        return !(((ModelEvent) event).getUserDeltas()).isEmpty();
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        if (event.isAdd()) {
            return "User creation notification";
        } else if (event.isModify()) {
            return "User modification notification";
        } else if (event.isDelete()) {
            return "User deletion notification";
        } else {
            return "(unknown user operation)";
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) throws SchemaException {

        boolean techInfo = generalNotifierType.getLevelOfDetail() != null && generalNotifierType.getLevelOfDetail() >= LEVEL_TECH_INFO;

        ModelContext<UserType,? extends ObjectType> modelContext = (ModelContext) ((ModelEvent) event).getModelContext();
        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        PrismObject<UserType> user = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        UserType userType = user.asObjectable();

        ObjectDelta<UserType> delta = ObjectDelta.summarize(((ModelEvent) event).getUserDeltas());

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
        body.append("Operation status: " + event.getOperationStatus() + "\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(modelContext.debugDump(2));
        }

        return body.toString();
    }

}
