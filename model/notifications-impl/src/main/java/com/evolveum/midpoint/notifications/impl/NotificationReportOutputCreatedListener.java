/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.ReportOutputCreatedEvent;
import com.evolveum.midpoint.notifications.impl.events.ReportOutputCreatedEventImpl;
import com.evolveum.midpoint.repo.common.activity.ReportOutputCreatedListener;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * Creates {@link ReportOutputCreatedEvent} instances.
 */
@Component
public class NotificationReportOutputCreatedListener implements ReportOutputCreatedListener {

    @Autowired private NotificationManager notificationManager;

    @Override
    public void onReportOutputCreated(
            @Nullable AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull ReportType report,
            @NotNull ReportDataType reportOutput,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ReportOutputCreatedEventImpl event = new ReportOutputCreatedEventImpl(report, reportOutput, activityRun);
        event.setRequesterAndRequesteeAsTaskOwner(task, result);
        notificationManager.processEvent(event, task, result);
    }
}
