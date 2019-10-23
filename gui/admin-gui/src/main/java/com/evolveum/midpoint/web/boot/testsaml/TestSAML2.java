/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.evolveum.midpoint.web.boot.testsaml;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;

import static org.springframework.security.saml.provider.service.config.SamlServiceProviderSecurityDsl.serviceProvider;

@EnableWebSecurity
public class TestSAML2 {

    @Configuration
    @Order(1000000000)
    public static class SamlSecurity extends SamlServiceProviderSecurityConfiguration {

        private AppConfig appConfig;

        public SamlSecurity(BeanConfig beanConfig, @Qualifier("appConfig") AppConfig appConfig) {
            super("/saml/sp/", beanConfig);
            this.appConfig = appConfig;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            super.configure(http);
            http.apply(serviceProvider())
                    .configure(appConfig);
        }
    }

    @Configuration
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    public static class AppSecurity extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/**")
                    .authorizeRequests()
                    .antMatchers("/**").authenticated()
                    .and()
                    .formLogin().loginPage("/saml/sp/select");
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            super.configure(web);
            // Web (SOAP) services
            web.ignoring().antMatchers("/model/**");
            web.ignoring().antMatchers("/ws/**");

            // REST service
            web.ignoring().antMatchers("/rest/**");

            // Special intra-cluster service to download and delete report outputs
            web.ignoring().antMatchers("/report");

            web.ignoring().antMatchers("/js/**");
            web.ignoring().antMatchers("/css/**");
            web.ignoring().antMatchers("/img/**");
            web.ignoring().antMatchers("/fonts/**");

            web.ignoring().antMatchers("/wro/**");
            web.ignoring().antMatchers("/static-web/**");
            web.ignoring().antMatchers("/less/**");

            web.ignoring().antMatchers("/wicket/resource/**");

            web.ignoring().antMatchers("/actuator");
            web.ignoring().antMatchers("/actuator/health");
        }
    }
}