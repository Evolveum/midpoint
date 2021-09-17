/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Utilities related to the helper activity tree state overview structure (maintained in the root task).
 */
public class ActivityStateOverviewUtil {

    public static final ItemPath ACTIVITY_TREE_STATE_OVERVIEW_PATH =
            ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TREE, ActivityTreeStateType.F_ACTIVITY);

    public static @NotNull ActivityStateOverviewType findOrCreateEntry(@NotNull ActivityStateOverviewType current,
            @NotNull ActivityPath path) {
        return findOrCreateEntry(current, path, true);
    }

    public static @Nullable ActivityStateOverviewType findEntry(@NotNull ActivityStateOverviewType current,
            @NotNull ActivityPath path) {
        return findOrCreateEntry(current, path, false);
    }

    /**
     * Finds or creates a state overview entry for given activity path.
     */
    @Contract("_, _, true -> !null")
    private static @Nullable ActivityStateOverviewType findOrCreateEntry(@NotNull ActivityStateOverviewType current,
            @NotNull ActivityPath path, boolean create) {
        if (path.isEmpty()) {
            return current;
        }
        ActivityStateOverviewType childEntry = findOrCreateChildEntry(current, path.first(), create);
        if (childEntry == null) {
            return null;
        }
        return findOrCreateEntry(childEntry, path.rest(), create);
    }

    @Contract("_, _, true -> !null")
    public static @Nullable ActivityStateOverviewType findOrCreateChildEntry(@NotNull ActivityStateOverviewType current,
            String identifier, boolean create) {
        List<ActivityStateOverviewType> matching = current.getActivity().stream()
                .filter(a -> Objects.equals(a.getIdentifier(), identifier))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            if (create) {
                ActivityStateOverviewType newEntry = new ActivityStateOverviewType()
                        .identifier(identifier);
                current.getActivity().add(newEntry);
                return newEntry;
            } else {
                return null;
            }
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new IllegalStateException("State overview entry " + current + " contains " + matching.size() + " entries " +
                    "for activity identifier '" + identifier + "': " + matching);
        }
    }

    public static @NotNull ActivityStateOverviewType getOrCreateStateOverview(@NotNull TaskType task) {
        return task.getActivityState() != null &&
                task.getActivityState().getTree() != null &&
                task.getActivityState().getTree().getActivity() != null ?
                task.getActivityState().getTree().getActivity() : new ActivityStateOverviewType();
    }

    public static @Nullable ActivityStateOverviewType getStateOverview(@NotNull TaskType task) {
        return task.getActivityState() != null &&
                task.getActivityState().getTree() != null ?
                task.getActivityState().getTree().getActivity() : null;
    }

    static @Nullable ActivityStateOverviewType getStateOverview(@NotNull TaskType task,
            @NotNull ActivityPath activityPath) {
        var root = getStateOverview(task);
        return root != null ? findEntry(root, activityPath) : null;
    }

    static boolean hasStateOverview(@NotNull TaskType task) {
        return getStateOverview(task) != null;
    }

    public static ActivityTaskStateOverviewType findOrCreateTaskEntry(@NotNull ActivityStateOverviewType entry,
            @NotNull ObjectReferenceType taskRef) {
        argCheck(taskRef.getOid() != null, "OID is null in %s", taskRef);
        List<ActivityTaskStateOverviewType> matching = entry.getTask().stream()
                .filter(t -> t.getTaskRef().getOid().equals(taskRef.getOid()))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            ActivityTaskStateOverviewType newEntry = new ActivityTaskStateOverviewType()
                    .taskRef(taskRef.clone());
            entry.getTask().add(newEntry);
            return newEntry;
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new IllegalStateException("State overview entry " + entry + " contains " + matching.size() + " entries " +
                    "for task OID '" + taskRef.getOid() + "': " + matching);
        }
    }

    /**
     * Visits activity state overview objects in depth-first manner.
     *
     * Similar to {@link ActivityTreeUtil#processStates(TaskType, TaskResolver, ActivityTreeUtil.ActivityStateProcessor)}.
     * But this method has its life easier, because the structure of state overview objects is much simpler;
     * and contained in a single object (does not cross task boundaries).
     */
    static void acceptStateOverviewVisitor(@NotNull TaskType rootTask, @NotNull StateOverviewVisitor visitor) {
        ActivityStateOverviewType rootState = getStateOverview(rootTask);
        if (rootState != null) {
            acceptStateOverviewVisitor(rootState, visitor);
        }
    }

    public static void acceptStateOverviewVisitor(@NotNull ActivityStateOverviewType state,
            @NotNull StateOverviewVisitor visitor) {
        visitor.visit(state);
        state.getActivity().forEach(sub -> acceptStateOverviewVisitor(sub, visitor));
    }

    public interface StateOverviewVisitor {

        /** Visits given state overview object. When needed, we may add ActivityPath argument here. */
        void visit(@NotNull ActivityStateOverviewType state);
    }
}
