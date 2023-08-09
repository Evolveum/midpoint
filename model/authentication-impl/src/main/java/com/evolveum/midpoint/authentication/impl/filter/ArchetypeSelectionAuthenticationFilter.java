/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.ArchetypeSelectionAuthenticationToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class ArchetypeSelectionAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/archetypeSelection", "POST");
    private static final String ARCHETYPE_OID_KEY = "archetypeOid";
    private static final String ALLOW_UNDEFINED_ARCHETYPE_KEY = "allowUndefinedArchetype";
    public ArchetypeSelectionAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    protected ArchetypeSelectionAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        var archetypeOid = StringUtils.defaultIfEmpty(getArchetypeOidFromRequest(request), null);
        var allowUndefinedArchetype = getAllowUndefinedArchetypeFromRequest(request);
        var authToken = createAuthenticationToken(archetypeOid, allowUndefinedArchetype);

        return this.getAuthenticationManager().authenticate(authToken);
    }

    private String getArchetypeOidFromRequest(HttpServletRequest request) {
        return request.getParameter(ARCHETYPE_OID_KEY);
    }

    private boolean getAllowUndefinedArchetypeFromRequest(HttpServletRequest request) {
        return Boolean.parseBoolean(request.getParameter(ALLOW_UNDEFINED_ARCHETYPE_KEY));
    }

    private ArchetypeSelectionAuthenticationToken createAuthenticationToken(String archetypeOid, boolean allowUndefinedArchetype) {
        return new ArchetypeSelectionAuthenticationToken(archetypeOid, allowUndefinedArchetype);
    }

}
