/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.*;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MidpointFilterChainProxy extends FilterChainProxy {

    private static final transient Trace LOGGER = TraceManager.getTrace(MidpointFilterChainProxy.class);

    private final static String FILTER_APPLIED = MidpointFilterChainProxy.class.getName().concat(
            ".APPLIED");

    private List<SecurityFilterChain> filterChainsIgnoredPath;
    private List<SecurityFilterChain> filterChains;

    private MidpointFilterChainProxy.FilterChainValidator filterChainValidator = new MidpointFilterChainProxy.NullFilterChainValidator();

    private HttpFirewall firewall = new StrictHttpFirewall();

    @Autowired(required = false)
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    ApplicationContext context;

    @Autowired
    private MidPointGuiAuthorizationEvaluator accessDecisionManager;

    @Autowired
    private MidPointAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private MidPointAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private AuditedLogoutHandler auditedLogoutHandler;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private AuthenticationProvider midPointAuthenticationProvider;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter;

    @Autowired(required = false)
    private RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter;

    @Autowired(required = false)
    private CasAuthenticationFilter casFilter;

    @Autowired(required = false)
    private LogoutFilter requestSingleLogoutFilter;

    @Autowired
    private SamlConfiguration samlConfiguration;

    @Autowired
    private SamlProviderServerBeanConfiguration samlProviderServerBeanConfiguration;

    @Value("${security.enable-csrf:true}")
    private boolean csrfEnabled;

    @Value("${security.authentication.active:saml}")
    private String authenticationProfile;

    public MidpointFilterChainProxy() {
    }

    public MidpointFilterChainProxy(List<SecurityFilterChain> filterChains) {
        this.filterChainsIgnoredPath = filterChains;
    }

    @Override
    public void afterPropertiesSet() {
        filterChainValidator.validate(this);
    }

    private SecurityFilterChain getSamlFilter() throws Exception {
        SamlWebSecurity webSec = objectObjectPostProcessor.postProcess(new SamlWebSecurity(samlProviderServerBeanConfiguration, samlConfiguration));
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
        try {
            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        HttpSecurity http = webSec.getNewHttp();
        return http.build();
    }

    private SecurityFilterChain getSamlBasicFilter() throws Exception {
        DefaultWebSecurityConfig webSec = objectObjectPostProcessor.postProcess(new DefaultWebSecurityConfig());
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
        try {
            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        HttpSecurity http = webSec.getNewHttp();

        http.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/**").authenticated()
                .and()
                .formLogin().loginPage("/saml");

        return http.build();
    }

    private String readFile() throws IOException {
        String path = "/home/lskublik/test";
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    private SecurityFilterChain getInternalPasswordFilter() throws Exception {
        DefaultWebSecurityConfig webSec = initDefaultPasswordWebSecurityConfig();
        HttpSecurity http = webSec.getNewHttp();
        http.antMatcher("/internal/**")
                .authorizeRequests()
                .accessDecisionManager(accessDecisionManager)
                .anyRequest().fullyAuthenticated();
        http.formLogin()
                .loginPage("/internal/login")
                .loginProcessingUrl("/internal/spring_security_login")
                .successHandler(objectObjectPostProcessor.postProcess(new MidPointInternalAuthenticationSuccessHandler())).permitAll();
        http.exceptionHandling()
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint("/internal/login"));
        configureDefaultPasswordFilter(http);
        SecurityFilterChain filter = http.build();
        return filter;
    }

    private DefaultWebSecurityConfig initDefaultPasswordWebSecurityConfig(){
        DefaultWebSecurityConfig webSec = objectObjectPostProcessor.postProcess(new DefaultWebSecurityConfig());
        webSec.setObjectPostProcessor(objectObjectPostProcessor);
        webSec.setApplicationContext(context);
        webSec.addAuthenticationProvider(midPointAuthenticationProvider);
        try {
            webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        try {
            webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
        } catch( NoSuchBeanDefinitionException e ) {
        }
        return webSec;
    }

    private SecurityFilterChain getBasicPasswordFilter() throws Exception {
        DefaultWebSecurityConfig webSec = initDefaultPasswordWebSecurityConfig();
        HttpSecurity http = webSec.getNewHttp();
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
        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/spring_security_login")
                .successHandler(authenticationSuccessHandler).permitAll();
        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint);
        configureDefaultPasswordFilter(http);
        SecurityFilterChain filter = http.build();
        return filter;
    }

    private void configureDefaultPasswordFilter(HttpSecurity http) throws Exception {
        http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler);
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

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("cas"))) {
            http.addFilterAt(casFilter, CasAuthenticationFilter.class);
            http.addFilterBefore(requestSingleLogoutFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("sso"))) {
            http.addFilterBefore(requestHeaderAuthenticationFilter, LogoutFilter.class);
        }

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equalsIgnoreCase("ssoenv"))) {
            http.addFilterBefore(requestAttributeAuthenticationFilter, LogoutFilter.class);
        }

        if (!csrfEnabled) {
            http.csrf().disable();
        }
        http.headers().disable();
        http.headers().frameOptions().sameOrigin();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (filterChains != null){
            filterChains.clear();
        }

        List<SecurityFilterChain> filters = new ArrayList<SecurityFilterChain>();
        filters.addAll(filterChainsIgnoredPath);

        if (readFile().equals("saml\n")) {
            try {
                SecurityFilterChain filter = getInternalPasswordFilter();
                filters.add(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't build internal password filter", e);
            }

            try {
                SecurityFilterChain filter = getSamlFilter();
                filters.add(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't build saml basic filter", e);
            }

            try {
                SecurityFilterChain filter = getSamlBasicFilter();
                filters.add(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't build saml filter", e);
            }
        } else {

            try {
                SecurityFilterChain filter = getSamlFilter();
                filters.add(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't build saml basic filter", e);
            }

            try {
                SecurityFilterChain filter = getBasicPasswordFilter();
                filters.add(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't build basic password filter", e);
            }
        }

        this.filterChains = filters;

        boolean clearContext = request.getAttribute(FILTER_APPLIED) == null;
        if (clearContext) {
            try {
                request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
                doFilterInternal(request, response, chain);
            }
            finally {
                SecurityContextHolder.clearContext();
                request.removeAttribute(FILTER_APPLIED);
            }
        }
        else {
            doFilterInternal(request, response, chain);
        }
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {

        FirewalledRequest fwRequest = firewall
                .getFirewalledRequest((HttpServletRequest) request);
        HttpServletResponse fwResponse = firewall
                .getFirewalledResponse((HttpServletResponse) response);

        List<Filter> filters = getFilters(fwRequest);

        if (filters == null || filters.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(UrlUtils.buildRequestUrl(fwRequest)
                        + (filters == null ? " has no matching filters"
                        : " has an empty filter list"));
            }

            fwRequest.reset();

            chain.doFilter(fwRequest, fwResponse);

            return;
        }



        MidpointFilterChainProxy.VirtualFilterChain vfc = new MidpointFilterChainProxy.VirtualFilterChain(fwRequest, chain, filters);
        vfc.doFilter(fwRequest, fwResponse);
    }

    private List<Filter> getFilters(HttpServletRequest request) {
        for (SecurityFilterChain chain : filterChains) {
            if (chain.matches(request)) {
                return chain.getFilters();
            }
        }

        return null;
    }

    public List<Filter> getFilters(String url) {
        return getFilters(firewall.getFirewalledRequest((new FilterInvocation(url, "GET")
                .getRequest())));
    }

    public List<SecurityFilterChain> getFilterChains() {
        return Collections.unmodifiableList(filterChains);
    }

    public void setFilterChainValidator(MidpointFilterChainProxy.FilterChainValidator filterChainValidator) {
        this.filterChainValidator = filterChainValidator;
    }

    public void setFirewall(HttpFirewall firewall) {
        this.firewall = firewall;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FilterChainProxy[");
        sb.append("Filter Chains: ");
        sb.append(filterChains);
        sb.append("]");

        return sb.toString();
    }

    private static class VirtualFilterChain implements FilterChain {
        private final FilterChain originalChain;
        private final List<Filter> additionalFilters;
        private final FirewalledRequest firewalledRequest;
        private final int size;
        private int currentPosition = 0;

        private VirtualFilterChain(FirewalledRequest firewalledRequest,
                                   FilterChain chain, List<Filter> additionalFilters) {
            this.originalChain = chain;
            this.additionalFilters = additionalFilters;
            this.size = additionalFilters.size();
            this.firewalledRequest = firewalledRequest;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (currentPosition == size) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " reached end of additional filter chain; proceeding with original chain");
                }

                // Deactivate path stripping as we exit the security filter chain
                this.firewalledRequest.reset();

                originalChain.doFilter(request, response);
            }
            else {
                currentPosition++;

                Filter nextFilter = additionalFilters.get(currentPosition - 1);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " at position " + currentPosition + " of " + size
                            + " in additional filter chain; firing Filter: '"
                            + nextFilter.getClass().getSimpleName() + "'");
                }

                nextFilter.doFilter(request, response, this);
            }
        }
    }

    public interface FilterChainValidator {
        void validate(MidpointFilterChainProxy filterChainProxy);
    }

    private static class NullFilterChainValidator implements MidpointFilterChainProxy.FilterChainValidator {
        @Override
        public void validate(MidpointFilterChainProxy filterChainProxy) {
        }
    }

}
