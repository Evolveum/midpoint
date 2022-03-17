/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.ReportOutputCreatedEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleReportNotifierType;

/**
 * Basic notifier for {@link ReportOutputCreatedEvent} instances.
 */
@Component
public class SimpleReportNotifier extends AbstractGeneralNotifier<ReportOutputCreatedEvent, SimpleReportNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReportNotifier.class);

    @Override
    public Class<ReportOutputCreatedEvent> getEventType() {
        return ReportOutputCreatedEvent.class;
    }

    @Override
    public Class<SimpleReportNotifierType> getEventHandlerConfigurationType() {
        return SimpleReportNotifierType.class;
    }

    @Override
    protected String getSubject(
            @NotNull ReportOutputCreatedEvent event,
            SimpleReportNotifierType configuration,
            String transportName,
            Task task,
            OperationResult result) {
        return "Report " + event.getReportName() + " was created";
    }

    @Override
    protected List<NotificationMessageAttachmentType> getAttachment(
            ReportOutputCreatedEvent event,
            SimpleReportNotifierType configuration,
            String transportName,
            Task task,
            OperationResult result) {
        return List.of(
                new NotificationMessageAttachmentType()
                        .contentType(event.getContentType())
                        .contentFromFile(event.getFilePath()));
    }

    @Override
    protected String getBody(
            ReportOutputCreatedEvent event,
            SimpleReportNotifierType configuration,
            String transportName,
            Task task,
            OperationResult result) throws SchemaException {

        return "Notification about creating of report.\n\n"
                + "Report: " + event.getReportName() + "\n\n"
                + "You can see report output in attachment." + "\n";
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
