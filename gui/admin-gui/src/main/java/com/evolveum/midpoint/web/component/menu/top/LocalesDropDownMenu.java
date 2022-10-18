/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.security.LocaleDescriptor;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalesDropDownMenu extends BasePanel<List<LocaleDescriptor>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LOCALES = "locales";
    private static final String ID_LOCALES_LINK = "localesLink";
    private static final String ID_LOCALES_ICON = "localesIcon";
    private static final String ID_LOCALES_LABEL = "localesLabel";

    public LocalesDropDownMenu(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.prepend("class", "dropdown-menu dropdown-menu-right"));

        ListView<LocaleDescriptor> locales = new ListView<>(ID_LOCALES, Model.ofList(MidPointApplication.AVAILABLE_LOCALES)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<LocaleDescriptor> item) {
                item.setRenderBodyOnly(true);
                final AjaxLink<String> localeLink = new AjaxLink<>(ID_LOCALES_LINK) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeLocale(target, item.getModelObject());
                    }
                };
                item.add(localeLink);

                Label image = new Label(ID_LOCALES_ICON);
                image.add(AttributeModifier.append("class", () -> LocalePanel.getFlagIcon(item.getModelObject())));
                localeLink.add(image);

                Label label = new Label(ID_LOCALES_LABEL, () -> item.getModelObject().getName());
                label.setRenderBodyOnly(true);
                localeLink.add(label);
            }
        };
        add(locales);
    }

    protected void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {

    }
}
