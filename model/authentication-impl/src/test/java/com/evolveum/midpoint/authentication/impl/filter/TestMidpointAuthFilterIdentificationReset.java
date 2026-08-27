/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ArchetypeSelectionModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.otp.OtpModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 *
 * Tests the re-entry into identification (focus identification, archetype selection) when its form
 * is submitted while the HTTP session still holds a partially advanced authentication.
 *
 * Exercises {@link MidpointAuthFilter#resetToIdentificationModuleIfRequested} directly, the
 * inputs are the values the filter reads from the authentication wrapper.
 */
public class TestMidpointAuthFilterIdentificationReset extends AbstractHigherUnitTest {

    private static final String SEQUENCE_GUI_DEFAULT = "gui-default";
    private static final String MODULE_USER_NAME = "userName";
    private static final String MODULE_OIDC = "oidcAuth";
    private static final String MODULE_LDAP = "ldapAuth";
    private static final String MODULE_ARCHETYPE = "archSelect";
    private static final String FOCUS_IDENTIFICATION_PATH = "/focusIdentification";
    private static final String ARCHETYPE_SELECTION_PATH = "/archetypeSelection";
    private static final String ARCHETYPE_OID = "1f30b05b-ebe2-4aa1-8930-7802c702b781";
    private static final String MODULE_OTP = "otpAuth";
    private static final String OTP_PATH = "/otp";

    private static final int NO_MODULE = MidpointAuthentication.NO_MODULE_FOUND_INDEX;

    /** Replacing the auth modules of a flow fires a filter cleanup event through this static singleton. */
    @BeforeClass
    public void initializeSecurityFilterPublisher() {
        RemoveUnusedSecurityFilterPublisher publisher = new RemoveUnusedSecurityFilterPublisher();
        ReflectionTestUtils.setField(
                publisher, "applicationEventPublisher", (ApplicationEventPublisher) event -> {
                });
        publisher.afterConstruct();
    }

    @BeforeMethod
    public void registerIdentificationPages() {
        DescriptorLoaderImpl.getLoginPages().add(FOCUS_IDENTIFICATION_PATH);
        DescriptorLoaderImpl.getLoginPages().add(ARCHETYPE_SELECTION_PATH);
        DescriptorLoaderImpl.getMapForAuthPages().put(
                AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION,
                Collections.singletonList(FOCUS_IDENTIFICATION_PATH));
        DescriptorLoaderImpl.getMapForAuthPages().put(
                AuthenticationModuleNameConstants.ARCHETYPE_SELECTION,
                Collections.singletonList(ARCHETYPE_SELECTION_PATH));
        DescriptorLoaderImpl.getLoginPages().add(OTP_PATH);
        DescriptorLoaderImpl.getMapForAuthPages().put(
                AuthenticationModuleNameConstants.OTP,
                Collections.singletonList(OTP_PATH));
    }

    @AfterMethod
    public void unregisterIdentificationPages() {
        DescriptorLoaderImpl.getLoginPages().remove(FOCUS_IDENTIFICATION_PATH);
        DescriptorLoaderImpl.getLoginPages().remove(ARCHETYPE_SELECTION_PATH);
        DescriptorLoaderImpl.getMapForAuthPages().remove(AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION);
        DescriptorLoaderImpl.getMapForAuthPages().remove(AuthenticationModuleNameConstants.ARCHETYPE_SELECTION);
        DescriptorLoaderImpl.getLoginPages().remove(OTP_PATH);
        DescriptorLoaderImpl.getMapForAuthPages().remove(AuthenticationModuleNameConstants.OTP);
    }

    /**
     * Real login:
     *
     * - user submits a username, midPoint identifies the focus and asks for the OIDC or LDAP step
     * - the identification form is submitted again in the same session (back button, second tab, another user)
     * - the new submit has to be identified again, it must not continue with the step chosen for the previous user
     */
    @Test
    public void testStaleIdentificationSubmitRestartsIdentification() {
        given("session holding an authentication already advanced to the continuation module");
        AuthenticationSequenceType sequence = sequence(MODULE_USER_NAME, MODULE_LDAP);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);

        when("the identification form is submitted again");
        int moduleIndex = reset(authentication, authModules, sequence, FOCUS_IDENTIFICATION_PATH);

        then("the flow continues with the identification module");
        assertEquals("Identification module has to be processed", 0, moduleIndex);
        assertEquals("Wrong modules in sequence", List.of(MODULE_USER_NAME), sequenceModules(authentication));
        assertEquals("Wrong number of authentication modules", 1, authentication.getAuthModules().size());

        and("the state of the previous attempt is dropped");
        assertNull("Focus of the previous attempt is still set", authentication.getPrincipal());
        assertEquals("Wrong number of processed modules", 1, authentication.getAuthentications().size());
    }

    /**
     * Real login, archetype with its own security policy:
     *
     * - archetype policy defines only the OIDC or LDAP step, the identification module is inherited from
     * the global policy, therefore the merged sequence lists it as the second one
     * - the identification module is still executed first, its order (10) is lower than the inherited one (30)
     * - a stale identification submit must keep this executed module, not the one listed first
     */
    @Test
    public void testStaleIdentificationSubmitKeepsExecutedModuleOfMergedSequence() {
        given("merged sequence that lists the continuation module before the identification module");
        AuthenticationSequenceType sequence = mergedSequence(MODULE_OIDC);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);

        when("the identification form is submitted again");
        int moduleIndex = reset(authentication, authModules, sequence, FOCUS_IDENTIFICATION_PATH);

        then("the executed identification module is kept, not the one listed first");
        assertEquals("Identification module has to be processed", 0, moduleIndex);
        assertEquals("Wrong modules in sequence", List.of(MODULE_USER_NAME), sequenceModules(authentication));

        and("the flow can be evaluated once identification succeeds");
        authentication.getAuthentications().forEach(module -> module.setState(AuthenticationModuleState.SUCCESSFULLY));
        assertTrue("Sequence naming a module outside the flow is never finished, which suppresses the audit record",
                authentication.isFinished());
    }

    /**
     * Real login, sequence starting with archetype selection:
     *
     * - user selects an archetype, is identified and asked for the following step
     * - the archetype selection form is submitted again in the same session
     * - the whole flow has to start over, including the state of the previously selected archetype
     */
    @Test
    public void testStaleArchetypeSelectionSubmitRestartsFlow() {
        given("session holding an authentication with a selected archetype and an identified user");
        AuthenticationSequenceType sequence = archetypeSequence();
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);
        authentication.setArchetypeOid(ARCHETYPE_OID);
        authentication.setArchetypeSelected(true);

        when("the archetype selection form is submitted again");
        int moduleIndex = reset(authentication, authModules, sequence, ARCHETYPE_SELECTION_PATH);

        then("the flow starts over with the archetype selection module");
        assertEquals("Archetype selection module has to be processed", 0, moduleIndex);
        assertEquals("Wrong modules in sequence", List.of(MODULE_ARCHETYPE), sequenceModules(authentication));
        assertEquals("Wrong number of processed modules", 1, authentication.getAuthentications().size());

        and("the state of the previous attempt is dropped");
        assertNull("Focus of the previous attempt is still set", authentication.getPrincipal());
        assertFalse("Archetype of the previous attempt is still selected", authentication.isArchetypeDefined());
    }

    /**
     * Real login, sequence starting with archetype selection:
     *
     * - user selects an archetype, is identified and asked for the following step
     * - the identification form is submitted again in the same session
     * - the user is identified again within the already selected archetype, the selection is kept
     */
    @Test
    public void testStaleIdentificationSubmitAfterArchetypeSelectionKeepsArchetype() {
        given("session holding an authentication with a selected archetype and an identified user");
        AuthenticationSequenceType sequence = archetypeSequence();
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);
        authentication.setArchetypeOid(ARCHETYPE_OID);
        authentication.setArchetypeSelected(true);

        when("the identification form is submitted again");
        int moduleIndex = reset(authentication, authModules, sequence, FOCUS_IDENTIFICATION_PATH);

        then("the flow continues with the identification module");
        assertEquals("Identification module has to be processed", 1, moduleIndex);
        assertEquals("Wrong modules in sequence",
                List.of(MODULE_ARCHETYPE, MODULE_USER_NAME), sequenceModules(authentication));

        and("the archetype selection is kept, the identification of the previous attempt is dropped");
        assertEquals("Selected archetype has to be kept", ARCHETYPE_OID, authentication.getArchetypeOid());
        assertEquals("State of the archetype selection module has to be kept",
                AuthenticationModuleState.SUCCESSFULLY, authentication.getAuthentications().get(0).getState());
        assertNull("Focus of the previous attempt is still set", authentication.getPrincipal());
        assertEquals("Wrong number of processed modules", 2, authentication.getAuthentications().size());
    }

    /**
     * Real login, sequence with a TOTP second factor:
     *
     * - user A without TOTP credentials is identified, the OTP module is called off (acceptEmpty)
     * - the identification form is submitted again in the same session by user B who has TOTP enrolled
     * - the called-off decision belongs to user A and must be dropped, so the OTP module
     * applicability is evaluated again for user B
     */
    @Test
    public void testStaleIdentificationSubmitDropsOtpDecisionOfPreviousUser() {
        given("session where the OTP module was called off for the identified user");
        AuthenticationSequenceType sequence = sequence(MODULE_USER_NAME, MODULE_OTP);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);
        authentication.getAuthentications().get(1).setState(AuthenticationModuleState.CALLED_OFF);

        when("the identification form is submitted again");
        int moduleIndex = reset(authentication, authModules, sequence, FOCUS_IDENTIFICATION_PATH);

        then("the flow continues with the identification module");
        assertEquals("Identification module has to be processed", 0, moduleIndex);
        assertEquals("Wrong modules in sequence", List.of(MODULE_USER_NAME), sequenceModules(authentication));

        and("the called-off OTP decision of the previous user is dropped");
        assertEquals("Wrong number of processed modules", 1, authentication.getAuthentications().size());
        assertNull("Focus of the previous attempt is still set", authentication.getPrincipal());
    }

    /**
     * Real login, sequence with a TOTP second factor:
     *
     * - user is identified and submits the TOTP code of the following OTP step
     * - this submit belongs to the OTP module, a credential module never resets the flow
     */
    @Test
    public void testOtpSubmitIsNotReset() {
        given("session holding an authentication advanced to the OTP module");
        AuthenticationSequenceType sequence = sequence(MODULE_USER_NAME, MODULE_OTP);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);

        when("the TOTP code is submitted");
        int moduleIndex = reset(authentication, authModules, sequence, OTP_PATH);

        then("the flow is left untouched");
        assertEquals("Flow must not be reset", NO_MODULE, moduleIndex);
        assertEquals("Sequence must not be trimmed", 2, authentication.getSequence().getModule().size());
        assertEquals("State of the identification module has to be kept",
                AuthenticationModuleState.SUCCESSFULLY, authentication.getAuthentications().get(0).getState());
    }

    /**
     * Real login:
     *
     * - user has been identified and submits the password of the following LDAP step
     * - this submit belongs to the LDAP module, identification must not be started again
     */
    @Test
    public void testContinuationModuleSubmitIsNotReset() {
        given("sequence whose first executed module is not the identification one");
        AuthenticationSequenceType sequence = sequence(MODULE_LDAP, MODULE_OIDC);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);

        when("the form is submitted");
        int moduleIndex = reset(authentication, authModules, sequence, FOCUS_IDENTIFICATION_PATH);

        then("the flow is left untouched");
        assertEquals("Flow must not be reset", NO_MODULE, moduleIndex);
        assertEquals("Sequence must not be trimmed", 2, authentication.getSequence().getModule().size());
    }

    /**
     * Real login:
     *
     * - user only opens the identification page, nothing is submitted yet
     * - rendering the page must not restart the flow stored in the session
     */
    @Test
    public void testIdentificationPageRenderingIsNotReset() {
        given("authentication advanced to the continuation module");
        AuthenticationSequenceType sequence = sequence(MODULE_USER_NAME, MODULE_LDAP);
        List<AuthModule<?>> authModules = authModules(sequence);
        MidpointAuthentication authentication = advancedAuthentication(sequence, authModules);

        when("the identification page is only displayed");
        int moduleIndex = new MidpointAuthFilter(Map.of())
                .resetToIdentificationModuleIfRequested(
                        authentication, authModules, sequence, request("GET", FOCUS_IDENTIFICATION_PATH));

        then("the flow is left untouched");
        assertEquals("Flow must not be reset", NO_MODULE, moduleIndex);
        assertEquals("Sequence must not be trimmed", 2, authentication.getSequence().getModule().size());
    }

    private int reset(MidpointAuthentication authentication, List<AuthModule<?>> authModules,
            AuthenticationSequenceType sequence, String submittedPage) {
        return new MidpointAuthFilter(Map.of())
                .resetToIdentificationModuleIfRequested(authentication, authModules, sequence, request("POST", submittedPage));
    }

    private MockHttpServletRequest request(String method, String page) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, page);
        request.setServletPath(page);
        return request;
    }

    /** Authentication of a user who has already passed identification and waits for the following module. */
    private MidpointAuthentication advancedAuthentication(
            AuthenticationSequenceType sequence, List<AuthModule<?>> authModules) {
        MidpointAuthentication authentication = new MidpointAuthentication(sequence);
        authentication.setAuthModules(authModules);
        authentication.setPrincipal(new GuiProfiledPrincipal(new UserType().oid("user-oid").name("user")));

        authModules.forEach(module -> authentication.addAuthentication(module.getBaseModuleAuthentication()));
        for (int i = 0; i < authModules.size() - 1; i++) {
            authentication.getAuthentications().get(i).setState(AuthenticationModuleState.SUCCESSFULLY);
        }
        return authentication;
    }

    private List<String> sequenceModules(MidpointAuthentication authentication) {
        return authentication.getSequence().getModule().stream()
                .map(AuthenticationSequenceModuleType::getIdentifier)
                .toList();
    }

    /** Modules as the filter builds them, i.e. sorted by the order in which they are executed. */
    private List<AuthModule<?>> authModules(AuthenticationSequenceType sequence) {
        List<AuthenticationSequenceModuleType> sortedModules = new ArrayList<>(sequence.getModule());
        sortedModules.sort((m1, m2) -> Integer.compare(m1.getOrder(), m2.getOrder()));

        List<AuthModule<?>> authModules = new ArrayList<>();
        sortedModules.forEach(module -> authModules.add(authModule(module)));
        return authModules;
    }

    private AuthModule<?> authModule(AuthenticationSequenceModuleType sequenceModule) {
        String identifier = sequenceModule.getIdentifier();
        ModuleAuthenticationImpl moduleAuthentication;
        if (MODULE_USER_NAME.equals(identifier)) {
            moduleAuthentication = new FocusIdentificationModuleAuthenticationImpl(sequenceModule);
        } else if (MODULE_ARCHETYPE.equals(identifier)) {
            moduleAuthentication = new ArchetypeSelectionModuleAuthenticationImpl(sequenceModule);
        } else if (MODULE_OTP.equals(identifier)) {
            moduleAuthentication = new OtpModuleAuthentication(sequenceModule);
        } else {
            moduleAuthentication = new ModuleAuthenticationImpl(identifier, sequenceModule);
        }
        moduleAuthentication.setNameOfModule(identifier);

        return AuthModuleImpl.build(
                new DefaultSecurityFilterChain(request -> true),
                LoginFormModuleWebSecurityConfiguration.build(moduleDefinition(identifier), SEQUENCE_GUI_DEFAULT),
                moduleAuthentication);
    }

    private AbstractAuthenticationModuleType moduleDefinition(String identifier) {
        if (MODULE_USER_NAME.equals(identifier)) {
            return new FocusIdentificationAuthenticationModuleType().identifier(identifier);
        } else if (MODULE_ARCHETYPE.equals(identifier)) {
            return new ArchetypeSelectionModuleType().identifier(identifier);
        } else if (MODULE_OTP.equals(identifier)) {
            return new TOtpAuthenticationModuleType().identifier(identifier);
        } else if (MODULE_OIDC.equals(identifier)) {
            return new OidcAuthenticationModuleType().identifier(identifier);
        } else if (MODULE_LDAP.equals(identifier)) {
            return new LdapAuthenticationModuleType().identifier(identifier);
        }
        throw new IllegalArgumentException("Unsupported module identifier: " + identifier);
    }

    /** Sequence listing the modules in the order in which they are executed. */
    private AuthenticationSequenceType sequence(String firstModule, String secondModule) {
        return emptySequence()
                .module(sequenceModule(firstModule, 10, AuthenticationSequenceModuleNecessityType.REQUISITE))
                .module(sequenceModule(secondModule, 30, AuthenticationSequenceModuleNecessityType.SUFFICIENT));
    }

    /** Sequence selecting an archetype first, identifying the user second, authenticating last. */
    private AuthenticationSequenceType archetypeSequence() {
        return emptySequence()
                .module(sequenceModule(MODULE_ARCHETYPE, 10, AuthenticationSequenceModuleNecessityType.REQUISITE))
                .module(sequenceModule(MODULE_USER_NAME, 20, AuthenticationSequenceModuleNecessityType.REQUISITE))
                .module(sequenceModule(MODULE_LDAP, 30, AuthenticationSequenceModuleNecessityType.SUFFICIENT));
    }

    /**
     * Sequence as produced by merging an archetype policy defining only the continuation module with the
     * global policy defining the identification module, i.e. the inherited module is appended.
     */
    private AuthenticationSequenceType mergedSequence(String continuationModule) {
        return emptySequence()
                .module(sequenceModule(continuationModule, 30, AuthenticationSequenceModuleNecessityType.SUFFICIENT))
                .module(sequenceModule(MODULE_USER_NAME, 10, AuthenticationSequenceModuleNecessityType.REQUIRED));
    }

    private AuthenticationSequenceModuleType sequenceModule(
            String identifier, int order, AuthenticationSequenceModuleNecessityType necessity) {
        return new AuthenticationSequenceModuleType()
                .identifier(identifier)
                .order(order)
                .necessity(necessity);
    }

    private AuthenticationSequenceType emptySequence() {
        return new AuthenticationSequenceType()
                .identifier(SEQUENCE_GUI_DEFAULT)
                .channel(new AuthenticationSequenceChannelType()
                        .channelId(SchemaConstants.CHANNEL_USER_URI)
                        .urlSuffix(SEQUENCE_GUI_DEFAULT)
                        ._default(true));
    }
}
