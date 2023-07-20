/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.ArchetypeSelectionAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.prism.path.ItemPath;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

public class ArchetypeSelectionAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/archetypeSelection", "POST");
    private static final String SPRING_SECURITY_FORM_ARCHETYPE_OID_KEY = "archetypeOid";
    public ArchetypeSelectionAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    protected ArchetypeSelectionAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        var archetypeOid = getArchetypeOidFromRequest(request);
        var authToken = createAuthenticationToken(archetypeOid);

        return this.getAuthenticationManager().authenticate(authToken);
    }

    private String getArchetypeOidFromRequest(HttpServletRequest request) {
        return request.getParameter(SPRING_SECURITY_FORM_ARCHETYPE_OID_KEY);
    }

    private ArchetypeSelectionAuthenticationToken createAuthenticationToken(String archetypeOid) {
        return new ArchetypeSelectionAuthenticationToken(archetypeOid);
    }

}
