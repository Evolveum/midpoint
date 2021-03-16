/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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

    public static boolean isDryRun(Task task) throws SchemaException {
        return findExtensionItemValueInThisOrParent(task, SchemaConstants.MODEL_EXTENSION_DRY_RUN, false);
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
        if (task instanceof RunningTask) {
            RunningTask runningTask = (RunningTask) task;
            if (runningTask.isLightweightAsynchronousTask() && runningTask.getParentForLightweightAsynchronousTask() != null) {
                return findExtensionItemValue(runningTask.getParentForLightweightAsynchronousTask(), path);
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
}
