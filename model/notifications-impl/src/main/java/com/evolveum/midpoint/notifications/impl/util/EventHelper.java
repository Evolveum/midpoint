/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.util;

import com.evolveum.midpoint.schema.config.EventHandlerConfigItem;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class EventHelper {

    private static final Trace LOGGER = TraceManager.getTrace(EventHelper.class);

    private static final String OP_PROCESS_EVENT = EventHelper.class.getName() + ".processEvent";

    @Autowired private NotificationManager notificationManager;

    public void processEvent(Event event, Task task, OperationResult parentResult) {
        processEvent(event, null, null, task, parentResult);
    }

    public void processEvent(
            Event event,
            EventHandlerConfigItem adHocEventHandler,
            ExpressionProfile adHocEventExpressionProfile,
            Task task,
            OperationResult parentResult) {
        // It would be better if we could go without creating a subresult. However, we need to record
        // the exception, so it should be done so.
        OperationResult result = parentResult.subresult(OP_PROCESS_EVENT)
                .setMinor()
                .build();
        try {
            notificationManager.processEvent(
                    event,
                    adHocEventHandler,
                    adHocEventExpressionProfile,
                    task,
                    result);
        } catch (RuntimeException e) {
            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
        } finally {
            result.close();
        }
    }
}
