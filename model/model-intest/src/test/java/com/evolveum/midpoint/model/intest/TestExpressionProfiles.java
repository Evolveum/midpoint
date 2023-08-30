/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.model.api.BulkActionExecutionOptions;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.BulkActionsService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.RunFlag;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

/**
 * Tests the use of expression profiles in various contexts.
 *
 * . `test1xx`: auto-assigned roles
 * . `test2xx`: various expressions in explicitly-assigned roles
 * . `test3xx`: bulk actions
 *
 * Expression profiles:
 *
 * . `restricted` - allows a lot of expressions, except with some limitations on Groovy method calls
 * . `trusted` - no restrictions whatsoever
 * . `little-trusted` - allows almost nothing; just an invocation of one specifically trusted library function
 * . `little-trusted-variant` - as before, but allows a different function
 * . `little-trusted-variant-two` - as before, but allows a different function library (`two`)
 * . `forbidden-generate-value-action`, `forbidden-generate-value-action-alt` - allows everything except `generate-value` action
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestExpressionProfiles extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "profiles");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ValuePolicyType> VALUE_POLICY_TEST = TestObject.file(
            TEST_DIR, "value-policy-test.xml", "cdea9694-1882-424e-abe6-70941e8fc882");
    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_ONE = TestObject.file(
            TEST_DIR, "function-library-one.xml", "8752dce7-432a-48ad-aa10-1d4deb31dcba");
    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_TWO = TestObject.file(
            TEST_DIR, "function-library-two.xml", "f28e0119-5dfe-4e6f-92d7-3a1ad5b1cc91");

    private static final DummyTestResource RESOURCE_SIMPLE_TARGET = new DummyTestResource(
            TEST_DIR, "resource-simple-target.xml", "2003a0c3-62a3-413d-9941-6fecaef84a16", "simple-target");

    private static final TestObject<RoleType> METAROLE_DUMMY = TestObject.file(
            TEST_DIR, "metarole-dummy.xml", "d9a263b7-b272-46d8-84dc-cdf96d79128e");
    private static final TestObject<RoleType> ROLE_UNRESTRICTED = TestObject.file(
            TEST_DIR, "role-unrestricted.xml", "ffe619f0-a7e7-4803-b50a-8a10eed96bf9");
    private static final TestObject<RoleType> ROLE_SCRIPTING = TestObject.file(
            TEST_DIR, "role-scripting.xml", "16c7a275-83b5-44c7-9b37-0114a56efb2b");

    private static final TestObject<UserType> USER_JOE = TestObject.file(
            TEST_DIR, "user-joe.xml", "562d5f0a-1a5a-4751-b0ee-6ef7b928944d");

    private static final TestObject<ArchetypeType> ARCHETYPE_RESTRICTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-restricted-role.xml", "a2242707-43cd-4f18-b986-573cb468693d");
    private static final TestObject<ArchetypeType> ARCHETYPE_NO_PRIVILEGE_ELEVATION = TestObject.file(
            TEST_DIR, "archetype-no-privilege-elevation.xml", "f2d01dd2-50b4-4d4d-babd-e00671923f2c");
    private static final TestObject<ArchetypeType> ARCHETYPE_TRUSTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-trusted-role.xml", "b162bbaa-d7f4-42ff-9f9c-754d495f9e52");
    private static final TestObject<ArchetypeType> ARCHETYPE_LITTLE_TRUSTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-little-trusted-role.xml", "11733320-82a1-4a48-84d5-c7b551446a0b");
    private static final TestObject<ArchetypeType> ARCHETYPE_LITTLE_TRUSTED_VARIANT_ROLE = TestObject.file(
            TEST_DIR, "archetype-little-trusted-variant-role.xml", "3f765457-3078-4759-ba5f-816a97a92f39");
    private static final TestObject<ArchetypeType> ARCHETYPE_LITTLE_TRUSTED_VARIANT_TWO_ROLE = TestObject.file(
            TEST_DIR, "archetype-little-trusted-variant-two-role.xml", "ae434551-4b41-4258-babb-e169b08ec27e");
    private static final TestObject<ArchetypeType> ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ROLE = TestObject.file(
            TEST_DIR, "archetype-forbidden-generate-value-action-role.xml", "f7714bb8-242e-41c7-bb81-d9cf78415873");
    private static final TestObject<ArchetypeType> ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ALT_ROLE = TestObject.file(
            TEST_DIR, "archetype-forbidden-generate-value-action-alt-role.xml", "b51a1997-d526-4d45-90d1-e978856e2630");

    // auto-assigned roles are initialized only in specific tests (and then deleted)
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_GOOD = TestObject.file(
            TEST_DIR, "role-restricted-auto-good.xml", "a6ace69f-ecfb-457b-97ea-0b5a8b4a6ad3");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION = TestObject.file(
            TEST_DIR, "role-restricted-auto-filter-expression.xml", "885bc0b4-2493-4658-a523-8a3f7eeb770b");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION = TestObject.file(
            TEST_DIR, "role-restricted-auto-bad-mapping-expression.xml", "26f61dc6-efff-4614-aac6-25753b81512f");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION = TestObject.file(
            TEST_DIR, "role-restricted-auto-bad-mapping-condition.xml", "eceab160-de05-4b1b-9d09-27cc789252c3");

    private static final TestObject<RoleType> ROLE_RESTRICTED_GOOD = TestObject.file(
            TEST_DIR, "role-restricted-good.xml", "ca8c4ffd-98ed-4ca7-9097-44db924155c9");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_FOCUS_MAPPING = TestObject.file(
            TEST_DIR, "role-restricted-bad-focus-mapping.xml", "eee455ef-312c-4bc7-a541-3add09d8e90d");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING = TestObject.file(
            TEST_DIR, "role-restricted-bad-construction-mapping.xml", "c8cae775-2c3b-49bb-98e3-482a095316ef");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_ASSIGNMENT_CONDITION = TestObject.file(
            TEST_DIR, "role-restricted-bad-assignment-condition.xml", "4e373fa2-ceed-4edc-90b2-6379ee4f6bf0");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_INDUCEMENT_CONDITION = TestObject.file(
            TEST_DIR, "role-restricted-bad-inducement-condition.xml", "6f859e0b-e42e-4546-b446-645d48542b4f");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_ROLE_CONDITION = TestObject.file(
            TEST_DIR, "role-restricted-bad-role-condition.xml", "baa44eab-8057-4e19-88f9-dd83085df38a");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_ASSIGNMENT_TARGET_FILTER = TestObject.file(
            TEST_DIR, "role-restricted-bad-assignment-target-filter.xml", "eb318451-4774-405a-b2a1-bb05da7842c9");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_INDUCEMENT_TARGET_FILTER = TestObject.file(
            TEST_DIR, "role-restricted-bad-inducement-target-filter.xml", "bae5a90d-87c0-44f8-a585-0ea11e42ee9a");
    private static final TestObject<RoleType> ROLE_NO_ELEVATION_ASSIGNMENT_TARGET_SEARCH_FILTER = TestObject.file(
            TEST_DIR, "role-no-elevation-assignment-target-search-filter.xml", "69d783c8-8b59-4b2f-988f-db6097b828c2");

    private static final File FILE_SCRIPTING_EXECUTE_SCRIPT = new File(TEST_DIR, "scripting-execute-script.xml");
    private static final File FILE_SCRIPTING_EXPRESSION_EXECUTE_SCRIPT = new File(TEST_DIR, "scripting-expression-execute-script.xml");
    private static final File FILE_SCRIPTING_NOTIFICATION_CUSTOM_HANDLER = new File(TEST_DIR, "scripting-notification-custom-handler.xml");
    private static final File FILE_SCRIPTING_SCRIPT_IN_QUERY = new File(TEST_DIR, "scripting-script-in-query.xml");
    private static final File FILE_SCRIPTING_SCRIPT_IN_UNASSIGN_FILTER = new File(TEST_DIR, "scripting-script-in-unassign-filter.xml");

    private static final File FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION = new File(TEST_DIR, "scripting-execute-simpleTrustedFunction.xml");
    private static final File FILE_SCRIPTING_GENERATE_VALUE = new File(TEST_DIR, "scripting-generate-value.xml");

    private static final String DETAIL_REASON_MESSAGE_BOOM_RESTRICTED =
            "Access to Groovy method com.evolveum.midpoint.model.intest.TestExpressionProfiles#boom denied"
                    + " (applied expression profile 'restricted')";

    private static final RunFlag BOOMED_FLAG = new RunFlag();

    private static final String ID_EMPTY = "empty";

    @Autowired private BulkActionsService bulkActionsService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SIMPLE_TARGET.initAndTest(this, initTask, initResult);

        initTestObjects(initTask, initResult,
                VALUE_POLICY_TEST,
                FUNCTION_LIBRARY_ONE,
                FUNCTION_LIBRARY_TWO,
                ARCHETYPE_RESTRICTED_ROLE,
                ARCHETYPE_NO_PRIVILEGE_ELEVATION,
                ARCHETYPE_TRUSTED_ROLE,
                ARCHETYPE_LITTLE_TRUSTED_ROLE,
                ARCHETYPE_LITTLE_TRUSTED_VARIANT_ROLE,
                ARCHETYPE_LITTLE_TRUSTED_VARIANT_TWO_ROLE,
                ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ROLE,
                ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ALT_ROLE,
                METAROLE_DUMMY,
                ROLE_UNRESTRICTED,
                ROLE_SCRIPTING,
                USER_JOE,
                ROLE_RESTRICTED_GOOD,
                ROLE_RESTRICTED_BAD_FOCUS_MAPPING,
                ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING,
                ROLE_RESTRICTED_BAD_INDUCEMENT_CONDITION, // seemingly does not evaluate inducement here
                ROLE_RESTRICTED_BAD_ROLE_CONDITION, // the same here
                ROLE_RESTRICTED_BAD_INDUCEMENT_TARGET_FILTER, // same here
                ROLE_NO_ELEVATION_ASSIGNMENT_TARGET_SEARCH_FILTER);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /** "Correct" restricted auto-assigned role is used. */
    @Test
    public void test100RestrictedRoleAutoGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_GOOD.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name(getTestNameShort())
                    .costCenter("auto");
            var userOid = addObject(user.asPrismObject(), task, result);

            then("user is created");
            assertSuccess(result);
            assertUserAfter(userOid)
                    .assignments()
                    .single()
                    .assertRole(ROLE_RESTRICTED_AUTO_GOOD.oid);
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_GOOD.oid);
        }

        // only by mistake, as the role does not involve such a call
        BOOMED_FLAG.assertNotSet();
    }

    /** This checks that filter expressions are not supported - hence, safe. :) */
    @Test
    public void test110RestrictedRoleAutoFilterExpression() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name(getTestNameShort());
            var userOid = addObject(user.asPrismObject(), task, result);

            then("user is created but without assignments");
            assertSuccess(result);
            assertUserAfter(userOid)
                    .assertAssignments(0);
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION.oid);
        }

        BOOMED_FLAG.assertNotSet();
    }

    /** Non-compliant script in mapping expression. */
    @Test
    public void test120RestrictedRoleAutoBadMappingExpression() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name(getTestNameShort())
                    .costCenter("auto");
            try {
                addObject(user.asPrismObject(), task, result);
                fail("unexpected success");
            } catch (SecurityViolationException e) {
                assertExpectedException(e)
                        .hasMessageContaining("Denied access to functionality of script in expression in mapping in autoassign mapping")
                        .hasMessageContaining(DETAIL_REASON_MESSAGE_BOOM_RESTRICTED);
                assertLocation(
                        e,
                        ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION,
                        ItemPath.create(
                                RoleType.F_AUTOASSIGN,
                                AutoassignSpecificationType.F_FOCUS,
                                FocalAutoassignSpecificationType.F_MAPPING,
                                123L));

                assertFailure(result);
            }
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION.oid);
        }

        BOOMED_FLAG.assertNotSet();
    }

    /** Non-compliant script in mapping condition. */
    @Test
    public void test130RestrictedRoleAutoBadMappingCondition() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name(getTestNameShort())
                    .costCenter("auto");
            try {
                addObject(user.asPrismObject(), task, result);
                fail("unexpected success");
            } catch (SecurityViolationException e) {
                assertExpectedException(e)
                        .hasMessageContaining("Denied access to functionality of script in condition in mapping in autoassign mapping")
                        .hasMessageContaining(DETAIL_REASON_MESSAGE_BOOM_RESTRICTED);
                assertLocation(
                        e,
                        ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION,
                        ItemPath.create(
                                RoleType.F_AUTOASSIGN,
                                AutoassignSpecificationType.F_FOCUS,
                                FocalAutoassignSpecificationType.F_MAPPING,
                                456L));

                assertFailure(result);
            }
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION.oid);
        }

        resetBoomed();
    }

    /** "Correct" restricted role is used. */
    @Test
    public void test200RestrictedRoleGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("user with correct restricted role is added");
        String name = getTestNameShort();
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_RESTRICTED_GOOD.assignmentTo());
        var userOid = addObject(user.asPrismObject(), task, result);

        then("user is created");
        assertSuccess(result);
        assertUserAfter(userOid)
                .assertDescription("My name is '" + name + "'")
                .assertLiveLinks(1);
        assertDummyAccountByUsername(RESOURCE_SIMPLE_TARGET.name, name)
                .display();
    }

    /** "Incorrect" restricted role is used: bad focus mapping. */
    @Test
    public void test210RestrictedRoleBadFocusMapping() throws Exception {
        runNegativeRoleAssignmentTest(ROLE_RESTRICTED_BAD_FOCUS_MAPPING, null); // FIXME path
    }

    private void runNegativeRoleAssignmentTest(
            @NotNull TestObject<RoleType> role, @Nullable ItemPath expectedPath)
            throws CommonException {
        runNegativeRoleAssignmentTest(
                role, expectedPath, "Denied access to functionality of script", DETAIL_REASON_MESSAGE_BOOM_RESTRICTED);
    }
    private void runNegativeRoleAssignmentTest(
            @NotNull TestObject<RoleType> role, @Nullable ItemPath expectedPath, @NotNull String msg1, @NotNull String msg2)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        when("user with a non-compliant role is created");
        UserType user = new UserType()
                .name(getTestNameShort())
                .assignment(role.assignmentTo());
        try {
            addObject(user.asPrismObject(), task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining(msg1)
                    .hasMessageContaining(msg2);
            assertLocation(e, role, expectedPath);
            assertFailure(result);
        }

        BOOMED_FLAG.assertNotSet();
    }

    /** "Incorrect" restricted role is used: bad mapping in construction. */
    @Test
    public void test220RestrictedRoleBadConstructionMapping() throws Exception {
        runNegativeRoleAssignmentTest(ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING, null); // FIXME path
    }

    /** Bad condition in metarole assignment. This time it is impossible even to create the role. */
    @Test
    public void test230RestrictedRoleBadAssignmentCondition() throws Exception {
        runNegativeRoleCreationTest(ROLE_RESTRICTED_BAD_ASSIGNMENT_CONDITION);
    }

    private void runNegativeRoleCreationTest(TestObject<RoleType> role) throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        resetBoomed();

        then("non-compliant role is NOT created");
        try {
            addObject(role.get(), task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Denied access to functionality of script")
                    .hasMessageContaining("in delta for role:") // we are adding the whole object
                    .hasMessageContaining(DETAIL_REASON_MESSAGE_BOOM_RESTRICTED);
            // Note that the assignment path is not fully correct here. It is "assignment[xxx]" as per the "object new".
            // However, due to complex processing of assignments, the value gets into a "assignment delta" and thus the
            // information about the ID is distorted.
            assertLocation(e, role, RoleType.F_ASSIGNMENT);
            assertFailure(result);
        }

        BOOMED_FLAG.assertNotSet();

        and("the object is really not in repo");
        assertObjectDoesntExist(RoleType.class, role.oid);
    }

    /**
     * The bad role was added into the repository. We check we are not able to evaluate it.
     */
    @Test
    public void test235RestrictedRoleBadAssignmentConditionAlreadyInRepo() throws Exception {
        given("role is put into the repo in raw mode (not evaluating expressions)");
        ROLE_RESTRICTED_BAD_ASSIGNMENT_CONDITION.initRaw(this, getTestOperationResult());

        then("the expressions should not be evaluable");
        runNegativeRoleAssignmentTest(
                ROLE_RESTRICTED_BAD_ASSIGNMENT_CONDITION,
                RoleType.F_ASSIGNMENT.append(111L));
    }

    /** Bad condition in inducement. */
    @Test
    public void test240RestrictedRoleBadInducementCondition() throws Exception {
        runNegativeRoleAssignmentTest(
                ROLE_RESTRICTED_BAD_INDUCEMENT_CONDITION,
                RoleType.F_INDUCEMENT.append(111L));
    }

    /** Bad condition in role itself. */
    @Test
    public void test245RestrictedRoleBadRoleCondition() throws Exception {
        runNegativeRoleAssignmentTest(
                ROLE_RESTRICTED_BAD_ROLE_CONDITION, null); // FIXME path
    }

    /** Bad expression in assignment `targetRef` filter. Role cannot be created. */
    @Test
    public void test250RestrictedRoleBadAssignmentTargetFilter() throws Exception {
        runNegativeRoleCreationTest(
                ROLE_RESTRICTED_BAD_ASSIGNMENT_TARGET_FILTER); // FIXME path
    }

    /** Bad expression in assignment `targetRef` filter. Role was put into repo in raw mode. */
    @Test
    public void test255RestrictedRoleBadAssignmentTargetFilterAlreadyInRepo() throws Exception {
        given("role is put into the repo in raw mode (not evaluating expressions)");
        ROLE_RESTRICTED_BAD_ASSIGNMENT_TARGET_FILTER.initRaw(this, getTestOperationResult());

        then("the expressions should not be evaluable");
        runNegativeRoleAssignmentTest(
                ROLE_RESTRICTED_BAD_ASSIGNMENT_TARGET_FILTER, null); // FIXME path
    }

    /** Bad expression in inducement `targetRef` filter. */
    @Test
    public void test260RestrictedRoleBadInducementTargetFilter() throws Exception {
        runNegativeRoleAssignmentTest(
                ROLE_RESTRICTED_BAD_INDUCEMENT_TARGET_FILTER,
                RoleType.F_INDUCEMENT.append(333L));
    }

    /** Bad `inducement/focusMappings/mapping/expression/assignmentTargetSearch/filter` filter. Should fail. */
    @Test
    public void test265NoElevationRoleBadAssignmentTargetSearchFilter() throws Exception {
        runNegativeRoleAssignmentTest(
                ROLE_NO_ELEVATION_ASSIGNMENT_TARGET_SEARCH_FILTER,
                null,
                "Access to privilege elevation feature denied",
                "expression profile 'no-privilege-elevation'");
    }

    /** Executing script directly with the `trusted` origin. Should succeed. */
    @Test
    public void test300BulkActionWithKnownOrigin() throws CommonException, IOException {
        runPositiveBulkActionTest(FILE_SCRIPTING_EXECUTE_SCRIPT, originForArchetype(ARCHETYPE_TRUSTED_ROLE));
    }

    /** Executing script directly (with the default profile). Should fail. */
    @Test
    public void test310UntrustedBulkExecutingScriptDirectly() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_EXECUTE_SCRIPT,
                ConfigurationItemOrigin.rest(),
                "Access to script expression evaluator not allowed",
                "expression profile: ##legacyUnprivilegedBulkActions");
    }

    /** Executing script via expression (with the default profile). Should fail. */
    @Test
    public void test315UntrustedBulkExecutingScriptViaExpression() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_EXPRESSION_EXECUTE_SCRIPT,
                ConfigurationItemOrigin.rest(),
                "Access to script expression evaluator not allowed",
                "expression profile: ##legacyUnprivilegedBulkActions");
    }

    /** Executing script via notification (with the default profile). Should fail. */
    @Test
    public void test320UntrustedBulkExecutingScriptViaNotification() throws CommonException, IOException {
        runNegativeNotificationBulkActionTest(FILE_SCRIPTING_NOTIFICATION_CUSTOM_HANDLER);
    }

    /** Executing script via search filter (with the default profile). Should fail. */
    @Test
    public void test325UntrustedBulkExecutingScriptViaSearchFilter() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_SCRIPT_IN_QUERY,
                ConfigurationItemOrigin.rest(),
                "Access to script expression evaluator not allowed",
                "expression profile: ##legacyUnprivilegedBulkActions");
    }

    /** Executing script via filter in `unassign` action. Should fail. */
    @Test
    public void test330UntrustedBulkExecutingScriptViaUnassignFilter() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_SCRIPT_IN_UNASSIGN_FILTER,
                ConfigurationItemOrigin.rest(),
                "Access to script expression evaluator not allowed",
                "expression profile: ##legacyUnprivilegedBulkActions");
    }

    /**
     * Executing script in "allowed" trusted library function (`simpleTrustedFunction` call is allowed
     * by `little-trusted` profile. Should succeed.
     */
    @Test
    public void test350LittleTrustedLibraryCall() throws CommonException, IOException {
        runPositiveBulkActionTest(
                FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION,
                originForArchetype(ARCHETYPE_LITTLE_TRUSTED_ROLE));
    }

    /**
     * Executing script in "not allowed" trusted library function (`simpleTrustedFunction` call is not allowed
     * by `little-trusted-variant` profile. Should fail.
     */
    @Test
    public void test355LittleTrustedVariantLibraryCall() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION,
                originForArchetype(ARCHETYPE_LITTLE_TRUSTED_VARIANT_ROLE),
                "Access to function library method simpleTrustedFunction",
                "expression profile 'little-trusted-variant', libraries profile 'little-trusted-variant'");
    }

    /**
     * Executing script in "not allowed" trusted library function (library `one` is not allowed
     * by `little-trusted-variant-two` profile. Should fail.
     */
    @Test
    public void test358LittleTrustedVariantTwoLibraryCall() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION,
                originForArchetype(ARCHETYPE_LITTLE_TRUSTED_VARIANT_TWO_ROLE),
                "Access to function library method simpleTrustedFunction",
                "expression profile 'little-trusted-variant-two', libraries profile 'little-trusted-variant-two'");
    }


    /**
     * Executing `generate-value` is allowed by the default profile. Just a baseline test. Should succeed.
     */
    @Test
    public void test360GenerateValueAllowed() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        when("bulk action is executed");
        var script = prismContext.parserFor(FILE_SCRIPTING_GENERATE_VALUE)
                .xml()
                .parseRealValue(ExecuteScriptType.class);
        var executionResult = bulkActionsService.executeBulkAction(
                ExecuteScriptConfigItem.of(script, ConfigurationItemOrigin.rest()),
                VariablesMap.emptyMap(),
                BulkActionExecutionOptions.create(),
                task, result);
        assertSuccess(result);

        displayCollection("output", executionResult.getDataOutput());
        //noinspection unchecked
        var userAfter = (PrismObjectValue<UserType>) executionResult.getDataOutput().get(0).getValue();
        assertThat(userAfter.asObjectable().getEmployeeNumber()).as("generated emp#").isNotNull();
    }

    /**
     * Executing `generate-value` is forbidden by the `forbidden-generate-value-action` profile.
     */
    @Test
    public void test364GenerateValueForbidden() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_GENERATE_VALUE,
                originForArchetype(ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ROLE),
                "Access to action 'generate-value' ('generateValue')",
                "expression profile 'forbidden-generate-value-action', actions profile 'forbidden-generate-value-action'");
    }

    /**
     * Executing `generate-value` is forbidden by the `forbidden-generate-value-action-alt` profile.
     */
    @Test
    public void test368GenerateValueForbiddenAlt() throws CommonException, IOException {
        runNegativeBulkActionTest(
                FILE_SCRIPTING_GENERATE_VALUE,
                originForArchetype(ARCHETYPE_FORBIDDEN_GENERATE_VALUE_ACTION_ALT_ROLE),
                "Access to action 'generate-value' ('generateValue')",
                "expression profile 'forbidden-generate-value-action-alt', actions profile 'forbidden-generate-value-action-alt'");
    }

    /**
     * Tests "defaults" setting for bulk actions expression profiles.
     *
     * Must be run as the last test, as it modifies the system configuration object.
     */
    @Test
    public void test999BulkActionsDefaults() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        setDefaultBulkActionProfiles(ID_EMPTY, ID_EMPTY, result);

        when("*** testing for unprivileged user");
        login(USER_JOE.getNameOrig());
        runNegativeBulkActionTestLoggedIn(
                FILE_SCRIPTING_GENERATE_VALUE,
                ConfigurationItemOrigin.rest(),
                "Access to action 'search'",
                "expression profile 'empty', actions profile 'empty'");

        when("*** testing for privileged user");
        login(USER_ADMINISTRATOR_USERNAME);
        runNegativeBulkActionTestLoggedIn(
                FILE_SCRIPTING_GENERATE_VALUE,
                ConfigurationItemOrigin.rest(),
                "Access to action 'search'",
                "expression profile 'empty', actions profile 'empty'");
    }

    @SuppressWarnings("SameParameterValue")
    private void setDefaultBulkActionProfiles(String unprivileged, String privileged, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        repositoryService.modifyObject(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_EXPRESSIONS,
                                SystemConfigurationExpressionsType.F_DEFAULTS,
                                DefaultExpressionProfilesConfigurationType.F_BULK_ACTIONS)
                        .replace(unprivileged)
                        .item(SystemConfigurationType.F_EXPRESSIONS,
                                SystemConfigurationExpressionsType.F_DEFAULTS,
                                DefaultExpressionProfilesConfigurationType.F_PRIVILEGED_BULK_ACTIONS)
                        .replace(privileged)
                        .asItemDeltas(),
                result);
    }

    private void runPositiveBulkActionTest(File file, ConfigurationItemOrigin origin) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        when("bulk action is executed");
        var script = prismContext.parserFor(file).xml().parseRealValue(ExecuteScriptType.class);
        bulkActionsService.executeBulkAction(
                ExecuteScriptConfigItem.of(script, origin),
                VariablesMap.emptyMap(),
                BulkActionExecutionOptions.create(),
                task, result);
        assertSuccess(result);

        and("'boomed' flag is set");
        BOOMED_FLAG.assertSet();
    }

    private ConfigurationItemOrigin originForArchetype(TestObject<ArchetypeType> archetype) {
        return ConfigurationItemOrigin.inObject(
                        new RoleType()
                                .assignment(archetype.assignmentTo())
                                .archetypeRef(archetype.ref())
                                .roleMembershipRef(archetype.ref()),
                        ItemPath.EMPTY_PATH)
                .toApproximate();
    }

    private void runNegativeBulkActionTest(File file, ConfigurationItemOrigin origin, String msg1, String msg2)
            throws CommonException, IOException {
        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        runNegativeBulkActionTestLoggedIn(file, origin, msg1, msg2);
    }
    private void runNegativeBulkActionTestLoggedIn(
            File file, ConfigurationItemOrigin origin, String msg1, String msg2)
            throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when("dangerous bulk action is executed");
        var script = prismContext.parserFor(file).xml().parseRealValue(ExecuteScriptType.class);
        try {
            bulkActionsService.executeBulkAction(
                    ExecuteScriptConfigItem.of(script, origin),
                    VariablesMap.emptyMap(),
                    BulkActionExecutionOptions.create(),
                    task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining(msg1)
                    .hasMessageContaining(msg2);
        }

        // checking this, although not all bulk actions set this flag
        and("not boomed");
        BOOMED_FLAG.assertNotSet();
    }

    /**
     * Different from {@link #runNegativeBulkActionTest(File, ConfigurationItemOrigin, String, String)} in that
     * the exception is not thrown (notifications don't do that)
     */
    @SuppressWarnings("SameParameterValue")
    private void runNegativeNotificationBulkActionTest(File file) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        when("dangerous bulk action is executed");
        var script = prismContext.parserFor(file).xml().parseRealValue(ExecuteScriptType.class);
        bulkActionsService.executeBulkAction(
                ExecuteScriptConfigItem.of(
                        script,
                        ConfigurationItemOrigin.rest()),
                VariablesMap.emptyMap(),
                BulkActionExecutionOptions.create(),
                task, result);

        then("not boomed");
        BOOMED_FLAG.assertNotSet();

        and("result is an error");
        assertFailure(result);

        // these asserts may be fragile
        assertThat(result.getMessage())
                .contains("Access to script expression evaluator not allowed")
                .contains("expression profile: ##legacyUnprivilegedBulkActions")
                .contains("in event filter expression");
    }

    // TODO what about import-time resolution? Is that even supported? Probably not but we should write a test for it.
    //  And what about deltas?

    public static void boom() {
        // We intentionally do not throw an exception here
        BOOMED_FLAG.set();
    }

    private static void resetBoomed() {
        BOOMED_FLAG.reset();
    }

    // TODO move upwards
    private void assertLocation(
            @NotNull Throwable e, @Nullable TestObject<?> object, @Nullable ItemPath itemPath) {
        String message = e.getMessage();
        var asserter = assertThat(message).as("exception message");
        if (object != null) {
            asserter.contains(object.getNameOrig());
            if (object.oid != null) {
                asserter.contains(object.oid);
            }
        }
        if (itemPath != null) {
            asserter.contains(itemPath.toString());
        }
    }
}
