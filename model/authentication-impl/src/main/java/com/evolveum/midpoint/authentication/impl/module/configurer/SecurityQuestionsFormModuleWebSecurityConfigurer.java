/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.SecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsFormAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * @author skublik
 */

public class SecurityQuestionsFormModuleWebSecurityConfigurer extends ModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, SecurityQuestionsFormAuthenticationModuleType> {

    public SecurityQuestionsFormModuleWebSecurityConfigurer(SecurityQuestionsFormAuthenticationModuleType moduleType,
            String prefixOfSequence,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, prefixOfSequence, authenticationChannel, postProcessor, request, provider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, new MidpointFormLoginConfigurer<>(new SecurityQuestionsAuthenticationFilter()))
                .loginPage("/securityquestions")
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler())).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/securityquestions"));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() +"/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());
    }
}
