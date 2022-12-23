/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.AttributeVerificationToken;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class AttributeVerificationAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String SPRING_SECURITY_FORM_ANSWER_KEY = "answer";
    private static final String SPRING_SECURITY_FORM_USER_KEY = "user";

    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        setUsernameParameter(SPRING_SECURITY_FORM_USER_KEY);
        String username = obtainUsername(request);
        if (username == null) {
            username = "";
        }

        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest =
                new AttributeVerificationToken(username, null);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected Map<String, String> obtainAnswers(HttpServletRequest request) {
        String answers = request.getParameter(SPRING_SECURITY_FORM_ANSWER_KEY);

        return AuthSequenceUtil.obtainAnswers(answers, AuthConstants.SEC_QUESTION_J_QID, AuthConstants.SEC_QUESTION_J_QANS);
    }
}
