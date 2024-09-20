/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class CertCampaignSummaryPanel extends ObjectVerticalSummaryPanel<AccessCertificationCampaignType> {

    public CertCampaignSummaryPanel(String id, IModel<AccessCertificationCampaignType> model) {
        super(id, model);
    }

    @Override
    protected IModel<String> getTitleForNewObject(AccessCertificationCampaignType modelObject) {
        return () -> LocalizationUtil.translate("CertCampaignSummaryPanel.new");
    }

    @Override
    protected @NotNull IModel<List<DetailsTableItem>> createDetailsItems() {
        return new LoadableModel<>(false) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> list = new ArrayList<>();
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.progress"),
                        () -> "") {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new ProgressBarPanel(id, CertMiscUtil.createCampaignCasesProgressBarModel(
                                getModelObject(), null, getPageBase()));
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertDefinition.numberOfStages"),
                        () -> "" + getModelObject().getStageDefinition().size()));
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(getModelObject());
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.currentState"),
                        null) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        BadgePanel status = new BadgePanel(id, createBadgeModel());
                        status.setOutputMarkupId(true);
                        return status;
                    }

                    private IModel<Badge> createBadgeModel() {
                        Badge badge = new Badge("colored-form-info", resolveCurrentStateName());
                        return Model.of(badge);
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.table.deadline"),
                        null) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new DeadlinePanel(id, getDeadlineModel());
                    }

                    private IModel<XMLGregorianCalendar> getDeadlineModel() {
                        return () -> stage != null ? stage.getDeadline() : null;
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.owner"),
                        () -> "") {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new LinkedReferencePanel<>(id, Model.of(getModelObject().getOwnerRef())) {

                            @Override
                            protected String getAdditionalCssStyle() {
                                return "";
                            }
                        };
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.iteration"),
                        () -> "" + CertCampaignTypeUtil.norm(getModelObject().getIteration())));

                return list;
            }
        };
    }

    private String resolveCurrentStateName() {
        int stageNumber = getModelObject().getStageNumber();
        AccessCertificationCampaignStateType state = getModelObject().getState();
        switch (state) {
            case CREATED:
            case IN_REMEDIATION:
            case CLOSED:
                return LocalizationUtil.translateEnum(state);
            case IN_REVIEW_STAGE:
            case REVIEW_STAGE_DONE:
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(getModelObject());
                String stageName = stage != null ? stage.getName() : null;
                if (stageName != null) {
                    String key = WebComponentUtil.createEnumResourceKey(state) + "_FULL";
                    return LocalizationUtil.translate(key, new Object[]{stageNumber, stageName});
                } else {
                    String key = WebComponentUtil.createEnumResourceKey(state);
                    return LocalizationUtil.translate(key) + " " + stageNumber;
                }
            default:
                return null;        // todo warning/error?
        }
    }
}
