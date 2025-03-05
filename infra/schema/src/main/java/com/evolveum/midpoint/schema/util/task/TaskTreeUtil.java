/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utilities related to task trees.
 *
 * They include working with subtasks.
 */
public class TaskTreeUtil {
    /**
     * Returns a stream of the task and all of its subtasks.
     *
     * Expects that the subtasks are resolved into full objects.
     * I.e. does not do task ID resolution itself: if a subtask is not already resolved, throws an exception.
     */
    @NotNull
    public static Stream<TaskType> getAllTasksStream(TaskType root) {
        return Stream.concat(Stream.of(root),
                getResolvedSubtasks(root).stream().flatMap(TaskTreeUtil::getAllTasksStream));
    }

    /**
     * Get resolved direct subtasks from a task.
     *
     * Expects that the subtasks are resolved into full objects.
     */
    public static List<TaskType> getResolvedSubtasks(TaskType parent) {
        return getResolvedSubtasks(parent, TaskResolver.empty());
    }

    public static List<TaskType> getResolvedSubtasks(TaskType parent, TaskResolver taskResolver) {
        List<TaskType> rv = new ArrayList<>();
        for (ObjectReferenceType childRef : parent.getSubtaskRef()) {
            if (childRef.getOid() == null && childRef.getObject() == null) {
                continue;
            }
            PrismObject<TaskType> child = childRef.getObject();
            @NotNull TaskType resolved;
            if (child != null) {
                resolved = child.asObjectable();
            } else {
                try {
                    resolved = taskResolver.resolve(childRef.getOid());
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't resolve subtask " + childRef.getOid() + " of " + parent +
                            " because of schema exception", e);
                } catch (ObjectNotFoundException e) {
                    throw new IllegalStateException("Unresolvable subtaskRef in " + parent + ": " + childRef);
                }
            }
            rv.add(resolved);
        }
        return rv;
    }

    /**
     * Adds a subtask to parent children list.
     */
    public static void addSubtask(TaskType parent, TaskType child) {
        parent.getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(child));
    }

    public static TaskType findChild(TaskType parent, String childOid) {
        for (TaskType subtask : getResolvedSubtasks(parent)) {
            if (childOid.equals(subtask.getOid())) {
                return subtask;
            }
        }
        return null;
    }

    public static TaskType findChildIfResolved(TaskType parent, String childOid) {
        if (allSubtasksAreResolved(parent)) {
            return findChild(parent, childOid);
        } else {
            return null;
        }
    }

    public static boolean allSubtasksAreResolved(TaskType parent) {
        return parent.getSubtaskRef().stream()
                .noneMatch(childRef -> childRef.getOid() != null && childRef.getObject() == null);
    }
}
