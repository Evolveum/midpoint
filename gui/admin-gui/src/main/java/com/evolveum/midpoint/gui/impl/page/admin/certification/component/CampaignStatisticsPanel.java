/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@PanelType(name = "campaignStatistics")
@PanelInstance(identifier = "campaignStatistics",
        applicableForType = AccessCertificationCampaignType.class,
        display = @PanelDisplay(label = "CampaignStatisticsPanel.label",
                icon = GuiStyleConstants.CLASS_TASK_STATISTICS_ICON, order = 10))
public class CampaignStatisticsPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CampaignStatisticsPanel.class);

    private static final String DOT_CLASS = CampaignStatisticsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CERT_ITEMS = DOT_CLASS + "loadCertItems";
    private static final String OPERATION_REVIEWER = DOT_CLASS + "loadReviewer";

    private static final String ID_RELATED_REPORTS = "relatedTasks";
    private static final String ID_REVIEWERS_PANEL = "reviewersPanel";

    //todo?
    private static final int MAX_REVIEWERS = 5;
    private int realreviewersCount;

    public CampaignStatisticsPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        add(initReviewersPanel(ID_REVIEWERS_PANEL, true));
        add(new RelatedTasksPanel(ID_RELATED_REPORTS, getObjectDetailsModels()));
    }

    private StatisticListBoxPanel<ObjectReferenceType> initReviewersPanel(String id, boolean allowViewAll) {
        IModel<List<StatisticBoxDto<ObjectReferenceType>>> reviewersModel = getReviewersModel(allowViewAll);
        return new StatisticListBoxPanel<>(id,
                getReviewersPanelDisplayModel(reviewersModel.getObject().size()), reviewersModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isViewAllAllowed() {
                return reviewersCountExceedsLimit() && allowViewAll;
            }

            @Override
            protected void viewAllActionPerformed(AjaxRequestTarget target) {
                showAllReviewersPerformed(target);
            }

            @Override
            protected Component createRightSideBoxComponent(String id, StatisticBoxDto<ObjectReferenceType> statisticObject) {
                ObjectReferenceType reviewerRef = statisticObject.getStatisticObject();
                DoughnutChartConfiguration chartConfig = getReviewerProgressChartConfig(reviewerRef);

                ChartJsPanel<ChartConfiguration> chartPanel = new ChartJsPanel<>(id, Model.of(chartConfig)) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.setName("canvas");
                        super.onComponentTag(tag);
                    }
                };
                chartPanel.setOutputMarkupId(true);
                return chartPanel;
            }
        };
    }

    private DoughnutChartConfiguration getReviewerProgressChartConfig(ObjectReferenceType reviewerRef) {
        PrismObject<FocusType> reviewer = WebModelServiceUtils.loadObject(reviewerRef, getPageBase());
        if (reviewer == null) {
            return null;
        }
        AccessCertificationCampaignType campaign = getObjectDetailsModels().getObjectType();
        MidPointPrincipal principal = MidPointPrincipal.create(reviewer.asObjectable());
        return CertMiscUtil.createDoughnutChartConfigForCampaigns(
                Collections.singletonList(campaign.getOid()), principal, getPageBase());
    }

    private LoadableDetachableModel<List<StatisticBoxDto<ObjectReferenceType>>> getReviewersModel(boolean restricted) {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<StatisticBoxDto<ObjectReferenceType>> load() {
                List<StatisticBoxDto<ObjectReferenceType>> list = new ArrayList<>();
                List<ObjectReferenceType> reviewers = loadReviewers();
                if (restricted) {
                    realreviewersCount = reviewers.size();
                    reviewers.stream().limit(MAX_REVIEWERS).forEach(r -> list.add(createReviewerStatisticBoxDto(r)));
                } else {
                    reviewers.forEach(r -> list.add(createReviewerStatisticBoxDto(r)));
                }
                return list;
            }
        };
    }

    private boolean reviewersCountExceedsLimit() {
        return realreviewersCount > MAX_REVIEWERS;
    }

    private List<ObjectReferenceType> loadReviewers() {
        OperationResult result = new OperationResult(OPERATION_LOAD_CERT_ITEMS);
        AccessCertificationCampaignType campaign = getObjectDetailsModels().getObjectType();
        Integer iteration = CertCampaignTypeUtil.norm(campaign.getIteration());
        Integer stage = CertCampaignTypeUtil.accountForClosingStates(campaign.getStageNumber(), campaign.getState());
        ObjectQuery query = getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                .ownerId(campaign.getOid())
                .and()
                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER).eq(stage)
                .and()
                .item(AccessCertificationWorkItemType.F_ITERATION).eq(iteration)
                .build();
        List<AccessCertificationWorkItemType> certItems = WebModelServiceUtils.searchContainers(
                AccessCertificationWorkItemType.class, query, null, result, getPageBase());
        return collectReviewers(certItems);
    }

    private List<ObjectReferenceType> collectReviewers(List<AccessCertificationWorkItemType> certItems) {
        List<ObjectReferenceType> reviewersList = new ArrayList<>();
        certItems.forEach(certItem -> certItem.getAssigneeRef()
                .forEach(assignee -> {
                    if (!alreadyExistInList(reviewersList, assignee)) {
                        reviewersList.add(assignee);
                    }
                }));
        return reviewersList;
    }

    private boolean alreadyExistInList(List<ObjectReferenceType> reviewersList, ObjectReferenceType ref) {
        return reviewersList
                .stream()
                .anyMatch(r -> r.getOid().equals(ref.getOid()));
    }

    private StatisticBoxDto<ObjectReferenceType> createReviewerStatisticBoxDto(ObjectReferenceType ref) {
        OperationResult result = new OperationResult(OPERATION_REVIEWER);
        Task task = getPageBase().createSimpleTask(OPERATION_REVIEWER);
        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                        .item(FocusType.F_JPEG_PHOTO).retrieve()
                        .build();
        PrismObject<FocusType> object = WebModelServiceUtils.loadObject(FocusType.class, ref.getOid(), options,
                getPageBase(), task, result);
        String name = WebComponentUtil.getName(object);
        String displayName = WebComponentUtil.getDisplayName(object);
        DisplayType displayType = new DisplayType()
                .label(name)
                .help(displayName)
                .icon(new IconType().cssClass("fa fa-user"));
        IResource userPhoto = WebComponentUtil.createJpegPhotoResource(object);
        return new StatisticBoxDto<>(Model.of(displayType), Model.of(userPhoto)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public ObjectReferenceType getStatisticObject() {
                return ref;
            }

        };
    }

    private IModel<DisplayType> getReviewersPanelDisplayModel(int reviewersCount) {
        String reviewersCountKey = reviewersCount == 1 ? "CampaignStatisticsPanel.reviewersPanel.singleReviewerCount" :
                "CampaignStatisticsPanel.reviewersPanel.reviewersCount";
        return () -> new DisplayType()
                .label("CampaignStatisticsPanel.reviewersPanel.title")
                .help(createStringResource(reviewersCountKey, reviewersCount).getString());
    }

    private void showAllReviewersPerformed(AjaxRequestTarget target) {
        getPageBase().showMainPopup(initReviewersPanel(getPageBase().getMainPopupBodyId(), false), target);
    }
}
