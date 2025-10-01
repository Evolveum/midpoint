/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;
import java.util.Objects;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.test.RunFlag;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType.FULL_ELEVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests various combinations of `runAsRef`, `runAsRunner`, and `taskOwnerRef` (in `executeScript` policy action).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRunAs extends AbstractEmptyModelIntegrationTest {

    protected static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "run-as");

    private static final TestObject<FunctionLibraryType> LIBRARY_PRIVILEGED = TestObject.file(
            TEST_DIR, "library-privileged.xml", "ce5dd72f-8606-49e2-9e1d-d9e6d6806fb8");
    private static final TestObject<RoleType> ROLE_REGULAR_USER = TestObject.file(
            TEST_DIR, "role-regular-user.xml", "959f0672-e122-44b6-aef6-ca7612ade154");

    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_STANDARD = TestObject.file(
            TEST_DIR, "role-with-service-mapping-standard.xml", "8496dbbe-0032-4533-8b94-b2ab89658136");
    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_RUN_AS = TestObject.file(
            TEST_DIR, "role-with-service-mapping-run-as.xml", "b22e91cc-64c8-4c3b-99d3-11d86b57dda8");
    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_PRIVILEGED = TestObject.file(
            TEST_DIR, "role-with-service-mapping-privileged.xml", "8c32a639-97dc-4fed-83a4-3f496c5cb0df");

    private static final TestObject<RoleType> ROLE_WITH_METHOD_STANDARD = TestObject.file(
            TEST_DIR, "role-with-method-standard.xml", "5a833aaf-a1f6-47e7-af86-dbf97143cf12");

    private static final TestObject<ServiceType> SERVICE_ONE = TestObject.file(
            TEST_DIR, "service-one.xml", "3c938d46-8fee-4e23-8618-6514b969b4b2");

    private static final TestObject<UserType> USER_DISABLED = TestObject.file(
            TEST_DIR, "user-disabled.xml", "334022b7-11ea-47f9-b561-d1224fbe93a6");
    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_RUN_AS_DISABLED = TestObject.file(
            TEST_DIR, "role-with-service-mapping-run-as-disabled.xml", "f6e1ac76-714c-4ce3-977b-8165a9ee7947");

    private static final File USER_PRIVILEGED = new File(TEST_DIR, "user-privileged.xml");
    private static final String USER_PRIVILEGED_NAME = "PrivelegedUser";

    private static final File TASK_RECOMPUTE_PRIVILEGED_USER = new File(TEST_DIR, "task-recompute-privileged-user.xml");
    private static final String TASK_RECOMPUTE_PRIVILEGED_USER_OID = "3e8b389e-7cc4-11ef-9295-4f3d13b4c390";

    protected static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");

    public static final RunFlag FLAG_LOGIN_MODE = new RunFlag();
    public static final RunFlag FLAG_ALREADY_LOGGED_IN = new RunFlag();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyAuditService.setEnabled(true);

        initTestObjects(initTask, initResult,
                LIBRARY_PRIVILEGED,
                ROLE_REGULAR_USER,
                ROLE_WITH_SERVICE_MAPPING_STANDARD,
                ROLE_WITH_SERVICE_MAPPING_RUN_AS,
                ROLE_WITH_SERVICE_MAPPING_PRIVILEGED,
                ROLE_WITH_METHOD_STANDARD,
                SERVICE_ONE,
                USER_DISABLED,
                ROLE_WITH_SERVICE_MAPPING_RUN_AS_DISABLED);
    }

    /** Processing standard mapping (no run-as/run-privileged) under administrator. */
    @Test
    public void test100StandardMappingAsAdmin() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("regular user with 'mapping standard' is created");
        UserType user = addRegularUser(getTestNameShort(), ROLE_WITH_SERVICE_MAPPING_STANDARD);

        and("logged in as administrator");
        login(userAdministrator);

        when("description is set");
        setDescription(user.getOid(), "desc1", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("desc1: s:one p:administrator a:administrator");
    }

    @SafeVarargs
    private UserType addRegularUser(String name, TestObject<RoleType>... roles) throws CommonException {
        UserType user = new UserType()
                .name(name)
                .assignment(ROLE_REGULAR_USER.assignmentTo());
        for (TestObject<RoleType> role : roles) {
            user.assignment(role.assignmentTo());
        }
        login(userAdministrator);
        addObject(user.asPrismObject(), getTestTask(), getTestOperationResult());
        return user;
    }

    private void setDescription(String userOid, String description, Task task, OperationResult result) throws CommonException {
        modifyUserReplace(userOid, UserType.F_DESCRIPTION, task, result, description);
    }

    /** Processing standard mapping (no run-as/run-privileged) under user with limited privileges. */
    @Test
    public void test110StandardMappingAsRegular() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("regular user with 'mapping standard' is created");
        UserType user = addRegularUser(getTestNameShort(), ROLE_WITH_SERVICE_MAPPING_STANDARD);

        and("logged in as itself");
        login(user.asPrismObject());

        when("description is set");
        setDescription(user.getOid(), "desc2", task, result);

        then("the service is NOT visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                // no privileges to read service
                .assertCostCenter("desc2: s:null p:test110StandardMappingAsRegular a:test110StandardMappingAsRegular");
    }

    /** Processing "runAs" mapping under user with limited privileges. */
    @Test
    public void test120RunAsMappingAsRegular() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("regular user with 'runAs mapping' is created");
        UserType user = addRegularUser(getTestNameShort(), ROLE_WITH_SERVICE_MAPPING_RUN_AS);

        and("logged in as itself");
        login(user.asPrismObject());
        setPrincipalAsTestTaskOwner();

        when("description is set");
        dummyAuditService.clear();
        setDescription(user.getOid(), "desc3", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("desc3: s:one p:administrator a:test120RunAsMappingAsRegular"); // run-as

        and("audit is OK");
        displayDumpable("audit", dummyAuditService);

        assertAuditRecords(AuditEventType.MODIFY_OBJECT, user.getOid(), user.getOid(), null); // user
        assertAuditRecords(AuditEventType.ADD_OBJECT, user.getOid(), USER_ADMINISTRATOR_OID, null); // service
    }

    private void assertAuditRecords(
            AuditEventType eventType,
            String initiatorOid,
            String effectivePrincipalOid,
            EffectivePrivilegesModificationType privilegesModification) {
        dummyAuditService.getRecordsOfType(eventType).forEach(r -> {
            assertThat(r.getInitiatorRef().getOid()).as("initiator OID").isEqualTo(initiatorOid);
            assertThat(Objects.requireNonNull(r.getEffectivePrincipalRef()).getOid())
                    .as("effective principal OID").isEqualTo(effectivePrincipalOid);
            assertThat(r.getEffectivePrivilegesModification())
                    .as("privileges modified flag")
                    .isEqualTo(privilegesModification);
        });
    }

    /** Processing "privileged" mapping under user with limited privileges. */
    @Test
    public void test130PrivilegedMappingAsRegular() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("regular user with 'privileged mapping' is created");
        UserType user = addRegularUser(getTestNameShort(), ROLE_WITH_SERVICE_MAPPING_PRIVILEGED);

        and("logged in as itself");
        login(user.asPrismObject());
        setPrincipalAsTestTaskOwner();

        when("description is set");
        dummyAuditService.clear();
        setDescription(user.getOid(), "desc4", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("desc4: s:one p:test130PrivilegedMappingAsRegular a:test130PrivilegedMappingAsRegular");

        and("audit is OK");
        displayDumpable("audit", dummyAuditService);

        assertAuditRecords(AuditEventType.MODIFY_OBJECT, user.getOid(), user.getOid(), null); // user
        assertAuditRecords(AuditEventType.ADD_OBJECT, user.getOid(), user.getOid(), FULL_ELEVATION); // service
    }

    /** Using the standard library method under administrator. */
    @Test
    public void test200StandardMethodAsAdmin() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("regular user with 'method standard' is created");
        UserType user = addRegularUser(getTestNameShort(), ROLE_WITH_METHOD_STANDARD);

        and("logged in as administrator");
        login(userAdministrator);

        when("description is set");
        setDescription(user.getOid(), "desc1", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("desc1: s:one p:administrator a:administrator");
    }

    /** RunAs should fail if the target user is disabled. */
    @Test
    public void test400RunAsDisabled() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("regular user with 'runAs mapping disabled' is created");
        try {
            addRegularUser(getTestNameShort(), ROLE_WITH_SERVICE_MAPPING_RUN_AS_DISABLED);
            fail("unexpected success");
        } catch (Exception e) {
            assertExpectedException(e)
                    .hasMessageContaining("The principal is disabled");
        }
    }

    /** #10826 task's ownerRef points to the logged in user while reconciliation task running
     * We use loginMode check not to execute the script code during user's login.
     * Therefore, exception (within the script, look at the user-privileged.xml) while loginMode
     * will be caught and FLAG_LOGIN_MODE flag will be set.
     * If the script is run after the user's login, the exception won't be thrown, and
     * the FLAG_ALREADY_LOGGED_IN flag won't be set.
     * */
    @Test
    public void test500RunTaskAsLoggedInUser() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        FLAG_LOGIN_MODE.reset();
        FLAG_ALREADY_LOGGED_IN.reset();

        login(userAdministrator);

        importObjectFromFile(RESOURCE_DUMMY_FILE, result);
        importObjectFromFile(USER_PRIVILEGED, result);

        login(USER_PRIVILEGED_NAME);
        addObject(TASK_RECOMPUTE_PRIVILEGED_USER);

        when();
        waitForTaskStart(TASK_RECOMPUTE_PRIVILEGED_USER_OID);
        waitForTaskFinish(TASK_RECOMPUTE_PRIVILEGED_USER_OID);

        then();
        FLAG_LOGIN_MODE.assertSet();
        FLAG_ALREADY_LOGGED_IN.assertNotSet();
    }
}
