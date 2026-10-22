/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Authentication of users whose archetype references its own security policy ().
 *
 * Global policy: password, then TOTP that is skipped for users without TOTP credential.
 * Archetype policy: TOTP is mandatory.
 * Expected: user without the archetype is let in with password only, user with the archetype is asked for TOTP code.
 *
 * Archetype policy has to adjust the sequence of the global policy under the same identifier.
 * A sequence with different identifier for the same channel ends with a generic error.
 *
 * For more information see issue 12478.
 */
@ContextConfiguration(locations = "classpath:ctx-authentication-test-main.xml")
@DirtiesContext
public class TestArchetypeSecurityPolicySequence extends AbstractModelIntegrationTest {

    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    private static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
    private static final String GLOBAL_POLICY_OID = "7d1c2a35-2c1a-4a54-9d0c-000000012478";
    private static final String ARCHETYPE_POLICY_OID = "0e9a1c56-4d5b-4c0e-9c61-000000012478";
    private static final String ARCHETYPE_OID = "5d0a6f0b-7a43-4b8b-8a0d-000000012478";
    private static final String ROLE_SELF_SERVICE_OID = "a3c1f9d2-6c0e-4c2f-b1a7-000000012478";

    private static final String USER_EXTERNAL = "external12478";
    private static final String USER_INTERNAL = "internal12478";
    private static final String PASSWORD = "Passw0rd.12478";

    private static final String MODULE_LOGIN_FORM = "loginForm";
    private static final String MODULE_TOTP = "totpModule";

    private static final String GLOBAL_SEQUENCE = "default-totp-sequence";
    private static final String GLOBAL_SUFFIX = "gui-totp";

    private static final String MESSAGE_GENERIC_ERROR = "web.security.provider.invalid";

    /** What happens after the password was verified. */
    private enum Step {
        /** User is asked for TOTP code. */
        TOTP_REQUESTED,
        /** TOTP module is skipped, user is authenticated. */
        TOTP_SKIPPED,
        /** Authentication was dropped, user has to start again. */
        RESTARTED
    }

    @Autowired private MidpointProviderManager authenticationManager;
    @Autowired private AuthModuleRegistryImpl authModuleRegistry;
    @Autowired private AuthChannelRegistryImpl authChannelRegistry;
    @Autowired private RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private GuiProfiledPrincipalManager principalManager;
    @Autowired private ApplicationContext applicationContext;

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);
        modelService.postInit(result);

        overwrite(new RoleType()
                .oid(ROLE_SELF_SERVICE_OID)
                .name("self-service-12478")
                .authorization(new AuthorizationType().action(AuthorizationConstants.AUTZ_UI_SELF_ALL_URL)), result);
        overwrite(globalPolicy(true), result);
        overwrite(archetypePolicy(GLOBAL_SEQUENCE, GLOBAL_SUFFIX), result);
        overwrite(new ArchetypeType()
                .oid(ARCHETYPE_OID)
                .name("internal-user-12478")
                .archetypeType(ArchetypeTypeType.STRUCTURAL)
                .securityPolicyRef(ARCHETYPE_POLICY_OID, SecurityPolicyType.COMPLEX_TYPE), result);
        overwrite(new SystemConfigurationType()
                .oid(SYSTEM_CONFIGURATION_OID)
                .name("SystemConfiguration")
                .globalSecurityPolicyRef(GLOBAL_POLICY_OID, SecurityPolicyType.COMPLEX_TYPE), result);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, true, result);
        login(repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, true, result));
        addObject(user(USER_EXTERNAL, null).asPrismObject(), initTask, result);
        addObject(user(USER_INTERNAL, ARCHETYPE_OID).asPrismObject(), initTask, result);
    }

    @AfterMethod
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /** Control case: TOTP is mandatory in the global policy. */
    @Test
    public void test090MandatoryTotpInGlobalPolicy() throws Exception {
        given("global policy with mandatory TOTP");
        OperationResult result = getTestOperationResult();
        overwrite(globalPolicy(false), result);
        try {
            when("user without archetype enters password");
            Outcome outcome = authenticateWithPassword(USER_EXTERNAL);

            then("TOTP code is requested");
            assertThat(outcome).isEqualTo(new Outcome(Step.TOTP_REQUESTED, null));
        } finally {
            overwrite(globalPolicy(true), result);
        }
    }

    @Test
    public void test100UserWithoutArchetypeSkipsTotp() throws Exception {
        given("archetype policy exists, user has no archetype");
        overwrite(archetypePolicy(GLOBAL_SEQUENCE, GLOBAL_SUFFIX), getTestOperationResult());

        when("user enters password");
        Outcome outcome = authenticateWithPassword(USER_EXTERNAL);

        then("TOTP is skipped");
        assertThat(outcome).isEqualTo(new Outcome(Step.TOTP_SKIPPED, null));
    }

    /** Archetype policy reuses the identifier and URL suffix of the global sequence and overrides the TOTP module. */
    @Test
    public void test110SameIdentifierSameSuffix() throws Exception {
        given("archetype sequence with the same identifier and URL suffix as the global one");
        overwrite(archetypePolicy(GLOBAL_SEQUENCE, GLOBAL_SUFFIX), getTestOperationResult());

        when("user with archetype enters password");
        Outcome outcome = authenticateWithPassword(USER_INTERNAL);

        then("TOTP code is requested");
        assertThat(outcome).isEqualTo(new Outcome(Step.TOTP_REQUESTED, null));
    }

    /**
     * Configuration from issue 12478, not supported. Sequence of the archetype policy takes over the requests
     * of the sequence the user has started with.
     */
    @Test
    public void test120DifferentIdentifierSameSuffix() throws Exception {
        given("archetype sequence with different identifier and the same URL suffix as the global one");
        overwrite(archetypePolicy("alternative-totp-sequence", GLOBAL_SUFFIX), getTestOperationResult());

        when("user with archetype enters password");
        Outcome outcome = authenticateWithPassword(USER_INTERNAL);

        then("authentication is dropped, generic error is prepared for the login page");
        assertThat(outcome).isEqualTo(new Outcome(Step.RESTARTED, MESSAGE_GENERIC_ERROR));
    }

    /** Not supported, archetype sequence becomes the default one of the channel. */
    @Test
    public void test130DifferentIdentifierDifferentSuffix() throws Exception {
        given("archetype sequence with different identifier and different URL suffix than the global one");
        overwrite(archetypePolicy("alternative-totp-sequence", "gui-totp-alternative"), getTestOperationResult());

        when("user with archetype enters password");
        Outcome outcome = authenticateWithPassword(USER_INTERNAL);

        then("authentication is dropped, generic error is prepared for the login page");
        assertThat(outcome).isEqualTo(new Outcome(Step.RESTARTED, MESSAGE_GENERIC_ERROR));
    }

    @Test
    public void test140SameIdentifierDifferentSuffix() throws Exception {
        given("archetype sequence with the same identifier and different URL suffix than the global one");
        overwrite(archetypePolicy(GLOBAL_SEQUENCE, "gui-totp-alternative"), getTestOperationResult());

        when("user with archetype enters password");
        Outcome outcome = authenticateWithPassword(USER_INTERNAL);

        then("TOTP code is requested");
        assertThat(outcome).isEqualTo(new Outcome(Step.TOTP_REQUESTED, null));
    }

    /**
     * Global policy is replaced while the user is logged in. The sequence keeps its identifier, but the module
     * used for the login is not defined in the new policy at all.
     * The principal learns about the new policy when its profile is refreshed (e.g. after change of system configuration).
     *
     * Modules built during the login are kept in the authentication stored in the session, {@link MidpointAuthFilter}
     * takes the filters for each request of the user from them, not from the policy.
     * They must not be replaced by the modules of the new policy, the module used for the login would be missing.
     */
    @Test
    public void test200PolicyChangedForAuthenticatedUser() throws Exception {
        given("user logged in via sequence with single login form module");
        OperationResult result = getTestOperationResult();
        overwrite(loginFormPolicy(MODULE_LOGIN_FORM), result);
        try {
            MockHttpSession session = new MockHttpSession();
            MockHttpServletRequest loginRequest = request("/login", session);
            prepareAuthentication(null, loginRequest).buildMidPointAuthentication(loginRequest);
            MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();
            verifyPassword(authentication, USER_EXTERNAL);
            ModuleAuthentication loginForm = authentication.getAuthentications().get(0);
            assertThat(authentication.isAuthenticated()).as("authenticated after password").isTrue();

            when("module of the sequence is replaced, profile of the user is refreshed and next request comes");
            overwrite(loginFormPolicy("internalLoginForm"), result);
            principalManager.refreshCompiledProfile((GuiProfiledPrincipal) authentication.getPrincipal());
            prepareAuthentication(authentication, request("/self/dashboard", session));

            then("user is still authenticated, module used for the login is available");
            assertThat(authentication.isAuthenticated()).as("authenticated after policy change").isTrue();
            assertThat(authentication.getIndexOfModule(loginForm))
                    .as("index of module used for the login")
                    .isNotEqualTo(MidpointAuthentication.NO_MODULE_FOUND_INDEX);
        } finally {
            overwrite(globalPolicy(true), result);
        }
    }

    /**
     * Two requests of the login, as {@link MidpointAuthFilter} processes them:
     *
     * . request of unknown user, authentication is started with the sequence of the global policy
     * . password is verified, the user and its security policy are known from now on
     * . following request, the authentication is prepared again, now with the policy of the user
     */
    private Outcome authenticateWithPassword(String username) {
        SecurityContextHolder.clearContext();
        MockHttpSession session = new MockHttpSession();

        MockHttpServletRequest firstRequest = request("/login", session);
        prepareAuthentication(null, firstRequest).buildMidPointAuthentication(firstRequest);
        MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();

        verifyPassword(authentication, username);

        MockHttpServletRequest nextRequest = request(
                authentication.getAuthenticationChannel().getPathDuringProccessing(), session);
        prepareAuthentication(authentication, nextRequest);

        Exception error = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        return new Outcome(nextStep(authentication), error != null ? error.getMessage() : null);
    }

    private AuthenticationWrapper prepareAuthentication(MidpointAuthentication authentication, MockHttpServletRequest request) {
        return new AuthenticationWrapper(
                authenticationManager,
                authModuleRegistry,
                sharedObjects(),
                removeUnusedSecurityFilterPublisher,
                systemObjectCache,
                modelInteractionService)
                .create(authentication, request, taskManager, authChannelRegistry);
    }

    /** Objects the modules need to build their filters, {@link MidpointAuthFilter} gets them from Spring Security. */
    private Map<Class<?>, Object> sharedObjects() {
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, applicationContext);
        sharedObjects.put(SecurityContextRepository.class, new HttpSessionSecurityContextRepository());
        sharedObjects.put(PathPatternRequestMatcher.Builder.class, PathPatternRequestMatcher.withDefaults());
        return sharedObjects;
    }

    /** Password is verified by the provider of the login form module, the module is marked as successful afterwards. */
    private void verifyPassword(MidpointAuthentication authentication, String username) {
        UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(username, PASSWORD);
        AuthenticationProvider provider = authentication.getAuthModules().get(0).getAuthenticationProviders().stream()
                .filter(p -> p.supports(UsernamePasswordAuthenticationToken.class))
                .findFirst()
                .orElseThrow();
        provider.authenticate(credentials);
        authentication.getProcessingModuleAuthentication().setState(AuthenticationModuleState.SUCCESSFULLY);
    }

    private Step nextStep(MidpointAuthentication authentication) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return Step.RESTARTED;
        }
        int index = authentication.getIndexOfProcessingModule(true);
        ModuleAuthentication module = authentication.getAuthentications().get(index);
        assertThat(module.getModuleIdentifier()).as("module to process after password").isEqualTo(MODULE_TOTP);
        return module.applicable() ? Step.TOTP_REQUESTED : Step.TOTP_SKIPPED;
    }

    /** Some of the authentication beans live in the scope of HTTP session, therefore the request has to be bound. */
    private MockHttpServletRequest request(String path, MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private void overwrite(ObjectType object, OperationResult result) throws Exception {
        repositoryService.addObject(object.asPrismObject(), RepoAddOptions.createOverwrite(), result);
        systemObjectCache.invalidateCaches();
    }

    private SecurityPolicyType globalPolicy(boolean totpAcceptEmpty) {
        return new SecurityPolicyType()
                .oid(GLOBAL_POLICY_OID)
                .name("global-12478")
                .authentication(new AuthenticationsPolicyType()
                        .modules(new AuthenticationModulesType()
                                .loginForm(new LoginFormAuthenticationModuleType().identifier(MODULE_LOGIN_FORM))
                                .totp(totpModule()))
                        .sequence(guiSequence(GLOBAL_SEQUENCE, GLOBAL_SUFFIX, totpAcceptEmpty)));
    }

    /** Global policy with password only. */
    private SecurityPolicyType loginFormPolicy(String moduleIdentifier) {
        return new SecurityPolicyType()
                .oid(GLOBAL_POLICY_OID)
                .name("global-12478")
                .authentication(new AuthenticationsPolicyType()
                        .modules(new AuthenticationModulesType()
                                .loginForm(new LoginFormAuthenticationModuleType().identifier(moduleIdentifier)))
                        .sequence(new AuthenticationSequenceType()
                                .identifier(GLOBAL_SEQUENCE)
                                .channel(new AuthenticationSequenceChannelType()
                                        ._default(true)
                                        .channelId(SchemaConstants.CHANNEL_USER_URI)
                                        .urlSuffix(GLOBAL_SUFFIX))
                                .module(new AuthenticationSequenceModuleType()
                                        .identifier(moduleIdentifier)
                                        .order(1)
                                        .necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT))));
    }

    private SecurityPolicyType archetypePolicy(String sequenceIdentifier, String urlSuffix) {
        return new SecurityPolicyType()
                .oid(ARCHETYPE_POLICY_OID)
                .name("archetype-12478")
                .authentication(new AuthenticationsPolicyType()
                        .modules(new AuthenticationModulesType().totp(totpModule()))
                        .sequence(guiSequence(sequenceIdentifier, urlSuffix, false)));
    }

    private TOtpAuthenticationModuleType totpModule() {
        return new TOtpAuthenticationModuleType()
                .identifier(MODULE_TOTP)
                .issuer("midPoint test");
    }

    private AuthenticationSequenceType guiSequence(String identifier, String urlSuffix, boolean totpAcceptEmpty) {
        return new AuthenticationSequenceType()
                .identifier(identifier)
                .channel(new AuthenticationSequenceChannelType()
                        ._default(true)
                        .channelId(SchemaConstants.CHANNEL_USER_URI)
                        .urlSuffix(urlSuffix))
                .module(new AuthenticationSequenceModuleType()
                        .identifier(MODULE_LOGIN_FORM)
                        .order(1)
                        .necessity(AuthenticationSequenceModuleNecessityType.REQUISITE))
                .module(new AuthenticationSequenceModuleType()
                        .identifier(MODULE_TOTP)
                        .order(2)
                        .necessity(AuthenticationSequenceModuleNecessityType.REQUISITE)
                        .acceptEmpty(totpAcceptEmpty));
    }

    private UserType user(String name, String archetypeOid) {
        ProtectedStringType password = new ProtectedStringType();
        password.setClearValue(PASSWORD);
        UserType user = new UserType()
                .name(name)
                .credentials(new CredentialsType().password(new PasswordType().value(password)))
                .assignment(new AssignmentType().targetRef(ROLE_SELF_SERVICE_OID, RoleType.COMPLEX_TYPE));
        if (archetypeOid != null) {
            user.assignment(new AssignmentType().targetRef(archetypeOid, ArchetypeType.COMPLEX_TYPE));
        }
        return user;
    }

    /**
     * @param error message of the authentication error stored for the login page, if there is any
     */
    private record Outcome(Step step, String error) {
    }
}
