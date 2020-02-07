/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.SecurityQuestionsAuthenticationToken;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.github.openjson.JSONObject;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author skublik
 */

public class HttpSecurityQuestionsAuthenticationFilter extends HttpAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpSecurityQuestionsAuthenticationFilter.class);

    public static final String J_ANSWER = "answer";
    public static final String J_USER = "user";

    public HttpSecurityQuestionsAuthenticationFilter(AuthenticationManager authenticationManager,
                                                     AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            String header = request.getHeader("Authorization");

            int delim = header.indexOf(" ");

            if (delim == -1) {
                throw new BadCredentialsException("Invalid authentication header, value of header don't contains delimiter ' '."
                        + " Please use form 'Authorization: <type> <credentials>' for successful authentication");
            }

            if (header == null || !header.toLowerCase().startsWith(NameOfModuleType.SECURITY_QUESTIONS.getName().toLowerCase() + " ")) {
                chain.doFilter(request, response);
                return;
            }

            JSONObject tokens = extractAndDecodeHeader(header, request);
            if (!tokens.keySet().contains(J_USER) || !tokens.keySet().contains(J_ANSWER)){
                throw new AuthenticationServiceException("Authorization header doesn't contains attribute 'user' or 'answer'");
            }

            String username = tokens.getString(J_USER);

            LOGGER.debug("Security Questions - Authentication Authorization header found for user '" + username + "'");

            if (authenticationIsRequired(username, SecurityQuestionsAuthenticationToken.class)) {
                Map<String, String> answers = SecurityUtils.obtainAnswers(tokens.get(J_ANSWER).toString(),
                        SecurityQuestionsAuthenticationFilter.J_QID, SecurityQuestionsAuthenticationFilter.J_QANS);
                SecurityQuestionsAuthenticationToken authRequest = new SecurityQuestionsAuthenticationToken(
                        username, answers);
                authRequest.setDetails(
                        getAuthenticationDetailsSource().buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                SecurityUtils.resolveProxyUserOidHeader(request);

                onSuccessfulAuthentication(request, response, authResult);

                LOGGER.debug("Authentication success: " + authResult);

                getRememberMeServices().loginSuccess(request, response, authResult);

            }

        }
        catch (AuthenticationException failed) {
            LOGGER.debug("Authentication request for failed: " + failed);

            getRememberMeServices().loginFail(request, response);

            this.getAuthenticationEntryPoint().commence(request, response, failed);

            return;
        }

        chain.doFilter(request, response);
    }


    private JSONObject extractAndDecodeHeader(String header, HttpServletRequest request)
            throws IOException {

        int startIndex = NameOfModuleType.SECURITY_QUESTIONS.getName().length() + 1;
        byte[] base64Token = header.substring(startIndex).getBytes(StandardCharsets.UTF_8);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode security question authentication token");
        }

        String token = new String(decoded, getCredentialsCharset(request));

        return new JSONObject(token);
    }

}
