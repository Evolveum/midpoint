/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.util.Map;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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
        return new CorrelationVerificationToken(attributeValues, null, 0); //TDO refactor
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        validateRequest(request);

        Map<ItemPath, String> attributeValues = obtainAttributeValues(request);
        String correlatorName = request.getParameter(AuthConstants.ATTR_VERIFICATION_CORRELATOR_NAME);
        String correlatorIndex = request.getParameter(AuthConstants.ATTR_VERIFICATION_CORRELATOR_INDEX);
        int correlatorIndexVal = 0;
        if (StringUtils.isNotEmpty(correlatorIndex)) {
            try {
                correlatorIndexVal = Integer.parseInt(correlatorIndex);
            } catch (Exception e) {
                //nothing to do here
            }
        }
        CorrelationVerificationToken authRequest = new CorrelationVerificationToken(attributeValues, correlatorName, correlatorIndexVal);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
