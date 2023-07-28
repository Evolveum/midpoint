/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

/**
 * Tests various combinations of `runAsRef`, `runAsPrivileged`, and `taskOwnerRef` (in `executeScript` policy action).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRunAs extends AbstractEmptyModelIntegrationTest {

    protected static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "run-as");

    private static final TestObject<RoleType> ROLE_REGULAR_USER = TestObject.file(
            TEST_DIR, "role-regular-user.xml", "959f0672-e122-44b6-aef6-ca7612ade154");

    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_STANDARD = TestObject.file(
            TEST_DIR, "role-with-service-mapping-standard.xml", "8496dbbe-0032-4533-8b94-b2ab89658136");
    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_RUN_AS = TestObject.file(
            TEST_DIR, "role-with-service-mapping-run-as.xml", "b22e91cc-64c8-4c3b-99d3-11d86b57dda8");
    private static final TestObject<RoleType> ROLE_WITH_SERVICE_MAPPING_PRIVILEGED = TestObject.file(
            TEST_DIR, "role-with-service-mapping-privileged.xml", "8c32a639-97dc-4fed-83a4-3f496c5cb0df");

    private static final TestObject<ServiceType> SERVICE_ONE = TestObject.file(
            TEST_DIR, "service-one.xml", "3c938d46-8fee-4e23-8618-6514b969b4b2");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                ROLE_REGULAR_USER,
                ROLE_WITH_SERVICE_MAPPING_STANDARD,
                ROLE_WITH_SERVICE_MAPPING_RUN_AS,
                ROLE_WITH_SERVICE_MAPPING_PRIVILEGED,
                SERVICE_ONE);
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
        setDescription(user.getOid(), "hi", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("hi: one (administrator)");
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
        setDescription(user.getOid(), "hi", task, result);

        then("the service is NOT visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("hi: null (test110StandardMappingAsRegular)"); // no privileges to read service
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

        when("description is set");
        setDescription(user.getOid(), "hi", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("hi: one (administrator)"); // run-as
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

        when("description is set");
        setDescription(user.getOid(), "hi", task, result);

        then("the service is visible in the cost center value");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .assertCostCenter("hi: one (test130PrivilegedMappingAsRegular)");
    }
}
