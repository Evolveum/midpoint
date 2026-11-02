/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliation;
import com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliationOptions;
import com.evolveum.midpoint.repo.common.activity.run.state.OtherActivityState;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.ActivityStateInContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Provides common functionality related to activities (something like {@link TaskManager} does for tasks).
 */
@Experimental
@Component
public class TaskActivityManager {

    private static final String OP_RECONCILE_WORKERS = TaskActivityManager.class.getName() + ".reconcileWorkers";
    private static final String OP_RECONCILE_WORKERS_FOR_ACTIVITY = TaskActivityManager.class.getName() + ".reconcileWorkersForActivity";
    private static final String OP_DELETE_ACTIVITY_STATE_AND_WORKERS = TaskActivityManager.class.getName() + ".deleteActivityStateAndWorkers";

    private static final Trace LOGGER = TraceManager.getTrace(TaskActivityManager.class);

    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired @Qualifier("repositoryService") private RepositoryService plainRepositoryService;
    @Autowired private TaskManager taskManager;
    @Autowired private CommonTaskBeans beans;

    // TODO reconsider the concept of resolver (as it is useless now - we have to fetch the subtasks manually!)
    /** Assumes that the whole task tree is loaded. */
    public ActivityProgressInformation getProgressInformationFromTaskTree(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityProgressInformation.fromRootTask(
                getTaskWithSubtasks(rootTaskOid, result),
                createTaskResolver(result),
                InformationSource.FULL_STATE_ONLY);
    }

    public ActivityProgressInformation getProgressInformation(@NotNull String rootTaskOid, @NotNull InformationSource source,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityProgressInformation.fromRootTask(
                source == InformationSource.FULL_STATE_ONLY ?
                        getTaskWithSubtasks(rootTaskOid, result) :
                        getTaskWithoutSubtasks(rootTaskOid, result),
                source);
    }

    public TreeNode<ActivityPerformanceInformation> getPerformanceInformation(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityTreeUtil.transformStates(
                getTaskWithSubtasks(rootTaskOid, result),
                createTaskResolver(result),
                (path, state, workerStates, task) -> {
                    if (workerStates != null) {
                        return ActivityPerformanceInformation.forCoordinator(path, workerStates);
                    } else {
                        ActivityItemProcessingStatisticsType itemStats = getItemStats(state);
                        if (itemStats != null) {
                            return ActivityPerformanceInformation.forRegularActivity(path, itemStats, state.getProgress());
                        } else {
                            return ActivityPerformanceInformation.notApplicable(path);
                        }
                    }
                });
    }

    private ActivityItemProcessingStatisticsType getItemStats(ActivityStateType state) {
        return state != null && state.getStatistics() != null ?
                state.getStatistics().getItemProcessing() : null;
    }

    private TaskResolver createTaskResolver(OperationResult result) {
        return oid -> getTaskWithSubtasks(oid, result);
    }

    @NotNull
    private TaskType getTaskWithSubtasks(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> withChildren = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();

        return taskManager.getTask(oid, withChildren, result)
                .getUpdatedTaskObject()
                .asObjectable();
    }

    @NotNull
    private TaskType getTaskWithoutSubtasks(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return taskManager.getTask(oid, null, result)
                .getUpdatedTaskObject()
                .asObjectable();
    }

    public @NotNull Activity<?, ?> getActivity(Task rootTask, ActivityPath activityPath)
            throws SchemaException, ConfigurationException {
        return ActivityTree.create(rootTask)
                .getActivity(activityPath);
    }

    /**
     * Note that we reconcile only workers for distributed activities that already have their state.
     */
    public @NotNull Map<ActivityPath, WorkersReconciliationResultType> reconcileWorkers(@NotNull String rootTaskOid,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_RECONCILE_WORKERS)
                .addParam("rootTaskOid", rootTaskOid)
                .build();
        try {
            Task rootTask = taskManager.getTaskTree(rootTaskOid, result);
            TaskType rootTaskBean = rootTask.getRawTaskObjectClonedIfNecessary().asObjectable();
            Map<ActivityPath, WorkersReconciliationResultType> resultMap = new HashMap<>();
            ActivityTreeUtil.processStates(rootTaskBean, TaskResolver.empty(), (path, state, workerStates, task) -> {
                if (shouldReconcileActivity(workerStates, state)) {
                    resultMap.put(
                            path,
                            reconcileWorkersForActivity(rootTask, task, path, result));
                }
            });
            return resultMap;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean shouldReconcileActivity(List<ActivityStateType> workerStates, ActivityStateType state) {
        return workerStates != null &&
                state != null && // actually, workerStates != null implies state != null
                state.getRealizationState() == ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED;
    }

    /**
     * Note: common exceptions are not propagated - these are reflected only in the operation result
     */
    private @NotNull WorkersReconciliationResultType reconcileWorkersForActivity(@NotNull Task rootTask, @NotNull TaskType coordinatorTaskBean,
            @NotNull ActivityPath path, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_RECONCILE_WORKERS_FOR_ACTIVITY)
                .addArbitraryObjectAsParam("rootTask", rootTask)
                .addParam("coordinatorTask", coordinatorTaskBean)
                .addArbitraryObjectAsParam("path", path)
                .build();
        try {
            Task coordinatorTask = taskManager.createTaskInstance(coordinatorTaskBean.asPrismObject(), result);
            WorkersReconciliationOptions options = new WorkersReconciliationOptions();
            options.setDontCloseWorkersWhenWorkDone(true); // TODO
            return new WorkersReconciliation(rootTask, coordinatorTask, path, options, beans)
                    .execute(result);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reconcile workers for activity path '{}' in {}/{}", e, path,
                    coordinatorTaskBean, rootTask);
            result.recordFatalError(e);
            return new WorkersReconciliationResultType()
                    .status(OperationResultStatusType.FATAL_ERROR);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * TODO somehow unify with TaskStatePurger
     */
    public void deleteActivityStateAndWorkers(@NotNull String rootTaskOid, boolean deleteWorkers, long subtasksWaitTime,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_DELETE_ACTIVITY_STATE_AND_WORKERS)
                .addArbitraryObjectAsParam("rootTaskOid", rootTaskOid)
                .addParam("deleteWorkers", deleteWorkers)
                .addParam("subtasksWaitTime", subtasksWaitTime)
                .build();
        try {
            boolean suspended = taskManager.suspendTaskTree(rootTaskOid, subtasksWaitTime, result);
            if (!suspended) {
                // TODO less harsh handling
                throw new IllegalStateException("Not all tasks could be suspended. Please retry to operation.");
            }
            Task rootTask = taskManager.getTaskTree(rootTaskOid, result);
            TaskType rootTaskBean = rootTask.getRawTaskObjectClonedIfNecessary().asObjectable();
            TreeNode<ActivityStateInContext> stateTree = ActivityTreeUtil.toStateTree(rootTaskBean, TaskResolver.empty());

            deleteWorkersOrTheirState(stateTree, deleteWorkers, result);
            purgeOrDeleteActivityState(stateTree, null, result);
            makeRootReadyOnResumeIfNothingToWaitFor(rootTaskOid, result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteWorkersOrTheirState(TreeNode<ActivityStateInContext> root, boolean deleteWorkers, OperationResult result) {
        root.acceptDepthFirst(node -> {
            if (node.getUserObject().isCoordinator()) {
                deleteActivityWorkersOrTheirState(node.getUserObject(), deleteWorkers, result);
            }
        });
    }

    private void deleteActivityWorkersOrTheirState(@NotNull ActivityStateInContext activityStateInContext,
            boolean deleteWorkers, OperationResult result) {
        TaskType coordinatorTaskBean = activityStateInContext.getTask();
        ActivityPath activityPath = activityStateInContext.getActivityPath();
        Set<String> workerOids =
                ActivityTreeUtil.getSubtasksForPath(coordinatorTaskBean, activityPath, TaskResolver.empty()).stream()
                        .map(ObjectType::getOid)
                        .collect(Collectors.toSet());
        if (deleteWorkers) {
            taskManager.suspendAndDeleteTasks(workerOids, TaskManager.DO_NOT_WAIT, true, result);
            LOGGER.info("Deleted workers: {}", workerOids);
        } else {
            for (String workerOid : workerOids) {
                deleteCompleteState(workerOid, result);
            }
        }
    }

    private void deleteCompleteState(String taskOid, OperationResult result) {
        try {
            List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
                    .item(TaskType.F_ACTIVITY_STATE).replace()
                    .asItemDeltas();
            plainRepositoryService.modifyObject(TaskType.class, taskOid, modifications, result);
            LOGGER.info("Deleted complete task activity state in {}", taskOid);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete activity state in {}", e, taskOid);
        }
    }

    /**
     * @param parentTaskBean Task holding the state of the parent activity (null for the root). A node whose state lives
     * in a different task is the local root of a delegate subtask.
     */
    private void purgeOrDeleteActivityState(
            TreeNode<ActivityStateInContext> node, @Nullable TaskType parentTaskBean, OperationResult result) {
        ActivityStateInContext stateInContext = node.getUserObject();
        if (stateInContext.getActivityState() == null) {
            return;
        }

        TaskType taskBean = stateInContext.getTask();
        if (parentTaskBean != null && !parentTaskBean.getOid().equals(taskBean.getOid())) {
            // A delegate subtask cannot run without its state, and the parent would wait for it forever. So the subtask
            // (with its own subtasks) and the delegation state in the parent go away; the parent will delegate anew.
            deleteDelegate(taskBean, stateInContext.getActivityPath(), result);
            deleteState(parentTaskBean, stateInContext.getActivityPath(), result);
        } else if (ActivityTreeUtil.hasDelegatedActivity(node)) {
            purgeState(taskBean, stateInContext.getActivityPath(), stateInContext.getActivityState(), result);
            for (TreeNode<ActivityStateInContext> child : node.getChildren()) {
                purgeOrDeleteActivityState(child, taskBean, result);
            }
        } else {
            deleteState(taskBean, stateInContext.getActivityPath(), result);
        }
    }

    private void deleteDelegate(TaskType delegateTaskBean, ActivityPath activityPath, OperationResult result) {
        taskManager.suspendAndDeleteTasks(List.of(delegateTaskBean.getOid()), TaskManager.DO_NOT_WAIT, true, result);
        LOGGER.info("Deleted delegate subtask {} for '{}'", delegateTaskBean, activityPath);
    }

    /**
     * The root was suspended while waiting for its subtasks. If none are left (they were deleted above), a resume
     * would put it back into the waiting state with nothing to wake it up, so it must resume as ready instead.
     */
    private void makeRootReadyOnResumeIfNothingToWaitFor(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task root = taskManager.getTaskPlain(rootTaskOid, result);
        TaskType rootBean = root.getRawTaskObjectClonedIfNecessary().asObjectable();
        if (rootBean.getSchedulingStateBeforeSuspend() != TaskSchedulingStateType.WAITING) {
            return;
        }
        if (!root.listSubtasks(true, result).isEmpty()) {
            return;
        }
        try {
            plainRepositoryService.modifyObject(TaskType.class, rootTaskOid,
                    prismContext.deltaFor(TaskType.class)
                            .item(TaskType.F_STATE_BEFORE_SUSPEND).replace(TaskExecutionStateType.RUNNABLE)
                            .item(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND).replace(TaskSchedulingStateType.READY)
                            .asItemDeltas(),
                    result);
            LOGGER.info("Task {} has no subtasks left to wait for, it will resume as ready", root);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when updating the state before suspend");
        }
    }

    private void deleteState(TaskType task, ActivityPath activityPath, OperationResult result) {
        try {
            TaskActivityStateType taskActivityState = task.getActivityState();
            ItemPath stateItemPath = ActivityStateUtil.getStateItemPath(taskActivityState, activityPath);
            List<ItemDelta<?, ?>> itemDeltas;
            if (activityPath.equals(ActivityStateUtil.getLocalRootPath(taskActivityState))) {
                // This is the [local] root activity. Delete everything!
                itemDeltas = PrismContext.get().deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY_STATE).replace()
                        .asItemDeltas();
            } else {
                Long id = ItemPath.toId(stateItemPath.last());
                assert id != null;
                itemDeltas = PrismContext.get().deltaFor(TaskType.class)
                        .item(stateItemPath.allExceptLast()).delete(new ActivityStateType().id(id))
                        .asItemDeltas();
            }
            plainRepositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
            LOGGER.info("Deleted activity state for '{}' in {}", activityPath, task);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete state for activity path '{}' in {}", e, activityPath, task);
        }
    }

    private void purgeState(TaskType task, ActivityPath activityPath, ActivityStateType activityState, OperationResult result) {
        try {
            TaskActivityStateType taskActivityState = task.getActivityState();
            ItemPath stateItemPath = ActivityStateUtil.getStateItemPath(taskActivityState, activityPath);
            List<ItemDelta<?, ?>> itemDeltas = PrismContext.get().deltaFor(TaskType.class)
                    .item(stateItemPath.append(ActivityStateType.F_RESULT_STATUS)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_PROGRESS)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_STATISTICS)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_BUCKETING)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_COUNTERS)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_POLICIES)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_HALTING_INFORMATION)).replace()
                    .item(stateItemPath.append(ActivityStateType.F_ABORTING_INFORMATION)).replace()
                    .asItemDeltas();
            if (!(activityState.getWorkState() instanceof DelegationWorkStateType)) {
                itemDeltas.addAll(
                        PrismContext.get().deltaFor(TaskType.class)
                                .item(stateItemPath.append(ActivityStateType.F_REALIZATION_STATE)).replace()
                                .item(stateItemPath.append(ActivityStateType.F_WORK_STATE)).replace()
                                .asItemDeltas());
            }
            plainRepositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
            LOGGER.info("Purged activity state for '{}' in {}", activityPath, task);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete state for activity path '{}' in {}", e, activityPath, task);
        }
    }

    /** Computes affected objects for a given activity definition, either simple or composite. */
    public @Nullable TaskAffectedObjectsType computeAffectedObjects(
            @Nullable ActivityDefinitionType activityDefinitionBean, AbstractActivityWorkStateType state)
            throws SchemaException, ConfigurationException {
        if (activityDefinitionBean == null) {
            return null;
        }
        // The origin has no use here; but we need to provide any.
        ConfigurationItemOrigin origin = ConfigurationItemOrigin.undeterminedSafe();
        var definition = ActivityDefinition.createActivityDefinition(activityDefinitionBean, origin);
        if (definition == null) {
            return null; // Normally should not occur, but let's not fail here.
        }
        // Note: tailoring is currently ignored here; it is applied only when the activity is run, because
        // run-time child activities are known only then.
        return definition.getAffectedObjectsInformation(state).toBean();
    }

    /**
     * Returns all activity state beans in the task tree. Note that for delegated activities, there are usually two states
     * returned: one from the delegator task and one from the delegate task. In a similar way, for distributed activities,
     * there is usually one state from the coordinator task and multiple states from the worker tasks.
     */
    public @NotNull Multimap<ActivityPath, OtherActivityState> getAllActivityStates(String rootTaskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Multimap<ActivityPath, OtherActivityState> states = ArrayListMultimap.create();
        ActivityTreeUtil.processStates(
                getTaskWithSubtasks(rootTaskOid, result),
                createTaskResolver(result),
                new ActivityTreeUtil.ActivityStateProcessor() {

                    @Override
                    public void process(
                            @NotNull ActivityPath path,
                            @Nullable ActivityStateType state,
                            @Nullable List<ActivityStateType> workerStates,
                            @NotNull TaskType task) {
                        if (state != null) {
                            try {
                                states.put(path, OtherActivityState.of(task, path, result));
                            } catch (SchemaException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    @Override
                    public void processWorkerState(
                            @NotNull ActivityPath path, @NotNull ActivityStateType state, @NotNull TaskType task) {
                        try {
                            states.put(path, OtherActivityState.of(task, path, result));
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        return states;
    }
}
