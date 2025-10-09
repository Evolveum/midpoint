/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public class HintAuthenticationFilter extends MidpointFocusVerificationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/hint", "POST");


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
