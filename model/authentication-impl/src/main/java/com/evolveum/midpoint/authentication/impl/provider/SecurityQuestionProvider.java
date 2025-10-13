/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.SecurityQuestionsAuthenticationToken;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;

/**
 * @author skublik
 */

public class SecurityQuestionProvider extends AbstractCredentialProvider<SecurityQuestionsAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityQuestionProvider.class);

    @Autowired
    private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext, UsernamePasswordAuthenticationToken> questionAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<SecurityQuestionsAuthenticationContext, UsernamePasswordAuthenticationToken> getEvaluator() {
        return questionAuthenticationEvaluator;
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        if (!(authentication instanceof SecurityQuestionsAuthenticationToken securityQuestionsAuthenticationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        Map<String, String> answers = securityQuestionsAuthenticationToken.getCredentials();
        SecurityQuestionsAuthenticationContext authContext = new SecurityQuestionsAuthenticationContext(enteredUsername,
                focusType, answers, requireAssignment, channel);

        Authentication token = getEvaluator().authenticate(connEnv, authContext);

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;

    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SecurityQuestionsAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
        return SecurityQuestionsCredentialsPolicyType.class;
    }

}
