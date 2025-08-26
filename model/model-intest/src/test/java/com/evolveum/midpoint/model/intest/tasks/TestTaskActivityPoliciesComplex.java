/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskActivityPoliciesComplex extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds/complex");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "dummy.xml",
            "8f82e457-6c6e-42d7-a433-1a346b1899ee",
            "resource-dummy",
            TestTaskActivityPoliciesComplex::populateWithSchema);

    private static final TestObject<RoleType> ROLE_POLICY =
            TestObject.file(TEST_DIR, "role-policy.xml", "630d103c-7d1e-4d71-b7ff-027e052c1622");

    private static final TestObject<RoleType> TASK_RECONCILIATION =
            TestObject.file(TEST_DIR, "task-reconciliation.xml", "831266b9-ee6a-4ffc-8005-27c964ea6ffa");

    private static final TestObject<RoleType> TASK_RECONCILIATION_COMPOSITE =
            TestObject.file(TEST_DIR, "task-reconciliation-composite.xml", "ccd6df6c-123a-4d9d-b48e-e4de9bf3f2e2");

    private DummyResourceContoller dummyResourceCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        dummyResourceCtl.addAccount("jdoe", "John Doe");
        dummyResourceCtl.addAccount("jsmith", "Jim Smith");
        dummyResourceCtl.addAccount("jblack", "Jack Black");
        dummyResourceCtl.addAccount("wwhite", "William White");

        addAndRecompute(ROLE_POLICY, initTask, initResult);
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();
    }

    @Test(enabled = false)
    public void testReconciliation() throws Exception {
        clearAfterReconciliation();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        TASK_RECONCILIATION.reset();
        deleteIfPresent(TASK_RECONCILIATION, result);

        addObject(TASK_RECONCILIATION, task, result);

        waitForTaskCloseOrSuspend(TASK_RECONCILIATION.oid, 20000L);

        System.out.println(PrismTestUtil.serializeAnyData(getTask(TASK_RECONCILIATION.oid).asObjectable().getActivityState(), TaskType.F_ACTIVITY_STATE));

        TaskAsserter<Void> asserter = TaskAsserter.forTask(getObject(TaskType.class, TASK_RECONCILIATION.oid));
        // @formatter:off
        System.out.println(asserter.getObject());
        // @formatter:on

        repositoryService.searchObjects(TaskType.class, null, null, result)
                .forEach(t-> {
                    try {
                        System.out.println(PrismTestUtil.serializeToXml(t.asObjectable()));

                        ActivityStateType state = t.asObjectable().getActivityState().getActivity();

                        assertExecutionAttempts(state);
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void assertExecutionAttempts(ActivityStateType state) throws SchemaException {
        if (state.getExecutionAttempt() == null || state.getExecutionAttempt() != 1) {
            Assertions.fail("Expected exactly one execution attempt, but was: " + state.getExecutionAttempt());
            System.out.println(PrismTestUtil.serializeAnyData(state, TaskType.F_ACTIVITY_STATE));
        }

        state.getActivity().forEach(s -> {
            try {
                assertExecutionAttempts(s);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void clearAfterReconciliation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = PrismTestUtil.getPrismContext()
                .queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).eq("reconciliation")
                .build();

        AtomicInteger counter = new AtomicInteger(0);
        repositoryService.searchObjects(UserType.class, query, null, result)
                .forEach(user -> {
                    try {
                        deleteObjectRaw(UserType.class, user.getOid(), task, result);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        repositoryService.searchObjects(ShadowType.class, null, null, result)
                .forEach(shadow -> {
                    try {
                        deleteObjectRaw(ShadowType.class, shadow.getOid(), task, result);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        System.out.println("Deleted " + counter.get() + " objects after reconciliation.");
    }

    @Test(enabled = false)
    public void testReconciliationComposite() throws Exception {
        clearAfterReconciliation();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        TASK_RECONCILIATION_COMPOSITE.reset();
        deleteIfPresent(TASK_RECONCILIATION_COMPOSITE, result);

        addObject(TASK_RECONCILIATION_COMPOSITE, task, result);

        waitForTaskCloseOrSuspend(TASK_RECONCILIATION_COMPOSITE.oid, 300000L);

        System.out.println(PrismTestUtil.serializeAnyData(getTask(TASK_RECONCILIATION_COMPOSITE.oid).asObjectable().getActivityState(), TaskType.F_ACTIVITY_STATE));
    }
}
