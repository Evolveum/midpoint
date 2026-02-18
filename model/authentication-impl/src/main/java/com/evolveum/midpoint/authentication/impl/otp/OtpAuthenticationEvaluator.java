/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.authentication.api.OtpServiceFactory;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.evaluator.CredentialsAuthenticationEvaluatorImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component
public class OtpAuthenticationEvaluator
        extends CredentialsAuthenticationEvaluatorImpl<OtpCredentialsType, OtpAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(OtpAuthenticationEvaluator.class);

    @Autowired private Protector protector;

    @Autowired private OtpServiceFactory otpServiceFactory;

    @Override
    protected void checkEnteredCredentials(ConnectionEnvironment env, OtpAuthenticationContext ctx) {
        if (ctx.getEnteredCredential() == null) {
            recordModuleAuthenticationFailure(ctx.getUsername(), null, env, null, "empty otp code provided");
            throw new BadCredentialsException(AuthUtil.generateBadCredentialsMessageKey(SecurityContextHolder.getContext().getAuthentication()));
        }
    }

    @Override
    protected boolean supportsAuthzCheck() {
        return true;
    }

    @Override
    protected OtpCredentialsType getCredential(CredentialsType credentials) {
        return credentials.getOtps();
    }

    @Override
    protected void validateCredentialNotNull(ConnectionEnvironment connEnv, @NotNull MidPointPrincipal principal, OtpCredentialsType credentials) {
        List<OtpCredentialType> otps = credentials.getOtp();

        if (otps == null || otps.isEmpty()) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "no otp stored for user");
            throw new AuthenticationCredentialsNotFoundException("web.security.provider.otp.bad");
        }
    }

    @Override
    protected boolean passwordMatches(
            ConnectionEnvironment env,
            @NotNull MidPointPrincipal principal,
            OtpCredentialsType otpCredentials,
            OtpAuthenticationContext ctx) {

        if (otpCredentials == null) {
            return false;
        }

        final Integer code = ctx.getEnteredCredential();
        if (code == null) {
            return false;
        }

        OtpService service = otpServiceFactory.create(ctx.getOtpAuthenticationModule());

        for (OtpCredentialType otp : otpCredentials.getOtp()) {
            if (!otp.isVerified()) {
                // don't check unverified OTPs, they are not active yet
                continue;
            }

            ProtectedStringType secret = otp.getSecret();
            if (secret == null) {
                continue;
            }

            try {
                ProtectedStringType cloned = secret.clone();
                protector.decrypt(cloned);

                String clearValue = cloned.getClearValue();
                if (clearValue == null) {
                    continue;
                }

                if (service.verifyCode(clearValue, code)) {
                    return true;
                }
            } catch (EncryptionException | SchemaException ex) {
                LOGGER.error("Error dealing with credentials of user \"{}\" credentials: {}", principal.getUsername(), ex.getMessage());
                recordModuleAuthenticationFailure(principal.getUsername(), principal, env, null, "error decrypting password: ");
                throw new AuthenticationServiceException("web.security.provider.unavailable", ex);
            }
        }

        return false;
    }

    @Override
    protected CredentialPolicyType getEffectiveCredentialPolicy(SecurityPolicyType securityPolicy, OtpAuthenticationContext ctx) {
        OtpCredentialsPolicyType policy = ctx.getPolicy();
        if (policy == null) {
            policy = SecurityUtil.getEffectiveOtpCredentialsPolicy(securityPolicy);
        }
        ctx.setPolicy(policy);

        return policy;
    }

    @Override
    protected boolean supportsActivation() {
        return true;
    }
}
