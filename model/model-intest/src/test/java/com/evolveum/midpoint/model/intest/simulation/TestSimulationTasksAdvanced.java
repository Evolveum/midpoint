/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_FOCUS_ACTIVATED;

import java.io.File;
import java.math.BigDecimal;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests advanced features of simulation tasks: multi-threading, multi-node operation, suspend-and-resume, and so on.
 *
 * TODO finish the code
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSimulationTasksAdvanced extends AbstractSimulationsTest {

    private static final File TEST_DIR = SIM_TEST_DIR;

    private static final TestTask TASK_USER_RECOMPUTATION_SIMULATED = new TestTask(
            TEST_DIR, "task-user-recomputation-simulated.xml", "df2c380b-c07f-4aea-8544-03bc43db10c8");

    private static final int USERS = 500;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoObjectCreatorFor(UserType.class)
                .withObjectCount(USERS)
                .withNamePattern("user-%03d")
                .execute(initResult);

        TASK_USER_RECOMPUTATION_SIMULATED.init(this, initTask, initResult);
    }

    /**
     * Single thread recomputation of users - suspended and resumed.
     */
    @Test
    public void test100CreateUserWithLinkedDevelopmentAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run for a while");
        TASK_USER_RECOMPUTATION_SIMULATED.runFor(2000, result);

        then("something (but not everything) is processed");
        // @formatter:off
        TASK_USER_RECOMPUTATION_SIMULATED.doAssert("after first run")
                .assertSuspended() // May fail on an extremely fast machine
                .activityState(ActivityPath.empty())
                    .display()
                    .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL);
        // @formatter:on

        and("no metrics there");
        assertSimulationResult(TASK_USER_RECOMPUTATION_SIMULATED.oid, "after first run")
                .display()
                .assertMetricValueByEventMark(MARK_FOCUS_ACTIVATED.oid, BigDecimal.ZERO); // no commit yet -> no metrics

        and("some processed objects are present");
        assertProcessedObjects(TASK_USER_RECOMPUTATION_SIMULATED.oid, "after first run")
                .display()
                .assertSizeBetween(1, USERS - 1);

        when("task is resumed");
        TASK_USER_RECOMPUTATION_SIMULATED.resumeAndWaitForFinish(result);

        then("everything is processed");
        // @formatter:off
        TASK_USER_RECOMPUTATION_SIMULATED.doAssert("after finish")
                .assertClosed()
                .activityState(ActivityPath.empty())
                    .display()
                    .assertRealizationState(ActivityRealizationStateType.COMPLETE);
        // @formatter:on

        and("metrics are OK");
        assertSimulationResult(TASK_USER_RECOMPUTATION_SIMULATED.oid, "after finish")
                .display()
                .assertMetricValueByEventMark(MARK_FOCUS_ACTIVATED.oid, BigDecimal.valueOf(USERS));

        and("correct # of processed objects is present");
        assertProcessedObjects(TASK_USER_RECOMPUTATION_SIMULATED.oid, "after finish")
                .assertSize(USERS);
    }
}
