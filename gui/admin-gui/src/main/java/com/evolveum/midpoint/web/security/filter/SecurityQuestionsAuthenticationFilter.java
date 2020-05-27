/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.evolveum.midpoint.web.security.module.authentication.SecurityQuestionsAuthenticationToken;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * @author skublik
 */
public class SecurityQuestionsAuthenticationFilter
        extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String SPRING_SECURITY_FORM_ANSWER_KEY = "answer";
    private static final String SPRING_SECURITY_FORM_USER_KEY = "user";

    public static final String J_QID = "qid";
    public static final String J_QANS = "qans";
    public static final String J_QTXT = "qtxt";

    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        setUsernameParameter(SPRING_SECURITY_FORM_USER_KEY);
        String username = obtainUsername(request);
        Map<String, String> answers = obtainAnswers(request);

        if (username == null) {
            username = "";
        }

        if (answers == null) {
            answers = new HashMap<>();
        }

        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest =
                new SecurityQuestionsAuthenticationToken(username, answers);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected Map<String, String> obtainAnswers(HttpServletRequest request) {
        String answers = request.getParameter(SPRING_SECURITY_FORM_ANSWER_KEY);

        return SecurityUtils.obtainAnswers(answers, J_QID, J_QANS);
    }
}
