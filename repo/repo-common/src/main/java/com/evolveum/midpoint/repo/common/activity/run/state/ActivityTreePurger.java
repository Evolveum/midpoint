/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatePersistenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.PERMANENT_ERROR;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Responsible for purging detailed state of the activities, including worker and delegator tasks.
 *
 * This also includes:
 *
 * 1. Deletion of eligible subtasks. This is done here (and not elsewhere) because we need to know which
 * subtasks to purge and which not: and the distinction is similar to distinction when purging the state
 * itself (the state persistence).
 *
 * 2. Deletion of the task-level statistics. The reason is similar: we need to know which
 * activities are "persistent" and which are not. (The distinction is only approximate, because the task
 * statistics are common for all activities in the task. So we keep them if at least one of the activities
 * is persistent.)
 */
public class ActivityTreePurger {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTreePurger.class);

    /** Path of the activity whose state (and state of its descendants) is being purged. */
    @NotNull private final ActivityPath rootPath;

    /** Task in which we need to start the search for activities to purge. */
    @NotNull private final RunningTask runningTask;

    /**
     * If true, then we are restarting the {@link #rootPath} activity, so we need to increment the attempt number.
     * During one purge operation, there is only one activity being restarted. The attempt number of child activities is
     * simply cleared (reset).
     */
    private final boolean restarting;

    @NotNull private final CommonTaskBeans beans = CommonTaskBeans.get();

    /**
     * These paths contain something of interest. So states on the path to them should not be removed.
     */
    @NotNull private final Set<ActivityPath> pathsToKeep = new HashSet<>();

    public ActivityTreePurger(@NotNull ActivityPath rootPath, @NotNull RunningTask runningTask, boolean restarting) {
        this.rootPath = rootPath;
        this.runningTask = runningTask;
        this.restarting = restarting;
    }

    /**
     * Purges state (including task-level stats) from current task and its subtasks. Deletes subtasks if possible.
     *
     * * Pre: task is an execution root
     * * Post: task is refreshed
     */
    public void purge(OperationResult result) throws ActivityRunException {
        LOGGER.trace("Purging activity state for task {}, root path {}, restarting={}", runningTask, rootPath, restarting);
        try {
            boolean canDeleteRoot = purgeInTasksRecursively(runningTask, result);
            if (canDeleteRoot && rootPath.isEmpty()) { // TODO implement restarting statistics for non-root activities as well
                runningTask.restartCollectingStatisticsFromZero();
                runningTask.updateAndStoreStatisticsIntoRepository(true, result);
            }
        } catch (CommonException e) {
            throw new ActivityRunException("Couldn't purge activity tree state", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /**
     * Purges all activity states in a given task and all of its subtasks.
     *
     * Returns true if nothing of any relevance did remain in the task nor in the subtasks, so the task itself can be deleted
     * if needed.
     */
    private boolean purgeInTasksRecursively(Task currentRootTask, OperationResult result)
            throws CommonException {
        boolean noSubtasksLeft = purgeInSubtasks(currentRootTask, result);
        boolean canDeleteThisTask = purgeInTaskLocally(currentRootTask, result);
        return noSubtasksLeft && canDeleteThisTask;
    }

    private boolean purgeInTaskLocally(Task task, OperationResult result) throws CommonException {
        return new TaskStatePurger(task)
                .doPurge(result);
    }

    /**
     * Purges activity states in the subtasks. Deletes those subtasks if possible.
     *
     * @return true if there are no subtasks left
     */
    private boolean purgeInSubtasks(Task parent, OperationResult result) throws CommonException {
        LOGGER.trace("Purging in subtasks of {}, root to purge: {}", parent, rootPath);
        List<? extends Task> subtasks = parent.listSubtasks(true, result);
        for (Iterator<? extends Task> iterator = subtasks.iterator(); iterator.hasNext(); ) {
            Task subtask = iterator.next();
            LOGGER.trace("Considering subtask {}", subtask);
            TaskActivityStateType taskActivityState = subtask.getActivitiesStateOrClone();
            if (taskActivityState == null) {
                LOGGER.error("Non-activity related subtask {} of {}. Please resolve manually.", subtask, parent);
                continue;
            }
            ActivityPath subtaskRoot = ActivityPath.fromBean(taskActivityState.getLocalRoot());
            boolean canDeleteSubtask;
            if (subtaskRoot.startsWith(rootPath)) {
                LOGGER.trace("Activity path {} is inside the subtask (subtask root: {})", rootPath, subtaskRoot);
                canDeleteSubtask = purgeInTasksRecursively(subtask, result);
            } else if (rootPath.startsWith(subtaskRoot)) {
                LOGGER.trace("Activity path {} encompasses the whole subtask (subtask root: {})", rootPath, subtaskRoot);
                canDeleteSubtask = purgeInTasksRecursively(subtask, result);
            } else {
                LOGGER.trace("Activity path {} is not related to subtask (subtask root: {}). Skipping.", rootPath, subtaskRoot);
                canDeleteSubtask = false;
            }
            if (canDeleteSubtask) {
                LOGGER.info("Deleting obsolete subtask {}", subtask);
                beans.taskManager.deleteTask(subtask.getOid(), result);
                iterator.remove();
            }
        }
        return subtasks.isEmpty();
    }

    /**
     * Purges activity states in a single task.
     */
    private class TaskStatePurger {

        /** Task in which we are purging the state. */
        @NotNull private final Task task;

        /** Cached activity state of the task. */
        final TaskActivityStateType taskActivityState;

        /** Deltas to be applied to the task, gradually constructed. */
        @NotNull private final List<ItemDelta<?, ?>> deltas = new ArrayList<>();

        /** True if nothing of relevance remains in the task, so it can be deleted (if needed). */
        private boolean canDelete;

        private TaskStatePurger(@NotNull Task task) {
            this.task = task;
            this.taskActivityState = task.getActivitiesStateOrClone();
        }

        /**
         * Purges the state in the current task.
         * Assumes that task's children already have been processed.
         *
         * @return True if nothing of relevance remains in the task, so it can be deleted (if needed).
         */
        private boolean doPurge(OperationResult result) throws CommonException {
            if (taskActivityState == null || taskActivityState.getActivity() == null) {
                return true;
            }
            var taskRoot = ActivityPath.fromBean(taskActivityState.getLocalRoot());
            canDelete = true;
            doPurge(Context.root(taskRoot, taskActivityState));
            LOGGER.trace("Purging in {} resulted with canDelete={} and deltas:\n{}",
                    task, canDelete, DebugUtil.debugDumpLazily(deltas, 1));
            if (!deltas.isEmpty()) {
                beans.repositoryService.modifyObject(TaskType.class, task.getOid(), deltas, result);
                if (task instanceof RunningTask) {
                    task.refresh(result);
                }
            }
            return canDelete;
        }

        private void doPurge(Context ctx) throws CommonException {
            LOGGER.trace("doPurge called for {} (root to purge: {}), processing children", ctx, rootPath);
            if (!ctx.currentActivityPath.isRelatedTo(rootPath)) {
                LOGGER.trace("doPurge skipping {}, not related to root to purge ({})", ctx.currentActivityPath, rootPath);
                canDelete = false;
                return;
            }
            for (ActivityStateType child : ctx.currentState.getActivity()) {
                doPurge(ctx.forChild(child));
            }
            if (!ctx.currentActivityPath.startsWith(rootPath)) {
                LOGGER.trace("doPurge skipping {}, not inside root to purge ({})", ctx.currentActivityPath, rootPath);
                canDelete = false;
                return;
            }
            var restartingThis = restarting && ctx.currentActivityPath.equals(rootPath);
            LOGGER.trace("doPurge continuing with {}, paths to keep: {} (restarting this: {})",
                    ctx.currentActivityPath, pathsToKeep, restartingThis);
            if (!restartingThis && isTransient(ctx.currentState) && hasNoPersistentChild(ctx.currentActivityPath)) {
                removeCurrentState(ctx);
            } else {
                purgeCurrentState(ctx, restartingThis);
                canDelete = false;
            }
        }

        private boolean hasNoPersistentChild(ActivityPath activityPath) {
            return pathsToKeep.stream().noneMatch(
                    pathToKeep -> pathToKeep.startsWith(activityPath));
        }

        private void removeCurrentState(Context ctx) throws CommonException {
            if (ctx.isLocalRoot()) {
                deleteFromSingleValuedState(ctx);
            } else {
                deleteFromMultiValuedState(ctx, ctx.currentState.getId());
            }
        }

        private void deleteFromSingleValuedState(Context ctx) throws SchemaException {
            LOGGER.trace("Deleting from single-valued state: task = {}, activity path = '{}', item path = '{}'",
                    task, ctx.currentActivityPath, ctx.currentStateItemPath);

            swallow(
                    beans.prismContext.deltaFor(TaskType.class)
                            .item(ctx.currentStateItemPath).replace()
                            .asItemDeltas());
        }

        private void deleteFromMultiValuedState(Context ctx, Long id) throws SchemaException {
            LOGGER.trace("Deleting from multi-valued state: task = {}, activity path = '{}', item path = '{}' with id = {}",
                    task, ctx.currentActivityPath, ctx.currentStateItemPath, id);

            argCheck(id != null, "Null activity state PCV id in task %s activity path '%s' item path '%s'",
                    task, ctx.currentActivityPath, ctx.currentStateItemPath);
            swallow(
                    beans.prismContext.deltaFor(TaskType.class)
                            .item(ctx.currentStateItemPath.allExceptLast()).delete(new ActivityStateType().id(id))
                            .asItemDeltas());
        }

        private void purgeCurrentState(Context ctx, boolean restartingThis) throws SchemaException {
            if (!isTransient(ctx.currentState)) {
                pathsToKeep.add(ctx.currentActivityPath);
            }

            LOGGER.trace("Purging from multi: task = {}, activity path = '{}', item path = '{}' (restarting this = {})",
                    task, ctx.currentActivityPath, ctx.currentStateItemPath, restartingThis);

            Integer newExecAttempt;
            if (restartingThis) {
                Integer currentAttempt = ctx.currentState.getExecutionAttempt();
                newExecAttempt = Objects.requireNonNullElse(currentAttempt, 1) + 1;
            } else {
                newExecAttempt = null;
            }

            swallow(
                    beans.prismContext.deltaFor(TaskType.class)
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_STATE)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_ABORTING_INFORMATION)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_START_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_END_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_RUN_START_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_RUN_END_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_RESULT_STATUS)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_BUCKETING)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_SIMULATION)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_POLICIES)).replace() // TODO reconsider
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_EXECUTION_ATTEMPT)).replace(newExecAttempt)
                            .asItemDeltas());

            var currentStatePersistence = ctx.currentState.getPersistence();
            if (currentStatePersistence != ActivityStatePersistenceType.PERPETUAL) {
                // I.e. "single realization" or "perpetual except for statistics"
                swallow(
                        beans.prismContext.deltaFor(TaskType.class)
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_PROGRESS)).replace()
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_STATISTICS)).replace()
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_COUNTERS)).replace()
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_REPORTS)).replace() // TODO reconsider
                                .asItemDeltas());
            }

            if (currentStatePersistence == null || currentStatePersistence == ActivityStatePersistenceType.SINGLE_REALIZATION) {
                swallow(
                        beans.prismContext.deltaFor(TaskType.class)
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_WORK_STATE)).replace()
                                .asItemDeltas());
            }

            // keeping these:
            //  - F_IDENTIFIER
            //  - F_TASK_RUN_IDENTIFIER ???
            //  - F_PERSISTENCE
            //  - F_ACTIVITY (i.e., children)
        }

        private void swallow(Collection<ItemDelta<?, ?>> deltas) {
            this.deltas.addAll(deltas);
        }

        private boolean isTransient(ActivityStateType state) {
            ActivityStatePersistenceType persistence = state.getPersistence();
            return persistence == null || persistence == ActivityStatePersistenceType.SINGLE_REALIZATION;
        }
    }

    /**
     * Context for purging methods in {@link TaskStatePurger}.
     *
     * This class is at this level because Java 11 does not allow to have static classes embedded in inner classes.
     */
    private static class Context {

        /** Activity path whose state is being purged or considered to be purged. */
        @NotNull private final ActivityPath currentActivityPath;

        /**
         * Path pointing to the current activity state. It cannot be used for direct deletion.
         * But it can be used for modification of state components.
         */
        @NotNull private final ItemPath currentStateItemPath;

        /** Object holding the current state (i.e. TaskActivityStateType or ActivityStateType). */
        @NotNull private final Object currentStateHolder;

        /** Current state being purged. */
        @NotNull private final ActivityStateType currentState;

        private Context(@NotNull ActivityPath currentActivityPath, @NotNull ItemPath currentStateItemPath,
                @NotNull Object currentStateHolder, @NotNull ActivityStateType currentState) {
            this.currentActivityPath = currentActivityPath;
            this.currentStateItemPath = currentStateItemPath;
            this.currentStateHolder = currentStateHolder;
            this.currentState = currentState;
        }

        static @NotNull Context root(ActivityPath path, @NotNull TaskActivityStateType taskActivityState) {
            return new Context(
                    path,
                    ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY),
                    taskActivityState,
                    taskActivityState.getActivity());
        }

        @NotNull Context forChild(ActivityStateType child) {
            return new Context(
                    currentActivityPath.append(child.getIdentifier()),
                    currentStateItemPath.append(TaskActivityStateType.F_ACTIVITY, child.getId()),
                    currentState,
                    child);
        }

        boolean isLocalRoot() {
            return currentStateHolder instanceof TaskActivityStateType;
        }

        @Override
        public String toString() {
            return "Context{" +
                    "currentActivityPath=" + currentActivityPath +
                    ", currentStateItemPath=" + currentStateItemPath +
                    ", currentStateHolder:" + currentStateHolder.getClass().getSimpleName() +
                    ", currentState=" + currentState +
                    '}';
        }
    }
}
