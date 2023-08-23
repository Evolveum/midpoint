/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class UseCsrfFilterOnlyForAuthenticatedRequest implements RequestMatcher {

    private final RequestMatcher requireCsrfProtectionMatcher = CsrfFilter.DEFAULT_CSRF_MATCHER;

    @Override
    public boolean matches(HttpServletRequest request) {
        MidpointAuthentication mPAuthentication = AuthUtil.getMidpointAuthentication();
        if (mPAuthentication != null && mPAuthentication.isAuthenticated()) {
            return requireCsrfProtectionMatcher.matches(request);
        }
        return false;
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
        return requireCsrfProtectionMatcher.matcher(request);
    }
}
