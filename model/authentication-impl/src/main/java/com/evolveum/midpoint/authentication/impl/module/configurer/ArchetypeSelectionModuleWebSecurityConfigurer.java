/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.ArchetypeSelectionAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointAttributeConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;

import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class ArchetypeSelectionModuleWebSecurityConfigurer extends ModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, ArchetypeSelectionModuleType> {


    public ArchetypeSelectionModuleWebSecurityConfigurer(ArchetypeSelectionModuleType moduleType,
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
        ArchetypeSelectionAuthenticationFilter archetypeSelectionFilter =
                new ArchetypeSelectionAuthenticationFilter();
        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, new MidpointAttributeConfigurer<>(archetypeSelectionFilter))
                .loginPage("/archetypeSelection")
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler())).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/archetypeSelection"));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() +"/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());

        http.addFilterAfter(archetypeSelectionFilter, UsernamePasswordAuthenticationFilter.class);
    }

}
