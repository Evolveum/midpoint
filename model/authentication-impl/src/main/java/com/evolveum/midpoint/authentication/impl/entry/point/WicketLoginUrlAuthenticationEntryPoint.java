/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.entry.point;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.impl.WicketRedirectStrategy;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

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
