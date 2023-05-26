/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module;

import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.web.security.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.web.security.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.web.security.filter.MailNonceAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointFormLoginConfigurer;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MailNonceFormModuleWebSecurityConfig<C extends ModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    private C configuration;

    public MailNonceFormModuleWebSecurityConfig(C configuration) {
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
        http.antMatcher(SecurityUtils.stripEndingSlashes(getPrefix()) + "/**");
        getOrApply(http, new MidpointFormLoginConfigurer(new MailNonceAuthenticationFilter()))
                .loginPage(getConfiguration().getSpecificLoginUrl() == null ? "/emailNonce" : getConfiguration().getSpecificLoginUrl())
                .failureHandler(new MidpointAuthenticationFailureHandler())
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler().setPrefix(configuration.getPrefix()))).permitAll();
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
//                .logoutUrl(stripEndingSlases(getPrefix()) +"/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(createLogoutHandler());
    }

    private boolean uriMatchesInvitationChannel(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.endsWith(SchemaConstants.CHANNEL_INVITATION_QNAME.getLocalPart());
    }
}
