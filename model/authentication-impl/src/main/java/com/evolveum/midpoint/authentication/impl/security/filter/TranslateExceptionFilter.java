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

import org.springframework.web.filter.OncePerRequestFilter;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.impl.security.util.AuthSequenceUtil;

/**
 * @author skublik
 */

public class TranslateExceptionFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(TranslateExceptionFilter.class);

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
