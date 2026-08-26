/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.ModuleFactory;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.authentication.impl.factory.channel.AbstractChannelFactory;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeSelectionModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.otp.OtpModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LdapAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TOtpAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests focus-identification handling when multiple authentication attempts share
 * the same HTTP session.
 *
 * Verifies that a stale focus-identification POST is processed by the
 * identification module instead of continuing with a module selected for a
 * previous authentication attempt.
 */
public class TestMidpointAuthFilterFocusIdentification extends AbstractHigherUnitTest {

    private static final String SEQUENCE_GUI_DEFAULT = "gui-default";
    private static final String MODULE_USER_NAME = "userName";
    private static final String MODULE_OIDC = "oidcAuth";
    private static final String MODULE_LDAP = "ldapAuth";
    private static final String ATTR_EXECUTED_MODULE = "executedModule";
    private static final String FOCUS_IDENTIFICATION_PATH = "/focusIdentification";
    private static final String MODULE_ARCHETYPE = "archSelect";
    private static final String MODULE_OTP = "otpAuth";
    private static final String ARCHETYPE_SELECTION_PATH = "/archetypeSelection";
    private static final String ARCHETYPE_OID = "1f30b05b-ebe2-4aa1-8930-7802c702b781";

    @BeforeClass
    public void initializeRemoveUnusedSecurityFilterPublisher() throws Exception {
        RemoveUnusedSecurityFilterPublisher publisher = new RemoveUnusedSecurityFilterPublisher();
        Field field = RemoveUnusedSecurityFilterPublisher.class.getDeclaredField("applicationEventPublisher");
        field.setAccessible(true);
        field.set(publisher, (ApplicationEventPublisher) event -> { });
        publisher.afterConstruct();
    }

    @DataProvider
    public Object[][] staleFocusIdentificationScenarios() {
        return new Object[][] {
                { "external-user", MODULE_LDAP, "internal-user" },
                { "internal-user", MODULE_OIDC, "external-user" },
        };
    }

    @Test(dataProvider = "staleFocusIdentificationScenarios")
    public void testStaleFocusIdentificationPostDoesNotContinuePreviousSessionAuthentication(
            String existingUser, String existingContinuation, String submittedUser) throws Exception {

        AuthenticationFlowFixture fixture = new AuthenticationFlowFixture();

        given("partially advanced authentication stored in a shared HTTP session");
        fixture.storePartiallyAdvancedAuthentication(existingUser, existingContinuation);

        and("a stale focus-identification form submission from another browser flow");
        MockHttpServletRequest request = fixture.staleFocusIdentificationSubmitRequest(submittedUser);

        when("the request is processed through MidpointAuthFilter");
        fixture.runMidpointAuthFilter(request);

        then("the request is routed back through focus identification");
        assertEquals(
                "Stale focusIdentification POST must be handled by the identification module, "
                        + "not by the previous flow continuation",
                MODULE_USER_NAME,
                request.getAttribute(ATTR_EXECUTED_MODULE));
    }

    /**
     * Real login, archetype policy defines only the continuation module:
     *
     * - merged policy sequence lists the inherited identification module (order 10) after the
     *   continuation module (order 30), the document order is not the execution order
     * - a stale identification submit must keep the identification module in the flow
     *
     * Fails with the current implementation: the reset keeps the first listed module (the
     * continuation one), the flow then holds a sequence naming a module that never runs, so it can
     * never be evaluated as finished and SequenceAuditFilter writes no audit record.
     */
    @Test(dataProvider = "staleFocusIdentificationScenarios")
    public void testStaleFocusIdentificationPostKeepsExecutedModuleOfMergedSequence(
            String existingUser, String existingContinuation, String submittedUser) throws Exception {

        AuthenticationFlowFixture fixture = new AuthenticationFlowFixture();

        given("partially advanced authentication using a merged policy sequence");
        fixture.storePartiallyAdvancedAuthenticationWithPolicySequence(
                existingUser, fixture.mergedSequence(existingContinuation));

        and("a stale focus-identification form submission from another browser flow");
        MockHttpServletRequest request = fixture.staleFocusIdentificationSubmitRequest(submittedUser);

        when("the request is processed through MidpointAuthFilter");
        fixture.runMidpointAuthFilter(request);

        then("the request is routed back through focus identification");
        assertEquals("Wrong module processed the request",
                MODULE_USER_NAME, request.getAttribute(ATTR_EXECUTED_MODULE));

        and("the flow keeps the executed identification module, not the first listed one");
        assertEquals("Wrong modules in sequence",
                Collections.singletonList(MODULE_USER_NAME), fixture.resultingSequenceModules());
    }

    /**
     * Real login, sequence: archetype selection, focus identification, LDAP:
     *
     * - user selects an archetype, is identified and waits for the LDAP step
     * - the archetype selection form is submitted again in the same session
     * - the flow has to start over with the archetype selection
     *
     * Fails with the current implementation: the reset only handles a focus-identification module
     * at the first position, the stale submit is processed by a module of the previous attempt.
     */
    @Test
    public void testStaleArchetypeSelectionPostRestartsArchetypeSelection() throws Exception {
        AuthenticationFlowFixture fixture = new AuthenticationFlowFixture();

        given("authentication with a selected archetype and an identified user");
        fixture.storeArchetypeFlowAuthentication(
                "external-user", MODULE_LDAP, AuthenticationModuleState.LOGIN_PROCESSING);

        and("a stale archetype-selection form submission from another browser flow");
        MockHttpServletRequest request = fixture.staleSubmitRequest(ARCHETYPE_SELECTION_PATH, "internal-user");

        when("the request is processed through MidpointAuthFilter");
        fixture.runMidpointAuthFilter(request);

        then("the request is routed back through archetype selection");
        assertEquals("Wrong module processed the request",
                MODULE_ARCHETYPE, request.getAttribute(ATTR_EXECUTED_MODULE));
    }

    /**
     * Real login, sequence: archetype selection, focus identification, LDAP:
     *
     * - user selects an archetype, is identified and waits for the LDAP step
     * - the identification form is submitted again in the same session
     * - the user has to be identified again within the already selected archetype
     *
     * Fails with the current implementation: the reset only inspects the first module of the
     * sequence, an identification module at a later position is not recognized.
     */
    @Test
    public void testStaleFocusIdentificationPostAfterArchetypeSelectionRunsIdentification() throws Exception {
        AuthenticationFlowFixture fixture = new AuthenticationFlowFixture();

        given("authentication with a selected archetype and an identified user");
        fixture.storeArchetypeFlowAuthentication(
                "external-user", MODULE_LDAP, AuthenticationModuleState.LOGIN_PROCESSING);

        and("a stale focus-identification form submission from another browser flow");
        MockHttpServletRequest request = fixture.staleSubmitRequest(FOCUS_IDENTIFICATION_PATH, "internal-user");

        when("the request is processed through MidpointAuthFilter");
        fixture.runMidpointAuthFilter(request);

        then("the request is routed back through focus identification");
        assertEquals("Wrong module processed the request",
                MODULE_USER_NAME, request.getAttribute(ATTR_EXECUTED_MODULE));
    }

    /**
     * Real login, sequence: archetype selection, focus identification, TOTP with acceptEmpty:
     *
     * - user A without TOTP credentials is identified, the OTP module is called off
     * - the identification form is submitted again in the same session by user B with TOTP enrolled
     * - the called-off decision belongs to user A and must be dropped together with the rest of the
     *   stale state, so the OTP applicability is evaluated again for user B
     *
     * Fails with the current implementation: the stale submit is not recognized (identification is
     * not the first module), the called-off second factor decision of user A stays in the session.
     */
    @Test
    public void testStaleFocusIdentificationPostDropsOtpDecisionOfPreviousAttempt() throws Exception {
        AuthenticationFlowFixture fixture = new AuthenticationFlowFixture();

        given("authentication where the OTP module was called off for the identified user");
        fixture.storeArchetypeFlowAuthentication(
                "user-without-totp", MODULE_OTP, AuthenticationModuleState.CALLED_OFF);

        and("a stale focus-identification form submission of a user with TOTP enrolled");
        MockHttpServletRequest request = fixture.staleSubmitRequest(FOCUS_IDENTIFICATION_PATH, "user-with-totp");

        when("the request is processed through MidpointAuthFilter");
        fixture.runMidpointAuthFilter(request);

        then("the request is routed back through focus identification");
        assertEquals("Wrong module processed the request",
                MODULE_USER_NAME, request.getAttribute(ATTR_EXECUTED_MODULE));

        and("the called-off OTP decision of the previous user is dropped");
        assertTrue("Called-off OTP decision of the previous attempt is still present",
                fixture.resultingAuthentication().getAuthentications().stream()
                        .noneMatch(module -> AuthenticationModuleState.CALLED_OFF == module.getState()));
    }

    private static class FakeModuleFactory implements ModuleFactory<AbstractAuthenticationModuleType, ModuleAuthentication> {

        private final AuthenticationModulesType moduleDefinitions;

        private FakeModuleFactory(AuthenticationModulesType moduleDefinitions) {
            this.moduleDefinitions = moduleDefinitions;
        }

        @Override
        public AuthModule<ModuleAuthentication> createAuthModule(
                AbstractAuthenticationModuleType moduleType,
                String sequenceSuffix,
                ServletRequest request,
                Map<Class<?>, Object> sharedObjects,
                AuthenticationModulesType authenticationsPolicy,
                CredentialsPolicyType credentialPolicy,
                AuthenticationChannel authenticationChannel,
                AuthenticationSequenceModuleType sequenceModule) {

            return createAuthModule(moduleType, sequenceModule);
        }

        @Override
        public boolean match(
                AbstractAuthenticationModuleType module,
                AuthenticationChannel authenticationChannel) {
            return true;
        }

        @Override
        public Integer getOrder() {
            return 0;
        }

        private List<AuthModule<?>> createAuthModules(
                AuthenticationSequenceType sequence) {

            List<AuthModule<?>> modules = new ArrayList<>();
            for (AuthenticationSequenceModuleType module : sequence.getModule()) {
                modules.add(createAuthModule(moduleDefinition(module.getIdentifier()), module));
            }
            return modules;
        }

        private AuthModule<ModuleAuthentication> createAuthModule(
                AbstractAuthenticationModuleType moduleType,
                AuthenticationSequenceModuleType sequenceModule) {

            String identifier = moduleType.getIdentifier();
            ModuleAuthenticationImpl authentication;
            if (MODULE_USER_NAME.equals(identifier)) {
                authentication = new FocusIdentificationModuleAuthenticationImpl(sequenceModule);
            } else if (MODULE_ARCHETYPE.equals(identifier)) {
                authentication = new ArchetypeSelectionModuleAuthenticationImpl(sequenceModule);
            } else if (MODULE_OTP.equals(identifier)) {
                authentication = new OtpModuleAuthentication(sequenceModule);
            } else {
                authentication = new ModuleAuthenticationImpl(identifier, sequenceModule);
            }

            authentication.setNameOfModule(identifier);

            SecurityFilterChain filterChain =
                    new DefaultSecurityFilterChain(
                            request -> true,
                            (Filter) (request, response, chain) ->
                                    request.setAttribute(ATTR_EXECUTED_MODULE, identifier));

            //noinspection unchecked
            return AuthModuleImpl.build(
                    filterChain,
                    LoginFormModuleWebSecurityConfiguration.build(
                            moduleType, SEQUENCE_GUI_DEFAULT),
                    authentication);
        }

        private AbstractAuthenticationModuleType moduleDefinition(String identifier) {
            if (MODULE_ARCHETYPE.equals(identifier)) {
                return moduleDefinitions.getArchetypeSelection().get(0);
            } else if (MODULE_OTP.equals(identifier)) {
                return moduleDefinitions.getTotp().get(0);
            } else if (MODULE_USER_NAME.equals(identifier)) {
                return moduleDefinitions.getFocusIdentification().get(0);
            } else if (MODULE_OIDC.equals(identifier)) {
                return moduleDefinitions.getOidc().get(0);
            } else if (MODULE_LDAP.equals(identifier)) {
                return moduleDefinitions.getLdap().get(0);
            }
            throw new IllegalArgumentException("Unsupported module identifier: " + identifier);
        }
    }

    private class AuthenticationFlowFixture {

        private final MockHttpSession session = new MockHttpSession();
        private final SecurityContextRepository contextRepository =
                new HttpSessionSecurityContextRepository();
        private final AuthenticationModulesType moduleDefinitions = moduleDefinitions();
        private final FakeModuleFactory moduleFactory = new FakeModuleFactory(moduleDefinitions);

        private MidpointAuthentication resultingAuthentication;

        MidpointAuthentication resultingAuthentication() {
            return resultingAuthentication;
        }

        List<String> resultingSequenceModules() {
            return resultingAuthentication.getSequence().getModule().stream()
                    .map(AuthenticationSequenceModuleType::getIdentifier)
                    .toList();
        }

        /** Stores an authentication whose policy sequence lists the modules as a merged policy does. */
        void storePartiallyAdvancedAuthenticationWithPolicySequence(
                String username, AuthenticationSequenceType policySequence) throws Exception {

            MockHttpServletRequest request = loginProcessingRequest();
            request.setSession(session);

            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(
                    partiallyAdvancedAuthentication(username, policyWithSequence(policySequence)));

            contextRepository.saveContext(context, request, new MockHttpServletResponse());
        }

        /**
         * Stores an authentication of a flow that selected an archetype, identified the user and
         * waits for the third module. State of the third module is configurable, e.g. CALLED_OFF
         * for an OTP module not applicable to the identified user.
         */
        void storeArchetypeFlowAuthentication(
                String username, String thirdModule, AuthenticationModuleState thirdModuleState) throws Exception {

            MockHttpServletRequest request = loginProcessingRequest();
            request.setSession(session);

            AuthenticationSequenceType sequence = archetypeSequence(thirdModule);
            MidpointAuthentication authentication = new MidpointAuthentication(sequence);
            authentication.setAuthModules(moduleFactory.createAuthModules(sequence));
            authentication.setPrincipal(principal(username, policyWithSequence(archetypeSequence(thirdModule))));
            authentication.setArchetypeOid(ARCHETYPE_OID);
            authentication.setArchetypeSelected(true);

            for (int i = 0; i < authentication.getAuthModules().size(); i++) {
                ModuleAuthentication moduleAuthentication =
                        authentication.getAuthModules().get(i).getBaseModuleAuthentication();
                moduleAuthentication.setState(i < 2 ? AuthenticationModuleState.SUCCESSFULLY : thirdModuleState);
                authentication.addAuthentication(moduleAuthentication);
            }

            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(authentication);
            contextRepository.saveContext(context, request, new MockHttpServletResponse());
        }

        MockHttpServletRequest staleSubmitRequest(String page, String username) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", page);
            request.setServletPath(page);
            request.setSession(session);
            request.addParameter("username", username);
            return request;
        }

        void storePartiallyAdvancedAuthentication(
                String username, String continuationModule) throws Exception {

            MockHttpServletRequest request = loginProcessingRequest();
            request.setSession(session);

            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(
                    partiallyAdvancedAuthentication(
                            username, securityPolicy(continuationModule)));

            contextRepository.saveContext(
                    context,
                    request,
                    new MockHttpServletResponse());
        }

        MockHttpServletRequest staleFocusIdentificationSubmitRequest(String username) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", FOCUS_IDENTIFICATION_PATH);
            request.setServletPath(FOCUS_IDENTIFICATION_PATH);
            request.setSession(session);
            request.addParameter("username", username);
            return request;
        }

        void runMidpointAuthFilter(MockHttpServletRequest request) throws Exception {
            SecurityContextHolder.clearContext();
            registerFocusIdentificationPage();
            try {
                SecurityContextHolder.setContext(
                        contextRepository.loadDeferredContext(request).get());

                midpointAuthFilter().doFilter(
                        request,
                        new MockHttpServletResponse(),
                        new MockFilterChain());

                resultingAuthentication =
                        SecurityContextHolder.getContext().getAuthentication() instanceof MidpointAuthentication result
                                ? result : null;
            } finally {
                unregisterFocusIdentificationPage();
                SecurityContextHolder.clearContext();
            }
        }

        private MidpointAuthFilter midpointAuthFilter() throws Exception {
            Map<Class<?>, Object> sharedObjects = new HashMap<>();
            sharedObjects.put(SecurityContextRepository.class, contextRepository);

            MidpointAuthFilter filter = new MidpointAuthFilter(sharedObjects);
            setField(filter, "authModuleRegistry", moduleRegistry());
            setField(filter, "authChannelRegistry", authChannelRegistry());
            setField(filter, "authenticationManager", new MidpointProviderManager());
            setField(filter, "taskManager", taskManager());
            setField(
                    filter,
                    "removeUnusedSecurityFilterPublisher",
                    RemoveUnusedSecurityFilterPublisher.get());
            return filter;
        }

        private void registerFocusIdentificationPage() {
            DescriptorLoaderImpl.getLoginPages().add(FOCUS_IDENTIFICATION_PATH);
            DescriptorLoaderImpl.getMapForAuthPages().put(
                    AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION,
                    Collections.singletonList(FOCUS_IDENTIFICATION_PATH));
            DescriptorLoaderImpl.getLoginPages().add(ARCHETYPE_SELECTION_PATH);
            DescriptorLoaderImpl.getMapForAuthPages().put(
                    AuthenticationModuleNameConstants.ARCHETYPE_SELECTION,
                    Collections.singletonList(ARCHETYPE_SELECTION_PATH));
        }

        private void unregisterFocusIdentificationPage() {
            DescriptorLoaderImpl.getLoginPages().remove(FOCUS_IDENTIFICATION_PATH);
            DescriptorLoaderImpl.getMapForAuthPages().remove(
                    AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION);
            DescriptorLoaderImpl.getLoginPages().remove(ARCHETYPE_SELECTION_PATH);
            DescriptorLoaderImpl.getMapForAuthPages().remove(
                    AuthenticationModuleNameConstants.ARCHETYPE_SELECTION);
        }

        private void setField(Object target, String fieldName, Object value) throws Exception {
            Field field = MidpointAuthFilter.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }

        private MidpointAuthentication partiallyAdvancedAuthentication(
                String username, SecurityPolicyType securityPolicy) {

            AuthenticationSequenceType sequence = sequence(MODULE_USER_NAME);
            MidpointAuthentication authentication = new MidpointAuthentication(sequence);
            authentication.setAuthModules(moduleFactory.createAuthModules(sequence));
            authentication.setPrincipal(principal(username, securityPolicy));

            ModuleAuthentication userNameAuthentication =
                    authentication.getAuthModules().get(0).getBaseModuleAuthentication();
            userNameAuthentication.setState(AuthenticationModuleState.SUCCESSFULLY);
            authentication.addAuthentication(userNameAuthentication);

            return authentication;
        }

        private SecurityPolicyType securityPolicy(String continuationModule) throws Exception {
            return policyWithSequence(sequence(MODULE_USER_NAME, continuationModule));
        }

        private SecurityPolicyType policyWithSequence(AuthenticationSequenceType sequence) throws Exception {
            SecurityPolicyType policy = instantiateObject(SecurityPolicyType.class).asObjectable();
            policy.authentication(
                    new AuthenticationsPolicyType()
                            .modules(moduleDefinitions)
                            .sequence(sequence));
            return policy;
        }

        /**
         * Sequence as produced by merging an archetype policy defining only the continuation module
         * with the global policy defining the identification module: the inherited identification
         * module is appended, so the document order is the reverse of the execution order.
         */
        private AuthenticationSequenceType mergedSequence(String continuationModule) {
            AuthenticationSequenceType sequence = sequence();
            sequence.module(module(continuationModule, 30, AuthenticationSequenceModuleNecessityType.SUFFICIENT));
            sequence.module(module(MODULE_USER_NAME, 10, AuthenticationSequenceModuleNecessityType.REQUIRED));
            return sequence;
        }

        /** Sequence selecting an archetype first, identifying the user second, authenticating last. */
        private AuthenticationSequenceType archetypeSequence(String thirdModule) {
            AuthenticationSequenceType sequence = sequence();
            sequence.module(module(MODULE_ARCHETYPE, 10, AuthenticationSequenceModuleNecessityType.REQUISITE));
            sequence.module(module(MODULE_USER_NAME, 20, AuthenticationSequenceModuleNecessityType.REQUISITE));
            sequence.module(module(thirdModule, 30, AuthenticationSequenceModuleNecessityType.SUFFICIENT));
            return sequence;
        }

        private AuthenticationSequenceModuleType module(
                String identifier, int order, AuthenticationSequenceModuleNecessityType necessity) {
            return new AuthenticationSequenceModuleType()
                    .identifier(identifier)
                    .order(order)
                    .necessity(necessity);
        }

        private AuthenticationModulesType moduleDefinitions() {
            return new AuthenticationModulesType()
                    .focusIdentification(
                            new FocusIdentificationAuthenticationModuleType()
                                    .identifier(MODULE_USER_NAME))
                    .oidc(
                            new OidcAuthenticationModuleType()
                                    .identifier(MODULE_OIDC))
                    .ldap(
                            new LdapAuthenticationModuleType()
                                    .identifier(MODULE_LDAP))
                    .archetypeSelection(
                            new ArchetypeSelectionModuleType()
                                    .identifier(MODULE_ARCHETYPE))
                    .totp(
                            new TOtpAuthenticationModuleType()
                                    .identifier(MODULE_OTP));
        }

        private GuiProfiledPrincipal principal(String name, SecurityPolicyType securityPolicy) {
            GuiProfiledPrincipal principal =
                    new GuiProfiledPrincipal(
                            new UserType()
                                    .oid(name + "-oid")
                                    .name(name));
            principal.setApplicableSecurityPolicy(securityPolicy);
            return principal;
        }

        private MockHttpServletRequest loginProcessingRequest() {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", "/auth/" + SEQUENCE_GUI_DEFAULT);
            request.setServletPath("/auth/" + SEQUENCE_GUI_DEFAULT);
            return request;
        }

        private AuthModuleRegistryImpl moduleRegistry() {
            AuthModuleRegistryImpl registry = new AuthModuleRegistryImpl();
            registry.addToRegistry(moduleFactory);
            return registry;
        }

        private TaskManager taskManager() {
            return (TaskManager) java.lang.reflect.Proxy.newProxyInstance(
                    TaskManager.class.getClassLoader(),
                    new Class<?>[] { TaskManager.class },
                    (proxy, method, args) -> {
                        if ("getLocalNodeGroups".equals(method.getName())) {
                            return Collections.<ObjectReferenceType>emptyList();
                        }
                        if (method.getReturnType().equals(boolean.class)) {
                            return false;
                        }
                        if (method.getReturnType().equals(int.class)) {
                            return 0;
                        }
                        return null;
                    });
        }

        private AuthChannelRegistryImpl authChannelRegistry() {
            AuthChannelRegistryImpl registry = new AuthChannelRegistryImpl();
            registry.addToRegistry(new AbstractChannelFactory() {

                @Override
                public boolean match(String channelId) {
                    return SchemaConstants.CHANNEL_USER_URI.equals(channelId);
                }

                @Override
                public AuthenticationChannel createAuthChannel(
                        AuthenticationSequenceChannelType channel) {
                    return new AuthenticationChannelImpl(channel);
                }
            });
            return registry;
        }

        private AuthenticationSequenceType sequence(String... modules) {
            AuthenticationSequenceType sequence =
                    new AuthenticationSequenceType()
                            .identifier(SEQUENCE_GUI_DEFAULT)
                            .channel(
                                    new AuthenticationSequenceChannelType()
                                            .channelId(SchemaConstants.CHANNEL_USER_URI)
                                            .urlSuffix(SEQUENCE_GUI_DEFAULT)
                                            ._default(true));

            for (int i = 0; i < modules.length; i++) {
                sequence.module(
                        new AuthenticationSequenceModuleType()
                                .identifier(modules[i])
                                .order(i == 0 ? 10 : 30)
                                .necessity(
                                        i == 0
                                                ? AuthenticationSequenceModuleNecessityType.REQUISITE
                                                : AuthenticationSequenceModuleNecessityType.SUFFICIENT));
            }

            return sequence;
        }
    }
}
