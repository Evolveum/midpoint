/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
