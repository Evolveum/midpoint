/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.evaluator.AttributeVerificationEvaluatorImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.AttributeVerificationToken;
import com.evolveum.midpoint.authentication.api.evaluator.context.AttributeVerificationAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AttributeVerificationProvider extends AbstractCredentialProvider<AttributeVerificationAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeVerificationProvider.class);

    @Autowired public AttributeVerificationEvaluatorImpl authenticationEvaluator;

    @Override
    protected AttributeVerificationEvaluatorImpl getEvaluator() {
        return authenticationEvaluator;
    }


    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);
        if (enteredUsername == null) {
            LOGGER.error("No username provided in the authentication token");
            return authentication;  //TODO should not be exception?
        }

        ConnectionEnvironment connEnv = createEnvironment(channel);

        if (!(authentication instanceof AttributeVerificationToken attributeVerificationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        Map<ItemPath, String> attrValuesMap = attributeVerificationToken.getCredentials();
        AttributeVerificationAuthenticationContext authContext = new AttributeVerificationAuthenticationContext(enteredUsername,
                focusType, attrValuesMap, requireAssignment, channel);
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
        return AttributeVerificationToken.class.equals(authentication);
    }

    @Override
    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
        return AttributeVerificationCredentialsPolicyType.class;
    }
}
