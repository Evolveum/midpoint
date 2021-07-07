/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for navigating throughout activity trees, potentially distributed throughout a task tree.
 */
public class ActivityTreeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTreeUtil.class);

    /**
     * Transforms activity state objects into custom ones, organized into a tree.
     * Delegation states are ignored. Distribution states are considered, and their workers' states are (currently) ignored.
     */
    public static <X> @NotNull TreeNode<X> transformStates(@NotNull TaskType rootTask,
            @NotNull TaskResolver resolver,
            @NotNull ActivityStateTransformer<X> transformer) {
        TreeNode<X> root = new TreeNode<>();
        transformStates(root, getLocalRootPath(rootTask), getLocalRootState(rootTask), rootTask, resolver, transformer);
        return root;
    }

    public static @NotNull TreeNode<ActivityStateInContext> toStateTree(@NotNull TaskType rootTask,
            @NotNull TaskResolver resolver) {
        return ActivityTreeUtil.transformStates(rootTask, resolver, ActivityStateInContext::new);
    }

    private static ActivityPath getLocalRootPath(TaskType task) {
        return task.getActivityState() != null ?
                ActivityPath.fromBean(task.getActivityState().getLocalRoot()) : ActivityPath.empty();
    }

    private static ActivityStateType getLocalRootState(TaskType task) {
        return task.getActivityState() != null ?
                task.getActivityState().getActivity() : null;
    }

    private static <X> void transformStates(@NotNull TreeNode<X> transformed, @NotNull ActivityPath path,
            @Nullable ActivityStateType state, @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityTreeUtil.ActivityStateTransformer<X> transformer) {
        if (state != null && ActivityStateUtil.isDelegated(state)) {
            processDelegatedState(transformed, path, state, task, resolver, transformer);
        } else {
            processNonDelegatedState(transformed, path, state, task, resolver, transformer);
        }
    }

    private static <X> void processNonDelegatedState(@NotNull TreeNode<X> transformed, @NotNull ActivityPath path,
            @Nullable ActivityStateType state, @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityStateTransformer<X> transformer) {

        List<ActivityStateType> workerStates = collectWorkerStates(path, state, task, resolver);
        transformed.setUserObject(transformer.transform(path, state, workerStates, task));

        if (state != null) {
            for (ActivityStateType childState : state.getActivity()) {
                TreeNode<X> child = new TreeNode<>();
                transformed.add(child);
                transformStates(child, path.append(childState.getIdentifier()), childState, task, resolver, transformer);
            }
        }
    }

    private static List<ActivityStateType> collectWorkerStates(@NotNull ActivityPath path, ActivityStateType state,
            @NotNull TaskType task, @NotNull TaskResolver resolver) {
        if (BucketingUtil.isCoordinator(state)) {
            return ActivityTreeUtil.getSubtasksForPath(task, path, resolver).stream()
                    .map(subtask -> ActivityStateUtil.getActivityState(subtask.getActivityState(), path))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private static <X> void processDelegatedState(@NotNull TreeNode<X> transformed, @NotNull ActivityPath path,
            @NotNull ActivityStateType state, @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityTreeUtil.ActivityStateTransformer<X> transformer) {
        ObjectReferenceType delegateTaskRef = getDelegatedTaskRef(state);
        TaskType delegateTask = getSubtask(delegateTaskRef, path, task, resolver);
        if (delegateTask != null) {
            transformStates(transformed, path, getLocalRootState(delegateTask), delegateTask, resolver, transformer);
        } else {
            // nothing to report
        }
    }

    private static ObjectReferenceType getDelegatedTaskRef(ActivityStateType state) {
        AbstractActivityWorkStateType workState = state.getWorkState();
        return workState instanceof DelegationWorkStateType ? ((DelegationWorkStateType) workState).getTaskRef() : null;
    }

    private static TaskType getSubtask(ObjectReferenceType subtaskRef, ActivityPath path,
            TaskType task, TaskResolver resolver) {
        String subTaskOid = subtaskRef != null ? subtaskRef.getOid() : null;
        if (subTaskOid == null) {
            LOGGER.warn("No subtask for delegated activity '{}' in {}", path, task);
            return null;
        }
        TaskType inTask = TaskTreeUtil.findChildIfResolved(task, subTaskOid);
        if (inTask != null) {
            return inTask;
        }
        try {
            return resolver.resolve(subTaskOid);
        } catch (ObjectNotFoundException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve subtask {} for '{}' in {}", e, subTaskOid, task);
            return null;
        }
    }

    @Experimental
    @FunctionalInterface
    public interface ActivityStateTransformer<X> {

        /**
         * Worker states are present in the case of distributed coordinator-workers scenario.
         */
        X transform(@NotNull ActivityPath path, @Nullable ActivityStateType state,
                @Nullable List<ActivityStateType> workerStates, @NotNull TaskType task);
    }

    public static @NotNull List<TaskType> getSubtasksForPath(TaskType task, ActivityPath activityPath,
            TaskResolver taskResolver) {
        return TaskTreeUtil.getResolvedSubtasks(task, taskResolver).stream()
                .filter(t -> activityPath.equalsBean(ActivityStateUtil.getLocalRootPathBean(t.getActivityState())))
                .collect(Collectors.toList());
    }

    public static @NotNull List<ActivityStateType> getAllLocalStates(@NotNull TaskActivityStateType taskActivityState) {
        List<ActivityStateType> allStates = new ArrayList<>();
        collectLocalStates(allStates, taskActivityState.getActivity());
        return allStates;
    }

    private static void collectLocalStates(@NotNull List<ActivityStateType> allStates, @Nullable ActivityStateType state) {
        if (state == null) {
            return;
        }
        allStates.add(state);
        state.getActivity().forEach(child -> collectLocalStates(allStates, child));
    }

    /**
     * Activity state with all the necessary context: the path, the task, and the partial states of coordinated workers.
     * Maybe we should find better name.
     */
    public static class ActivityStateInContext {

        @NotNull private final ActivityPath activityPath;
        @Nullable private final ActivityStateType activityState;
        @Nullable private final List<ActivityStateType> workerStates;
        @NotNull private final TaskType task;

        ActivityStateInContext(@NotNull ActivityPath activityPath, @Nullable ActivityStateType activityState,
                @Nullable List<ActivityStateType> workerStates, @NotNull TaskType task) {
            this.activityPath = activityPath;
            this.activityState = activityState;
            this.workerStates = workerStates;
            this.task = task;
        }

        public @NotNull ActivityPath getActivityPath() {
            return activityPath;
        }

        public @Nullable ActivityStateType getActivityState() {
            return activityState;
        }

        public @Nullable List<ActivityStateType> getWorkerStates() {
            return workerStates;
        }

        public @NotNull TaskType getTask() {
            return task;
        }

        public @NotNull List<ActivityStateType> getAllStates() {
            return getAllStatesStream()
                    .collect(Collectors.toList());
        }

        public @NotNull Stream<ActivityStateType> getAllStatesStream() {
            return Stream.concat(
                    Stream.ofNullable(activityState),
                    workerStates != null ? workerStates.stream() : Stream.empty());
        }
    }
}
