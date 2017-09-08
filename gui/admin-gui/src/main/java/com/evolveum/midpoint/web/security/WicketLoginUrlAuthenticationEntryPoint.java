/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import org.apache.wicket.request.http.WebRequest;
import org.springframework.security.core.AuthenticationException;
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

        if (!isWicketAjaxRequest(request)) {
            super.commence(request, response, authException);

            return;
        }

        String url = buildRedirectUrlToLoginPage(request, response, authException);

        WicketRedirectStrategy strategy = new WicketRedirectStrategy();
        strategy.sendRedirect(request, response, url);
    }

    private boolean isWicketAjaxRequest(HttpServletRequest request) {
        String value = request.getParameter(WebRequest.PARAM_AJAX);
        if (value != null && "true".equals(value)) {
            return true;
        }

        value = request.getHeader(WebRequest.HEADER_AJAX);
        if (value != null && "true".equals(value)) {
            return true;
        }

        return false;
    }
}
