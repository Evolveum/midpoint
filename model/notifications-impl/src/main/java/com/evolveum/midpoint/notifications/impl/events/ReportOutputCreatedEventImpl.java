/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.ActivityEvent;
import com.evolveum.midpoint.notifications.api.events.ReportOutputCreatedEvent;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Signals that a report output has been created.
 *
 * Reasons for the existence of this specific event - i.e. why we don't use generic {@link TaskEvent} or {@link ActivityEvent}:
 * The event itself is quite simple. After it's created, it has all the relevant data ({@link #report}, {@link #reportData}),
 * and generally, it is really easy to use. No analysis of task extension, activity definition or state, or anything else
 * is necessary to work with this event.
 *
 * Moreover, it is possible that in the future, some reports might be created outside tasks/activities.
 */
public class ReportOutputCreatedEventImpl extends BaseEventImpl implements ReportOutputCreatedEvent {

    /** The report definition object. */
    @NotNull private final ReportType report;

    /** The report output (data) object. */
    @NotNull private final ReportDataType reportData;

    /** The activity run that produced the report data. Currently always present, but in the future it may be missing. */
    @Nullable private final AbstractActivityRun<?, ?, ?> activityRun;

    public ReportOutputCreatedEventImpl(
            @NotNull ReportType report,
            @NotNull ReportDataType reportData,
            @Nullable AbstractActivityRun<?, ?, ?> activityRun) {
        this.report = report;
        this.reportData = reportData;
        this.activityRun = activityRun;
    }

    @Override
    public @NotNull ReportType getReport() {
        return report;
    }

    @Override
    public @NotNull ReportDataType getReportData() {
        return reportData;
    }

    @Override
    public @Nullable AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityRun;
    }

    @Override
    public String debugDump(int indent) {
        return null;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        // These events are emitted only if the report output was successfully created.
        return eventStatus == EventStatusType.SUCCESS || eventStatus == EventStatusType.ALSO_SUCCESS;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        // Conceptually, these events mean that "something has been added".
        return eventOperation == EventOperationType.ADD;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return false; // We have no special category for these events.
    }
}
