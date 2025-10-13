/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RefuseUnauthenticatedRequestFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(RefuseUnauthenticatedRequestFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication mpAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (AuthSequenceUtil.isPermitAll(request)
                || AuthSequenceUtil.isLoginPage(request)
                || (mpAuthentication instanceof MidpointAuthentication && mpAuthentication.isAuthenticated())) {
            filterChain.doFilter(request, response);
            return;
        }

        LOGGER.debug("Unauthenticated request");
        throw new AuthenticationServiceException("Unauthenticated request");
    }
}
