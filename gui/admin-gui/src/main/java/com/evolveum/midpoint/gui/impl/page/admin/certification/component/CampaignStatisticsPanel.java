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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.page.admin.reports.ReportDownloadHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@PanelType(name = "campaignStatistics")
@PanelInstance(identifier = "campaignStatistics",
        applicableForType = AccessCertificationCampaignType.class,
        display = @PanelDisplay(label = "CampaignStatisticsPanel.label",
                icon = GuiStyleConstants.CLASS_TASK_STATISTICS_ICON, order = 10))
public class CampaignStatisticsPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CampaignStatisticsPanel.class);

    private static final String DOT_CLASS = CampaignStatisticsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CERT_ITEMS = DOT_CLASS + "loadCertItems";
    private static final String OPERATION_REVIEWER = DOT_CLASS + "loadReviewer";

    private static final String ID_CREATED_REPORTS = "createdReports";
    private static final String ID_REVIEWERS_PANEL = "reviewersPanel";

    public CampaignStatisticsPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        initReviewersPanel();
        initCreatedReportsPanel();
    }

    private void initCreatedReportsPanel() {
        IModel<List<StatisticBoxDto<ReportDataType>>> createdReportsModel = getCreatedReportsModel();
        StatisticListBoxPanel<ReportDataType> createdReports = new StatisticListBoxPanel<>(ID_CREATED_REPORTS,
                getCreatedReportsDisplayModel(createdReportsModel.getObject().size()), createdReportsModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void viewAllActionPerformed(AjaxRequestTarget target) {
                //todo show in the popup? redirect to the report page?
            }

            @Override
            protected Component createRightSideBoxComponent(String id, IModel<StatisticBoxDto<ReportDataType>> model) {
                ReportDataType currentReport = model.getObject().getStatisticObject();
                AjaxDownloadBehaviorFromStream ajaxDownloadBehavior =
                        ReportDownloadHelper.createAjaxDownloadBehaviorFromStream(currentReport, getPageBase());
                AjaxIconButton downloadButton = new AjaxIconButton(id, Model.of("fa fa-download"),
                        createStringResource("pageCreatedReports.button.download")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ajaxDownloadBehavior.initiate(target);
                    }
                };
                downloadButton.add(ajaxDownloadBehavior);
                return downloadButton;
            }
        };
        add(createdReports);
    }

    private void initReviewersPanel() {
        IModel<List<StatisticBoxDto<ObjectReferenceType>>> reviewersModel = getReviewersModel();
        StatisticListBoxPanel<ObjectReferenceType> reviewersPanel = new StatisticListBoxPanel<>(ID_REVIEWERS_PANEL,
                getReviewersPanelDisplayModel(reviewersModel.getObject().size()), reviewersModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void viewAllActionPerformed(AjaxRequestTarget target) {
                //todo show in the popup?
            }

            @Override
            protected Component createRightSideBoxComponent(String id, IModel<StatisticBoxDto<ObjectReferenceType>> model) {
                ObjectReferenceType reviewerRef = model.getObject().getStatisticObject();
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
        add(reviewersPanel);
    }

    private DoughnutChartConfiguration getReviewerProgressChartConfig(ObjectReferenceType reviewerRef) {
        PrismObject<FocusType> reviewer = WebModelServiceUtils.loadObject(reviewerRef, getPageBase());
        if (reviewer == null) {
            return null;
        }
        AccessCertificationCampaignType campaign = getObjectDetailsModels().getObjectType();
        MidPointPrincipal principal = MidPointPrincipal.create(reviewer.asObjectable());
        long notDecidedCertItemsCount = CertMiscUtil.countOpenCertItems(Collections.singletonList(campaign.getOid()),
                principal, true, getPageBase());
        return CertMiscUtil.createDoughnutChartConfigForCampaigns(
                Collections.singletonList(campaign.getOid()), principal, getPageBase());
    }

    private IModel<List<StatisticBoxDto<ObjectReferenceType>>> getReviewersModel() {
        return () -> {
            List<StatisticBoxDto<ObjectReferenceType>> list = new ArrayList<>();
            List<ObjectReferenceType> reviewers = loadReviewers();
            reviewers.forEach(r -> list.add(createReviewerStatisticBoxDto(r)));
            return list;
        };
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

    private IModel<List<StatisticBoxDto<ReportDataType>>> getCreatedReportsModel() {
        return () -> {
            List<StatisticBoxDto<ReportDataType>> list = new ArrayList<>();
            List<ReportDataType> reports = loadReports();
            if (reports == null) {
                return list;
            }
            reports.forEach(r -> list.add(createReportStatisticBoxDto(r)));
            return list;
        };
    }

    private List<ReportDataType> loadReports() {
        ObjectQuery query = getPrismContext().queryFor(ReportDataType.class).build();
        try {
            List<PrismObject<ReportDataType>> reports =
                    WebModelServiceUtils.searchObjects(ReportDataType.class, query, null,
                            new OperationResult("OPERATION_LOAD_REPORTS"), getPageBase());
            return reports.stream()
                    .map(r -> r.asObjectable())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get reports", ex);
        }
        return null;
    }

    private StatisticBoxDto<ReportDataType> createReportStatisticBoxDto(ReportDataType report) {
        DisplayType displayType = new DisplayType()
                .label(report.getName())
                .help(getCreatedOnDateLabel(report))
                .icon(new IconType().cssClass("fa fa-chart-pie"));
        return new StatisticBoxDto<>(Model.of(displayType), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public ReportDataType getStatisticObject() {
                return report;
            }
        };
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


    private IModel<DisplayType> getCreatedReportsDisplayModel(int reportsCount) {
        String reportsCountKey = reportsCount == 1 ? "PageCertCampaign.singleCreatedReportCount" :
                "PageCertCampaign.createdReportsCount";
        return () -> new DisplayType()
                .label("PageCertCampaign.createdReportsTitle")
                .help(createStringResource(reportsCountKey, reportsCount).getString());
    }

    private IModel<DisplayType> getReviewersPanelDisplayModel(int reviewersCount) {
        String reviewersCountKey = reviewersCount == 1 ? "CampaignStatisticsPanel.reviewersPanel.singleReviewerCount" :
                "CampaignStatisticsPanel.reviewersPanel.reviewersCount";
        return () -> new DisplayType()
                .label("CampaignStatisticsPanel.reviewersPanel.title")
                .help(createStringResource(reviewersCountKey, reviewersCount).getString());
    }

    private String getCreatedOnDateLabel(ReportDataType report) {
        XMLGregorianCalendar createDate = report.getMetadata() != null ? report.getMetadata().getCreateTimestamp() : null;
        String createdOn = WebComponentUtil.getLocalizedDate(createDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        if (StringUtils.isNotEmpty(createdOn)) {
            return createStringResource("PageCertCampaign.createdOn", createdOn).getString();
        }
        return null;
    }

}
