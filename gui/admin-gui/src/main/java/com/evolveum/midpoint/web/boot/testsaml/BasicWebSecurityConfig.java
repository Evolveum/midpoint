/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.*;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * @author skublik
 */

@Configuration
@EnableWebSecurity
public class BasicWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Value("${auth.sso.header:SM_USER}")
    private String principalRequestHeader;

    @Value("${auth.sso.env:REMOTE_USER}")
    private String principalRequestEnvVariable;

    @Value("${auth.cas.server.url:}")
    private String casServerUrl;

    @Value("${auth.logout.url:/}")
    private String authLogoutUrl;

    @Bean
    public MidPointGuiAuthorizationEvaluator accessDecisionManager(SecurityEnforcer securityEnforcer,
                                                                   SecurityContextManager securityContextManager,
                                                                   TaskManager taskManager) {
        return new MidPointGuiAuthorizationEvaluator(securityEnforcer, securityContextManager, taskManager);
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

    @Bean
    public MidPointAccessDeniedHandler accessDeniedHandler() {
        return new MidPointAccessDeniedHandler();
    }

    @Profile("!cas")
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new WicketLoginUrlAuthenticationEntryPoint("/login");
    }

//    @Override
//    public void init(WebSecurity web) throws Exception {
//        //rewrite init method because this is basic configuration for bean and ignored paths
//    }

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

//            web.debug(true);
    }

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @ConditionalOnMissingBean(name = "midPointAuthenticationProvider")
    @Bean
    public AuthenticationProvider midPointAuthenticationProvider() throws Exception {
        return new MidPointAuthenticationProvider();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(midPointAuthenticationProvider);
    }


    @Profile("sso")
    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(principalRequestHeader);
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }

    @Profile("ssoenv")
    @Bean
    public RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter() {
        RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
        filter.setPrincipalEnvironmentVariable(principalRequestEnvVariable);
        filter.setExceptionIfVariableMissing(false);
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }

    @Profile("cas")
    @Bean
    public CasAuthenticationFilter casFilter() {
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }

    @Profile("cas")
    @Bean
    public LogoutFilter requestSingleLogoutFilter() {
        LogoutFilter filter = new LogoutFilter(casServerUrl + "/logout", new SecurityContextLogoutHandler());
        filter.setFilterProcessesUrl("/logout");

        return filter;
    }

    @Profile("cas")
    @Bean
    public SingleSignOutFilter singleSignOutFilter() {
        SingleSignOutFilter filter = new SingleSignOutFilter();
        filter.setCasServerUrlPrefix(casServerUrl);

        return filter;
    }

    @Bean
    public ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }
}
