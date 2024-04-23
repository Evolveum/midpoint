/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDto;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class CampaignTilePanel extends BasePanel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> {

    @Serial private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(CampaignTilePanel.class);

    private static final String ID_SELECT_TILE_CHECKBOX = "selectTileCheckbox";
    private static final String ID_STATUS = "status";
    private static final String ID_MENU = "menu";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_PROGRESS_BAR = "progressBar";
    private static final String ID_DEADLINE = "deadline";
    private static final String ID_STAGE = "stage";
    private static final String ID_ITERATION = "iteration";
    private static final String ID_ACTION_BUTTON = "actionButton";
    private static final String ID_ACTION_BUTTON_LABEL = "actionButtonLabel";
    private static final String ID_ACTION_BUTTON_ICON = "actionButtonIcon";

    CampaignStateHelper campaignStateHelper;

    public CampaignTilePanel(String id, IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        campaignStateHelper = new CampaignStateHelper(getCampaign().getState());

        initLayout();
    }

    protected void initLayout() {
        add(AttributeAppender.append("class",
                "campaign-tile-panel catalog-tile-panel d-flex flex-column align-items-center bordered p-4"));

        setOutputMarkupId(true);

        CheckBoxPanel selectTileCheckbox = new CheckBoxPanel(ID_SELECT_TILE_CHECKBOX, getSelectedModel());
        selectTileCheckbox.setOutputMarkupId(true);
        add(selectTileCheckbox);

        BadgePanel status = new BadgePanel(ID_STATUS, getStatusModel());
        status.setOutputMarkupId(true);
        add(status);

        AjaxButton menu = new AjaxButton(ID_MENU) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // TODO implement
            }
        };
        menu.setOutputMarkupId(true);
        add(menu);

        Label title = new Label(ID_TITLE, getTitleModel());
        title.setOutputMarkupId(true);
        add(title);

        Label description = new Label(ID_DESCRIPTION, Model.of(getModelObject().getDescription()));
        description.setOutputMarkupId(true);
        add(description);

        ProgressBarPanel progressBar = new ProgressBarPanel(ID_PROGRESS_BAR, createProgressBarModel());
        progressBar.setOutputMarkupId(true);
        add(progressBar);

        Label deadline = new Label(ID_DEADLINE, getDeadlineModel());
        deadline.setOutputMarkupId(true);
        add(deadline);

        Label stage = new Label(ID_STAGE, getStageModel());
        stage.setOutputMarkupId(true);
        add(stage);

        Label iteration = new Label(ID_ITERATION, getIterationModel());
        iteration.setOutputMarkupId(true);
        add(iteration);

        AjaxLink<Void> actionButton = new AjaxLink<>(ID_ACTION_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                campaignActionPerformed(target);
            }
        };
        actionButton.add(AttributeModifier.append("class", campaignStateHelper.getNextAction().getActionCssClass()));
        actionButton.setOutputMarkupId(true);
        add(actionButton);

        Label actionButtonLabel = new Label(ID_ACTION_BUTTON_LABEL,
                createStringResource(campaignStateHelper.getNextAction().getActionLabelKey()));
        actionButtonLabel.setOutputMarkupId(true);
        actionButton.add(actionButtonLabel);

        WebMarkupContainer actionButtonIcon = new WebMarkupContainer(ID_ACTION_BUTTON_ICON);
        actionButtonIcon.add(AttributeModifier.append("class", campaignStateHelper.getNextAction().getActionIcon().getCssClass()));
        actionButtonIcon.setOutputMarkupId(true);
        actionButton.add(actionButtonIcon);


    }

    private IModel<Boolean> getSelectedModel() {
        return new IModel<>() {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            public Boolean getObject() {
                return getModelObject().isSelected();
            }

            @Override
            public void setObject(Boolean object) {
                getModelObject().setSelected(object);
            }
        };
    }
    private IModel<Badge> getStatusModel() {
        return Model.of(campaignStateHelper.createBadge());
    }

    private IModel<String> getTitleModel() {
        return Model.of(WebComponentUtil.getName(getCampaign()));
    }

    private AccessCertificationCampaignType getCampaign() {
        return getModelObject().getValue().getValue();
    }

    protected @NotNull IModel<List<ProgressBar>> createProgressBarModel() {
        return () -> {
            AccessCertificationCampaignType campaign = getCampaign();
            float completed = CertCampaignTypeUtil.getCasesCompletedPercentageAllStagesAllIterations(campaign);

            ProgressBar progressBar = new ProgressBar(completed, ProgressBar.State.INFO);
            return Collections.singletonList(progressBar);
        };
    }

    private IModel<String> getDeadlineModel() {
        return Model.of(WebComponentUtil.formatDate(getCampaign().getEndTimestamp()));
    }

    private IModel<String> getStageModel() {
        int stageNumber = getCampaign().getStageNumber();
        int numberOfStages = CertCampaignTypeUtil.getNumberOfStages(getCampaign());
        return Model.of(stageNumber + "/" + numberOfStages);
    }

    private IModel<String> getIterationModel() {
        return Model.of("" + CertCampaignTypeUtil.norm(getCampaign().getIteration()));
    }

    //todo unify with PageCertCampaigns menu actions
    private void campaignActionPerformed(AjaxRequestTarget target) {
        int processed = 0;
        AccessCertificationService acs = getPageBase().getCertificationService();

        CampaignStateHelper.CampaignAction action = campaignStateHelper.getNextAction();
        String operationName = LocalizationUtil.translate(action.getActionLabelKey());

        OperationResult result = new OperationResult(operationName);
        try {
            Task task = getPageBase().createSimpleTask(operationName);
            if (CampaignStateHelper.CampaignAction.START_CAMPAIGN.equals(action)) {
//                if (campaign.getState() == AccessCertificationCampaignStateType.CREATED) {
                    acs.openNextStage(getCampaign().getOid(), task, result);
                    processed++;
//                }
            } else if (CampaignStateHelper.CampaignAction.CLOSE_CAMPAIGN.equals(operationName)) {
//                if (campaign.getState() != AccessCertificationCampaignStateType.CLOSED) {
                    acs.closeCampaign(getCampaign().getOid(), task, result);
                    processed++;
//                }
            } else if (CampaignStateHelper.CampaignAction.REITERATE_CAMPAIGN.equals(operationName)) {
                //todo
//                if (item.isReiterable()) {
                    acs.reiterateCampaign(getCampaign().getOid(), task, result);
                    processed++;
//                }
            } else {
                throw new IllegalStateException("Unknown action: " + operationName);
            }
        } catch (Exception ex) {
            result.recordPartialError(createStringResource(
                    "PageCertCampaigns.message.actOnCampaignsPerformed.partialError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process campaign", ex);
        }

        if (processed == 0) {
            warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource(
                    "PageCertCampaigns.message.actOnCampaignsPerformed.success", processed).getString());
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add(this);
    }

}
