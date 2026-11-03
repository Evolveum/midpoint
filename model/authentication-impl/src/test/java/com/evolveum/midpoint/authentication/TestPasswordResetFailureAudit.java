/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.authentication.impl.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.authentication.impl.channel.ResetPasswordAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.SequenceCompletionFilter;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.authentication.AttributeVerificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.LoginFormModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.AttributeVerificationToken;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.provider.AttributeVerificationProvider;
import com.evolveum.midpoint.authentication.impl.provider.FocusIdentificationProvider;
import com.evolveum.midpoint.authentication.impl.provider.MailNonceProvider;
import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginFormAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailNonceAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Verifies auditing of failed password-reset authentication attempts, including
 * invalid, expired, and reused nonces, attribute verification failures, and
 * unsuccessful focus identification.
 *
 * Uses real authentication providers and a simulated failure-handler/filter
 * chain to verify that each rejected submission produces exactly one audit event,
 * without exposing sensitive values or changing existing authentication behavior.
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
public class TestPasswordResetFailureAudit extends AbstractModelImplementationIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    private static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    private static final String RESET_SEQUENCE = "password-reset";
    private static final String NORMAL_LOGIN_SEQUENCE = "normal-login";
    private static final String MODULE_NONCE = "mailNonce";
    private static final String MODULE_ATTRIBUTE = "employeeNumberVerification";
    private static final String MODULE_FOCUS = "focusIdentification";
    private static final String MODULE_LOGIN = "loginForm";

    private static final String NONCE = "valid-reset-nonce";
    private static final String BAD_NONCE = "submitted-bad-nonce-secret";
    private static final String EMPTY_NONCE_REASON = "empty nonce provided";
    private static final String PASSWORD = "ResetAudit-123";
    private static final String BAD_PASSWORD = "submitted-bad-password-secret";
    private static final String WRONG_VERIFICATION = "submitted-wrong-verification-secret";
    private static final String UNTRUSTED_IDENTIFICATION = "administrator";
    private static final String REMOTE_ADDRESS = "192.0.2.15";
    private static final String OTHER_SEQUENCE = "other-password-reset-sequence";

    private static final String USER_NONCE_OID = "5600d4f0-5ca8-4f84-b2b7-100000000001";
    private static final String USER_NONCE_NAME = "reset-audit-nonce";
    private static final String USER_REPEAT_OID = "5600d4f0-5ca8-4f84-b2b7-100000000002";
    private static final String USER_REPEAT_NAME = "reset-audit-repeat";
    private static final String USER_EMPTY_OID = "5600d4f0-5ca8-4f84-b2b7-100000000003";
    private static final String USER_EMPTY_NAME = "reset-audit-empty";
    private static final String USER_ATTRIBUTE_OID = "5600d4f0-5ca8-4f84-b2b7-100000000004";
    private static final String USER_ATTRIBUTE_NAME = "reset-audit-attribute";
    private static final String USER_SUCCESS_OID = "5600d4f0-5ca8-4f84-b2b7-100000000005";
    private static final String USER_SUCCESS_NAME = "reset-audit-success";
    private static final String USER_LOGIN_OID = "5600d4f0-5ca8-4f84-b2b7-100000000006";
    private static final String USER_LOGIN_NAME = "reset-audit-login";

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    @AfterMethod
    public void clearAuthenticationAfterTest() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        modelService.postInit(initResult);
        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        PrismObject<UserType> administrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(administrator);

        addTestUser(USER_NONCE_OID, USER_NONCE_NAME, initTask, initResult);
        addTestUser(USER_REPEAT_OID, USER_REPEAT_NAME, initTask, initResult);
        addTestUser(USER_EMPTY_OID, USER_EMPTY_NAME, initTask, initResult);
        addTestUser(USER_ATTRIBUTE_OID, USER_ATTRIBUTE_NAME, initTask, initResult);
        addTestUser(USER_SUCCESS_OID, USER_SUCCESS_NAME, initTask, initResult);
        addTestUser(USER_LOGIN_OID, USER_LOGIN_NAME, initTask, initResult);
    }

    private void addTestUser(String oid, String name, Task task, OperationResult result) throws Exception {
        UserType user = new UserType()
                .oid(oid)
                .name(name)
                .employeeNumber("EMP-001")
                .credentials(new CredentialsType()
                        .password(new PasswordType().value(protectedString(PASSWORD)))
                        .nonce(new NonceType()
                                .value(protectedString(NONCE))
                                .sequenceIdentifier(RESET_SEQUENCE)));
        addObject(user.asPrismObject(), ModelExecuteOptions.create().raw(), task, result);
    }

    @Test
    public void test100FailedNonceSubmissionProducesOneResetAuditEvent() throws Exception {
        given("a reset sequence waiting for a mail nonce, with another module still to follow");
        setupNonce(USER_NONCE_OID);
        MidpointAuthentication authentication = createNonceAuthentication(true);

        when("an incorrect nonce is submitted");
        submitBadNonce(authentication, USER_NONCE_NAME, BAD_NONCE);

        then("exactly one failed reset-channel session event is recorded");
        AuditEventRecord record = assertSingleFailure(USER_NONCE_NAME, USER_NONCE_OID, "password mismatch");
        assertDoesNotContain(record, BAD_NONCE);

        and("the valid nonce is still present");
        assertUserNonce(USER_NONCE_OID, NONCE);
    }

    @Test
    public void test110RepeatedIncorrectNonceSubmissionsProduceOneEventEach() throws Exception {
        given("a valid nonce in a reset sequence after successful focus identification");
        setupNonce(USER_REPEAT_OID);
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_REPEAT_NAME, UserType.class, ProfileCompilerOptions.create());
        MidpointAuthentication authentication = createRetryableNonceAuthentication(principal);

        when("the nonce is submitted incorrectly twice");
        for (int i = 0; i < 2; i++) {
            int recordsBeforeAttempt = createSessionRecords().size();
            submitBadNonce(authentication, 1, USER_REPEAT_NAME, BAD_NONCE + i);
            assertThat(createSessionRecords()).hasSize(recordsBeforeAttempt + 1);
            if (i < 1) {
                restartAuthenticationForNonceRetry(authentication, principal);
            }
        }

        then("there is one distinct failed reset audit event per submission");
        List<AuditEventRecord> records = createSessionRecords();
        assertThat(records).hasSize(2);
        assertThat(records)
                .allSatisfy(record -> assertFailure(record, USER_REPEAT_NAME, USER_REPEAT_OID, "password mismatch"));
        assertThat(records.stream().map(AuditEventRecord::getEventIdentifier)).doesNotHaveDuplicates();
        assertThat(records).allSatisfy(record -> assertDoesNotContain(record, BAD_NONCE));

        and("the valid nonce remains available");
        assertUserNonce(USER_REPEAT_OID, NONCE);
    }

    @Test
    public void test120EmptyNonceSubmissionIsAuditedOnce() throws Exception {
        given("a reset sequence waiting for a mail nonce");
        setupNonce(USER_EMPTY_OID);
        MidpointAuthentication authentication = createNonceAuthentication(true);

        when("an empty nonce is submitted");
        submitBadNonce(authentication, USER_EMPTY_NAME, "");

        then("the deferred empty-credential failure is audited exactly once");
        assertSingleFailure(USER_EMPTY_NAME, USER_EMPTY_OID, EMPTY_NONCE_REASON);
    }

    @Test
    public void test130IncorrectAttributeVerificationBeforeSequenceCompletionIsAudited() throws Exception {
        given("an identified focus at the attribute verification step, with a later module still pending");
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_ATTRIBUTE_NAME, UserType.class, ProfileCompilerOptions.create());
        MidpointAuthentication authentication = createAttributeVerificationAuthentication(principal);

        when("an incorrect verification value is submitted");
        AuthenticationException failure;
        try {
            providerOf(authentication, 1).authenticate(
                    new AttributeVerificationToken(principal, Map.of(UserType.F_EMPLOYEE_NUMBER, WRONG_VERIFICATION)));
            fail("Unexpected success with incorrect attribute verification input");
            return;
        } catch (AuthenticationException e) {
            failure = e;
        }
        finishFailedRequest(authentication, failure);

        then("one failure event is recorded although the overall sequence is not complete");
        AuditEventRecord record = assertSingleFailure(USER_ATTRIBUTE_NAME, USER_ATTRIBUTE_OID, "password mismatch");
        assertDoesNotContain(record, WRONG_VERIFICATION);
    }

    @Test
    public void test140FailedFocusIdentificationIsNotAttributedOrExposed() throws Exception {
        given("focus identification by a non-username attribute");
        MidpointAuthentication authentication = createFocusIdentificationAuthentication();

        when("an arbitrary identification value that matches an unrelated username is submitted");
        AuthenticationException failure;
        try {
            providerOf(authentication, 0).authenticate(
                    new FocusVerificationToken(Map.of(UserType.F_EMPLOYEE_NUMBER, UNTRUSTED_IDENTIFICATION)));
            fail("Unexpected focus identification success");
            return;
        } catch (AuthenticationException e) {
            failure = e;
        }
        finishFailedRequest(authentication, failure);

        then("the failure is audited without a username or initiator");
        List<AuditEventRecord> records = createSessionRecords();
        assertThat(records).hasSize(1);
        AuditEventRecord record = records.get(0);
        assertBaseFailure(record, SchemaConstants.CHANNEL_RESET_PASSWORD_URI, "no focus");
        assertThat(record.getParameter()).isNull();
        assertThat(record.getInitiatorRef()).isNull();
        assertDoesNotContain(record, UNTRUSTED_IDENTIFICATION);
    }

    @Test
    public void test150SuccessAfterFailedAttemptsIsStillAudited() throws Exception {
        given("a nonce-only reset sequence and a valid nonce");
        setupNonce(USER_SUCCESS_OID);
        MidpointAuthentication authentication = createNonceAuthentication(false);

        and("two failed attempts have already been audited");
        for (int i = 0; i < 2; i++) {
            submitBadNonce(authentication, USER_SUCCESS_NAME, BAD_NONCE + i);
            restartAuthenticationForNonceRetry(authentication, null);
        }

        when("the valid nonce is submitted");
        providerOf(authentication, 0).authenticate(new MailNonceAuthenticationToken(USER_SUCCESS_NAME, NONCE));
        authentication.getProcessingModuleAuthentication().setState(AuthenticationModuleState.SUCCESSFULLY);
        finishSuccessfulRequest(authentication);

        then("the failures and the existing success event are all present exactly once");
        List<AuditEventRecord> records = createSessionRecords();
        assertThat(records).hasSize(3);
        assertThat(records.stream()
                .filter(record -> record.getOutcome() == OperationResultStatus.FATAL_ERROR))
                .hasSize(2);
        assertThat(records.stream()
                .filter(record -> record.getOutcome() == OperationResultStatus.SUCCESS))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getEventStage()).isEqualTo(AuditEventStage.REQUEST);
                    assertThat(record.getChannel()).isEqualTo(SchemaConstants.CHANNEL_RESET_PASSWORD_URI);
                    assertThat(record.getParameter()).isEqualTo(USER_SUCCESS_NAME);
                    assertThat(record.getInitiatorRef()).isNotNull();
                    assertThat(record.getInitiatorRef().getOid()).isEqualTo(USER_SUCCESS_OID);
                });
    }

    @Test
    public void test160NormalLoginFailureAuditingIsUnchanged() throws Exception {
        given("an ordinary GUI login sequence");
        MidpointAuthentication authentication = createLoginAuthentication();

        when("the password is incorrect");
        AuthenticationException failure;
        try {
            providerOf(authentication, 0).authenticate(
                    new UsernamePasswordAuthenticationToken(USER_LOGIN_NAME, BAD_PASSWORD));
            fail("Unexpected success with incorrect password");
            return;
        } catch (AuthenticationException e) {
            failure = e;
        }
        finishFailedRequest(authentication, failure);

        then("the existing user-channel failure event is recorded once");
        List<AuditEventRecord> records = createSessionRecords();
        assertThat(records).hasSize(1);
        AuditEventRecord record = records.get(0);
        assertBaseFailure(record, SchemaConstants.CHANNEL_USER_URI, "password mismatch");
        assertThat(record.getParameter()).isEqualTo(USER_LOGIN_NAME);
        assertThat(record.getInitiatorRef()).isNotNull();
        assertThat(record.getInitiatorRef().getOid()).isEqualTo(USER_LOGIN_OID);
        assertDoesNotContain(record, BAD_PASSWORD);
    }

    @Test
    public void test170ExpiredNonceIsAuditedOnceWithConnectionInformation() throws Exception {
        given("an expired nonce issued for the password reset sequence");
        setupNonce(USER_ATTRIBUTE_OID, RESET_SEQUENCE,
                new MetadataType().createTimestamp("2000-01-01T00:00:00Z"));
        MidpointAuthentication authentication = createNonceAuthentication(true);

        when("the expired nonce is submitted");
        MockHttpServletRequest request = submitBadNonce(authentication, USER_ATTRIBUTE_NAME, NONCE);

        then("one failed reset event contains the connection context without exposing the nonce");
        AuditEventRecord record = assertSingleFailure(USER_ATTRIBUTE_NAME, USER_ATTRIBUTE_OID, "password expired");
        assertThat(record.getRemoteHostAddress()).isEqualTo(REMOTE_ADDRESS);
        assertThat(record.getSessionIdentifier()).isEqualTo(SecurityUtil.getOrCreateAuditSessionId(request));
        assertDoesNotContain(record, NONCE);
    }

    @Test
    public void test180RemovedNonceIsAuditedOnce() throws Exception {
        given("a password reset nonce that has already been removed");
        setupNonce(USER_NONCE_OID);
        removeNonce(USER_NONCE_OID);
        MidpointAuthentication authentication = createNonceAuthentication(true);

        when("the old nonce value is submitted");
        submitBadNonce(authentication, USER_NONCE_NAME, NONCE);

        then("the rejected attempt is audited once without exposing the old nonce");
        AuditEventRecord record = assertSingleFailure(USER_NONCE_NAME, USER_NONCE_OID, "no credentials in user");
        assertDoesNotContain(record, NONCE);
    }

    @Test
    public void test190NonceIssuedForDifferentSequenceIsAuditedOnce() throws Exception {
        given("a nonce issued for a different authentication sequence");
        setupNonce(USER_EMPTY_OID, OTHER_SEQUENCE, null);
        MidpointAuthentication authentication = createNonceAuthentication(true);

        when("that nonce is submitted to the password reset sequence");
        submitBadNonce(authentication, USER_EMPTY_NAME, NONCE);

        then("the sequence mismatch is audited once without exposing the nonce");
        AuditEventRecord record = assertSingleFailure(USER_EMPTY_NAME, USER_EMPTY_OID, "password mismatch");
        assertDoesNotContain(record, NONCE);
    }

    private MidpointAuthentication createNonceAuthentication(boolean addFollowingModule) {
        AuthenticationSequenceModuleType nonceType = sequenceModule(
                MODULE_NONCE, 10, addFollowingModule
                        ? AuthenticationSequenceModuleNecessityType.REQUIRED
                        : AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        AuthenticationSequenceType sequence = resetSequence().module(nonceType);
        if (addFollowingModule) {
            sequence.module(sequenceModule(
                    MODULE_ATTRIBUTE, 20, AuthenticationSequenceModuleNecessityType.REQUIRED));
        }

        MidpointAuthentication authentication = newResetAuthentication(sequence);
        MailNonceModuleAuthenticationImpl nonceModule = new MailNonceModuleAuthenticationImpl(nonceType);
        nonceModule.setNameOfModule(MODULE_NONCE);
        nonceModule.setCredentialType(NonceCredentialsPolicyType.class);

        List<AuthModule<?>> modules = new ArrayList<>();
        modules.add(authModule(RESET_SEQUENCE,
                new MailNonceAuthenticationModuleType().identifier(MODULE_NONCE),
                nonceModule, autowired(new MailNonceProvider())));
        if (addFollowingModule) {
            AuthenticationSequenceModuleType followingType = sequence.getModule().get(1);
            AttributeVerificationModuleAuthenticationImpl followingModule = new AttributeVerificationModuleAuthenticationImpl(followingType);
            followingModule.setNameOfModule(MODULE_ATTRIBUTE);
            modules.add(authModule(RESET_SEQUENCE,
                    new AttributeVerificationAuthenticationModuleType().identifier(MODULE_ATTRIBUTE),
                    followingModule, null));
        }
        authentication.setAuthModules(modules);
        authentication.addAuthentications(modules.get(0).getBaseModuleAuthentication());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private MidpointAuthentication createRetryableNonceAuthentication(MidPointPrincipal principal) {
        AuthenticationSequenceModuleType focusType = sequenceModule(MODULE_FOCUS, 10, AuthenticationSequenceModuleNecessityType.REQUISITE);
        AuthenticationSequenceModuleType nonceType = sequenceModule(MODULE_NONCE, 20, AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceType sequence = resetSequence().module(focusType).module(nonceType);

        FocusIdentificationModuleAuthenticationImpl focusModule = new FocusIdentificationModuleAuthenticationImpl(focusType);
        focusModule.setNameOfModule(MODULE_FOCUS);
        focusModule.setState(AuthenticationModuleState.SUCCESSFULLY);
        MailNonceModuleAuthenticationImpl nonceModule = new MailNonceModuleAuthenticationImpl(nonceType);
        nonceModule.setNameOfModule(MODULE_NONCE);
        nonceModule.setCredentialType(NonceCredentialsPolicyType.class);

        MidpointAuthentication authentication = newResetAuthentication(sequence);
        List<AuthModule<?>> modules = List.of(
                authModule(RESET_SEQUENCE,
                        new FocusIdentificationAuthenticationModuleType().identifier(MODULE_FOCUS),
                        focusModule, null),
                authModule(RESET_SEQUENCE,
                        new MailNonceAuthenticationModuleType().identifier(MODULE_NONCE),
                        nonceModule, autowired(new MailNonceProvider())));
        authentication.setAuthModules(modules);
        authentication.addAuthentications(modules.get(0).getBaseModuleAuthentication());
        authentication.setPrincipal(principal);
        authentication.addAuthentications(modules.get(1).getBaseModuleAuthentication());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private void restartAuthenticationForNonceRetry(MidpointAuthentication authentication, MidPointPrincipal principal) {
        authentication.restart();
        for (AuthModule<?> module : authentication.getAuthModules()) {
            authentication.addAuthentications(module.getBaseModuleAuthentication());
        }
        authentication.setPrincipal(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(authentication.getProcessingModuleAuthenticationIdentifier()).isEqualTo(MODULE_NONCE);
    }

    private MidpointAuthentication createAttributeVerificationAuthentication(MidPointPrincipal principal) {
        AuthenticationSequenceModuleType focusType = sequenceModule(MODULE_FOCUS, 10, AuthenticationSequenceModuleNecessityType.REQUISITE);
        AuthenticationSequenceModuleType attributeType = sequenceModule(MODULE_ATTRIBUTE, 20, AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceModuleType nonceType = sequenceModule(MODULE_NONCE, 30, AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceType sequence = resetSequence().module(focusType).module(attributeType).module(nonceType);

        FocusIdentificationModuleAuthenticationImpl focusModule = new FocusIdentificationModuleAuthenticationImpl(focusType);
        focusModule.setNameOfModule(MODULE_FOCUS);
        focusModule.setState(AuthenticationModuleState.SUCCESSFULLY);
        AttributeVerificationModuleAuthenticationImpl attributeModule = new AttributeVerificationModuleAuthenticationImpl(attributeType);
        attributeModule.setNameOfModule(MODULE_ATTRIBUTE);
        attributeModule.setCredentialType(AttributeVerificationCredentialsPolicyType.class);
        MailNonceModuleAuthenticationImpl nonceModule = new MailNonceModuleAuthenticationImpl(nonceType);
        nonceModule.setNameOfModule(MODULE_NONCE);

        MidpointAuthentication authentication = newResetAuthentication(sequence);
        authentication.setAuthModules(List.of(
                authModule(RESET_SEQUENCE,
                        new FocusIdentificationAuthenticationModuleType().identifier(MODULE_FOCUS),
                        focusModule, null),
                authModule(RESET_SEQUENCE,
                        new AttributeVerificationAuthenticationModuleType().identifier(MODULE_ATTRIBUTE),
                        attributeModule, autowired(new AttributeVerificationProvider())),
                authModule(RESET_SEQUENCE,
                        new MailNonceAuthenticationModuleType().identifier(MODULE_NONCE),
                        nonceModule, null)));
        authentication.addAuthentications(focusModule);
        authentication.setPrincipal(principal);
        authentication.addAuthentications(attributeModule);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private MidpointAuthentication createFocusIdentificationAuthentication() {
        AuthenticationSequenceModuleType focusType = sequenceModule(MODULE_FOCUS, 10, AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceModuleType nonceType = sequenceModule(MODULE_NONCE, 20, AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceType sequence = resetSequence().module(focusType).module(nonceType);

        FocusIdentificationModuleAuthenticationImpl focusModule = new FocusIdentificationModuleAuthenticationImpl(focusType);
        focusModule.setNameOfModule(MODULE_FOCUS);
        focusModule.setModuleConfiguration(List.of(new ModuleItemConfigurationType().path(new ItemPathType(UserType.F_EMPLOYEE_NUMBER))));
        MailNonceModuleAuthenticationImpl nonceModule = new MailNonceModuleAuthenticationImpl(nonceType);
        nonceModule.setNameOfModule(MODULE_NONCE);

        MidpointAuthentication authentication = newResetAuthentication(sequence);
        authentication.setAuthModules(List.of(
                authModule(RESET_SEQUENCE,
                        new FocusIdentificationAuthenticationModuleType().identifier(MODULE_FOCUS),
                        focusModule, autowired(new FocusIdentificationProvider())),
                authModule(RESET_SEQUENCE,
                        new MailNonceAuthenticationModuleType().identifier(MODULE_NONCE),
                        nonceModule, null)));
        authentication.addAuthentications(focusModule);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private MidpointAuthentication createLoginAuthentication() {
        AuthenticationSequenceModuleType loginType = sequenceModule(MODULE_LOGIN, 10, AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier(NORMAL_LOGIN_SEQUENCE)
                .channel(new AuthenticationSequenceChannelType()
                        .channelId(SchemaConstants.CHANNEL_USER_URI)
                        .urlSuffix("login"))
                .module(loginType);

        LoginFormModuleAuthenticationImpl loginModule = new LoginFormModuleAuthenticationImpl(loginType);
        loginModule.setNameOfModule(MODULE_LOGIN);
        loginModule.setCredentialType(PasswordCredentialsPolicyType.class);

        MidpointAuthentication authentication = new MidpointAuthentication(sequence);
        authentication.setAuthenticationChannel(new AuthenticationChannelImpl(sequence.getChannel()));
        authentication.setAuthModules(List.of(
                authModule(NORMAL_LOGIN_SEQUENCE,
                        new LoginFormAuthenticationModuleType().identifier(MODULE_LOGIN),
                        loginModule, autowired(new PasswordProvider()))));
        authentication.addAuthentications(loginModule);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private AuthenticationSequenceType resetSequence() {
        return new AuthenticationSequenceType()
                .identifier(RESET_SEQUENCE)
                .channel(new AuthenticationSequenceChannelType()
                        .channelId(SchemaConstants.CHANNEL_RESET_PASSWORD_URI)
                        .urlSuffix("resetPassword"));
    }

    private MidpointAuthentication newResetAuthentication(AuthenticationSequenceType sequence) {
        MidpointAuthentication authentication = new MidpointAuthentication(sequence);
        authentication.setAuthenticationChannel(new ResetPasswordAuthenticationChannel(sequence.getChannel()));
        return authentication;
    }

    private AuthenticationSequenceModuleType sequenceModule(String identifier, int order, AuthenticationSequenceModuleNecessityType necessity) {
        return new AuthenticationSequenceModuleType()
                .identifier(identifier)
                .order(order)
                .necessity(necessity);
    }

    private AuthModule<?> authModule(String sequenceIdentifier, AbstractAuthenticationModuleType moduleType,
            ModuleAuthentication moduleAuthentication, AuthenticationProvider provider) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType, sequenceIdentifier);
        if (provider != null) {
            configuration.addAuthenticationProvider(provider);
        }
        return AuthModuleImpl.build(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE),
                configuration, moduleAuthentication);
    }

    private <P extends AuthenticationProvider> P autowired(P provider) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);
        return provider;
    }

    private AuthenticationProvider providerOf(MidpointAuthentication authentication, int moduleIndex) {
        return authentication.getAuthModules().get(moduleIndex).getAuthenticationProviders().get(0);
    }

    private MockHttpServletRequest submitBadNonce(MidpointAuthentication authentication, String username, String submittedNonce) throws Exception {
        return submitBadNonce(authentication, 0, username, submittedNonce);
    }

    private MockHttpServletRequest submitBadNonce(MidpointAuthentication authentication, int moduleIndex,
            String username, String submittedNonce) throws Exception {
        AuthenticationException failure;
        try {
            providerOf(authentication, moduleIndex).authenticate(new MailNonceAuthenticationToken(username, submittedNonce));
            fail("Unexpected nonce authentication success");
            return null;
        } catch (AuthenticationException e) {
            failure = e;
        }
        return finishFailedRequest(authentication, failure);
    }

    private MockHttpServletRequest finishFailedRequest(MidpointAuthentication authentication, AuthenticationException failure) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/resetPassword");
        request.setRemoteAddr(REMOTE_ADDRESS);
        request.getSession();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MidpointAuthenticationFailureHandler().onAuthenticationFailure(request, response, failure);

        Object resetPasswordFailureMarker = request.getAttribute(MidpointAuthenticationFailureHandler.RESET_PASSWORD_FAILURE_ATTRIBUTE);
        if (SchemaConstants.CHANNEL_RESET_PASSWORD_URI.equals(authentication.getAuthenticationChannel().getChannelId())) {
            assertThat(resetPasswordFailureMarker).isEqualTo(Boolean.TRUE);
        } else {
            assertThat(resetPasswordFailureMarker).isNull();
        }
        runNestedSequenceFilters(request, response, authentication);
        assertThat(request.getAttribute(MidpointAuthenticationFailureHandler.RESET_PASSWORD_FAILURE_ATTRIBUTE)).isNull();
        return request;
    }

    private void finishSuccessfulRequest(MidpointAuthentication authentication) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/resetPassword");
        MockHttpServletResponse response = new MockHttpServletResponse();
        runNestedSequenceFilters(request, response, authentication);
    }

    private void runNestedSequenceFilters(MockHttpServletRequest request, MockHttpServletResponse response, MidpointAuthentication authentication) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SequenceCompletionFilter outerFilter = new SequenceCompletionFilter(authenticationRecorder);
        SequenceCompletionFilter innerFilter = new SequenceCompletionFilter(authenticationRecorder);
        int[] recordsAfterInnerFilter = { -1 };

        outerFilter.doFilter(request, response, (nestedRequest, nestedResponse) -> {
            innerFilter.doFilter(nestedRequest, nestedResponse, new MockFilterChain());
            assertThat(nestedRequest.getAttribute(MidpointAuthenticationFailureHandler.RESET_PASSWORD_FAILURE_ATTRIBUTE)).isNull();
            recordsAfterInnerFilter[0] = createSessionRecords().size();
        });

        assertThat(createSessionRecords()).hasSize(recordsAfterInnerFilter[0]);
    }

    private AuditEventRecord assertSingleFailure(String username, String userOid, String failureReason) {
        List<AuditEventRecord> records = createSessionRecords();
        assertThat(records).hasSize(1);
        AuditEventRecord record = records.get(0);
        assertFailure(record, username, userOid, failureReason);
        return record;
    }

    private void assertFailure(AuditEventRecord record, String username, String userOid, String failureReason) {
        assertBaseFailure(record, SchemaConstants.CHANNEL_RESET_PASSWORD_URI, failureReason);
        assertThat(record.getParameter()).isEqualTo(username);
        assertThat(record.getInitiatorRef()).isNotNull();
        assertThat(record.getInitiatorRef().getOid()).isEqualTo(userOid);
    }

    private void assertBaseFailure(AuditEventRecord record, String channel, String failureReason) {
        assertThat(record.getEventType()).isEqualTo(AuditEventType.CREATE_SESSION);
        assertThat(record.getEventStage()).isEqualTo(AuditEventStage.REQUEST);
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getOutcome()).isEqualTo(OperationResultStatus.FATAL_ERROR);
        assertThat(record.getChannel()).isEqualTo(channel);
        assertThat(record.getMessage()).isEqualTo(failureReason);
    }

    private void assertDoesNotContain(AuditEventRecord record, String sensitiveValue) {
        assertThat(record.debugDump()).doesNotContain(sensitiveValue);
    }

    private List<AuditEventRecord> createSessionRecords() {
        return dummyAuditService.getRecordsOfType(AuditEventType.CREATE_SESSION);
    }

    private void setupNonce(String userOid) throws Exception {
        setupNonce(userOid, RESET_SEQUENCE, null);
    }

    private void setupNonce(String userOid, String sequenceIdentifier, MetadataType metadata) throws Exception {
        ProtectedStringType nonceValue = protectedString(NONCE);
        protector.encrypt(nonceValue);
        NonceType nonce = new NonceType()
                .value(nonceValue)
                .sequenceIdentifier(sequenceIdentifier);
        if (metadata != null) {
            nonce.setMetadata(metadata);
        }
        repositoryService.modifyObject(
                UserType.class,
                userOid,
                prismContext.deltaFor(UserType.class)
                        .item(SchemaConstants.PATH_NONCE)
                        .replace(nonce)
                        .asItemDeltas(),
                createOperationResult());
    }

    private void removeNonce(String userOid) throws Exception {
        repositoryService.modifyObject(
                UserType.class,
                userOid,
                prismContext.deltaFor(UserType.class)
                        .item(SchemaConstants.PATH_NONCE)
                        .replace()
                        .asItemDeltas(),
                createOperationResult());
    }

    private void assertUserNonce(String userOid, String expectedNonce) throws Exception {
        UserType user = repositoryService
                .getObject(UserType.class, userOid, null, createOperationResult())
                .asObjectable();
        assertThat(user.getCredentials()).isNotNull();
        assertThat(user.getCredentials().getNonce()).isNotNull();
        assertThat(protector.decryptString(user.getCredentials().getNonce().getValue()))
                .isEqualTo(expectedNonce);
    }

    private ProtectedStringType protectedString(String clearValue) {
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(clearValue);
        return value;
    }
}
