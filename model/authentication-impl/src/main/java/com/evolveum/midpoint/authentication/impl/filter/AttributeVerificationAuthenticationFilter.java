/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.AttributeVerificationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.security.api.SecurityUtil;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class AttributeVerificationAuthenticationFilter extends MidpointFocusVerificationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/attributeVerification", "POST");
    public AttributeVerificationAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    protected AttributeVerificationAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    @Override
    protected AbstractAuthenticationToken createAuthenticationToken(Map<ItemPath, String> attributeValues) {
        return new AttributeVerificationToken(SecurityUtil.getPrincipalSilent(), attributeValues);
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

}
