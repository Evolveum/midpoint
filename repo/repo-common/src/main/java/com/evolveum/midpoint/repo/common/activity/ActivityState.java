/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class ActivityState<WS extends AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    private static final @NotNull ItemPath ROOT_WORK_STATE_PATH = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);

    @NotNull private final AbstractActivityExecution<?, ?, WS> activityExecution;

    @NotNull private final ComplexTypeDefinition workStateDefinition;

    /** Path to the work state container value related to this execution. */
    private ItemPath stateItemPath;

    public ActivityState(@NotNull AbstractActivityExecution<?, ?, WS> activityExecution) {
        this.activityExecution = activityExecution;
        this.workStateDefinition = determineWorkStateDefinition(activityExecution);
    }

    @NotNull
    private ComplexTypeDefinition determineWorkStateDefinition(AbstractActivityExecution<?, ?, WS> activityExecution) {
        return requireNonNull(
                getBeans().prismContext.getSchemaRegistry()
                        .findComplexTypeDefinitionByType(activityExecution.getWorkStateTypeName()),
                () -> new SystemException("Couldn't find definition for " + activityExecution.getWorkStateTypeName()));
    }

    public void initialize(OperationResult result) throws ActivityExecutionException {
        try {
            stateItemPath = findOrCreateActivityState(result);
            if (getActivityHandler().shouldCreateWorkStateOnInitialization()) {
                createWorkStateIfNeeded(result);
            }
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            // We consider all such exceptions permanent. There's basically nothing that could resolve "by itself".
            throw new ActivityExecutionException("Couldn't initialize activity state for " + getActivity() + ": " + e.getMessage(),
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /**
     * Creates a work state compartment for this activity and returns its path (related to the execution task).
     */
    @NotNull
    private ItemPath findOrCreateActivityState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        LOGGER.trace("findOrCreateActivityWorkState starting in activity with path '{}' in {}", activityExecution.getPath(),
                getRunningTask());
        AbstractActivityExecution<?, ?, ?> localParentExecution = activityExecution.getLocalParentExecution();
        if (localParentExecution == null) {
            LOGGER.trace("No local parent execution, checking or creating root work state");
            findOrCreateRootActivityState(result);
            return ROOT_WORK_STATE_PATH;
        } else {
            ItemPath parentWorkStatePath = localParentExecution.getWorkState().findOrCreateActivityState(result);
            LOGGER.trace("Found parent work state prism item path: {}", parentWorkStatePath);
            return findOrCreateChildActivityState(parentWorkStatePath, getActivity().getIdentifier(), result);
        }
    }

    @NotNull
    private Activity<?, ?> getActivity() {
        return activityExecution.getActivity();
    }

    private void findOrCreateRootActivityState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = getRunningTask();
        ActivityStateType value = task.getActivityStateOrClone(ROOT_WORK_STATE_PATH);
        if (value == null) {
            addActivityState(TaskType.F_ACTIVITY_STATE, result, task, getActivity().getIdentifier());
        }
    }

    @NotNull
    private ItemPath findOrCreateChildActivityState(ItemPath parentWorkStatePath, String identifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = getRunningTask();
        ItemPath childPath = findChildState(task, parentWorkStatePath, identifier);
        if (childPath != null) {
            LOGGER.trace("Child work state exists with the path of '{}'", childPath);
            return childPath;
        }

        addActivityState(parentWorkStatePath, result, task, identifier);

        ItemPath childPathAfter = findChildState(task, parentWorkStatePath, identifier);
        LOGGER.trace("Child work state created with the path of '{}'", childPathAfter);

        stateCheck(childPathAfter != null, "Child work state not found even after its creation in %s", task);
        return childPathAfter;
    }

    @Nullable
    private ItemPath findChildState(RunningTask task, ItemPath parentWorkStatePath, String identifier) {
        ActivityStateType parentValue = task.getActivityStateOrClone(parentWorkStatePath);
        stateCheck(parentValue != null, "Parent activity work state does not exist in %s; path = %s",
                task, parentWorkStatePath);
        for (ActivityStateType childState : parentValue.getActivity()) {
            if (Objects.equals(childState.getIdentifier(), identifier)) {
                Long childPcvId = childState.getId();
                stateCheck(childPcvId != null, "Child activity work state without an ID: %s in %s",
                        childState, task);
                return parentWorkStatePath.append(ActivityStateType.F_ACTIVITY, childPcvId);
            }
        }
        return null;
    }

    private void addActivityState(ItemPath stateItemPath, OperationResult result, RunningTask task, String identifier)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(stateItemPath.append(ActivityStateType.F_ACTIVITY))
                .add(new ActivityStateType(getPrismContext())
                        .identifier(identifier)
                        .executionState(ActivityExecutionStateType.EXECUTING))
                .asItemDelta();
        task.modify(itemDelta);
        task.flushPendingModifications(result);
        task.refresh(result);
        LOGGER.debug("Activity state created for activity identifier={} in {} in {}", identifier, stateItemPath, task);
    }

    private void createWorkStateIfNeeded(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemPath path = stateItemPath.append(ActivityStateType.F_WORK_STATE);
        if (!getRunningTask().doesItemExist(path)) {
            createWorkState(path, result);
        }
    }

    private void createWorkState(ItemPath path, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Task task = getRunningTask();
        PrismContainerDefinition<?> def = getPrismContext().definitionFactory().createContainerDefinition(
                ActivityStateType.F_WORK_STATE, workStateDefinition);
        PrismContainer<?> workStateContainer = def.instantiate();
        PrismContainerValue<?> newWorkStateValue = workStateContainer.createNewValue().clone();
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(path)
                .add(newWorkStateValue)
                .asItemDelta();
        task.modify(itemDelta);
        task.flushPendingModifications(result);
        task.refresh(result);
        LOGGER.debug("Work state created in {} in {}", stateItemPath, task);
    }

    private CommonTaskBeans getBeans() {
        return activityExecution.getBeans();
    }

    public ItemPath getItemPath() {
        return stateItemPath;
    }

    public <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getRunningTask()
                .getPropertyRealValue(getWorkStateItemPath().append(path), expectedType);
    }

    private RunningTask getRunningTask() {
        return activityExecution.getRunningTask();
    }

    public void setPropertyRealValue(ItemPath path, Object value) throws SchemaException {
        RunningTask task = getRunningTask();
        LOGGER.trace("setPropertyRealValue: path={}, value={} in {}", path, value, task);

        task.modify(
                getPrismContext().deltaFor(TaskType.class)
                        .item(getWorkStateItemPath().append(path), getWorkStateItemDefinition(path))
                        .replace(value)
                        .asItemDelta());
    }

    @NotNull
    private ItemDefinition<?> getWorkStateItemDefinition(ItemPath path) throws SchemaException {
        return MiscUtil.<ItemDefinition<?>, SchemaException>requireNonNull(
                workStateDefinition.findItemDefinition(path),
                () -> new SchemaException("Definition for " + path + " couldn't be found in " + workStateDefinition));
    }

    private PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    public @NotNull ComplexTypeDefinition getWorkStateDefinition() {
        return workStateDefinition;
    }

    public void flushPendingModifications(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        getRunningTask().flushPendingModifications(result);
    }

    @NotNull ActivityHandler<?, ?> getActivityHandler() {
        return activityExecution.getActivityHandler();
    }

    public boolean isComplete() {
        return getExecutionState() == ActivityExecutionStateType.COMPLETE;
    }

    private ActivityExecutionStateType getExecutionState() {
        return getRunningTask().getPropertyRealValue(getExecutionStateItemPath(), ActivityExecutionStateType.class);
    }

    @NotNull ItemPath getWorkStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_WORK_STATE);
    }

    @NotNull
    private ItemPath getExecutionStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_EXECUTION_STATE);
    }

    public void markComplete(OperationResult result) throws ActivityExecutionException {
        setExecutionState(ActivityExecutionStateType.COMPLETE, result);
    }

    public void setExecutionState(ActivityExecutionStateType value, OperationResult result) throws ActivityExecutionException {
        try {
            ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                    .item(getExecutionStateItemPath()).replace(value)
                    .asItemDelta();
            getRunningTask().modify(itemDelta);
            flushPendingModifications(result);
            LOGGER.info("Execution state was set to {} for {}", value, activityExecution);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't set activity execution state to " + value,
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }
}
