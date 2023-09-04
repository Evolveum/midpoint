/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.web.component.data.SelectableDataTable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.util.visit.IVisitor;

/**
 * @author lskublik
 */
public class ExportingPanel extends BasePanel<ExportingPanel> implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(ExportingPanel.class);

    private static final long serialVersionUID = 1L;
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_EXPORT = "export";
    private static final String ID_CANCEL = "cancelButton";
    private static final String ID_TABLE = "table";
    private static final String ID_NAME = "name";

    private final DataTable<?, ?> dataTable;
    private final List<Integer> exportedColumnsIndex;
    private final Long exportSizeLimit;
    private final IModel<String> nameModel;

    public ExportingPanel(String id, DataTable<?, ?> dataTable, List<Integer> exportedColumnsIndex,
            Long exportSizeLimit, IModel<String> name) {
        super(id);
        this.dataTable = dataTable;
        this.exportedColumnsIndex = exportedColumnsIndex;
        this.exportSizeLimit = exportSizeLimit;
        this.nameModel = name;
        nameModel.setObject("");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MidpointForm form = new MidpointForm<>(ID_MAIN_FORM, true);

        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        form.add(warningMessage);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        form.add(feedbackList);

        TextPanel<String> nameField = new TextPanel<>(ID_NAME, nameModel);
        form.add(nameField);

        BoxedTablePanel<SelectableBean<Integer>> table = createTable(ID_TABLE, dataTable);
        form.add(table);

        AjaxSubmitButton exportSelected = new AjaxSubmitButton(ID_EXPORT, getPageBase().createStringResource("ExportingPopupPanel.exportSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                performSelectedColumns(table);
                if (exportedColumnsIndex.isEmpty()) {
                    LOGGER.warn("None columns selected");
                    getPageBase().warn(getPageBase().createStringResource("ExportingPanel.message.error.selectColumn").getString());
                    target.add(feedbackList);
                    return;
                }
                ((PageBase) getPage()).hideMainPopup(target);
                exportPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        form.add(exportSelected);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL,
                new StringResourceModel("Button.cancel", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        form.add(cancelButton);
        add(form);
    }

    private void performSelectedColumns(BoxedTablePanel<SelectableBean<Integer>> table) {
        exportedColumnsIndex.clear();

        table.getDataTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem<SelectableBean<Integer>>, Void>) (row, visit) -> {
            if (row.getModelObject().isSelected()) {
                exportedColumnsIndex.add(row.getModelObject().getValue());
            }
        });
//        exportedColumnsIndex.addAll(availableData);
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

    private BoxedTablePanel<SelectableBean<Integer>> createTable(String id, DataTable<?, ?> dataTable) {

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
                IModel stringModel = ((IExportableColumn) allColumns.get(rowModel.getObject().getValue())).getDisplayModel();
                cellItem.add(new Label(componentId, stringModel));
            }
        };
        columns.add(nameColumn);

        SelectableListDataProvider<SelectableBean<Integer>, Integer> provider =
                new SelectableListDataProvider<SelectableBean<Integer>, Integer>(getPageBase(), Model.ofList(exportableColumnIndex));

        BoxedTablePanel<SelectableBean<Integer>> table =
                new BoxedTablePanel<SelectableBean<Integer>>(id, provider, columns) {
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
                    protected boolean hideFooterIfSinglePage() {
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
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ExportingPopupPanel.title");
    }
}
