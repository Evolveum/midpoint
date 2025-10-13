/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.evaluator;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

/**
 * Evaluator which checks credentials of identity and return authenticated data about authenticated identity.
 *
 * @author semancik
 */
public interface AuthenticationEvaluator<T extends AbstractAuthenticationContext, A extends Authentication> {

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
    A authenticate(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

}
