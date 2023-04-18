/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.SecurityQuestionsAuthenticationToken;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class HttpSecurityQuestionsAuthenticationFilter extends HttpAuthenticationFilter<JSONObject> {

    private static final Trace LOGGER = TraceManager.getTrace(HttpSecurityQuestionsAuthenticationFilter.class);

    public static final String J_ANSWER = "answer";
    public static final String J_USER = "user";

    public HttpSecurityQuestionsAuthenticationFilter(AuthenticationManager authenticationManager,
                                                     AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    protected JSONObject extractAndDecodeHeader(String header, HttpServletRequest request) {
        String token = createCredentialsFromHeader(header);
        JSONObject json = new JSONObject(token);
        if (!json.keySet().contains(J_USER) || !json.keySet().contains(J_ANSWER)){
            throw new AuthenticationServiceException("Authorization header doesn't contains attribute 'user' or 'answer'");
        }
        return json;
    }

    @Override
    protected UsernamePasswordAuthenticationToken createAuthenticationToken(JSONObject json, HttpServletRequest request) {
        Map<String, String> answers = AuthSequenceUtil.obtainAnswers(json.get(J_ANSWER).toString(),
                AuthConstants.SEC_QUESTION_J_QID, AuthConstants.SEC_QUESTION_J_QANS);
        return new SecurityQuestionsAuthenticationToken(getUsername(json), answers);
    }

    private String getUsername(JSONObject json) {
        return json.getString(J_USER);
    }

    @Override
    protected boolean authenticationIsRequired(JSONObject json, HttpServletRequest request) {
        return authenticationIsRequired(getUsername(json), SecurityQuestionsAuthenticationToken.class);
    }

    @Override
    protected void logFoundAuthorizationHeader(JSONObject json, HttpServletRequest request) {
        LOGGER.debug("Security Questions Authentication - Authorization header found for user '" + getUsername(json) + "'");
    }

    @Override
    protected @NotNull String getModuleIdentifier() {
        return AuthenticationModuleNameConstants.SECURITY_QUESTIONS;
    }

}
