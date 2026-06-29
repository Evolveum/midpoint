/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.otp.OtpModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * Tests for TOTP module with acceptEmpty=true behaviour.
 *
 * Covers the scenario where a user without TOTP credentials logs in through
 * a sequence that includes a TOTP module configured with acceptEmpty=true.
 * In that case the TOTP module should be called off and the overall
 * authentication should still be considered successful.
 */
public class TestOtpAcceptEmptyAuthentication extends AuthenticationModulesTest {

    /**
     * Baseline: with the OTP module still in LOGIN_PROCESSING the overall
     * authentication must NOT yet be complete.
     */
    @Test
    public void testIsNotAuthenticatedWhileOtpLoginProcessing() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(true);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(passwordModule(sequence, AuthenticationModuleState.SUCCESSFULLY));
        auth.addAuthentication(otpModule(sequence, AuthenticationModuleState.LOGIN_PROCESSING));

        assertFalse(auth.isAuthenticated(),
                "Authentication must not be complete while OTP module is still in LOGIN_PROCESSING");
    }

    /**
     * Core requirement: once the OTP module is set to CALLED_OFF (because the
     * user has no TOTP credentials) and the module was configured with
     * acceptEmpty=true, the overall authentication IS complete.
     *
     * This is the state that the pre-flight applicability check in
     * MidpointAuthFilter must produce before redirecting to the dashboard.
     */
    @Test
    public void testIsAuthenticatedWhenOtpCalledOffWithAcceptEmpty() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(true);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(passwordModule(sequence, AuthenticationModuleState.SUCCESSFULLY));
        auth.addAuthentication(otpModule(sequence, AuthenticationModuleState.CALLED_OFF));

        assertTrue(auth.isAuthenticated(),
                "Authentication must be complete when OTP was called off and acceptEmpty=true");
    }

    /**
     * Safety check: if acceptEmpty=false (the default), CALLED_OFF must NOT
     * count as success – the authentication should fail.
     */
    @Test
    public void testIsNotAuthenticatedWhenOtpCalledOffWithoutAcceptEmpty() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(false);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(passwordModule(sequence, AuthenticationModuleState.SUCCESSFULLY));
        auth.addAuthentication(otpModule(sequence, AuthenticationModuleState.CALLED_OFF));

        assertFalse(auth.isAuthenticated(),
                "Authentication must NOT be complete when OTP was called off but acceptEmpty=false");
    }

    private ModuleAuthenticationImpl passwordModule(
            AuthenticationSequenceType sequence, AuthenticationModuleState state) {

        AuthenticationSequenceModuleType seqModule = sequence.getModule().get(0);
        ModuleAuthenticationImpl module = new ModuleAuthenticationImpl("password", seqModule);
        module.setNameOfModule(MODULE_PASSWORD);
        module.setState(state);
        return module;
    }

    private OtpModuleAuthentication otpModule(
            AuthenticationSequenceType sequence, AuthenticationModuleState state) {

        AuthenticationSequenceModuleType seqModule = sequence.getModule().get(1);
        OtpModuleAuthentication module = new OtpModuleAuthentication(seqModule);
        module.setNameOfModule(MODULE_OTP);
        module.setState(state);
        return module;
    }
}
