/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.NodeAuthenticationToken;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RefuseUnauthenticatedRequestFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(RefuseUnauthenticatedRequestFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (AuthSequenceUtil.isPermitAll(request)
                || AuthSequenceUtil.isLoginPage(request)
                || (authentication instanceof MidpointAuthentication && authentication.isAuthenticated())
                || (AuthSequenceUtil.isClusterSequence(request)
                    && authentication instanceof NodeAuthenticationToken
                    && authentication.isAuthenticated())) {
            filterChain.doFilter(request, response);
            return;
        }

        LOGGER.debug("Unauthenticated request");
        throw new AuthenticationServiceException("Unauthenticated request");
    }
}
