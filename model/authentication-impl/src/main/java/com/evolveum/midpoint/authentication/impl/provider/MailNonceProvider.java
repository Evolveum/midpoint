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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class MailNonceProvider extends AbstractCredentialProvider<NonceAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(MailNonceProvider.class);

    @Autowired
    private AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> nonceAuthenticationEvaluator;

    @Autowired
    private RepositoryService repositoryService;

    @Override
    protected AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> getEvaluator() {
        return nonceAuthenticationEvaluator;
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        String nonce = (String) authentication.getCredentials();

        NonceAuthenticationContext authContext = new NonceAuthenticationContext(enteredUsername,
                focusType, nonce, requireAssignment, channel);
        Authentication token = getEvaluator().authenticate(connEnv, authContext);

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;

    }

    @Override
    void postAuthenticationProcess() {
        @Nullable MidPointPrincipal principal = AuthUtil.getMidpointPrincipal();
        if (principal != null) {
            removeNonceAfterSuccessfulAuthentication(principal.getFocus());
        }
    }

    private void removeNonceAfterSuccessfulAuthentication(FocusType focus) {
        try {
            NonceType nonce = focus.getCredentials().getNonce();
            ObjectDelta<FocusType> deleteNonce = PrismContext.get().deltaFactory()
                    .object()
                    .createModificationDeleteContainer(FocusType.class, focus.getOid(),
                            ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_NONCE),
                            nonce.clone());

            repositoryService.modifyObject(
                    (Class)focus.getClass(),
                    focus.getOid(),
                    deleteNonce.getModifications(),
                    new OperationResult("Remove nonce from focus"));

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException e) {
            LOGGER.error("Couldn't remove nonce from focus {}", focus, e);
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new MailNonceAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MailNonceAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class getTypeOfCredential() {
        return NonceCredentialsPolicyType.class;
    }

}
