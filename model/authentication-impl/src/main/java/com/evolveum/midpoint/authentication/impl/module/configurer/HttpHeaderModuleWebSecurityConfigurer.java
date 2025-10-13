/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import java.io.IOException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpHeaderAuthenticationModuleType;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.configuration.HttpHeaderModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.MidpointRequestHeaderAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * @author skublik
 */

public class HttpHeaderModuleWebSecurityConfigurer extends LoginFormModuleWebSecurityConfigurer<HttpHeaderModuleWebSecurityConfiguration, HttpHeaderAuthenticationModuleType> {

    @Autowired private MidpointProviderManager authenticationManager;

    public HttpHeaderModuleWebSecurityConfigurer(HttpHeaderAuthenticationModuleType httpHeaderAuthenticationModuleType,
            String prefixOfSequence, AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(httpHeaderAuthenticationModuleType, prefixOfSequence, authenticationChannel, postProcessor, request, provider);
    }

    @Override
    protected HttpHeaderModuleWebSecurityConfiguration buildConfiguration(HttpHeaderAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        return HttpHeaderModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        getOrApply(http, getMidpointFormLoginConfigurer())
                .loginPage("/error/401");

        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/error/401"));

        http.addFilterBefore(requestHeaderAuthenticationFilter(), LogoutFilter.class);
        http.logout().logoutSuccessHandler(createLogoutHandler(getConfiguration().getDefaultSuccessLogoutURL()));
    }


    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        MidpointRequestHeaderAuthenticationFilter filter = new MidpointRequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(getConfiguration().getPrincipalRequestHeader());
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationFailureHandler(new MidpointAuthenticationFailureHandler() {
            @Override
            protected String getPathAfterUnsuccessfulAuthentication(AuthenticationChannel authenticationChannel) {
                return "/error/401";
            }
        });
        MidPointAuthenticationSuccessHandler successHandler = new MidPointAuthenticationSuccessHandler(){
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
                if (getRequestCache().getRequest(request, response) == null) {
                    getRequestCache().saveRequest(request, response);
                }
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
        filter.setAuthenticationSuccessHandler(getObjectPostProcessor().postProcess(successHandler));
        filter.setSessionRegistry(getSessionRegistry());

        return filter;
    }
}
