/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.security;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.DISABLED;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.authentication.api.MidpointSessionRegistry;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import com.evolveum.midpoint.schema.util.SimpleExpressionUtil;

import com.evolveum.midpoint.test.IntegrationTestTools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGuiProfiledPrincipalManager extends AbstractInternalModelIntegrationTest {

    private static final String ROLE_CLUSTERWIDE_INVALIDATION_OID = "f32e5728-2fc7-4a88-9ec2-dd43333c2f36";
    private static final String ROLE_AUTHORIZATION_INVALIDATION_OID = "a7777550-9514-4a33-b4e9-f74bde8d86e5";
    private static final String USER_AUTHORIZATION_INVALIDATION_OID = "5bb4f8fc-dc70-437e-b5f9-efda02871c54";
    private static final String ARCHETYPE_INVALIDATION_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c401";
    private static final String ARCHETYPE_INDUCED_ROLE_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c402";
    private static final String ARCHETYPE_USER_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c403";
    private static final String INDUCEMENT_ROLE_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c404";
    private static final String INDUCED_ROLE_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c405";
    private static final String INDUCEMENT_USER_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c406";
    private static final String INDIRECT_OUTER_ROLE_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c407";
    private static final String INDIRECT_INTERMEDIATE_ROLE_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c408";
    private static final String INDIRECT_USER_OID = "a1347d64-4e54-4d0e-b6ef-a755aaf5c409";

    @Autowired private ClusterwideCacheInvalidationDispatcher clusterwideCacheInvalidationDispatcher;

    @Test
    public void test100DeputyNoLimits() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingAll(),
                a -> {});
    }

    @Test // MID-4111
    public void test110DeputyAssignmentDisabled() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.setActivation(new ActivationType().administrativeStatus(DISABLED)));
    }

    @Test // MID-4111
    public void test120DeputyAssignmentNotValid() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.setActivation(new ActivationType().validTo("2017-03-31T00:00:00+01:00")));
    }

    @Test
    public void test130DeputyAssignmentFalseCondition() throws Exception {
        executeDeputyLimitationsTest(
                null, // because of no deputy
                (a) -> a.beginCondition()
                        .expression(SimpleExpressionUtil.groovyExpression(
                                "false", IntegrationTestTools.trustedForTests())));
    }

    @Test
    public void test140DeputyBlockOtherPrivileges() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone(),
                (a) -> a.limitOtherPrivileges(new OtherPrivilegesLimitationType()));
    }

    @Test
    public void test150DeputyAllowOnlyCases() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone().allow(OtherPrivilegesLimitations.Type.CASES),
                (a) -> a.limitOtherPrivileges(
                        new OtherPrivilegesLimitationType()
                                .caseManagementWorkItems(new WorkItemSelectorType()
                                        .all(true))));
    }

    @Test
    public void test160DeputyAllowOnlyCasesLegacy() throws Exception {
        executeDeputyLimitationsTest(
                OtherPrivilegesLimitations.Limitation.allowingNone().allow(OtherPrivilegesLimitations.Type.CASES),
                (a) -> a.limitOtherPrivileges(
                        new OtherPrivilegesLimitationType()
                                .approvalWorkItems(new WorkItemSelectorType()
                                        .all(true))));
    }

    /**
     * Two sessions of the same user have two principals (equal by OID), each with its own compiled profile.
     * Change in system configuration has to invalidate both of them (issue 11041).
     */
    @Test
    public void test200InvalidateProfilesOfAllSessions() throws Exception {
        given("two sessions of the same user");
        OperationResult result = createOperationResult();
        GuiProfiledPrincipal olderPrincipal = focusProfileService.getPrincipal(userAdministrator, null, result);
        GuiProfiledPrincipal newerPrincipal = focusProfileService.getPrincipal(userAdministrator, null, result);
        assertThat(newerPrincipal).isNotSameAs(olderPrincipal).isEqualTo(olderPrincipal);

        TestSessionRegistry sessionRegistry = new TestSessionRegistry();
        sessionRegistry.registerNewSession("older-session", olderPrincipal);
        sessionRegistry.registerNewSession("newer-session", newerPrincipal);

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);
        try {
            when("system configuration is changed");
            manager.invalidate(SystemConfigurationType.class, null, null);
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", null);
        }

        then("profiles of both sessions are invalid");
        assertThat(olderPrincipal.getCompiledGuiProfile().isInvalid()).as("older session profile invalid").isTrue();
        assertThat(newerPrincipal.getCompiledGuiProfile().isInvalid()).as("newer session profile invalid").isTrue();
    }

    @Test
    public void test250RoleModificationIsInvalidatedClusterwide() throws Exception {
        given("a concrete role and an invalidation listener");
        OperationResult result = createOperationResult();
        deleteClusterwideInvalidationRoleIfPresent(result);
        RoleType role = new RoleType()
                .oid(ROLE_CLUSTERWIDE_INVALIDATION_OID)
                .name("clusterwide-invalidation-test-role")
                .activation(new ActivationType().administrativeStatus(ActivationStatusType.ENABLED));

        AtomicInteger matchingEvents = new AtomicInteger();
        AtomicReference<Class<? extends ObjectType>> invalidatedType = new AtomicReference<>();
        AtomicReference<Boolean> clusterwide = new AtomicReference<>();
        ClusterwideCacheInvalidationListener listener = new ClusterwideCacheInvalidationListener() {
            @Override
            public <O extends ObjectType> void invalidate(
                    Class<O> type, String oid, boolean isClusterwide, CacheInvalidationContext context) {
                if (ROLE_CLUSTERWIDE_INVALIDATION_OID.equals(oid)) {
                    matchingEvents.incrementAndGet();
                    invalidatedType.set(type);
                    clusterwide.set(isClusterwide);
                }
            }
        };
        boolean listenerRegistered = false;

        try {
            repositoryService.addObject(role.asPrismObject(), null, result);
            clusterwideCacheInvalidationDispatcher.registerListener(listener);
            listenerRegistered = true;

            when("the role activation is modified through the repository cache");
            repositoryService.modifyObject(
                    RoleType.class,
                    ROLE_CLUSTERWIDE_INVALIDATION_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                            .replace(ActivationStatusType.DISABLED)
                            .asItemDeltas(),
                    result);

            then("the concrete role invalidation is marked clusterwide");
            assertThat(matchingEvents.get()).isEqualTo(1);
            assertThat(invalidatedType.get()).isEqualTo(RoleType.class);
            assertThat(clusterwide.get()).isTrue();
        } finally {
            try {
                if (listenerRegistered) {
                    clusterwideCacheInvalidationDispatcher.unregisterListener(listener);
                }
            } finally {
                deleteClusterwideInvalidationRoleIfPresent(result);
            }
        }
    }

    @Test
    public void test260RoleAuthorizationModificationInvalidatesLocalProfile() throws Exception {
        given("a user with an authorization-bearing role and a GUI session");
        OperationResult result = createOperationResult();
        deleteAuthorizationInvalidationObjectsIfPresent(result);

        RoleType role = new RoleType()
                .oid(ROLE_AUTHORIZATION_INVALIDATION_OID)
                .name("authorization-invalidation-test-role")
                .authorization(new AuthorizationType()
                        .name("initial-authorization")
                        .action(AuthorizationConstants.AUTZ_UI_USERS_URL));
        UserType user = new UserType()
                .oid(USER_AUTHORIZATION_INVALIDATION_OID)
                .name("authorization-invalidation-test-user")
                .assignment(new AssignmentType()
                        .targetRef(ROLE_AUTHORIZATION_INVALIDATION_OID, RoleType.COMPLEX_TYPE));

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        Object originalSessionRegistry = ReflectionTestUtils.getField(manager, "sessionRegistry");
        try {
            repositoryService.addObject(role.asPrismObject(), null, result);
            repositoryService.addObject(user.asPrismObject(), null, result);

            GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user.asPrismObject(), null, result);
            CompiledGuiProfile initialProfile = principal.getCompiledGuiProfile();
            assertThat(initialProfile.isInvalid()).as("initial profile invalid").isFalse();
            assertThat(initialProfile.derivedFrom(ROLE_AUTHORIZATION_INVALIDATION_OID))
                    .as("profile derived from the role")
                    .isTrue();

            TestSessionRegistry sessionRegistry = new TestSessionRegistry();
            sessionRegistry.registerNewSession("authorization-invalidation-session", principal);
            ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);

            when("an authorization is added to the role through the repository cache");
            repositoryService.modifyObject(
                    RoleType.class,
                    ROLE_AUTHORIZATION_INVALIDATION_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_AUTHORIZATION)
                            .add(new AuthorizationType()
                                    .name("added-authorization")
                                    .action(AuthorizationConstants.AUTZ_ALL_URL))
                            .asItemDeltas(),
                    result);

            then("the existing profile is invalidated");
            assertThat(initialProfile.isInvalid()).isTrue();

            when("the profile is refreshed and the role authorizations are removed through the repository cache");
            CompiledGuiProfile refreshedProfile = focusProfileService.refreshCompiledProfile(principal);
            assertThat(refreshedProfile.isInvalid()).as("refreshed profile invalid").isFalse();
            repositoryService.modifyObject(
                    RoleType.class,
                    ROLE_AUTHORIZATION_INVALIDATION_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_AUTHORIZATION)
                            .replace()
                            .asItemDeltas(),
                    result);

            then("the refreshed profile is invalidated as well");
            assertThat(refreshedProfile.isInvalid()).isTrue();
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", originalSessionRegistry);
            deleteAuthorizationInvalidationObjectsIfPresent(result);
        }
    }

    @Test
    public void test270ArchetypeAuthorizationAndInducementModificationInvalidatesLocalProfile() throws Exception {
        given("a user assigned to an authorization-bearing archetype and a GUI session");
        OperationResult result = createOperationResult();
        deleteArchetypeInvalidationObjectsIfPresent(result);

        RoleType inducedRole = new RoleType()
                .oid(ARCHETYPE_INDUCED_ROLE_OID)
                .name("archetype-induced-role");
        ArchetypeType archetype = new ArchetypeType()
                .oid(ARCHETYPE_INVALIDATION_OID)
                .name("authorization-invalidation-test-archetype")
                .authorization(new AuthorizationType()
                        .name("initial-archetype-authorization")
                        .action(AuthorizationConstants.AUTZ_UI_USERS_URL));
        UserType user = new UserType()
                .oid(ARCHETYPE_USER_OID)
                .name("archetype-authorization-invalidation-test-user")
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_INVALIDATION_OID, ArchetypeType.COMPLEX_TYPE));

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        Object originalSessionRegistry = ReflectionTestUtils.getField(manager, "sessionRegistry");
        try {
            repositoryService.addObject(inducedRole.asPrismObject(), null, result);
            repositoryService.addObject(archetype.asPrismObject(), null, result);
            repositoryService.addObject(user.asPrismObject(), null, result);

            GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user.asPrismObject(), null, result);
            CompiledGuiProfile initialProfile = principal.getCompiledGuiProfile();
            assertThat(initialProfile.isInvalid()).as("initial profile invalid").isFalse();
            assertThat(initialProfile.derivedFrom(ARCHETYPE_INVALIDATION_OID))
                    .as("profile derived from the archetype")
                    .isTrue();

            TestSessionRegistry sessionRegistry = new TestSessionRegistry();
            sessionRegistry.registerNewSession("archetype-invalidation-session", principal);
            ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);

            when("an authorization is added to the archetype through the repository cache");
            repositoryService.modifyObject(
                    ArchetypeType.class,
                    ARCHETYPE_INVALIDATION_OID,
                    prismContext.deltaFor(ArchetypeType.class)
                            .item(ArchetypeType.F_AUTHORIZATION)
                            .add(new AuthorizationType()
                                    .name("added-archetype-authorization")
                                    .action(AuthorizationConstants.AUTZ_ALL_URL))
                            .asItemDeltas(),
                    result);

            then("the existing profile is invalidated");
            assertThat(initialProfile.isInvalid()).isTrue();

            when("the profile is refreshed and an inducement is added to the archetype through the repository cache");
            CompiledGuiProfile refreshedProfile = focusProfileService.refreshCompiledProfile(principal);
            assertThat(refreshedProfile.isInvalid()).as("refreshed profile invalid").isFalse();
            repositoryService.modifyObject(
                    ArchetypeType.class,
                    ARCHETYPE_INVALIDATION_OID,
                    prismContext.deltaFor(ArchetypeType.class)
                            .item(ArchetypeType.F_INDUCEMENT)
                            .add(new AssignmentType()
                                    .targetRef(ARCHETYPE_INDUCED_ROLE_OID, RoleType.COMPLEX_TYPE))
                            .asItemDeltas(),
                    result);

            then("the refreshed profile is invalidated by the archetype inducement change");
            assertThat(refreshedProfile.isInvalid()).isTrue();
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", originalSessionRegistry);
            deleteArchetypeInvalidationObjectsIfPresent(result);
        }
    }

    @Test
    public void test280RoleInducementAdditionAndRemovalInvalidatesLocalProfile() throws Exception {
        given("a user assigned to a role and a GUI session");
        OperationResult result = createOperationResult();
        deleteInducementInvalidationObjectsIfPresent(result);

        RoleType inducedRole = new RoleType()
                .oid(INDUCED_ROLE_OID)
                .name("induced-authorization-role")
                .authorization(new AuthorizationType()
                        .name("induced-authorization")
                        .action(AuthorizationConstants.AUTZ_UI_USERS_URL));
        RoleType role = new RoleType()
                .oid(INDUCEMENT_ROLE_OID)
                .name("inducement-invalidation-test-role");
        UserType user = new UserType()
                .oid(INDUCEMENT_USER_OID)
                .name("inducement-invalidation-test-user")
                .assignment(new AssignmentType()
                        .targetRef(INDUCEMENT_ROLE_OID, RoleType.COMPLEX_TYPE));

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        Object originalSessionRegistry = ReflectionTestUtils.getField(manager, "sessionRegistry");
        try {
            repositoryService.addObject(inducedRole.asPrismObject(), null, result);
            repositoryService.addObject(role.asPrismObject(), null, result);
            repositoryService.addObject(user.asPrismObject(), null, result);

            GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user.asPrismObject(), null, result);
            CompiledGuiProfile initialProfile = principal.getCompiledGuiProfile();
            assertThat(initialProfile.isInvalid()).as("initial profile invalid").isFalse();
            assertThat(initialProfile.derivedFrom(INDUCEMENT_ROLE_OID))
                    .as("profile derived from the assigned role")
                    .isTrue();

            TestSessionRegistry sessionRegistry = new TestSessionRegistry();
            sessionRegistry.registerNewSession("inducement-invalidation-session", principal);
            ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);

            when("an inducement is added to the assigned role through the repository cache");
            repositoryService.modifyObject(
                    RoleType.class,
                    INDUCEMENT_ROLE_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_INDUCEMENT)
                            .add(new AssignmentType()
                                    .targetRef(INDUCED_ROLE_OID, RoleType.COMPLEX_TYPE))
                            .asItemDeltas(),
                    result);

            then("the existing profile is invalidated");
            assertThat(initialProfile.isInvalid()).isTrue();

            when("the profile is refreshed and the inducement is removed through the repository cache");
            CompiledGuiProfile refreshedProfile = focusProfileService.refreshCompiledProfile(principal);
            assertThat(refreshedProfile.isInvalid()).as("refreshed profile invalid").isFalse();
            assertThat(refreshedProfile.derivedFrom(INDUCED_ROLE_OID))
                    .as("refreshed profile derived from the induced role")
                    .isTrue();
            repositoryService.modifyObject(
                    RoleType.class,
                    INDUCEMENT_ROLE_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_INDUCEMENT)
                            .replace()
                            .asItemDeltas(),
                    result);

            then("the refreshed profile is invalidated as well");
            assertThat(refreshedProfile.isInvalid()).isTrue();
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", originalSessionRegistry);
            deleteInducementInvalidationObjectsIfPresent(result);
        }
    }

    @Test
    public void test290IntermediateRoleAuthorizationModificationInvalidatesLocalProfile() throws Exception {
        given("a user indirectly assigned an authorization-bearing role and a GUI session");
        OperationResult result = createOperationResult();
        deleteIndirectInvalidationObjectsIfPresent(result);

        RoleType intermediateRole = new RoleType()
                .oid(INDIRECT_INTERMEDIATE_ROLE_OID)
                .name("indirect-intermediate-authorization-role")
                .authorization(new AuthorizationType()
                        .name("intermediate-authorization")
                        .action(AuthorizationConstants.AUTZ_UI_USERS_URL));
        RoleType outerRole = new RoleType()
                .oid(INDIRECT_OUTER_ROLE_OID)
                .name("indirect-outer-role")
                .inducement(new AssignmentType()
                        .targetRef(INDIRECT_INTERMEDIATE_ROLE_OID, RoleType.COMPLEX_TYPE));
        UserType user = new UserType()
                .oid(INDIRECT_USER_OID)
                .name("indirect-authorization-invalidation-test-user")
                .assignment(new AssignmentType()
                        .targetRef(INDIRECT_OUTER_ROLE_OID, RoleType.COMPLEX_TYPE));

        GuiProfiledPrincipalManagerImpl manager = AopTestUtils.getUltimateTargetObject(focusProfileService);
        Object originalSessionRegistry = ReflectionTestUtils.getField(manager, "sessionRegistry");
        try {
            repositoryService.addObject(intermediateRole.asPrismObject(), null, result);
            repositoryService.addObject(outerRole.asPrismObject(), null, result);
            repositoryService.addObject(user.asPrismObject(), null, result);

            GuiProfiledPrincipal principal = focusProfileService.getPrincipal(user.asPrismObject(), null, result);
            CompiledGuiProfile profile = principal.getCompiledGuiProfile();
            assertThat(profile.isInvalid()).as("initial profile invalid").isFalse();
            assertThat(profile.derivedFrom(INDIRECT_OUTER_ROLE_OID))
                    .as("profile derived from the directly assigned outer role")
                    .isTrue();
            assertThat(profile.derivedFrom(INDIRECT_INTERMEDIATE_ROLE_OID))
                    .as("profile derived from the indirectly assigned intermediate role")
                    .isTrue();

            TestSessionRegistry sessionRegistry = new TestSessionRegistry();
            sessionRegistry.registerNewSession("indirect-invalidation-session", principal);
            ReflectionTestUtils.setField(manager, "sessionRegistry", sessionRegistry);

            when("the intermediate role authorization is modified through the repository cache");
            repositoryService.modifyObject(
                    RoleType.class,
                    INDIRECT_INTERMEDIATE_ROLE_OID,
                    prismContext.deltaFor(RoleType.class)
                            .item(RoleType.F_AUTHORIZATION)
                            .add(new AuthorizationType()
                                    .name("added-intermediate-authorization")
                                    .action(AuthorizationConstants.AUTZ_ALL_URL))
                            .asItemDeltas(),
                    result);

            then("the dependent profile is invalidated");
            assertThat(profile.isInvalid()).isTrue();
        } finally {
            ReflectionTestUtils.setField(manager, "sessionRegistry", originalSessionRegistry);
            deleteIndirectInvalidationObjectsIfPresent(result);
        }
    }

    private void deleteClusterwideInvalidationRoleIfPresent(OperationResult result) {
        OperationResult cleanupResult =
                new OperationResult(result.getOperation() + ".deleteClusterwideInvalidationRoleIfPresent");
        try {
            repositoryService.deleteObject(RoleType.class, ROLE_CLUSTERWIDE_INVALIDATION_OID, cleanupResult);
        } catch (ObjectNotFoundException ignored) {
            // The fixture is already absent, which is the desired state.
        }
    }

    private void deleteAuthorizationInvalidationObjectsIfPresent(OperationResult result) {
        deleteObjectIfPresent(UserType.class, USER_AUTHORIZATION_INVALIDATION_OID, result);
        deleteObjectIfPresent(RoleType.class, ROLE_AUTHORIZATION_INVALIDATION_OID, result);
    }

    private void deleteArchetypeInvalidationObjectsIfPresent(OperationResult result) {
        deleteObjectIfPresent(UserType.class, ARCHETYPE_USER_OID, result);
        deleteObjectIfPresent(ArchetypeType.class, ARCHETYPE_INVALIDATION_OID, result);
        deleteObjectIfPresent(RoleType.class, ARCHETYPE_INDUCED_ROLE_OID, result);
    }

    private void deleteInducementInvalidationObjectsIfPresent(OperationResult result) {
        deleteObjectIfPresent(UserType.class, INDUCEMENT_USER_OID, result);
        deleteObjectIfPresent(RoleType.class, INDUCEMENT_ROLE_OID, result);
        deleteObjectIfPresent(RoleType.class, INDUCED_ROLE_OID, result);
    }

    private void deleteIndirectInvalidationObjectsIfPresent(OperationResult result) {
        deleteObjectIfPresent(UserType.class, INDIRECT_USER_OID, result);
        deleteObjectIfPresent(RoleType.class, INDIRECT_OUTER_ROLE_OID, result);
        deleteObjectIfPresent(RoleType.class, INDIRECT_INTERMEDIATE_ROLE_OID, result);
    }

    private <O extends ObjectType> void deleteObjectIfPresent(Class<O> type, String oid, OperationResult result) {
        OperationResult cleanupResult = new OperationResult(result.getOperation() + ".deleteObjectIfPresent");
        try {
            repositoryService.deleteObject(type, oid, cleanupResult);
        } catch (ObjectNotFoundException ignored) {
            // The fixture is already absent, which is the desired state.
        }
    }

    private static class TestSessionRegistry extends SessionRegistryImpl implements MidpointSessionRegistry {

        @Override
        public List<SessionInformation> getLoggedInUsersSession(Object principal) {
            return getAllSessions(principal, false);
        }
    }

    private void executeDeputyLimitationsTest(
            OtherPrivilegesLimitations.Limitation expectedJackLimitation, Consumer<AssignmentType> assignmentModifier)
            throws CommonException {

        given();
        AssignmentType assignment = new AssignmentType()
                .targetRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
        assignmentModifier.accept(assignment);

        UserType deputy = prismContext.createObjectable(UserType.class)
                .name("deputy")
                .oid("deputy")
                .assignment(assignment);

        when();
        AbstractIntegrationTest.display("Logging in as", deputy);
        login(deputy.asPrismObject());

        then();
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        OtherPrivilegesLimitations limitations = principal.getOtherPrivilegesLimitations();
        displayDumpable("other privileges limitations", limitations);
        var limitation = limitations.get(UserType.class, USER_JACK_OID);
        assertThat(limitation).as("limitation for jack").isEqualTo(expectedJackLimitation);
    }
}
