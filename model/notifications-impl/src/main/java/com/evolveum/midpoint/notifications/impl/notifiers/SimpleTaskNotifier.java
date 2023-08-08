/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleTaskNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SimpleTaskNotifier extends AbstractGeneralNotifier<TaskEvent, SimpleTaskNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleTaskNotifier.class);

    @Override
    public @NotNull Class<TaskEvent> getEventType() {
        return TaskEvent.class;
    }

    @Override
    public @NotNull Class<SimpleTaskNotifierType> getEventHandlerConfigurationType() {
        return SimpleTaskNotifierType.class;
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleTaskNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends TaskEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        String taskName = PolyString.getOrig(event.getTask().getName());
        if (event.isAdd()) {
            return "Task '" + taskName + "' start notification";
        } else if (event.isDelete()) {
            return "Task '" + taskName + "' finish notification: " + event.getOperationResultStatus();
        } else {
            return "(unknown " + taskName + " operation)";
        }
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleTaskNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends TaskEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        var task = event.getTask();
        var taskName = PolyString.getOrig(task.getName());

        StringBuilder body = new StringBuilder();

        body.append("Notification about task-related operation.\n\n");
        body.append("Task: ").append(taskName).append("\n");
        body.append("Handler: ").append(task.getHandlerUri()).append("\n\n");
        if (event.getTaskRunResult() != null) {
            body.append("Run result status: ").append(event.getTaskRunResult().getRunResultStatus()).append("\n");
        }
        body.append("Status: ").append(event.getOperationResultStatus()).append("\n");
        String message = event.getMessage();
        if (StringUtils.isNotBlank(message)) {
            body.append("Message: ").append(message).append("\n");
        }
        body.append("Progress: ").append(event.getProgress()).append("\n");
        body.append("\n");
        body.append("Notification created on: ").append(new Date()).append("\n\n");

        PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
        if (taskOwner != null) {
            FocusType owner = taskOwner.asObjectable();
            body.append("Task owner: ");
            if (owner instanceof UserType) {
                body.append(((UserType)owner).getFullName()).append(" (").append(owner.getName()).append(")");
            } else {
                body.append(owner.getName());
            }
            body.append("\n");
        }
        body.append("Channel: ").append(event.getChannel()).append("\n\n");

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
