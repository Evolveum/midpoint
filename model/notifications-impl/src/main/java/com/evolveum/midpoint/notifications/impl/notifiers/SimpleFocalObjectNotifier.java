/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Date;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
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

/**
 * This is the "main" notifier that deals with modifications of focal objects i.e. AssignmentHolderType and below.
 */
@Component
public class SimpleFocalObjectNotifier extends AbstractGeneralNotifier<ModelEvent, SimpleFocalObjectNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleFocalObjectNotifier.class);

    @Override
    public Class<ModelEvent> getEventType() {
        return ModelEvent.class;
    }

    @Override
    public Class<? extends SimpleFocalObjectNotifierType> getEventHandlerConfigurationType() {
        return SimpleFocalObjectNotifierType.class;
    }

    Class<? extends AssignmentHolderType> getFocusClass() {
        return AssignmentHolderType.class;
    }

    @Override
    protected boolean quickCheckApplicability(ModelEvent event, SimpleFocalObjectNotifierType configuration, OperationResult result) {
        event.getFocusContext();
        if (!event.hasFocusOfType(getFocusClass())) {
            LOGGER.trace("{} is not applicable to non-{} related model operations, continuing in the handler chain",
                    getClass().getSimpleName(), getFocusClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(ModelEvent event, SimpleFocalObjectNotifierType configuration, OperationResult result) {
        if (!event.hasContentToShow(isWatchAuxiliaryAttributes(configuration))) {
            LOGGER.trace("No modifications to show, skipping the notification");
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getSubject(ModelEvent event, SimpleFocalObjectNotifierType configuration, String transport,
            Task task, OperationResult result) {

        String typeName = event.getFocusTypeName();

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
    protected String getBody(ModelEvent event, SimpleFocalObjectNotifierType configuration, String transport,
            Task task, OperationResult result) throws SchemaException {

        String typeName = event.getFocusTypeName();
        String typeNameLower = typeName.toLowerCase();

        boolean techInfo = Boolean.TRUE.equals(configuration.isShowTechnicalInformation());

        //noinspection unchecked
        ModelContext<AssignmentHolderType> modelContext = (ModelContext<AssignmentHolderType>) event.getModelContext();
        ModelElementContext<AssignmentHolderType> focusContext = modelContext.getFocusContext();
        PrismObject<AssignmentHolderType> focusObject = focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
        AssignmentHolderType focus = focusObject.asObjectable();
        String oid = focusContext.getOid();

        String fullName = emptyIfNull(getFullName(focus));

        ObjectDelta<AssignmentHolderType> delta = ObjectDeltaCollectionsUtil.summarize(event.getFocusDeltas());

        StringBuilder body = new StringBuilder();

        String status = event.getStatusAsText();
        String attemptedTo = event.isSuccess() ? "" : "(attempted to be) ";

        body.append("Notification about ").append(typeNameLower).append("-related operation (status: ").append(status).append(")\n\n");
        body.append(typeName).append(": ").append(fullName).append(" (").append(focus.getName()).append(", oid ").append(oid).append(")\n");
        body.append("Notification created on: ").append(new Date()).append("\n\n");

        final boolean watchAuxiliaryAttributes = isWatchAuxiliaryAttributes(configuration);
        if (delta.isAdd()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("created with the following data:\n");
            body.append(event.getContentAsFormattedList( watchAuxiliaryAttributes));
        } else if (delta.isModify()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("modified. Modified attributes are:\n");
            body.append(event.getContentAsFormattedList(watchAuxiliaryAttributes));
        } else if (delta.isDelete()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("removed.\n");
        }
        body.append("\n");

        if (!event.isSuccess()) {
            body.append("More information about the status of the request was displayed and/or is present in log files.\n\n");
        }

        addRequesterAndChannelInformation(body, event, result);

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(modelContext.debugDump(2));
        }

        return body.toString();
    }

    @Nullable
    private String getFullName(AssignmentHolderType focus) {
        String fullName;
        if (focus instanceof UserType) {
            fullName = PolyString.getOrig(((UserType) focus).getFullName());
        } else if (focus instanceof AbstractRoleType) {
            fullName = PolyString.getOrig(((AbstractRoleType) focus).getDisplayName());
        } else {
            fullName = "";          // TODO
        }
        return fullName;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
