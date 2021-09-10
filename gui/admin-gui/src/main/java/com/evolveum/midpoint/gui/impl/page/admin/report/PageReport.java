/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.report;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;

import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.report.component.ReportOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.ReportSummaryPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportObjectsListPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import java.util.Collection;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/reportNew")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = "PageAdminCases.auth.reportsAll.label",
                description = "PageAdminCases.auth.reportsAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORT_URL,
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description") })
public class PageReport extends PageAssignmentHolderDetails<ReportType, AssignmentHolderDetailsModel<ReportType>> {

    private static final String ID_TABLE_CONTAINER = "tableContainer";
    private static final String ID_TABLE_BOX = "tableBox";
    private static final String ID_REPORT_TABLE = "reportTable";

    private Boolean runReport = false;

    public PageReport() {
        super();
    }

    public PageReport(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageReport(PrismObject<ReportType> report) {
        super(report);
    }

    @Override
    protected Class<ReportType> getType() {
        return ReportType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<ReportType> summaryModel) {
        return new ReportSummaryPanel(id, summaryModel, this);
    }

    @Override
    protected OperationalButtonsPanel<ReportType> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ReportType>> wrapperModel) {
        return new ReportOperationalButtonsPanel(id, wrapperModel) {
            @Override
            protected Boolean isEditObject() {
                return PageReport.this.isEditObject();
            }

            @Override
            protected Component getTableContainer() {
                return PageReport.this.getTableContainer();
            }

            @Override
            protected Component getTableBox() {
                return PageReport.this.getTableBox();
            }

            @Override
            protected ReportObjectsListPanel<?> getReportTable() {
                return PageReport.this.getReportTable();
            }

            @Override
            public void saveAndRunPerformed(AjaxRequestTarget target) {
                PageReport.this.saveAndRunPerformed(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageReport.this.savePerformed(target);
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);
        add(tableContainer);

        ReportObjectsListPanel<?> reportTable = new ReportObjectsListPanel<>(ID_REPORT_TABLE, Model.of(getModelObjectType()));
        reportTable.setOutputMarkupId(true);

        WebMarkupContainer tableBox = new WebMarkupContainer(ID_TABLE_BOX);
        tableBox.add(new VisibleBehaviour(() -> getOperationalButtonsPanel().isShowingPreview() && reportTable.hasView()));
        tableBox.setOutputMarkupId(true);
        tableContainer.add(tableBox);

        tableBox.add(reportTable);
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if (runReport && !result.isError()) {
            showResult(result);
            Task task = createSimpleTask("run_task");

            PrismObject<ReportType> report;
            if (getModelObjectType().getOid() != null) {
                report = getModelPrismObject();
            } else {
                report = (PrismObject<ReportType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
            }
            if (ReportOperationalButtonsPanel.hasParameters(report.asObjectable())) {
                try {
                    getReportManager().runReport(report, null, task, result);
                } catch (Exception ex) {
                    result.recordFatalError(ex);
                } finally {
                    result.computeStatusIfUnknown();
                }

            } else {

                RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(getMainPopupBodyId(), report.asObjectable()) {

                    private static final long serialVersionUID = 1L;

                    protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam) {
                        try {
                            getReportManager().runReport(reportType.asPrismObject(), reportParam, task, result);
                        } catch (Exception ex) {
                            result.recordFatalError(ex);
                        } finally {
                            result.computeStatusIfUnknown();
                        }
                        hideMainPopup(target);
                    }
                };
                showResult(result);
                if (!isKeepDisplayingResults()) {
                    redirectBack();
                } else {
                    target.add(getFeedbackPanel());
                }
                showMainPopup(runReportPopupPanel, target);
            }
            this.runReport = false;
        } else if (!isKeepDisplayingResults()) {
            showResult(result);
            redirectBack();
        }
    }

    public void saveAndRunPerformed(AjaxRequestTarget target) {
        this.runReport = true;
        savePerformed(target);
    }

    private ReportObjectsListPanel<?> getReportTable() {
        return (ReportObjectsListPanel<?>) get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX, ID_REPORT_TABLE));
    }

    private Component getTableBox() {
        return get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX));
    }

    private Component getTableContainer() {
        return get(ID_TABLE_CONTAINER);
    }

    @Override
    protected ReportOperationalButtonsPanel getOperationalButtonsPanel() {
        return (ReportOperationalButtonsPanel) super.getOperationalButtonsPanel();
    }
}
