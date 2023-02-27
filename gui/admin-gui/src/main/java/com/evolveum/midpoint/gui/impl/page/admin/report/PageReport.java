/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.report;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;

import com.evolveum.midpoint.gui.impl.page.admin.report.component.ReportOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.ReportSummaryPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportObjectsListPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import java.util.Collection;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/report")
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

    private static final Trace LOGGER = TraceManager.getTrace(PageReport.class);

    private static final String ID_TABLE_CONTAINER = "tableContainer";
    private static final String ID_TABLE_BOX = "tableBox";
    private static final String ID_REPORT_TABLE = "reportTable";

    private static final String DOT_CLASS = PageReport.class.getName() + ".";
    protected static final String OPERATION_SAVE_AND_RUN_REPORT = DOT_CLASS + "saveAndRunReport";
    protected static final String OPERATION_RUN_REPORT = DOT_CLASS + "saveAndRunReport";
    private boolean processingOfSaveAndRun =false;

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
    public Class<ReportType> getType() {
        return ReportType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ReportType> summaryModel) {
        return new ReportSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ReportOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ReportType>> wrapperModel) {
        return new ReportOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected ReportType getReport() {
                return getReportWithAppliedChanges();
            }

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageReport.this.refresh(target);
            }

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

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageReport.this.hasUnsavedChanges(target);
            }
        };
    }

    protected void initLayout() {
        super.initLayout();

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);
        add(tableContainer);

        ReportObjectsListPanel<?> reportTable = new ReportObjectsListPanel<>(ID_REPORT_TABLE, () -> getReportWithAppliedChanges());
        reportTable.setOutputMarkupId(true);

        WebMarkupContainer tableBox = new WebMarkupContainer(ID_TABLE_BOX);
        tableBox.add(new VisibleBehaviour(() -> getOperationalButtonsPanel() != null && getOperationalButtonsPanel().isShowingPreview() && reportTable.hasView()));
        tableBox.setOutputMarkupId(true);
        tableContainer.add(tableBox);

        tableBox.add(reportTable);
    }

    private ReportType getReportWithAppliedChanges() {
        try {
            return getModelWrapperObject().getObjectApplyDelta().asObjectable();
        } catch (SchemaException e) {
            LOGGER.debug("Cannot apply changes for report, returning original state");
            return getModelWrapperObject().getObjectOld().asObjectable();
        }
    }

    public void saveAndRunPerformed(AjaxRequestTarget target) {
        try {
            processingOfSaveAndRun = true;
            OperationResult saveResult = new OperationResult(OPERATION_SAVE);
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = saveOrPreviewPerformed(target, saveResult, false);

            if (!saveResult.isError()) {
                PrismObject<ReportType> report = getReport(executedDeltas);
                if (!ReportOperationalButtonsPanel.hasParameters(report.asObjectable())) {
                    runReport(report, null);
                } else {

                    RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(getMainPopupBodyId(), report.asObjectable()) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public StringResourceModel getTitle() {
                            return createStringResource("PageReport.reportPreview");
                        }

                        protected void runConfirmPerformed(AjaxRequestTarget target, PrismObject<ReportType> report, PrismContainer<ReportParameterType> reportParam) {
                            runReport(report, reportParam);
                            hideMainPopup(target);
                        }
                    };
                    showMainPopup(runReportPopupPanel, target);
                }
            }
        } finally {
            processingOfSaveAndRun = false;
        }
    }

    private PrismObject<ReportType> getReport(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas) {
        try {
            if (getModelObjectType().getOid() != null) {
                return getModelWrapperObject().getObjectApplyDelta();
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't apply deltas to report.", e);
        }
        return (PrismObject<ReportType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
    }

    private void runReport(PrismObject<ReportType> report, PrismContainer<ReportParameterType> reportParam) {
        Task task = createSimpleTask(OPERATION_RUN_REPORT);
        OperationResult saveAndRunResult = task.getResult();
        try {
            getReportManager().runReport(report, reportParam, task, saveAndRunResult);
        } catch (Exception ex) {
            saveAndRunResult.recordFatalError(ex);
        } finally {
            saveAndRunResult.computeStatusIfUnknown();
            saveAndRunResult.setBackgroundTaskOid(task.getOid());
            showResult(saveAndRunResult);
            redirectBack();
        }
    }

    @Override
    protected boolean allowRedirectBack() {
        return !processingOfSaveAndRun;
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
