/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public abstract class TestThresholdsStoryRecon extends TestThresholdsStory {

    @Override
    protected Collection<ActivityState> getExecutionStates(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task task = taskManager.getTaskPlain(getTaskOid(), result);
        // Brutal hack...
        List<ActivityState> states = new ArrayList<>();
        states.addAll(getStateIfExists(ActivityPath.empty(), result, task));
        states.addAll(getStateIfExists(ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_PATH, result, task));
        states.addAll(getStateIfExists(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PATH, result, task));
        states.addAll(getStateIfExists(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH, result, task));
        return states;
    }

    @NotNull
    private Collection<ActivityState> getStateIfExists(ActivityPath path, OperationResult result, Task task) {
        try {
            return List.of(
                    ActivityState.getActivityStateDownwards(
                            path,
                            task,
                            AbstractActivityWorkStateType.COMPLEX_TYPE,
                            result));
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    Consumer<PrismObject<TaskType>> getWorkerThreadsCustomizer() {
        return tailoringWorkerThreadsCustomizer(getWorkerThreads());
    }
}
