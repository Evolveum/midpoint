/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web.component.data.table;

import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

/**
 * Specialized boxed table panel that uses {@link CollapsableDataTable}
 * to provide per-row expandable (collapsible) content.
 */
public class CollapsibleBoxedTablePanel<T> extends BoxedTablePanel<T> {

    public CollapsibleBoxedTablePanel(String id, ISortableDataProvider<T, String> provider, List<IColumn<T, String>> iColumns) {
        super(id, provider, iColumns);
    }

    public CollapsibleBoxedTablePanel(String id, ISortableDataProvider<T, String> provider, List<IColumn<T, String>> iColumns,
                                      UserProfileStorage.TableId tableId) {
        super(id, provider, iColumns, tableId);
    }

    @Override
    protected @NotNull DataTable<T, String> createDataTableComponent(
            List<IColumn<T, String>> columns,
            ISortableDataProvider<T, String> provider,
            int pageSize,
            @NotNull WebMarkupContainer tableContainer) {

        DataTable<T, String> table = new CollapsableDataTable<>(BoxedTablePanel.ID_TABLE, columns, provider, pageSize) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Item<T> newRowItem(String id, int index, IModel<T> rowModel) {
                Item<T> item = super.newRowItem(id, index, rowModel);
                return customizeNewRowItem(item, rowModel);
            }

            @Override
            public Component createCollapsibleContent(String id, IModel<T> rowModel) {
                return CollapsibleBoxedTablePanel.this.createCollapsibleContent(id, rowModel);
            }

            @Override
            protected void onPageChanged() {
                super.onPageChanged();
                CollapsibleBoxedTablePanel.this.onPageChanged();
            }
        };

        table.setOutputMarkupId(true);
        table.add(AttributeAppender.append("class", this::getTableAdditionalCssClasses));
        table.add(new VisibleBehaviour(this::isDataTableVisible));
        tableContainer.add(table);

        return table;
    }

    /**
     * Override this method to define the content that will be shown
     * when a row is expanded.
     */
    protected Component createCollapsibleContent(String id, @NotNull IModel<T> rowModel) {
        Label label = new Label(id, createStringResource("CollapsibleToggleColumn.nothingToShow"));
        label.add(AttributeAppender.append("class", "p-3 d-flex justify-content-center align-items-center"));
        return label;
    }

    @Override
    protected void customizeColumns(@NotNull List<IColumn<T, String>> iColumns) {
        if (isCollapseToggleColumnVisible()) {
            iColumns.add(0, new CollapsibleToggleColumn<>());
        }
    }

    protected boolean isCollapseToggleColumnVisible() {
        return true;
    }
}
