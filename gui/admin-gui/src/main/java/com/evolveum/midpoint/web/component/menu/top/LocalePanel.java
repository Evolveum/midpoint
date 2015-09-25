/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);
    private static final String LOCALIZATION_DESCRIPTOR = "/localization/locale.properties";
    private static final List<LocaleDescriptor> AVAILABLE_LOCALES;

    private static final String PROP_NAME = ".name";
    private static final String PROP_FLAG = ".flag";

    private static final String ID_SELECT = "select";
    private static final String ID_OPTIONS = "options";

    static {
        List<LocaleDescriptor> locales = new ArrayList<>();
        try {
            ClassLoader classLoader = LocalePanel.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(LOCALIZATION_DESCRIPTOR);
            while (urls.hasMoreElements()) {
                final URL url = urls.nextElement();
                LOGGER.debug("Found localization descriptor {}.", new Object[]{url.toString()});

                Properties properties = new Properties();
                Reader reader = null;
                try {
                    reader = new InputStreamReader(url.openStream(), "utf-8");
                    properties.load(reader);

                    Map<String, Map<String, String>> localeMap = new HashMap<>();
                    Set<String> keys = (Set) properties.keySet();
                    for (String key : keys) {
                        String[] array = key.split("\\.");
                        if (array.length != 2) {
                            continue;
                        }

                        String locale = array[0];
                        Map<String, String> map = localeMap.get(locale);
                        if (map == null) {
                            map = new HashMap<>();
                            localeMap.put(locale, map);
                        }

                        map.put(key, properties.getProperty(key));
                    }

                    for (String key : localeMap.keySet()) {
                        Map<String, String> localeDefinition = localeMap.get(key);
                        if (!localeDefinition.containsKey(key + PROP_NAME)
                                || !localeDefinition.containsKey(key + PROP_FLAG)) {
                            continue;
                        }

                        LocaleDescriptor descriptor = new LocaleDescriptor(
                                localeDefinition.get(key + PROP_NAME),
                                localeDefinition.get(key + PROP_FLAG),
                                WebMiscUtil.getLocaleFromString(key)
                        );
                        locales.add(descriptor);
                    }
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load localization", ex);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }

            Collections.sort(locales);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load locales", ex);
        }

        AVAILABLE_LOCALES = Collections.unmodifiableList(locales);
    }

    public LocalePanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        final IModel<LocaleDescriptor> model = new Model(getSelectedLocaleDescriptor());
        Select<LocaleDescriptor> select = new Select<>(ID_SELECT, model);
        select.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                changeLocale(target, model.getObject());
            }
        });
        select.setOutputMarkupId(true);
        add(select);
        SelectOptions<LocaleDescriptor> options = new SelectOptions<LocaleDescriptor>(ID_OPTIONS, AVAILABLE_LOCALES,
                new IOptionRenderer<LocaleDescriptor>() {

                    @Override
                    public String getDisplayValue(LocaleDescriptor object) {
                        return object.getName();
                    }

                    @Override
                    public IModel<LocaleDescriptor> getModel(LocaleDescriptor value) {
                        return new Model<>(value);
                    }
                }) {


            @Override
            protected SelectOption<LocaleDescriptor> newOption(String text, IModel<? extends LocaleDescriptor> model) {
                SelectOption option = super.newOption("&nbsp;" + text, model);
                option.add(new AttributeModifier("data-icon", "flag-" + model.getObject().getFlag()));

                return option;
            }
        };
        select.add(options);
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
        for (LocaleDescriptor desc : AVAILABLE_LOCALES) {
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

        String selectId = get(ID_SELECT).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", new Object[]{descriptor.getLocale()});
        getSession().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
