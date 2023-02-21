/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.HintAuthenticationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class HintAuthenticationFilter extends MidpointFocusVerificationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/hint", "POST");
    private static final String SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY = "attributeValues";

    public HintAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public HintAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    protected void validateRequest(HttpServletRequest request) {
        super.validateRequest(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser(authentication);
        if (principal == null) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }
    }

    @Override
    protected AbstractAuthenticationToken createAuthenticationToken(Map<ItemPath, String> attributeValues) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new HintAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials());
    }

}
