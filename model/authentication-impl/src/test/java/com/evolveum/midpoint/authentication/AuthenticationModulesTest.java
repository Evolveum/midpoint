package com.evolveum.midpoint.authentication;

import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * Base class for different aspects of authentication sequence modules configuration.
 *
 * Contains helper methods to build two module sequence using password and OTP.
 */
public abstract class AuthenticationModulesTest extends AbstractHigherUnitTest {

    protected static final String MODULE_PASSWORD = "password";

    protected static final String MODULE_OTP = "otp";

    protected AuthenticationSequenceType buildTwoModuleSequence(
            AuthenticationSequenceModuleNecessityType passwordNecessity,
            AuthenticationSequenceModuleNecessityType otpNecessity,
            Boolean otpAcceptEmpty) {

        AuthenticationSequenceModuleType passwordSeqModule = new AuthenticationSequenceModuleType();
        passwordSeqModule.setIdentifier(MODULE_PASSWORD);
        passwordSeqModule.setNecessity(passwordNecessity);

        AuthenticationSequenceModuleType otpSeqModule = new AuthenticationSequenceModuleType();
        otpSeqModule.setIdentifier(MODULE_OTP);
        otpSeqModule.setNecessity(otpNecessity);
        otpSeqModule.setAcceptEmpty(otpAcceptEmpty);

        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.getModule().add(passwordSeqModule);
        sequence.getModule().add(otpSeqModule);
        return sequence;
    }

    protected AuthenticationSequenceType buildTwoModuleSequence(Boolean otpAcceptEmpty) {
        return buildTwoModuleSequence(AuthenticationSequenceModuleNecessityType.REQUISITE, otpAcceptEmpty);
    }

    protected AuthenticationSequenceType buildTwoModuleSequence(
            AuthenticationSequenceModuleNecessityType necessity, Boolean otpAcceptEmpty) {

        return buildTwoModuleSequence(necessity, necessity, otpAcceptEmpty);
    }
}
