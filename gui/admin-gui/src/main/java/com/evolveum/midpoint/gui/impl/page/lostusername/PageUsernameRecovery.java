/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.self.PageSelf;

import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/loginRecovery", matchUrlForSecurity = "/loginRecovery")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERNAME_RECOVERY_URL) })
public class PageUsernameRecovery extends AbstractPageLogin {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageUsernameRecovery.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageUsernameRecovery.class);
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    private static final String ID_LOGIN_NAME_PANEL = "loginNamePanel";
    private static final String ID_LOGIN_NAME = "loginName";
    private static final String ID_PHOTO = "photo";
    private static final String ID_CONFIGURED_ITEMS_PANEL = "configuredItemsPanel";
    private static final String ID_ITEM_NAME = "itemName";
    private static final String ID_ITEM_VALUE = "itemValue";

    private LoadableModel<List<ItemPathType>> configuredItemsModel;


    public PageUsernameRecovery() {
        super();
        initModels();
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void initCustomLayout() {
        WebMarkupContainer loginNamePanel = new WebMarkupContainer(ID_LOGIN_NAME_PANEL);
        loginNamePanel.setOutputMarkupId(true);
        loginNamePanel.add(new VisibleBehaviour(() -> !configuredItemsExist()));
        add(loginNamePanel);

        NonCachingImage img = new NonCachingImage(ID_PHOTO, getImageResource());
        loginNamePanel.add(img);

        Label label = new Label(ID_LOGIN_NAME, getAuthorizedUserLoginNameModel());
        label.add(new VisibleBehaviour(this::isUserFound));
        loginNamePanel.add(label);

        initConfiguredItemsPanel();
    }

    private void initModels() {
        configuredItemsModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemPathType> load() {
                return loadConfiguredItemPathList();
            }
        };
    }

    private List<ItemPathType> loadConfiguredItemPathList() {
        List<ItemPathType> configuredItems = new ArrayList<>();

        var task = createSimpleTask(OPERATION_GET_SECURITY_POLICY);
        var result = new OperationResult(OPERATION_GET_SECURITY_POLICY);
        try {
            var securityPolicy = getModelInteractionService().getSecurityPolicy(getMidpointPrincipal().getFocusPrismObject(),
                    getMidpointAuthentication().getArchetypeOid(), task, result);
            var loginNameRecoveryConfig = securityPolicy.getLoginNameRecovery();
            configuredItems = loginNameRecoveryConfig.getItemToDisplay();
        } catch (Exception e) {
            LOGGER.debug("Unable to load the configured items list for login recovery page, ", e);
        }
        return configuredItems;
    }

    private boolean configuredItemsExist() {
        return configuredItemsModel != null && CollectionUtils.isNotEmpty(configuredItemsModel.getObject());
    }

    private AbstractResource getImageResource() {
        byte[] photo = null;
        if (isUserFound()) {
            var guiProfile = getPrincipal().getCompiledGuiProfile();
            photo = guiProfile.getJpegPhoto();
        }
        if (photo == null) {
            URL defaultImage = this.getClass().getClassLoader().getResource("static/img/placeholder.png");
            if (defaultImage == null) {
                return null;
            }
            try {
                photo = IOUtils.toByteArray(defaultImage);
            } catch (IOException e) {
                return null;
            }
        }
        return new ByteArrayResource("image/jpeg", photo);
    }

    private IModel<String> getAuthorizedUserLoginNameModel() {
        var principal = getMidpointPrincipal();
        var loginName = principal == null ? "" : principal.getUsername();
        return Model.of(loginName);
    }

    private void initConfiguredItemsPanel() {
        ListView<ItemPathType> configuredItemsPanel = new ListView<>(ID_CONFIGURED_ITEMS_PANEL, configuredItemsModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> item) {
                Label itemName = new Label(ID_ITEM_NAME, resolveItemName(item.getModelObject()));
                item.add(itemName);

                Label itemValue = new Label(ID_ITEM_VALUE, resolveItemValue(item.getModelObject()));
                item.add(itemValue);
            }
        };
        configuredItemsPanel.setOutputMarkupId(true);
        configuredItemsPanel.add(new VisibleBehaviour(() -> isUserFound() && configuredItemsExist()));
        add(configuredItemsPanel);
    }

    private IModel<String> resolveItemName(ItemPathType itemPath) {
        return () -> {
            ItemDefinition<?> def = new UserType().asPrismObject().getDefinition().findItemDefinition(itemPath.getItemPath());
            return WebComponentUtil.getItemDefinitionDisplayNameOrName(def);
        };
    }

    private IModel<String> resolveItemValue(ItemPathType itemPath) {
        return () -> {
            FocusType focus = getPrincipalFocus();
            var value = focus.asPrismObject().findItem(itemPath.getItemPath()).getRealValue();
            return value == null ? "" : value.toString();
        };
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource(getTitleKey());
    }

    private String getTitleKey() {
        return isUserFound() ? "PageUsernameRecovery.title.success" : "PageUsernameRecovery.title.fail";
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource(getTitleDescriptionKey());
    }

    private String getTitleDescriptionKey() {
        if (isUserFound() && configuredItemsExist()) {
            return "PageUsernameRecovery.title.success.configuredItems.description";
        }
        if (isUserFound()) {
            return "PageUsernameRecovery.title.success.description";
        }
        return "PageUsernameRecovery.title.fail.description";
    }

    private boolean isUserFound() {
        return getMidpointPrincipal() != null;
    }

    private MidPointPrincipal getMidpointPrincipal() {
        var mpAuthentication = getMidpointAuthentication();
        var principal = mpAuthentication.getPrincipal();
        if (principal instanceof MidPointPrincipal) {
            return (MidPointPrincipal) principal;
        }
        return null;
    }

    private MidpointAuthentication getMidpointAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        return (MidpointAuthentication) authentication;
    }
}
