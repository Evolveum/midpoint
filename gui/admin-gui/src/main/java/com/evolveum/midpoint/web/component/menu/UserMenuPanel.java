/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu;

import java.io.IOException;
import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author lazyman
 */
public class UserMenuPanel extends BasePanel<UserMenuPanel> {

    private static final Trace LOGGER = TraceManager.getTrace(UserMenuPanel.class);

    private static final String ID_USERNAME_LINK = "usernameLink";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_USERNAME = "username";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_PASSWORD_QUESTIONS = "passwordQuestions";
    private static final String ID_ICON_BOX = "menuIconBox";
    private static final String ID_PHOTO = "menuPhoto";
    private static final String ID_PANEL_ICON_BOX = "menuPanelIconBox";
    private static final String ID_PANEL_PHOTO = "menuPanelPhoto";

    public UserMenuPanel(String id) {
        super(id);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        IModel<AbstractResource> jpegPhotoModel = loadJpegPhotoModel();

        NonCachingImage img = new NonCachingImage(ID_PHOTO, jpegPhotoModel);
        iconBox.add(img);

        Label usernameLink = new Label(ID_USERNAME_LINK, (IModel<String>) this::getShortUserName);
        add(usernameLink);

        WebMarkupContainer panelIconBox = new WebMarkupContainer(ID_PANEL_ICON_BOX);
        add(panelIconBox);

        NonCachingImage panelImg = new NonCachingImage(ID_PANEL_PHOTO, jpegPhotoModel);
        panelIconBox.add(panelImg);

        Label username = new Label(ID_USERNAME, (IModel<String>) this::getShortUserName);
        username.setRenderBodyOnly(true);
        add(username);

        Label focusType = new Label(ID_FOCUS_TYPE, getPageBase().createStringResource("PageTemplate." + getFocusType()));
        add(focusType);

        MidpointForm<?> form = new MidpointForm<>(ID_LOGOUT_FORM);
        form.add(AttributeModifier.replace("action",
                (IModel<String>) () -> SecurityUtils.getPathForLogoutWithContextPath(
                        getRequest().getContextPath(), getAuthenticatedModule())));
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    private IModel<AbstractResource> loadJpegPhotoModel() {
        return new ReadOnlyModel<>(() -> {

            GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
            if (principal == null) {
                return null;
            }
            CompiledGuiProfile profile = principal.getCompiledGuiProfile();
            byte[] jpegPhoto = profile.getJpegPhoto();

            if (jpegPhoto == null) {
                URL placeholder = UserMenuPanel.class.getClassLoader().getResource("static/img/placeholder.png");
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

            return new ByteArrayResource("image/jpeg", jpegPhoto);
        });
    }

    private ModuleAuthentication getAuthenticatedModule() {
        ModuleAuthentication moduleAuthentication = SecurityUtils.getAuthenticatedModule();

        if (moduleAuthentication == null) {
            String message = "Unauthenticated request";
            throw new IllegalArgumentException(message);
        }
        return moduleAuthentication;
    }

    private String getShortUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "Unknown";
        }

        if (principal instanceof MidPointPrincipal) {
            MidPointPrincipal princ = (MidPointPrincipal) principal;
            return WebComponentUtil.getOrigStringFromPoly(princ.getName());
        }

        return principal.toString();
    }

    private String getFocusType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "Unknown";
        }
        Object principal = authentication.getPrincipal();

        if (principal == null || principal.equals("anonymousUser")) {
            return "Unknown";
        }

        QName type = WebComponentUtil.classToQName(getPageBase().getPrismContext(), WebModelServiceUtils.getLoggedInFocus().getClass());
        return type.getLocalPart();
    }
}
