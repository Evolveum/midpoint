/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.button;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.panel.Popover;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Popupable panel with listed items which can be searched and selected
 * The component can be used as More button popup or saved searches popup on search panel
 */
public abstract class SelectableItemListPopoverPanel<T extends FilterableSearchItemWrapper> extends BasePanel<List<T>> {

    private static final long serialVersionUID = 1L;
    private static final String ID_POPOVER = "popover";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_ITEM_LIST = "itemList";
    private static final String ID_ITEMS_TITLE = "itemsTitle";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM_CHECKBOX = "itemCheckbox";
    private static final String ID_ITEM_LINK = "itemLink";
    private static final String ID_ITEM_NAME = "itemName";
    private static final String ID_ITEM_HELP = "itemHelp";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_CLOSE_BUTTON = "closeButton";

    public SelectableItemListPopoverPanel(String id, IModel<List<T>> popupItemModel) {
        super(id, popupItemModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Popover popover = new Popover(ID_POPOVER) {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getPopoverReferenceComponent() {
                return SelectableItemListPopoverPanel.this.getPopoverReferenceComponent();
            }

            @Override
            protected String getArrowCustomStyle() {
                return getPopoverCustomArrowStyle();
            }
        };
        add(popover);

        final WebMarkupContainer propList = new WebMarkupContainer(ID_ITEM_LIST);
        propList.setOutputMarkupId(true);
        popover.add(propList);

        Label itemsTitle = new Label(ID_ITEMS_TITLE, getPopoverTitleModel());
        itemsTitle.setOutputMarkupId(true);
        itemsTitle.add(new VisibleBehaviour(() -> getPopoverTitleModel() != null));
        propList.add(itemsTitle);

        IModel<String> searchTextModel = Model.of("");
        TextField<?> addText = new TextField<>(ID_SEARCH_TEXT, searchTextModel);
        addText.add(WebComponentUtil.preventSubmitOnEnterKeyDownBehavior());

        popover.add(addText);
        addText.add(new AjaxFormComponentUpdatingBehavior("keyup") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(propList);
            }
        });
        popover.add(addText);

        ListView<T> properties = new ListView<>(ID_ITEMS, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<T> item) {
                CheckBox check = new CheckBox(ID_ITEM_CHECKBOX,
                        new PropertyModel<>(item.getModel(), FilterableSearchItemWrapper.F_SELECTED));
                check.add(new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        //nothing, just update model.
                    }
                });
                check.add(new VisibleBehaviour(() -> (item.getModelObject() instanceof SelectableRow)));
                item.add(check);

                AjaxLink<Void> propLink = new AjaxLink<Void>(ID_ITEM_LINK) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (item.getModelObject() instanceof SelectableRow) {
                            ((SelectableRow) item.getModelObject()).setSelected(true);
                        }
                        addItemsPerformed(Arrays.asList(item.getModelObject()), target);
                    }
                };
                item.add(propLink);

                Label name = new Label(ID_ITEM_NAME, () -> getItemName(item.getModelObject()));
                propLink.add(name);
                propLink.setOutputMarkupId(true);

                check.add(AttributeModifier.append("aria-labelledby", name.getMarkupId()));

                Label help = new Label(ID_ITEM_HELP);
                String helpText = getItemHelp(item.getModelObject()) != null ? getItemHelp(item.getModelObject()) : "";
                help.add(AttributeModifier.replace("title", createStringResource(helpText)));
                help.add(new InfoTooltipBehavior() {
                    @Override
                    public String getDataPlacement() {
                        return "left";
                    }
                });
                help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpText)));
                help.add(AttributeAppender.append(
                        "aria-label",
                        getParentPage().createStringResource(
                                "SelectableItemListPopoverPanel.helpTooltip",
                                getItemName(item.getModelObject()))));
                item.add(help);

                item.add(new VisibleBehaviour(() -> !(item.getModelObject() instanceof AbstractRoleSearchItemWrapper)
                        && !item.getModelObject().isVisible()
                        && isPropertyItemVisible(getItemName(item.getModelObject()), searchTextModel.getObject())));
            }

            private boolean isPropertyItemVisible(String itemName, String propertySearchText) {
                String name = itemName != null ? createStringResource(itemName).getString() : "";
                return StringUtils.isEmpty(propertySearchText)
                        || name.toLowerCase().contains(propertySearchText.toLowerCase());
            }
        };
        propList.add(properties);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON, createStringResource("SearchPanel.add")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addItemsPerformed(getSelectedItemList(), target);
                Component component = getPopoverReferenceComponent();
                if (component != null && component.getMarkupId() != null) {
                    target.appendJavaScript(String.format("window.MidPointTheme.setFocus('%s');", component.getMarkupId()));
                }
            }
        };
        addButton.add(new VisibleBehaviour(this::isSelectable));
        popover.add(addButton);

        AjaxButton close = new AjaxButton(ID_CLOSE_BUTTON, createStringResource("SearchPanel.close")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeMorePopoverPerformed(target);
            }
        };
        popover.add(close);

    }

    private boolean isSelectable() {
        if (getModelObject() == null) {
            return false;
        }
        return CollectionUtils.isNotEmpty(getModelObject().stream().filter(item -> item instanceof SelectableRow).collect(Collectors.toList()));
    }

    protected abstract void addItemsPerformed(List<T> item, AjaxRequestTarget target);

    protected abstract Component getPopoverReferenceComponent();

    protected abstract String getItemName(T item);

    protected abstract String getItemHelp(T item);

    protected void closeMorePopoverPerformed(AjaxRequestTarget target) {
        togglePopover(target);
    }

    private List<T> getSelectedItemList() {
        return getModelObject().stream().filter(item -> (item  instanceof SelectableRow))
                .filter(item -> ((SelectableRow)item).isSelected()).collect(Collectors.toList());
    }

    protected IModel<String> getPopoverTitleModel() {
        return null;
    }

    public void togglePopover(AjaxRequestTarget target) {
        ((Popover) get(ID_POPOVER)).toggle(target);
    }

    protected String getPopoverCustomArrowStyle() {
        return null;
    }

    public String getPopoverMarkupId() {
        return get(ID_POPOVER).getMarkupId();
    }
}
