/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointAccessDeniedHandler implements AccessDeniedHandler {

    private AccessDeniedHandler defaultHandler = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
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

            return;
        }

        defaultHandler.handle(request, response, accessDeniedException);
    }
}
