/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * Tests for the REQUISITE module abort behaviour.
 *
 * When a REQUISITE module has already failed the whole authentication flow
 * must be aborted on the next request, preventing subsequent modules
 * (e.g. an OTP step) from being reached.
 */
public class TestRequisiteModuleAbort extends AuthenticationModulesTest {

    /**
     * If the first (REQUISITE) module already failed, the
     * authentication should be aborted before the second module runs.
     */
    @Test
    public void testAbortWhenRequisiteModuleFailed() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUISITE,
                AuthenticationSequenceModuleNecessityType.REQUISITE);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(module(MODULE_PASSWORD, sequence, 0, AuthenticationModuleState.FAILURE));

        assertTrue(auth.authenticationShouldBeAborted(),
                "Authentication must be aborted when a REQUISITE module has failed");
    }

    /**
     * Sanity check: no abort when the REQUISITE module succeeded.
     */
    @Test
    public void testNoAbortWhenRequisiteModuleSucceeded() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUISITE,
                AuthenticationSequenceModuleNecessityType.REQUISITE);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(module(MODULE_PASSWORD, sequence, 0, AuthenticationModuleState.SUCCESSFULLY));

        assertFalse(auth.authenticationShouldBeAborted(),
                "Authentication must NOT be aborted when the REQUISITE module succeeded");
    }

    /**
     * A REQUIRED module failure must NOT trigger an abort — only REQUISITE does.
     */
    @Test
    public void testNoAbortWhenRequiredModuleFailed() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUIRED,
                AuthenticationSequenceModuleNecessityType.REQUIRED);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(module(MODULE_PASSWORD, sequence, 0, AuthenticationModuleState.FAILURE));

        assertFalse(auth.authenticationShouldBeAborted(),
                "Authentication must NOT be aborted when only a REQUIRED (not REQUISITE) module failed");
    }

    /**
     * If the second module (OTP) failed but the first (REQUISITE) succeeded,
     * there is nothing to abort based on the first module.
     */
    @Test
    public void testNoAbortWhenOnlySecondRequisiteModuleFailed() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUISITE,
                AuthenticationSequenceModuleNecessityType.REQUISITE);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(module(MODULE_PASSWORD, sequence, 0, AuthenticationModuleState.SUCCESSFULLY));
        auth.addAuthentication(module(MODULE_OTP, sequence, 1, AuthenticationModuleState.FAILURE));

        // The second REQUISITE module failed — the flow should also be aborted.
        assertTrue(auth.authenticationShouldBeAborted(),
                "Authentication must be aborted when any REQUISITE module has failed");
    }

    /**
     * OTP retry scenario: password succeeded, OTP failed (last module).
     * After resetLastFailedModuleForRetry() resets OTP back to LOGIN_PROCESSING,
     * authenticationShouldBeAborted must return false — the user should be allowed
     * to try the OTP code again without restarting the whole flow.
     *
     * This mirrors the ordering in MidpointAuthFilter: resetRetryableModuleIfNeeded
     * runs before validateAuthenticationCanContinue.
     */
    @Test
    public void testNoAbortAfterRetryResetOfLastOtpModule() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUISITE,
                AuthenticationSequenceModuleNecessityType.REQUISITE);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        auth.addAuthentication(module(MODULE_PASSWORD, sequence, 0, AuthenticationModuleState.SUCCESSFULLY));
        auth.addAuthentication(module(MODULE_OTP, sequence, 1, AuthenticationModuleState.FAILURE));
        auth.setAuthModules(List.of(new AuthModuleImpl<>(), new AuthModuleImpl<>()));

        auth.resetLastFailedModuleForRetry();

        assertFalse(auth.authenticationShouldBeAborted(),
                "Authentication must NOT be aborted after retry-reset of the last (OTP) module");
    }

    /**
     * No modules processed yet — nothing to abort.
     */
    @Test
    public void testNoAbortWithNoModulesProcessed() {
        AuthenticationSequenceType sequence = buildTwoModuleSequence(
                AuthenticationSequenceModuleNecessityType.REQUISITE,
                AuthenticationSequenceModuleNecessityType.REQUISITE);
        MidpointAuthentication auth = new MidpointAuthentication(sequence);

        assertFalse(auth.authenticationShouldBeAborted(),
                "Authentication must NOT be aborted when no modules have been processed yet");
    }

    private AuthenticationSequenceType buildTwoModuleSequence(
            AuthenticationSequenceModuleNecessityType passwordNecessity,
            AuthenticationSequenceModuleNecessityType otpNecessity) {

        AuthenticationSequenceModuleType passwordSeqModule = new AuthenticationSequenceModuleType();
        passwordSeqModule.setIdentifier(MODULE_PASSWORD);
        passwordSeqModule.setNecessity(passwordNecessity);

        AuthenticationSequenceModuleType otpSeqModule = new AuthenticationSequenceModuleType();
        otpSeqModule.setIdentifier(MODULE_OTP);
        otpSeqModule.setNecessity(otpNecessity);

        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.getModule().add(passwordSeqModule);
        sequence.getModule().add(otpSeqModule);
        return sequence;
    }

    private ModuleAuthenticationImpl module(
            String identifier,
            AuthenticationSequenceType sequence,
            int seqModuleIndex,
            AuthenticationModuleState state) {

        AuthenticationSequenceModuleType seqModule = sequence.getModule().get(seqModuleIndex);
        ModuleAuthenticationImpl module = new ModuleAuthenticationImpl(identifier, seqModule);
        module.setNameOfModule(identifier);
        module.setState(state);
        return module;
    }
}
