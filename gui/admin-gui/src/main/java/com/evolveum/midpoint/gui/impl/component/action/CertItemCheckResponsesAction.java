/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertResponseDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import java.util.List;

@ActionType(
        identifier = "certItemHistory",
        applicableForType = AccessCertificationWorkItemType.class,
        bulkAction = false,
        display = @PanelDisplay(label = "CertItemCheckResponsesAction.label", icon = "fa fa-check text-success", order = 10))
public class CertItemCheckResponsesAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    public CertItemCheckResponsesAction() {
        super();
    }

    public CertItemCheckResponsesAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected void executeAction(List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        if (workItems.size() != 1) {
            return;
        }

        AccessCertificationWorkItemType workItem = workItems.get(0);
        AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(workItem);
        if (certCase == null) {
            return;
        }
        AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
        if (campaign == null) {
            return;
        }
        int currentStageNumber = campaign.getStageNumber();

        PrismContainerValueWrapper<AccessCertificationCaseType> caseWrapper =
                new PrismContainerValueWrapperImpl<>(null, certCase.asPrismContainerValue(), ValueStatus.NOT_CHANGED);
            CertResponseDetailsPanel panel = new CertResponseDetailsPanel(pageBase.getMainPopupBodyId(),
                    Model.of(caseWrapper), currentStageNumber);
            pageBase.showMainPopup(panel, target);
    }

}
