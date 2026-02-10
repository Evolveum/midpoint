/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;

public interface OtpManager {

    /**
     * Check if OTP module is available in currently authenticated user. Can be used to determine if we
     * need to show OTP input form, but also to determine if we can create new OTP credentials for the user.
     */
    static boolean isOtpAvailable() {
        return getCurrentUserOtpAuthenticationModule() != null;
    }

    /**
     * Check if OTP module is required in current authentication process. Can be used to determine if we need to show
     * OTP input form.
     *
     * @return true if {@link AuthenticationSequenceModuleNecessityType} is
     * {@link AuthenticationSequenceModuleNecessityType#REQUIRED} or {@link AuthenticationSequenceModuleNecessityType#REQUISITE}.
     * False in case authentication is not defined or when OTP module is not present in sequence or necessity is
     * {@link AuthenticationSequenceModuleNecessityType#SUFFICIENT}, {@link AuthenticationSequenceModuleNecessityType#OPTIONAL}
     * or null.
     */
    static boolean isOtpRequired() {
        ModuleAuthentication ma = getCurrentUserOtpAuthenticationModule();
        if (ma == null) {
            return false;
        }

        AuthenticationSequenceModuleNecessityType necessity = ma.getNecessity();

        return AuthenticationSequenceModuleNecessityType.REQUIRED.equals(necessity)
                || AuthenticationSequenceModuleNecessityType.REQUISITE.equals(necessity);
    }

    static ModuleAuthentication getCurrentUserOtpAuthenticationModule() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof MidpointAuthentication ma)) {
            throw new IllegalStateException("Authentication in security context is not MidpointAuthentication");
        }

        List<? extends ModuleAuthentication> authentications = ma.getAuthModules().stream()
                .map(am -> am.getBaseModuleAuthentication())
                .filter(m -> AuthenticationModuleNameConstants.OTP.equals(m.getModuleTypeName()))
                .toList();

        if (authentications.isEmpty()) {
            return null;
        }

        if (authentications.size() > 1) {
            throw new IllegalStateException("Multiple OTP authentication modules found for the current user");
        }

        return authentications.get(0);
    }

    /**
     * Create new OTP credential for the currently logged-in user. The credential is not persisted,
     * it needs to be saved by the caller. The secret in the credential is not encrypted, it is
     * caller's responsibility to encrypt it before saving.
     */
    OtpCredentialType createOtpCredential();

    /**
     * Create OTP auth URL for the given credential. The URL can be used to generate QR code that can
     * be scanned by authenticator app.
     */
    String createOtpAuthUrl(OtpCredentialType credential);

    /**
     * Verify the provided OTP code against the secret in the credential.
     * If the code is correct, the credential is marked as verified.
     *
     * return true if the code is correct, false otherwise.
     */
    boolean verifyOtpCredential(OtpCredentialType credential, int code);
}
