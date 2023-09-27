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
import com.evolveum.midpoint.test.RunFlagsCollector;
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

import static com.evolveum.midpoint.model.intest.TestLifecycleBasic.LocalAssertion.*;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.LIFECYCLE_DRAFT;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the most basic functioning of the lifecycle feature.
 *
 * In comparison to {@link TestLifecycle}, this class uses the default (relative) projection enforcement, and no special
 * lifecycle configurations like transitions between states.
 *
 * See https://docs.evolveum.com/midpoint/devel/design/lifecycle/current-state/.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLifecycleBasic extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/lifecycle-basic");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_USER = TestObject.file(
            TEST_DIR, "template-user.xml", "1df94b0b-def5-4fcc-a109-965a1e8270db");

    private static final TestObject<ResourceType> RESOURCE_TEMPLATE = TestObject.file(
            TEST_DIR, "resource-template.xml", "ca449b85-f86e-45bd-ab40-4f36028f5b58");

    private static final DummyTestResource RESOURCE_DIRECT_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-direct-active.xml", "cd1d3aca-72e5-4b8c-9075-1db6c20b1949", "resource-direct-active");
    private static final DummyTestResource RESOURCE_DIRECT_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-direct-draft.xml", "fdfd157e-68f7-4dee-84c2-4a399f12aa23", "resource-direct-draft");
    private static final DummyTestResource RESOURCE_DIRECT_ASSIGNED_IN_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-direct-assigned-in-draft.xml", "77440749-8a6d-4630-b1d4-80bc6173c3aa", "resource-direct-assigned-in-draft");

    private static final TestObject<ArchetypeType> ARCHETYPE_ACTIVE = TestObject.file(
            TEST_DIR, "archetype-active.xml", "36f0ede7-959c-450d-9fbc-d0c2cb75e167");
    private static final DummyTestResource RESOURCE_ARCHETYPE_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-archetype-active.xml", "842752b4-f0e1-4ed6-a80b-8f919e5a7427", "resource-archetype-active");

    private static final TestObject<ArchetypeType> ARCHETYPE_DRAFT = TestObject.file(
            TEST_DIR, "archetype-draft.xml", "105ef36d-cffa-4d9a-bf08-ac92fa4042f0");
    private static final DummyTestResource RESOURCE_ARCHETYPE_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-archetype-draft.xml", "1a23bcf9-4406-477f-bd86-493ce6920fd6", "resource-archetype-draft");

    private static final TestObject<ArchetypeType> ARCHETYPE_ASSIGNED_IN_DRAFT = TestObject.file(
            TEST_DIR, "archetype-assigned-in-draft.xml", "54f3eb91-b90b-4391-b3bf-244ec7fe920e");
    private static final DummyTestResource RESOURCE_ARCHETYPE_ASSIGNED_IN_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-archetype-assigned-in-draft.xml", "b32ee8bc-6d83-487d-88af-1cefdd0fba59", "resource-archetype-assigned-in-draft");

    private static final TestObject<RoleType> ROLE_ACTIVE = TestObject.file(
            TEST_DIR, "role-active.xml", "5df4cceb-07c4-4097-ac06-748ac1a3c938");
    private static final DummyTestResource RESOURCE_ROLE_ACTIVE = new DummyTestResource(
            TEST_DIR, "resource-role-active.xml", "bd08e2c7-17bd-42cd-ae07-f78aa16e11b3", "resource-role-active");

    private static final TestObject<RoleType> ROLE_DRAFT = TestObject.file(
            TEST_DIR, "role-draft.xml", "608c6831-9e53-42a9-9233-12965cac6076");
    private static final DummyTestResource RESOURCE_ROLE_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-role-draft.xml", "1118ae9c-3e85-416b-865c-f99071a9a1a6", "resource-role-draft");

    private static final TestObject<RoleType> ROLE_ASSIGNED_IN_DRAFT = TestObject.file(
            TEST_DIR, "role-assigned-in-draft.xml", "bd910517-8c96-4f35-b95a-c23aac8692f3");
    private static final DummyTestResource RESOURCE_ROLE_ASSIGNED_IN_DRAFT = new DummyTestResource(
            TEST_DIR, "resource-role-assigned-in-draft.xml", "35341da9-97e5-4c30-8c03-f8db6200a939", "resource-role-assigned-in-draft");

    public static final RunFlagsCollector OBJECT_CONSTRAINTS = new RunFlagsCollector("object policy constraints");
    public static final RunFlagsCollector ASSIGNMENT_CONSTRAINTS = new RunFlagsCollector("assignment policy constraints");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                RESOURCE_TEMPLATE,
                TEMPLATE_USER,
                RESOURCE_DIRECT_ACTIVE,
                RESOURCE_DIRECT_DRAFT,
                RESOURCE_DIRECT_ASSIGNED_IN_DRAFT,
                ARCHETYPE_ACTIVE, RESOURCE_ARCHETYPE_ACTIVE,
                ARCHETYPE_DRAFT, RESOURCE_ARCHETYPE_DRAFT,
                ARCHETYPE_ASSIGNED_IN_DRAFT, RESOURCE_ARCHETYPE_ASSIGNED_IN_DRAFT,
                ROLE_ACTIVE, RESOURCE_ROLE_ACTIVE,
                ROLE_DRAFT, RESOURCE_ROLE_DRAFT,
                ROLE_ASSIGNED_IN_DRAFT, RESOURCE_ROLE_ASSIGNED_IN_DRAFT);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100CreateEnabledUser() throws Exception {
        preTestCleanup();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, null);

        then("user is OK");
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test110CreateStatusDisabledUser() throws Exception {
        preTestCleanup();

        when("disabled user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, ActivationStatusType.DISABLED, null);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test120CreateStatusArchivedUser() throws Exception {
        preTestCleanup();

        when("archived user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, ActivationStatusType.ARCHIVED, null);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test130CreateDraftUser() throws Exception {
        preTestCleanup();

        when("draft user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_DRAFT);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test140CreateProposedUser() throws Exception {
        preTestCleanup();

        when("proposed user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_PROPOSED);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test150CreateSuspendedUser() throws Exception {
        preTestCleanup();

        when("suspended user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_SUSPENDED);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test160CreateDeprecatedUser() throws Exception {
        preTestCleanup();

        when("deprecated user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_DEPRECATED);

        then("user is OK");
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test170CreateArchivedUser() throws Exception {
        preTestCleanup();

        when("archived user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_ARCHIVED);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test180CreateFailedUser() throws Exception {
        preTestCleanup();

        when("failed user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, SchemaConstants.LIFECYCLE_FAILED);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test200EnabledToStatusDisabledUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        preTestCleanup();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, null);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);

        and("user is disabled");
        executeChanges(
                deltaFor(UserType.class)
                        .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test210ActiveToSuspendedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        preTestCleanup();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, null);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);

        and("user is suspended");
        clearConstraintFlags();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_SUSPENDED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test220ActiveToArchivedUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        preTestCleanup();

        when("standard user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, null);
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);

        and("user is archived");
        clearConstraintFlags();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ARCHIVED)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    @Test
    public void test230DraftToActiveUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        preTestCleanup();

        when("draft user is created");
        String userName = getTestNameShort();
        String userOid = addUser(userName, null, LIFECYCLE_DRAFT);
        assertUser(userOid, USER_DISABLED, ASSIGNMENTS_INACTIVE, GLOBAL_POLICY_RULES_APPLIED);

        and("user is activated");
        clearConstraintFlags();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
                        .asObjectDelta(userOid),
                null, task, result);

        then("user is OK");
        assertUser(userOid, USER_ENABLED, ASSIGNMENTS_ACTIVE, GLOBAL_POLICY_RULES_APPLIED);
    }

    private static void preTestCleanup() {
        clearConstraintFlags();
    }

    private static void clearConstraintFlags() {
        OBJECT_CONSTRAINTS.clear();
        ASSIGNMENT_CONSTRAINTS.clear();
    }

    private void assertUser(
            String userOid,
            LocalAssertion... assertions) throws Exception {

        var user = assertUserAfter(userOid).getObjectable();
        MidPointPrincipal principal = getPrincipal(user.getName().getOrig());

        for (var assertion : assertions) {
            switch (assertion) {
                case USER_ENABLED ->
                        assertUser(user, "after")
                                .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED);
                case USER_DISABLED ->
                        assertUser(user, "after")
                                .activation()
                                .assertEffectiveStatus(ActivationStatusType.DISABLED);
                case ASSIGNMENTS_ACTIVE ->
                        assertAssignmentsActive(user, principal);
                case ASSIGNMENTS_INACTIVE ->
                        assertAssignmentsInactive(user, principal);
                case GLOBAL_POLICY_RULES_APPLIED ->
                        OBJECT_CONSTRAINTS.assertPresent("global");
                default -> throw new AssertionError(assertion);
            }
        }

        displayValue("Object constraints executed", OBJECT_CONSTRAINTS);
        displayValue("Assignment constraints executed", ASSIGNMENT_CONSTRAINTS);
    }

    private void assertAssignmentsActive(UserType user, MidPointPrincipal principal) throws Exception {
        assertAssignment(user, principal, ARCHETYPE_ACTIVE,
                ASSIGNMENT_ENABLED,
                ARCHETYPE_REF_PRESENT,
                ROLE_MEMBERSHIP_REF_PRESENT,
                MAPPINGS_APPLIED,
                AUTHORIZATIONS_PRESENT,
                OBJECT_POLICY_RULES_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED,
                ACCOUNTS_PRESENT);
        assertAssignment(user, principal, ROLE_ACTIVE,
                ASSIGNMENT_ENABLED,
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_PRESENT,
                MAPPINGS_APPLIED,
                AUTHORIZATIONS_PRESENT,
                OBJECT_POLICY_RULES_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED,
                ACCOUNTS_PRESENT);
        assertAssignment(user, principal, ARCHETYPE_DRAFT,
                ASSIGNMENT_ENABLED,
                ARCHETYPE_REF_PRESENT,
                ROLE_MEMBERSHIP_REF_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_NOT_APPLIED,
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ROLE_DRAFT,
                ASSIGNMENT_ENABLED,
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_NOT_APPLIED,
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ARCHETYPE_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ARCHETYPE_REF_PRESENT, // TODO later (archetype assignment cannot have lifecycle state)
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_APPLIED, // FIXME
                ASSIGNMENT_POLICY_RULES_APPLIED,
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ROLE_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_APPLIED, // FIXME
                ASSIGNMENT_POLICY_RULES_APPLIED,
                ACCOUNTS_NOT_PRESENT);

        assertResourceAssignment(user, RESOURCE_DIRECT_ACTIVE,
                ASSIGNMENT_ENABLED,
                ACCOUNTS_PRESENT);
        assertResourceAssignment(user, RESOURCE_DIRECT_DRAFT,
                ASSIGNMENT_ENABLED,
                ACCOUNTS_NOT_PRESENT);
        assertResourceAssignment(user, RESOURCE_DIRECT_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ACCOUNTS_NOT_PRESENT);
    }

    private void assertAssignmentsInactive(UserType user, MidPointPrincipal principal) throws Exception {
        assertAssignment(user, principal, ARCHETYPE_ACTIVE,
                ASSIGNMENT_ENABLED, // TODO probably not correct
                ARCHETYPE_REF_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ROLE_ACTIVE,
                ASSIGNMENT_ENABLED, // TODO probably not correct
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ARCHETYPE_DRAFT,
                ASSIGNMENT_ENABLED, // TODO probably not correct
                ARCHETYPE_REF_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_NOT_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ROLE_DRAFT,
                ASSIGNMENT_ENABLED, // TODO probably not correct
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_NOT_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ARCHETYPE_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ARCHETYPE_REF_PRESENT, // FIXME
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);
        assertAssignment(user, principal, ROLE_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ARCHETYPE_REF_NOT_PRESENT,
                ROLE_MEMBERSHIP_REF_NOT_PRESENT,
                MAPPINGS_NOT_APPLIED,
                AUTHORIZATIONS_NOT_PRESENT,
                OBJECT_POLICY_RULES_NOT_APPLIED,
                ASSIGNMENT_POLICY_RULES_APPLIED, // TODO not sure if correct
                ACCOUNTS_NOT_PRESENT);

        assertResourceAssignment(user, RESOURCE_DIRECT_ACTIVE,
                ASSIGNMENT_ENABLED, // FIXME
                ACCOUNTS_NOT_PRESENT);
        assertResourceAssignment(user, RESOURCE_DIRECT_DRAFT,
                ASSIGNMENT_ENABLED, // FIXME
                ACCOUNTS_NOT_PRESENT);
        assertResourceAssignment(user, RESOURCE_DIRECT_ASSIGNED_IN_DRAFT,
                ASSIGNMENT_DISABLED,
                ACCOUNTS_NOT_PRESENT);
    }

    private Set<String> getAuthorizations(MidPointPrincipal principal) {
        return principal.getAuthorities().stream()
                .flatMap(a -> a.getAction().stream())
                .map(a -> StringUtils.removeStart(a, "http://test.evolveum.com/authorization#"))
                .collect(Collectors.toSet());
    }

    private String addUser(String name, ActivationStatusType administrativeStatus, String lifecycleState)
            throws CommonException {
        var user = new UserType()
                .name(name)
                .activation(new ActivationType()
                        .administrativeStatus(administrativeStatus))
                .lifecycleState(lifecycleState)
                .assignment(
                        ARCHETYPE_ACTIVE.assignmentTo())
                .assignment(
                        ROLE_ACTIVE.assignmentTo())
                .assignment(
                        ARCHETYPE_DRAFT.assignmentTo())
                .assignment(
                        ROLE_DRAFT.assignmentTo())
                .assignment(
                        ARCHETYPE_ASSIGNED_IN_DRAFT.assignmentTo()
                                .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT))
                .assignment(
                        ROLE_ASSIGNED_IN_DRAFT.assignmentTo()
                                .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT))
                .assignment(new AssignmentType()
                        .construction(
                                RESOURCE_DIRECT_ACTIVE.defaultConstruction()))
                .assignment(new AssignmentType()
                        .construction(
                                RESOURCE_DIRECT_DRAFT.defaultConstruction()))
                .assignment(new AssignmentType()
                        .construction(
                                RESOURCE_DIRECT_ASSIGNED_IN_DRAFT.defaultConstruction())
                        .lifecycleState(SchemaConstants.LIFECYCLE_DRAFT));

        return addObject(user, getTestTask(), getTestOperationResult());
    }

    private MidPointPrincipal getPrincipal(String userName) throws CommonException {
        return focusProfileService.getPrincipal(userName, UserType.class);
    }

    private void assertAssignment(
            UserType user, MidPointPrincipal principal, TestObject<? extends AbstractRoleType> target,
            LocalAssertion... assertions) throws Exception {

        String userName = user.getName().getOrig();
        String targetName = target.getNameOrig();
        String targetOid = target.oid;
        var archetypeOids = user.getArchetypeRef().stream()
                .map(ref -> ref.getOid())
                .collect(Collectors.toSet());
        var organizations = user.getOrganization().stream()
                .map(o -> o.getOrig())
                .collect(Collectors.toSet());
        var autzActions = getAuthorizations(principal);

        for (LocalAssertion assertion : assertions) {
            switch (assertion) {
                case ASSIGNMENT_ENABLED, ASSIGNMENT_DISABLED ->
                        assertUser(user, "after")
                                .assignments()
                                .by().targetOid(target.oid).find()
                                .activation()
                                .assertEffectiveStatus(
                                        assertion == ASSIGNMENT_ENABLED ?
                                                ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
                case ROLE_MEMBERSHIP_REF_PRESENT ->
                        assertUser(user, "after")
                                .roleMembershipRefs()
                                .by().targetOid(target.oid).find();
                case ROLE_MEMBERSHIP_REF_NOT_PRESENT ->
                        assertUser(user, "after")
                                .roleMembershipRefs()
                                .by().targetOid(target.oid).assertNone();
                case ARCHETYPE_REF_PRESENT ->
                        assertThat(archetypeOids)
                                .withFailMessage("Archetype %s not present in archetypeRef even if it should be; values: %s", targetName, archetypeOids)
                                .contains(targetOid);
                case ARCHETYPE_REF_NOT_PRESENT ->
                        assertThat(archetypeOids)
                                .withFailMessage("Archetype %s is present in archetypeRef even if it should not be; values: %s", targetName, archetypeOids)
                                .doesNotContain(targetOid);
                case MAPPINGS_APPLIED ->
                        assertThat(organizations)
                                .withFailMessage("Mapping for %s was not executed", targetName)
                                .contains(targetName);
                case MAPPINGS_NOT_APPLIED ->
                        assertThat(organizations)
                                .withFailMessage("Mapping for %s was executed even if it should not be", targetName)
                                .doesNotContain(targetName);
                case AUTHORIZATIONS_PRESENT ->
                        assertThat(autzActions)
                                .withFailMessage("Authorization for %s is not present", targetName)
                                .contains(targetName);
                case AUTHORIZATIONS_NOT_PRESENT ->
                        assertThat(autzActions)
                                .withFailMessage("Authorization for %s is present even if it should not be", targetName)
                                .doesNotContain(targetName);
                case OBJECT_POLICY_RULES_APPLIED -> OBJECT_CONSTRAINTS.assertPresent(targetName);
                case OBJECT_POLICY_RULES_NOT_APPLIED -> OBJECT_CONSTRAINTS.assertNotPresent(targetName);
                case ASSIGNMENT_POLICY_RULES_APPLIED -> ASSIGNMENT_CONSTRAINTS.assertPresent(targetName);
                case ASSIGNMENT_POLICY_RULES_NOT_APPLIED -> ASSIGNMENT_CONSTRAINTS.assertNotPresent(targetName);
                case ACCOUNTS_PRESENT ->
                        getDummyResourceController("resource-" + targetName)
                                .assertAccountByUsername(userName);
                case ACCOUNTS_NOT_PRESENT ->
                        getDummyResourceController("resource-" + targetName)
                                .assertNoAccountByUsername(userName);
                default -> throw new AssertionError(assertion);
            }
        }
    }

    private void assertResourceAssignment(
            UserType user, DummyTestResource resource,
            LocalAssertion... assertions) throws Exception {

        String userName = user.getName().getOrig();
        String resourceName = resource.getNameOrig();

        for (LocalAssertion assertion : assertions) {
            switch (assertion) {
                case ASSIGNMENT_ENABLED, ASSIGNMENT_DISABLED ->
                        assertUser(user, "after")
                                .assignments()
                                .by().resourceOid(resource.oid).find()
                                .activation()
                                .assertEffectiveStatus(
                                        assertion == ASSIGNMENT_ENABLED ?
                                                ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
                case ACCOUNTS_PRESENT ->
                        getDummyResourceController(resourceName)
                                .assertAccountByUsername(userName);
                case ACCOUNTS_NOT_PRESENT ->
                        getDummyResourceController(resourceName)
                                .assertNoAccountByUsername(userName);
                default -> throw new AssertionError(assertion);
            }
        }
    }

    enum LocalAssertion {
        /** User's effective status is "enabled". */
        USER_ENABLED,

        /** User's effective status is "disabled". */
        USER_DISABLED,

        /**
         * Assignments do their job according to their status, see {@link #assertAssignmentsActive(UserType, MidPointPrincipal)}.
         */
        ASSIGNMENTS_ACTIVE,

        /**
         * Assignments do not do their job, see {@link #assertAssignmentsInactive(UserType, MidPointPrincipal)}.
         */
        ASSIGNMENTS_INACTIVE,

        /** Assignment's effective status is "enabled". */
        ASSIGNMENT_ENABLED,

        /** Assignment's effective status is "disabled". */
        ASSIGNMENT_DISABLED,

        /** Assignment's target is in "roleMembershipRef". */
        ROLE_MEMBERSHIP_REF_PRESENT,

        /** Assignment's target is not in "roleMembershipRef". */
        ROLE_MEMBERSHIP_REF_NOT_PRESENT,

        /** Assignment's target is in "archetypeRef". */
        ARCHETYPE_REF_PRESENT,

        /** Assignment's target is not in "archetypeRef". */
        ARCHETYPE_REF_NOT_PRESENT,

        /** Mappings induced by the assignment target are applied. */
        MAPPINGS_APPLIED,

        /** Mappings induced by the assignment target are not applied. */
        MAPPINGS_NOT_APPLIED,

        /** Authorizations present in assignment target are applied. */
        AUTHORIZATIONS_PRESENT,

        /** Authorizations present in assignment target are not applied. */
        AUTHORIZATIONS_NOT_PRESENT,

        /** Policy rules targeted to the object (focus) are applied. */
        OBJECT_POLICY_RULES_APPLIED,

        /** Policy rules targeted to the object (focus) are not applied. */
        OBJECT_POLICY_RULES_NOT_APPLIED,

        /** Policy rules targeted to the assignment are applied. */
        ASSIGNMENT_POLICY_RULES_APPLIED,

        /** Policy rules targeted to the assignment are not applied. */
        ASSIGNMENT_POLICY_RULES_NOT_APPLIED,

        /** Accounts assigned or induced are present. */
        ACCOUNTS_PRESENT,

        /** Accounts assigned or induced are not present. */
        ACCOUNTS_NOT_PRESENT,

        /** Global policy rules are applied. */
        GLOBAL_POLICY_RULES_APPLIED
    }
}
