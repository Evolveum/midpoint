/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.search.Search;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lskublik
 */
public class ExportingPanel extends BasePanel implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(ExportingPanel.class);

    private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "panel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_CREATE_REPORT = "createReport";
    private static final String ID_EXPORT = "export";
    private static final String ID_CANCEL = "cancelButton";
    private static final String ID_TAB = "tabPanel";

    private DataTable<?,?> dataTable;
    private List<Integer> exportedColumnsIndex;
    private Long exportSizeLimit;
    private LoadableModel<Search> search;

    public ExportingPanel(String id, DataTable<?, ?> dataTable, List<Integer> exportedColumnsIndex,
            Long exportSizeLimit, LoadableModel<Search> search) {
        super(id);
        this.dataTable = dataTable;
        this.exportedColumnsIndex = exportedColumnsIndex;
        this.exportSizeLimit = exportSizeLimit;
        this.search = search;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);

        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        panel.add(warningMessage);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        panel.add(feedbackList);

        TabbedPanel<ITab> tabPanel = new TabbedPanel(ID_TAB, createTabs(feedbackList)){
            @Override
            protected WebMarkupContainer newLink(String linkId, int index) {
                return new AjaxLink<Void>(linkId) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setSelectedTab(index);
                        ExportingPanel.this.getPageBase().getMainPopup().show(target);
                        target.add(ExportingPanel.this);
                    }
                };
            }
        };
        panel.add(tabPanel);
        PanelTab columnTabPanel = (PanelTab) tabPanel.getTabs().getObject().get(0);
        PanelTab filterTabPanel = (PanelTab) tabPanel.getTabs().getObject().get(1);

        AjaxButton createReport = new AjaxButton(ID_CREATE_REPORT,
                new StringResourceModel("ExportingPopupPanel.createReport", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                performSelectedColumns(columnTabPanel, target);
                if (exportedColumnsIndex.isEmpty()) {
                    LOGGER.warn("None columns selected");
                    getPageBase().warn("ExportingPanel.message.error.selectColumn");
                    target.add(feedbackList);
                    return;
                }
                SearchFilterType filter = null;
                if (filterTabPanel.getPanel() != null) {
                    filter = ((ExportingFilterTabPanel)filterTabPanel.getPanel()).getFilter();
                }
                createReportPerformed(filter, target);
                ((PageBase) getPage()).hideMainPopup(target);
            }
        };
        panel.add(createReport);
        AjaxButton exportSelected = new AjaxButton(ID_EXPORT,
                new StringResourceModel("ExportingPopupPanel.exportSelected", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                performSelectedColumns(columnTabPanel, target);
                if (exportedColumnsIndex.isEmpty()) {
                    LOGGER.warn("None columns selected");
                    getPageBase().warn("ExportingPanel.message.error.selectColumn");
                    target.add(feedbackList);
                    return;
                }
                ((PageBase) getPage()).hideMainPopup(target);
                exportPerformed(target);
            }
        };
        panel.add(exportSelected);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL,
                new StringResourceModel("Button.cancel", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        panel.add(cancelButton);
        add(panel);
    }

    private void performSelectedColumns(PanelTab columnTabPanel, AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<Integer>> table = (BoxedTablePanel<SelectableBean<Integer>>) columnTabPanel.getPanel();
        List<Integer> availableData = ((SelectableListDataProvider) table.getDataTable().getDataProvider()).getSelectedObjects();
        exportedColumnsIndex.addAll(availableData);
    }

    protected void createReportPerformed(SearchFilterType filter, AjaxRequestTarget target) {

    }

    private List<ITab> createTabs(FeedbackAlerts feedbackList) {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(getPageBase().createStringResource("ExportingPopupPanel.columns")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createTable(panelId, dataTable);
            }
        });
        tabs.add(new PanelTab(getPageBase().createStringResource("ExportingPopupPanel.filter")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ExportingFilterTabPanel(panelId, search, feedbackList);
            }
        });
        return tabs;
    }

    private IModel<String> getWarningMessageModel() {
        if (exportSizeLimit != null) {
            return getPageBase().createStringResource("CsvDownloadButtonPanel.confirmationMessage", exportSizeLimit);
        }
        return null;
    }

    public void exportPerformed(AjaxRequestTarget target) {

    }

    public void cancelPerformed(AjaxRequestTarget target) {
        ((PageBase) getPage()).hideMainPopup(target);
    }

    private BoxedTablePanel<SelectableBean<Integer>> createTable(String id, DataTable<?,?> dataTable) {

        List<? extends IColumn<?, ?>> allColumns = dataTable.getColumns();
        List<Integer> exportableColumnIndex = getExportableColumns(dataTable);
        if (exportableColumnIndex.isEmpty()) {
            throw new IllegalArgumentException("List of exportable columns is empty");
        }

        List<IColumn<SelectableBean<Integer>, String>> columns = new ArrayList<>();
        CheckBoxHeaderColumn<SelectableBean<Integer>> checkboxColumn = new CheckBoxHeaderColumn<>();
        columns.add(checkboxColumn);
        StringResourceModel nameString = getPageBase().createStringResource("ExportingPopupPanel.nameColumn");
        IColumn<SelectableBean<Integer>, String> nameColumn = new AbstractColumn<SelectableBean<Integer>, String>(nameString) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<Integer>>> cellItem, String componentId,
                    IModel<SelectableBean<Integer>> rowModel) {
                IModel stringModel = ((IExportableColumn)allColumns.get(rowModel.getObject().getValue())).getDisplayModel();
                cellItem.add(new Label(componentId, stringModel));
            }
        };
        columns.add(nameColumn);

        SelectableListDataProvider<SelectableBean<Integer>, Integer> provider =
                new SelectableListDataProvider<SelectableBean<Integer>, Integer>(getPageBase(), Model.ofList(exportableColumnIndex));

        BoxedTablePanel<SelectableBean<Integer>> table =
                new BoxedTablePanel<SelectableBean<Integer>>(id, provider, columns, null, 20) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new WebMarkupContainer(headerId);
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return null;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return new WebMarkupContainer(id);
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
                return true;
            }

            @Override
            public int getAutoRefreshInterval() {
                return 0;
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return false;
            }
        };
        table.setOutputMarkupId(true);

        return table;
    }

    private List<Integer> getExportableColumns(DataTable<?, ?> dataTable) {
        List<? extends IColumn<?, ?>> columns = dataTable.getColumns();
        List<Integer> exportableColumn = new ArrayList<>();
        for (IColumn column : columns) {
            if (column instanceof IExportableColumn) {
                exportableColumn.add(columns.indexOf(column));
            }
        }
        return exportableColumn;
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 550;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ExportingPopupPanel.title");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}
