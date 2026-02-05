/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.filter.MidpointUsernamePasswordAuthenticationFilter;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class OtpAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String SPRING_SECURITY_FORM_CODE_KEY = "code";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        String username = getIdentifiedUsername();
        if (username == null) {
            throw new AuthenticationServiceException("Authentication failed: username not available.");
        }

        String codeStr = request.getParameter(SPRING_SECURITY_FORM_CODE_KEY);
        Integer code;
        try {
            code = Integer.valueOf(codeStr);
        } catch (NumberFormatException e) {
            throw new AuthenticationServiceException("Authentication failed: invalid code format.");
        }

        UsernamePasswordAuthenticationToken authRequest = new OtpAuthenticationToken(username, code);

        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private String getIdentifiedUsername() {
        MidpointAuthentication midpointAuthentication = AuthUtil.getMidpointAuthentication();
        if (!(midpointAuthentication.getPrincipal() instanceof MidPointPrincipal principal)) {
            return null;
        }

        FocusType focus = principal.getFocus();
        return focus.getName() != null ? focus.getName().getOrig() : null;
    }
}
