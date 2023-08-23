/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author skublik
 */
public abstract class AbstractPageRemoteAuthenticationSelect extends AbstractPageLogin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROVIDERS = "providers";
    private static final String ID_PROVIDER = "provider";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";

    public AbstractPageRemoteAuthenticationSelect() {
    }

    @Override
    protected void initCustomLayout() {
        List<IdentityProvider> providers = getProviders();
        add(new ListView<>(ID_PROVIDERS, providers) {
            @Override
            protected void populateItem(ListItem<IdentityProvider> item) {
                item.add(new ExternalLink(ID_PROVIDER, item.getModelObject().getRedirectLink(), item.getModelObject().getLinkText()));
            }
        });
        MidpointForm<?> form = new MidpointForm<>(ID_LOGOUT_FORM);
        ModuleAuthentication actualModule = AuthUtil.getProcessingModuleIfExist();
        if (actualModule != null) {
            Class<? extends Authentication> actualAuthClass = actualModule.getAuthentication().getClass();
            Class<?> detailsClass = actualModule.getAuthentication().getDetails() != null ?
                    actualModule.getAuthentication().getDetails().getClass() : null;
            String authName = actualModule.getModuleTypeName();
            form.add(new VisibleBehaviour(() -> existRemoteAuthentication(actualAuthClass, detailsClass, authName)));
            String prefix = actualModule.getPrefix();

            form.add(AttributeModifier.replace("action",
                    (IModel<String>) () -> {
                        boolean exist = existRemoteAuthentication(actualAuthClass, detailsClass, authName);

                        if (exist) {
                            return SecurityUtils.getPathForLogoutWithContextPath(getRequest().getContextPath(), prefix);
                        }

                        return "";
                    }));
        } else {
            form.add(new VisibleBehaviour(() -> false));
        }
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    abstract protected Class<? extends Authentication> getSupportedAuthToken();

    private boolean existRemoteAuthentication(
            Class<? extends Authentication> actualAuthClass,
            Class<?> detailsClass,
            String actualModuleName) {
        return getClass().getAnnotation(PageDescriptor.class).authModule().equals(actualModuleName)
                && (getSupportedAuthToken().isAssignableFrom(actualAuthClass)
                || (AnonymousAuthenticationToken.class.isAssignableFrom(actualAuthClass)
                && detailsClass != null
                && getSupportedAuthToken().isAssignableFrom(detailsClass)));
    }

    private List<IdentityProvider> getProviders() {
        List<IdentityProvider> providers = new ArrayList<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof RemoteModuleAuthentication) {
                providers = ((RemoteModuleAuthentication) moduleAuthentication).getProviders();
                if (providers.isEmpty()) {
                    String key = getErrorKeyEmptyProviders();
                    error(getString(key));
                }
                return providers;
            }
            String key = getErrorKeyUnsupportedType();
            error(getString(key));
            return providers;
        }
        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return providers;
    }

    protected abstract String getErrorKeyUnsupportedType();

    protected abstract String getErrorKeyEmptyProviders();

}
