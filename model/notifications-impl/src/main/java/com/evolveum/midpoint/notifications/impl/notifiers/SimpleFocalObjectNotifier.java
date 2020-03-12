/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

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
        if (modelEvent.getFocusContext() == null || !modelEvent.getFocusContext().isOfType(FocusType.class)) {
            LOGGER.trace("{} is not applicable to non-focus related model operations, continuing in the handler chain", getClass().getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        List<ObjectDelta<FocusType>> deltas = ((ModelEvent) event).getFocusDeltas();
        if (deltas.isEmpty()) {
            LOGGER.trace("No deltas found, skipping the notification");
            return false;
        }

        if (isWatchAuxiliaryAttributes(generalNotifierType)) {
            return true;
        }

        for (ObjectDelta<FocusType> delta : deltas) {
            if (!delta.isModify() || deltaContainsOtherPathsThan(delta, functions.getAuxiliaryPaths())) {
                return true;
            }
        }

        LOGGER.trace("No deltas for non-auxiliary attributes found, skipping the notification");
        return false;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {

        final ModelEvent modelEvent = (ModelEvent) event;
        String typeName = modelEvent.getFocusTypeName();

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

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) throws SchemaException {

        ModelEvent modelEvent = (ModelEvent) event;

        String typeName = modelEvent.getFocusTypeName();
        String typeNameLower = typeName.toLowerCase();

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        //noinspection unchecked
        ModelContext<FocusType> modelContext = (ModelContext<FocusType>) modelEvent.getModelContext();
        ModelElementContext<FocusType> focusContext = modelContext.getFocusContext();
        PrismObject<FocusType> focusObject = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        FocusType focus = focusObject.asObjectable();
        String oid = focusContext.getOid();

        String fullName = emptyIfNull(getFullName(focus));

        ObjectDelta<FocusType> delta = ObjectDeltaCollectionsUtil.summarize(modelEvent.getFocusDeltas());

        StringBuilder body = new StringBuilder();

        String status = modelEvent.getStatusAsText();
        String attemptedTo = event.isSuccess() ? "" : "(attempted to be) ";

        body.append("Notification about ").append(typeNameLower).append("-related operation (status: ").append(status).append(")\n\n");
        body.append(typeName).append(": ").append(fullName).append(" (").append(focus.getName()).append(", oid ").append(oid).append(")\n");
        body.append("Notification created on: ").append(new Date()).append("\n\n");

        final boolean watchAuxiliaryAttributes = isWatchAuxiliaryAttributes(generalNotifierType);
        if (delta.isAdd()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("created with the following data:\n");
            body.append(modelEvent.getContentAsFormattedList(false, watchAuxiliaryAttributes));
        } else if (delta.isModify()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("modified. Modified attributes are:\n");
            body.append(modelEvent.getContentAsFormattedList(false, watchAuxiliaryAttributes));
        } else if (delta.isDelete()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("removed.\n");
        }
        body.append("\n");

        if (!event.isSuccess()) {
            body.append("More information about the status of the request was displayed and/or is present in log files.\n\n");
        }

        functions.addRequesterAndChannelInformation(body, event, result);

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(modelContext.debugDump(2));
        }

        return body.toString();
    }

    @Nullable
    private String getFullName(FocusType focus) {
        String fullName;
        if (focus instanceof UserType) {
            fullName = PolyString.getOrig(((UserType) focus).getFullName());
        } else if (focus instanceof AbstractRoleType) {
            fullName = PolyString.getOrig(((AbstractRoleType) focus).getDisplayName());
        } else {
            fullName = "";          // TODO (currently it's not possible to get here)
        }
        return fullName;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
