/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static org.assertj.core.api.Assertions.assertThat;

public class TestThresholdsStoryReconExecuteMultinode extends TestThresholdsStoryReconExecute {

    /**
     * This task is used for common import tests drive by the superclass.
     */
    private static final TestObject<TaskType> TASK_RECONCILE_OPENDJ_EXECUTE_MULTINODE = TestObject.file(TEST_DIR,
            "task-opendj-reconcile-execute-multinode.xml", "04c62c99-8b43-4782-bd02-954f709fff98");

    @Override
    protected TestObject<TaskType> getTaskTestResource() {
        return TASK_RECONCILE_OPENDJ_EXECUTE_MULTINODE;
    }

    @Override
    protected boolean isMultiNode() {
        return true;
    }

    @Override
    TaskType assertTaskAfter() throws SchemaException, ObjectNotFoundException {
        TaskType coordinator = assertTaskTree(getTaskOid(), "after")
                .subtaskForPath(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .display()
                    .getObjectable();
        TaskType suspended = TaskTreeUtil.getAllTasksStream(coordinator)
                .filter(task -> task.getExecutionState() == TaskExecutionStateType.SUSPENDED)
                .findFirst().orElse(null);
        assertThat(suspended).as("suspended subtask").isNotNull();

        taskManager.suspendTaskTree(getTaskOid(), 5000L, getTestOperationResult()); // to be able to re-run it later
        return suspended;
    }
}
