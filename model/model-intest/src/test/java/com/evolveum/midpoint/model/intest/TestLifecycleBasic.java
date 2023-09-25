/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.AssignmentsState.ASSIGNMENTS_ACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.AssignmentsState.ASSIGNMENTS_INACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.Authorizations.AUTHORIZATIONS_ACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.Authorizations.AUTHORIZATIONS_INACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.Mappings.MAPPINGS_APPLIED;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.Mappings.MAPPINGS_NOT_APPLIED;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.RoleMembershipRef.ROLE_MEMBERSHIP_REF_ACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.RoleMembershipRef.ROLE_MEMBERSHIP_REF_INACTIVE;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.UserState.USER_DISABLED;
import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.UserState.USER_ENABLED;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the most basic functioning of the lifecycle feature.
 *
 * In comparison to {@link TestLifecycle}, this class uses the default (relative) projection enforcement, and no special
 * lifecycle configurations like transitions between states.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLifecycleBasic extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/lifecycle-basic");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_USER = TestObject.file(
            TEST_DIR, "template-user.xml", "1df94b0b-def5-4fcc-a109-965a1e8270db");

    private static final TestObject<ArchetypeType> ARCHETYPE_BASIC = TestObject.file(
            TEST_DIR, "archetype-basic.xml", "36f0ede7-959c-450d-9fbc-d0c2cb75e167");

    private static final DummyTestResource RESOURCE_TARGET_ONE = new DummyTestResource(
            TEST_DIR, "resource-target-one.xml", "bd08e2c7-17bd-42cd-ae07-f78aa16e11b3", "target-one");
    private static final DummyTestResource RESOURCE_TARGET_TWO = new DummyTestResource(
            TEST_DIR, "resource-target-two.xml", "2fe7c02d-5f0e-42a4-b4d7-495452179691", "target-two");

    private static final TestObject<RoleType> ROLE_FIXED_ACTIVE = TestObject.file(
            TEST_DIR, "role-fixed-active.xml", "5df4cceb-07c4-4097-ac06-748ac1a3c938");

    private static final TestObject<RoleType> ROLE_FIXED_DRAFT = TestObject.file(
            TEST_DIR, "role-fixed-draft.xml", "608c6831-9e53-42a9-9233-12965cac6076");

    private static final TestObject<RoleType> ROLE_ASSIGNED_FIXED_DRAFT = TestObject.file(
            TEST_DIR, "role-assigned-fixed-draft.xml", "bd910517-8c96-4f35-b95a-c23aac8692f3");

    private static final TestObject<RoleType> ROLE_TARGET_TWO = TestObject.file(
            TEST_DIR, "role-target-two.xml", "9f7722eb-6f1a-4c48-a413-50f68871dda2");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                TEMPLATE_USER,
                ARCHETYPE_BASIC,
                RESOURCE_TARGET_ONE,
                RESOURCE_TARGET_TWO,
                ROLE_FIXED_ACTIVE,
                ROLE_FIXED_DRAFT,
                ROLE_ASSIGNED_FIXED_DRAFT,
                ROLE_TARGET_TWO);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100CreateEnabledUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addObject(standardUser(userName), task, result);

        then("user is OK");
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test110CreateDisabledUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("disabled user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .activation(new ActivationType()
                                .administrativeStatus(ActivationStatusType.DISABLED)),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test120CreateArchivedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("archived user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .activation(new ActivationType()
                                .administrativeStatus(ActivationStatusType.ARCHIVED)),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test130CreateDraftUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("draft user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE);
    }

    @Test
    public void test140CreateProposedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("proposed user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE);
    }

    @Test
    public void test150CreateSuspendedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("suspended user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_SUSPENDED),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test160CreateDeprecatedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("deprecated user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_DEPRECATED),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test170CreateArchivedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("archived user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_ARCHIVED),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE);
    }

    @Test
    public void test180CreateFailedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("failed user is created");
        String userName = getTestNameShort();
        String userOid = addObject(
                standardUser(userName)
                        .lifecycleState(SchemaConstants.LIFECYCLE_FAILED),
                task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE);
    }

    @Test
    public void test200EnabledToDisabledUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addObject(standardUser(userName), task, result);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE);

        and("user is disabled");
        executeChanges(
                deltaFor(UserType.class)
                        .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test210ActiveToSuspendedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addObject(standardUser(userName), task, result);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE);

        and("user is suspended");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_SUSPENDED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE);
    }

    @Test
    public void test220ActiveToArchivedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addObject(standardUser(userName), task, result);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE);

        and("user is archived");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ARCHIVED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE);
    }

    private void assertUser(
            String userOid,
            UserState userState,
            AssignmentsState assignmentsState) throws Exception {
        // @formatter:off
        var user = assertUserAfter(userOid)
                .activation()
                .assertEffectiveStatus(
                        userState == USER_ENABLED ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED)
                .end()
                .getObjectable();
        // @formatter:on

        MidPointPrincipal principal = getPrincipal(user.getName().getOrig());

        if (assignmentsState == ASSIGNMENTS_ACTIVE) {
            assertAssignmentsActive(user, principal);
        } else {
            assertAssignmentsInactive(user, principal);
        }
    }

    private void assertAssignmentsActive(UserType user, MidPointPrincipal principal) throws Exception {
        assertRole(user, principal, ARCHETYPE_BASIC,
                ActivationStatusType.ENABLED,
                ROLE_MEMBERSHIP_REF_ACTIVE,
                MAPPINGS_APPLIED,
                AUTHORIZATIONS_ACTIVE);
        assertRole(user, principal, ROLE_FIXED_ACTIVE,
                ActivationStatusType.ENABLED,
                ROLE_MEMBERSHIP_REF_ACTIVE,
                MAPPINGS_APPLIED,
                AUTHORIZATIONS_ACTIVE);
        assertRole(user, principal, ROLE_FIXED_DRAFT,
                ActivationStatusType.ENABLED,
                ROLE_MEMBERSHIP_REF_ACTIVE,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_INACTIVE);
        assertRole(user, principal, ROLE_ASSIGNED_FIXED_DRAFT,
                ActivationStatusType.DISABLED,
                ROLE_MEMBERSHIP_REF_INACTIVE,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_INACTIVE);

        assertUser(user, "after")
                .assertLiveLinks(2);
        RESOURCE_TARGET_ONE.controller.assertAccountByUsername(user.getName().getOrig());
        RESOURCE_TARGET_TWO.controller.assertAccountByUsername(user.getName().getOrig());
    }

    private void assertAssignmentsInactive(UserType user, MidPointPrincipal principal) throws Exception {
        assertRole(user, principal, ARCHETYPE_BASIC,
                ActivationStatusType.ENABLED, // TODO why?
                ROLE_MEMBERSHIP_REF_ACTIVE, // will change
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_ACTIVE); // FIXME
        assertRole(user, principal, ROLE_FIXED_ACTIVE,
                ActivationStatusType.ENABLED, // TODO why?
                ROLE_MEMBERSHIP_REF_INACTIVE,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_INACTIVE);
        assertRole(user, principal, ROLE_FIXED_DRAFT,
                ActivationStatusType.ENABLED, // TODO why?
                ROLE_MEMBERSHIP_REF_INACTIVE,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_INACTIVE);
        assertRole(user, principal, ROLE_ASSIGNED_FIXED_DRAFT,
                ActivationStatusType.DISABLED,
                ROLE_MEMBERSHIP_REF_INACTIVE,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_INACTIVE);
        // assertUser(user, "after")
        //        .assertLiveLinks(0); // FIXME MID-9061
        RESOURCE_TARGET_ONE.controller.assertNoAccountByUsername(user.getName().getOrig());
        // RESOURCE_TARGET_TWO.controller.assertNoAccountByUsername(user.getName().getOrig()); // FIXME MID-9061
    }

    private Set<String> getAuthorizations(MidPointPrincipal principal) {
        return principal.getAuthorities().stream()
                .flatMap(a -> a.getAction().stream())
                .map(a -> StringUtils.removeStart(a, "http://test.evolveum.com/authorization#"))
                .collect(Collectors.toSet());
    }

    private UserType standardUser(String name) {
        return new UserType()
                .name(name)
                .assignment(
                        ARCHETYPE_BASIC.assignmentTo())
                .assignment(
                        ROLE_FIXED_DRAFT.assignmentTo())
                .assignment(
                        ROLE_ASSIGNED_FIXED_DRAFT.assignmentTo()
                                .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT))
                .assignment(
                        ROLE_FIXED_ACTIVE.assignmentTo())
                .assignment(new AssignmentType()
                        .construction(
                                RESOURCE_TARGET_ONE.defaultConstruction()))
                .assignment(
                        ROLE_TARGET_TWO.assignmentTo());
    }

    private MidPointPrincipal getPrincipal(String userName) throws CommonException {
        return focusProfileService.getPrincipal(userName, UserType.class);
    }

    private void assertRole(
            UserType user, MidPointPrincipal principal, TestObject<? extends AbstractRoleType> abstractRole,
            ActivationStatusType assignmentEffectiveStatus,
            RoleMembershipRef roleMembershipRefState,
            Mappings mappingsState,
            Authorizations authorizationsState) throws CommonException {

        String roleName = abstractRole.getNameOrig();

        // @formatter:off
        assertUser(user, "after")
                .assignments()
                .by().targetOid(abstractRole.oid).find()
                .activation()
                .assertEffectiveStatus(assignmentEffectiveStatus)
                .end()
                .end()
                .end();
        // @formatter:on

        if (roleMembershipRefState == ROLE_MEMBERSHIP_REF_ACTIVE) {
            assertUser(user, "after")
                    .roleMembershipRefs()
                    .by().targetOid(abstractRole.oid).find()
                    .end();
        } else {
            assertUser(user, "after")
                    .roleMembershipRefs()
                    .by().targetOid(abstractRole.oid).assertNone()
                    .end();
        }

        var organizations = user.getOrganization().stream()
                .map(o -> o.getOrig())
                .collect(Collectors.toSet());
        if (mappingsState == MAPPINGS_APPLIED) {
            assertThat(organizations)
                    .withFailMessage("Mapping for %s was not executed", roleName)
                    .contains(roleName);
        } else {
            assertThat(organizations)
                    .withFailMessage("Mapping for %s was executed even if it should not be", roleName)
                    .doesNotContain(roleName);
        }

        var actions = getAuthorizations(principal);
        if (authorizationsState == AUTHORIZATIONS_ACTIVE) {
            assertThat(actions)
                    .withFailMessage("Authorization for %s is not present", roleName)
                    .contains(roleName);
        } else {
            assertThat(actions)
                    .withFailMessage("Authorization for %s is present even if it should not be", roleName)
                    .doesNotContain(roleName);
        }
    }

    enum UserState {
        USER_ENABLED, USER_DISABLED
    }

    enum AssignmentsState {
        ASSIGNMENTS_ACTIVE, ASSIGNMENTS_INACTIVE
    }

    enum RoleMembershipRef {
        ROLE_MEMBERSHIP_REF_ACTIVE, ROLE_MEMBERSHIP_REF_INACTIVE
    }

    enum Mappings {
        MAPPINGS_APPLIED, MAPPINGS_NOT_APPLIED
    }

    enum Authorizations {
        AUTHORIZATIONS_ACTIVE, AUTHORIZATIONS_INACTIVE
    }
}
