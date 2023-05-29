/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.MailNonceAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;


/**
 * @author skublik
 */

public class MailNonceFormModuleWebSecurityConfigurer<C extends ModuleWebSecurityConfiguration> extends ModuleWebSecurityConfigurer<C> {

    public MailNonceFormModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.antMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, new MidpointFormLoginConfigurer<>(new MailNonceAuthenticationFilter()))
                .loginPage(getConfiguration().getSpecificLoginUrl() == null ? "/emailNonce" : getConfiguration().getSpecificLoginUrl())
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler())).permitAll();
        MidpointExceptionHandlingConfigurer exceptionConfigurer = new MidpointExceptionHandlingConfigurer() {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken anonymousAuthenticationToken,
                    AuthenticationSequenceChannelType channel) {
                if (channel != null && SchemaConstants.CHANNEL_INVITATION_URI.equals(channel.getChannelId())) {
                    anonymousAuthenticationToken.setAuthenticated(false);
                    return anonymousAuthenticationToken;
                }
                return null;
            }
        };
        getOrApply(http, exceptionConfigurer)
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint(
                        getConfiguration().getSpecificLoginUrl() == null ? "/emailNonce" : getConfiguration().getSpecificLoginUrl()));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(getLogoutMatcher(http, getPrefix() +"/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());
    }
}
