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

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);
    private static final String LOCALIZATION_DESCRIPTOR = "Messages.localization";
    private static final List<LocaleDescriptor> AVAILABLE_LOCALES;

    static {
        List<LocaleDescriptor> locales = new ArrayList<LocaleDescriptor>();
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

                    final LocaleDescriptor descriptor = new LocaleDescriptor(properties);
                    if (descriptor != null) {
                        locales.add(descriptor);

                        if (StringUtils.isNotEmpty(descriptor.getFlag())) {
                            mountFlagImage(url, descriptor.getFlag());
                        }
                    }
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load localization", ex);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load locales", ex);
        }

        AVAILABLE_LOCALES = Collections.unmodifiableList(locales);
    }

    public LocalePanel(String id) {
        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("locale");
        container.setOutputMarkupId(true);

        ListView<LocaleDescriptor> ulList = new ListView<LocaleDescriptor>("locales",
                new AbstractReadOnlyModel<List<LocaleDescriptor>>() {

                    @Override
                    public List<LocaleDescriptor> getObject() {
                        return AVAILABLE_LOCALES;
                    }
                }) {

            @Override
            protected void populateItem(final ListItem<LocaleDescriptor> components) {
                AjaxLink<LocaleDescriptor> link = new AjaxLink<LocaleDescriptor>("localeLink",
                        components.getModel()) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeLocale(target, getModelObject());
                    }
                };
                components.add(link);
                link.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isEnabled() {
                        Locale locale = getSession().getLocale();
                        if (locale == null) {
                            return true;
                        }

                        LocaleDescriptor descriptor = components.getModelObject();
                        return !locale.equals(descriptor.getLocale());
                    }
                });
                link.add(new AttributeModifier("style", createStyle(link.getModelObject())));
                link.add(new AttributeModifier("title", new PropertyModel<String>(link.getModelObject(), "name")));
            }
        };
        container.add(ulList);
        add(container);
    }

    private static void mountFlagImage(URL url, String flag) throws URISyntaxException, MalformedURLException {
        WebApplication application = MidPointApplication.get();

        URI uri = url.toURI();
        String newPath = uri.getScheme() + ":" + new File(url.getFile()).getParent() + "/" + flag;
        final URL flagURL = new URL(newPath);

        application.mountResource(ImgResources.BASE_PATH + "/flag/" + flag,
                new ResourceReference(flag) {

                    @Override
                    public IResource getResource() {
                        return new FlagImageResource(flagURL);
                    }
                });
    }

    private IModel<String> createStyle(LocaleDescriptor descriptor) {
        return new Model<String>("background: url('img/flag/" + descriptor.getFlag() + "') no-repeat;");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssReferenceHeaderItem.forReference(
                new PackageResourceReference(LocalePanel.class, "LocalePanel.css")));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", new Object[]{descriptor.getLocale()});
        getSession().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
