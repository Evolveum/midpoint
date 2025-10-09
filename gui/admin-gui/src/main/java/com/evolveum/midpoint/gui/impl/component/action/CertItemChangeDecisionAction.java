/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

@ActionType(
        identifier = "certItemChangeDecision",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "CertificationItemsPanel.action.changeDecision", order = 7))
public class CertItemChangeDecisionAction extends CertItemResolveAction {

    public CertItemChangeDecisionAction() {
        super();
    }

    public CertItemChangeDecisionAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected boolean isVisibleForRow(AccessCertificationWorkItemType certItem) {
        String itemResponse = certItem != null && certItem.getOutput() != null ? certItem.getOutput().getOutcome() : null;
        return certItem == null || itemResponse != null;
    }
}
