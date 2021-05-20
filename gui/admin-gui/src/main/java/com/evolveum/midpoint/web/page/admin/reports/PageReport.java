/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.util.*;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.component.EngineReportTabPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportMainPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportObjectsListPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.page.admin.server.TaskBasicTabPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/report", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORT_URL,
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description")})
public class PageReport extends PageAdminObjectDetails<ReportType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageReport.class);

    private static final String ID_TABLE_CONTAINER = "tableContainer";
    private static final String ID_TABLE_BOX = "tableBox";
    private static final String ID_REPORT_TABLE = "reportTable";

    private static final List<DisplayableValue<String>> TYPE_OF_REPORTS;
    private static final String OPERATION_UPDATE_WRAPPER = "update report wrapper";

    static {
        TYPE_OF_REPORTS = Arrays.asList(
                new SearchValue<>(SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value(), "CollectionReports.title"),
                new SearchValue<>(SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value(), "DashboardReports.title"));
    }

    private Boolean runReport = false;
    private IModel<Boolean> isShowingPreview = Model.of(Boolean.FALSE);
    private IModel<DisplayableValue<String>> archetypeOid;

    public PageReport() {
        initialize(null);
    }

    public PageReport(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageReport(final PrismObject<ReportType> userToEdit) {
        initialize(userToEdit);
    }

    public PageReport(final PrismObject<ReportType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }

    public PageReport(final PrismObject<ReportType> unitToEdit, boolean isNewObject, boolean isReadonly)  {
        initialize(unitToEdit, isNewObject, isReadonly);
    }

    @Override
    protected void initializeModel(PrismObject<ReportType> objectToEdit, boolean isNewObject, boolean isReadonly) {
        super.initializeModel(objectToEdit, isNewObject, isReadonly);
        ReportType report = getObjectModel().getObject().getObject().asObjectable();
        archetypeOid = new Model();
        if (WebComponentUtil.hasArchetypeAssignment(report, SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value())) {
            archetypeOid.setObject(getDisplayValue(SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value()));
        } else if (WebComponentUtil.hasArchetypeAssignment(report, SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value())) {
            archetypeOid.setObject(getDisplayValue(SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value()));
        }
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

    private SearchValue<String> getDisplayValue(String oid) {
        for (DisplayableValue<String> value : TYPE_OF_REPORTS){
            if (oid.equals(value.getValue())) {
                return (SearchValue<String>) value;
            }
        }
        return null;
    }

    @Override
    public Class<ReportType> getCompileTimeClass() {
        return ReportType.class;
    }

    @Override
    protected ReportType createNewObject() {
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
        if(runReport && !result.isError()) {
            showResult(result);
            Task task = createSimpleTask("run_task");
            try {

                PrismObject<ReportType> report;
                if (getObjectModel().getObject().getOid() != null) {
                    report = getObjectModel().getObject().getObject();
                } else {
                    report = (PrismObject<ReportType>) executedDeltas.iterator().next().getObjectDelta().getObjectToAdd();
                }
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
            this.runReport = false;
        } else if (!isKeepDisplayingResults()) {
            showResult(result);
            redirectBack();
        }
    }

    @Override
    protected ReportMainPanel createMainPanel(String id) {
        return new ReportMainPanel(id, getObjectModel(), this);
    }

    public void saveAndRunPerformed(AjaxRequestTarget target){
        this.runReport = true;
        savePerformed(target);
    }

    protected boolean isChangeArchetypeAllowed() {
        return false;
    }

    @Override
    protected void initOperationalButtons(RepeatingView repeatingView) {
        String selectHeaderId = repeatingView.newChildId();
        String selectTypeId = repeatingView.newChildId();
        String refreshId = repeatingView.newChildId();
        String showPreviewId = repeatingView.newChildId();
        String showPreviewInPopupId = repeatingView.newChildId();

        Label label = new Label(selectHeaderId, createStringResource("PageReport.typeOfReport"));
        label.add(AttributeAppender.append("style", "padding-right: 0px; padding-top: 4px;"));
        label.add(new VisibleBehaviour(() -> archetypeOid.getObject() == null || archetypeOid.getObject().getValue() == null));
        repeatingView.add(label);

        DropDownChoicePanel dropDownPanel = WebComponentUtil.createDropDownChoices(selectTypeId, archetypeOid, Model.ofList(TYPE_OF_REPORTS), true, this);
        dropDownPanel.add(new VisibleBehaviour(() -> archetypeOid.getObject() == null || archetypeOid.getObject().getValue() == null));
        dropDownPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                addArchetype(target);
                target.add(dropDownPanel);
                target.add(repeatingView.get(showPreviewInPopupId));
                target.add(repeatingView.get(showPreviewId));
                target.add(getOperationalButtonsPanel());
                refreshEngineTab(target);
                target.add(getFeedbackPanel());
            }
        });
        dropDownPanel.add(AttributeAppender.append("style", "margin-top: -7px; margin-left: -12px;"));
        dropDownPanel.setOutputMarkupId(true);
        repeatingView.add(dropDownPanel);

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
        showPreview.add(new VisibleBehaviour(() -> isCollectionReport()));
        showPreview.add(AttributeAppender.append("class", "btn-default btn-sm"));
        showPreview.setOutputMarkupId(true);
        repeatingView.add(showPreview);

        AjaxButton showPreviewInPopup = new AjaxButton(showPreviewInPopupId, createStringResource("pageCreateCollectionReport.button.showPreviewInPopup")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RunReportPopupPanel reportPopup = new RunReportPopupPanel(getMainPopupBodyId(), getReport(), false){
                    @Override
                    public StringResourceModel getTitle() {
                        return createStringResource("PageReport.reportPreview");
                    }
                };
                showMainPopup(reportPopup, target);
                target.add(getOperationalButtonsPanel());
            }
        };
        showPreviewInPopup.add(new VisibleBehaviour(() -> isCollectionReport() && !isShowingPreview.getObject()));
        showPreviewInPopup.add(AttributeAppender.append("class", "btn-default btn-sm"));
        showPreviewInPopup.setOutputMarkupId(true);
        repeatingView.add(showPreviewInPopup);
    }

    private void refreshEngineTab(AjaxRequestTarget target) {
        Component panel = getMainPanel().getTabbedPanel().get(TabbedPanel.TAB_PANEL_ID);
        if (panel instanceof EngineReportTabPanel) {
            ((PanelTab)getMainPanel().getTabbedPanel().getTabs().getObject().get(
                    getMainPanel().getTabbedPanel().getSelectedTab())).resetPanel();
            getMainPanel().getTabbedPanel().setSelectedTab(getMainPanel().getTabbedPanel().getSelectedTab());
            target.add(getMainPanel().getTabbedPanel());
            target.add(getMainPanel());
        } else {
            getMainPanel().getTabbedPanel().getTabs().getObject().forEach(tab -> {
                if (((PanelTab)tab).getPanel() instanceof EngineReportTabPanel){
                    ((PanelTab)tab).resetPanel();
                }
            });
        }

    }

    private void addArchetype(AjaxRequestTarget target) {
        WebComponentUtil.addNewArchetype(getObjectWrapper(), archetypeOid.getObject().getValue(), target, PageReport.this);
        PrismObjectWrapperFactory<ReportType> wrapperFactory = findObjectWrapperFactory(getReport().asPrismObject().getDefinition());
        Task task = createSimpleTask(OPERATION_UPDATE_WRAPPER);
        OperationResult result = task.getResult();
        WrapperContext ctx = new WrapperContext(task, result);
        try {
            wrapperFactory.updateWrapper(getObjectWrapper(), ctx);

            //TODO ugly hack: after updateWrapper method is called, previously set assignment item
            // are marked as NOT_CHANGED with the same value.

            PrismContainerWrapper<AssignmentType> assignmentWrapper = getObjectWrapper().findContainer(ItemPath.create(TaskType.F_ASSIGNMENT));
            for (PrismContainerValueWrapper<AssignmentType> assignmentWrapperValue : assignmentWrapper.getValues()) {
                if (WebComponentUtil.isArchetypeAssignment(assignmentWrapperValue.getRealValue())) {
                    assignmentWrapperValue.setStatus(ValueStatus.ADDED);
                }
            }

        } catch (SchemaException e) {
            LOGGER.error("Unexpected problem occurs during updating wrapper. Reason: {}", e.getMessage(), e);
        }
    }

    private ReportType getReport() {
        return getObjectModel().getObject().getObject().asObjectable();
    }

    @Override
    protected Boolean isOperationalButtonsVisible() {
        return true;
    }

    private boolean isCollectionReport() {
        if (archetypeOid.getObject() != null) {
            return SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value().equals(archetypeOid.getObject().getValue());
        }
        return false;
    }

    private ReportObjectsListPanel getReportTable(){
        return (ReportObjectsListPanel) get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX, ID_REPORT_TABLE));
    }

    private Component getTableBox(){
        return get(createComponentPath(ID_TABLE_CONTAINER, ID_TABLE_BOX));
    }

    private Component getTableContainer(){
        return get(ID_TABLE_CONTAINER);
    }

}
