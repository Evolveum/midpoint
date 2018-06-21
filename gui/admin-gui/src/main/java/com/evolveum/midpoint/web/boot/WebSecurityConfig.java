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

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * Created by Viliam Repan (lazyman).
 */
@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
//TODO
//@EnableGlobalMethodSecurity(securedEnabled = true)
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationProvider authenticationProvider;
    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;
    @Value("${auth.logout.url:/}")
    private String authLogoutUrl;
    
    @Bean
    public WicketLoginUrlAuthenticationEntryPoint wicketAuthenticationEntryPoint() {
        return new WicketLoginUrlAuthenticationEntryPoint("/login");
    }

    @Bean
    public MidPointGuiAuthorizationEvaluator accessDecisionManager(SecurityEnforcer securityEnforcer,
                                                                   SecurityContextManager securityContextManager,
                                                                   TaskManager taskManager) {
        return new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager);
    }

    @Profile("sso")
    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter(AuthenticationManager authenticationManager) {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader("SM_USER");
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
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
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .antMatchers("/j_spring_security_check",
                        "/spring_security_login",
                        "/login",
                        "/forgotpassword",
                        "/registration",
                        "/confirm/registration",
                        "/confirm/reset",
                        "/error",
                        "/error/*",
                        "/bootstrap").permitAll()
                .anyRequest().fullyAuthenticated();

        http.logout()
                .logoutUrl("/j_spring_security_logout")
                .invalidateHttpSession(true)
                .logoutSuccessHandler(logoutHandler());

        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true);

        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/spring_security_login")
                .successHandler(authenticationSuccessHandler()).permitAll();

        http.exceptionHandling()
                .authenticationEntryPoint(wicketAuthenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler());

        if (!csrfEnabled) {
            http.csrf().disable();
        }

        http.headers().disable();
    }

    @Bean
    public MidPointAccessDeniedHandler accessDeniedHandler() {
        return new MidPointAccessDeniedHandler();
    }

    @Profile({"!ldap", "!cas"})
    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new MidPointAuthenticationProvider();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    public MidPointAuthenticationSuccessHandler authenticationSuccessHandler() {
        MidPointAuthenticationSuccessHandler handler = new MidPointAuthenticationSuccessHandler();
        handler.setUseReferer(true);
        handler.setDefaultTargetUrl("/login");

        return handler;
    }

    @Bean
    public AuditedLogoutHandler logoutHandler() {
        AuditedLogoutHandler handler = new AuditedLogoutHandler();
        handler.setDefaultTargetUrl(authLogoutUrl);

        return handler;
    }
}

