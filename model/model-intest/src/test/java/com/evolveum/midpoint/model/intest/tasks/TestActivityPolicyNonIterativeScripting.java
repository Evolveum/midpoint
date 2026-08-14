/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyProcessorHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests {@link ActivityPolicyProcessorHelper} - the entry point for activity policy evaluation
 * in non-iterative activities. The activity policies are normally evaluated after each processed
 * item, so a non-iterative activity has to invoke the evaluation explicitly. Here a non-iterative
 * scripting activity calls the helper from a Groovy loop, and the policies declared on the activity
 * must be enforced.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestActivityPolicyNonIterativeScripting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activity-policies-non-iterative");

    private static final TestObject<TaskType> TASK_SUSPEND = TestObject.file(
            TEST_DIR, "task-non-iterative-suspend.xml", "a7e00000-0000-0000-0000-000000000001");

    private static final TestObject<TaskType> TASK_RESTART = TestObject.file(
            TEST_DIR, "task-non-iterative-restart.xml", "d5c0d175-ebda-4506-821d-6205eeae85cf");

    private static final long TIMEOUT = 60_000;

    /**
     * The script loops 10x500 ms and calls the helper in each iteration.
     * The execution-time policy (2 seconds, suspendTask) must suspend the task
     * before the loop completes.
     */
    @Test
    public void test100SuspendOnExecutionTime() throws Exception {
        OperationResult result = getTestOperationResult();

        when("non-iterative scripting task with an execution-time suspend policy runs");
        addObject(TASK_SUSPEND, getTestTask(), result);
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_SUSPEND.oid, result, TIMEOUT);

        then("task is suspended and the policy trigger is recorded in the activity state");
        // @formatter:off
        assertTaskTree(TASK_SUSPEND.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Max. 2s execution")
                            .assertTriggerCount(1)
                        .end()
                    .end()
                    .itemProcessingStatistics()
                        // limit is 2 seconds, 5 seconds planned
                        .assertRunTimeBetween(2000L, 5000L)
                    .end();
        // @formatter:on
    }

    /**
     * Two policies: restart the activity when the run takes more than 3 seconds,
     * and suspend the task when the number of execution attempts exceeds 2.
     * The activity must be restarted twice and then suspended at the start
     * of the third attempt.
     */
    @Test
    public void test110RestartThenSuspendOnAttempts() throws Exception {
        OperationResult result = getTestOperationResult();

        when("non-iterative scripting task with restart + attempt-limit policies runs");
        addObject(TASK_RESTART, getTestTask(), result);
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_RESTART.oid, result, TIMEOUT);

        then("activity was restarted twice and then suspended on the third attempt");
        // Note: the policy trigger state is purged on each restart, so after the final
        // suspension only the trigger from the last (third) run is present.
        // @formatter:off
        assertTaskTree(TASK_RESTART.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(3)
                    .policies()
                        .policy("Limit restarts")
                            .assertTriggerCount(1)
                        .end()
                    .end();
        // @formatter:on
    }
}
