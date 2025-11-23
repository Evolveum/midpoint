/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests basic functionality of reconciliation tasks.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTaskMultiNodeTailored extends TestReconTask {

    private static final TestTask TASK_RECONCILIATION_MULTINODE =
            TestTask.file(TEST_DIR, "task-reconciliation-multinode-tailored.xml", "fa96680f-d0a0-40c9-873b-bd49a63bf9be");

    @Override
    TestObject<TaskType> getReconciliationTask() {
        return TASK_RECONCILIATION_MULTINODE;
    }

    @Override
    boolean isMultiNode() {
        return true;
    }

    /**
     * TODO failing test
     */
    @Test(enabled = false)
    public void test200SuspendAndDeleteWorkersAndWorkState() throws Exception {
        when();

        OperationResult result = getTestOperationResult();

        String rootTaskOid = getReconciliationTaskOid();

        Task origRootTask = taskManager.getTask(rootTaskOid, null, result);
        restartTask(rootTaskOid, result);

        waitForTaskStatusUpdated(rootTaskOid, "Waiting for resourceObjects subtask to start", new Checker() {

            @Override
            public boolean check() throws CommonException {
                var task = taskManager.getTaskWithResult(rootTaskOid, result);
                var activitiesState = task.getActivitiesStateOrClone();
                if (activitiesState == null) {
                    return false;
                }
                var activity = activitiesState.getActivity();
                if (activity == null) {
                    return false;
                }

                ActivityStateType rootState = ActivityStateUtil.getActivityState(task.getRawTaskObjectClonedIfNecessary().asObjectable(),
                        ActivityPath.empty());
                if (rootState == null) {
                    return false;
                }

                ActivityStateType resourceObjectsState = rootState.getActivity().stream()
                        .filter(a -> ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID.equals(a.getIdentifier()))
                        .findFirst()
                        .orElse(null);

                if (resourceObjectsState == null) {
                    return false;
                }

                return resourceObjectsState.getRealizationState() == ActivityRealizationStateType.IN_PROGRESS_DELEGATED;
            }
        }, 30000L);

        taskManager.suspendTaskTree(rootTaskOid, 2000L, result);

        // Verify that there are 4 subtasks (2 workers + 2 coordinators)
        List<? extends Task> subtasks = origRootTask.listSubtasksDeeply(true, result);
        Assertions.assertThat(subtasks).hasSize(4);

        // Now delete workers and their work state
        activityManager.deleteActivityStateAndWorkers(rootTaskOid, true, 2000L, result);

        // Verify that there are only 2 subtasks left (the coordinators)
        subtasks = origRootTask.listSubtasksDeeply(true, result);
        Assertions.assertThat(subtasks).hasSize(2);

        then();

        taskManager.resumeTaskTree(rootTaskOid, result);
        waitForTaskFinish(rootTaskOid, 30000);

        // @formatter:off
        assertTaskTree(getReconciliationTaskOid(), "after")
                .display()
                .assertSuccess()
                .assertClosed();
        // @formatter:on
    }

    private String getReconciliationTaskOid() {
        return getReconciliationTask().oid;
    }
}
