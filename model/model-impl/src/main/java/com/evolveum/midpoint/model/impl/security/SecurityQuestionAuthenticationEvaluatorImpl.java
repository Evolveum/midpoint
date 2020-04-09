/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component("securityQuestionsAuthenticationEvaluator")
public class SecurityQuestionAuthenticationEvaluatorImpl
        extends AuthenticationEvaluatorImpl<SecurityQuestionsCredentialsType, SecurityQuestionsAuthenticationContext> {

    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment connEnv,
            SecurityQuestionsAuthenticationContext authCtx) {
        if (MapUtils.isEmpty(authCtx.getQuestionAnswerMap())) {
            recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
            throw new BadCredentialsException("web.security.provider.password.encoding");
        }

        Map<String, String> enteredQuestionAnswer = authCtx.getQuestionAnswerMap();
        boolean allBlank = false;
        for (String enteredAnswers : enteredQuestionAnswer.values()) {
            if (StringUtils.isBlank(enteredAnswers)) {
                allBlank = true;
            }
        }

        if (allBlank) {
            recordAuthenticationFailure(authCtx.getUsername(), connEnv, "empty password provided");
            throw new BadCredentialsException("web.security.provider.password.encoding");
        }
    }

    @Override
    protected boolean supportsAuthzCheck() {
        return true;
    }

    @Override
    protected SecurityQuestionsCredentialsType getCredential(CredentialsType credentials) {
        return credentials.getSecurityQuestions();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv,
            @NotNull MidPointPrincipal principal, SecurityQuestionsCredentialsType credential) {
        List<SecurityQuestionAnswerType> securityQuestionsAnswers = credential.getQuestionAnswer();

        if (securityQuestionsAnswers == null || securityQuestionsAnswers.isEmpty()) {
            recordAuthenticationFailure(principal, connEnv, "no stored security questions");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.password.bad");
        }

    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal,
            SecurityQuestionsCredentialsType passwordType, SecurityQuestionsAuthenticationContext authCtx) {

        SecurityQuestionsCredentialsPolicyType policy = authCtx.getPolicy();
        Integer iNumberOfQuestions = policy.getQuestionNumber();
        int numberOfQuestions = iNumberOfQuestions != null ? iNumberOfQuestions : 0;

        Map<String, String> enteredQuestionsAnswers = authCtx.getQuestionAnswerMap();
        if (numberOfQuestions > enteredQuestionsAnswers.size()) {
            return false;
        }

        List<SecurityQuestionAnswerType> questionsAnswers = passwordType.getQuestionAnswer();
        int matched = 0;
        for (SecurityQuestionAnswerType questionAnswer : questionsAnswers) {
            String enteredAnswer = enteredQuestionsAnswers.get(questionAnswer.getQuestionIdentifier());
            if (StringUtils.isNotBlank(enteredAnswer)) {
                if (decryptAndMatch(connEnv, principal, questionAnswer.getQuestionAnswer(), enteredAnswer)) {
                    matched++;
                }
            }
        }

        return matched > 0 && matched >= numberOfQuestions;

    }

    @Override
    protected CredentialPolicyType getEffectiveCredentialPolicy(
            SecurityPolicyType securityPolicy, SecurityQuestionsAuthenticationContext authnCtx) {
        SecurityQuestionsCredentialsPolicyType policy = authnCtx.getPolicy();
        if (policy == null) {
            policy = SecurityUtil.getEffectiveSecurityQuestionsCredentialsPolicy(securityPolicy);
        }
        authnCtx.setPolicy(policy);
        return policy;
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }

}
