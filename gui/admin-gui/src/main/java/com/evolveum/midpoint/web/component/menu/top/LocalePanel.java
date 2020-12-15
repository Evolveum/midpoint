/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.component.DashboardSearchPanel;
import com.evolveum.midpoint.web.security.LocaleDescriptor;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import java.util.ArrayList;
import java.util.Locale;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);

    private static final String ID_LOCALES = "locales";
    private static final String ID_LOCALE_ICON = "localeIcon";
    private static final String ID_LOCALE_LINK = "localeLink";
    private static final String ID_LOCALE_ITEM_ICON = "localeItemIcon";

    public LocalePanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        Label image = new Label(ID_LOCALE_ICON);
        image.add(AttributeModifier.replace("class", new IModel<String>(){
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject(){
                StringBuilder sb = new StringBuilder("flag-");
                sb.append(getSelectedLocaleDescriptor() != null ?
                        getSelectedLocaleDescriptor().getFlag() : "");
                return sb.toString();
            }
        }));
        image.setOutputMarkupId(true);
        add(image);

        ListView<LocaleDescriptor> locales = new ListView<LocaleDescriptor>(ID_LOCALES,
                Model.ofList(MidPointApplication.AVAILABLE_LOCALES)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<LocaleDescriptor> item) {
                final Label image = new Label(ID_LOCALE_ITEM_ICON);
                image.add(AttributeModifier.append("class", "flag-" + item.getModelObject().getFlag()));
                image.setOutputMarkupId(true);
                item.add(image);

                final AjaxLink<String> localeLink = new AjaxLink<String>(ID_LOCALE_LINK) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getBody() {
                        return Model.of(item.getModelObject().getName());
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeLocale(target, item.getModelObject());
                    }


                };
                localeLink.setOutputMarkupId(true);
                item.add(localeLink);
            }
        };
        locales.setOutputMarkupId(true);
        add(locales);
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

        String selectId = get(ID_LOCALE_ICON).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", descriptor.getLocale());
        getSession().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
