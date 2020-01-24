/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * @author skublik
 */
public class HttpAuthenticationEntryPoint implements AuthenticationEntryPoint{

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) throws IOException {

       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            List<ModuleAuthentication> parallelProcessingModules = mpAuthentication.getParallelProcessingModules();
            if (!parallelProcessingModules.isEmpty()) {
                StringBuilder sb = new StringBuilder();

                boolean first = true;
                for (ModuleAuthentication moduleAuthentication : parallelProcessingModules) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(moduleAuthentication.getNameOfModuleType().getName())
                            .append(" realm=\"").append(moduleAuthentication.getNameOfModule()).append("\"");
                }
                response.setHeader("WWW-Authenticate",sb.toString());
            }
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(" test error ");
        response.getWriter().flush();
        response.getWriter().close();
    }
}
