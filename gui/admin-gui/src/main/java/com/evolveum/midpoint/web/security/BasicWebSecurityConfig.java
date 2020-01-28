/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.MidpointRequestAttributeAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.configurers.AuthFilterConfigurer;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author skublik
 */

@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
@EnableWebSecurity
public class BasicWebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Trace LOGGER = TraceManager.getTrace(BasicWebSecurityConfig.class);

    @Autowired
    private AuthModuleRegistryImpl authRegistry;

    @Autowired
    AuthChannelRegistryImpl authChannelRegistry;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Value("${auth.sso.header:SM_USER}")
    private String principalRequestHeader;

    @Value("${auth.sso.env:REMOTE_USER}")
    private String principalRequestEnvVariable;

    @Value("${auth.cas.server.url:}")
    private String casServerUrl;

    @Value("${auth.logout.url:/}")
    private String authLogoutUrl;

    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    public BasicWebSecurityConfig(){
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

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        // Web (SOAP) services
        web.ignoring().antMatchers("/model/**");

        // REST service
        web.ignoring().requestMatchers(new RequestMatcher() {
            @Override
            public boolean matches(HttpServletRequest httpServletRequest) {
                AntPathMatcher mather = new AntPathMatcher();
                boolean isExperimentalEnabled = false;
                try {
                    isExperimentalEnabled = SystemConfigurationTypeUtil.isExperimentalCodeEnabled(
                            systemObjectCache.getSystemConfiguration(new OperationResult("Load System Config")).asObjectable());
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't load system configuration", e);
                }
                if (isExperimentalEnabled
                        && mather.match("/ws/rest/**", httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length()))) {
                    return false;
                }
                if (mather.match("/ws/**", httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length()))) {
                    return true;
                }
                return false;
            }
        });
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

        AnonymousAuthenticationFilter anonymousFilter = new MidpointAnonymousAuthenticationFilter(authRegistry, authChannelRegistry, UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        http.setSharedObject(AuthenticationTrustResolverImpl.class, new MidpointAuthenticationTrustResolverImpl());
        http.addFilter(new WebAsyncManagerIntegrationFilter())
                .sessionManagement().and()
                .securityContext();
        http.apply(new AuthFilterConfigurer());


        http.sessionManagement()
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .maxSessionsPreventsLogin(true);
    }

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
//        providers.add(midPointAuthenticationProvider());
        return new MidpointProviderManager(providers);
    }

//    @ConditionalOnMissingBean(name = "midPointAuthenticationProvider")
//    @Bean
//    public AuthenticationProvider midPointAuthenticationProvider() throws Exception {
//        return new MidPointAuthenticationProvider();
//    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.authenticationProvider(midPointAuthenticationProvider);
//    }


//    @Profile("sso")
//    @Bean
//    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
//        MidpointRequestHeaderAuthenticationFilter filter = new MidpointRequestHeaderAuthenticationFilter();
//        filter.setPrincipalRequestHeader(principalRequestHeader);
//        filter.setExceptionIfHeaderMissing(false);
//        filter.setAuthenticationManager(authenticationManager);
//        filter.setAuthenticationFailureHandler(new MidpointAuthenticationFauileHandler());
//
//        return filter;
//    }

    @Profile("ssoenv")
    @Bean
    public RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter() {
        MidpointRequestAttributeAuthenticationFilter filter = new MidpointRequestAttributeAuthenticationFilter();
        filter.setPrincipalEnvironmentVariable(principalRequestEnvVariable);
        filter.setExceptionIfVariableMissing(false);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationFailureHandler(new MidpointAuthenticationFauileHandler());

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
