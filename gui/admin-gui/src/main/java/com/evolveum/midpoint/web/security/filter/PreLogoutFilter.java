/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.security.saml.util.StringUtils.stripEndingSlases;

/**
 * @author skublik
 */

public class PreLogoutFilter implements FilterChain {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        ModuleAuthentication moduleAuthentication = null;
        if (authentication != null && authentication.isAuthenticated()) {
            moduleAuthentication = SecurityUtils.getAuthenticatedModule();
        }
        if (authentication == null || ((moduleAuthentication == null || !moduleAuthentication.isInternalLogout())
                && !((HttpServletRequest) request).getServletPath().endsWith("/logout"))) {
            return;
        }
        if (moduleAuthentication != null) {
            moduleAuthentication.setState(StateOfModule.LOGOUT_PROCESSING);
        }
    }
}
