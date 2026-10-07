/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.authentication.impl.channel.AccountActivationAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.authentication.impl.filter.SequenceCompletionFilter;
import com.evolveum.midpoint.authentication.impl.module.authentication.LoginFormModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.provider.MailNonceProvider;
import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Account activation sequence: mail nonce from the notification link followed by the login form.
 * There is no retry of a failed module in place in 4.10, the sequence restarts and the user opens the link again.
 *
 * Issue 5490
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
public class TestAccountActivationNonce extends AbstractModelImplementationIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    private static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    private static final String USER_ACTIVATOR_OID = "0f3c9b52-2c4e-4b0e-9a1d-5490aaaa0001";
    private static final String USER_ACTIVATOR_NAME = "activator";
    private static final String USER_LOCKER_OID = "0f3c9b52-2c4e-4b0e-9a1d-5490aaaa0002";
    private static final String USER_LOCKER_NAME = "locker";

    private static final String PASSWORD_GOOD = "Activate5490!";
    private static final String PASSWORD_BAD = "not my password";
    private static final String NONCE = "qwertyuiop123456";

    private static final String MODULE_MAIL_NONCE = "accountActivationMailNonce";
    private static final String MODULE_LOGIN_FORM = "loginForm";
    private static final String SEQUENCE_IDENTIFIER = "account-activation";

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;
    @Autowired private Protector protector;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        modelService.postInit(initResult);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

        // Added via model, the same way the users are created in the real deployment,
        // so the credentials are stored according to the credentials policy.
        addObject(createActivationUser(USER_ACTIVATOR_OID, USER_ACTIVATOR_NAME).asPrismObject(), initTask, initResult);
        addObject(createActivationUser(USER_LOCKER_OID, USER_LOCKER_NAME).asPrismObject(), initTask, initResult);
    }

    private UserType createActivationUser(String oid, String name) {
        return new UserType()
                .oid(oid)
                .name(name)
                .emailAddress(name + "@example.com")
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .value(protectedString(PASSWORD_GOOD)))
                        .nonce(new NonceType()
                                .value(protectedString(NONCE))
                                .sequenceIdentifier(SEQUENCE_IDENTIFIER)));
    }

    private ProtectedStringType protectedString(String clearValue) {
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue(clearValue);
        return value;
    }

    /** Opening the link authenticates by the nonce only. The nonce stays, the sequence waits for the password. */
    @Test
    public void test100LinkOpenedNonceKept() throws Exception {
        given("user with nonce from the activation notification");
        setupNonce(USER_ACTIVATOR_OID);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            when("activation link is opened");
            MidpointAuthentication mpAuthentication = openActivationLink(USER_ACTIVATOR_NAME, NONCE);

            then("mail nonce module succeeded, login form is being processed, sequence not authenticated");
            assertEquals(AuthenticationModuleState.SUCCESSFULLY, mailNonceModule(mpAuthentication).getState());
            assertEquals(MODULE_LOGIN_FORM, mpAuthentication.getProcessingModuleAuthentication().getModuleIdentifier());
            assertFalse("Sequence must not be authenticated by the nonce alone", mpAuthentication.isAuthenticated());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is kept for the password step");
        assertUserHasNonce(USER_ACTIVATOR_OID);
    }

    /** Wrong password keeps the nonce; the sequence restarts and the link from the mail works again. */
    @Test
    public void test110WrongPasswordNonceKeptLinkWorksAgain() throws Exception {
        given("activation link opened");
        setupNonce(USER_ACTIVATOR_OID);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            MidpointAuthentication mpAuthentication = openActivationLink(USER_ACTIVATOR_NAME, NONCE);

            when("wrong password is submitted");
            try {
                submitPassword(mpAuthentication, USER_ACTIVATOR_NAME, PASSWORD_BAD);
                fail("Unexpected success with wrong password");
            } catch (BadCredentialsException e) {
                displayExpectedException(e);
            }

            then("login form failed, sequence failed, user is not locked");
            assertEquals(AuthenticationModuleState.FAILURE, loginFormModule(mpAuthentication).getState());
            assertFalse(mpAuthentication.isAuthenticated());
            assertTrue("Sequence should be finished (failed)", mpAuthentication.isFinished());
            assertFalse(mpAuthentication.isOverLockoutMaxAttempts());
            assertUserLockout(USER_ACTIVATOR_OID, LockoutStatusType.NORMAL);

            and("nonce is kept");
            assertUserHasNonce(USER_ACTIVATOR_OID);

            when("next request arrives");
            then("sequence has no module to continue with, it is restarted");
            assertEquals(MidpointAuthentication.NO_MODULE_FOUND_INDEX, mpAuthentication.getIndexOfProcessingModule(false));

            when("activation link is opened again");
            MidpointAuthentication restarted = openActivationLink(USER_ACTIVATOR_NAME, NONCE);

            then("mail nonce module succeeds again, login form is being processed");
            assertEquals(AuthenticationModuleState.SUCCESSFULLY, mailNonceModule(restarted).getState());
            assertEquals(MODULE_LOGIN_FORM, restarted.getProcessingModuleAuthentication().getModuleIdentifier());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is still there");
        assertUserHasNonce(USER_ACTIVATOR_OID);
    }

    /** Correct password after a wrong one completes the sequence and spends the nonce. */
    @Test
    public void test120CorrectPasswordAfterWrongOneSpendsNonce() throws Exception {
        given("activation link opened, wrong password submitted once");
        setupNonce(USER_ACTIVATOR_OID);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            MidpointAuthentication mpAuthentication = openActivationLink(USER_ACTIVATOR_NAME, NONCE);
            try {
                submitPassword(mpAuthentication, USER_ACTIVATOR_NAME, PASSWORD_BAD);
                fail("Unexpected success with wrong password");
            } catch (BadCredentialsException e) {
                displayExpectedException(e);
            }
            assertUserHasNonce(USER_ACTIVATOR_OID);

            when("activation link is opened again and correct password is submitted");
            mpAuthentication = openActivationLink(USER_ACTIVATOR_NAME, NONCE);
            submitPassword(mpAuthentication, USER_ACTIVATOR_NAME, PASSWORD_GOOD);

            then("sequence is authenticated");
            assertTrue("Sequence should be authenticated", mpAuthentication.isAuthenticated());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is spent");
        assertUserHasNoNonce(USER_ACTIVATOR_OID);
        assertUserLockout(USER_ACTIVATOR_OID, LockoutStatusType.NORMAL);
    }

    /**
     * Wrong passwords over lockoutMaxFailedAttempts of the password policy (3 in the test policy) lock the user
     * for lockoutDuration. The nonce is kept, the link can be used again after the lockout expires.
     */
    @Test
    public void test130WrongPasswordsLockUserNonceKept() throws Exception {
        given("activation link opened");
        setupNonce(USER_LOCKER_OID);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            when("wrong password is submitted three times, the link is opened again after each failure");
            for (int i = 1; i <= 3; i++) {
                MidpointAuthentication mpAuthentication = openActivationLink(USER_LOCKER_NAME, NONCE);
                try {
                    submitPassword(mpAuthentication, USER_LOCKER_NAME, PASSWORD_BAD);
                    fail("Unexpected success with wrong password");
                } catch (BadCredentialsException e) {
                    displayExpectedException(e);
                }
                if (i < 3) {
                    assertUserLockout(USER_LOCKER_OID, LockoutStatusType.NORMAL);
                }
            }

            then("user is locked, nonce is kept");
            PrismObject<UserType> userLocked = getUserRepo(USER_LOCKER_OID);
            display("user locked", userLocked);
            assertUserLockout(userLocked, LockoutStatusType.LOCKED);
            assertNotNull("No lockout expiration",
                    userLocked.asObjectable().getActivation().getLockoutExpirationTimestamp());
            assertUserHasNonce(USER_LOCKER_OID);

            when("link is opened again and correct password is submitted while locked");
            MidpointAuthentication mpAuthentication = openActivationLink(USER_LOCKER_NAME, NONCE);
            try {
                submitPassword(mpAuthentication, USER_LOCKER_NAME, PASSWORD_GOOD);
                fail("Unexpected success while locked");
            } catch (LockedException e) {
                displayExpectedException(e);
            }

            then("authentication is refused, nonce is kept");
            assertTrue("Lockout not propagated to authentication", mpAuthentication.isOverLockoutMaxAttempts());
            assertFalse(mpAuthentication.isAuthenticated());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        assertUserLockout(USER_LOCKER_OID, LockoutStatusType.LOCKED);
        assertUserHasNonce(USER_LOCKER_OID);
    }

    /** The activation nonce must not open another sequence that starts with a mail nonce module, e.g. password reset. */
    @Test
    public void test200ActivationNonceRefusedByOtherSequence() throws Exception {
        given("user with nonce issued for the activation sequence");
        setupNonce(USER_ACTIVATOR_OID);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            when("the token is used in the password reset sequence");
            try {
                openResetLink(USER_ACTIVATOR_NAME, NONCE);
                fail("Unexpected success with nonce of another sequence");
            } catch (BadCredentialsException e) {
                displayExpectedException(e);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        then("nonce is kept for the activation sequence");
        assertUserHasNonce(USER_ACTIVATOR_OID);
    }

    /** A nonce issued before 4.11 carries no sequence identifier and is accepted, so links in flight keep working. */
    @Test
    public void test210NonceWithoutSequenceIdentifierAccepted() throws Exception {
        given("user with nonce without sequence identifier");
        setupNonce(USER_ACTIVATOR_OID, null);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            when("activation link is opened and correct password submitted");
            MidpointAuthentication mpAuthentication = openActivationLink(USER_ACTIVATOR_NAME, NONCE);
            submitPassword(mpAuthentication, USER_ACTIVATOR_NAME, PASSWORD_GOOD);

            then("sequence is authenticated");
            assertTrue(mpAuthentication.isAuthenticated());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is spent");
        assertUserHasNoNonce(USER_ACTIVATOR_OID);
    }

    /**
     * Opens the activation link: the mail nonce module authenticates, the request ends as in the real flow
     * and the login form module becomes the processing one.
     */
    private MidpointAuthentication openActivationLink(String username, String nonce) throws Exception {
        AuthenticationSequenceModuleType mailNonceModuleType = new AuthenticationSequenceModuleType()
                .identifier(MODULE_MAIL_NONCE)
                .order(10)
                .necessity(AuthenticationSequenceModuleNecessityType.REQUISITE);
        AuthenticationSequenceModuleType loginFormModuleType = new AuthenticationSequenceModuleType()
                .identifier(MODULE_LOGIN_FORM)
                .order(20)
                .necessity(AuthenticationSequenceModuleNecessityType.REQUIRED);
        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier(SEQUENCE_IDENTIFIER)
                .channel(new AuthenticationSequenceChannelType()
                        .channelId(SchemaConstants.CHANNEL_ACCOUNT_ACTIVATION_URI)
                        .urlSuffix("accountActivation"))
                .module(mailNonceModuleType)
                .module(loginFormModuleType);

        MidpointAuthentication mpAuthentication = new MidpointAuthentication(sequence);
        mpAuthentication.setAuthenticationChannel(new AccountActivationAuthenticationChannel(sequence.getChannel()));

        MailNonceModuleAuthenticationImpl mailNonceModule = new MailNonceModuleAuthenticationImpl(mailNonceModuleType);
        mailNonceModule.setNameOfModule(MODULE_MAIL_NONCE);
        mailNonceModule.setCredentialType(NonceCredentialsPolicyType.class);
        LoginFormModuleAuthenticationImpl loginFormModule = new LoginFormModuleAuthenticationImpl(loginFormModuleType);
        loginFormModule.setNameOfModule(MODULE_LOGIN_FORM);
        loginFormModule.setCredentialType(PasswordCredentialsPolicyType.class);

        // the modules with their providers, as built for each request of the sequence
        mpAuthentication.setAuthModules(List.of(
                authModule(new MailNonceAuthenticationModuleType().identifier(MODULE_MAIL_NONCE),
                        mailNonceModule, autowired(new MailNonceProvider())),
                authModule(new LoginFormAuthenticationModuleType().identifier(MODULE_LOGIN_FORM),
                        loginFormModule, autowired(new PasswordProvider()))));

        mpAuthentication.addAuthentication(mailNonceModule);
        SecurityContextHolder.getContext().setAuthentication(mpAuthentication);

        providerOf(mpAuthentication, 0).authenticate(new MailNonceAuthenticationToken(username, nonce));
        finishRequest(mpAuthentication, null);

        mpAuthentication.addAuthentication(loginFormModule);
        return mpAuthentication;
    }

    /** Opens a link of a password reset like sequence: mail nonce module only, in the reset password channel. */
    private void openResetLink(String username, String nonce) {
        AuthenticationSequenceModuleType mailNonceModuleType = new AuthenticationSequenceModuleType()
                .identifier(MODULE_MAIL_NONCE)
                .order(10)
                .necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier("password-reset")
                .channel(new AuthenticationSequenceChannelType()
                        .channelId(SchemaConstants.CHANNEL_RESET_PASSWORD_URI)
                        .urlSuffix("resetPassword"))
                .module(mailNonceModuleType);

        MidpointAuthentication mpAuthentication = new MidpointAuthentication(sequence);
        mpAuthentication.setAuthenticationChannel(new AuthenticationChannelImpl(sequence.getChannel()));

        MailNonceModuleAuthenticationImpl mailNonceModule = new MailNonceModuleAuthenticationImpl(mailNonceModuleType);
        mailNonceModule.setNameOfModule(MODULE_MAIL_NONCE);
        mailNonceModule.setCredentialType(NonceCredentialsPolicyType.class);
        mpAuthentication.setAuthModules(List.of(
                authModule(new MailNonceAuthenticationModuleType().identifier(MODULE_MAIL_NONCE),
                        mailNonceModule, autowired(new MailNonceProvider()))));
        mpAuthentication.addAuthentication(mailNonceModule);
        SecurityContextHolder.getContext().setAuthentication(mpAuthentication);

        try {
            providerOf(mpAuthentication, 0).authenticate(new MailNonceAuthenticationToken(username, nonce));
            finishRequest(mpAuthentication, null);
        } catch (AuthenticationException e) {
            finishRequest(mpAuthentication, e);
            throw e;
        }
    }

    private AuthModule<?> authModule(
            AbstractAuthenticationModuleType moduleType, ModuleAuthentication moduleAuthentication, AuthenticationProvider provider) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType, SEQUENCE_IDENTIFIER);
        configuration.addAuthenticationProvider(provider);
        return AuthModuleImpl.build(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE), configuration, moduleAuthentication);
    }

    private <P extends AuthenticationProvider> P autowired(P provider) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);
        return provider;
    }

    private AuthenticationProvider providerOf(MidpointAuthentication mpAuthentication, int moduleIndex) {
        return mpAuthentication.getAuthModules().get(moduleIndex).getAuthenticationProviders().get(0);
    }

    /** Submits the login form. The request ends as in the real flow, i.e. the handlers and the audit filter run. */
    private void submitPassword(MidpointAuthentication mpAuthentication, String username, String password) {
        try {
            providerOf(mpAuthentication, 1).authenticate(new UsernamePasswordAuthenticationToken(username, password));
            finishRequest(mpAuthentication, null);
        } catch (AuthenticationException e) {
            finishRequest(mpAuthentication, e);
            throw e;
        }
    }

    /**
     * What the success or failure handler and {@link SequenceCompletionFilter} do at the end of a request
     * that processed the current module.
     */
    private void finishRequest(MidpointAuthentication mpAuthentication, AuthenticationException failure) {
        ModuleAuthentication module = mpAuthentication.getProcessingModuleAuthentication();
        if (failure == null) {
            module.setState(AuthenticationModuleState.SUCCESSFULLY);
        } else {
            module.recordFailure(failure);
        }
        new SequenceCompletionFilter(authenticationRecorder).writeRecord(new MockHttpServletRequest(), mpAuthentication);
    }

    private ModuleAuthentication mailNonceModule(MidpointAuthentication mpAuthentication) {
        return mpAuthentication.getAuthentications().get(0);
    }

    private ModuleAuthentication loginFormModule(MidpointAuthentication mpAuthentication) {
        return mpAuthentication.getAuthentications().get(1);
    }

    private void setupNonce(String userOid) throws Exception {
        setupNonce(userOid, SEQUENCE_IDENTIFIER);
    }

    /** Nonce as issued by createAccountActivationLink; null sequence identifier = nonce issued before 4.11. */
    private void setupNonce(String userOid, String sequenceIdentifier) throws Exception {
        Task task = getTestTask();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(SchemaConstants.PATH_NONCE)
                        .replace(new NonceType().value(protectedString(NONCE)).sequenceIdentifier(sequenceIdentifier))
                        .asObjectDelta(userOid),
                null, task, task.getResult());
        assertUserHasNonce(userOid);
    }

    /** Repository read: the security context holds the activating user during the tests, model reads would be denied. */
    private PrismObject<UserType> getUserRepo(String oid) throws Exception {
        return repositoryService.getObject(UserType.class, oid, null, createOperationResult());
    }

    private void assertUserHasNonce(String userOid) throws Exception {
        CredentialsType credentials = getUserRepo(userOid).asObjectable().getCredentials();
        assertTrue("Nonce is missing", credentials != null && credentials.getNonce() != null
                && credentials.getNonce().getValue() != null);
        assertEquals("Unexpected nonce value", NONCE, decrypt(credentials.getNonce().getValue()));
    }

    private void assertUserHasNoNonce(String userOid) throws Exception {
        PrismObject<UserType> user = getUserRepo(userOid);
        display("user after", user);
        CredentialsType credentials = user.asObjectable().getCredentials();
        assertTrue("Nonce was not spent", credentials == null || credentials.getNonce() == null);
    }

    private String decrypt(ProtectedStringType value) throws EncryptionException {
        return protector.decryptString(value);
    }

    private void assertUserLockout(String userOid, LockoutStatusType expected) throws Exception {
        assertUserLockout(getUserRepo(userOid), expected);
    }
}
