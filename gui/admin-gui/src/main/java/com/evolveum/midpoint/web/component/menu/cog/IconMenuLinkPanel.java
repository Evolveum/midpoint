/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
                return ((ButtonInlineMenuItemWithCount)dto).getCount();
            }
            return "";
        });
        badge.add(new VisibleBehaviour(() -> dto instanceof ButtonInlineMenuItemWithCount));
        getLinkContainer().add(badge);
    }
}
