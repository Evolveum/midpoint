/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.test.RunFlag;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

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
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestExpressionProfiles extends AbstractEmptyModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestExpressionProfiles.class);

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "profiles");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_ONE = TestObject.file(
            TEST_DIR, "function-library-one.xml", "8752dce7-432a-48ad-aa10-1d4deb31dcba");

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
    private static final TestObject<ArchetypeType> ARCHETYPE_TRUSTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-trusted-role.xml", "b162bbaa-d7f4-42ff-9f9c-754d495f9e52");
    private static final TestObject<ArchetypeType> ARCHETYPE_LITTLE_TRUSTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-little-trusted-role.xml", "11733320-82a1-4a48-84d5-c7b551446a0b");

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

    private static final File FILE_SCRIPTING_EXECUTE_SCRIPT = new File(TEST_DIR, "scripting-execute-script.xml");
    private static final File FILE_SCRIPTING_EXPRESSION_EXECUTE_SCRIPT = new File(TEST_DIR, "scripting-expression-execute-script.xml");
    private static final File FILE_SCRIPTING_NOTIFICATION_CUSTOM_HANDLER = new File(TEST_DIR, "scripting-notification-custom-handler.xml");
    private static final File FILE_SCRIPTING_SCRIPT_IN_QUERY = new File(TEST_DIR, "scripting-script-in-query.xml");
    private static final File FILE_SCRIPTING_SCRIPT_IN_UNASSIGN_FILTER = new File(TEST_DIR, "scripting-script-in-unassign-filter.xml");

    private static final File FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION = new File(TEST_DIR, "scripting-execute-simpleTrustedFunction.xml");

    private static final String DETAIL_REASON_MESSAGE =
            "Access to Groovy method com.evolveum.midpoint.model.intest.TestExpressionProfiles#boom denied"
                    + " (applied expression profile 'restricted')";

    private static final RunFlag BOOMED_FLAG = new RunFlag();

    @Autowired private ScriptingService scriptingService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SIMPLE_TARGET.initAndTest(this, initTask, initResult);

        initTestObjects(initTask, initResult,
                FUNCTION_LIBRARY_ONE,
                ARCHETYPE_RESTRICTED_ROLE,
                ARCHETYPE_TRUSTED_ROLE,
                ARCHETYPE_LITTLE_TRUSTED_ROLE,
                METAROLE_DUMMY,
                ROLE_UNRESTRICTED,
                ROLE_SCRIPTING,
                USER_JOE,
                ROLE_RESTRICTED_GOOD,
                ROLE_RESTRICTED_BAD_FOCUS_MAPPING,
                ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING,
                ROLE_RESTRICTED_BAD_INDUCEMENT_CONDITION, // seemingly does not evaluate inducement here
                ROLE_RESTRICTED_BAD_ROLE_CONDITION, // the same here
                ROLE_RESTRICTED_BAD_INDUCEMENT_TARGET_FILTER); // same here
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
                        .hasMessageContaining(DETAIL_REASON_MESSAGE);
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
                        .hasMessageContaining(DETAIL_REASON_MESSAGE);
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
        UserType user = new UserType()
                .name("test100")
                .assignment(ROLE_RESTRICTED_GOOD.assignmentTo());
        var userOid = addObject(user.asPrismObject(), task, result);

        then("user is created");
        assertSuccess(result);
        assertUserAfter(userOid)
                .assertDescription("My name is 'test100'")
                .assertLiveLinks(1);
        assertDummyAccountByUsername(RESOURCE_SIMPLE_TARGET.name, "test100")
                .display();
    }

    /** "Incorrect" restricted role is used: bad focus mapping. */
    @Test
    public void test210RestrictedRoleBadFocusMapping() throws Exception {
        runNegativeRoleAssignmentTest(ROLE_RESTRICTED_BAD_FOCUS_MAPPING, null); // FIXME path
    }

    private void runNegativeRoleAssignmentTest(
            @NotNull TestObject<RoleType> role, @Nullable ItemPath expectedPath) throws CommonException {
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
                    .hasMessageContaining("Denied access to functionality of script")
                    .hasMessageContaining(DETAIL_REASON_MESSAGE);
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
                    .hasMessageContaining(DETAIL_REASON_MESSAGE);
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

    /** Executing script directly with the `trusted` origin. Should succeed. */
    @Test
    public void test300BulkActionWithKnownOrigin() throws CommonException, IOException {
        runPositiveBulkActionTest(FILE_SCRIPTING_EXECUTE_SCRIPT, ARCHETYPE_TRUSTED_ROLE);
    }

    /** Executing script directly (with the default profile). Should fail. */
    @Test
    public void test310UntrustedBulkExecutingScriptDirectly() throws CommonException, IOException {
        runNegativeBulkActionTest(FILE_SCRIPTING_EXECUTE_SCRIPT);
    }

    /** Executing script via expression (with the default profile). Should fail. */
    @Test
    public void test315UntrustedBulkExecutingScriptViaExpression() throws CommonException, IOException {
        runNegativeBulkActionTest(FILE_SCRIPTING_EXPRESSION_EXECUTE_SCRIPT);
    }

    /** Executing script via notification (with the default profile). Should fail. */
    @Test
    public void test320UntrustedBulkExecutingScriptViaNotification() throws CommonException, IOException {
        runNegativeNotificationBulkActionTest(FILE_SCRIPTING_NOTIFICATION_CUSTOM_HANDLER);
    }

    /** Executing script via search filter (with the default profile). Should fail. */
    @Test
    public void test325UntrustedBulkExecutingScriptViaSearchFilter() throws CommonException, IOException {
        runNegativeBulkActionTest(FILE_SCRIPTING_SCRIPT_IN_QUERY);
    }

    /** Executing script via filter in `unassign` action. Should fail. */
    @Test
    public void test330UntrustedBulkExecutingScriptViaUnassignFilter() throws CommonException, IOException {
        runNegativeBulkActionTest(FILE_SCRIPTING_SCRIPT_IN_UNASSIGN_FILTER);
    }

    /** Executing script in trusted library function. Should succeed. */
    @Test
    public void test350LittleTrustedLibraryCall() throws CommonException, IOException {
        runPositiveBulkActionTest(FILE_SCRIPTING_EXECUTE_SIMPLE_TRUSTED_FUNCTION, ARCHETYPE_LITTLE_TRUSTED_ROLE);
    }

    private void runPositiveBulkActionTest(File file, TestObject<ArchetypeType> archetype) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        when("bulk action is executed");
        var script = prismContext.parserFor(file).xml().parseRealValue(ExecuteScriptType.class);
        scriptingService.evaluateExpression(
                ExecuteScriptConfigItem.of(
                        script,
                        originForArchetype(archetype)),
                VariablesMap.emptyMap(),
                false,
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

    private void runNegativeBulkActionTest(File file) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        given("unprivileged user is logged in");
        login(USER_JOE.getNameOrig());

        when("dangerous bulk action is executed");
        var script = prismContext.parserFor(file).xml().parseRealValue(ExecuteScriptType.class);
        try {
            scriptingService.evaluateExpression(
                    ExecuteScriptConfigItem.of(
                            script,
                            ConfigurationItemOrigin.rest()),
                    VariablesMap.emptyMap(),
                    false,
                    task, result);
            fail("unexpected success");
        } catch (ScriptExecutionException e) {
            var cause = ExceptionUtil.findCause(e, SecurityViolationException.class);
            assertThat(cause).as("security violation cause").isNotNull();
            assertExpectedException(cause)
                    .hasMessageContaining("Access to script expression evaluator not allowed")
                    .hasMessageContaining("expression profile: ##legacyUnprivilegedScripting");
        }

        and("not boomed");
        BOOMED_FLAG.assertNotSet();
    }

    /**
     * Different from {@link #runNegativeBulkActionTest(File)} in that the exception is not thrown (notifications don't do that)
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
        scriptingService.evaluateExpression(
                ExecuteScriptConfigItem.of(
                        script,
                        ConfigurationItemOrigin.rest()),
                VariablesMap.emptyMap(),
                false,
                task, result);

        then("not boomed");
        BOOMED_FLAG.assertNotSet();

        and("result is an error");
        assertFailure(result);

        // these asserts may be fragile
        assertThat(result.getMessage())
                .contains("Access to script expression evaluator not allowed")
                .contains("expression profile: ##legacyUnprivilegedScripting")
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
