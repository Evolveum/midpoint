/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.Collections;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.button.ReloadableButton;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

public abstract class CampaignActionButton extends ReloadableButton {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CampaignActionButton.class.getName() + ".";
    private static final String OPERATION_CERTIFICATION_ITEM_ACTION = DOT_CLASS + "certificationItemAction";

    private LoadableDetachableModel<AccessCertificationCampaignType> campaignModel;

    public CampaignActionButton(String id, PageBase pageBase,
            LoadableDetachableModel<AccessCertificationCampaignType> campaignModel,
            LoadableDetachableModel<String> buttonLabelModel, String runningTaskOid) {
        super(id, pageBase, buttonLabelModel, runningTaskOid);
        this.campaignModel = campaignModel;
    }

    @Override
    protected String getCreatedTaskOid(AjaxRequestTarget target) {
        AccessCertificationCampaignType campaign = campaignModel.getObject();
        OperationResult result = new OperationResult(OPERATION_CERTIFICATION_ITEM_ACTION);
        CampaignStateHelper.CampaignAction action = new CampaignStateHelper(campaign).getNextAction();
        CampaignProcessingHelper.campaignActionConfirmed(Collections.singletonList(campaign), action,
                pageBase, target, result);
        OpResult opResult = OpResult.getOpResult(pageBase, result);
        return opResult.getBackgroundTaskOid();
    }

    @Override
    protected IModel<String> getConfirmMessage() {
        return getActionConfirmationMessageModel();
    }

    @Override
    protected String getIconCssClass() {
        return getActionButtonCssModel().getObject();
    }

    private IModel<String> getActionConfirmationMessageModel() {
        AccessCertificationCampaignType campaign = campaignModel.getObject();
        CampaignStateHelper.CampaignAction action = new CampaignStateHelper(campaign).getNextAction();
        String messageKey = action.getConfirmation().getSingleConfirmationMessageKey();
        String campaignNameTranslated = LocalizationUtil.translatePolyString(campaign.getName());
        return pageBase.createStringResource(messageKey, campaignNameTranslated);
    }

    @Override
    protected String getButtonCssClass() {
        return "";
    }

    protected LoadableDetachableModel<String> getActionButtonCssModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                AccessCertificationCampaignType campaign = campaignModel.getObject();
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(campaign);
                return campaignStateHelper.getNextAction().getActionIcon().getCssClass();
            }
        };
    }


}
