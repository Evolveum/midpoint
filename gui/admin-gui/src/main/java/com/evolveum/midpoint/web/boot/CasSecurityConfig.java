/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.lang.reflect.Constructor;

/**
 * Created by Viliam Repan (lazyman).
 */
@Profile("cas")
@Configuration
public class CasSecurityConfig {

    @Value("${auth.cas.midpoint.url}")
    private String casMidpointUrl;
    @Value("${auth.cas.server.url}")
    private String casServerUrl;
    @Value("${auth.cas.ticketValidator}")
    private String ticketValidator;

    @Bean
    public ServiceProperties serviceProperties() {
        ServiceProperties properties = new ServiceProperties();
        properties.setService(casMidpointUrl + "/login/cas");
        properties.setSendRenew(false);

        return properties;
    }

    @Bean
    public CasAuthenticationEntryPoint authenticationEntryPoint() {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl(casServerUrl + "/login");
        entryPoint.setServiceProperties(serviceProperties());

        return entryPoint;
    }

    @Profile("cas")
    @Bean
    public AuthenticationProvider midPointAuthenticationProvider(UserDetailsService guiProfiledPrincipalManager) throws Exception {
        CasAuthenticationProvider provider = new CasAuthenticationProvider();
        provider.setAuthenticationUserDetailsService(new UserDetailsByNameServiceWrapper<>(guiProfiledPrincipalManager));
        provider.setServiceProperties(serviceProperties());
        provider.setTicketValidator(createTicketValidatorInstance());
        provider.setKey("CAS_ID");

        return provider;
    }

    private TicketValidator createTicketValidatorInstance() throws Exception {
        if (!StringUtils.contains(ticketValidator, "\\.")) {
            ticketValidator = "org.jasig.cas.client.validation." + ticketValidator;
        }

        Class<TicketValidator> type = (Class) Class.forName(ticketValidator);
        Constructor<TicketValidator> c = type.getConstructor(String.class);

        return c.newInstance(casServerUrl);
    }
}
