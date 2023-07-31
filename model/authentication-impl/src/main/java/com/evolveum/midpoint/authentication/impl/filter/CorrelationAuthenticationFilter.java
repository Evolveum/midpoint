/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.CorrelationVerificationToken;
import com.evolveum.midpoint.prism.path.ItemPath;

public class CorrelationAuthenticationFilter extends MidpointFocusVerificationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/correlation", "POST");

    public CorrelationAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public CorrelationAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    protected AbstractAuthenticationToken createAuthenticationToken(Map<ItemPath, String> attributeValues) {
        return new CorrelationVerificationToken(attributeValues, null); //TDO refactor
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        validateRequest(request);

        Map<ItemPath, String> attributeValues = obtainAttributeValues(request);
        String correlatorName = request.getParameter("correlatorName");
        AbstractAuthenticationToken authRequest = new CorrelationVerificationToken(attributeValues, correlatorName);

        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
