/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.evaluator;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.api.util.AuthenticationEvaluatorUtil;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

@Component("preAuthenticatedEvaluator")
public class PreAuthenticatedEvaluatorImpl<C extends AbstractAuthenticationContext>
        extends AuthenticationEvaluatorImpl<C, PreAuthenticatedAuthenticationToken>
        implements AuthenticationEvaluator<C, PreAuthenticatedAuthenticationToken> {


    /**
     * create authentication token for identity, but without checking credentials only find identity, check authorization
     * and check required assignment
     *
     * @param connEnv
     * @param authnCtx
     * @return token with {@link com.evolveum.midpoint.security.api.MidPointPrincipal}
     * @throws DisabledException when object found by authentication identifier is disabled
     * @throws AuthenticationServiceException when occur some internal server error during authentication
     * @throws UsernameNotFoundException when object not found by authentication identifier
     */

//    authenticate(ConnectionEnvironment connEnv, T authnCtx)
//            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
//            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException;
    @Override
    public PreAuthenticatedAuthenticationToken authenticate(
            ConnectionEnvironment connEnv, C authnCtx)
            throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException,
            CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {

        MidPointPrincipal principal = getAndCheckPrincipal(connEnv, authnCtx, authnCtx.isSupportActivationByChannel());

        // Authorizations
        if (hasNoAuthorizations(principal)) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "no authorizations");
            throw new DisabledException("web.security.provider.access.denied");
        }

        if (AuthenticationEvaluatorUtil.checkRequiredAssignmentTargets(principal.getFocus(), authnCtx.getRequireAssignments())) {
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities());
            recordModuleAuthenticationSuccess(principal, connEnv, true);
            return token;
        } else {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "not contains required assignment");
            throw new DisabledException("web.security.flexAuth.invalid.required.assignment");
        }
    }

//    @Override
//    public @NotNull FocusType checkCredentials(ConnectionEnvironment connEnv, C authnCtx) throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException, CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {
//        return null;
//    }

}
