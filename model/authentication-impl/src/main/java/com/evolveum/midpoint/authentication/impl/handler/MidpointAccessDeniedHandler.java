/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.handler;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.WicketRedirectStrategy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

public class MidpointAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler defaultHandler = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        boolean ended = handleInternal(request, response, accessDeniedException);
        if (ended) {
            return;
        }

        defaultHandler.handle(request, response, accessDeniedException);
    }

    protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return true;
        }

        if (accessDeniedException instanceof CsrfException) {
            // handle invalid csrf token exception gracefully when user tries to log in/out with expired exception
            // handle session timeout for ajax cases -> redirect to base context (login)
            if (WicketRedirectStrategy.isWicketAjaxRequest(request)) {
                WicketRedirectStrategy redirect = new WicketRedirectStrategy();
                redirect.sendRedirect(request, response, request.getContextPath());
            } else {
                response.sendRedirect(request.getContextPath());
            }

            return true;
        }
        return false;
    }
}
