/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.function.Consumer;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySubtaskDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Multi-node flavor of {@link TestFocusPolicyHrScenario}: the composition is distributed into subtasks (each
 * reconciliation child runs in its own subtask) and each child additionally uses worker threads. The simulate
 * reconciliation therefore suspends in its own subtask, so the initial-suspend assertion is overridden.
 */
public class TestFocusPolicyHrScenarioMultiNode extends TestFocusPolicyHrScenario {

    private static final int THREADS = 3;

    @Override
    protected Consumer<PrismObject<TaskType>> topology() {
        return taskObj -> {
            ActivityDefinitionType root = taskObj.asObjectable().getActivity();
            ActivityDefinitionUtil.findOrCreateDistribution(root).setSubtasks(new ActivitySubtaskDefinitionType());
            for (ActivityDefinitionType child : root.getComposition().getActivity()) {
                ActivityDefinitionUtil.findOrCreateDistribution(child).setWorkerThreads(THREADS);
            }
        };
    }

    @Override
    protected void assertSimulateSuspended(String taskOid, String counterId) throws Exception {
        // @formatter:off
        assertTaskTree(taskOid, "after run")
                .display()
                .assertSuspended()
                .subtask("simulate", false)
                    .display()
                    .assertInProgress()
                    .assertSuspended()
                    .activityState(SIMULATE)
                        .previewModePolicyRulesCounters()
                            .assertCounterMinMax(counterId, DELETE_THRESHOLD, DELETE_THRESHOLD + 3);
        // @formatter:on
    }

    @Override
    protected void assertRunSucceeded(String taskOid) throws Exception {
        String simulateId = ActivityPolicyUtils.buildPolicyIdentifier(getTask(taskOid), SIMULATE, "max-deleted", true);
        String executeId = ActivityPolicyUtils.buildPolicyIdentifier(getTask(taskOid), EXECUTE, "max-deleted-execute", true);
        // @formatter:off
        assertTaskTree(taskOid, "after successful run")
                .display()
                .assertClosed()
                .assertSuccess()
                .subtask("simulate", false)
                    .display()
                    .assertClosed()
                    .activityState(SIMULATE)
                        .assertNoPolicies()
                        .previewModePolicyRulesCounters()
                            .assertCounterMinMax(simulateId, 1, DELETE_THRESHOLD - 1)
                            .end()
                        .end()
                    .end()
                .subtask("execute", false)
                    .display()
                    .assertClosed()
                    .activityState(EXECUTE)
                        .assertNoPolicies()
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounterMinMax(executeId, 1, DELETE_THRESHOLD - 1);
        // @formatter:on
    }
}
