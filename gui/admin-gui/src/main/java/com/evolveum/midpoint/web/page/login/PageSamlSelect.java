/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.IdentityProvider;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author skublik
 */
@PageDescriptor(urls = {
                    @Url(mountUrl = "/saml2/select", matchUrlForSecurity = "/saml2/select")
                }, permitAll = true, loginPage = true)
public class PageSamlSelect extends AbstractPageLogin implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSamlSelect.class);

    public PageSamlSelect() {
    }

    @Override
    protected void initCustomLayer() {
        List<IdentityProvider> providers = getProviders();
        add(new ListView<IdentityProvider>("providers", providers) {
            @Override
            protected void populateItem(ListItem<IdentityProvider> item) {
                item.add(new ExternalLink("provider", item.getModelObject().getRedirectLink(), item.getModelObject().getLinkText()));
            }
        });
    }

    private List<IdentityProvider> getProviders() {
        List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && moduleAuthentication instanceof Saml2ModuleAuthentication){
                providers = ((Saml2ModuleAuthentication) moduleAuthentication).getProviders();
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
}
