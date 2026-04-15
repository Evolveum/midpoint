/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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

    private static final Pattern CODE_PATTERN = Pattern.compile("^(?:\\d{6}|\\d{8})$");

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication failed, request method not supported.");
        }

        String username = getIdentifiedUsername();
        if (username == null) {
            throw new AuthenticationServiceException("Authentication failed, username not available.");
        }

        String codeStr = request.getParameter(SPRING_SECURITY_FORM_CODE_KEY);
        if (StringUtils.isEmpty(codeStr) || !CODE_PATTERN.matcher(codeStr).matches()) {
            throw new AuthenticationServiceException("Authentication failed, invalid code format.");
        }

        Integer code = Integer.valueOf(codeStr);

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
