/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import static org.testng.AssertJUnit.assertEquals;

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
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
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
            ModuleAuthenticationImpl authentication =
                    MODULE_USER_NAME.equals(identifier)
                            ? new FocusIdentificationModuleAuthenticationImpl(sequenceModule)
                            : new ModuleAuthenticationImpl(identifier, sequenceModule);

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
            if (MODULE_USER_NAME.equals(identifier)) {
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
        }

        private void unregisterFocusIdentificationPage() {
            DescriptorLoaderImpl.getLoginPages().remove(FOCUS_IDENTIFICATION_PATH);
            DescriptorLoaderImpl.getMapForAuthPages().remove(
                    AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION);
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
            SecurityPolicyType policy = instantiateObject(SecurityPolicyType.class).asObjectable();
            policy.authentication(
                    new AuthenticationsPolicyType()
                            .modules(moduleDefinitions)
                            .sequence(sequence(MODULE_USER_NAME, continuationModule)));
            return policy;
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
                                    .identifier(MODULE_LDAP));
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
