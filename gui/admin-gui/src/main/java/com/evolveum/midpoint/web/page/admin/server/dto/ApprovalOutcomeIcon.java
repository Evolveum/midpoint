/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

/**
 *  @author mederly
 */
public enum ApprovalOutcomeIcon {

	// TODO move 'fa-fw fa-lg' to style constants as well?
    UNKNOWN("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_UNKNOWN_COLORED, "MyRequestsPanel.unknown"),
    APPROVED("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED, "MyRequestsPanel.approved"),
    REJECTED("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED, "MyRequestsPanel.rejected"),
    SKIPPED("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_SKIPPED_COLORED, "MyRequestsPanel.skipped"),
    IN_PROGRESS("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED, "MyRequestsPanel.inProgress"),
    FUTURE("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_FUTURE_COLORED, "MyRequestsPanel.future"),
    CANCELLED("fa-fw fa-lg " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_CANCELLED_COLORED, "MyRequestsPanel.cancelled"),
    EMPTY("", "");      // to be used for cases when it won't be really displayed; reconsider this

    private String icon;
    private String title;

    ApprovalOutcomeIcon(String icon, String title) {
        this.icon = icon;
        this.title = title;
    }

    public String getIcon(){
        return icon;
    }

    public String getTitle() {
        return title;
    }
}
