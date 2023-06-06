/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author skublik
 */

public class MailNonceAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    private final RequestMatcher requiredFilter = httpServletRequest -> {
        if (httpServletRequest.getParameter(SchemaConstants.USER_ID) != null
                && httpServletRequest.getParameter(SchemaConstants.TOKEN) != null) {
            return true;
        }
        return false;
    };

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return this.requiredFilter.matches(request);
    }

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter(SchemaConstants.USER_ID);
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(SchemaConstants.TOKEN);
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("GET")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        } else {
            String username = this.obtainUsername(request);
            String password = this.obtainPassword(request);
            if (username == null) {
                username = "";
            }

            if (password == null) {
                password = "";
            }

            username = username.trim();
            MailNonceAuthenticationToken authRequest = new MailNonceAuthenticationToken(username, password);
            this.setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        }
    }
}
