/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth.filter;

import com.evolveum.midpoint.web.boot.auth.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.boot.auth.module.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.boot.auth.util.StateOfModule;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author skublik
 */

public class PreLogoutFilter implements FilterChain {

    private static final String DEFAULT_LOGOUT_PATH = "(/[a-zA-Z0-9\\-_]+)*/logout$";

    private RequestMatcher matcher = new RegexRequestMatcher(DEFAULT_LOGOUT_PATH, "POST");
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//        if (!matcher.matches((HttpServletRequest) request)
//                || authentication == null
//                || !authentication.isAuthenticated()) {
//            return;
//        }

        if (!((HttpServletRequest) request).getServletPath().endsWith("/logout")) {
            return;
        }
        String path = ((HttpServletRequest) request).getServletPath();
        String prefix = path.substring(0, path.indexOf("logout"));

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
                if (prefix.equals(moduleAuthentication.getPrefix())
                        && StateOfModule.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                    moduleAuthentication.setState(StateOfModule.LOGOUT_PROCESSING);
                    break;
                }
            }
        } else {
            String message = "Unsuported type " + (authentication == null ? null : authentication.getClass().getName())
                    + " of authenticacion for MidpointLogoutRedirectFilter, supported is only MidpointAuthentication";
            throw new IllegalArgumentException(message);
        }

    }
}
