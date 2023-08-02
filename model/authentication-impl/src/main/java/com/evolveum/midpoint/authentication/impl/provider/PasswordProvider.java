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

import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
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

    @Autowired
    private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext> getEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    private String getEnteredUsername(Authentication authentication) {
        String enteredUsername = (String) authentication.getPrincipal();
        if (StringUtils.isNotBlank(enteredUsername)) {
            return enteredUsername;
        }
        LOGGER.trace("No username entered, trying current midPoint authentication if the identification was already done.");
        MidPointPrincipal principal = AuthUtil.getMidpointPrincipal();
        return principal.getUsername();
    }


    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        String enteredUsername = getEnteredUsername(authentication);
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel, authentication);

        try {
            Authentication token;
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                String enteredPassword = (String) authentication.getCredentials();

                PasswordAuthenticationContext authContext = new PasswordAuthenticationContext(enteredUsername, enteredPassword, focusType, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = getEvaluator().authenticate(connEnv, authContext);
            } else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
                token = getEvaluator().authenticateUserPreAuthenticated(connEnv, new PreAuthenticationContext(enteredUsername, focusType, requireAssignment));
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication failed for {}: {}", enteredUsername, e.getMessage());
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else if (actualAuthentication instanceof PreAuthenticatedAuthenticationToken) {
            return new PreAuthenticatedAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (UsernamePasswordAuthenticationToken.class.equals(authentication)) {
            return true;
        }
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
        return PasswordCredentialsPolicyType.class;
    }

}
