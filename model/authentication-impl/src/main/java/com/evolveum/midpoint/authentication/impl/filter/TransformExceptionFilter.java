/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

/**
 * @author skublik
 */

public class TransformExceptionFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(TransformExceptionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (!response.isCommitted()) {
                if (AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }
}
