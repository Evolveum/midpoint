/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.OtpManager;
import com.evolveum.midpoint.authentication.api.OtpService;
import com.evolveum.midpoint.authentication.api.OtpServiceFactory;
import com.evolveum.midpoint.authentication.api.SecurityPolicyFinder;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component
public class OtpManagerImpl implements OtpManager {

    private static final Trace LOGGER = TraceManager.getTrace(OtpManagerImpl.class);

    private final OtpServiceFactory otpServiceFactory;

    private final Clock clock;

    private final Protector protector;

    private final SecurityPolicyFinder securityPolicyFinder;

    private final SystemObjectCache systemObjectCache;

    public OtpManagerImpl(
            OtpServiceFactory otpServiceFactory,
            Clock clock,
            Protector protector,
            SecurityPolicyFinder securityPolicyFinder,
            SystemObjectCache systemObjectCache) {

        this.otpServiceFactory = otpServiceFactory;
        this.clock = clock;
        this.protector = protector;
        this.securityPolicyFinder = securityPolicyFinder;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public <F extends FocusType> boolean isOtpAvailable(PrismObject<F> focus, Task task, OperationResult result) {
        return findOtpModuleConfigurationForFocus(focus, task, result) != null;
    }

    @Override
    public <F extends FocusType> OtpCredentialType createOtpCredential(PrismObject<F> focus, Task task, OperationResult result) {
        OtpService service = createOtpService(focus, task, result);

        String secretTxt = service.generateSecret();

        OtpCredentialType credential = new OtpCredentialType();
        credential.setSecret(ProtectedStringType.fromClearValue(secretTxt));
        credential.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());

        return credential;
    }

    @Override
    public <F extends FocusType> String createOtpAuthUrl(
            PrismObject<F> focus, OtpCredentialType credential, Task task, OperationResult result) {

        OtpAuthenticationModuleType module = findOtpModuleConfigurationForFocus(focus, task, result);
        if (module == null) {
            throw new IllegalArgumentException("OTP configuration is not available for currently logged in user");
        }

        try {
            ProtectedStringType secret = credential.getSecret();
            if (secret == null) {
                throw new IllegalArgumentException("No secret provided for OTP credential");
            }

            String secretTxt = protector.decryptString(secret);

            String account = evaluateAccountName(focus, module);

            OtpService service = createOtpService(module);
            return service.generateAuthUrl(account, secretTxt);
        } catch (EncryptionException ex) {
            throw new SystemException("Couldn't verify OTP credential", ex);
        }
    }

    @Override
    public <F extends FocusType> boolean verifyOtpCredential(PrismObject<F> focus, OtpCredentialType credential, int code, Task task, OperationResult result) {
        OtpAuthenticationModuleType module = findOtpModuleConfigurationForFocus(focus, task, result);
        if (module == null) {
            throw new IllegalArgumentException("OTP configuration is not available for currently logged in user");
        }

        ProtectedStringType secret = credential.getSecret();
        if (secret == null) {
            return false;
        }

        try {
            String secretTxt = protector.decryptString(secret);

            OtpService service = createOtpService(module);
            boolean correct = service.verifyCode(secretTxt, code);
            if (correct) {
                credential.setVerified(true);
            }

            return correct;
        } catch (EncryptionException ex) {
            throw new SystemException("Couldn't verify OTP credential", ex);
        }
    }

    private <F extends FocusType> String evaluateAccountName(PrismObject<F> focus, OtpAuthenticationModuleType module) {
        String defaultName = focus.getName().getOrig();

        ItemPathType pathType = module.getLabel();
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

    private OtpService createOtpService(OtpAuthenticationModuleType module) {
        if (module == null) {
            throw new IllegalStateException("No OTP authentication module found in security policy");
        }

        return otpServiceFactory.create(module);
    }

    private <F extends FocusType> OtpService createOtpService(PrismObject<F> focus, Task task, OperationResult result) {

        OtpAuthenticationModuleType otp = findOtpModuleConfigurationForFocus(focus, task, result);

        return createOtpService(otp);
    }

    private <F extends FocusType> OtpAuthenticationModuleType findOtpModuleConfigurationForFocus(
            PrismObject<F> focus, Task task, OperationResult result) {
        try {
            SecurityPolicyType securityPolicy;
            if (isCurrentPrincipal(focus)) {
                // Take security policy type from currently authenticated user
                securityPolicy = getCurrentPrincipalSecurityPolicy();
            } else {
                // We have to find applicable security policy for the focus.
                // This is needed for example when administrator is creating OTP credential for some user.
                PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
                securityPolicy = securityPolicyFinder.locateSecurityPolicyForFocus(focus, systemConfiguration, task, result);
            }

            return findOtpModuleInSecurityPolicy(securityPolicy);
        } catch (SchemaException ex) {
            throw new SystemException("Couldn't find OTP authentication module", ex);
        }
    }

    private boolean isCurrentPrincipal(PrismObject<? extends FocusType> focus) {
        FocusType principal = getCurrentUserFocus();
        return focus.getOid().equals(principal.getOid());
    }

    private SecurityPolicyType getCurrentPrincipalSecurityPolicy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof MidpointAuthentication ma)) {
            throw new IllegalStateException("Authentication in security context is not MidpointAuthentication");
        }

        if (!(ma.getPrincipal() instanceof MidPointPrincipal principal)) {
            throw new IllegalStateException("Principal in authentication is not MidPointPrincipal");
        }

        return principal.getApplicableSecurityPolicy();
    }

    /**
     * TODO this is not checking TOTP module usage in sequences, also not checking whether there's TOTP and
     *  other OTP (HOTP) module defined in the same policy
     */
    private OtpAuthenticationModuleType findOtpModuleInSecurityPolicy(SecurityPolicyType securityPolicy) {
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            return null;
        }

        AuthenticationsPolicyType authentication = securityPolicy.getAuthentication();
        AuthenticationModulesType modules = authentication.getModules();
        if (modules == null) {
            return null;
        }

        return modules.getTotp().get(0);
    }

    @NotNull
    private FocusType getCurrentUserFocus() {
        return getCurrentUserFocus(false);
    }

    private FocusType getCurrentUserFocus(boolean noException) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof MidpointAuthentication ma)) {
            if (noException) {
                return null;
            }
            throw new IllegalStateException("Authentication in security context is not MidpointAuthentication");
        }

        if (!(ma.getPrincipal() instanceof MidPointPrincipal principal)) {
            if (noException) {
                return null;
            }
            throw new IllegalStateException("Principal in authentication is not MidPointPrincipal");
        }

        PrismObject<? extends FocusType> focus = principal.getFocusPrismObject();
        return focus.asObjectable();
    }
}
