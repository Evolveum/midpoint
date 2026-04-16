/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;

import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;

import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

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
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.isObjectSelected;
import static com.evolveum.midpoint.gui.impl.component.tile.column.ColumnTileTable.setColumnTileSelected;

/**
 * Reusable tile panel that renders one logical row using standard Wicket table columns.
 *
 * <p>The panel works with two types:
 * <ul>
 *   <li><b>O</b> - the tile row object</li>
 *   <li><b>PV</b> - the delegated value rendered by the tile columns</li>
 * </ul>
 *
 * <p>This allows a tile row to be a DTO or grouped object, while still rendering
 * existing column definitions built for another value type.
 */
public class ColumnTilePanel<
        O extends ColumnValueProvider<PV>,
        PV extends Serializable,
        T extends ColumnTile<O, PV>>
        extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT_CONTAINER = "contentContainer";
    private static final String ID_COLUMNS_TILE_FRAGMENT = "columnTileFragment";
    private static final String ID_COLUMNS = "columns";
    private static final String ID_CELL = "cell";
    private static final String ID_COMPONENT = "component";

    private static final String ID_TOOLBAR = "toolbar";
    IModel<PV> rowModel;

    public ColumnTilePanel(String id, IModel<T> model, IModel<PV> rowModel) {
        super(id, model);
        this.rowModel = rowModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        this.add(AttributeAppender.append("class", getDefaultPanelCss()));
        this.setOutputMarkupId(true);
        add(createContentFragment(ID_CONTENT_CONTAINER));
    }

    /**
     * Creates a fragment containing the rendered column content for this tile.
     */
    public Component createContentFragment(String id) {
        Fragment fragment = new Fragment(id, ID_COLUMNS_TILE_FRAGMENT, this);

        List<IColumn<PV, String>> columns = getModelObject().getColumns();
        ListView<IColumn<PV, String>> columnsView = new ListView<>(ID_COLUMNS, columns) {
            @Override
            protected void populateItem(@NotNull ListItem<IColumn<PV, String>> item) {

                IColumn<PV, String> column = item.getModelObject();

                if (!isInlineMenuButtonVisible()) {
                    hideButtonInlineMenus(column);
                }

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

        RepeatingView toolbar = new RepeatingView(ID_TOOLBAR);
        fragment.add(toolbar);
        addToolbarButtons(toolbar);

        columnsView.setOutputMarkupId(true);
        columnsView.setRenderBodyOnly(true);
        fragment.add(columnsView);

        return fragment;
    }

    protected boolean isInlineMenuButtonVisible() {
        return true;
    }

    private static <PV extends Serializable> void hideButtonInlineMenus(IColumn<PV, String> column) {
        if (column instanceof InlineMenuButtonColumn<PV> inlineMenuButtonColumn) {
            for (InlineMenuItem menuItem : inlineMenuButtonColumn.menuItems) {
                if (menuItem instanceof ButtonInlineMenuItem) {
                    menuItem.setVisible(() -> false);
                }
            }
        }
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
                Component component = ColumnTilePanel.this.findParent(ColumnTileTable.class);
                target.add(Objects.requireNonNullElse(component, ColumnTilePanel.this));

                component = ColumnTilePanel.this.findParent(MultiSelectContainerActionTileTablePanel.class);
                target.add(Objects.requireNonNullElse(component, ColumnTilePanel.this));
            }

        };
        checkBox.setOutputMarkupId(true);
        return checkBox;
    }

    protected boolean isCheckboxVisible() {
        return true;
    }

    protected @NotNull String getDefaultPanelCss() {
        return "card col-12 py-1 px-3 m-0";
    }
}
