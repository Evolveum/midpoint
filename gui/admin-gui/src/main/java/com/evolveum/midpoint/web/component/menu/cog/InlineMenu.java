/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu.cog;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * DEPRECATED: use DropdownButtonPanel instead
 *
 * This is the old cog-like dropdown from pre-adminlte times
 *
 * @author lazyman
 */
@Deprecated
public class InlineMenu extends BasePanel<List<InlineMenuItem>> {

    private static final String ID_MENU_ITEM_CONTAINER= "menuItemContainer";
    private static final String ID_MENU_ITEM_BUTTON = "menuItemButton";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";
    private static final String ID_MENU_ITEM_ICON = "menuItemIcon";

    private boolean hideByDefault;

    public InlineMenu(String id, IModel<List<InlineMenuItem>> model) {
        this(id, model, false);
    }

    public InlineMenu(String id, IModel<List<InlineMenuItem>> model, boolean hideByDefault) {
        super(id, model);
        this.hideByDefault = hideByDefault;

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initInlineMenu('").append(getMarkupId()).append("', ").append(hideByDefault).append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    private void initLayout() {
        WebMarkupContainer menuItemContainer = new WebMarkupContainer(ID_MENU_ITEM_CONTAINER);
        menuItemContainer.setOutputMarkupId(true);
        menuItemContainer.setOutputMarkupPlaceholderTag(true);
        menuItemContainer.add(new AttributeAppender("class", getMenuItemContainerClass()));
        menuItemContainer.add(new AttributeAppender("style", getMenuItemContainerStyle()));
        add(menuItemContainer);

        AjaxButton menuItemButton = new AjaxButton(ID_MENU_ITEM_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        menuItemButton.setOutputMarkupId(true);
        menuItemButton.setOutputMarkupPlaceholderTag(true);
        menuItemButton.add(new AttributeAppender("class", "dropdown-toggle " + getAdditionalButtonClass()));
        menuItemButton.add(new AttributeAppender("style", getMenuItemButtonStyle()));
        menuItemContainer.add(menuItemButton);

        WebMarkupContainer icon = new WebMarkupContainer(ID_MENU_ITEM_ICON);
        icon.setOutputMarkupId(true);
        icon.add(new AttributeAppender("class", getIconClass()));
        menuItemButton.add(icon);

        ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_MENU_ITEM, getModel()) {

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                initMenuItem(item);
            }
        };
        li.setOutputMarkupId(true);
        li.setOutputMarkupPlaceholderTag(true);
        li.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                List list = InlineMenu.this.getModel().getObject();
                return list != null && !list.isEmpty();
            }
        });
        menuItemContainer.add(li);

        menuItemContainer.add(new VisibleBehaviour(() -> InlineMenu.this.getModelObject() != null && !InlineMenu.this.getModelObject().isEmpty()));
    }

    private void initMenuItem(ListItem<InlineMenuItem> menuItem) {
        final InlineMenuItem item = menuItem.getModelObject();

        menuItem.add(AttributeModifier.append("class", new IModel<String>() {

            @Override
            public String getObject() {
                if (item.isMenuHeader()) {
                    return "dropdown-header";
                } else if (item.isDivider()) {
                    return "divider";
                }

                return getBoolean(item.getEnabled(), true) ? null : "disabled";
            }
        }));

            menuItem.add(new VisibleEnableBehaviour() {

                @Override
                public boolean isEnabled() {
                    return getBoolean(item.getEnabled(), true);
                }

                @Override
                public boolean isVisible() {
                    return getBoolean(item.getVisible(), true);
                }
            });

            menuItem.setOutputMarkupId(true);
            menuItem.setOutputMarkupPlaceholderTag(true);

        WebMarkupContainer menuItemBody;
        if (item.isMenuHeader() || item.isDivider()) {
            menuItemBody = new MenuDividerPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
        } else {
            menuItemBody = new MenuLinkPanel(ID_MENU_ITEM_BODY, menuItem.getModel());
        }
        menuItemBody.setRenderBodyOnly(true);
        menuItemBody.setOutputMarkupPlaceholderTag(true);
        menuItemBody.setOutputMarkupId(true);
        menuItem.add(menuItemBody);

    }

    protected String getIconClass(){
        return "fa fa-cog";
    }

    protected String getAdditionalButtonClass(){
        return "";
    }

    protected String getMenuItemButtonStyle(){
        return "border-top: 2px; !important";
    }

    protected String getMenuItemContainerClass(){
        return "nav nav-pills cog pull-right";
    }

    protected String getMenuItemContainerStyle(){
        return "";
    }

    private boolean getBoolean(IModel<Boolean> model, boolean def) {
        if (model == null) {
            return def;
        }

        Boolean value = model.getObject();
        return value != null ? value : def;
    }
}
