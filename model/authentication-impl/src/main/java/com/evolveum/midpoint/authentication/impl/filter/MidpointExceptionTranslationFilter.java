/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.savedrequest.RequestCache;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class MidpointExceptionTranslationFilter extends ExceptionTranslationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointExceptionTranslationFilter.class);

    private final RequestCache requestCache;

    public MidpointExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint, RequestCache requestCache) {
        super(authenticationEntryPoint, requestCache);
        this.requestCache = requestCache;
    }

    @Override
    protected void sendStartAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, AuthenticationException reason) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
            requestCache.saveRequest(request, response);
        }
        if (reason != null) {
            LOGGER.debug(reason.getMessage());
        }
        LOGGER.debug("Calling Authentication entry point.");
        getAuthenticationEntryPoint().commence(request, response, reason);
        if (authentication instanceof MidpointAuthentication){
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken
                    && !mpAuthentication.hasSucceededAuthentication()) {
                AuthenticationSequenceChannelType channel = mpAuthentication.getSequence() != null ?
                        mpAuthentication.getSequence().getChannel() : null;
                moduleAuthentication.setAuthentication(
                        createNewAuthentication((AnonymousAuthenticationToken) moduleAuthentication.getAuthentication(),
                                channel));
                mpAuthentication.setPrincipal(null);
            }
            SecurityContextHolder.getContext().setAuthentication(mpAuthentication);
        }
    }

    protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication,
            AuthenticationSequenceChannelType channel) {
        return null;
    }

}
