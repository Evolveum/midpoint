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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@PanelType(name = "createdReports")
@PanelInstance(identifier = "createdReports",
        applicableForType = AccessCertificationCampaignType.class,
        display = @PanelDisplay(label = "ResponsesOverviewPanel.createdReports.label",
                icon = GuiStyleConstants.CLASS_REPORT_ICON, order = 10))
public class CreatedReportsPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CreatedReportsPanel.class);

    private static final String DOT_CLASS = CreatedReportsPanel.class.getName() + ".";

    private static final String ID_CREATED_REPORTS = "createdReports";

    public CreatedReportsPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
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

}
