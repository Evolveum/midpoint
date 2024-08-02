/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SelectReportTemplatePanel extends BasePanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SelectReportTemplatePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REPORTS = DOT_CLASS + "loadReports";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runCertItemsReport";

    private static final String ID_MESSAGE_LABEL = "messageLabel";
    private static final String ID_REPORTS_FOR_CASES_LABEL = "reportsForCasesLabel";
    private static final String ID_REPORTS_FOR_ITEMS_LABEL = "reportsForItemsLabel";
    private static final String ID_CASES_REPORTS_CONTAINER = "casesReportContainer";
    private static final String ID_CASES_REPORT_SELECTION_PANEL = "casesReportSelectionPanel";
    private static final String ID_ITEMS_REPORTS_CONTAINER = "itemsReportContainer";
    private static final String ID_ITEMS_REPORT_SELECTION_PANEL = "itemsReportSelectionPanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_FEEDBACK = "feedback";

    PrismObject<ReportType> selectedReport = null;

    public SelectReportTemplatePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);

        add(createLabel(ID_MESSAGE_LABEL, "SelectReportTemplatePanel.message"));
        //todo the idea was to load all reports for cases and cert items and to divide them in 2 groups
        //at the moment, it's impossible to search for reports by object type, so only initial object reports are loaded
//        add(createLabel(ID_REPORTS_FOR_CASES_LABEL, "SelectReportTemplatePanel.forCases.message"));
//        add(createLabel(ID_REPORTS_FOR_ITEMS_LABEL, "SelectReportTemplatePanel.forCertItems.message"));

        initCasesReportsPanel();
        initCertItemsReportsPanel();
    }

    private Label createLabel(String id, String key) {
        Label label = new Label(id, getPageBase().createStringResource(key));
        label.setEscapeModelStrings(true);
        return label;
    }

    private void initCasesReportsPanel() {
        List<PrismObject<ReportType>> reports = loadCasesReports();
        initReportsPanel(ID_CASES_REPORTS_CONTAINER, ID_CASES_REPORT_SELECTION_PANEL, reports);
    }

    private void initCertItemsReportsPanel() {
        List<PrismObject<ReportType>> reports = loadCertItemsReports();
        initReportsPanel(ID_ITEMS_REPORTS_CONTAINER, ID_ITEMS_REPORT_SELECTION_PANEL, reports);
    }

    private void initReportsPanel(String listViewComponentId, String listItemComponentId, List<PrismObject<ReportType>> reports) {
        ListView<PrismObject<ReportType>> reportsPanel = new ListView<>(listViewComponentId, reports) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<PrismObject<ReportType>> item) {
                SelectableInfoBoxPanel<PrismObject<ReportType>> widget =
                        new SelectableInfoBoxPanel<>(listItemComponentId, item.getModel()) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected void itemSelectedPerformed(PrismObject<ReportType> report, AjaxRequestTarget target) {
                                selectedReport = report;
                                target.add(SelectReportTemplatePanel.this);
                            }

                            @Override
                            protected IModel<String> getIconClassModel() {
                                return () -> GuiStyleConstants.CLASS_REPORT_ICON + " text-gray";
                            }

                            @Override
                            protected IModel<String> getLabelModel() {
                                return () -> WebComponentUtil.getName(item.getModelObject());
                            }

                            @Override
                            protected IModel<String> getDescriptionModel() {
                                return () -> getModelObject().asObjectable().getDescription();
                            }

                            protected IModel<String> getAdditionalLinkStyle() {
                                return getItemPanelAdditionalStyle(item.getModelObject());
                            }
                        };
                item.add(widget);
            }
        };
        reportsPanel.setOutputMarkupId(true);
        add(reportsPanel);
    }

    @Override
    @NotNull
    public Component getFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxSubmitButton confirmButton = new AjaxSubmitButton(ID_CONFIRM_BUTTON,
                getPageBase().createStringResource("PageBase.button.run")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                runReportPerformed(selectedReport, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

        };
        footer.add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, getPageBase().createStringResource("PageBase.button.cancel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        footer.add(cancelButton);

        return footer;
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    private void runReportPerformed(PrismObject<ReportType> report, AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
        Task task = getPageBase().createSimpleTask(OPERATION_RUN_REPORT);
        OperationResult result = task.getResult();
        try {
            PrismContainer<ReportParameterType> params = createParameters(report);
            getPageBase().getReportManager().runReport(report, params, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    protected PrismContainer<ReportParameterType> createParameters(PrismObject<ReportType> report) {
        return null;
    }

    private List<PrismObject<ReportType>> loadCasesReports() {
        String oid = "00000000-0000-0000-0000-000000000150";    //initial object report for campaign cases
        return Collections.singletonList(loadReport(oid));
//        return loadReports(AccessCertificationCaseType.COMPLEX_TYPE);
    }

    private List<PrismObject<ReportType>> loadCertItemsReports() {
        String oid = "00000000-0000-0000-0000-000000000160";    //initial object report for cert items
        return Collections.singletonList(loadReport(oid));
//        return loadReports(AccessCertificationWorkItemType.COMPLEX_TYPE);
    }

    private PrismObject<ReportType> loadReport(String oid) {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> report;
        try {
            report = getPageBase().getModelService().getObject(ReportType.class, oid, null, task, result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load report", ex);
            report = null;
        }
        return report;
    }

    //todo ReportType.F_OBJECT_COLLECTION is not searchable at the moment
    //may be can be used in the future
    private List<PrismObject<ReportType>> loadReports(QName type) {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REPORTS);
        OperationResult result = task.getResult();
        ObjectQuery query = getPageBase().getPrismContext().queryFor(ReportType.class)
                .item(ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW,
                        GuiObjectListViewType.F_TYPE))
                .eq(type)
                .build();
        List<PrismObject<ReportType>> reports;
        try {
            reports = getPageBase().getModelService().searchObjects(ReportType.class, query, null, task, result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load reports", ex);
            reports = new ArrayList<>();
        }
        return reports;
    }

    private IModel<String> getItemPanelAdditionalStyle(PrismObject<ReportType> report) {
        return isSelected(report) ? Model.of("active") : Model.of("");
    }

    private boolean isSelected(PrismObject<ReportType> report) {
        return report != null && report.equals(selectedReport);
    }

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 300;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("SelectReportTemplatePanel.title");
    }

}
