/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author lazyman
 */
public class TablePanel<T> extends Panel implements Table {

    private static final String ID_TABLE = "table";
    private static final String ID_PAGING = "paging";

    private final IModel<Boolean> showPaging = new Model<>(true);
    private final IModel<Boolean> showCount = new Model<>(true);

    private UserProfileStorage.TableId tableId;

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null, UserProfileStorage.DEFAULT_PAGING_SIZE);
    }

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, long pageSize) {
        super(id);
        Validate.notNull(provider, "Object type must not be null.");
        Validate.notNull(columns, "Columns must not be null.");

        this.tableId = tableId;

        initLayout(columns, provider, pageSize);
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider, long pageSize) {
        DataTable<T, String> table = new SelectableDataTable<>(ID_TABLE, columns, provider, (int) pageSize);

        table.setOutputMarkupId(true);

        TableHeadersToolbar<?> headers = new TableHeadersToolbar<>(table, provider);
        headers.setOutputMarkupId(true);
        table.addTopToolbar(headers);

        CountToolbar count = new CountToolbar(table) {

            @Override
            protected void pageSizeChanged(AjaxRequestTarget target) {
                PageBase page = (PageBase) getPage();
                Integer pageSize = page.getSessionStorage().getUserProfile().getPagingSize(tableId);

                setItemsPerPage(pageSize);
                target.add(getNavigatorPanel());
                target.add(getDataTable());
            }

            @Override
            protected boolean isPageSizePopupVisible() {
                return tableId != null && enableSavePageSize();
            }

        };
        count.add(new VisibleBehaviour(() -> showCount.getObject()));
        table.addBottomToolbar(count);

        add(table);

        NavigatorPanel nb2 = new NavigatorPanel(ID_PAGING, table, showPagedPagingModel(provider));
        nb2.add(new VisibleBehaviour(() -> showPaging.getObject()));
        add(nb2);
    }

    @Override
    public UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    @Override
    public boolean enableSavePageSize() {
        return true;
    }

    private IModel<Boolean> showPagedPagingModel(ISortableDataProvider provider) {
        if (!(provider instanceof BaseSortableDataProvider)) {
            return () -> true;
        }

        BaseSortableDataProvider<?> baseProvider = (BaseSortableDataProvider<?>) provider;
        return baseProvider.isSizeAvailableModel();
    }

    @Override
    public DataTable<T, ?> getDataTable() {
        return (DataTable<T, ?>) get(ID_TABLE);
    }

    public NavigatorPanel getNavigatorPanel() {
        return (NavigatorPanel) get(ID_PAGING);
    }

    @Override
    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    @Override
    public int getItemsPerPage() {
        return (int) getDataTable().getItemsPerPage();
    }

    @Override
    public void setCurrentPage(long page) {
        getDataTable().setCurrentPage(page);
    }

    @Deprecated
    @Override
    public void setCurrentPageAndSort(ObjectPaging paging) {
        WebComponentUtil.setCurrentPage(this, paging);
    }

    @Override
    public void setShowPaging(boolean showPaging) {
        this.showPaging.setObject(showPaging);
        this.showCount.setObject(showPaging);

        if (!showPaging) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(10);
        }
    }

    public void setShowCount(boolean showCount) {
        this.showCount.setObject(showCount);
    }

    public void setTableCssClass(String cssClass) {
        Validate.notEmpty(cssClass, "Css class must not be null or empty.");

        DataTable<?, ?> table = getDataTable();
        table.add(new AttributeAppender("class", new Model<>(cssClass), " "));
    }

    public void setStyle(String value) {
        Validate.notEmpty(value, "Value must not be null or empty.");

        DataTable<?, ?> table = getDataTable();
        table.add(new AttributeModifier("style", new Model<>(value)));
    }
}
