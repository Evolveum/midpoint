/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.util.Collection;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.ImportReportPopupPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportMainPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportObjectsListPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/report", matchUrlForSecurity = "/admin/report")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = "PageAdminCases.auth.reportsAll.label",
                description = "PageAdminCases.auth.reportsAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORT_URL,
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description") })
public class PageReport extends PageAdminObjectDetails<ReportType> {

    private static final String ID_TABLE_CONTAINER = "tableContainer";
    private static final String ID_TABLE_BOX = "tableBox";
    private static final String ID_REPORT_TABLE = "reportTable";

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";
    private static final String OPERATION_IMPORT_REPORT = DOT_CLASS + "importReport";

    private Boolean runReport = false;
    private IModel<Boolean> isShowingPreview = Model.of(Boolean.FALSE);

    public PageReport() {
        initialize(null);
    }

    public PageReport(PageParameters parameters) {
        super(parameters);
        initialize(null);
    }

    public PageReport(final PrismObject<ReportType> unitToEdit, boolean isNewObject) {
        super(unitToEdit, isNewObject);
        initialize(unitToEdit, isNewObject);
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);
        add(tableContainer);

        ReportObjectsListPanel reportTable = new ReportObjectsListPanel(ID_REPORT_TABLE, Model.of(getReport()));
        reportTable.setOutputMarkupId(true);

        WebMarkupContainer tableBox = new WebMarkupContainer(ID_TABLE_BOX);
        tableBox.add(new VisibleBehaviour(() -> isShowingPreview.getObject() && reportTable.hasView()));
        tableBox.setOutputMarkupId(true);
        tableContainer.add(tableBox);

        tableBox.add(reportTable);
    }

    @Override
    public Class<ReportType> getCompileTimeClass() {
        return ReportType.class;
    }

    @Override
    public ReportType createNewObject() {
        return new ReportType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<ReportType> createSummaryPanel(IModel<ReportType> summaryModel) {
        return new ReportSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this);
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageReport.class;
    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
        if (runReport && !result.isError()) {
            showResult(result);
            Task task = createSimpleTask("run_task");

            PrismObject<ReportType> report;
            if (getObjectModel().getObject().getOid() != null) {
                report = getObjectModel().getObject().getObject();
            } else {
                report = (PrismObject<ReportType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
            }
            if (hasParameters(report.asObjectable())) {
                try {
                    getReportManager().runReport(report, null, task, result);
                } catch (Exception ex) {
                    result.recordFatalError(ex);
                } finally {
                    result.computeStatusIfUnknown();
                }
                showResult(result);
                if (!isKeepDisplayingResults()) {
                    redirectBack();
                } else {
                    target.add(getFeedbackPanel());
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
                        showResult(result);
                        if (!isKeepDisplayingResults()) {
                            redirectBack();
                        } else {
                            target.add(getFeedbackPanel());
                        }
                    }
                };
                showMainPopup(runReportPopupPanel, target);
            }
            this.runReport = false;
        } else if (!isKeepDisplayingResults()) {
            showResult(result);
            redirectBack();
        }
    }

    private static boolean hasParameters(ReportType report) {
        return report.getObjectCollection() == null || report.getObjectCollection().getParameter().isEmpty();
    }

    @Override
    protected ReportMainPanel createMainPanel(String id) {
        return new ReportMainPanel(id, getObjectModel(), this);
    }

    public void saveAndRunPerformed(AjaxRequestTarget target) {
        this.runReport = true;
        savePerformed(target);
    }

    protected boolean isChangeArchetypeAllowed() {
        return false;
    }

    @Override
    protected void initOperationalButtons(RepeatingView repeatingView) {
        String refreshId = repeatingView.newChildId();
        String showPreviewId = repeatingView.newChildId();
        String showPreviewInPopupId = repeatingView.newChildId();

        AjaxButton refresh = new AjaxButton(refreshId, createStringResource("pageCreateCollectionReport.button.refresh")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ReportObjectsListPanel table = getReportTable();
                table.getPageStorage().setSearch(null);
                table.resetSearchModel();
                table.resetTable(target);
                table.refreshTable(target);
                table.checkView();
                target.add(table);
                target.add(getTableBox());
                target.add(getTableContainer());
                target.add(getFeedbackPanel());
            }
        };
        refresh.add(new VisibleBehaviour(() -> isShowingPreview.getObject()));
        refresh.add(AttributeAppender.append("class", "btn-info btn-sm"));
        refresh.setOutputMarkupId(true);
        repeatingView.add(refresh);

        AjaxButton showPreview = new AjaxButton(showPreviewId, createStringResource("pageCreateCollectionReport.button.showPreview.${}", isShowingPreview)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                isShowingPreview.setObject(!isShowingPreview.getObject());
                ReportObjectsListPanel table = getReportTable();
                if (isShowingPreview.getObject()) {
                    table.getPageStorage().setSearch(null);
                    table.resetSearchModel();
                    table.resetTable(target);
                    table.refreshTable(target);
                    table.checkView();
                }
                target.add(getTableBox());
                if (isShowingPreview.getObject() && table.hasView()) {
                    info(createStringResource("PageReport.message.shownedReportPreview").getString());
                }
                target.add(getTableContainer());
                target.add(this);
                target.add(repeatingView.get(showPreviewInPopupId));
                target.add(repeatingView.get(refreshId));
                target.add(getOperationalButtonsPanel());
                target.add(getFeedbackPanel());
            }
        };
        showPreview.add(new VisibleBehaviour(this::isCollectionReport));
        showPreview.add(AttributeAppender.append("class", "btn-default btn-sm"));
        showPreview.setOutputMarkupId(true);
        repeatingView.add(showPreview);

        AjaxButton showPreviewInPopup = new AjaxButton(showPreviewInPopupId, createStringResource("pageCreateCollectionReport.button.showPreviewInPopup")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RunReportPopupPanel reportPopup = new RunReportPopupPanel(getMainPopupBodyId(), getReport(), false);
                showMainPopup(reportPopup, target);
                target.add(getOperationalButtonsPanel());
            }
        };
        showPreviewInPopup.add(new VisibleBehaviour(() -> isCollectionReport() && !isShowingPreview.getObject()));
        showPreviewInPopup.add(AttributeAppender.append("class", "btn-default btn-sm"));
        showPreviewInPopup.setOutputMarkupId(true);
        repeatingView.add(showPreviewInPopup);

        AjaxButton runReport = new AjaxButton(repeatingView.newChildId(), createStringResource("pageCreateCollectionReport.button.runOriginalReport")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                runReportPerformed(target, getOriginalReport(), PageReport.this);
            }
        };
        runReport.add(new VisibleBehaviour(() -> isEditingFocus() && !WebComponentUtil.isImportReport(getOriginalReport())));
        runReport.add(AttributeAppender.append("class", "btn-info btn-sm"));
        runReport.setOutputMarkupId(true);
        repeatingView.add(runReport);

        AjaxButton importReport = new AjaxButton(repeatingView.newChildId(), createStringResource("pageCreateCollectionReport.button.importOriginalReport")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                importReportPerformed(target, getOriginalReport(), PageReport.this);
            }
        };
        importReport.add(new VisibleBehaviour(() -> isEditingFocus() && WebComponentUtil.isImportReport(getOriginalReport())));
        importReport.add(AttributeAppender.append("class", "btn-info btn-sm"));
        importReport.setOutputMarkupId(true);
        repeatingView.add(importReport);
    }

    private ReportType getOriginalReport() {
        return getObjectWrapper().getObjectOld().asObjectable();
    }

    public static void importReportPerformed(AjaxRequestTarget target, ReportType report, PageBase pageBase) {
        ImportReportPopupPanel importReportPopupPanel = new ImportReportPopupPanel(pageBase.getMainPopupBodyId(), report) {

            private static final long serialVersionUID = 1L;

            protected void importConfirmPerformed(AjaxRequestTarget target, ReportDataType reportImportData) {
                PageReport.importConfirmPerformed(target, report, reportImportData, pageBase);
                pageBase.hideMainPopup(target);

            }
        };
        pageBase.showMainPopup(importReportPopupPanel, target);
    }

    private static void importConfirmPerformed(AjaxRequestTarget target, ReportType reportType, ReportDataType reportImportData, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_IMPORT_REPORT);
        Task task = pageBase.createSimpleTask(OPERATION_IMPORT_REPORT);

        try {
            pageBase.getReportManager().importReport(reportType.asPrismObject(), reportImportData.asPrismObject(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        pageBase.showResult(result);
        target.add(pageBase.getFeedbackPanel());
    }

    public static void runReportPerformed(AjaxRequestTarget target, ReportType report, PageBase pageBase) {

        if (hasParameters(report)) {
            runConfirmPerformed(target, report, null, pageBase);
            return;
        }

        RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(pageBase.getMainPopupBodyId(), report) {

            private static final long serialVersionUID = 1L;

            protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam) {
                PageReport.runConfirmPerformed(target, reportType, reportParam, pageBase);
                pageBase.hideMainPopup(target);
            }
        };
        pageBase.showMainPopup(runReportPopupPanel, target);

    }

    private static void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
        Task task = pageBase.createSimpleTask(OPERATION_RUN_REPORT);

        try {
            pageBase.getReportManager().runReport(reportType.asPrismObject(), reportParam, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        pageBase.showResult(result);
        target.add(pageBase.getFeedbackPanel());
    }

    private ReportType getReport() {
        return getObjectWrapper().getObject().asObjectable();
    }

    @Override
    protected Boolean isOperationalButtonsVisible() {
        return true;
    }

    private boolean isCollectionReport() {
        return getObjectWrapper().findItemDefinition(ReportType.F_OBJECT_COLLECTION) != null;
    }

    private ReportObjectsListPanel getReportTable() {
        return (ReportObjectsListPanel) get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX, ID_REPORT_TABLE));
    }

    private Component getTableBox() {
        return get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX));
    }

    private Component getTableContainer() {
        return get(ID_TABLE_CONTAINER);
    }
}
