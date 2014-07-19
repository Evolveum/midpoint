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
import com.evolveum.midpoint.web.component.menu.top.MenuBarItem;
import com.evolveum.midpoint.web.component.menu.top.MenuItem;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.gui.admin_1.*;
import org.apache.commons.lang.Validate;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
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

    private static List<MenuBarItem> menuBarItems = new ArrayList<>();
    private static Map<String, DisplayableValue<String>[]> actions = new HashMap<>();

    public static List<MenuBarItem> getMenuBarItems() {
        return menuBarItems;
    }

    public static Map<String, DisplayableValue<String>[]> getActions() {
        return actions;
    }

    public void loadData(MidPointApplication application) {
        LOGGER.info("Loading data from descriptor files.");

        String baseFileName = "WEB-INF/descriptor.xml";
        String customFileName = "WEB-INF/classes/descriptor.xml";

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
            MenuType menu = descriptor.getMenu();
            Map<Integer, RootMenuItemType> rootMenuItems = new HashMap<>();
            mergeRootMenuItems(menu, rootMenuItems);

            DescriptorType customDescriptor = null;
            if (customInput != null) {
                element = (JAXBElement) unmarshaller.unmarshal(customInput);
                customDescriptor = element.getValue();
                mergeRootMenuItems(customDescriptor.getMenu(), rootMenuItems);
            }

            List<RootMenuItemType> sortedRootMenuItems = sortRootMenuItems(rootMenuItems);
            loadMenuBar(sortedRootMenuItems);

            scanPackagesForPages(descriptor.getPackagesToScan(), application);
            if (customDescriptor != null) {
                scanPackagesForPages(customDescriptor.getPackagesToScan(), application);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't process application descriptor", ex);
        }
    }

    private List<RootMenuItemType> sortRootMenuItems(Map<Integer, RootMenuItemType> items) {
        List<RootMenuItemType> list = new ArrayList<>();
        list.addAll(items.values());

        Collections.sort(list, new Comparator<RootMenuItemType>() {

            @Override
            public int compare(RootMenuItemType o1, RootMenuItemType o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });

        return list;
    }

    private void mergeRootMenuItems(MenuType menu, Map<Integer, RootMenuItemType> items) {
        if (menu == null) {
            return;
        }

        for (RootMenuItemType root : menu.getRoot()) {
            if (items.containsKey(root.getOrder())) {
                items.remove(root.getOrder());
            }

            items.put(root.getOrder(), root);
        }
    }

    private void loadMenuBar(List<RootMenuItemType> roots) throws ClassNotFoundException {
        for (RootMenuItemType root : roots) {
            Validate.notNull(root.getName(), "Root menu name must not be null.");

            Class page = root.getPage() != null ? Class.forName(root.getPage()) : null;
            MenuBarItem menuBarItem = new MenuBarItem(createStringResource(root.getName()), page);
            menuBarItems.add(menuBarItem);

            for (AbstractMenuItemType item : root.getItemOrHeaderOrDivider()) {
                if (item instanceof MenuItemType) {
                    MenuItemType menuItem = (MenuItemType) item;
                    Validate.notNull(menuItem.getName(), "Menu item name must not be null.");
                    Validate.notNull(menuItem.getPage(), "Menu item page class must not be null.");

                    page = Class.forName(menuItem.getPage());
                    menuBarItem.addMenuItem(new MenuItem(createStringResource(menuItem.getName()), page));
                } else if (item instanceof MenuItemDividerType) {
                    menuBarItem.addMenuItem(new MenuItem(null));
                } else if (item instanceof MenuItemHeaderType) {
                    MenuItemHeaderType menuItem = (MenuItemHeaderType) item;
                    Validate.notNull(menuItem.getName(), "Header menu item name must not be null.");

                    menuBarItem.addMenuItem(new MenuItem(createStringResource(menuItem.getName()), true, null, null));
                }
            }
        }
    }

    public StringResourceModel createStringResource(String resourceKey) {
        return new StringResourceModel(resourceKey, new Model<String>(), resourceKey);
    }

    private void scanPackagesForPages(List<String> packages, MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        for (String pac : packages) {
            LOGGER.info("Scanning package package {} for page annotations", new Object[]{pac});

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
            for (AuthorizationAction action : descriptor.action()) {
                actions.add(new AuthorizationActionValue(action.actionUri(), action.label(), action.description()));
            }
            actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URI,
                    AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
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
