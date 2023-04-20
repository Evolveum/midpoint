/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ParsedGetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ExtensionValueGenerator;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.AssignmentGenerator;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the performance of various `model-impl` components.
 *
 * First, it checks the `applySchemasAndSecurity` method.
 *
 * Later, it may be extended or split into smaller tests. The time will tell.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPerformance extends AbstractEmptyInternalModelTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf");

    private static final TestObject<RoleType> ROLE_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "role-can-read-all.xml", "8d79c980-0999-49f7-ba11-6776dad41770");
    private static final TestObject<UserType> USER_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "user-can-read-all.xml", "564261c3-efe8-4f35-845e-f928395d2cf1");
    private static final TestObject<RoleType> ROLE_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "role-can-read-almost-all.xml", "b6774d03-b2c5-4b1b-a175-6deacbdd0115");
    private static final TestObject<UserType> USER_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "user-can-read-almost-all.xml", "78eaaa5c-b8f1-4959-b356-6c41c04d613e");
    private static final TestObject<RoleType> ROLE_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "role-can-read-few.xml", "46302f20-2197-4345-9d4b-ab183a028aa9");
    private static final TestObject<UserType> USER_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "user-can-read-few.xml", "a17e2af6-7b60-4cf3-bebf-513af4a61b16");

    @Autowired private SchemaTransformer schemaTransformer;

    private final ExtensionValueGenerator extensionValueGenerator = ExtensionValueGenerator.withDefaults();
    private final AssignmentGenerator assignmentGenerator = AssignmentGenerator.withDefaults();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                ROLE_CAN_READ_ALL, USER_CAN_READ_ALL,
                ROLE_CAN_READ_ALMOST_ALL, USER_CAN_READ_ALMOST_ALL,
                ROLE_CAN_READ_FEW, USER_CAN_READ_FEW);
        InternalsConfig.reset(); // We want to measure performance, so no consistency checking and the like.
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    /** Tests schema/security application for full autz (superuser). */
    @Test
    public void test100ApplyFullAutz() throws CommonException {
        executeAutzTest("full", null);
    }

    /** Tests schema/security application for "read all" autz. */
    @Test
    public void test110ApplyReadAll() throws CommonException {
        login(USER_CAN_READ_ALL.get());
        executeAutzTest("read all", user -> {
            assertUserAfter(user.asPrismObject())
                    .extension()
                    .assertSize(50)
                    .end();
            var assignment = user.getAssignment().get(0);
            assertThat(assignment.getTargetRef()).isNotNull();
            assertThat(assignment.getDescription()).isNotNull();
        });
    }

    /** Tests schema/security application for "read almost all" autz. */
    @Test
    public void test120ApplyReadAlmostAll() throws CommonException {
        login(USER_CAN_READ_ALMOST_ALL.get());
        executeAutzTest("read almost all", user -> {
            assertUserAfter(user.asPrismObject())
                    .extension()
                    .assertSize(49)
                    .end();
            var assignment = user.getAssignment().get(0);
            assertThat(assignment.getTargetRef()).isNotNull();
            assertThat(assignment.getDescription()).isNotNull();
        });
    }

    /** Tests schema/security application for "read few" autz. */
    @Test
    public void test130ApplyReadFew() throws CommonException {
        login(USER_CAN_READ_FEW.get());
        executeAutzTest("read few", user -> {
            assertUserAfter(user.asPrismObject())
                    .extension()
                    .assertSize(1)
                    .end();
            var assignment = user.getAssignment().get(0);
            assertThat(assignment.getTargetRef()).isNull();
            assertThat(assignment.getDescription()).isNotNull();
        });
    }

    private void executeAutzTest(String label, CheckedConsumer<UserType> asserter)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int iterations = 20;
        int numUsers = 2000;
        long start = System.currentTimeMillis();
        long netDuration = 0;
        for (int i = 0; i < iterations; i++) {
            netDuration += executeSingleAutzTestIteration(
                    numUsers, i == 0 ? asserter : null, task, result);
        }
        long grossDuration = System.currentTimeMillis() - start;
        int executions = numUsers * iterations;
        display(String.format(
                "Testing %s: Applied a security to a single user in %.3f ms (%,d ms gross and %,d ms net duration, %,d executions)",
                label, (double) netDuration / executions, grossDuration, netDuration, executions));
    }

    private long executeSingleAutzTestIteration(
            int numUsers, CheckedConsumer<UserType> asserter, Task task, OperationResult result)
            throws CommonException {
        SearchResultList<PrismObject<UserType>> users = new SearchResultList<>();
        for (int i = 0; i < numUsers; i++) {
            UserType user = new UserType()
                    .name("user " + i);
            extensionValueGenerator.populateExtension(user.asPrismObject(), 50);
            assignmentGenerator.populateAssignments(user, 50);
            users.add(user.asPrismObject());
        }

        long start = System.currentTimeMillis();
        var usersAfter =
                schemaTransformer.applySchemasAndSecurityToObjects(users, ParsedGetOperationOptions.empty(), task, result);
        long duration = System.currentTimeMillis() - start;

        if (asserter != null) {
            asserter.accept(usersAfter.get(0).asObjectable());
        }

        return duration;
    }
}
