/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.DummyResourceContoller;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.RunFlag;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the use of "semi-safe" expression profile set in recommended midPoint configuration.
 *
 * #10060
 *
 * Expression profiles:
 *
 * . `safe` - allow only safe expression evaluators. No scripting.
 * . `permissive` - allow all expressions
 * . `prohibitive` - allow no expressions
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestExpressionProfileSemiSafe extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "profile-semi-safe");

    // TODO: This should be later switched to default system config from initial objects
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final DummyTestResource RESOURCE_SCRIPTED_TARGET = new DummyTestResource(
            TEST_DIR, "resource-scripted-target.xml", "b6921940-b317-11f0-a8d9-bb8df5a5aed5",
            "scripted-target", controller -> controller.extendSchemaPirate());

    private static final TestObject<RoleType> ROLE_AUTO_NICE = TestObject.file(
            TEST_DIR, "role-auto-nice.xml", "9296ee02-b011-11f0-a82e-270fe586cfa4");
    private static final String ROLE_AUTO_NICE_TITLE = "AutoNice";

    private static final TestObject<RoleType> ROLE_AUTO_MALICIOUS = TestObject.file(
            TEST_DIR, "role-auto-malicious.xml", "939476d2-b334-11f0-821b-9f7be5660bfd");

    private static final TestObject<RoleType> ROLE_HARMLESS = TestObject.file(
            TEST_DIR, "role-harmless.xml", "15f860d0-b32a-11f0-b169-431d2df5bf12");
    private static final String ROLE_HARMLESS_TITLE = "Harmless";

    private static final TestObject<RoleType> ROLE_MALICIOUS_CONDITION = TestObject.file(
            TEST_DIR, "role-malicious-condition.xml", "e9e23d72-b333-11f0-9c68-4f06c24ddea7");
    private static final TestObject<RoleType> ROLE_MALICIOUS_CONDITION_FILTER = TestObject.file(
            TEST_DIR, "role-malicious-condition-filter.xml", "47c3c21a-b336-11f0-afd8-8f70911233b3");
    private static final TestObject<RoleType> ROLE_MALICIOUS_APPLICATION_OUTBOUND = TestObject.file(
            TEST_DIR, "role-application-malicious-outbound.xml", "e55fbaa2-b330-11f0-a4e1-43f895ecb4de");
    private static final TestObject<RoleType> ROLE_MALICIOUS_BUSINESS_INDUCEMENT_CONDITION = TestObject.file(
            TEST_DIR, "role-business-malicious-inducement-condition.xml", "a150b170-b332-11f0-8eba-439d792f85bb");
    private static final TestObject<RoleType> ROLE_FUNCTION_REVERSE = TestObject.file(
            TEST_DIR, "role-function-reverse.xml", "10898900-b3e1-11f0-a523-cbd9df3e4f0b");
    private static final TestObject<RoleType> ROLE_FUNCTION_MALICIOUS = TestObject.file(
            TEST_DIR, "role-function-malicious.xml", "15298904-b3e3-11f0-9bce-db0c2d0f5a5a");
    private static final TestObject<RoleType> ROLE_BUSINESS_REF_FILTER_GOOD = TestObject.file(
            TEST_DIR, "role-business-ref-filter-good.xml", "2a572852-c12d-11f0-83c6-9b5ebb5d1de4");
    private static final TestObject<RoleType> ROLE_BUSINESS_REF_FILTER_MALICIOUS = TestObject.file(
            TEST_DIR, "role-business-ref-filter-malicious.xml", "d75ca578-c131-11f0-8ce5-b3ed67b6be8d");

    private static final TestObject<UserType> USER_ALICE = TestObject.file(
            TEST_DIR, "user-alice.xml", "8dcc5b00-b318-11f0-a529-9f8b26779770");
    private static final TestObject<UserType> USER_BOB = TestObject.file(
            TEST_DIR, "user-bob.xml", "2c1955f8-b335-11f0-b7f1-e30fee0b1bdf");
    private static final TestObject<UserType> USER_CAROL = TestObject.file(
            TEST_DIR, "user-carol.xml", "b098abb3-dc82-4d4f-8e7b-93742a7eeb35");
    private static final TestObject<UserType> USER_DAVE = TestObject.file(
            TEST_DIR, "user-dave.xml", "e756da24-b3e5-11f0-bde5-6be66deb5c92");
    private static final TestObject<UserType> USER_MALLORY = TestObject.file(
            TEST_DIR, "user-mallory.xml", "4925ba3e-b32a-11f0-ba58-1b26237260e8");

    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_SUPPORT = TestObject.file(
            TEST_DIR, "function-library-support.xml", "9522632c-b3e0-11f0-a092-bb7b6afca320");


    private static final RunFlag BOOMED_FLAG = new RunFlag();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SCRIPTED_TARGET.initAndTest(this, initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_APPLICATION_ROLE,
                CommonInitialObjects.ARCHETYPE_BUSINESS_ROLE,
                ROLE_AUTO_NICE,
                ROLE_HARMLESS,
                ROLE_MALICIOUS_CONDITION,
                ROLE_MALICIOUS_CONDITION_FILTER,
                ROLE_MALICIOUS_APPLICATION_OUTBOUND,
                ROLE_MALICIOUS_BUSINESS_INDUCEMENT_CONDITION,
                ROLE_FUNCTION_REVERSE,
                ROLE_FUNCTION_MALICIOUS,
                FUNCTION_LIBRARY_SUPPORT,
                USER_CAROL);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /** Alice is set up correctly, everything matches with expression profiles.
     * Everything should go smoothly. */
    @Test
    public void test100AddAlice() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        addObject(USER_ALICE, task, result);

        // TODO: BUG?????
        recomputeUser(USER_ALICE.oid);

        then("user is created");
        assertSuccess(result);
        assertAliceBaseline();
        BOOMED_FLAG.assertNotSet();
    }

    void assertAliceBaseline()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException, ConfigurationException, ObjectNotFoundException, ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        assertUserAfter(USER_ALICE.oid)
                .display()
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_AUTO_NICE.oid)
                .assertRole(ROLE_HARMLESS.oid);

        assertDummyAccountByUsername(RESOURCE_SCRIPTED_TARGET.name, USER_ALICE.getNameOrig())
                .display()
                .assertFullName(USER_ALICE.getObjectable().getFullName().getOrig())
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                        "Came from London")
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                        ROLE_AUTO_NICE_TITLE, ROLE_HARMLESS_TITLE);

    }

    /** Try to assign Alice a role that tries to execute Groovy code in role condition.
     * This should end with an error. */
    @Test
    public void test200AssignAliceRoleMaliciousCondition() throws Exception {
        runNegativeAssignAliceTest(ROLE_MALICIOUS_CONDITION, "Access to script expression evaluator not allowed");
    }

    /** Try to assign Alice a role that tries to execute Groovy code in role condition, inside a filter.
     * This should end with an error. */
    @Test
    public void test210AssignAliceRoleMaliciousConditionFilter() throws Exception {
        runNegativeAssignAliceTest(ROLE_MALICIOUS_CONDITION_FILTER, "Access to script expression evaluator not allowed");
    }

    /** Try to assign Alice an application role that tries to execute Groovy code in construction/outbound.
     * This should end with an error.
     * Application role archetype is applied to the role, to make sure the archetype does not ruin expression profile. */
    @Test
    public void test220AssignAliceRoleMaliciousApplicationOutbound() throws Exception {
        runNegativeAssignAliceTest(ROLE_MALICIOUS_APPLICATION_OUTBOUND, "Access to script expression evaluator not allowed");
    }

    /** Try to assign Alice a business role that tries to execute Groovy code in inducement condition.
     * This should end with an error.
     * Business role archetype is applied to the role, to make sure the archetype does not ruin expression profile.*/
    @Test
    public void test230AssignAliceRoleMaliciousBusinessInducementCondition() throws Exception {
        runNegativeAssignAliceTest(ROLE_MALICIOUS_BUSINESS_INDUCEMENT_CONDITION, "Access to script expression evaluator not allowed");
    }

    private void runNegativeAssignAliceTest(TestObject<RoleType> role, String expectedMessage)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException, IOException, PolicyViolationException, ObjectAlreadyExistsException, ConflictException, SchemaViolationException, InterruptedException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        try {

            when();
            assignRole(USER_ALICE.oid, role.oid, task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            // Expected exception
            then();
            assertFailure(result);
            assertExpectedException(e)
                    .hasMessageContaining(expectedMessage);
        }

        assertAliceBaseline();

        BOOMED_FLAG.assertNotSet();
    }


    /** Mallory tries to execute Groovy code in the assignment expression.
     * This should end with an error. */
    @Test
    public void test300AddMallory() throws Exception {
        runNegativeAddObjectTest(USER_MALLORY, "Access to script expression evaluator not allowed");
    }

    /** Dave dares to execute expression inside an assignment.
     * It is a safe expression evaluator, probably no harm done.
     * However, we do not want any expressions in user objects.
     * This should end with an error. */
    @Test
    public void test310AddDave() throws Exception {
        runNegativeAddObjectTest(USER_DAVE, "Access to expression evaluator literal");
    }

    private <O extends ObjectType> void runNegativeAddObjectTest(TestObject<O> testObject, String expectedMessage)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException, IOException, PolicyViolationException, ObjectAlreadyExistsException, ConflictException, SchemaViolationException, InterruptedException, SecurityViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        try {

            when();
            addObject(testObject, task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            // Expected exception
            then();
            assertFailure(result);
            assertExpectedException(e)
                    .hasMessageContaining(expectedMessage);
        }

        assertNoObject(testObject.getType(), testObject.oid);
        assertNoDummyAccount(RESOURCE_SCRIPTED_TARGET.name, testObject.getNameOrig());
        BOOMED_FLAG.assertNotSet();
    }

    /** We set up the system by importing malicious autoassigned role.
     * Then, adding Bob should fail, even though Bob is otherwise a correct user. */
    @Test
    public void test400AddBobTrap() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        given();
        ROLE_AUTO_MALICIOUS.init(this, task, result);

        try {

            when();
            addObject(USER_BOB, task, result);
            fail("unexpected success");

        } catch (SecurityViolationException e) {
            // Expected exception
            then();
            assertFailure(result);
            assertExpectedException(e)
                    .hasMessageContaining("Access to script expression evaluator not allowed");
        } finally {
            deleteObject(RoleType.class, ROLE_AUTO_MALICIOUS.oid);
        }


        assertNoObject(UserType.class, USER_BOB.oid);
        assertNoDummyAccount(RESOURCE_SCRIPTED_TARGET.name, USER_BOB.getNameOrig());
        BOOMED_FLAG.assertNotSet();
    }

    /** Adding Bob without the malicious autoassign role should go smoothly.
     *  Just to make sure.
     *  This also tests that autoassign condition in Nice role is evaluated correctly,
     *  as it is evaluated as false for Bob. */
    @Test
    public void test410AddBob() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        addObject(USER_BOB, task, result);

        // TODO: BUG?????
        recomputeUser(USER_BOB.oid);

        then("user is created");
        assertSuccess(result);
        assertUserAfter(USER_BOB.oid)
                .display()
                .assertAssignments(1)
                .assignments()
                .assertRole(ROLE_HARMLESS.oid);

        assertDummyAccountByUsername(RESOURCE_SCRIPTED_TARGET.name, USER_BOB.getNameOrig())
                .display()
                .assertFullName(USER_BOB.getObjectable().getFullName().getOrig())
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                        "Came from New York")
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                        ROLE_HARMLESS_TITLE);

        BOOMED_FLAG.assertNotSet();
    }

    /**
     * Assigning bob a "Reverse Function Role".
     * This is a safe role, using function expression to invoke reverse function in support function library.
     * It is using only safe expressions to specify function parameters.
     * Everything should go smoothly.
     */
    @Test
    public void test420AssignBobRoleFunctionReverse() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        when();
        assignRole(USER_BOB.oid, ROLE_FUNCTION_REVERSE.oid, task, result);


        then();
        assertSuccess(result);
        assertUserAfter(USER_BOB.oid)
                .display()
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_HARMLESS.oid)
                .assertRole(ROLE_FUNCTION_REVERSE.oid);

        assertDummyAccountByUsername(RESOURCE_SCRIPTED_TARGET.name, USER_BOB.getNameOrig())
                .display()
                .assertFullName(USER_BOB.getObjectable().getFullName().getOrig())
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                        "Came from New York")
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                        ROLE_HARMLESS_TITLE, "lesnuoC etaroproC");

        BOOMED_FLAG.assertNotSet();
    }

    /**
     * Assigning bob a "Malicious Function Role".
     * This is a malicious role,that is trying to abuse function expression,
     * including groovy code in parameter specification.
     * This should fail.
     */
    @Test
    public void test430AssignBobRoleFunctionMalicious() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        try {

            when();
            assignRole(USER_BOB.oid, ROLE_FUNCTION_MALICIOUS.oid, task, result);
            fail("unexpected success");

        } catch (SecurityViolationException e) {
            // Expected exception
            then();
            assertFailure(result);
            assertExpectedException(e)
                    .hasMessageContaining("Access to script expression evaluator not allowed");
        }

        assertUserAfter(USER_BOB.oid)
                .display()
                .assertAssignments(2)
                .assignments()
                .assertRole(ROLE_HARMLESS.oid)
                .assertRole(ROLE_FUNCTION_REVERSE.oid);

        assertDummyAccountByUsername(RESOURCE_SCRIPTED_TARGET.name, USER_BOB.getNameOrig())
                .display()
                .assertFullName(USER_BOB.getObjectable().getFullName().getOrig())
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                        "Came from New York")
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                        ROLE_HARMLESS_TITLE, "lesnuoC etaroproC");

        BOOMED_FLAG.assertNotSet();
    }

    /** Add business role with filter in ref.
     * Everything should go smoothly. */
    @Test
    public void test500AddRoleBusinessRefFilterGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        addObject(ROLE_BUSINESS_REF_FILTER_GOOD, task, result);

        then("role is created");
        assertSuccess(result);
        assertRoleAfter(ROLE_BUSINESS_REF_FILTER_GOOD.oid)
                .display();
                // We do not have appropriate inducements asserters here, do it later

        BOOMED_FLAG.assertNotSet();
    }

    /**
     * TODO.
     * Everything should go smoothly.
     */
    @Test
    public void test502AssignCarolRoleBusinessRefFilterGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        when();
        assignRole(USER_CAROL.oid, ROLE_BUSINESS_REF_FILTER_GOOD.oid, task, result);


        then();
        assertSuccess(result);
        assertUserAfter(USER_CAROL.oid)
                .display()
                .assertAssignments(1)
                .assignments()
                    .assertRole(ROLE_BUSINESS_REF_FILTER_GOOD.oid);

        assertDummyAccountByUsername(RESOURCE_SCRIPTED_TARGET.name, USER_CAROL.getNameOrig())
                .display()
                .assertFullName(USER_CAROL.getObjectable().getFullName().getOrig())
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                        ROLE_HARMLESS_TITLE);

        BOOMED_FLAG.assertNotSet();
    }

    /**
     * TODO.
     * Everything should go smoothly.
     */
    @Test
    public void test509UnassignCarolRoleBusinessRefFilterGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        when();
        unassignRole(USER_CAROL.oid, ROLE_BUSINESS_REF_FILTER_GOOD.oid, task, result);


        then();
        assertSuccess(result);
        assertUserAfter(USER_CAROL.oid)
                .display()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_SCRIPTED_TARGET.name, USER_CAROL.getNameOrig());

        BOOMED_FLAG.assertNotSet();
    }

    /** Adding a role with a malicious script in filter in targetRef.
     * For some reason, the filter is not resolved at this point, making this operation a success.
     * However, what is important is that the script code was NOT executed (no "boom").
     * (See #10947)
     */
    @Test
    public void test510AddRoleBusinessRefFilterMalicious() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        addObject(ROLE_BUSINESS_REF_FILTER_MALICIOUS, task, result);

        then("role is created");
        assertSuccess(result);
        assertRoleAfter(ROLE_BUSINESS_REF_FILTER_MALICIOUS.oid)
                .display();
        // We do not have appropriate inducements asserters here, do it later

        // This is important
        BOOMED_FLAG.assertNotSet();
    }

    /**
     * Assigning a role with has malicious expression in filter in targetRef.
     * This reference was not resolved before, therefore midPoint tries to resolve it.
     * It should fail, as groovy expressions are not allowed for roles.
     * (See #10947)
     */
    @Test
    public void test512AssignCarolRoleBusinessRefFilterMalicious() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        try {
            when();
            assignRole(USER_CAROL.oid, ROLE_BUSINESS_REF_FILTER_MALICIOUS.oid, task, result);

        } catch (SecurityViolationException e) {
            // Expected exception
            then();
            assertFailure(result);
            assertExpectedException(e)
                    .hasMessageContaining("Access to script expression evaluator not allowed");
        }

        then();
        assertUserAfter(USER_CAROL.oid)
                .display()
                .assertAssignments(0);

        assertNoDummyAccount(RESOURCE_SCRIPTED_TARGET.name, USER_CAROL.getNameOrig());

        BOOMED_FLAG.assertNotSet();
    }

    /** Just a clean up.
     * Everything should go smoothly. */
    @Test
    public void test519DeleteRoleBusinessRefFilterMalicious() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        deleteObject(RoleType.class, ROLE_BUSINESS_REF_FILTER_MALICIOUS.oid, task, result);

        then("role is deleted");
        assertSuccess(result);
        assertNoObject(RoleType.class, ROLE_BUSINESS_REF_FILTER_MALICIOUS.oid);

        BOOMED_FLAG.assertNotSet();
    }

    /** Filter expressions are ignored during import.
     * Therefore, the import should fail due to unresolvable reference.
     * However, that is not really important.
     * The important check is that the "boom" expression was no evaluated.
     * (See #10947)
     */
    @Test
    public void test520ImportRoleBusinessRefFilterMalicious() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        resetBoomed();

        when();
        try (InputStream stream = ROLE_BUSINESS_REF_FILTER_MALICIOUS.getInputStream()) {
            modelService.importObjectsFromStream(
                    stream, PrismContext.LANG_XML, MiscSchemaUtil.getDefaultImportOptions(), task, result);
        }

        then();
        // Not really important
        assertFailure(result);
        assertNoObject(RoleType.class, ROLE_BUSINESS_REF_FILTER_MALICIOUS.oid);
        // We do not have appropriate inducements asserters here, do it later

        // This is *very* important check
        BOOMED_FLAG.assertNotSet();
    }


    private static void resetBoomed() {
        BOOMED_FLAG.reset();
    }

    public static void boom() {
        // We intentionally do not throw an exception here
        BOOMED_FLAG.set();
    }
}
