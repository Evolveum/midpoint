/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

/**
 * Evaluator which checks credentials of identity and return authenticated data about authenticated identity.
 *
 * @author semancik
 */
public interface AuthenticationEvaluator<T extends AbstractAuthenticationContext> {

    /**
     * Checks credentials of identity and create token with {@link com.evolveum.midpoint.security.api.MidPointPrincipal}
     * of authenticated identity.
     *
     * @param connEnv Properties of connection environment
     * @param authnCtx Authentication context of type {@link AbstractAuthenticationContext}, which contains data needed
     * for authentication of identity.
     *
     * @return token with {@link com.evolveum.midpoint.security.api.MidPointPrincipal}
     * @throws BadCredentialsException when was set wrong authentication data
     * @throws AuthenticationCredentialsNotFoundException when object found by authentication identifier not contains credentials
     * @throws DisabledException when object found by authentication identifier is disabled
     * @throws LockedException when object found by authentication identifier is locked
     * @throws CredentialsExpiredException when object found by authentication identifier was expired credentials
     * @throws AuthenticationServiceException when occur some internal server error during authentication
     * @throws AccessDeniedException when object found by authentication identifier is unauthorized
     * @throws UsernameNotFoundException when object not found by authentication identifier
     */
    UsernamePasswordAuthenticationToken authenticate(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

    /**
     * Only one part of authentication - check credentials
     *
     * @param connEnv
     * @param authnCtx
     * @return focus identify by authentication context, after successfully checking
     */
    @NotNull
    FocusType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

    /**
     * create authentication token for identity, but without checking credentials only find identity, check authorization
     * and check required assignment
     *
     * @param connEnv
     * @param authnCtx
     * @return token with {@link com.evolveum.midpoint.security.api.MidPointPrincipal}
     */
    <AC extends AbstractAuthenticationContext> PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(ConnectionEnvironment connEnv, AC authnCtx);

}
