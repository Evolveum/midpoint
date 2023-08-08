/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.tasks;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.DeltaExecutionPreprocessor;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Updates "indexed" values in task objects; currently {@link TaskType#getAffectedObjects()}.
 */
@Component
public class TaskOperationalDataManager implements DeltaExecutionPreprocessor {

    private static final Trace LOGGER = TraceManager.getTrace(TaskOperationalDataManager.class);

    @Autowired private PrismContext prismContext;
    @Autowired private TaskActivityManager activityManager;

    /** Computes or re-computes "affected objects" on task being added. */
    public <O extends ObjectType> void updateOnElementAdd(@NotNull O objectToAdd, OperationResult result) {
        if (!(objectToAdd instanceof TaskType taskToAdd)) {
            LOGGER.trace("Not a TaskType: {}", objectToAdd);
            return;
        }
        taskToAdd.setAffectedObjects(
                computeAffectedObjectsChecked(taskToAdd, result));
    }

    /**
     * Updates "affected objects" data on task modification
     * (by adding necessary changes to the `delta` parameter.)
     */
    public <O extends ObjectType> void updateOnElementModify(
            O current,
            @NotNull ObjectDelta<O> delta,
            @NotNull Class<O> objectClass,
            @NotNull LensElementContext<O> elementContext,
            OperationResult result) throws SchemaException {
        if (!TaskType.class.isAssignableFrom(objectClass)
                || !(current instanceof TaskType)) {
            LOGGER.trace("Not a TaskType or no current object: {}, {}", objectClass, current);
            return;
        }
        if (!delta.hasRelatedDelta(TaskType.F_ACTIVITY)) {
            LOGGER.trace("No change in activity: {}", delta);
            return;
        }
        TaskType expectedNew = (TaskType) elementContext.getObjectNewRequired().asObjectable();

        delta.addModifications(
                computeAffectedObjectsDeltas(
                        expectedNew,
                        computeAffectedObjectsChecked(expectedNew, result)));
    }

    private @Nullable TaskAffectedObjectsType computeAffectedObjectsChecked(TaskType task, OperationResult result) {
        try {
            return activityManager.computeAffectedObjects(task.getActivity());
        } catch (CommonException e) {
            // This can happen e.g. if the task is misconfigured. We don't want the operation to fail here.
            LoggingUtils.logException(LOGGER, "Couldn't compute affected objects for task {}", e, task);
            result.recordWarning("Couldn't compute affected objects: " + e.getMessage(), e);
            return null;
        }
    }

    private Collection<? extends ItemDelta<?, ?>> computeAffectedObjectsDeltas(
            @NotNull TaskType expectedNewTask,
            @Nullable TaskAffectedObjectsType newAffectedObjects)
            throws SchemaException {
        var existingAffected = expectedNewTask.getAffectedObjects();
        if (existingAffected == null) {
            LOGGER.trace("No 'affected objects' data in new task object -> adding the value 'as is'");
            if (newAffectedObjects == null) {
                return List.of();
            } else {
                return prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_AFFECTED_OBJECTS)
                        .add(newAffectedObjects)
                        .asItemDeltas();
            }
        } else {
            // We clone the identity to remove path information from it (the root will be the PCV itself).
            //noinspection unchecked
            PrismContainerValue<TaskAffectedObjectsType> existingAffectedCloned =
                    existingAffected.clone().asPrismContainerValue();
            LOGGER.trace("Existing 'affected objects' data found -> computing a delta; the old value is:\n{}",
                    existingAffectedCloned.debugDumpLazily(1));
            if (newAffectedObjects == null) {
                LOGGER.trace("No new 'affected objects' data should be there, removing the old value");
                return prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_AFFECTED_OBJECTS)
                        .replace()
                        .asItemDeltas();
            } else {
                //noinspection rawtypes
                Collection<? extends ItemDelta> differences =
                        existingAffectedCloned.diff(
                                newAffectedObjects.asPrismContainerValue(),
                                EquivalenceStrategy.DATA);
                // Now we re-add the path information to the item deltas
                differences.forEach(
                        delta ->
                                delta.setParentPath(
                                        ItemPath.create(
                                                TaskType.F_AFFECTED_OBJECTS,
                                                delta.getParentPath())));
                LOGGER.trace("Computed 'affected objects' deltas:\n{}", DebugUtil.debugDumpLazily(differences, 1));
                //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
                return (Collection<? extends ItemDelta<?, ?>>) differences;
            }
        }
    }
}
