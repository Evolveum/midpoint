/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestActivityPolicyThresholds extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestObject<TaskType> TASK_SIMPLE_NOOP =
            TestObject.file(TEST_DIR, "task-simple-noop.xml", "54464a8e-7cd3-47e3-9bf6-0d07692a893b");

    @Test
    public void test100SimpleTask() throws Exception {
        given();
        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        TestObject<TaskType> task = TASK_SIMPLE_NOOP;

        when();

        deleteIfPresent(task, testResult);
        addObject(task, getTestTask(), testResult);
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, testResult, 7000L);

        then();

        assertSimpleTask(task);

        when("repeated execution");

        taskManager.resumeTaskTree(task.oid, testResult);
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, testResult, 2000L);

        then("repeated execution");

        assertSimpleTaskRepeatedExecution(task);

        if (isNativeRepository()) {
            and("there are no simulation results"); // MID-8936
            assertNoRepoObjects(SimulationResultType.class);
        }
    }

    private void assertSimpleTask(TestObject<TaskType> testObject) throws Exception {
        var options = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> task = taskManager.getObject(TaskType.class, testObject.oid, options, getTestOperationResult());

        ActivityPolicyType policy = task.asObjectable().getActivity().getPolicies().getPolicy().get(0);
        String identifier = ActivityPolicyUtils.createIdentifier(task.getOid(), policy);

        // @formatter:off
        var asserter = assertTaskTree(task.getOid(), "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .activityPolicyStates()
                    .display()
                    .assertOnePolicyStateTriggers(identifier, 1)
                .end()
                .assertInProgressLocal()
                .progress().assertSuccessCount(6, 0).display().end()
                .itemProcessingStatistics().display().end();
        // @formatter:on
    }

    private void assertSimpleTaskRepeatedExecution(TestObject<TaskType> task) {
        // todo implement
    }
}
