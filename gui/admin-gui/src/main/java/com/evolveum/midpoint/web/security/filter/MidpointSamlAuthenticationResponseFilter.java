/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.RequestState;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class MidpointSamlAuthenticationResponseFilter extends SamlAuthenticationResponseFilter {

    public MidpointSamlAuthenticationResponseFilter(SamlProviderProvisioning<ServiceProviderService> provisioning) {
        super(provisioning);
//        this.provisioning = provisioning;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean sendedRequest = false;
        Saml2ModuleAuthentication moduleAuthentication = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && RequestState.SENDED.equals(moduleAuthentication.getRequestState())) {
                sendedRequest = true;
            }
            boolean requiresAuthentication = requiresAuthentication((HttpServletRequest) req, (HttpServletResponse) res);

            if (!requiresAuthentication && sendedRequest) {
                AuthenticationServiceException exception = new AuthenticationServiceException("web.security.flexAuth.saml.not.response");
                unsuccessfulAuthentication((HttpServletRequest) req, (HttpServletResponse) res, exception);
                return;
            } else {
                if (moduleAuthentication != null && requiresAuthentication && sendedRequest) {
                    moduleAuthentication.setRequestState(RequestState.RECEIVED);
                }
                super.doFilter(req, res, chain);
            }
        } else {
            throw new AuthenticationServiceException("Unsupported type of Authentication");
        }

    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {

//        if (logger.isDebugEnabled()) {
//            logger.debug("Authentication request failed: " + failed.toString(), failed);
//            logger.debug("Updated SecurityContextHolder to contain null Authentication");
//            logger.debug("Delegating to authentication failure handler " + failureHandler);
//        }

        getRememberMeServices().loginFail(request, response);

        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }
}
