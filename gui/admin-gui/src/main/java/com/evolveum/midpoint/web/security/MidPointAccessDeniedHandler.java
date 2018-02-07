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
import org.springframework.security.web.csrf.MissingCsrfTokenException;

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

        // handle invalid csrf token exception gracefully when user tries to log in/out with expired exception
        if (isLoginLogoutRequest(request) && (accessDeniedException instanceof MissingCsrfTokenException)) {
            response.sendRedirect(request.getContextPath());
            return;
        }

        defaultHandler.handle(request, response, accessDeniedException);
    }

    private boolean isLoginLogoutRequest(HttpServletRequest req) {
        if (!"post".equalsIgnoreCase(req.getMethod())) {
            return false;
        }

        String uri = req.getRequestURI();
        return "/j_spring_security_logout".equals(uri) || "/spring_security_login".equals(uri);
    }
}
