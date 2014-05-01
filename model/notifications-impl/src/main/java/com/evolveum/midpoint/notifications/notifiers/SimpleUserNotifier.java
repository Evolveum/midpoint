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
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class SimpleUserNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleUserNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleUserNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ModelEvent)) {
            LOGGER.trace("SimpleUserNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        }
        ModelEvent modelEvent = (ModelEvent) event;
        if (modelEvent.getFocusContext() == null || !UserType.class.isAssignableFrom(modelEvent.getFocusContext().getObjectTypeClass())) {
            LOGGER.trace("SimpleUserNotifier is not applicable to non-user related model operations, continuing in the handler chain");
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        List<ObjectDelta<UserType>> deltas = ((ModelEvent) event).getUserDeltas();
        if (deltas.isEmpty()) {
            return false;
        }

        if (isWatchAuxiliaryAttributes(generalNotifierType)) {
            return true;
        }

        for (ObjectDelta<UserType> delta : deltas) {
            if (!delta.isModify() || deltaContainsOtherPathsThan(delta, auxiliaryPaths)) {
                return true;
            }
        }

        return false;
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

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        ModelContext<UserType> modelContext = (ModelContext) ((ModelEvent) event).getModelContext();
        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
        PrismObject<UserType> user = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        UserType userType = user.asObjectable();
        String oid = focusContext.getOid();

        ObjectDelta<UserType> delta = ObjectDelta.summarize(((ModelEvent) event).getUserDeltas());

        StringBuilder body = new StringBuilder();

        String status;
        if (event.isSuccess()) {
            status = "SUCCESS";
        } else if (event.isOnlyFailure()) {
            status = "FAILURE";
        } else if (event.isFailure()) {
            status = "PARTIAL FAILURE";
        } else if (event.isInProgress()) {
            status = "IN PROGRESS";
        } else {
            status = "UNKNOWN";
        }

        String attemptedTo = event.isSuccess() ? "" : "(attempted to be) ";

        body.append("Notification about user-related operation (status: " + status + ")\n\n");
        body.append("User: " + userType.getFullName() + " (" + userType.getName() + ", oid " + oid + ")\n");
        body.append("Notification created on: " + new Date() + "\n\n");

        List<ItemPath> hiddenPaths = isWatchAuxiliaryAttributes(generalNotifierType) ? new ArrayList<ItemPath>() : auxiliaryPaths;
        if (delta.isAdd()) {
            body.append("The user record was " + attemptedTo + "created with the following data:\n");
            body.append(textFormatter.formatObject(delta.getObjectToAdd(), hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType)));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append("The user record was " + attemptedTo + "modified. Modified attributes are:\n");
            body.append(textFormatter.formatObjectModificationDelta(delta, hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType)));
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("The user record was " + attemptedTo + "removed.\n\n");
        }

        if (!event.isSuccess()) {
            body.append("More information about the status of the request was displayed and/or is present in log files.\n\n");
        }

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(modelContext.debugDump(2));
        }

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
