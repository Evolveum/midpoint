/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.gui.api.GuiConstants;
import com.evolveum.midpoint.model.api.authentication.*;

import com.evolveum.midpoint.web.security.saml.NotShowedAuthenticationServiceException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MidpointAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();

    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    public RequestCache getRequestCache() {
        return requestCache;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String urlSuffix = GuiConstants.DEFAULT_PATH_AFTER_LOGIN;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            if (mpAuthentication.isAuthenticated()) {
                getRedirectStrategy().sendRedirect(request, response, urlSuffix);
                return;
            }
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (mpAuthentication.getAuthenticationChannel() != null) {
                if (mpAuthentication.isLast(moduleAuthentication) && mpAuthentication.getAuthenticationChannel().isDefault()) {
                    urlSuffix = getPathAfterUnsuccessfulAuthentication(mpAuthentication.getAuthenticationChannel());
                } else {
                    urlSuffix = mpAuthentication.getAuthenticationChannel().getPathDuringProccessing();
                }
            }
            moduleAuthentication.setState(StateOfModule.FAILURE);
        }

        if (!(exception instanceof NotShowedAuthenticationServiceException)) {
            saveException(request, exception);
        }

        SavedRequest savedRequest = getRequestCache().getRequest(request, response);

        if (savedRequest == null || StringUtils.isBlank(savedRequest.getRedirectUrl())
                || ((DefaultSavedRequest) savedRequest).getServletPath().startsWith(ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH)) {
            getRedirectStrategy().sendRedirect(request, response, urlSuffix);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, savedRequest.getRedirectUrl());
    }

    protected String getPathAfterUnsuccessfulAuthentication(AuthenticationChannel authenticationChannel) {
        return authenticationChannel.getPathAfterUnsuccessfulAuthentication();
    }
}
