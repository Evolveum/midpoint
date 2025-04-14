/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.task.TaskResultStatus;

public enum GuiTaskResultStatus {

    SUCCESS(TaskResultStatus.SUCCESS, "fa fa-check-circle text-success"),

    ERROR(TaskResultStatus.ERROR, "fa fa-times-circle text-danger"),

    IN_PROGRESS(TaskResultStatus.IN_PROGRESS, "fa fa-pause-circle text-info"),

    NOT_FINISHED(TaskResultStatus.NOT_FINISHED, "fa fa-pause-circle text-info"),

    UNKNOWN(TaskResultStatus.UNKNOWN, "fa fa-question-circle text-secondary");

    public final TaskResultStatus status;

    public String icon;

    GuiTaskResultStatus(TaskResultStatus status, String icon) {
        this.status = status;
        this.icon = icon;
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
}
