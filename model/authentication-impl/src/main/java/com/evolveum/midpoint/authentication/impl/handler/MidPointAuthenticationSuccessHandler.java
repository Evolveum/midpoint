/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.handler;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

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
        if (requestCache instanceof HttpSessionRequestCache httpSessionRequestCache) {
            httpSessionRequestCache.setMatchingRequestParameterName(null);
        }

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
        MidpointAuthentication mpAuthentication = AuthUtil.getMidpointAuthentication();
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        moduleAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
        if (mpAuthentication.getAuthenticationChannel() != null) {
            authenticatedChannel = mpAuthentication.getAuthenticationChannel().getChannelId();

            if (mpAuthentication.isLast(moduleAuthentication) && !mpAuthentication.isAuthenticated()) {
                urlSuffix = mpAuthentication.getAuthenticationChannel().getPathAfterUnsuccessfulAuthentication();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", mpAuthentication.getAuthenticationExceptionIfExists());
                }

                getRedirectStrategy().sendRedirect(request, response, urlSuffix);
                return;
            }
            if (mpAuthentication.isAuthenticated()) {
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
            //TODO: record success?


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
