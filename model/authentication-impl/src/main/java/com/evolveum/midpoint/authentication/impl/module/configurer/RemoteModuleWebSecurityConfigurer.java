/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import java.util.UUID;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.impl.entry.point.RemoteAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.module.configuration.RemoteModuleWebSecurityConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.prism.PrismContext;

/**
 * @author skublik
 */

public abstract class RemoteModuleWebSecurityConfigurer<C extends RemoteModuleWebSecurityConfiguration> extends ModuleWebSecurityConfigurer<C> {

    @Autowired
    private ModelAuditRecorder auditProvider;

    @Autowired
    private AuthModuleRegistryImpl authRegistry;

    @Autowired
    private AuthChannelRegistryImpl authChannelRegistry;

    public RemoteModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    protected ModelAuditRecorder getAuditProvider() {
        return auditProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.antMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        http.csrf().disable();

        MidpointExceptionHandlingConfigurer exceptionConfigurer = new MidpointExceptionHandlingConfigurer() {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken anonymousAuthenticationToken) {
                if (anonymousAuthenticationToken.getDetails() != null
                        && getAuthTokenClass().isAssignableFrom(anonymousAuthenticationToken.getDetails().getClass())) {
                    return (Authentication) anonymousAuthenticationToken.getDetails();
                }
                return null;
            }
        };
        getOrApply(http, exceptionConfigurer)
                .authenticationEntryPoint(new RemoteAuthenticationEntryPoint(getAuthEntryPointUrl()));

        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher(getPrefix() + "/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(getLogoutRequestSuccessHandler());
    }

    protected abstract String getAuthEntryPointUrl();

    protected abstract LogoutSuccessHandler getLogoutRequestSuccessHandler();

    @Override
    protected AnonymousAuthenticationFilter createAnonymousFilter() {
        AnonymousAuthenticationFilter filter = new MidpointAnonymousAuthenticationFilter(authRegistry, authChannelRegistry, PrismContext.get(),
                UUID.randomUUID().toString(), "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")){
            @Override
            protected void processAuthentication(ServletRequest req) {
                if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
                    MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                    ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
                    if (moduleAuthentication != null
                            && (moduleAuthentication.getAuthentication() == null
                            || getAuthTokenClass().isAssignableFrom(moduleAuthentication.getAuthentication().getClass()))) {
                        Authentication authentication = createBasicAuthentication((HttpServletRequest) req);
                        moduleAuthentication.setAuthentication(authentication);
                        mpAuthentication.setPrincipal(authentication.getPrincipal());
                    }
                }
            }
        };

        filter.setAuthenticationDetailsSource(new RemoteAuthenticationDetailsSource(getAuthTokenClass()));
        return filter;
    }

    protected abstract Class<? extends Authentication> getAuthTokenClass();

    private static class RemoteAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, Object> {

        private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();
        private final Class<? extends Authentication> getAuthTokenClass;

        private RemoteAuthenticationDetailsSource(Class<? extends Authentication> getAuthTokenClass) {
            this.getAuthTokenClass = getAuthTokenClass;
        }

        @Override
        public Object buildDetails(HttpServletRequest context) {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                if (moduleAuthentication != null && moduleAuthentication.getAuthentication() != null
                        && this.getAuthTokenClass.isAssignableFrom(moduleAuthentication.getAuthentication().getClass())) {
                    return moduleAuthentication.getAuthentication();
                }
            }
            return detailsSource.buildDetails(context);
        }
    }
}
