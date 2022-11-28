/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.entry.point;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import com.evolveum.midpoint.authentication.impl.module.authentication.HttpModuleAuthentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * @author skublik
 */
public class HttpAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String DEFAULT_REALM = "midpoint";

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            List<ModuleAuthentication> parallelProcessingModules =
                    mpAuthentication.getParallelProcessingModules();
            if (!parallelProcessingModules.isEmpty()) {
                for (ModuleAuthentication moduleAuthentication : parallelProcessingModules) {
                    response.addHeader("WWW-Authenticate", getRealmForHeader(moduleAuthentication, authException));
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private String getRealmForHeader(ModuleAuthentication moduleAuthentication, AuthenticationException authException) {
        if (moduleAuthentication instanceof HttpModuleAuthentication) {
            return ((HttpModuleAuthentication) moduleAuthentication).getRealmFroHeader(authException);
        }
        return moduleAuthentication.getModuleTypeName() +" realm=\"" + DEFAULT_REALM + "\"";
    }
}
