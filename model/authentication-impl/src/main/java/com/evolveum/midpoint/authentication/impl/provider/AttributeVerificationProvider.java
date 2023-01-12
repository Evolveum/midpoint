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
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.AttributeVerificationAuthenticationContext;
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
import org.springframework.security.core.AuthenticationException;
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
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        if (!(authentication.getPrincipal() instanceof MidPointPrincipal)) {
            return authentication;
        }
        String enteredUsername = ((MidPointPrincipal) authentication.getPrincipal()).getUsername();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        try {
            Authentication token;
            if (authentication instanceof AttributeVerificationToken) {
                Map<ItemPath, String> attrValuesMap = (Map<ItemPath, String>) authentication.getCredentials();
                AttributeVerificationAuthenticationContext authContext = new AttributeVerificationAuthenticationContext(enteredUsername,
                        focusType, attrValuesMap, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = getEvaluator().authenticate(connEnv, authContext);
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;
        } catch (AuthenticationException e) {
            LOGGER.info("Authentication failed for {}: {}", enteredUsername, e.getMessage());
            throw e;
        }
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
        return null; //todo
    }

}
