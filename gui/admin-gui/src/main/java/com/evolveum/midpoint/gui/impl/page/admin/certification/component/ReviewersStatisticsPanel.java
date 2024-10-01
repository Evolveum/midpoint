/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.WidgetRmChartComponent;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;

import java.io.Serial;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

import static com.evolveum.midpoint.util.MiscUtil.or0;

public class ReviewersStatisticsPanel extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ReviewersStatisticsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CERT_ITEMS = DOT_CLASS + "loadCertItems";
    private static final String OPERATION_REVIEWER = DOT_CLASS + "loadReviewer";

    private static final String ID_REVIEWERS = "reviewers";

    //todo?
    private static final int MAX_REVIEWERS = 5;
    private int realReviewersCount;
    private CertificationDetailsModel model;
    //as a default, sorting of the reviewers is done by the percentage of not decided items
    //can be switched to the number of not decided items
    private IModel<Boolean> percentageSortingModel = Model.of(true);
    private StatisticListBoxPanel<ObjectReferenceType> reviewersPopupPanel;

    private List<StatisticBoxDto<ObjectReferenceType>> sortedReviewerList = new ArrayList<>();
    public ReviewersStatisticsPanel(String id, CertificationDetailsModel model) {
        super(id);
        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        add(initReviewersPanel(ID_REVIEWERS, true));
    }

    private StatisticListBoxPanel<ObjectReferenceType> initReviewersPanel(String id, boolean allowViewAll) {
        IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersStatisticModel = getReviewersStatisticModel();
        LoadableDetachableModel<List<StatisticBoxDto<ObjectReferenceType>>> sortedReviewersModel =
                getSortedReviewersModel(reviewersStatisticModel, allowViewAll);
        StatisticListBoxPanel<ObjectReferenceType> panel = new StatisticListBoxPanel<>(id,
                getReviewersPanelDisplayModel(sortedReviewersModel.getObject().size()), sortedReviewersModel) {
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
                DoughnutChartConfiguration chartConfig = getReviewerProgressChartConfig(reviewerRef, reviewersStatisticModel);

                ChartedHeaderDto<DoughnutChartConfiguration> infoDto = new ChartedHeaderDto<>(chartConfig,
                        createStatisticBoxLabel(statisticObject.getStatisticObject(), reviewersStatisticModel), "",
                        createPercentageLabel(statisticObject.getStatisticObject(), reviewersStatisticModel));
                WidgetRmChartComponent<DoughnutChartConfiguration> chartComponent = new WidgetRmChartComponent<>(id,
                        Model.of(new DisplayType()), Model.of(infoDto));
                chartComponent.setOutputMarkupId(true);
                chartComponent.add(AttributeAppender.append(CLASS_CSS,"col-auto p-0"));
                chartComponent.add(AttributeAppender.append("style","min-width: 210px;"));
                return chartComponent;

            }

            @Override
            protected boolean isLabelClickable() {
                return true;
            }

            @Override
            protected Component createRightSideHeaderComponent(String id) {
                IModel<List<Toggle<Boolean>>> items = new LoadableModel<>(false) {

                    @Override
                    protected List<Toggle<Boolean>> load() {

                        List<Toggle<Boolean>> list = new ArrayList<>();

                        Toggle<Boolean> percentage = new Toggle<>("fa fa-solid fa-percent", "",
                                "ReviewersStatisticsPanel.toggle.sortByPercentage");
                        percentage.setActive(Boolean.TRUE.equals(percentageSortingModel.getObject()));
                        percentage.setValue(true);
                        list.add(percentage);

                        Toggle<Boolean> countable = new Toggle<>("fa fa-solid fa-arrow-down-9-1", "",
                                "ReviewersStatisticsPanel.toggle.sortByCount");
                        countable.setActive(Boolean.FALSE.equals(percentageSortingModel.getObject()));
                        countable.setValue(false);
                        list.add(countable);

                        return list;
                    }
                };
                return new TogglePanel<>(id, items) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<Boolean>> item) {
                        super.itemSelected(target, item);
                        percentageSortingModel.setObject(item.getObject().getValue());

                        target.add(ReviewersStatisticsPanel.this);
                        if (reviewersPopupPanel != null && reviewersPopupPanel.isVisible()) {
                            target.add(reviewersPopupPanel);
                        }
                    }
                };
            }
        };
        return panel;
    }

    private IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> getReviewersStatisticModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected HashMap<ObjectReferenceType, ReviewerStatisticDto> load() {
                HashMap<ObjectReferenceType, ReviewerStatisticDto> reviewersUnresolvedItemsCount = new HashMap<>();
                AccessCertificationCampaignType campaign = model.getObjectType();
                List<ObjectReferenceType> reviewers = CertMiscUtil.loadCampaignReviewers(campaign.getOid(),
                        ReviewersStatisticsPanel.this.getPageBase());

                reviewers.forEach(reviewerRef -> {
                    PrismObject<FocusType> reviewerObj = WebModelServiceUtils.loadObject(reviewerRef, getPageBase());
                    reviewersUnresolvedItemsCount.put(reviewerRef,
                        new ReviewerStatisticDto(reviewerRef, getNotDecidedOpenItemsCount(reviewerObj), getAllOpenItemsCount(reviewerObj)));
                });
                return reviewersUnresolvedItemsCount;
            }
        };
    }

    private DoughnutChartConfiguration getReviewerProgressChartConfig(ObjectReferenceType reviewerRef,
            IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersStatisticModel ) {
        ReviewerStatisticDto reviewerStatisticDto = reviewersStatisticModel.getObject().get(reviewerRef);
        DoughnutChartConfiguration config = new DoughnutChartConfiguration();

        ChartData chartData = new ChartData();
        ChartDataset dataset = new ChartDataset();
//        dataset.setLabel("Not decided");

        dataset.setFill(true);

        long notDecidedCertItemsCount = reviewerStatisticDto.getOpenNotDecidedItemsCount();
        long allOpenCertItemsCount = reviewerStatisticDto.getAllOpenItemsCount();
        long decidedCertItemsCount = allOpenCertItemsCount - notDecidedCertItemsCount;

        dataset.addData(decidedCertItemsCount);
        dataset.addBackgroudColor(getChartBackgroundColor(reviewerStatisticDto.getOpenDecidedItemsPercentage()));

        dataset.addData(notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");

        chartData.addDataset(dataset);

        config.setData(chartData);
        return config;
    }

    private String getChartBackgroundColor(float decidedItemsPercentage) {
        if (decidedItemsPercentage == 100) {
            return "green";
        } else if (decidedItemsPercentage > 50) {
            return "blue";
        } if (decidedItemsPercentage > 25) {
            return "#ffc107";
        } else {
            return "red";
        }
    }

    private LoadableDetachableModel<List<StatisticBoxDto<ObjectReferenceType>>> getSortedReviewersModel(
            IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersUnresolvedItemsCountModel, boolean restricted) {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<StatisticBoxDto<ObjectReferenceType>> load() {
                if (!sortedReviewerList.isEmpty()) {
                    return sortReviewers(reviewersUnresolvedItemsCountModel, sortedReviewerList);
                }
                List<StatisticBoxDto<ObjectReferenceType>> list = new ArrayList<>();
                HashMap<ObjectReferenceType, ReviewerStatisticDto> reviewersStatistics = reviewersUnresolvedItemsCountModel.getObject();
                List<ObjectReferenceType> reviewers = new ArrayList<>(reviewersStatistics.keySet());
                reviewers = reviewers.stream().sorted((r1, r2) -> {
                    ReviewerStatisticDto rs1 = reviewersStatistics.get(r1);
                    ReviewerStatisticDto rs2 = reviewersStatistics.get(r2);

                    if (Boolean.FALSE.equals(percentageSortingModel.getObject())) {
                        return Long.compare(rs2.getOpenNotDecidedItemsCount(), rs1.getOpenNotDecidedItemsCount());
                    }

                    float r1ItemsPercent = rs1.getOpenNotDecidedItemsPercentage();
                    float r2ItemsPercent = rs2.getOpenNotDecidedItemsPercentage();
                    return Float.compare(r2ItemsPercent, r1ItemsPercent);
                }).toList();
                if (restricted) {
                    realReviewersCount = reviewers.size();
                    reviewers.stream().limit(MAX_REVIEWERS).forEach(r -> list.add(createReviewerStatisticBoxDto(r)));
                } else {
                    reviewers.forEach(r -> list.add(createReviewerStatisticBoxDto(r)));
                }
                return list;
            }
        };
    }

    private List<StatisticBoxDto<ObjectReferenceType>> sortReviewers(
            IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersUnresolvedItemsCountModel,
            List<StatisticBoxDto<ObjectReferenceType>> reviewersList) {
        HashMap<ObjectReferenceType, ReviewerStatisticDto> reviewersStatistics = reviewersUnresolvedItemsCountModel.getObject();
        //todo duplicated piece of code
        reviewersList.sort((r1, r2) -> {
            ReviewerStatisticDto rs1 = reviewersStatistics.get(r1.getStatisticObject());
            ReviewerStatisticDto rs2 = reviewersStatistics.get(r2.getStatisticObject());

            if (Boolean.FALSE.equals(percentageSortingModel.getObject())) {
                return Long.compare(rs2.getOpenNotDecidedItemsCount(), rs1.getOpenNotDecidedItemsCount());
            }

            float r1ItemsPercent = rs1.getOpenNotDecidedItemsPercentage();
            float r2ItemsPercent = rs2.getOpenNotDecidedItemsPercentage();
            return Float.compare(r2ItemsPercent, r1ItemsPercent);
        });
        return reviewersList;
    }

    private long getNotDecidedOpenItemsCount(PrismObject<FocusType> reviewer) {
        if (reviewer == null) {
            return 0;
        }
        String campaignOid = model.getObjectType().getOid();
        MidPointPrincipal principal = MidPointPrincipal.create(reviewer.asObjectable());
        return CertMiscUtil.countOpenCertItems(Collections.singletonList(campaignOid), principal,
                true, getPageBase());
    }

    private long getAllOpenItemsCount(PrismObject<FocusType> reviewer) {
        if (reviewer == null) {
            return 0;
        }
        String campaignOid = model.getObjectType().getOid();
        MidPointPrincipal principal = MidPointPrincipal.create(reviewer.asObjectable());
        return CertMiscUtil.countOpenCertItems(Collections.singletonList(campaignOid), principal,
                false, getPageBase());
    }

    private boolean reviewersCountExceedsLimit() {
        return realReviewersCount > MAX_REVIEWERS;
    }

    private List<ObjectReferenceType> loadReviewers() {
        OperationResult result = new OperationResult(OPERATION_LOAD_CERT_ITEMS);
        AccessCertificationCampaignType campaign = model.getObjectType();
        Integer iteration = CertCampaignTypeUtil.norm(campaign.getIteration());
        Integer stage = CertCampaignTypeUtil.accountForClosingStates(or0(campaign.getStageNumber()), campaign.getState());
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
        ReviewersTileTablePanel reviewersPanel = new ReviewersTileTablePanel(getPageBase().getMainPopupBodyId(),
                getReviewersStatisticModel().getObject(), percentageSortingModel.getObject());
        getPageBase().showMainPopup(reviewersPanel, target);
    }



    private String createStatisticBoxLabel(ObjectReferenceType reviewerRef,
            IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersStatisticModel) {
        ReviewerStatisticDto reviewerStatistic = reviewersStatisticModel.getObject().get(reviewerRef);
        return getString("ReviewersStatisticsPanel.statistic.itemsWaitingForDecision",
                reviewerStatistic.getOpenNotDecidedItemsCount(), reviewerStatistic.getAllOpenItemsCount());
    }

    private String createPercentageLabel(ObjectReferenceType reviewerRef,
            IModel<HashMap<ObjectReferenceType, ReviewerStatisticDto>> reviewersStatisticModel) {
        ReviewerStatisticDto reviewerStatistic = reviewersStatisticModel.getObject().get(reviewerRef);
        return String.format("%.0f", reviewerStatistic.getOpenDecidedItemsPercentage()) + "%";
    }
}
