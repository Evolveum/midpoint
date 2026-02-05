/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

public class OtpModuleWebSecurityConfigurer
        extends ModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, OtpAuthenticationModuleType> {

    public static final String LOGIN_PAGE_URL = "/otp_verify";

    private static final String LOGIN_PROCESSING_URL = "/otp_verify";

    public OtpModuleWebSecurityConfigurer(
            OtpAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {

        super(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request, provider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        OtpAuthenticationFilter filter = new OtpAuthenticationFilter();
        MidpointFormLoginConfigurer configurer = new MidpointFormLoginConfigurer(filter);

        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");

        getOrApply(http, configurer)
                .loginPage(LOGIN_PAGE_URL)
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + LOGIN_PROCESSING_URL)
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new OtpAuthenticationSucessHandler())).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint(LOGIN_PAGE_URL));

        configureDefaultLogout(http);

        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }
}
