/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;

/**
 * @author skublik
 */
public class MidPointAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private String defaultTargetUrl;

    public MidPointAuthenticationSuccessHandler() {
        setRequestCache(new HttpSessionRequestCache());
    }

    private RequestCache requestCache;

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    public RequestCache getRequestCache() {
        return requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        String urlSuffix = AuthConstants.DEFAULT_PATH_AFTER_LOGIN;
        String authenticatedChannel = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
            if (mpAuthentication.getAuthenticationChannel() != null) {
                authenticatedChannel = mpAuthentication.getAuthenticationChannel().getChannelId();
                boolean continueSequence = false;
                if (mpAuthentication.getPrincipal() instanceof MidPointPrincipal) {
                    MidPointPrincipal principal = (MidPointPrincipal) mpAuthentication.getPrincipal();
                    SecurityPolicyType securityPolicy = principal.getApplicableSecurityPolicy();
                    if (securityPolicy != null) {
                        AuthenticationSequenceType processingSequence = mpAuthentication.getSequence();
                        AuthenticationSequenceType sequence = SecurityPolicyUtil.findSequenceByIdentifier(securityPolicy, processingSequence.getIdentifier());
                        if (processingSequence.getModule().size() != sequence.getModule().size()) {
                            continueSequence = true;
                        }
                    }
                }
                if (mpAuthentication.isAuthenticated() && !continueSequence) {
                    urlSuffix = mpAuthentication.getAuthenticationChannel().getPathAfterSuccessfulAuthentication();
                    mpAuthentication.getAuthenticationChannel().postSuccessAuthenticationProcessing();
                    if (mpAuthentication.getAuthenticationChannel().isPostAuthenticationEnabled()) {
                        getRedirectStrategy().sendRedirect(request, response, urlSuffix);
                        return;
                    }
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
                channelSavedRequest = AuthSequenceUtil.searchChannelByPath(localePath);
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
