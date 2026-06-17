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
 * Header panel for {@link ColumnTileTable} that renders shared column headers for tile-based layouts.
 *
 * <p>The panel is generic over:
 * <ul>
 *   <li><b>O</b> - the logical row object managed by the tile table</li>
 *   <li><b>PV</b> - the delegated value type rendered by individual columns</li>
 * </ul>
 *
 * <p>This mirrors the type split used by {@link ColumnTileTable}, allowing one tile row object
 * to expose another value type for reusable column rendering.
 */
public class ColumnTileHeaderPanel<O extends ColumnValueProvider<PV>, PV extends Serializable>
        extends BasePanel<List<IColumn<PV, String>>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_COLUMNS_TILE_FRAGMENT = "columnTileFragment";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_CELL = "cell";
    private static final String ID_HEADER = "header";
    private static final String ID_ICON = "icon";
    private static final String ID_HEADER_LABEL = "label";

    private static final String ID_TOOLBAR = "toolbar";
    boolean isSortingSupported = true;

    public ColumnTileHeaderPanel(String id, IModel<List<IColumn<PV, String>>> model) {
        super(id, model);
    }

    public ColumnTileHeaderPanel(String id, IModel<List<IColumn<PV, String>>> model, boolean isSortingSupported) {
        super(id, model);
        this.isSortingSupported = isSortingSupported;
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
        Fragment fragment = new Fragment(ID_CONTENT_CONTAINER, ID_COLUMNS_TILE_FRAGMENT, this);

        @SuppressWarnings("unchecked")
        ColumnTileTable<O, PV> table = (ColumnTileTable<O, PV>) findParent(ColumnTileTable.class);

        BaseSortableDataProvider<O> provider = null;
        if (table != null && table.getProvider() instanceof BaseSortableDataProvider<?> p) {
            @SuppressWarnings("unchecked")
            BaseSortableDataProvider<O> casted = (BaseSortableDataProvider<O>) p;
            provider = casted;
        }

        ISortStateLocator<String> sortLocator = provider;

        List<IColumn<PV, String>> columns = getModelObject();
        ListView<IColumn<PV, String>> columnsView = new ListView<>(ID_COLUMNS, columns) {
            @Override
            protected void populateItem(@NotNull ListItem<IColumn<PV, String>> item) {
                IColumn<PV, String> column = item.getModelObject();
                Item<?> cellItem = new Item<>(ID_CELL, item.getIndex());
                cellItem.setOutputMarkupId(true);

                WebMarkupContainer header;
                if (column.isSortable() && table != null && isSortingSupported) {
                    header = newSortableHeader(column.getSortProperty(), sortLocator, table);
                } else {
                    header = new WebMarkupContainer(ID_HEADER);
                }

                WebMarkupContainer icon = new WebMarkupContainer(ID_ICON) {
                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (column.isSortable() && table != null && isSortingSupported) {
                            super.onComponentTag(tag);
                            String css = getSortIconCss(column, sortLocator);
                            if (!css.isEmpty()) {
                                tag.put("class", css);
                            }
                        }
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

                hideSpecificHeaderIfNeeded(column, header);
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
    private @NotNull String getSortIconCss(@NotNull IColumn<PV, String> column, ISortStateLocator<String> locator) {
        String base = "icon fas fa-fw me-1";
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
            @NotNull ColumnTileTable<O, PV> table) {
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
                table.refreshAndDetach(target);
            }
        };
    }

    protected void hideSpecificHeaderIfNeeded(IColumn<PV, String> column, WebMarkupContainer header) {
        // By default, all headers are shown. Override this method to hide specific headers based on column type or properties.
    }
}
