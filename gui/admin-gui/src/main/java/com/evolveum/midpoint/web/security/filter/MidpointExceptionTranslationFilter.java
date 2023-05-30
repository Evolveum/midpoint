/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MidpointExceptionTranslationFilter extends ExceptionTranslationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointExceptionTranslationFilter.class);

    private RequestCache requestCache;

    public MidpointExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint, RequestCache requestCache) {
        super(authenticationEntryPoint, requestCache);
        this.requestCache = requestCache;
    }

    @Override
    protected void sendStartAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, AuthenticationException reason) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!SecurityUtils.isRecordSessionLessAccessChannel(request)) {
            requestCache.saveRequest(request, response);
        }
        if (reason != null) {
            LOGGER.debug(reason.getMessage());
        }
        LOGGER.debug("Calling Authentication entry point.");
        getAuthenticationEntryPoint().commence(request, response, reason);
        if (authentication instanceof MidpointAuthentication){
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken) {
                moduleAuthentication.setAuthentication(
                        createNewAuthentication((AnonymousAuthenticationToken) moduleAuthentication.getAuthentication()));
                mpAuthentication.setPrincipal(null);
            }
            SecurityContextHolder.getContext().setAuthentication(mpAuthentication);
        }
    }

    protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication) {
        return null;
    }

}
