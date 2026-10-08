/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.provider;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpServletRequest;
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
import com.evolveum.midpoint.authentication.TestAccountActivationNonce;
import com.evolveum.midpoint.authentication.impl.channel.SelfRegistrationAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.filter.SequenceCompletionFilter;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.authentication.impl.provider.MailNonceProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests that the nonce credential is spent (removed) when the authentication sequence
 * with the mail nonce module succeeds, e.g. when a self-registration confirmation
 * link is used, and that the authentication behavior recording does not resurrect it.
 * A leftover nonce later blocks the password reset flow, which considers the reset
 * mail as already sent. See #12082.
 *
 * The nonce is spent by {@link SequenceCompletionFilter} at the end of the sequence, not by
 * {@link MailNonceProvider} itself, see issue 5490 and {@link TestAccountActivationNonce}.
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
public class TestMailNonceProvider extends AbstractModelImplementationIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    private static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    private static final String USER_REGISTRANT_OID = "8e442ab6-5f24-4bc5-b8e7-2f4e0c81a260";
    private static final String USER_REGISTRANT_NAME = "registrant";
    private static final String USER_REGISTRANT_NONCE = "qwertyuiop123456";

    private static final String MODULE_IDENTIFIER = "MailNonce";
    private static final String SEQUENCE_IDENTIFIER = "self-registration-confirmation";

    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        modelService.postInit(initResult);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

        // Added via model (not raw), the same way self-registration stores the user,
        // so the nonce is stored according to the credentials policy.
        addObject(createRegistrant().asPrismObject(), initTask, initResult);
    }

    private UserType createRegistrant() {
        ProtectedStringType nonceValue = new ProtectedStringType();
        nonceValue.setClearValue(USER_REGISTRANT_NONCE);
        return new UserType()
                .oid(USER_REGISTRANT_OID)
                .name(USER_REGISTRANT_NAME)
                .emailAddress("registrant@example.com")
                .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT)
                .credentials(new CredentialsType()
                        .nonce(new NonceType()
                                .value(nonceValue)));
    }

    @Test
    public void test100NonceRemovedAfterSuccessfulAuthentication() throws Exception {
        given("user with nonce and mail nonce module being processed in self registration channel");
        assertUserHasNonce();

        MailNonceProvider provider = new MailNonceProvider();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);

        MidpointAuthentication mpAuthentication = createMpAuthentication(provider);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(mpAuthentication);

            when("mail nonce authentication (confirmation link) succeeds");
            Authentication authentication = provider.authenticate(
                    new MailNonceAuthenticationToken(USER_REGISTRANT_NAME, USER_REGISTRANT_NONCE));

            then("user is authenticated");
            assertNotNull("No authentication result", authentication);
            assertTrue("Principal was not set after successful authentication",
                    mpAuthentication.getPrincipal() instanceof MidPointPrincipal);

            and("sequence succeeds");
            finishSequence(mpAuthentication);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is spent, removed from the user");
        assertUserHasNoNonce();
    }

    @Test
    public void test110NonceRemovedWithPrecedingFocusIdentificationModule() throws Exception {
        given("user with nonce, focus identification succeeded, mail nonce module being processed");
        setupNonce();
        assertUserHasNonce();

        MailNonceProvider provider = new MailNonceProvider();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);

        MidpointAuthentication mpAuthentication = createMpAuthenticationWithFocusIdentification(provider);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(mpAuthentication);

            when("mail nonce authentication (confirmation link) succeeds");
            Authentication authentication = provider.authenticate(
                    new MailNonceAuthenticationToken(USER_REGISTRANT_NAME, USER_REGISTRANT_NONCE));

            then("user is authenticated");
            assertNotNull("No authentication result", authentication);
            assertTrue("Principal was not set after successful authentication",
                    mpAuthentication.getPrincipal() instanceof MidPointPrincipal);

            and("sequence succeeds");
            finishSequence(mpAuthentication);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        and("nonce is spent, removed from the user");
        assertUserHasNoNonce();
    }

    /**
     * After the sequence spends (removes) the nonce, FocusAuthenticationResultRecorder may record
     * further authentication results of the same principal. The in-memory principal focus still
     * contains the nonce, so the recording must not resurrect the already spent nonce.
     *
     * See #12082
     */
    @Test
    public void test120NonceNotResurrectedBySequenceAuthenticationRecording() throws Exception {
        given("user with nonce and mail nonce module being processed in self registration channel");
        setupNonce();
        assertUserHasNonce();

        MailNonceProvider provider = new MailNonceProvider();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);

        MidpointAuthentication mpAuthentication = createMpAuthentication(provider);

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(mpAuthentication);

            when("mail nonce authentication succeeds, sequence succeeds and spends the nonce");
            provider.authenticate(
                    new MailNonceAuthenticationToken(USER_REGISTRANT_NAME, USER_REGISTRANT_NONCE));
            finishSequence(mpAuthentication);
            assertUserHasNoNonce();

            and("authentication result is recorded again with the stale in-memory principal");
            ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
            connEnv.setSequenceIdentifier(SEQUENCE_IDENTIFIER);
            connEnv.setModuleIdentifier(MODULE_IDENTIFIER);
            MidPointPrincipal stalePrincipal = (MidPointPrincipal) mpAuthentication.getPrincipal();
            assertNotNull("Stale principal has no nonce, test setup is broken",
                    stalePrincipal.getFocus().getCredentials().getNonce());
            authenticationRecorder.recordModuleAuthenticationAttemptSuccess(stalePrincipal, connEnv);
            authenticationRecorder.recordSequenceAuthenticationSuccess(stalePrincipal, connEnv);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }

        then("nonce is spent and stays removed after the authentication behavior is recorded");
        assertUserHasNoNonce();
    }

    /**
     * What the success handler and {@link SequenceCompletionFilter} do at the end of the request
     * that completed the sequence.
     */
    private void finishSequence(MidpointAuthentication mpAuthentication) {
        mpAuthentication.getProcessingModuleAuthentication().setState(AuthenticationModuleState.SUCCESSFULLY);
        assertTrue("Sequence is not authenticated, test setup is broken", mpAuthentication.isAuthenticated());
        new SequenceCompletionFilter(authenticationRecorder).writeRecord(new MockHttpServletRequest(), mpAuthentication);
    }

    private void setupNonce() throws Exception {
        Task task = getTestTask();
        ProtectedStringType nonceValue = new ProtectedStringType();
        nonceValue.setClearValue(USER_REGISTRANT_NONCE);
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(SchemaConstants.PATH_NONCE)
                        .replace(new NonceType().value(nonceValue))
                        .asObjectDelta(USER_REGISTRANT_OID),
                null, task, task.getResult());
    }

    private void assertUserHasNonce() throws Exception {
        OperationResult result = createOperationResult();
        PrismObject<UserType> user =
                repositoryService.getObject(UserType.class, USER_REGISTRANT_OID, null, result);
        CredentialsType credentials = user.asObjectable().getCredentials();
        assertTrue("User has no nonce, test setup is broken",
                credentials != null && credentials.getNonce() != null && credentials.getNonce().getValue() != null);
    }

    private void assertUserHasNoNonce() throws Exception {
        OperationResult result = createOperationResult();
        PrismObject<UserType> userAfter =
                repositoryService.getObject(UserType.class, USER_REGISTRANT_OID, null, result);
        display("user after", userAfter);
        CredentialsType credentials = userAfter.asObjectable().getCredentials();
        assertTrue("Nonce was not removed after successful mail nonce authentication",
                credentials == null || credentials.getNonce() == null);
    }

    private MidpointAuthentication createMpAuthentication(MailNonceProvider provider) {
        AuthenticationSequenceModuleType moduleType = createMailNonceModuleType();

        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier(SEQUENCE_IDENTIFIER)
                .module(moduleType);

        MailNonceModuleAuthenticationImpl moduleAuthentication = createMailNonceModuleAuthentication(moduleType);
        MidpointAuthentication mpAuthentication = createMpAuthentication(sequence,
                List.of(authModule(new MailNonceAuthenticationModuleType().identifier(MODULE_IDENTIFIER), moduleAuthentication, provider)));
        mpAuthentication.addAuthentications(moduleAuthentication);
        return mpAuthentication;
    }

    private MidpointAuthentication createMpAuthenticationWithFocusIdentification(MailNonceProvider provider) throws Exception {
        AuthenticationSequenceModuleType focusIdentificationModuleType = new AuthenticationSequenceModuleType()
                .identifier("FocusIdentification")
                .order(10)
                .necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
        AuthenticationSequenceModuleType mailNonceModuleType = createMailNonceModuleType();

        AuthenticationSequenceType sequence = new AuthenticationSequenceType()
                .identifier(SEQUENCE_IDENTIFIER)
                .module(focusIdentificationModuleType)
                .module(mailNonceModuleType);

        FocusIdentificationModuleAuthenticationImpl focusIdentificationAuthentication =
                new FocusIdentificationModuleAuthenticationImpl(focusIdentificationModuleType);
        focusIdentificationAuthentication.setNameOfModule("FocusIdentification");
        focusIdentificationAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
        MailNonceModuleAuthenticationImpl mailNonceAuthentication = createMailNonceModuleAuthentication(mailNonceModuleType);

        MidpointAuthentication mpAuthentication = createMpAuthentication(sequence, List.of(
                authModule(new FocusIdentificationAuthenticationModuleType().identifier("FocusIdentification"),
                        focusIdentificationAuthentication, null),
                authModule(new MailNonceAuthenticationModuleType().identifier(MODULE_IDENTIFIER),
                        mailNonceAuthentication, provider)));

        mpAuthentication.addAuthentications(focusIdentificationAuthentication);
        // focus identification module writes the principal to the authentication
        mpAuthentication.setPrincipal(focusProfileService.getPrincipal(
                USER_REGISTRANT_NAME, UserType.class, ProfileCompilerOptions.create()));

        mpAuthentication.addAuthentications(mailNonceAuthentication);
        return mpAuthentication;
    }

    private MidpointAuthentication createMpAuthentication(AuthenticationSequenceType sequence, List<AuthModule<?>> authModules) {
        MidpointAuthentication mpAuthentication = new MidpointAuthentication(sequence);
        AuthenticationSequenceChannelType channelType = new AuthenticationSequenceChannelType()
                .channelId(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
        mpAuthentication.setAuthenticationChannel(new SelfRegistrationAuthenticationChannel(channelType));
        mpAuthentication.setAuthModules(authModules);
        return mpAuthentication;
    }

    /** The module with its provider, as built for each request of the sequence. */
    private AuthModule<?> authModule(
            AbstractAuthenticationModuleType moduleType, ModuleAuthentication moduleAuthentication, AuthenticationProvider provider) {
        ModuleWebSecurityConfigurationImpl configuration = ModuleWebSecurityConfigurationImpl.build(moduleType, SEQUENCE_IDENTIFIER);
        configuration.addAuthenticationProvider(provider);
        return AuthModuleImpl.build(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE), configuration, moduleAuthentication);
    }

    private AuthenticationSequenceModuleType createMailNonceModuleType() {
        return new AuthenticationSequenceModuleType()
                .identifier(MODULE_IDENTIFIER)
                .order(20)
                .necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
    }

    private MailNonceModuleAuthenticationImpl createMailNonceModuleAuthentication(
            AuthenticationSequenceModuleType moduleType) {
        MailNonceModuleAuthenticationImpl moduleAuthentication = new MailNonceModuleAuthenticationImpl(moduleType);
        moduleAuthentication.setNameOfModule(MODULE_IDENTIFIER);
        moduleAuthentication.setState(AuthenticationModuleState.LOGIN_PROCESSING);
        moduleAuthentication.setCredentialType(NonceCredentialsPolicyType.class);
        return moduleAuthentication;
    }
}
