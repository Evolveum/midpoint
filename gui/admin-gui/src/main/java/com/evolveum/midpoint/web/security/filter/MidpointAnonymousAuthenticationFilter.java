/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.ModuleAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * @author skublik
 */

public class MidpointAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAnonymousAuthenticationFilter.class);

    public MidpointAnonymousAuthenticationFilter(String key, Object principal, List<GrantedAuthority> authorities) {
        super(key, principal, authorities);
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
                    Authentication authentication = createAuthentication((HttpServletRequest) req);
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
}
