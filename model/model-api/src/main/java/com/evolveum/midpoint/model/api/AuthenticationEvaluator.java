/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public interface AuthenticationEvaluator<T extends AbstractAuthenticationContext> {

    UsernamePasswordAuthenticationToken authenticate(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

    @NotNull
    FocusType checkCredentials(ConnectionEnvironment connEnv, T authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;

    PreAuthenticatedAuthenticationToken authenticateUserPreAuthenticated(
            ConnectionEnvironment connEnv, AbstractAuthenticationContext authnCtx)
            throws DisabledException, AuthenticationServiceException, UsernameNotFoundException;

}
