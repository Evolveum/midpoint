/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import static com.evolveum.midpoint.schema.util.SecurityPolicyUtil.NO_CUSTOM_IGNORED_LOCAL_PATH;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;

/**
 * @author skublik
 */

public class MidpointAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAnonymousAuthenticationFilter.class);

    private final AuthModuleRegistryImpl authRegistry;
    private final AuthChannelRegistryImpl authChannelRegistry;
    private final PrismContext prismContext;
    private final String key;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

    public MidpointAnonymousAuthenticationFilter(AuthModuleRegistryImpl authRegistry,
            AuthChannelRegistryImpl authChannelRegistry, PrismContext prismContext,
            String key, Object principal, List<GrantedAuthority> authorities) {
        super(key, principal, authorities);
        this.key = key;
        this.authRegistry = authRegistry;
        this.authChannelRegistry = authChannelRegistry;
        this.prismContext = prismContext;
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(
                    createAuthentication((HttpServletRequest) req));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Populated SecurityContextHolder with anonymous token: '"
                        + SecurityContextHolder.getContext().getAuthentication() + "'");
            }
        } else {
            processAuthentication(req);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SecurityContextHolder not populated with anonymous token, as it already contained: '"
                        + SecurityContextHolder.getContext().getAuthentication() + "'");
            }
        }

        chain.doFilter(req, res);
    }

    protected void processAuthentication(ServletRequest req) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null && moduleAuthentication.getAuthentication() == null) {
                Authentication authentication = createBasicAuthentication((HttpServletRequest) req);
                moduleAuthentication.setAuthentication(authentication);
                if (!mpAuthentication.hasSucceededAuthentication()) {
                    mpAuthentication.setPrincipal(authentication.getPrincipal());
                }
            }
        }
    }

    protected Authentication createAuthentication(HttpServletRequest request) {
        Authentication auth = createBasicAuthentication(request);

        MidpointAuthentication authentication = new MidpointAuthentication(SecurityPolicyUtil.createDefaultSequence());
        AuthenticationsPolicyType authenticationsPolicy;
        try {
            authenticationsPolicy = SecurityPolicyUtil.createDefaultAuthenticationPolicy(
                    NO_CUSTOM_IGNORED_LOCAL_PATH, prismContext.getSchemaRegistry());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get default authentication policy");
            throw new IllegalArgumentException("Couldn't get default authentication policy", e);
        }
        AuthenticationSequenceType sequence = SecurityPolicyUtil.createDefaultSequence();
        AuthenticationChannel authenticationChannel = AuthSequenceUtil.buildAuthChannel(authChannelRegistry, sequence);
        List<AuthModule> authModules = AuthSequenceUtil.buildModuleFilters(
                authRegistry, sequence, request, authenticationsPolicy.getModules(),
                null, new HashMap<>(), authenticationChannel);
        authentication.setAuthModules(authModules);
        if (authModules != null && !authModules.isEmpty()) {
            ModuleAuthenticationImpl module = (ModuleAuthenticationImpl) authModules.get(0).getBaseModuleAuthentication();
            module.setAuthentication(auth);
            authentication.addAuthentications(module);
        }
        authentication.setPrincipal(auth.getPrincipal());
        return authentication;
    }

    protected Authentication createBasicAuthentication(HttpServletRequest request) {
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(key,
                getPrincipal(), getAuthorities());
        auth.setDetails(authenticationDetailsSource.buildDetails(request));

        return auth;
    }

    public void setAuthenticationDetailsSource(
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        Assert.notNull(authenticationDetailsSource,
                "AuthenticationDetailsSource required");
        this.authenticationDetailsSource = authenticationDetailsSource;
    }
}
