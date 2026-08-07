/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.isObjectSelected;
import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.setColumnTileSelected;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
//TODO abstract grouping tile for ColumnTileTable
/**
 * Tile panel for grouped rows that renders multiple delegated values inside one logical tile.
 *
 * <p>The panel works with:
 * <ul>
 *   <li><b>O</b> - logical tile row object, typically a grouped DTO</li>
 *   <li><b>PV</b> - delegated value rendered by existing column definitions</li>
 * </ul>
 *
 * <p>Each value returned by {@link ColumnValueProvider#getColumnsValues()} is rendered
 * as one inner row using the shared column model.
 */
public class GroupColumnTilePanel<
        O extends ColumnValueProvider<PV>,
        PV extends Serializable,
        T extends ColumnTile<O, PV>>
        extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_COLUMNS_TILE_FRAGMENT = "columnTileFragment";
    private static final String ID_ROWS = "rows";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_CELL = "cell";
    private static final String ID_COMPONENT = "component";
    private static final String ID_TOOLBAR = "toolbar";

    public GroupColumnTilePanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", getDefaultPanelCss()));
        setOutputMarkupId(true);
        add(createContentFragment(ID_CONTENT_CONTAINER));
    }

    public Component createContentFragment(String id) {
        Fragment fragment = new Fragment(id, ID_COLUMNS_TILE_FRAGMENT, this);

        O value = getModelObject().getValue();
        List<PV> columnValues = value != null ? value.getColumnsValues() : List.of();
        List<IColumn<PV, String>> columns = getModelObject().getColumns();

        RepeatingView toolbar = new RepeatingView(ID_TOOLBAR);
        fragment.add(toolbar);
        addToolbarButtons(toolbar);

        ListView<PV> rowsView = new ListView<>(ID_ROWS, columnValues) {
            @Override
            protected void populateItem(@NotNull ListItem<PV> rowItem) {
                PV rowValue = rowItem.getModelObject();
                IModel<PV> rowModel = Model.of(rowValue);

                ListView<IColumn<PV, String>> columnsView = new ListView<>(ID_COLUMNS, columns) {
                    @Override
                    protected void populateItem(@NotNull ListItem<IColumn<PV, String>> item) {
                        IColumn<PV, String> column = item.getModelObject();
                        Item<ICellPopulator<PV>> cellItem = new Item<>(ID_CELL, item.getIndex());
                        cellItem.setOutputMarkupId(true);
                        column.populateItem(cellItem, ID_COMPONENT, rowModel);

                        if (column instanceof AbstractColumn<?, ?> abstractColumn) {
                            String css = abstractColumn.getCssClass();
                            cellItem.add(AttributeAppender.append("class", css != null ? css : "col"));
                        }

                        item.add(cellItem);
                    }
                };

                columnsView.setRenderBodyOnly(true);
                columnsView.setOutputMarkupId(true);
                rowItem.add(columnsView);
            }
        };

        rowsView.setOutputMarkupId(true);
        fragment.add(rowsView);

        return fragment;
    }

    protected void addToolbarButtons(@NotNull RepeatingView repeatingView) {
        if (isCheckboxVisible()) {
            initCheckBoxPanel(repeatingView);
        }
    }

    private void initCheckBoxPanel(@NotNull RepeatingView repeatingView) {
        IModel<Boolean> selectedModel = new IModel<>() {
            @Override
            public @NotNull Boolean getObject() {
                return isObjectSelected(getModelObject().getValue());
            }

            @Override
            public void setObject(Boolean value) {
                setColumnTileSelected(getModelObject().getValue(), Boolean.TRUE.equals(value));
            }
        };

        IsolatedCheckBoxPanel checkBox = buildCheckBoxPanel(repeatingView, selectedModel);
        repeatingView.add(checkBox);
    }

    private @NotNull IsolatedCheckBoxPanel buildCheckBoxPanel(
            @NotNull RepeatingView repeatingView,
            IModel<Boolean> selectedModel) {
        IsolatedCheckBoxPanel checkBox = new IsolatedCheckBoxPanel(repeatingView.newChildId(), selectedModel) {
            @Override
            public void onUpdate(@NotNull AjaxRequestTarget target) {
                Component component = GroupColumnTilePanel.this.findParent(ColumnTileTable.class);
                target.add(Objects.requireNonNullElse(component, GroupColumnTilePanel.this));

                component = GroupColumnTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, GroupColumnTilePanel.this));
            }
        };
        checkBox.setOutputMarkupId(true);
        return checkBox;
    }

    protected boolean isCheckboxVisible() {
        return true;
    }

    protected @NotNull String getDefaultPanelCss() {
        return "card shadow-sm col-12 py-1 px-3 m-0";
    }
}
