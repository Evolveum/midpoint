/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.duo;

import java.io.IOException;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.DuoModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.DuoRequestToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.MultiValueMap;

import com.evolveum.midpoint.authentication.impl.filter.RemoteAuthenticationFilter;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;

@Order
public class DuoAuthenticationFilter extends AbstractAuthenticationProcessingFilter implements RemoteAuthenticationFilter {

    private final static String DUO_CODE="duo_code";
    private final static String STATE="state";

    private final ModelAuditRecorder auditProvider;

    public DuoAuthenticationFilter(String filterProcessesUrl, ModelAuditRecorder auditProvider) {
        super(filterProcessesUrl);
        this.auditProvider = auditProvider;
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        MultiValueMap<String, String> params = toMultiMap(request.getParameterMap());
        if (!isAuthorizationResponse(params)) {
            LOGGER.error("Parameters from request doesn't contain " + DUO_CODE + " and " + STATE);
            throw new AuthenticationServiceException("web.security.provider.invalid");
        }

        @Nullable ModuleAuthentication duoModule = AuthUtil.getProcessingModuleIfExist();
        if (!(duoModule instanceof DuoModuleAuthentication)) {
            LOGGER.error("Couldn't get processing duo module");
            throw new AuthenticationServiceException("web.security.provider.invalid");
        }

        if(StringUtils.isEmpty(((DuoModuleAuthentication)duoModule).getDuoState())
                || !((DuoModuleAuthentication)duoModule).getDuoState().equals(params.getFirst(STATE))) {
            LOGGER.error("State from received request and state saved in authentication module do not match.");
            throw new AuthenticationServiceException("web.security.provider.invalid");
        }

        DuoRequestToken token = new DuoRequestToken(
                params.getFirst(DUO_CODE),
                ((DuoModuleAuthentication)duoModule).getDuoUsername());

        return getAuthenticationManager().authenticate(token);
    }

    private boolean isAuthorizationResponse(MultiValueMap<String, String> request) {
        return StringUtils.isNotEmpty(request.getFirst(DUO_CODE))
                && StringUtils.isNotEmpty(request.getFirst(STATE));
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        remoteUnsuccessfulAuthentication(
                request, response, failed, auditProvider, getRememberMeServices(), getFailureHandler(), "OIDC");
    }
}
