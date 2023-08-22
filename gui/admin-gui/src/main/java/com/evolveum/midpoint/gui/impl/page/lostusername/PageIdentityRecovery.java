/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.lostusername;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.login.PageSelfRegistration;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/identityRecovery", matchUrlForSecurity = "/identityRecovery")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_IDENTITY_RECOVERY_URL) })
public class PageIdentityRecovery extends AbstractPageLogin {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageIdentityRecovery.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageIdentityRecovery.class);
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    private static final String ID_RECOVERED_IDENTITIES = "recoveredIdentities";
    private static final String ID_LOGIN_NAME_PANEL = "loginNamePanel";
    private static final String ID_LOGIN_NAME = "loginName";
    private static final String ID_PHOTO = "photo";
    private static final String ID_CONFIGURED_ITEMS_PANEL = "configuredItemsPanel";
    private static final String ID_ITEM_NAME = "itemName";
    private static final String ID_ITEM_VALUE = "itemValue";
    private static final String ID_REGISTRATION_LINK = "registrationLink";

    private LoadableModel<List<UserType>> recoveredIdentitiesModel;
    private LoadableModel<List<ItemPathType>> configuredItemsModel;
    private LoadableModel<SecurityPolicyType> securityPolicyModel;


    public PageIdentityRecovery() {
        super();
        initModels();
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected void initCustomLayout() {
        ListView<UserType> recoveredIdentitiesPanel = new ListView<>(ID_RECOVERED_IDENTITIES, recoveredIdentitiesModel) {
            @Override
            protected void populateItem(ListItem<UserType> item) {
                initRecoveredIdentitiesPanel(item);
            }
        };
        recoveredIdentitiesPanel.setOutputMarkupId(true);
        add(recoveredIdentitiesPanel);

        String urlRegistration = SecurityUtils.getRegistrationUrl(securityPolicyModel.getObject());
        AjaxLink<String> registrationLink = new AjaxLink<String>(ID_REGISTRATION_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                PageSelfRegistration p  = new PageSelfRegistration((UserType) SecurityUtils.findCorrelationModuleAuthentication(PageIdentityRecovery.this).getPreFocus());
                AuthUtil.clearMidpointAuthentication();
                setResponsePage(p);
            }
        };
        registrationLink.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(urlRegistration) && !recoveredIdentitiesExist()));
        add(registrationLink);
    }

    private void initRecoveredIdentitiesPanel(ListItem<UserType> item) {
        initLoginNamePanel(item);
        initConfiguredItemsPanel(item);
    }

    private void initModels() {
        recoveredIdentitiesModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<UserType> load() {
                return getRecoveredIdentities();
            }
        };
        configuredItemsModel = new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemPathType> load() {
                return loadConfiguredItemPathList();
            }
        };
        securityPolicyModel = new LoadableModel<>(false) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected SecurityPolicyType load() {
                var archetypeOid = getMidpointAuthentication().getArchetypeOid();
                return runPrivileged((Producer<SecurityPolicyType>) () -> {
                    var task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
                    var result = new OperationResult(OPERATION_GET_SECURITY_POLICY);
                    try {
                        return getModelInteractionService().getSecurityPolicy(null, archetypeOid,
                                task, result);
                    } catch (Exception e) {
                        LOGGER.debug("Unable to load the configured items list for identity recovery page, ", e);
                    }
                    return null;
                });
            }
        };
    }

    private List<ItemPathType> loadConfiguredItemPathList() {
                 var identityRecoveryConfig = securityPolicyModel.getObject().getIdentityRecovery();
                return identityRecoveryConfig.getItemToDisplay();
    }

    private boolean configuredItemsExist() {
        return configuredItemsModel != null && CollectionUtils.isNotEmpty(configuredItemsModel.getObject());
    }

    private AbstractResource getImageResource(UserType user) {
        byte[] photo = null;
        if (user != null) {
            photo = user.getJpegPhoto();
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

    private void initLoginNamePanel(ListItem<UserType> item) {
        WebMarkupContainer loginNamePanel = new WebMarkupContainer(ID_LOGIN_NAME_PANEL);
        loginNamePanel.setOutputMarkupId(true);
        loginNamePanel.add(new VisibleBehaviour(() -> recoveredIdentitiesExist() && !configuredItemsExist()));
        item.add(loginNamePanel);

        NonCachingImage img = new NonCachingImage(ID_PHOTO, getImageResource(item.getModelObject()));
        loginNamePanel.add(img);

        Label label = new Label(ID_LOGIN_NAME, getUserLoginNameModel(item.getModelObject()));
        loginNamePanel.add(label);
    }

    private IModel<String> getUserLoginNameModel(UserType user) {
        return Model.of(LocalizationUtil.translatePolyString(user.getName()));
    }

    private void initConfiguredItemsPanel(ListItem<UserType> userItem) {
        var user = userItem.getModelObject();
        ListView<ItemPathType> configuredItemsPanel = new ListView<>(ID_CONFIGURED_ITEMS_PANEL, configuredItemsModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> attributeItem) {
                Label itemName = new Label(ID_ITEM_NAME, resolveItemName(user, attributeItem.getModelObject()));
                itemName.setOutputMarkupId(true);
                attributeItem.add(itemName);

                Label itemValue = new Label(ID_ITEM_VALUE, resolveItemValue(user, attributeItem.getModelObject()));
                itemValue.setOutputMarkupId(true);
                attributeItem.add(itemValue);
            }
        };
        configuredItemsPanel.setOutputMarkupId(true);
        configuredItemsPanel.add(new VisibleBehaviour(() -> recoveredIdentitiesExist() && configuredItemsExist()));
        userItem.add(configuredItemsPanel);
    }

    private IModel<String> resolveItemName(UserType user, ItemPathType itemPath) {
        return () -> {
            ItemDefinition<?> def = user.asPrismObject().getDefinition().findItemDefinition(itemPath.getItemPath());
            return WebComponentUtil.getItemDefinitionDisplayNameOrName(def);
        };
    }

    private IModel<String> resolveItemValue(UserType user, ItemPathType itemPath) {
        return () -> {
            var item = user.asPrismObject().findItem(itemPath.getItemPath());
            var value = item != null ? item.getRealValue() : null;
            return value == null ? "" : value.toString();
        };
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource(getTitleKey());
    }

    private String getTitleKey() {
        return recoveredIdentitiesExist() ? "PageIdentityRecovery.title.success" : "PageIdentityRecovery.title.fail";
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource(getTitleDescriptionKey());
    }

    private String getTitleDescriptionKey() {
        if (recoveredIdentitiesExist() && configuredItemsExist()) {
            return "PageIdentityRecovery.title.success.configuredItems.description";
        }
        if (recoveredIdentitiesExist()) {
            return "PageIdentityRecovery.title.success.description";
        }
        return "PageIdentityRecovery.title.fail.description";
    }

    private boolean recoveredIdentitiesExist() {
        return CollectionUtils.isNotEmpty(getRecoveredIdentities());
    }

    private boolean isSingleRecoveredIdentity() {
        List<UserType> recoveredIdentities = getRecoveredIdentities();
        return recoveredIdentities != null && recoveredIdentities.size() == 1;
    }

    private List<UserType> getRecoveredIdentities() {
        var correlationModuleAuth = SecurityUtils.findCorrelationModuleAuthentication(PageIdentityRecovery.this);
        if (isSuccessfullyAuthenticated(correlationModuleAuth)) {
            return correlationModuleAuth.getOwners()
                    .stream()
                    .filter(o -> o instanceof UserType)
                    .map(o -> (UserType) o)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    private boolean isSuccessfullyAuthenticated(CorrelationModuleAuthentication auth) {
        return auth != null && AuthenticationModuleState.SUCCESSFULLY.equals(auth.getState());
    }

    private MidpointAuthentication getMidpointAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        return (MidpointAuthentication) authentication;
    }
}
