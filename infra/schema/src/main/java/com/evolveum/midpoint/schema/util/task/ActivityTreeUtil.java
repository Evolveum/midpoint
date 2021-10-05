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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Utility methods for navigating throughout activity trees, potentially distributed throughout a task tree.
 * (There are also variants that traverse only local activities in a task.)
 */
public class ActivityTreeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTreeUtil.class);

    /**
     * Transforms activity state objects into custom ones, organized into a tree.
     * Delegation states are ignored. Distribution states are considered, and their workers' states
     * are packed along them.
     */
    public static <X> @NotNull TreeNode<X> transformStates(@NotNull TaskType rootTask,
            @NotNull TaskResolver resolver,
            @NotNull ActivityStateTransformer<X> transformer) {
        TreeNode<X> root = new TreeNode<>();
        processStates(rootTask, resolver, new TreeTransformingProcessor<>(transformer, root));
        return root;
    }

    /**
     * Transforms task-local activity state objects into custom ones, organized into a tree.
     * Does not distinguish between local, delegated, and distributed states: all are treated the same.
     */
    @SuppressWarnings("WeakerAccess")
    public static <X> @NotNull TreeNode<X> transformLocalStates(@NotNull TaskType rootTask,
            @NotNull LocalActivityStateTransformer<X> transformer) {
        TreeNode<X> root = new TreeNode<>();
        processLocalStates(rootTask, transformer::transform);
        return root;
    }

    /**
     * Processes activity state objects using the same rules as in {@link #transformStates(TaskType, TaskResolver, ActivityStateTransformer)}:
     * delegation states are ignored, distribution states are considered, along with all their workers' states.
     */
    public static void processStates(@NotNull TaskType rootTask,
            @NotNull TaskResolver resolver,
            @NotNull ActivityStateProcessor processor) {
        processStates(getLocalRootPath(rootTask), getLocalRootState(rootTask), rootTask, resolver, processor);
    }

    /**
     * Processes local activity state objects using the same rules as in
     * {@link #transformLocalStates(TaskType, LocalActivityStateTransformer)} (TaskType, ActivityStateTransformer)}:
     * all states are treated the same.
     */
    public static void processLocalStates(@NotNull TaskType task,
            @NotNull LocalActivityStateProcessor processor) {
        ActivityStateType localRootState = getLocalRootState(task);
        if (localRootState != null) {
            processLocalStates(getLocalRootPath(task), localRootState, processor);
        }
    }

    /**
     * Special case of {@link #transformStates(TaskType, TaskResolver, ActivityStateTransformer)}: creates a {@link TreeNode}
     * of {@link ActivityStateInContext} objects.
     */
    public static @NotNull TreeNode<ActivityStateInContext> toStateTree(@NotNull TaskType rootTask,
            @NotNull TaskResolver resolver) {
        return ActivityTreeUtil.transformStates(rootTask, resolver, ActivityStateInContext::new);
    }

    /**
     * Creates a {@link TreeNode} of {@link ActivityStateInLocalContext} objects for activities locally contained in the task.
     */
    public static @NotNull TreeNode<ActivityStateInLocalContext> toLocalStateTree(@NotNull TaskType task) {
        return ActivityTreeUtil.transformLocalStates(task, ActivityStateInLocalContext::new);
    }

    private static ActivityPath getLocalRootPath(TaskType task) {
        return task.getActivityState() != null ?
                ActivityPath.fromBean(task.getActivityState().getLocalRoot()) : ActivityPath.empty();
    }

    private static ActivityStateType getLocalRootState(TaskType task) {
        return task.getActivityState() != null ?
                task.getActivityState().getActivity() : null;
    }

    private static void processStates(@NotNull ActivityPath path, @Nullable ActivityStateType state,
            @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityStateProcessor processor) {
        if (state != null && ActivityStateUtil.isDelegated(state)) {
            processDelegatedState(path, state, task, resolver, processor);
        } else {
            processNonDelegatedState(path, state, task, resolver, processor);
        }
    }

    private static void processNonDelegatedState(@NotNull ActivityPath path,
            @Nullable ActivityStateType state, @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityStateProcessor processor) {

        List<ActivityStateType> workerStates = collectWorkerStates(path, state, task, resolver);
        processor.process(path, state, workerStates, task);

        if (state != null) {
            for (ActivityStateType childState : state.getActivity()) {
                processor.toNewChild(childState);
                processStates(path.append(childState.getIdentifier()), childState, task, resolver, processor);
                processor.toParent();
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

    private static void processDelegatedState(@NotNull ActivityPath path, @NotNull ActivityStateType state,
            @NotNull TaskType task, @NotNull TaskResolver resolver,
            @NotNull ActivityTreeUtil.ActivityStateProcessor processor) {
        ObjectReferenceType delegateTaskRef = getDelegatedTaskRef(state);
        TaskType delegateTask = getSubtask(delegateTaskRef, path, task, resolver);
        if (delegateTask != null) {
            processStates(path, getLocalRootState(delegateTask), delegateTask, resolver, processor);
        } else {
            // nothing to process
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

    /**
     * We know this by comparing task OIDs (except for worker ones): all must be the same to be non-delegated.
     */
    public static boolean hasDelegatedActivity(TreeNode<ActivityStateInContext> node) {
        Set<String> oids = new HashSet<>();
        node.acceptDepthFirst(n ->
                oids.add(
                        requireNonNull(n.getUserObject())
                                .getTask()
                                .getOid()));
        return oids.size() > 1;
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

    private static void processLocalStates(@NotNull ActivityPath path, @NotNull ActivityStateType state,
            @NotNull LocalActivityStateProcessor processor) {
        processor.process(path, state);
        state.getActivity().forEach(child ->
                processLocalStates(
                        path.append(child.getIdentifier()),
                        child,
                        processor));
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

    @Experimental
    @FunctionalInterface
    public interface LocalActivityStateTransformer<X> {
        X transform(@NotNull ActivityPath path, @NotNull ActivityStateType state);
    }

    @Experimental
    @FunctionalInterface
    public interface ActivityStateProcessor {

        /**
         * Called when relevant state is found.
         *
         * Worker states are present in the case of distributed coordinator-workers scenario.
         */
        void process(@NotNull ActivityPath path, @Nullable ActivityStateType state,
                @Nullable List<ActivityStateType> workerStates, @NotNull TaskType task);

        /**
         * Called when new child is entered into.
         *
         * @param childState State of the child. Often can be ignored.
         */
        default void toNewChild(@NotNull ActivityStateType childState) {
        }

        /**
         * Called when new child is processed, and we want to return to the parent.
         */
        default void toParent() {
        }
    }

    @Experimental
    @FunctionalInterface
    public interface LocalActivityStateProcessor {
        void process(@NotNull ActivityPath path, @NotNull ActivityStateType state);
    }

    /**
     * Activity state with all the necessary context: the path, the task, and the partial states of coordinated workers.
     * Maybe we should find better name.
     */
    public static class ActivityStateInContext implements Serializable {

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

        public boolean isCoordinator() {
            return getWorkerStates() != null;
        }

        @Override
        public String toString() {
            return "ActivityStateInContext{" +
                    "activityPath=" + activityPath +
                    ", task=" + task +
                    '}';
        }
    }

    /**
     * Activity state in local context: just the path and the state.
     */
    public static class ActivityStateInLocalContext implements Serializable {
        @NotNull private final ActivityPath activityPath;
        @NotNull private final ActivityStateType activityState;

        ActivityStateInLocalContext(@NotNull ActivityPath activityPath, @NotNull ActivityStateType activityState) {
            this.activityPath = activityPath;
            this.activityState = activityState;
        }

        public @NotNull ActivityPath getActivityPath() {
            return activityPath;
        }

        public @NotNull ActivityStateType getActivityState() {
            return activityState;
        }
    }
}
