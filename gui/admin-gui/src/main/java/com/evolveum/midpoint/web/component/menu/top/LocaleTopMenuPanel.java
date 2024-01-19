/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.common.AvailableLocale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;

/**
 * @author lazyman
 */
public class LocaleTopMenuPanel extends LocalePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_LOCALES_MENU = "localesMenu";

    public LocaleTopMenuPanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.append("class", getSelectedFlagIcon()));
        image.setOutputMarkupId(true);
        add(image);

        LocalesDropDownMenu localesMenu = new LocalesDropDownMenu(ID_LOCALES_MENU) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void changeLocale(AjaxRequestTarget target, AvailableLocale.LocaleDescriptor descriptor) {
                LocaleTopMenuPanel.this.changeLocale(target, descriptor);
            }
        };
        add(localesMenu);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String selectId = get(ID_ICON).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }
}
