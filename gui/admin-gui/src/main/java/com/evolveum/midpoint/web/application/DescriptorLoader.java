/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.gui.admin_1.DescriptorType;
import com.evolveum.midpoint.xml.ns._public.gui.admin_1.ObjectFactory;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.*;

/**
 * @author lazyman
 */
public final class DescriptorLoader {

    private static final Trace LOGGER = TraceManager.getTrace(DescriptorLoader.class);

    private static Map<String, DisplayableValue<String>[]> actions = new HashMap<>();

    public static Map<String, DisplayableValue<String>[]> getActions() {
        return actions;
    }

    public void loadData(MidPointApplication application) {
        LOGGER.debug("Loading data from descriptor files.");

        String baseFileName = "/WEB-INF/descriptor.xml";
        String customFileName = "/WEB-INF/classes/descriptor.xml";

        try (InputStream baseInput = application.getServletContext().getResourceAsStream(baseFileName);
             InputStream customInput = application.getServletContext().getResourceAsStream(customFileName)) {
            if (baseInput == null) {
                LOGGER.error("Couldn't find " + baseFileName + " file, can't load application menu and other stuff.");
            }

            JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<DescriptorType> element = (JAXBElement) unmarshaller.unmarshal(baseInput);
            DescriptorType descriptor = element.getValue();

            LOGGER.debug("Loading menu bar from " + baseFileName + " .");
            DescriptorType customDescriptor = null;
            if (customInput != null) {
                element = (JAXBElement) unmarshaller.unmarshal(customInput);
                customDescriptor = element.getValue();
            }

            scanPackagesForPages(descriptor.getPackagesToScan(), application);
            if (customDescriptor != null) {
                scanPackagesForPages(customDescriptor.getPackagesToScan(), application);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't process application descriptor", ex);
        }
    }

    private void scanPackagesForPages(List<String> packages, MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        for (String pac : packages) {
            LOGGER.debug("Scanning package package {} for page annotations", new Object[]{pac});

            Set<Class> classes = ClassPathUtil.listClasses(pac);
            for (Class clazz : classes) {
                if (!WebPage.class.isAssignableFrom(clazz)) {
                    continue;
                }

                PageDescriptor descriptor = (PageDescriptor) clazz.getAnnotation(PageDescriptor.class);
                if (descriptor == null) {
                    continue;
                }

                mountPage(descriptor, clazz, application);
                loadActions(descriptor);
            }
        }
    }

    private void loadActions(PageDescriptor descriptor) {
        for (String url : descriptor.url()) {
            List<AuthorizationActionValue> actions = new ArrayList<>();

            //avoid of setting guiAll authz for "public" pages (e.g. login page)
            if (descriptor.action() == null || descriptor.action().length == 0) {
                return;
            }

            boolean canAccess = true;

            for (AuthorizationAction action : descriptor.action()) {
                actions.add(new AuthorizationActionValue(action.actionUri(), action.label(), action.description()));
                if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(action.actionUri())) {
                    canAccess = false;
                    break;
                }
            }

            //add http://.../..#guAll authorization only for displayable pages, not for pages used for development..
            if (canAccess) {

                actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL,
                        AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
                actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                        AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
            }
            this.actions.put(url, actions.toArray(new DisplayableValue[actions.size()]));
        }
    }

    private void mountPage(PageDescriptor descriptor, Class clazz, MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        for (String url : descriptor.url()) {
            IPageParametersEncoder encoder = descriptor.encoder().newInstance();

            LOGGER.trace("Mounting page '{}' to url '{}' with encoder '{}'.", new Object[]{
                    clazz.getName(), url, encoder.getClass().getSimpleName()});

            application.mount(new MountedMapper(url, clazz, encoder));
        }
    }
}
