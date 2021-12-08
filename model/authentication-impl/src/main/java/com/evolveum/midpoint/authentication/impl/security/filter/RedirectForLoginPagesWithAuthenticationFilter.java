/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.security.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author skublik
 */
public class RedirectForLoginPagesWithAuthenticationFilter extends OncePerRequestFilter {

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication instanceof MidpointAuthentication && authentication.isAuthenticated() && AuthSequenceUtil.isLoginPage(request)) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            redirectStrategy.sendRedirect(request, response, mpAuthentication.getAuthenticationChannel().getPathAfterSuccessfulAuthentication());
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
