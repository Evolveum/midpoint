/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.handler;

import java.io.IOException;

import com.evolveum.midpoint.authentication.impl.NotShowedAuthenticationServiceException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

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
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String urlSuffix = AuthConstants.DEFAULT_PATH_AFTER_LOGIN;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            if (mpAuthentication.isAuthenticated()) {
                getRedirectStrategy().sendRedirect(request, response, urlSuffix);
                return;
            }
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (mpAuthentication.getAuthenticationChannel() != null) {
                if (mpAuthentication.isLast(moduleAuthentication)){     //&& mpAuthentication.getAuthenticationChannel().isDefault()) { todo check this comment
                    urlSuffix = getPathAfterUnsuccessfulAuthentication(mpAuthentication.getAuthenticationChannel());
                } else {
                    urlSuffix = mpAuthentication.getAuthenticationChannel().getPathDuringProccessing();
                }
                if (!mpAuthentication.isLast(moduleAuthentication)) {
                    moduleAuthentication.recordFailure(exception);
                }
            }

            moduleAuthentication.recordFailure(exception);

            //abort the authentication in case of requisite module fail
            if (!mpAuthentication.isLast(moduleAuthentication)
                    && AuthenticationSequenceModuleNecessityType.REQUISITE.equals(moduleAuthentication.getNecessity())) {
                if (!(mpAuthentication.getAuthenticationExceptionIfExists() instanceof NotShowedAuthenticationServiceException)) {
                    saveException(request, mpAuthentication.getAuthenticationExceptionIfExists());
                }
                getRedirectStrategy().sendRedirect(request, response,
                        mpAuthentication.getAuthenticationChannel().getPathAfterUnsuccessfulAuthentication());
                return;
            }

            if (!(mpAuthentication.getAuthenticationExceptionIfExists() instanceof NotShowedAuthenticationServiceException)
                    && mpAuthentication.isLast(moduleAuthentication)
                    && !mpAuthentication.isAuthenticated()) {
                saveException(request, mpAuthentication.getAuthenticationExceptionIfExists());
            }

            if (!mpAuthentication.isOverLockoutMaxAttempts()) {
                getRedirectStrategy().sendRedirect(request, response, mpAuthentication.getAuthenticationChannel().getPathDuringProccessing());
                return;
            }
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
