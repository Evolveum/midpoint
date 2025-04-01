/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleFocalObjectNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This is the "main" notifier that deals with modifications of focal objects i.e. AssignmentHolderType and below.
 */
@Component
public class SimpleFocalObjectNotifier extends AbstractGeneralNotifier<ModelEvent, SimpleFocalObjectNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleFocalObjectNotifier.class);

    @Override
    public @NotNull Class<ModelEvent> getEventType() {
        return ModelEvent.class;
    }

    @Override
    public @NotNull Class<? extends SimpleFocalObjectNotifierType> getEventHandlerConfigurationType() {
        return SimpleFocalObjectNotifierType.class;
    }

    Class<? extends AssignmentHolderType> getFocusClass() {
        return AssignmentHolderType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends SimpleFocalObjectNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        if (!ctx.event().hasFocusOfType(getFocusClass())) {
            LOGGER.trace("{} is not applicable to non-{} related model operations, continuing in the handler chain",
                    getClass().getSimpleName(), getFocusClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(
            ConfigurationItem<? extends SimpleFocalObjectNotifierType> configuration,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {
        if (!ctx.event().hasContentToShow(isWatchAuxiliaryAttributes(configuration.value()))) {
            LOGGER.trace("No modifications to show, skipping the notification");
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleFocalObjectNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) {

        var event = ctx.event();
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
    protected String getBody(ConfigurationItem<? extends SimpleFocalObjectNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends ModelEvent> ctx,
            OperationResult result) throws SchemaException {

        var event = ctx.event();
        String typeName = event.getFocusTypeName();
        String typeNameLower = typeName.toLowerCase();

        boolean techInfo = Boolean.TRUE.equals(configuration.value().isShowTechnicalInformation());

        //noinspection unchecked
        ModelContext<AssignmentHolderType> modelContext = (ModelContext<AssignmentHolderType>) event.getModelContext();
        ModelElementContext<AssignmentHolderType> focusContext = modelContext.getFocusContext();
        PrismObject<AssignmentHolderType> focusObject =
                focusContext.getObjectNew() != null ? focusContext.getObjectNew() : focusContext.getObjectOld();
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

        boolean watchAuxiliaryAttributes = isWatchAuxiliaryAttributes(configuration.value());
        final Task task = ctx.task();
        if (delta.isAdd()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("created with the following data:\n");
            body.append(event.getContentAsFormattedList(watchAuxiliaryAttributes, task, result));
        } else if (delta.isModify()) {
            body.append("The ").append(typeNameLower).append(" record was ").append(attemptedTo).append("modified. Modified attributes are:\n");
            body.append(event.getContentAsFormattedList(watchAuxiliaryAttributes, task, result));
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
