/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.SecurityQuestionsAuthenticationContext;
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
    private AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> questionAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<SecurityQuestionsAuthenticationContext> getEvaluator() {
        return questionAuthenticationEvaluator;
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token;
        if (authentication instanceof SecurityQuestionsAuthenticationToken) {
            Map<String, String> answers = (Map<String, String>) authentication.getCredentials();
            SecurityQuestionsAuthenticationContext authContext = new SecurityQuestionsAuthenticationContext(enteredUsername,
                    focusType, answers, requireAssignment, channel);

            token = getEvaluator().authenticate(connEnv, authContext);
        } else {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

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
