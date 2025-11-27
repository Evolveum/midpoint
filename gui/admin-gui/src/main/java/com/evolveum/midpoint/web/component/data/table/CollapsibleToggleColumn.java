/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.table;

import com.evolveum.midpoint.web.component.AjaxIconButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * Column that displays an expand/collapse toggle icon for each row
 * in a {@link CollapsableDataTable}. When clicked, it toggles the
 * row’s expanded state and refreshes only that row.
 */
public class CollapsibleToggleColumn<T> extends AbstractColumn<T, String> {

    public CollapsibleToggleColumn() {
        super(Model.of());
    }

    public CollapsibleToggleColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(@NotNull Item<ICellPopulator<T>> cellItem,
            String componentId,
            IModel<T> rowModel) {

        AjaxIconButton toggleButton = new AjaxIconButton(componentId, getIconModel(cellItem), getTitleModel(cellItem)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                CollapsableDataTable<?, ?>.CollapsableRowItem rowItem = findParent(CollapsableDataTable.CollapsableRowItem.class);
                if (rowItem != null) {
                    rowItem.toggle(target);
                    target.add(rowItem);
                }
                target.add(this);
            }
        };

        toggleButton.showTitleAsLabel(false);
        toggleButton.setOutputMarkupId(true);
        toggleButton.setOutputMarkupPlaceholderTag(true);
        cellItem.add(toggleButton);
    }

    /** Title (tooltip) text */
    protected IModel<String> getTitleModel(Item<ICellPopulator<T>> cellItem) {
        return createStringResource("CollapsibleToggleColumn.title.expand." + isRowExpanded(cellItem));
    }

    /** Icon model (chevron direction) */
    protected IModel<String> getIconModel(Item<ICellPopulator<T>> cellItem) {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return isRowExpanded(cellItem)
                        ? "fa fa-chevron-down"
                        : "fa fa-chevron-right";
            }
        };
    }

    private boolean isRowExpanded(@NotNull Item<ICellPopulator<T>> cellItem) {
        CollapsableDataTable<?, ?>.CollapsableRowItem rowItem =
                cellItem.findParent(CollapsableDataTable.CollapsableRowItem.class);
        return rowItem != null && rowItem.isExpanded();
    }

    @Override
    public String getCssClass() {
        return "icon align-middle";
    }
}
