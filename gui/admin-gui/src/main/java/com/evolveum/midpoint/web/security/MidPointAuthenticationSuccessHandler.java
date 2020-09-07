/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.evolveum.midpoint.gui.api.GuiConstants;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author skublik
 */
public class MidPointAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private String defaultTargetUrl;

    // TODO: this seems to be useless, but setPrefix is called from multiple places
    private String prefix = "";

    public MidPointAuthenticationSuccessHandler setPrefix(String prefix) {
        this.prefix = "/" + stripSlashes(prefix) + "/";
        return this;
    }

    public MidPointAuthenticationSuccessHandler() {
        setRequestCache(new HttpSessionRequestCache());
    }

    private RequestCache requestCache;

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        String urlSuffix = GuiConstants.DEFAULT_PATH_AFTER_LOGIN;
        String authenticatedChannel = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(StateOfModule.SUCCESSFULLY);
            if (mpAuthentication.getAuthenticationChannel() != null) {
                authenticatedChannel = mpAuthentication.getAuthenticationChannel().getChannelId();
                if (mpAuthentication.isAuthenticated()) {
                    urlSuffix = mpAuthentication.getAuthenticationChannel().getPathAfterSuccessfulAuthentication();
                    mpAuthentication.getAuthenticationChannel().postSuccessAuthenticationProcessing();
                } else {
                    urlSuffix = mpAuthentication.getAuthenticationChannel().getPathDuringProccessing();
                }
            }
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null && savedRequest.getRedirectUrl().contains(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/")) {
            String target = savedRequest.getRedirectUrl().substring(0, savedRequest.getRedirectUrl().indexOf(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/")) + urlSuffix;
            getRedirectStrategy().sendRedirect(request, response, target);
            return;
        }
        if (savedRequest != null && authenticatedChannel != null) {
            int startIndex = savedRequest.getRedirectUrl().indexOf(request.getContextPath()) + request.getContextPath().length();
            int endIndex = savedRequest.getRedirectUrl().length() - 1;
            String channelSavedRequest = null;
            if ((startIndex < endIndex)) {
                String localePath = savedRequest.getRedirectUrl().substring(startIndex, endIndex);
                channelSavedRequest = SecurityUtils.searchChannelByPath(localePath);
            }
            if (channelSavedRequest == null) {
                channelSavedRequest = SecurityPolicyUtil.DEFAULT_CHANNEL;
            }
            if (!(channelSavedRequest.equals(authenticatedChannel))) {
                getRedirectStrategy().sendRedirect(request, response, urlSuffix);
                return;
            }

        } else {
            setDefaultTargetUrl(urlSuffix);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected String getTargetUrlParameter() {

        return defaultTargetUrl;
    }

    @Override
    public void setDefaultTargetUrl(String defaultTargetUrl) {
        this.defaultTargetUrl = defaultTargetUrl;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(defaultTargetUrl)) {
            return super.determineTargetUrl(request, response);
        }

        return defaultTargetUrl;
    }
}
