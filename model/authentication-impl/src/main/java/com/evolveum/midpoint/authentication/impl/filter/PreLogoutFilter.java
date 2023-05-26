/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author skublik
 */
public class PreLogoutFilter implements FilterChain {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        ModuleAuthenticationImpl moduleAuthentication = null;
        if (authentication != null && authentication.isAuthenticated()) {
            moduleAuthentication = (ModuleAuthenticationImpl) AuthUtil.getAuthenticatedModule();
        }
        if (authentication == null || ((moduleAuthentication == null || !moduleAuthentication.isInternalLogout())
                && !((HttpServletRequest) request).getServletPath().endsWith("/logout"))) {
            return;
        }
        if (moduleAuthentication != null) {
            moduleAuthentication.setState(AuthenticationModuleState.LOGOUT_PROCESSING);
        }
    }
}
