/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.impl.provider.AbstractCredentialProvider;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OtpAuthenticationProvider extends AbstractCredentialProvider<OtpAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(OtpAuthenticationProvider.class);

    private final OtpAuthenticationModuleType module;

    public OtpAuthenticationProvider(OtpAuthenticationModuleType module) {
        this.module = module;
    }

    @Override
    protected AuthenticationEvaluator<OtpAuthenticationContext, UsernamePasswordAuthenticationToken> getEvaluator() {
        return null;
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel,
            Class<? extends FocusType> focusType) {

        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment env = createEnvironment(channel);

        if (!(authentication instanceof OtpAuthenticationToken otpAuthentication)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        Integer code = otpAuthentication.getCredentials();
        OtpAuthenticationContext context =
                new OtpAuthenticationContext(enteredUsername, focusType, code, requireAssignment, channel);

        Authentication token = getEvaluator().authenticate(env, context);

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());

        return token;
    }

    @Override
    protected Authentication createNewAuthenticationToken(
            Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(
                    actualAuthentication.getPrincipal(),
                    actualAuthentication.getCredentials(),
                    newAuthorities);
        }

        return actualAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OtpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Class<? extends CredentialPolicyType> getTypeOfCredential() {
        return OtpCredentialsPolicyType.class;
    }
}
