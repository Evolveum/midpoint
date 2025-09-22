/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.IconMenuLinkPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

/**
 * Split button with a primary action (left) and a dropdown (right).
 * Uses DropdownButtonDto for items, reusing MidPoint's MenuLinkPanel rendering.
 * <p>
 * Markup contract (ids must match):
 * - primaryButton        : where the left button is rendered (container)
 * - dropdownButton       : the caret/toggle button (exists only in markup)
 * - menuItems            : ListView container
 * - menuItem             : each menu entry rendered by MenuLinkPanel
 */
public abstract class SplitButtonWithDropdownMenu extends BasePanel<DropdownButtonDto> {

    @Serial private static final long serialVersionUID = 1L;

    protected static final String ID_PRIMARY_BUTTON = "primaryButton";
    private static final String ID_DROPDOWN_BUTTON = "dropdownButton";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_MENU_ITEM = "menuItem";

    private static final String FRAGMENT_PRIMARY_BUTTON = "primaryButtonFragment";
    private static final String FRAGMENT_ITEM_ICON = "icon";
    private static final String FRAGMENT_ITEM_LABEL = "label";
    private static final String FRAGMENT_ITEM_INFO = "info";

    public SplitButtonWithDropdownMenu(String id, IModel<DropdownButtonDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Component primary = createPrimaryFragmentButton();
        primary.setOutputMarkupId(true);
        primary.add(new VisibleBehaviour(this::isPrimaryButtonVisible));
        add(primary);

        WebMarkupContainer dropdownBtn = new WebMarkupContainer(ID_DROPDOWN_BUTTON);
        dropdownBtn.setOutputMarkupId(true);
        dropdownBtn.add(new VisibleEnableBehaviour(this::isDropdownVisible, this::isDropdownEnabled));
        add(dropdownBtn);

        ListView<InlineMenuItem> menuItems = new ListView<>(
                ID_MENU_ITEMS,
                new PropertyModel<>(getModel(), DropdownButtonDto.F_ITEMS)) {

            @Override
            protected void populateItem(@NotNull ListItem<InlineMenuItem> item) {
                populateInlineMenuItems(item);
            }
        };
        menuItems.setOutputMarkupId(true);
        add(menuItems);
    }

    protected Fragment createPrimaryFragmentButton() {
        Fragment fragment = new Fragment(SplitButtonWithDropdownMenu.ID_PRIMARY_BUTTON, FRAGMENT_PRIMARY_BUTTON, this);
        WebMarkupContainer icon = new WebMarkupContainer(FRAGMENT_ITEM_ICON);
        icon.add(new AttributeAppender("class", new PropertyModel<>(getModel(), DropdownButtonDto.F_ICON)));
        icon.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getIcon() != null));
        fragment.add(icon);

        Label label = new Label(FRAGMENT_ITEM_LABEL, new PropertyModel<>(getModel(), DropdownButtonDto.F_LABEL));
        label.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getLabel() != null));
        fragment.add(label);

        Label info = new Label(FRAGMENT_ITEM_INFO, new PropertyModel<>(getModel(), DropdownButtonDto.F_INFO));
        info.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getInfo() != null));
        fragment.add(info);

        fragment.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                performPrimaryButtonAction(ajaxRequestTarget);
            }
        });
        return fragment;
    }

    protected void performPrimaryButtonAction(AjaxRequestTarget target) {

    }

    private void populateInlineMenuItems(@NotNull ListItem<InlineMenuItem> item) {
        item.setRenderBodyOnly(true);

        MenuLinkPanel<?> body;
        InlineMenuItem mi = item.getModelObject();

        if (showIcon() && mi instanceof ButtonInlineMenuItem) {
            @SuppressWarnings("unchecked")
            IModel<ButtonInlineMenuItem> m = (IModel<ButtonInlineMenuItem>) (IModel<?>) item.getModel();
            body = new IconMenuLinkPanel(ID_MENU_ITEM, m) {
                @Override
                protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<ButtonInlineMenuItem> model) {
                    onBeforeClickMenuItem(target, action, model);
                    super.onClick(target, action, model);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<ButtonInlineMenuItem> model) {
                    onBeforeClickMenuItem(target, action, model);
                    super.onSubmit(target, action, model);
                }

            };
        } else {
            body = new MenuLinkPanel<>(ID_MENU_ITEM, item.getModel()) {
                @Override
                protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<InlineMenuItem> model) {
                    onBeforeClickMenuItem(target, action, model);
                    super.onClick(target, action, model);
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<InlineMenuItem> model) {
                    onBeforeClickMenuItem(target, action, model);
                    super.onSubmit(target, action, model);
                }
            };
        }

        body.setRenderBodyOnly(true);
        item.add(body);
        item.add(new VisibleBehaviour(() -> item.getModelObject().getVisible().getObject()));
    }

    /** Called before any menu item action is invoked. */
    protected void onBeforeClickMenuItem(AjaxRequestTarget target,
            InlineMenuItemAction action,
            IModel<? extends InlineMenuItem> item) {
        // no-op by default
    }

    /** Show icons for ButtonInlineMenuItem entries? */
    protected boolean showIcon() {
        return false;
    }

    /** Visibility/enable rules for the caret toggle. */
    protected boolean isDropdownVisible() {
        return true;
    }

    protected boolean isDropdownEnabled() {
        return true;
    }

    /** Visibility/enable rules for the primary button (override if needed). */
    protected boolean isPrimaryButtonVisible() {
        return true;
    }
}
