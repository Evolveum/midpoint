/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;

import static org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityDsl.serviceProvider;

/**
 * @author skublik
 */

public class SamlWebSecurity extends SamlServiceProviderSecurityConfiguration {

    private SamlConfiguration SamlConfiguration;

    public SamlWebSecurity(SamlProviderServerBeanConfiguration samlProviderServerBeanConfiguration, SamlConfiguration SamlConfiguration) {//@Qualifier("appConfig") AppConfig appConfig) {
        super("/saml/", samlProviderServerBeanConfiguration);
        this.SamlConfiguration = SamlConfiguration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.apply(serviceProvider())
                .configure(SamlConfiguration);
    }

    public HttpSecurity getNewHttp() throws Exception {
        return getHttp();
    }
}

//    @Configuration
//    @Order(1000000000)
//    public static class SamlSecurity extends SamlServiceProviderSecurityConfiguration {
//
//        private SamlConfiguration SamlConfiguration;
//
//        public SamlSecurity(SamlProviderServerBeanConfiguration samlProviderServerBeanConfiguration, SamlConfiguration SamlConfiguration) {//@Qualifier("appConfig") AppConfig appConfig) {
//            super("/saml/", samlProviderServerBeanConfiguration);
//            this.SamlConfiguration = SamlConfiguration;
//        }
//
//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            super.configure(http);
//            http.apply(serviceProvider())
//                    .configure(SamlConfiguration);
//        }
//    }

//    @Configuration
//    @Order(SecurityProperties.BASIC_AUTH_ORDER)
//    public static class AppSecurity extends WebSecurityConfigurerAdapter {
//
//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            http.antMatcher("/**")
//                .authorizeRequests()
//                .antMatchers("/**").authenticated()
//                .and()
//                .formLogin().loginPage("/saml/select");
//        }
//
//
//    }
//}
