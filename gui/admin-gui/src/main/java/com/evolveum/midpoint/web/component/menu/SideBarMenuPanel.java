/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu;

import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.session.SessionStorage;

/**
 * @author Viliam Repan (lazyman)
 */
public class SideBarMenuPanel extends BasePanel<List<SideBarMenuItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SideBarMenuPanel.class);

    private static final String ID_SIDEBAR = "sidebar";

    private static final String ID_MENU_PHOTO = "menuPhoto";
    private static final String ID_USERNAME = "username";
    private static final String ID_USERNAME_DESCRIPTION = "usernameDescription";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_HEADER = "header";
    private static final String ID_NAME = "name";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public SideBarMenuPanel(String id, IModel<List<SideBarMenuItem>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    protected void initLayout() {
        //NonCachingImage img = new NonCachingImage(ID_MENU_PHOTO, loadJpegPhotoUrlModel());
        //add(img);
        IModel<String> photoUrlModel = loadJpegPhotoUrlModel();
        WebMarkupContainer photoContainer = new WebMarkupContainer(ID_MENU_PHOTO);
        photoContainer.add(AttributeAppender.append("style", "background-image: url('" + photoUrlModel.getObject() + "');"));
        add(photoContainer);

        Label username = new Label(ID_USERNAME, this::getShortUserName);
        username.add(AttributeAppender.append("title", this::getShortUserName));
        add(username);

        Label usernameDescription = new Label(ID_USERNAME_DESCRIPTION, this::getUsernameDescription);
        usernameDescription.add(new VisibleBehaviour(this::isUsernameDescriptionNotEmpty));
        add(usernameDescription);

        WebMarkupContainer sidebar = new WebMarkupContainer(ID_SIDEBAR);
        sidebar.setOutputMarkupId(true);
        add(sidebar);

        ListView<SideBarMenuItem> menuItems = new ListView<>(ID_MENU_ITEMS, getModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SideBarMenuItem> item) {
                item.add(createHeader(item.getModel()));
                item.add(createMenuItems(item.getModel()));
            }
        };
        menuItems.setOutputMarkupId(true);
        menuItems.setReuseItems(true);
        sidebar.add(menuItems);
    }

    private String getUsernameDescription() {
        Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principalObj instanceof MidPointPrincipal principal)) {
            return null;
        }

        // todo improve?
        return principal.getFocus().getDescription();
    }

    private boolean isUsernameDescriptionNotEmpty() {
        var usernameDescription = getUsernameDescription();
        return StringUtils.isNotEmpty(usernameDescription);
    }

    private String getShortUserName() {
        Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principalObj == null) {
            return "Unknown";
        }

        if (principalObj instanceof MidPointPrincipal principal) {
            return WebComponentUtil.getOrigStringFromPoly(principal.getName());
        }

        return principalObj.toString();
    }

    private IModel<String> loadJpegPhotoUrlModel() {
        return () -> {
            GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
            if (principal == null) {
                return null;
            }
            CompiledGuiProfile profile = principal.getCompiledGuiProfile();
            byte[] jpegPhoto = profile.getJpegPhoto();

            if (jpegPhoto == null) {
                URL placeholder = this.getClass().getClassLoader().getResource("static/img/placeholder.png");
                if (placeholder == null) {
                    return null;
                }
                try {
                    jpegPhoto = IOUtils.toByteArray(placeholder);
                } catch (IOException e) {
                    LOGGER.error("Cannot load placeholder for photo.");
                    return null;
                }
            }
            String base64Encoded = Base64.getEncoder().encodeToString(jpegPhoto);
            return "data:image/jpeg;base64," + base64Encoded;
        };
    }

    private Component createHeader(IModel<SideBarMenuItem> model) {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onMenuClick(model);
            }
        });
        header.add(AttributeAppender.append("class", () -> isMenuExpanded(model.getObject()) ? "" : "closed"));
        header.add(AttributeAppender.append("aria-expanded", () -> isMenuExpanded(model.getObject())));

        Label name = new Label(ID_NAME, () -> {
            String key = model.getObject().getName();
            return getString(key, null, key);
        });
        header.add(name);
        header.add(new VisibleBehaviour(() -> model.getObject().isVisible()));
        return header;
    }

    private Component createMenuItems(IModel<SideBarMenuItem> model) {
        ListView<MainMenuItem> items = new ListView<>(ID_ITEMS, new PropertyModel<>(model, SideBarMenuItem.F_ITEMS)) {

            @Override
            protected void populateItem(final ListItem<MainMenuItem> listItem) {
                MainMenuPanel item = new MainMenuPanel(ID_ITEM, listItem.getModel()) {
                    @Override
                    protected boolean isMenuExpanded() {
                        return SideBarMenuPanel.this.isMenuExpanded(model.getObject());
                    }
                };

                item.setOutputMarkupId(true);
                item.add(new VisibleBehaviour(() -> listItem.getModelObject().isVisible()));
                listItem.add(item);
            }
        };

        items.setReuseItems(true);
        return items;
    }

    private void onMenuClick(IModel<SideBarMenuItem> model) {
        SideBarMenuItem mainMenu = model.getObject();

        SessionStorage storage = getPageBase().getSessionStorage();
        Map<String, Boolean> menuState = storage.getMainMenuState();

        String menuLabel = mainMenu.getName();
        // we'll use menu label as key
        Boolean expanded = menuState.get(menuLabel);

        if (expanded == null) {
            expanded = true;
        }
        menuState.put(menuLabel, !expanded);
    }

    private boolean isMenuExpanded(SideBarMenuItem mainMenu) {
        SessionStorage storage = getPageBase().getSessionStorage();
        Map<String, Boolean> menuState = storage.getMainMenuState();

        String menuLabel = mainMenu.getName();
        // we'll use menu label as key
        Boolean expanded = menuState.get(menuLabel);

        return expanded != null ? expanded : true;
    }

}
