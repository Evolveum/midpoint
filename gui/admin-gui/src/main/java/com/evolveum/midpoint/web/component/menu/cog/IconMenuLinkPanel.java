/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class IconMenuLinkPanel extends MenuLinkPanel<ButtonInlineMenuItem> {

    private static final String ID_MENU_ITEM_ICON = "menuItemIcon";

    private static final String ID_MENU_ITEM_BADGE = "menuItemBadge";

    public IconMenuLinkPanel(String id, IModel<ButtonInlineMenuItem> item) {
        super(id, item);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ButtonInlineMenuItem dto = getModelObject();

        WebMarkupContainer icon = new WebMarkupContainer(ID_MENU_ITEM_ICON);
        icon.add(AttributeAppender.append("class", () -> dto.getIconCompositedBuilder().build().getBasicIcon()));
        getLinkContainer().add(icon);

        Label badge = new Label(ID_MENU_ITEM_BADGE, () -> {
            if (dto instanceof ButtonInlineMenuItemWithCount) {
                return ((ButtonInlineMenuItemWithCount) dto).getCount();
            }
            return "";
        });
        badge.add(new VisibleBehaviour(dto::isBadgeVisible));
        getLinkContainer().add(badge);
    }
}
