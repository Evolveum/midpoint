/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Arrays;

/**
 * Created by Viliam Repan (lazyman).
 */
//@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
//@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private Environment environment;
//    @Autowired
//    private AuthenticationProvider authenticationProvider;
//    @Autowired
//    private AuthenticationManager authenticationManager;

    @Autowired
    private MidPointAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    @Autowired private SessionRegistry sessionRegistry;

    @Value("${auth.sso.header:SM_USER}")
    private String principalRequestHeader;
    @Value("${auth.sso.env:REMOTE_USER}")
    private String principalRequestEnvVariable;

    @Value("${auth.cas.server.url:}")
    private String casServerUrl;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;

//    @Value("${auth.logout.url:/}")
//    private String authLogoutUrl;

    private ObjectPostProcessor<Object> objectPostProcessor;

    @Override
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
        super.setObjectPostProcessor(objectPostProcessor);
    }

    @Profile("!cas")
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new WicketLoginUrlAuthenticationEntryPoint("/login");
    }

//    @Bean
//    public MidPointGuiAuthorizationEvaluator accessDecisionManager(SecurityEnforcer securityEnforcer,
//                                                                   SecurityContextManager securityContextManager,
//                                                                   TaskManager taskManager) {
//        return new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager);
//    }

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

        web.ignoring().antMatchers("/actuator");
        web.ignoring().antMatchers("/actuator/health");
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

        http.logout().clearAuthentication(true)
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(auditedLogoutHandler);

        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);

        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/spring_security_login")
                .successHandler(authenticationSuccessHandler).permitAll();

        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler());

        if (!csrfEnabled) {
            http.csrf().disable();
        }

        http.headers().disable();
        http.headers().frameOptions().sameOrigin();

//        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
//            http.addFilterAt(casFilter(), CasAuthenticationFilter.class);
//            http.addFilterBefore(requestSingleLogoutFilter(), LogoutFilter.class);
////            http.addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class);
//        }
//
//        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("sso"))) {
//            http.addFilterBefore(requestHeaderAuthenticationFilter(), LogoutFilter.class);
//        }
//
//        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
//            http.addFilterBefore(requestAttributeAuthenticationFilter(), LogoutFilter.class);
//        }
    }

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public MidPointAccessDeniedHandler accessDeniedHandler() {
        return new MidPointAccessDeniedHandler();
    }

    @ConditionalOnMissingBean(name = "midPointAuthenticationProvider")
    @Bean
    public AuthenticationProvider midPointAuthenticationProvider() throws Exception {
        return objectPostProcessor.postProcess( new MidPointAuthenticationProvider());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(midPointAuthenticationProvider());
    }

//    @Bean
//    public MidPointAuthenticationSuccessHandler authenticationSuccessHandler() {
//        MidPointAuthenticationSuccessHandler handler = new MidPointAuthenticationSuccessHandler();
//        handler.setUseReferer(true);
//        handler.setDefaultTargetUrl("/login");
//
//        return handler;
//    }

//    @Bean
//    public AuditedLogoutHandler logoutHandler() {
//        AuditedLogoutHandler handler = new AuditedLogoutHandler();
//        handler.setDefaultTargetUrl(authLogoutUrl);
//
//        return handler;
//    }

//    @Profile("sso")
//    @Bean
//    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
//        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
//        filter.setPrincipalRequestHeader(principalRequestHeader);
//        filter.setExceptionIfHeaderMissing(false);
//        filter.setAuthenticationManager(authenticationManager);
//
//        return filter;
//    }

//    @Profile("ssoenv")
//    @Bean
//    public RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter() {
//        RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
//        filter.setPrincipalEnvironmentVariable(principalRequestEnvVariable);
//        filter.setExceptionIfVariableMissing(false);
//        filter.setAuthenticationManager(authenticationManager);
//
//        return filter;
//    }

//    @Profile("cas")
//    @Bean
//    public CasAuthenticationFilter casFilter() {
//        CasAuthenticationFilter filter = new CasAuthenticationFilter();
//        filter.setAuthenticationManager(authenticationManager);
//
//        return filter;
//    }

    @Profile("cas")
    @Bean
    public LogoutFilter requestSingleLogoutFilter() {
        LogoutFilter filter = new LogoutFilter(casServerUrl + "/logout", new SecurityContextLogoutHandler());
        filter.setFilterProcessesUrl("/logout");

        return filter;
    }

    @Bean
    public ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }

//    @Profile("cas")
//    @Bean
//    public SingleSignOutFilter singleSignOutFilter() {
//        SingleSignOutFilter filter = new SingleSignOutFilter();
//        filter.setCasServerUrlPrefix(casServerUrl);
//
//        return filter;
//    }

    public HttpSecurity getNewHttp() throws Exception {
        return getHttp();
    }

}

