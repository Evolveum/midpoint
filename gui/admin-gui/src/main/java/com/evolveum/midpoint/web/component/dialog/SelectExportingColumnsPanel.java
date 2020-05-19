/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import java.util.ArrayList;
import java.util.List;

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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lskublik
 */
public class SelectExportingColumnsPanel extends BasePanel implements Popupable {

    private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "panel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_EXPORT_ALL = "exportAll";
    private static final String ID_EXPORT_SELECTED = "exportSelected";
    private static final String ID_CANCEL = "cancelButton";
    private static final String ID_TABLE = "table";

    private DataTable<?,?> dataTable;
    private List<Integer> exportedColumnsIndex;
    private Long exportSizeLimit;

    public SelectExportingColumnsPanel(String id, DataTable<?,?> dataTable, List<Integer> exportedColumnsIndex) {
        this(id, dataTable, exportedColumnsIndex, null);
    }

    public SelectExportingColumnsPanel(String id, DataTable<?,?> dataTable, List<Integer> exportedColumnsIndex,
            Long exportSizeLimit) {
        super(id);
        this.dataTable = dataTable;
        this.exportedColumnsIndex = exportedColumnsIndex;
        this.exportSizeLimit = exportSizeLimit;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout(dataTable, exportedColumnsIndex, exportSizeLimit);
    }

    private void initLayout(DataTable<?,?> dataTable, List<Integer> exportedColumnsIndex, Long exportSizeLimit) {
        WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);

        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        panel.add(warningMessage);

        BoxedTablePanel<SelectableBean<Integer>> table = createTable(dataTable);
        panel.add(table);

        AjaxButton exportAll = new AjaxButton(ID_EXPORT_ALL,
                new StringResourceModel("SelectExportingColumnsPanel.exportAll", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<Integer> availableData = ((SelectableListDataProvider) table.getDataTable().getDataProvider()).getAvailableData();
                exportedColumnsIndex.addAll(availableData);
                ((PageBase) getPage()).hideMainPopup(target);
                exportPerformed(target);
            }
        };
        panel.add(exportAll);
        AjaxButton exportSelected = new AjaxButton(ID_EXPORT_SELECTED,
                new StringResourceModel("SelectExportingColumnsPanel.exportSelected", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<Integer> availableData = ((SelectableListDataProvider) table.getDataTable().getDataProvider()).getSelectedObjects();
                exportedColumnsIndex.addAll(availableData);
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

    private BoxedTablePanel<SelectableBean<Integer>> createTable(DataTable<?,?> dataTable) {

        List<? extends IColumn<?, ?>> allColumns = dataTable.getColumns();
        List<Integer> exportableColumnIndex = getExportableColumns(dataTable);
        if (exportableColumnIndex.isEmpty()) {
            throw new IllegalArgumentException("List of exportable columns is empty");
        }

        List<IColumn<SelectableBean<Integer>, String>> columns = new ArrayList<>();
        CheckBoxHeaderColumn<SelectableBean<Integer>> checkboxColumn = new CheckBoxHeaderColumn<>();
        columns.add(checkboxColumn);
        StringResourceModel nameString = getPageBase().createStringResource("SelectExportingColumnsPanel.nameColumn");
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
                new BoxedTablePanel<SelectableBean<Integer>>(ID_TABLE, provider, columns, null, 20) {
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
        return 500;
    }

    @Override
    public int getHeight() {
        int rows = getExportableColumns(dataTable).size();
        int message = 0;
        if (exportSizeLimit != null) {
            message = 60;
        }
        int height = (rows * 40) + 130 + message;
        return height > 500 ? 500 : height;
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
        return new StringResourceModel("SelectExportingColumnsPanel.title");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}
