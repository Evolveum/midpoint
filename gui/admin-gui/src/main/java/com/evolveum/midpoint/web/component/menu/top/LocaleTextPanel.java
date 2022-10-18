/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.web.security.LocaleDescriptor;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocaleTextPanel extends LocalePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "text";
    private static final String ID_LOCALES_MENU = "localesMenu";

    public LocaleTextPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.append("class", getSelectedFlagIcon()));
        image.setOutputMarkupId(true);
        add(image);

        Label text = new Label(ID_TEXT, () -> getSelectedLocaleDescriptor().getName());
        add(text);

        LocalesDropDownMenu localesMenu = new LocalesDropDownMenu(ID_LOCALES_MENU) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
                LocaleTextPanel.this.changeLocale(target, descriptor);
            }
        };
        add(localesMenu);
    }
}
