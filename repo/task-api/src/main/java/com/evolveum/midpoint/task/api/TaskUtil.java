/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods related to the {@link Task} objects.
 *
 * The ones that refer only to `TaskType` objects are located in the schema module, e.g. in {@link TaskTypeUtil} class.
 */
public class TaskUtil {

    public static List<String> tasksToOids(List<? extends Task> tasks) {
        return tasks.stream().map(Task::getOid).collect(Collectors.toList());
    }

    public static Task findByIdentifier(@NotNull String identifier, @NotNull Collection<Task> tasks) {
        return tasks.stream().filter(t -> identifier.equals(t.getTaskIdentifier())).findFirst().orElse(null);
    }

    public static @NotNull ExecutionModeType getExecutionMode(Task task) {
        if (task instanceof RunningTask) {
            return ((RunningTask) task).getActivityExecutionMode();
        } else {
            return ExecutionModeType.FULL;
        }
    }

    @Deprecated // Should be replaced by new "4.7" task execution mode
    public static boolean isDryRun(Task task) {
        return getExecutionMode(task) == ExecutionModeType.DRY_RUN;
    }

    @Deprecated // Should be replaced by new "4.7" task execution mode
    public static boolean isExecute(Task task) {
        return getExecutionMode(task) == ExecutionModeType.FULL;
    }

    public static boolean findExtensionItemValueInThisOrParent(Task task, QName path, boolean defaultValue) throws SchemaException {
        Boolean rawValue = findExtensionItemValueInThisOrParent(task, path);
        return rawValue != null ? rawValue : defaultValue;
    }

    private static Boolean findExtensionItemValueInThisOrParent(Task task, QName path) throws SchemaException {
        Boolean value = findExtensionItemValue(task, path);
        if (value != null) {
            return value;
        }
        if (task instanceof RunningLightweightTask) {
            RunningLightweightTask runningTask = (RunningLightweightTask) task;
            if (runningTask.getLightweightTaskParent() != null) {
                return findExtensionItemValue(runningTask.getLightweightTaskParent(), path);
            }
        }
        return null;
    }

    private static Boolean findExtensionItemValue(Task task, QName path) throws SchemaException {
        Validate.notNull(task, "Task must not be null.");
        PrismProperty<Boolean> item = task.getExtensionPropertyOrClone(ItemName.fromQName(path));
        if (item == null || item.isEmpty()) {
            return null;
        }
        if (item.getValues().size() > 1) {
            throw new SchemaException("Unexpected number of values for option '" + path + "'.");
        }
        return item.getValues().iterator().next().getValue();
    }

    public static List<? extends Task> getLeafTasks(List<? extends Task> allSubtasksInTree) {
        return allSubtasksInTree.stream()
                .filter(task -> !hasChildren(task, allSubtasksInTree))
                .collect(Collectors.toList());
    }

    private static boolean hasChildren(Task task, List<? extends Task> allTasks) {
        return allTasks.stream()
                .anyMatch(t -> task.getTaskIdentifier().equals(t.getParent()));
    }
}
