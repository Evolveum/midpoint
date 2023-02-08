/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
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
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

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

    @NotNull private final ActivityBasedTaskRun taskRun;
    @NotNull private final CommonTaskBeans beans;

    /**
     * These paths contain something of interest. So states on the path to them should not be removed.
     */
    @NotNull private final Set<ActivityPath> pathsToKeep = new HashSet<>();

    public ActivityTreePurger(@NotNull ActivityBasedTaskRun taskRun, @NotNull CommonTaskBeans beans) {
        this.taskRun = taskRun;
        this.beans = beans;
    }

    /**
     * Purges state (including task-level stats) from current task and its subtasks. Deletes subtasks if possible.
     *
     * * Pre: task is an execution root
     * * Post: task is refreshed
     */
    public void purge(OperationResult result) throws ActivityRunException {
        try {
            boolean canDeleteRoot = purgeInTasksRecursively(ActivityPath.empty(), taskRun.getRunningTask(), result);
            if (canDeleteRoot) {
                taskRun.getRunningTask().restartCollectingStatisticsFromZero();
                taskRun.getRunningTask().updateAndStoreStatisticsIntoRepository(true, result);
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
    private boolean purgeInTasksRecursively(ActivityPath activityPath, Task currentRootTask, OperationResult result)
            throws CommonException {
        boolean noSubtasksLeft = purgeInSubtasks(currentRootTask, result);
        boolean canDeleteThisTask = purgeInTaskLocally(activityPath, currentRootTask, result);
        return noSubtasksLeft && canDeleteThisTask;
    }

    private boolean purgeInTaskLocally(ActivityPath activityPath, Task task, OperationResult result) throws CommonException {
        return new TaskStatePurger(activityPath, task)
                .doPurge(result);
    }

    /**
     * Purges activity states in the subtasks. Deletes those subtasks if possible.
     *
     * @return true if there are no subtasks left
     */
    private boolean purgeInSubtasks(Task parent, OperationResult result) throws CommonException {
        List<? extends Task> subtasks = parent.listSubtasks(true, result);
        for (Iterator<? extends Task> iterator = subtasks.iterator(); iterator.hasNext(); ) {
            Task subtask = iterator.next();
            TaskActivityStateType taskActivityState = subtask.getActivitiesStateOrClone();
            if (taskActivityState == null) {
                LOGGER.error("Non-activity related subtask {} of {}. Please resolve manually.", subtask, parent);
                continue;
            }
            boolean canDeleteSubtask = purgeInTasksRecursively(
                    ActivityPath.fromBean(taskActivityState.getLocalRoot()),
                    subtask,
                    result);
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

        @NotNull private final ActivityPath localRootPath;
        @NotNull private final Task task;
        final TaskActivityStateType taskActivityState;
        @NotNull private final List<ItemDelta<?, ?>> deltas = new ArrayList<>();

        /** True if nothing of relevance remains in the task, so it can be deleted (if needed). */
        private boolean canDelete;

        private TaskStatePurger(@NotNull ActivityPath activityPath, @NotNull Task task) {
            this.localRootPath = activityPath;
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
            canDelete = true;
            doPurge(Context.root(localRootPath, taskActivityState));
            if (!deltas.isEmpty()) {
                beans.repositoryService.modifyObject(TaskType.class, task.getOid(), deltas, result);
                if (task instanceof RunningTask) {
                    task.refresh(result);
                }
            }
            return canDelete;
        }

        private void doPurge(Context ctx) throws CommonException {
            LOGGER.trace("doPurge called for {}, processing children", ctx);
            for (ActivityStateType child : ctx.currentState.getActivity()) {
                doPurge(
                        ctx.forChild(child));
            }
            LOGGER.trace("doPurge continuing with {}, paths to keep: {}", ctx.currentActivityPath, pathsToKeep);
            if (isTransient(ctx.currentState) && hasNoPersistentChild(ctx.currentActivityPath)) {
                removeCurrentState(ctx);
            } else {
                purgeCurrentState(ctx);
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

        private void purgeCurrentState(Context ctx) throws SchemaException {
            if (!isTransient(ctx.currentState)) {
                pathsToKeep.add(ctx.currentActivityPath);
            }

            LOGGER.trace("Purging from multi: task = {}, activity path = '{}', item path = '{}'",
                    task, ctx.currentActivityPath, ctx.currentStateItemPath);

            swallow(
                    beans.prismContext.deltaFor(TaskType.class)
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_STATE)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_START_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_REALIZATION_END_TIMESTAMP)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_RESULT_STATUS)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_BUCKETING)).replace()
                            .item(ctx.currentStateItemPath.append(ActivityStateType.F_SIMULATION)).replace()
                            .asItemDeltas());

            if (ctx.currentState.getPersistence() != ActivityStatePersistenceType.PERPETUAL) {
                // E.g. "perpetual except for statistics"
                swallow(
                        beans.prismContext.deltaFor(TaskType.class)
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_PROGRESS)).replace()
                                .item(ctx.currentStateItemPath.append(ActivityStateType.F_STATISTICS)).replace()
                                .asItemDeltas());
            }

            // keeping: workState + activity
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

        /** Activity path whose state is being purged. */
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

        public static @NotNull Context root(ActivityPath path, @NotNull TaskActivityStateType taskActivityState) {
            return new Context(
                    path,
                    ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY),
                    taskActivityState,
                    taskActivityState.getActivity());
        }

        public @NotNull Context forChild(ActivityStateType child) {
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
