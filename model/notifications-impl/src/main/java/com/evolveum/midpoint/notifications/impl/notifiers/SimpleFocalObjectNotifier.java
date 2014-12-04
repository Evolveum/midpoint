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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class SimpleFocalObjectNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleFocalObjectNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleFocalObjectNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ModelEvent)) {
            LOGGER.trace("{} is not applicable for this kind of event, continuing in the handler chain; event class = {}", getClass().getSimpleName(), event.getClass());
            return false;
        }
        ModelEvent modelEvent = (ModelEvent) event;
        if (modelEvent.getFocusContext() == null || !FocusType.class.isAssignableFrom(modelEvent.getFocusContext().getObjectTypeClass())) {
            LOGGER.trace("{} is not applicable to non-focus related model operations, continuing in the handler chain", getClass().getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        List<ObjectDelta<FocusType>> deltas = ((ModelEvent) event).getFocusDeltas();
        if (deltas.isEmpty()) {
            return false;
        }

        if (isWatchAuxiliaryAttributes(generalNotifierType)) {
            return true;
        }

        for (ObjectDelta<FocusType> delta : deltas) {
            if (!delta.isModify() || deltaContainsOtherPathsThan(delta, auxiliaryPaths)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        String typeName = getFocusTypeName(event);

        if (event.isAdd()) {
            return typeName + " creation notification";
        } else if (event.isModify()) {
            return typeName + " modification notification";
        } else if (event.isDelete()) {
            return typeName + " deletion notification";
        } else {
            return "(unknown " + typeName.toLowerCase() + " operation)";
        }
    }

    // assuming the quick availability check was passed
    private String getFocusTypeName(Event event) {
        String simpleName = ((ModelEvent) event).getFocusContext().getObjectTypeClass().getSimpleName();
        return StringUtils.substringBeforeLast(simpleName, "Type");         // should usually work ;)
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) throws SchemaException {

        String typeName = getFocusTypeName(event);
        String typeNameLower = typeName.toLowerCase();

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        ModelContext<FocusType> modelContext = (ModelContext) ((ModelEvent) event).getModelContext();
        ModelElementContext<FocusType> focusContext = modelContext.getFocusContext();
        PrismObject<FocusType> focus = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        FocusType userType = focus.asObjectable();
        String oid = focusContext.getOid();

        String fullName;
        if (userType instanceof UserType) {
            fullName = PolyString.getOrig(((UserType) userType).getFullName());
        } else if (userType instanceof AbstractRoleType) {
            fullName = PolyString.getOrig(((AbstractRoleType) userType).getDisplayName());
        } else {
            fullName = "";          // TODO (currently it's not possible to get here)
        }

        if (fullName == null) {
            fullName = "";          // "null" is not nice in notifications
        }

        ObjectDelta<FocusType> delta = ObjectDelta.summarize(((ModelEvent) event).getFocusDeltas());

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

        body.append("Notification about ").append(typeNameLower).append("-related operation (status: " + status + ")\n\n");
        body.append(typeName).append(": " + fullName + " (" + userType.getName() + ", oid " + oid + ")\n");
        body.append("Notification created on: " + new Date() + "\n\n");

        List<ItemPath> hiddenPaths = isWatchAuxiliaryAttributes(generalNotifierType) ? new ArrayList<ItemPath>() : auxiliaryPaths;
        if (delta.isAdd()) {
            body.append("The ").append(typeNameLower).append(" record was " + attemptedTo + "created with the following data:\n");
            body.append(textFormatter.formatObject(delta.getObjectToAdd(), hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType)));
            body.append("\n");
        } else if (delta.isModify()) {
            body.append("The ").append(typeNameLower).append(" record was " + attemptedTo + "modified. Modified attributes are:\n");
            body.append(textFormatter.formatObjectModificationDelta(delta, hiddenPaths, isWatchAuxiliaryAttributes(generalNotifierType), focusContext.getObjectOld(), focusContext.getObjectNew()));
            body.append("\n");
        } else if (delta.isDelete()) {
            body.append("The ").append(typeNameLower).append(" record was " + attemptedTo + "removed.\n\n");
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
