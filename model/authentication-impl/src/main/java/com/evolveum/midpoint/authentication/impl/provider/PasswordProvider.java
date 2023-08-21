/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.authentication.api.evaluator.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;

/**
 * @author skublik
 */

public class PasswordProvider extends AbstractCredentialProvider<PasswordAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(PasswordProvider.class);

    @Autowired private AuthenticationEvaluator<PasswordAuthenticationContext, UsernamePasswordAuthenticationToken> passwordAuthenticationEvaluator;


    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext, UsernamePasswordAuthenticationToken> getEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication, String enteredUsername, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        String enteredPassword = (String) authentication.getCredentials();

        PasswordAuthenticationContext authContext = new PasswordAuthenticationContext(
                enteredUsername,
                enteredPassword,
                focusType,
                requireAssignment,
                channel);
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
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
        return PasswordCredentialsPolicyType.class;
    }

}
