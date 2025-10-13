/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.SecurityQuestionsAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.util.StringUtil;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author skublik
 */
public class SecurityQuestionsAuthenticationFilter
        extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String SPRING_SECURITY_FORM_ANSWER_KEY = "answer";
    private static final String SPRING_SECURITY_FORM_USER_KEY = "user";

    private String getIdentifiedUsername() {
        MidpointAuthentication midpointAuthentication = AuthUtil.getMidpointAuthentication();
        Object principal = midpointAuthentication.getPrincipal();
        if (!(principal instanceof MidPointPrincipal)) {
            return "";
        }

        FocusType focus = ((MidPointPrincipal) principal).getFocus();
        return focus.getName().getNorm();

    }
    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        setUsernameParameter(SPRING_SECURITY_FORM_USER_KEY);
        String username = getIdentifiedUsername();
        if (StringUtils.isBlank(username)) {
            username = obtainUsername(request);
        }
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

        return AuthSequenceUtil.obtainAnswers(answers, AuthConstants.SEC_QUESTION_J_QID, AuthConstants.SEC_QUESTION_J_QANS);
    }
}
