/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.configurers.AuthFilterConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author skublik
 */
@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
@EnableWebSecurity
public class BasicWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthModuleRegistryImpl authRegistry;

    @Autowired
    AuthChannelRegistryImpl authChannelRegistry;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    PrismContext prismContext;

    @Autowired
    private RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;

    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    public BasicWebSecurityConfig() {
        super(true);
    }

    @Override
    public void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectObjectPostProcessor = objectPostProcessor;
        super.setObjectPostProcessor(objectPostProcessor);
    }

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
        handler.setDefaultTargetUrl("/");

        return handler;
    }

    @Bean
    public AuditedAccessDeniedHandler accessDeniedHandler() {
        return objectObjectPostProcessor.postProcess(new AuditedAccessDeniedHandler());
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new WicketLoginUrlAuthenticationEntryPoint("/login");
    }

    @Bean
    @SessionAndRequestScope
    @Override
    protected MidpointAuthenticationManager authenticationManager() throws Exception {
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
        return new MidpointProviderManager(providers);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        // Web (SOAP) services
        web.ignoring().antMatchers("/model/**");

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

        web.ignoring().antMatchers("/favicon.ico");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AnonymousAuthenticationFilter anonymousFilter = new MidpointAnonymousAuthenticationFilter(authRegistry, authChannelRegistry, prismContext,
                UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        http.setSharedObject(AuthenticationTrustResolverImpl.class, new MidpointAuthenticationTrustResolverImpl());
        http.addFilter(new WebAsyncManagerIntegrationFilter())
                .sessionManagement().and()
                .securityContext();
        http.apply(new AuthFilterConfigurer());

        createSessionContextRepository(http);

        http.sessionManagement()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);
    }

    private void createSessionContextRepository(HttpSecurity http) {
        HttpSessionSecurityContextRepository httpSecurityRepository = new HttpSessionSecurityContextRepository() {
            @Override
            public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
                if(!SecurityUtils.isRecordSessionLessAccessChannel(request)) {
                    super.saveContext(context, request, response);
                }
            }

            @Override
            protected SecurityContext generateNewContext() {
                return new MidpointSecurityContext(super.generateNewContext(), removeUnusedSecurityFilterPublisher);
            }
        };
        httpSecurityRepository.setDisableUrlRewriting(true);
        AuthenticationTrustResolver trustResolver = http.getSharedObject(AuthenticationTrustResolver.class);
        if (trustResolver != null) {
            httpSecurityRepository.setTrustResolver(trustResolver);
        }
        http.setSharedObject(SecurityContextRepository.class, httpSecurityRepository);
    }

//    TODO not used, don't delete because of possible future implementation authentication module
//
//    @Value("${auth.sso.env:REMOTE_USER}")
//    private String principalRequestEnvVariable;
//
//    @Profile("ssoenv")
//    @Bean
//    public RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter() {
//        MidpointRequestAttributeAuthenticationFilter filter = new MidpointRequestAttributeAuthenticationFilter();
//        filter.setPrincipalEnvironmentVariable(principalRequestEnvVariable);
//        filter.setExceptionIfVariableMissing(false);
//        filter.setAuthenticationManager(authenticationManager);
//        filter.setAuthenticationFailureHandler(new MidpointAuthenticationFauileHandler());
//
//        return filter;
//    }

//    TODO not used, don't delete because of possible future implementation CAS authentication module
//
//    @Value("${auth.cas.server.url:}")
//    private String casServerUrl;
//
//    @Profile("cas")
//    @Bean
//    public CasAuthenticationFilter casFilter() {
//        CasAuthenticationFilter filter = new CasAuthenticationFilter();
//        filter.setAuthenticationManager(authenticationManager);
//
//        return filter;
//    }
//
//    @Profile("cas")
//    @Bean
//    public LogoutFilter requestSingleLogoutFilter() {
//        LogoutFilter filter = new LogoutFilter(casServerUrl + "/logout", new SecurityContextLogoutHandler());
//        filter.setFilterProcessesUrl("/logout");
//
//        return filter;
//    }
//
//    @Profile("cas")
//    @Bean
//    public SingleSignOutFilter singleSignOutFilter() {
//        SingleSignOutFilter filter = new SingleSignOutFilter();
//        filter.setCasServerUrlPrefix(casServerUrl);
//
//        return filter;
//    }

    @Bean
    public ServletListenerRegistrationBean httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }
}
