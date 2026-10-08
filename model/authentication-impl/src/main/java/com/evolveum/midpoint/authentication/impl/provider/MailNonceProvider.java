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
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.AuthenticationSequenceListener;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;

import org.jetbrains.annotations.NotNull;
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
 * Authenticates by the nonce from the mail link. The nonce is a one-time credential of the whole authentication
 * sequence, so it is spent when the sequence succeeds, not when this module succeeds: a failure of a module that
 * follows the mail nonce (e.g. wrong password in account activation) keeps the link usable. Issue 5490.
 *
 * @author skublik
 */
public class MailNonceProvider extends AbstractCredentialProvider<NonceAuthenticationContext>
        implements AuthenticationSequenceListener {

    private static final Trace LOGGER = TraceManager.getTrace(MailNonceProvider.class);

    private static final String OPERATION_REMOVE_SPENT_NONCE = MailNonceProvider.class.getName() + ".removeSpentNonce";

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
    public void sequenceSucceeded(
            @NotNull MidpointAuthentication mpAuthentication, @Nullable ModuleAuthentication moduleAuthentication) {
        if (moduleAuthentication == null || moduleAuthentication.getState() != AuthenticationModuleState.SUCCESSFULLY) {
            return;
        }
        if (mpAuthentication.getPrincipal() instanceof MidPointPrincipal principal) {
            removeSpentNonce(principal);
        }
    }

    /**
     * The nonce is removed whatever its current value is, the in-memory principal may hold a stale copy (#12082).
     */
    private void removeSpentNonce(MidPointPrincipal principal) {
        OperationResult result = new OperationResult(OPERATION_REMOVE_SPENT_NONCE);
        try {
            repositoryService.modifyObject(FocusType.class, principal.getOid(),
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(SchemaConstants.PATH_NONCE).replace()
                            .asItemDeltas(),
                    result);
            LOGGER.debug("Removed spent nonce of user '{}'", principal.getUsername());
        } catch (CommonException e) {
            LOGGER.error("Couldn't remove spent nonce of user '{}': {}", principal.getUsername(), e.getMessage(), e);
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
