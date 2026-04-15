/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.schema.util.task.TaskResultStatus;

public enum GuiTaskResultStatus {

    SUCCESS(
            TaskResultStatus.SUCCESS,
            "fa fa-check-circle text-success",
            "GuiTaskResultStatus.success",
            Badge.State.SUCCESS),

    ERROR(
            TaskResultStatus.ERROR,
            "fa fa-times-circle text-danger",
            "GuiTaskResultStatus.error",
            Badge.State.DANGER),

    IN_PROGRESS(
            TaskResultStatus.IN_PROGRESS,
            "fa fa-spinner fa-spin text-info",
            "GuiTaskResultStatus.inProgress",
            Badge.State.INFO),

    NOT_FINISHED(
            TaskResultStatus.NOT_FINISHED,
            "fa fa-clock text-info",
            "GuiTaskResultStatus.notFinished",
            Badge.State.INFO),

    UNKNOWN(
            TaskResultStatus.UNKNOWN,
            "fa fa-question-circle text-secondary",
            "GuiTaskResultStatus.unknown",
            Badge.State.SECONDARY);

    public final TaskResultStatus status;
    public final String icon;
    final String labelKey;
    final Badge.State badgeState;

    GuiTaskResultStatus(TaskResultStatus status,
            String iconCss,
            String labelKey,
            Badge.State badgeState) {
        this.status = status;
        this.icon = iconCss;
        this.labelKey = labelKey;
        this.badgeState = badgeState;
    }

    public static GuiTaskResultStatus fromTaskResultStatus(TaskResultStatus status) {
        if (status == null) {
            return null;
        }

        for (GuiTaskResultStatus guiStatus : values()) {
            if (guiStatus.status == status) {
                return guiStatus;
            }
        }

        throw new IllegalArgumentException("Unknown status " + status);
    }

    public TaskResultStatus getStatus() {
        return status;
    }

    public String getIconCss() {
        return icon;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public Badge.State getBadgeState() {
        return badgeState;
    }
}
