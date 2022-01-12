/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public abstract class AbstractPageRemoteAuthenticationSelect extends AbstractPageLogin implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ID_PROVIDERS = "providers";
    private static final String ID_PROVIDER = "provider";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";

    public AbstractPageRemoteAuthenticationSelect() {
    }

    @Override
    protected void initCustomLayer() {
        List<IdentityProvider> providers = getProviders();
        add(new ListView<IdentityProvider>(ID_PROVIDERS, providers) {
            @Override
            protected void populateItem(ListItem<IdentityProvider> item) {
                item.add(new ExternalLink(ID_PROVIDER, item.getModelObject().getRedirectLink(), item.getModelObject().getLinkText()));
            }
        });
        MidpointForm<?> form = new MidpointForm<>(ID_LOGOUT_FORM);
        ModuleAuthentication actualModule = AuthUtil.getProcessingModuleIfExist();
        form.add(new VisibleBehaviour(() -> existRemoteAuthentication(actualModule)));
        form.add(AttributeModifier.replace("action",
                (IModel<String>) () -> existRemoteAuthentication(actualModule) ?
                        SecurityUtils.getPathForLogoutWithContextPath(getRequest().getContextPath(), actualModule) : ""));
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    abstract protected boolean existRemoteAuthentication(ModuleAuthentication actualModule);

    private List<IdentityProvider> getProviders() {
        List<IdentityProvider> providers = new ArrayList<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof RemoteModuleAuthentication) {
                providers = ((RemoteModuleAuthentication) moduleAuthentication).getProviders();
                if (providers.isEmpty()) {
                    String key = "PageSamlSelect.empty.providers";
                    error(getString(key));
                }
                return providers;
            }
            String key = "PageSamlSelect.unsupported.authentication.type";
            error(getString(key));
            return providers;
        }
        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return providers;
    }

    @Override
    protected void confirmUserPrincipal() {
    }
}
