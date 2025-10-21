/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.button;

import com.evolveum.midpoint.web.component.menu.cog.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Universal button to display drop-down menus. The button itself can have numerous decorations: icon, label and tag with count (info)
 *
 * @author katkav
 *
 */
public class DropdownButtonPanel extends BasePanel<DropdownButtonDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTON_CONTAINER = "buttonContainer";
    private static final String ID_INFO = "info";
    private static final String ID_ICON = "icon";
    private static final String ID_CARET = "caret";
    private static final String ID_LABEL = "label";

    private static final String ID_DROPDOWN_MENU = "dropDownMenu";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";

    public DropdownButtonPanel(String id, DropdownButtonDto model) {
        super(id, Model.of(model));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
        buttonContainer.setOutputMarkupId(true);
        buttonContainer.add(AttributeAppender.append("class", getSpecialButtonClass()));
        buttonContainer.add(AttributeAppender.append("class", () -> hasToggleIcon() ? " dropdown-toggle " : ""));
        add(buttonContainer);

        Label info = new Label(ID_INFO, new PropertyModel<>(getModel(), DropdownButtonDto.F_INFO));
        info.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getInfo() != null));
        buttonContainer.add(info);

        Label label = new Label(ID_LABEL, new PropertyModel<>(getModel(), DropdownButtonDto.F_LABEL));
        label.setRenderBodyOnly(true);
        label.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getLabel() != null));
        buttonContainer.add(label);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.append("class", new PropertyModel<>(getModel(), DropdownButtonDto.F_ICON)));
        icon.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getIcon() != null));
        buttonContainer.add(icon);

        WebMarkupContainer caret = new WebMarkupContainer(ID_CARET);
        caret.add(new VisibleBehaviour(this::visibleCaret));
        buttonContainer.add(caret);

        WebMarkupContainer dropdownMenuContainer = new WebMarkupContainer(ID_DROPDOWN_MENU);
        dropdownMenuContainer.setOutputMarkupId(true);
        dropdownMenuContainer.add(AttributeAppender.append("class", getSpecialDropdownMenuClass()));
        add(dropdownMenuContainer);

        ListView<InlineMenuItem> li = new ListView<>(ID_MENU_ITEM, new PropertyModel<>(getModel(), DropdownButtonDto.F_ITEMS)) {

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                populateMenuItem(ID_MENU_ITEM_BODY, item);
            }
        };

        dropdownMenuContainer.add(li);
    }

    protected boolean hasToggleIcon() {
        return true;
    }

    public WebMarkupContainer getButtonContainer() {
        return (WebMarkupContainer) get(ID_BUTTON_CONTAINER);
    }

    protected boolean visibleCaret() {
        return true;
    }

    protected void populateMenuItem(String componentId, ListItem<InlineMenuItem> menuItem) {
        menuItem.setRenderBodyOnly(true);
        IModel<InlineMenuItem> model = menuItem.getModel();
        Component menuItemBody = createMenuLinkPanel(componentId,
                model,
                showIcon(),
                this::onBeforeClickMenuItem);

        menuItemBody.setRenderBodyOnly(true);
        menuItem.add(menuItemBody);
        menuItem.add(new VisibleBehaviour(() -> menuItem.getModelObject().getVisible().getObject()));
    }

    public static <T extends InlineMenuItem> @NotNull Component createMenuLinkPanel(
            String componentId,
            IModel<T> model,
            boolean showIcon,
            OnBeforeClickHandler<T> beforeClickHandler) {

        if (showIcon && model.getObject() instanceof ButtonInlineMenuItem) {
            @SuppressWarnings("unchecked")
            IModel<ButtonInlineMenuItem> buttonModel = (IModel<ButtonInlineMenuItem>) model;

            return new IconMenuLinkPanel(componentId, buttonModel) {
                @SuppressWarnings("unchecked")
                @Override
                protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<ButtonInlineMenuItem> item) {
                    beforeClickHandler.handle(target, action, (IModel<T>) item);
                    super.onClick(target, action, item);
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<ButtonInlineMenuItem> item) {
                    beforeClickHandler.handle(target, action, (IModel<T>) item);
                    super.onSubmit(target, action, item);
                }
            };
        } else {
            return new MenuLinkPanel<>(componentId, model) {
                @Override
                protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<T> item) {
                    beforeClickHandler.handle(target, action, item);
                    super.onClick(target, action, item);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<T> item) {
                    beforeClickHandler.handle(target, action, item);
                    super.onSubmit(target, action, item);
                }
            };
        }
    }

    protected boolean showIcon() {
        return false;
    }

    @FunctionalInterface
    public interface OnBeforeClickHandler<T extends InlineMenuItem> extends Serializable {
        void handle(AjaxRequestTarget target, InlineMenuItemAction action, IModel<T> item);
    }

    protected void onBeforeClickMenuItem(
            AjaxRequestTarget target,
            InlineMenuItemAction action,
            IModel<? extends InlineMenuItem> item) {
    }

    protected String getSpecialButtonClass() {
        return "btn-app";
    }

    protected String getSpecialDropdownMenuClass() {
        return "dropdown-menu-right";
    }
}
