/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> implements ActivityExecution {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    private static final @NotNull ItemPath ROOT_WORK_STATE_PATH = ItemPath.create(TaskType.F_WORK_STATE, TaskWorkStateType.F_ACTIVITY);

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final TaskExecution taskExecution;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    /** Path to the work state container value related to this execution. */
    private ItemPath workStatePath;

    protected AbstractActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        this.taskExecution = context.getTaskExecution();
        this.activity = context.getActivity();
    }

    @NotNull
    @Override
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        workStatePath = findOrCreateActivityWorkState(result);

        logStart();

        // TODO check if not already executed
        ActivityExecutionResult executionResult = executeInternal(result);

        logEnd(executionResult);

        return executionResult;
    }

    private void logStart() {
        LOGGER.trace("Starting execution of activity with identifier '{}' and path '{}' (local: '{}') with work state "
                        + "prism item path: {}", activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                workStatePath);
    }

    private void logEnd(ActivityExecutionResult executionResult) {
        LOGGER.trace("Finished execution of activity with identifier '{}' and path '{}' (local: {}) with result: {}",
                activity.getIdentifier(), activity.getPath(), activity.getLocalPath(), executionResult);
    }

    /**
     * Carries out the actual execution of this activity.
     */
    protected abstract ActivityExecutionResult executeInternal(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException;

    /**
     * Creates a work state compartment for this activity and returns its path (related to the execution task).
     */
    @NotNull
    private ItemPath findOrCreateActivityWorkState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        LOGGER.trace("findOrCreateActivityWorkState starting in activity with path '{}' in {}", getPath(),
                taskExecution.getRunningTask());
        AbstractActivityExecution<?, ?> localParentExecution = getLocalParentExecution();
        if (localParentExecution == null) {
            LOGGER.trace("No local parent execution, checking or creating root work state");
            findOrCreateRootActivityWorkState(result);
            return ROOT_WORK_STATE_PATH;
        } else {
            ItemPath parentWorkStatePath = localParentExecution.findOrCreateActivityWorkState(result);
            LOGGER.trace("Found parent work state prism item path: {}", parentWorkStatePath);
            return findOrCreateChildActivityWorkState(parentWorkStatePath, activity.getIdentifier(), result);
        }
    }

    private void findOrCreateRootActivityWorkState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = taskExecution.getRunningTask();
        ActivityWorkStateType value = task.getActivityWorkStateOrClone(ROOT_WORK_STATE_PATH);
        if (value == null) {
            addActivityWorkState(TaskType.F_WORK_STATE, result, task, activity.getIdentifier());
        }
    }

    @NotNull
    private ItemPath findOrCreateChildActivityWorkState(ItemPath parentWorkStatePath, String identifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = taskExecution.getRunningTask();
        ItemPath childPath = findChildWorkState(task, parentWorkStatePath, identifier);
        if (childPath != null) {
            LOGGER.trace("Child work state exists with the path of '{}'", childPath);
            return childPath;
        }

        addActivityWorkState(parentWorkStatePath, result, task, identifier);

        ItemPath childPathAfter = findChildWorkState(task, parentWorkStatePath, identifier);
        LOGGER.trace("Child work state created with the path of '{}'", childPathAfter);

        stateCheck(childPathAfter != null, "Child work state not found even after its creation in %s", task);
        return childPathAfter;
    }

    @Nullable
    private ItemPath findChildWorkState(RunningTask task, ItemPath parentWorkStatePath, String identifier) {
        ActivityWorkStateType parentValue = task.getActivityWorkStateOrClone(parentWorkStatePath);
        stateCheck(parentValue != null, "Parent activity work state does not exist in %s; path = %s",
                task, parentWorkStatePath);
        for (ActivityWorkStateType childState : parentValue.getActivity()) {
            if (Objects.equals(childState.getIdentifier(), identifier)) {
                Long childPcvId = childState.getId();
                stateCheck(childPcvId != null, "Child activity work state without an ID: %s in %s",
                        childState, task);
                return parentWorkStatePath.append(ActivityWorkStateType.F_ACTIVITY, childPcvId);
            }
        }
        return null;
    }

    private void addActivityWorkState(ItemPath workStatePath, OperationResult result, RunningTask task, String identifier)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemDelta<?, ?> itemDelta = getBeans().prismContext.deltaFor(TaskType.class)
                .item(workStatePath.append(ActivityWorkStateType.F_ACTIVITY))
                .add(new ActivityWorkStateType(getBeans().prismContext)
                        .identifier(identifier)
                        .complete(false))
                .asItemDelta();
        task.modify(itemDelta);
        task.flushPendingModifications(result);
        task.refresh(result);
        LOGGER.debug("Activity work state created for id={} in {} in {}", identifier, workStatePath, task);
        LOGGER.debug("Task after:\n{}", task.debugDumpLazily());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "act=" + activity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (activity.isRoot()) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        return sb.toString();
    }

    public ActivityPath getLocalPath() {
        return activity.getLocalPath();
    }

    public ActivityPath getPath() {
        return activity.getPath();
    }

    public AbstractActivityExecution<?, ?> getLocalParentExecution() {
        if (activity.isLocalRoot()) {
            return null;
        }

        Activity<?, ?> parentActivity = activity.getParent();
        if (parentActivity != null) {
            return parentActivity.getExecution();
        } else {
            return null;
        }
    }

    public ItemPath getWorkStatePath() {
        return workStatePath;
    }

    public ActivityPath getActivityPath() {
        return activity.getPath();
    }
}
