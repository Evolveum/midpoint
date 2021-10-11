/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.web.security.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.MidpointAuthenticationFauileHandler;
import com.evolveum.midpoint.web.security.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.web.security.filter.SecurityQuestionsAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.web.security.module.configuration.LoginFormModuleWebSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import static org.springframework.security.saml.util.StringUtils.stripEndingSlases;

/**
 * @author skublik
 */

public class SecurityQuestionsFormModuleWebSecurityConfig<C extends LoginFormModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    private C configuration;

    public SecurityQuestionsFormModuleWebSecurityConfig(C configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    public HttpSecurity getNewHttpSecurity() throws Exception {
        return getHttp();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.antMatcher(stripEndingSlases(getPrefix()) + "/**");
        getOrApply(http, new MidpointFormLoginConfigurer(new SecurityQuestionsAuthenticationFilter()))
                .loginPage("/securityquestions")
                .loginProcessingUrl(stripEndingSlases(getPrefix()) + "/spring_security_login")
                .failureHandler(new MidpointAuthenticationFauileHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler().setPrefix(configuration.getPrefix()))).permitAll();
        getOrApply(http, new MidpointExceptionHandlingConfigurer())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/securityquestions"));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() +"/logout"))
//                .logoutUrl(stripEndingSlases(getPrefix()) +"/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());
    }
}
