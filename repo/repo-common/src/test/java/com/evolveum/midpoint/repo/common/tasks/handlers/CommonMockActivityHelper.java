/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityRun.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType.F_EXTENSION;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;

@Component
public class CommonMockActivityHelper {

    public static final ItemName EXECUTION_COUNT_NAME = new ItemName(NS_EXT, "executionCount");
    private static final ItemPath EXECUTION_COUNT_PATH = ItemPath.create(F_EXTENSION, EXECUTION_COUNT_NAME);

    private static final ItemName LAST_MESSAGE_NAME = new ItemName(NS_EXT, "lastMessage");
    private static final ItemPath LAST_MESSAGE_PATH = ItemPath.create(F_EXTENSION, LAST_MESSAGE_NAME);

    //region Execution count
    public void increaseExecutionCount(@NotNull CurrentActivityState<?> activityState, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        int count = or0(activityState.getWorkStatePropertyRealValue(EXECUTION_COUNT_PATH, Integer.class));
        activityState.setWorkStateItemRealValues(EXECUTION_COUNT_PATH, count + 1);
        activityState.flushPendingTaskModifications(result);
    }

    public void failIfNeeded(@NotNull AbstractActivityRun<?, ?, ?> activityRun, int initialFailures) {
        int count = activityRun.getActivityState().getWorkStatePropertyRealValue(EXECUTION_COUNT_PATH, Integer.class);
        if (count <= initialFailures) {
            throw new SystemException(String.format("Failed execution #%d. Expected initial failures: %d.",
                    count, initialFailures));
        }
    }
    //endregion

    //region Last message
    public String getLastMessage(@NotNull ActivityState activityState) {
        return activityState.getWorkStatePropertyRealValue(LAST_MESSAGE_PATH, String.class);
    }

    public void setLastMessage(@NotNull ActivityState activityState, String message, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        activityState.setWorkStateItemRealValues(LAST_MESSAGE_PATH, message);
        activityState.flushPendingTaskModifications(result);
    }
    //endregion
}
