/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import java.util.Locale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.LocaleDescriptor;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);

    private static final String FLAG_CLASS_PREFIX = "fi-";

    private static final String ID_ICON = "icon";
    private static final String ID_LOCALES = "locales";
    private static final String ID_LOCALES_LINK = "localesLink";
    private static final String ID_LOCALES_ICON = "localesIcon";

    private static final String ID_LOCALES_LABEL = "localesLabel";

    public LocalePanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.append("class", () -> FLAG_CLASS_PREFIX + getFlag()));
        image.setOutputMarkupId(true);
        add(image);

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
                image.add(AttributeModifier.append("class", () -> FLAG_CLASS_PREFIX + item.getModelObject().getFlag()));
                localeLink.add(image);

                Label label = new Label(ID_LOCALES_LABEL, () -> item.getModelObject().getName());
                label.setRenderBodyOnly(true);
                localeLink.add(label);
            }
        };
        add(locales);
    }

    private String getFlag() {
        LocaleDescriptor descriptor = getSelectedLocaleDescriptor();
        return descriptor != null ? descriptor.getFlag() : "";
    }

    private LocaleDescriptor getSelectedLocaleDescriptor() {
        Locale locale = getSession().getLocale();
        if (locale == null) {
            return null;
        }

        // The second condition is a fix attempt for issue MID-2075, where firefox
        // returns 'sk' as a locale from session, while other browsers return 'sk_SK'.
        // This is the reason, why in firefox selected locale is ignored (the commented
        // condition is not met) so we are adding second condition to overcome this issue.
        for (LocaleDescriptor desc : MidPointApplication.AVAILABLE_LOCALES) {
//            if (locale.equals(desc.getLocale())
            if (locale.equals(desc.getLocale()) || locale.getLanguage().equals(desc.getLocale().getLanguage())) {
                return desc;
            }
        }

        return null;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String selectId = get(ID_ICON).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", descriptor.getLocale());
        getSession().setLocale(descriptor.getLocale());
        WebComponentUtil.getPageBase(this).getCompiledGuiProfile().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
