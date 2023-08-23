/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import java.util.UUID;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.UseCsrfFilterOnlyForAuthenticatedRequest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.impl.entry.point.RemoteAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.module.configuration.RemoteModuleWebSecurityConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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

public abstract class RemoteModuleWebSecurityConfigurer<C extends RemoteModuleWebSecurityConfiguration, MT extends AbstractAuthenticationModuleType> extends ModuleWebSecurityConfigurer<C, MT> {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteModuleWebSecurityConfigurer.class);

    @Autowired private ModelAuditRecorder auditProvider;
    @Autowired private AuthModuleRegistryImpl authRegistry;
    @Autowired private AuthChannelRegistryImpl authChannelRegistry;
    @Autowired SystemObjectCache systemObjectCache;

    public RemoteModuleWebSecurityConfigurer(MT moduleType, String prefix, AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, prefix, authenticationChannel, postProcessor, request, provider);
    }

    protected ModelAuditRecorder getAuditProvider() {
        return auditProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.securityMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        http.csrf().requireCsrfProtectionMatcher(new UseCsrfFilterOnlyForAuthenticatedRequest());

        MidpointExceptionHandlingConfigurer exceptionConfigurer = new MidpointExceptionHandlingConfigurer() {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken anonymousAuthenticationToken,
                    AuthenticationSequenceChannelType channel) {
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

    protected String getPublicUrlPrefix(ServletRequest request) {
        try {
            PrismObject<SystemConfigurationType> systemConfig = systemObjectCache.getSystemConfiguration(new OperationResult("load system configuration"));
            return SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfig.asObjectable(), request.getServerName());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
            return null;
        }
    }
}
