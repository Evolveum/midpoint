/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class ActivityState<WS extends AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    private static final @NotNull ItemPath ROOT_WORK_STATE_PATH = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);

    @NotNull private final AbstractActivityExecution<?, ?, WS> activityExecution;

    @NotNull private final ComplexTypeDefinition workStateDefinition;

    /** Path to the work state container value related to this execution. */
    private ItemPath itemPath;

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

    public void initialize(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        itemPath = findOrCreateActivityState(result);
    }

    /**
     * Creates a work state compartment for this activity and returns its path (related to the execution task).
     */
    @NotNull
    private ItemPath findOrCreateActivityState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        LOGGER.trace("findOrCreateActivityWorkState starting in activity with path '{}' in {}", activityExecution.getPath(),
                activityExecution.getRunningTask());
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
        RunningTask task = activityExecution.getRunningTask();
        ActivityStateType value = task.getActivityWorkStateOrClone(ROOT_WORK_STATE_PATH);
        if (value == null) {
            addActivityState(TaskType.F_ACTIVITY_STATE, result, task, getActivity().getIdentifier());
        }
    }

    @NotNull
    private ItemPath findOrCreateChildActivityState(ItemPath parentWorkStatePath, String identifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = activityExecution.getRunningTask();
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
        ActivityStateType parentValue = task.getActivityWorkStateOrClone(parentWorkStatePath);
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

    private CommonTaskBeans getBeans() {
        return activityExecution.getBeans();
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType) {
        return activityExecution.getRunningTask()
                .getPropertyRealValue(itemPath.append(path), expectedType);
    }

    public void setPropertyRealValue(ItemPath path, Object value) throws SchemaException {
        activityExecution.getRunningTask()
                .modify(
                        getPrismContext().deltaFor(TaskType.class)
                                .item(itemPath.append(path), getWorkStateItemDefinition(path))
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
        activityExecution.getRunningTask()
                .flushPendingModifications(result);
    }
}
