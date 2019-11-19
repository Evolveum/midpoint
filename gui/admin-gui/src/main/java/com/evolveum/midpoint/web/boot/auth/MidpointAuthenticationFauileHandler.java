/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
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

public class MidpointAuthenticationFauileHandler extends SimpleUrlAuthenticationFailureHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();

    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    public RequestCache getRequestCache() {
        return requestCache;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        saveException(request, exception);

        SavedRequest savedRequest = getRequestCache().getRequest(request, response);

        if (savedRequest == null || StringUtils.isBlank(savedRequest.getRedirectUrl())) {
            getRedirectStrategy().sendRedirect(request, response, "self/dashboard");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, savedRequest.getRedirectUrl());
    }
}
