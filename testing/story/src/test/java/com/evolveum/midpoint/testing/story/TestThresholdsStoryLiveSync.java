/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public abstract class TestThresholdsStoryLiveSync extends TestThresholdsStory {

    @Override
    protected Collection<ActivityState> getExecutionStates(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task task = taskManager.getTaskPlain(getTaskOid(), result);
        return List.of(
                ActivityState.getActivityStateUpwards(
                        ActivityPath.empty(),
                        task,
                        AbstractActivityWorkStateType.COMPLEX_TYPE,
                        result));
    }

    @Override
    Consumer<PrismObject<TaskType>> getWorkerThreadsCustomizer() {
        return rootActivityWorkerThreadsCustomizer(getWorkerThreads());
    }
}
