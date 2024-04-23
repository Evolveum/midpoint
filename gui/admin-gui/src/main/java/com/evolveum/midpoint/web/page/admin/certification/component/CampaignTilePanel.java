/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class CampaignTilePanel extends BasePanel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SELECT_TILE_CHECKBOX = "selectTileCheckbox";
    private static final String ID_STATUS = "status";
    private static final String ID_MENU = "menu";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_PROGRESS_BAR = "progressBar";
    private static final String ID_DEADLINE = "deadline";
    private static final String ID_STAGE = "stage";
    private static final String ID_ITERATION = "iteration";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";

    public CampaignTilePanel(String id, IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        add(AttributeAppender.append("class",
                "catalog-tile-panel d-flex flex-column align-items-center bordered p-4"));

        setOutputMarkupId(true);

        CheckBoxPanel selectTileCheckbox = new CheckBoxPanel(ID_SELECT_TILE_CHECKBOX, getSelectedModel());
        selectTileCheckbox.setOutputMarkupId(true);
        add(selectTileCheckbox);

        Label status = new Label(ID_STATUS, getStatusModel());
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
    private IModel<String> getStatusModel() {
        return Model.of(LocalizationUtil.translateEnum(getCampaign().getState()));
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
}
