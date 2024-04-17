/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

public enum ApprovalOutcomeIcon {

    UNKNOWN(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_UNKNOWN_COLORED, "MyRequestsPanel.unknown"),
    APPROVED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED, "MyRequestsPanel.approved"),
    REJECTED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "MyRequestsPanel.rejected"),
    SKIPPED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_SKIPPED_COLORED, "MyRequestsPanel.skipped"),
    FORWARDED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FORWARDED_COLORED, "MyRequestsPanel.forwarded"),
    IN_PROGRESS(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED, "MyRequestsPanel.inProgress"),
    FUTURE(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FUTURE_COLORED, "MyRequestsPanel.future"),
    CANCELLED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_CANCELLED_COLORED, "MyRequestsPanel.cancelled"),
    UNRECOGNIZED(GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_UNRECOGNIZED_COLORED, "MyRequestsPanel.unrecognized"),
    EMPTY("", "");      // to be used for cases when it won't be really displayed; reconsider this

    private String icon;
    private String title;

    ApprovalOutcomeIcon(String icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }
}
