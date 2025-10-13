/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

@ActionType(
        identifier = "certItemAccept",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.accept", icon = "fa fa-check text-success", order = 1),
        button = true)
public class CertItemAcceptAction extends AbstractCertItemDecisionAction {

    public CertItemAcceptAction() {
        super();
    }

    public CertItemAcceptAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected AccessCertificationResponseType getResponse(AccessCertificationWorkItemType certItem) {
        return AccessCertificationResponseType.ACCEPT;
    }

}
