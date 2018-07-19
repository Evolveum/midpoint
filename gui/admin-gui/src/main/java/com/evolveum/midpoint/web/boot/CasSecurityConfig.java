/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.boot;

import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
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
    public AuthenticationProvider midPointAuthenticationProvider(UserDetailsService userDetailsService) throws Exception {
        CasAuthenticationProvider provider = new CasAuthenticationProvider();
        provider.setAuthenticationUserDetailsService(new UserDetailsByNameServiceWrapper<>(userDetailsService));
        provider.setServiceProperties(serviceProperties());
        provider.setTicketValidator(createTicketValidatorInstance());
        provider.setKey("CAS_ID");

        return provider;
    }

    private TicketValidator createTicketValidatorInstance() throws Exception {
        if (StringUtils.isEmpty(ticketValidator)) {
            return new Cas30ServiceTicketValidator(casServerUrl);
        }

        if (!ticketValidator.contains(".")) {
            ticketValidator = "org.jasig.cas.client.validation." + ticketValidator;
        }

        Class<TicketValidator> type = (Class) Class.forName(ticketValidator);
        Constructor<TicketValidator> c = type.getConstructor(String.class);
        return c.newInstance(casServerUrl);
    }
}
