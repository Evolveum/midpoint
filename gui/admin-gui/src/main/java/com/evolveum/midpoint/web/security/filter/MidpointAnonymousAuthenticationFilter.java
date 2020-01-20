/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author skublik
 */

public class MidpointAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAnonymousAuthenticationFilter.class);

    private AuthModuleRegistryImpl authRegistry;

    private AuthChannelRegistryImpl authChannelRegistry;

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private String key;

    public MidpointAnonymousAuthenticationFilter(AuthModuleRegistryImpl authRegistry, AuthChannelRegistryImpl authChannelRegistry,
                                                 String key, Object principal, List<GrantedAuthority> authorities) {
        super(key, principal, authorities);
        this.key = key;
        this.authRegistry = authRegistry;
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
        }
        else {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                if (moduleAuthentication != null && moduleAuthentication.getAuthentication() == null) {
                    Authentication authentication = createBasicAuthentication((HttpServletRequest) req);
                    moduleAuthentication.setAuthentication(authentication);
                    mpAuthentication.setPrincipal(authentication.getPrincipal());
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SecurityContextHolder not populated with anonymous token, as it already contained: '"
                        + SecurityContextHolder.getContext().getAuthentication() + "'");
            }
        }

        chain.doFilter(req, res);
    }

    protected Authentication createAuthentication(HttpServletRequest request) {
        Authentication auth = createBasicAuthentication(request);

        MidpointAuthentication authentication = new MidpointAuthentication(SecurityPolicyUtil.createDefaultSequence());
        AuthenticationsPolicyType authenticationsPolicy = SecurityPolicyUtil.createDefaultAuthenticationPolicy();
        AuthenticationSequenceType sequence = SecurityPolicyUtil.createDefaultSequence();
        AuthenticationChannel authenticationChannel = SecurityUtils.buildAuthChannel(authChannelRegistry, sequence);
        List<AuthModule> authModules = SecurityUtils.buildModuleFilters(authRegistry, sequence, request, authenticationsPolicy.getModules(),
                null, new HashMap<Class<? extends Object>, Object>(), authenticationChannel);
        authentication.setAuthModules(authModules);
        ModuleAuthentication module = authModules.get(0).getBaseModuleAuthentication();
        module.setAuthentication(auth);
        authentication.addAuthentications(module);
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
