/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WicketLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public WicketLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        if (!WicketRedirectStrategy.isWicketAjaxRequest(request)) {
            super.commence(request, response, authException);

            return;
        }

        String url = buildRedirectUrlToLoginPage(request, response, authException);

        WicketRedirectStrategy strategy = new WicketRedirectStrategy();
        strategy.sendRedirect(request, response, url);
    }
}
