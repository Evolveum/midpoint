/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.AttributeVerificationAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointAttributeConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;

public class AttributeVerificationModuleWebSecurityConfigurer extends ModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, AttributeVerificationAuthenticationModuleType> {

    public AttributeVerificationModuleWebSecurityConfigurer(AttributeVerificationAuthenticationModuleType moduleType,
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
        AttributeVerificationAuthenticationFilter verificationFilter = new AttributeVerificationAuthenticationFilter();
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, new MidpointAttributeConfigurer<>(verificationFilter))
                .loginPage("/attributeVerification")
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler())).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/attributeVerification"));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() +"/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());

        http.addFilterBefore(verificationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
