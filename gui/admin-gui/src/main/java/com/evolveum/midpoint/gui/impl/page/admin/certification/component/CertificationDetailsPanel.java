/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimpleContainerPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.component.CertificationItemsTabbedPanel;
import com.evolveum.midpoint.web.page.admin.certification.component.StatisticBoxDto;
import com.evolveum.midpoint.web.page.admin.certification.component.StatisticListBoxPanel;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.web.page.admin.reports.ReportDownloadHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "certificationDetails") //TODO better name
@PanelInstance(identifier = "certificationDetails", //TODO better name
        applicableForType = AccessCertificationCampaignType.class,
        defaultPanel = true,// change later to assignmentHolder type, probably we will want org assignments later
        display = @PanelDisplay(label = "Responses" /*TODO better name*/, icon = GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON, order = 1))
// TODO @Counter(provider = AssignmentCounter.class)
public class CertificationDetailsPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationDetailsPanel.class);

    private static final String DOT_CLASS = CertificationDetailsPanel.class.getName() + ".";

    private static final String ID_RESPONSES_CONTAINER = "responsesContainer";
    private static final String ID_RESPONSES = "responses";
    private static final String ID_CREATED_REPORTS = "createdReports"; //todo temporary here
    private static final String ID_ITEMS_TABBED_PANEL = "itemsTabbedPanel";

    public CertificationDetailsPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SimpleContainerPanel responsesContainer = new SimpleContainerPanel(ID_RESPONSES_CONTAINER,
                createStringResource("PageCertCampaign.statistics.responses"));
        responsesContainer.add(AttributeModifier.append("class", "card p-4"));
        responsesContainer.setOutputMarkupId(true);
        add(responsesContainer);

        ProgressBarPanel responsesPanel = new ProgressBarPanel(ID_RESPONSES, createResponseStatisticsModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isSimpleView() {
                return false;
            }

            @Override
            protected boolean isPercentageBar() {
                return false;
            }
        };
        responsesPanel.setOutputMarkupId(true);
        responsesContainer.add(responsesPanel);

        //todo temporary here; just for testing
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
        createdReports.add(new VisibleBehaviour(() -> false));
        add(createdReports);

        addOrReplaceCertItemsTabbedPanel();
    }

    private AccessCertificationCasesStatisticsType getStatistics() {
        return getObjectDetailsModels().getCertStatisticsModel().getObject();
    }

    private @NotNull LoadableModel<List<ProgressBar>> createResponseStatisticsModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                List<ProgressBar> progressBars = new ArrayList<>();

                AccessCertificationCasesStatisticsType statisticsType = getStatistics();
                CertificationItemResponseHelper responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.ACCEPT);
                ProgressBar accepted = new ProgressBar(statisticsType.getMarkedAsAccept(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(accepted);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REVOKE);
                ProgressBar revoked = new ProgressBar(statisticsType.getMarkedAsRevoke(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(revoked);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REDUCE);
                ProgressBar reduced = new ProgressBar(statisticsType.getMarkedAsReduce(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(reduced);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.NOT_DECIDED);
                ProgressBar notDecided = new ProgressBar(statisticsType.getMarkedAsNotDecide(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(notDecided);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.NO_RESPONSE);
                ProgressBar noResponse = new ProgressBar(statisticsType.getWithoutResponse(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(noResponse);
                return progressBars;
            }
        };
    }

    private IModel<List<StatisticBoxDto<ReportDataType>>> getCreatedReportsModel() {
        return () -> {
            List<StatisticBoxDto<ReportDataType>> list = new ArrayList<>();
            List<ReportDataType> reports = loadReports();
            if (reports == null) {
                return list;
            }
            reports.forEach(r -> list.add(createStatisticBoxDto(r)));
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

    private StatisticBoxDto<ReportDataType> createStatisticBoxDto(ReportDataType report) {
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


    private IModel<DisplayType> getCreatedReportsDisplayModel(int reportsCount) {
        String reportsCountKey = reportsCount == 1 ? "PageCertCampaign.singleCreatedReportCount" :
                "PageCertCampaign.createdReportsCount";
        return () -> new DisplayType()
                .label("PageCertCampaign.createdReportsTitle")
                .help(createStringResource(reportsCountKey, reportsCount).getString());
    }

    private String getCreatedOnDateLabel(ReportDataType report) {
        XMLGregorianCalendar createDate = report.getMetadata() != null ? report.getMetadata().getCreateTimestamp() : null;
        String createdOn = WebComponentUtil.getLocalizedDate(createDate, DateLabelComponent.SHORT_NOTIME_STYLE);
        if (StringUtils.isNotEmpty(createdOn)) {
            return createStringResource("PageCertCampaign.createdOn", createdOn).getString();
        }
        return null;
    }

    private void addOrReplaceCertItemsTabbedPanel() {
        //TODO change to wrapper model?
        CertificationItemsTabbedPanel items = new CertificationItemsTabbedPanel(ID_ITEMS_TABBED_PANEL, getObjectDetailsModels().getSummaryModel());
        items.setOutputMarkupId(true);
        addOrReplace(items);
    }
}
