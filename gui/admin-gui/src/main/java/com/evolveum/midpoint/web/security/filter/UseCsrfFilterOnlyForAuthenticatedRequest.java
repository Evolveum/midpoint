/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.filter;

import javax.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import com.evolveum.midpoint.util.exception.SystemException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class UseCsrfFilterOnlyForAuthenticatedRequest implements RequestMatcher {

    private final RequestMatcher requireCsrfProtectionMatcher = CsrfFilter.DEFAULT_CSRF_MATCHER;

    @Override
    public boolean matches(HttpServletRequest request) {
        MidpointAuthentication mPAuthentication = getMidpointAuthentication();
        if (mPAuthentication != null && mPAuthentication.isAuthenticated()) {
            return requireCsrfProtectionMatcher.matches(request);
        }
        return false;
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
        return requireCsrfProtectionMatcher.matcher(request);
    }

    public static MidpointAuthentication getMidpointAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            throw new SystemException("web.security.flexAuth.auth.wrong.type");
        }
        return (MidpointAuthentication) authentication;
    }
}
