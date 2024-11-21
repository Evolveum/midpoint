/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.button;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.AjaxButton;

import com.evolveum.midpoint.web.component.AjaxSubmitButton;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

public abstract class ActiveButtonWithDropDownPanel<T> extends BasePanel<List<T>> {

    private static final String ID_ACTION_BUTTON = "actionButton";
    private static final String ID_ACTION_BUTTON_ICON = "actionButtonIcon";
    private static final String ID_ACTION_BUTTON_LABEL = "actionButtonLabel";
    private static final String ID_DROPDOWN_BUTTON = "dropdownButton";
    private static final String ID_MENU_ITEMS = "items";
    private static final String ID_MENU_ITEM = "item";

    private final IModel<String> labelModel;

    public ActiveButtonWithDropDownPanel(String id, IModel<List<T>> model, IModel<String> labelModel) {
        super(id, model);
        this.labelModel = labelModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxSubmitButton actionButton = new AjaxSubmitButton(ID_ACTION_BUTTON) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onClickOnActionButton(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                refreshOnError(target);
            }
        };
        actionButton.setOutputMarkupId(true);
        actionButton.add(AttributeModifier.append("class", getButtonColorCssClass()));
        add(actionButton);

        WebMarkupContainer dropdownButtonIcon = new WebMarkupContainer(ID_ACTION_BUTTON_ICON);
        dropdownButtonIcon.setOutputMarkupId(true);
        dropdownButtonIcon.add(AttributeAppender.append("class", getIcon()));
        actionButton.add(dropdownButtonIcon);

        Label dropdownButtonLabel = new Label(ID_ACTION_BUTTON_LABEL, labelModel);
        dropdownButtonLabel.setOutputMarkupId(true);
        actionButton.add(dropdownButtonLabel);

        AjaxButton dropdownButton = new AjaxButton(ID_DROPDOWN_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        };
        dropdownButton.setOutputMarkupId(true);
        dropdownButton.add(AttributeModifier.append("class", getButtonColorCssClass()));
        dropdownButton.add(AttributeModifier.append(
                "aria-label",
                createStringResource("ActiveButtonWithDropDownPanel.dropdownButton", labelModel.getObject())));
        add(dropdownButton);

        ListView<T> menuItems = new ListView<>(ID_MENU_ITEMS, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<T> item) {
                AjaxSubmitButton ajaxSubmitPanel = new AjaxSubmitButton(ID_MENU_ITEM, () -> getLinkLabel(item.getModelObject())) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        onClickMenuItem(item.getModelObject(), target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        refreshOnError(target);
                    }
                };

                ajaxSubmitPanel.add(new Behavior() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void bind(Component component) {
                        super.bind(component);

                        component.add(AttributeModifier.replace("onkeydown",
                                Model.of(
                                        "if (event.keyCode == 32){"
                                                + "this.click();"
                                                + "}"
                                )));
                    }
                });

                item.add(ajaxSubmitPanel);
            }
        };
        menuItems.setOutputMarkupId(true);
        add(menuItems);
    }

    protected String getButtonColorCssClass() {
        return "btn-info";
    }

    protected String getIcon() {
        return null;
    }

    protected abstract void onClickMenuItem(T modelObject, AjaxRequestTarget target);

    protected abstract String getLinkLabel(T object);

    protected void refreshOnError(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());
    }

    protected abstract void onClickOnActionButton(AjaxRequestTarget target);
}
