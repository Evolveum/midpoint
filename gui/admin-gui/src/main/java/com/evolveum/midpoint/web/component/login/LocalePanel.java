/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    public static final String INIT_PARAM = "availableLocales";

    public LocalePanel(String id) {
        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("locale");
        container.setOutputMarkupId(true);

        ListView<Locale> ulList = new ListView<Locale>("locales", createLocaleModel()) {

            @Override
            protected void populateItem(ListItem<Locale> components) {
                AjaxLink<Locale> link = new AjaxLink<Locale>("localeLink", components.getModel()) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeLocale(target, getModelObject());
                    }
                };
                components.add(link);
                link.add(new AttributeAppender("style", createStyle(link.getModelObject()), " "));
                link.add(new AttributeAppender("title", new Model<String>(components.getModelObject().getLanguage().toLowerCase()), " "));
            }
        };
        container.add(ulList);
        add(container);
    }

    private IModel<String> createStyle(Locale locale) {
        return new Model<String>("background: url('img/flag/"
                + locale.getLanguage().toLowerCase() + ".png') no-repeat;");
    }

    private IModel<List<Locale>> createLocaleModel() {
        return new LoadableModel<List<Locale>>() {

            @Override
            protected List<Locale> load() {
                List<Locale> locales = new ArrayList<Locale>();

                MidPointApplication application = (MidPointApplication) getApplication();
                //todo: get locales
                String value = application.getServletContext().getInitParameter(INIT_PARAM);
                if (!StringUtils.isEmpty(value)) {
                    StringTokenizer tokenizer = new StringTokenizer(value);
                    while (tokenizer.hasMoreTokens()) {
                        locales.add(stringToLocale(tokenizer.nextToken()));
                    }
                }

                if (locales.isEmpty()) {
                    locales.add(Locale.US);
                }

                return locales;
            }
        };
    }

    public Locale stringToLocale(String locale) {
        StringTokenizer tokenizer = new StringTokenizer(locale, "_");
        String l = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        String c = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

        return new Locale(l, c);
    }


    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(LocalePanel.class, "LocalePanel.css"));
    }

    private void changeLocale(AjaxRequestTarget target, Locale locale) {
        getSession().setLocale(locale);

    }
}
