/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.OtpManager;
import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.authentication.api.OtpServiceFactory;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component
public class OtpManagerImpl implements OtpManager {

    private final OtpServiceFactory otpServiceFactory;

    private final Clock clock;

    private final Protector protector;

    public OtpManagerImpl(OtpServiceFactory otpServiceFactory, Clock clock, Protector protector) {
        this.otpServiceFactory = otpServiceFactory;
        this.clock = clock;
        this.protector = protector;
    }

    @Override
    public OtpCredentialType createOtpCredential() {
        if (!OtpManager.isOtpAvailable()) {
            throw new IllegalArgumentException("OTP configuration is not available for currently logged in user");
        }

        OtpService service = createOtpService();

        String secretTxt = service.generateSecret();

        OtpCredentialType credential = new OtpCredentialType();
        credential.setSecret(ProtectedStringType.fromClearValue(secretTxt));
        credential.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());

        return credential;
    }

    @Override
    public String createOtpAuthUrl(OtpCredentialType credential) {
        if (!OtpManager.isOtpAvailable()) {
            throw new IllegalArgumentException("OTP configuration is not available for currently logged in user");
        }

        try {
            ProtectedStringType secret = credential.getSecret();
            if (secret == null) {
                throw new IllegalArgumentException("No secret provided for OTP credential");
            }

            String secretTxt = protector.decryptString(secret);

            String account = evaluateAccountName();

            OtpService service = createOtpService();
            return service.generateAuthUrl(account, secretTxt);
        } catch (EncryptionException ex) {
            throw new SystemException("Couldn't verify OTP credential", ex);
        }
    }

    @Override
    public boolean verifyOtpCredential(OtpCredentialType credential, int code) {
        if (!OtpManager.isOtpAvailable()) {
            throw new IllegalArgumentException("OTP configuration is not available for currently logged in user");
        }

        OtpService service = createOtpService();

        ProtectedStringType secret = credential.getSecret();
        if (secret == null) {
            return false;
        }

        try {
            String secretTxt = protector.decryptString(secret);

            boolean correct = service.verifyCode(secretTxt, code);
            if (correct) {
                credential.setVerified(true);
            }

            return correct;
        } catch (EncryptionException ex) {
            throw new SystemException("Couldn't verify OTP credential", ex);
        }
    }

    private String evaluateAccountName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof MidpointAuthentication ma)) {
            throw new IllegalStateException("Authentication in security context is not MidpointAuthentication");
        }

        if (!(ma.getPrincipal() instanceof MidPointPrincipal principal)) {
            throw new IllegalStateException("Principal in authentication is not MidPointPrincipal");
        }

        PrismObject<? extends FocusType> focus = principal.getFocusPrismObject();
        String defaultName = focus.getName().getOrig();

        OtpModuleAuthentication moduleAuthentication =
                (OtpModuleAuthentication) OtpManager.getCurrentUserOtpAuthenticationModule();

        ItemPathType pathType = moduleAuthentication.getModule().getLabel();
        ItemPath path = pathType != null ? pathType.getItemPath() : null;
        if (path == null) {
            return defaultName;
        }

        PrismProperty<?> property = focus.findProperty(path);
        if (property == null || property.isEmpty()) {
            return defaultName;
        }

        Object anyValue = property.getAnyRealValue();
        if (anyValue == null) {
            return defaultName;
        }

        return anyValue.toString();
    }

    /**
     * @return OTP service for the currently logged-in user if available, otherwise throws {@link IllegalStateException}.
     * The authentication module is determined from the security context.
     */
    private OtpService createOtpService() {
        ModuleAuthentication moduleAuthentication = OtpManager.getCurrentUserOtpAuthenticationModule();
        if (moduleAuthentication == null) {
            throw new IllegalStateException("No OTP authentication module found for the current user");
        }

        return otpServiceFactory.create(moduleAuthentication);
    }
}
