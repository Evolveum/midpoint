/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * A reusable, column-based tile that renders its content using existing {@link IColumn}
 * definitions from Wicket tables. Allows tile and table to share the same column model.
 *
 * @author tchrapovic
 */
public class ColumnTileHeaderPanel<O extends Serializable> extends BasePanel<List<IColumn<O, String>>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_COLUMNS_TILE_FRAGMENT = "columnTileFragment";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_CELL = "cell";
    private static final String ID_HEADER = "header";
    private static final String ID_ICON = "icon";
    private static final String ID_HEADER_LABEL = "label";

    private static final String ID_TOOLBAR = "toolbar";

    public ColumnTileHeaderPanel(String id, IModel<List<IColumn<O, String>>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createContentFragment());
    }

    private @NotNull Component createContentFragment() {
        Fragment fragment = new Fragment(ColumnTileHeaderPanel.ID_CONTENT_CONTAINER, ID_COLUMNS_TILE_FRAGMENT, this);

        @SuppressWarnings("unchecked")
        ColumnTileTable<O> table = this.findParent(ColumnTileTable.class);

        BaseSortableDataProvider<O> provider = null;
        if (table != null && table.getProvider() != null
                && table.getProvider() instanceof BaseSortableDataProvider<O> p) {
            provider = p;
        }
        ISortStateLocator<String> sortLocator = provider;

        List<IColumn<O, String>> columns = getModelObject();
        ListView<IColumn<O, String>> columnsView = new ListView<>(ID_COLUMNS, columns) {
            @Override
            protected void populateItem(@NotNull ListItem<IColumn<O, String>> item) {
                IColumn<O, String> column = item.getModelObject();
                Item<?> cellItem = new Item<>(ID_CELL, 0);
                cellItem.setOutputMarkupId(true);

                WebMarkupContainer header;
                if (column.isSortable()) {
                    header = newSortableHeader(column.getSortProperty(), sortLocator, table);
                } else {
                    header = new WebMarkupContainer(ID_HEADER);
                }

                WebMarkupContainer icon = new WebMarkupContainer(ID_ICON) {
                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("class", getSortIconCss(column, sortLocator));
                    }
                };
                icon.setOutputMarkupId(true);
                header.add(icon);
                header.add(column.getHeader(ID_HEADER_LABEL));
                header.setRenderBodyOnly(false);

                if (column instanceof AbstractColumn<?, ?> abstractColumn) {
                    String css = abstractColumn.getCssClass();
                    cellItem.add(AttributeAppender.append("class", css != null ? css : "col"));
                }

                cellItem.add(header);
                item.add(cellItem);
            }
        };

        RepeatingView toolbar = new RepeatingView(ID_TOOLBAR);
        addToolbarButtons(toolbar);

        fragment.add(toolbar);
        fragment.add(columnsView);
        return fragment;
    }

    /**
     * Returns the icon class based on sort state.
     */
    private @NotNull String getSortIconCss(@NotNull IColumn<O, String> column, ISortStateLocator<String> locator) {
        String base = "icon fas fa-fw mr-1";
        if (column.isSortable() && locator != null) {
            ISortState<String> sortState = locator.getSortState();
            SortOrder dir = sortState.getPropertySortOrder(column.getSortProperty());
            return switch (dir) {
                case ASCENDING -> base + " fa-sort-up text-primary";
                case DESCENDING -> base + " fa-sort-down text-primary";
                default -> base + " fa-sort text-muted";
            };
        }
        return "";
    }

    protected void addToolbarButtons(RepeatingView repeatingView) {

    }

    protected WebMarkupContainer newSortableHeader(
            final String property,
            final ISortStateLocator<String> locator,
            @NotNull ColumnTileTable<O> table) {
        IDataProvider<O> provider = table.getProvider();
        if (provider instanceof BaseSortableDataProvider<O> sortableDataProvider) {
            if (sortableDataProvider.isOrderingDisabled()) {
                return new WebMarkupContainer(ID_HEADER);
            }
        }

        return new AjaxFallbackOrderByBorder<>(ID_HEADER, property, locator) {

            @Override
            protected void onSortChanged() {
                table.getTiles().setCurrentPage(0);
            }

            @Override
            protected void onAjaxClick(AjaxRequestTarget target) {
                target.add(table);
            }
        };
    }
}
