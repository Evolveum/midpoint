/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

public abstract class ListItemWithPanelForItemPanel<IT extends SelectableRow> extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_DEFINITION = "searchDefinition";
    private static final String ID_NEW_VALUE_BUTTON = "newValueButton";
    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST_OF_VALUES = "listOfValues";
    private static final String ID_VALUE = "value";
    private static final String ID_PANEL_FOR_ITEM = "panelForItem";

    private IModel<IT> typeValueModel;
    private LoadableDetachableModel<List<IT>> valuesModel;
    private final IModel<String> searchItemModel = Model.of();

    public ListItemWithPanelForItemPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initValueModels();
        initLayout();
    }

    public IModel<String> getSearchItemModel() {
        return searchItemModel;
    }

    private void initValueModels() {

        if (valuesModel == null) {
            valuesModel = new LoadableDetachableModel<>() {
                @Override
                protected List<IT> load() {
                    return createListOfItem(getSearchItemModel());
                }
            };
        }

        selectOneValueOfItem();

        if (typeValueModel == null) {
            typeValueModel = new LoadableDetachableModel<>() {
                @Override
                protected IT load() {
                    return getSelectedItem();
                }
            };
        }
    }

    protected abstract IT getSelectedItem();

    protected abstract List<IT> createListOfItem(IModel<String> searchItemModel);

    private void selectOneValueOfItem() {
        if (isNoAnyDefinitionSelected()) {
            valuesModel.getObject().get(0).setSelected(true);
        }
    }

    private boolean isNoAnyDefinitionSelected() {
        return !valuesModel.getObject().isEmpty() && !valuesModel.getObject().stream().anyMatch(SelectableRow::isSelected);
    }

    private void initLayout() {

        TextField defSearch = new TextField<>(ID_SEARCH_DEFINITION, searchItemModel);
        defSearch.setOutputMarkupId(true);
        defSearch.add(new AjaxFormComponentUpdatingBehavior("keyup") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                valuesModel.detach();
                if (isNoAnyDefinitionSelected()) {
                    typeValueModel.detach();
                    unselectAllDefinitionValues();
                    selectOneValueOfItem();
                }
                addPanelForItem();
                target.add(ListItemWithPanelForItemPanel.this.get(ID_LIST_CONTAINER));
                target.add(ListItemWithPanelForItemPanel.this.get(ID_PANEL_FOR_ITEM));
            }
        });
        defSearch.add(new VisibleBehaviour(ListItemWithPanelForItemPanel.this::isSearchForItemVisible));
        add(defSearch);

        AjaxIconButton newValueButton = new AjaxIconButton(
                ID_NEW_VALUE_BUTTON,
                Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE),
                getLabelForNewItem()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickNewDefinitionType(target);
            }
        };
        newValueButton.showTitleAsLabel(true);
        newValueButton.add(new VisibleBehaviour(ListItemWithPanelForItemPanel.this::isNewItemButtonVisible));
        add(newValueButton);

        createValueList();
        addPanelForItem();
    }

    protected IModel<String> getLabelForNewItem() {
        return createStringResource("ComplexTypeDefinitionPanel.createNewValue");
    }

    protected boolean isSearchForItemVisible() {
        return true;
    }

    protected boolean isNewItemButtonVisible() {
        return true;
    }

    protected void unselectAllDefinitionValues() {
        valuesModel.getObject().forEach(item -> item.setSelected(false));
    }

    private void addPanelForItem() {

        WebMarkupContainer panelForItem = createPanelForItem(ID_PANEL_FOR_ITEM, getSelectedItemModel());
        panelForItem.setOutputMarkupId(true);
        addOrReplace(panelForItem);
    }

    protected abstract WebMarkupContainer createPanelForItem(String idPanelForItem, IModel<IT> selectedItemModel);

    protected void onClickNewDefinitionType(AjaxRequestTarget target) {
    }

    private void createValueList() {
        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);

        ListView values = new ListView<>(ID_LIST_OF_VALUES, valuesModel) {
            @Override
            protected void populateItem(ListItem<IT> item) {
                LoadableDetachableModel<String> labelModel = createItemLabel(item.getModel());

                IModel<String> iconModel = createItemIcon(item.getModel());

                AjaxIconButton value = new AjaxIconButton(ID_VALUE, iconModel, labelModel) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        clickOnListItem(valuesModel, item, target);
                    }
                };
                value.add(AttributeAppender.append("class", () -> item.getModelObject().isSelected() ? "active-second-level" : ""));
                value.showTitleAsLabel(true);
                item.add(value);
            }
        };
        values.setOutputMarkupId(true);
        listContainer.add(values);
    }

    protected abstract IModel<String> createItemIcon(IModel<IT> model);

    protected abstract LoadableDetachableModel<String> createItemLabel(IModel<IT> model);

    protected void clickOnListItem(LoadableDetachableModel<List<IT>> valuesModel, ListItem<IT> item, AjaxRequestTarget target) {
        valuesModel.getObject().forEach(value -> value.setSelected(false));
        item.getModelObject().setSelected(true);
        typeValueModel.detach();
        addPanelForItem();
        target.add(ListItemWithPanelForItemPanel.this.get(ID_LIST_CONTAINER));
        target.add(ListItemWithPanelForItemPanel.this.get(ID_PANEL_FOR_ITEM));
    }

    protected IModel<IT> getSelectedItemModel() {
        return () -> typeValueModel.getObject();
    }

    public WebMarkupContainer getPanelForItem() {
        return (WebMarkupContainer) get(ID_PANEL_FOR_ITEM);
    }
}
