/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.SelectableRow;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

public abstract class MultiSelectTileTablePanel<E extends Serializable, O extends SelectableRow, T extends Tile>
        extends SingleSelectTileTablePanel<O, T> {

    protected static final String ID_SELECTED_ITEMS_CONTAINER = "selectedItemsContainer";
    private static final String ID_SELECTED_ITEM_CONTAINER = "selectedItemContainer";
    private static final String ID_SELECTED_ITEM = "selectedItem";
    private static final String ID_DESELECT_BUTTON = "deselectButton";

    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";


    public MultiSelectTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId) {
        this(id, Model.of(ViewToggle.TILE), tableId);
    }

    public MultiSelectTileTablePanel(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId) {
        super(id, viewToggle, tableId);
    }

    @Override
    protected Component createToolbarButtons(String id) {
        ButtonBar<Containerable, SelectableRow<?>> buttonBar = new ButtonBar<>(id, ID_BUTTON_BAR,
                MultiSelectTileTablePanel.this, createToolbarButtonsList(ID_BUTTON));
        buttonBar.setOutputMarkupId(true);
        buttonBar.add(new VisibleBehaviour(this::idToolbarButtonsVisible));
        return buttonBar;
    }

    @Override
    protected Fragment createHeaderFragment(String id) {
        Fragment headerFragment = super.createHeaderFragment(id);

        headerFragment.add(AttributeAppender.replace("class", ""));

        WebMarkupContainer selectedItemsContainer = new WebMarkupContainer(ID_SELECTED_ITEMS_CONTAINER);
        selectedItemsContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(new VisibleBehaviour(this::isSelectedItemsPanelVisible));
        headerFragment.add(selectedItemsContainer);

        ListView<E> selectedContainer = new ListView<>(
                ID_SELECTED_ITEM_CONTAINER,
                getSelectedItemsModel()) {

            @Override
            protected void populateItem(ListItem<E> item) {
                E entry = item.getModelObject();

                item.add(new Label(ID_SELECTED_ITEM, getItemLabelModel(entry)));
                AjaxButton deselectButton = new AjaxButton(ID_DESELECT_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deselectItem(entry);
                        refresh(target);
                    }
                };
                item.add(deselectButton);
            }
        };
        selectedContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(selectedContainer);
        return headerFragment;
    }

    protected boolean isSelectedItemsPanelVisible() {
        return true;
    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        super.refresh(target);
        if (isSelectedItemsPanelVisible()) {
            target.add(getSelectedItemPanel());
        }
    }

    protected Component getSelectedItemPanel() {
        if (isTileViewVisible()) {
            return get(createComponentPath(ID_TILE_VIEW, ID_HEADER, ID_SELECTED_ITEMS_CONTAINER));
        }
        return get(createComponentPath(ID_TABLE, ID_HEADER, ID_SELECTED_ITEMS_CONTAINER));
    }

    protected abstract void deselectItem(E entry);

    protected abstract IModel<String> getItemLabelModel(E entry);

    protected abstract IModel<List<E>> getSelectedItemsModel();

    void onSelectTableRow(IModel<O> model, AjaxRequestTarget target) {
        boolean oldState = model.getObject().isSelected();

        model.getObject().setSelected(!oldState);
        processSelectOrDeselectItem(model.getObject(), getProvider(), target);

        refresh(target);
    }

    protected void processSelectOrDeselectItem(O value, ISortableDataProvider<O, String> provider, AjaxRequestTarget target) {
    }

    protected List<Component> createToolbarButtonsList(String idButton) {
        return new ArrayList<>();
    }

    protected boolean idToolbarButtonsVisible() {
        return true;
    }
}
