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
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.repo.common.activity.ActivityState.Wrapper.w;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.*;

public class ActivityState<WS extends AbstractActivityWorkStateType> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    private static final @NotNull ItemPath ROOT_ACTIVITY_STATE_PATH =
            ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);

    @NotNull private final AbstractActivityExecution<?, ?, WS> activityExecution;

    @NotNull private final ComplexTypeDefinition workStateDefinition;

    /** Path to the work state container value related to this execution. */
    private ItemPath stateItemPath;

    private boolean initialized;

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

    //region Initialization
    public void initialize(OperationResult result) throws ActivityExecutionException {
        if (initialized) {
            return;
        }
        try {
            stateItemPath = findOrCreateActivityState(result);
            if (activityExecution.shouldCreateWorkStateOnInitialization()) {
                createWorkStateIfNeeded(result);
            }
            initialized = true;
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
        LOGGER.trace("findOrCreateActivityWorkState starting in activity with path '{}' in {}", activityExecution.getActivityPath(),
                getRunningTask());
        AbstractActivityExecution<?, ?, ?> localParentExecution = activityExecution.getLocalParentExecution();
        if (localParentExecution == null) {
            LOGGER.trace("No local parent execution, checking or creating root work state");
            findOrCreateRootActivityState(result);
            return ROOT_ACTIVITY_STATE_PATH;
        } else {
            ItemPath parentWorkStatePath = localParentExecution.getActivityState().findOrCreateActivityState(result);
            LOGGER.trace("Found parent work state prism item path: {}", parentWorkStatePath);
            return findOrCreateChildActivityState(parentWorkStatePath, getActivity().getIdentifier(), result);
        }
    }

    private void findOrCreateRootActivityState(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        RunningTask task = getRunningTask();
        ActivityStateType value = task.getActivityStateOrClone(ROOT_ACTIVITY_STATE_PATH);
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
                        .resultStatus(OperationResultStatusType.IN_PROGRESS))
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
    //endregion

    //region Realization state
    public ActivityRealizationStateType getRealizationState() {
        return getRunningTask().getPropertyRealValue(getRealizationStateItemPath(), ActivityRealizationStateType.class);
    }

    private @NotNull ItemPath getRealizationStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_REALIZATION_STATE);
    }

    public void markInProgressLocal(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_LOCAL), w(IN_PROGRESS), result);
    }

    public void markInProgressDelegated(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_DELEGATED), w(IN_PROGRESS), result);
    }

    public void markInProgressDistributed(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED), w(IN_PROGRESS), result);
    }

    public boolean isComplete() {
        return getRealizationState() == ActivityRealizationStateType.COMPLETE;
    }

    public void markComplete(OperationResultStatus resultStatus, OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.COMPLETE), w(resultStatus), result);
    }

    private void setRealizationStateWithoutCommit(ActivityRealizationStateType value) throws SchemaException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(getRealizationStateItemPath()).replace(value)
                .asItemDelta();
        getRunningTask().modify(itemDelta);
        LOGGER.info("Setting realization state to {} for {}", value, activityExecution);
    }

    //endregion

    //region Result status
    public OperationResultStatusType getResultStatusRaw() {
        return getRunningTask().getPropertyRealValue(getResultStatusItemPath(), OperationResultStatusType.class);
    }

    public OperationResultStatus getResultStatus() {
        return OperationResultStatus.parseStatusType(getResultStatusRaw());
    }

    public void setResultStatus(@NotNull OperationResultStatus status, OperationResult result)
            throws ActivityExecutionException {
        setCombined(null, w(status), result);
    }

    private void setResultStateWithoutCommit(OperationResultStatus resultStatus) throws SchemaException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(getResultStatusItemPath()).replace(OperationResultStatus.createStatusType(resultStatus))
                .asItemDelta();
        getRunningTask().modify(itemDelta);
        LOGGER.info("Setting result status to {} for {}", resultStatus, activityExecution);
    }

    private @NotNull ItemPath getResultStatusItemPath() {
        return stateItemPath.append(ActivityStateType.F_RESULT_STATUS);
    }
    //endregion

    //region Generic access
    public <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getRunningTask()
                .getPropertyRealValue(stateItemPath.append(path), expectedType);
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    public void setItemRealValues(ItemPath path, Object... values) throws SchemaException {
        setItemRealValues(path, isSingleNull(values) ? List.of() : Arrays.asList(values));
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    public void setItemRealValues(ItemPath path, Collection<?> values) throws SchemaException {
        RunningTask task = getRunningTask();
        LOGGER.trace("setItemRealValues: path={}, values={} in {}", path, values, task);

        task.modify(
                getPrismContext().deltaFor(TaskType.class)
                        .item(stateItemPath.append(path))
                        .replaceRealValues(values)
                        .asItemDelta());
    }
    //endregion

    //region Work state
    public @NotNull ComplexTypeDefinition getWorkStateDefinition() {
        return workStateDefinition;
    }

    public <T> T getWorkStatePropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getPropertyRealValue(ActivityStateType.F_WORK_STATE.append(path), expectedType);
    }

    public ObjectReferenceType getWorkStateReferenceRealValue(ItemPath path) {
        return getRunningTask()
                .getReferenceRealValue(getWorkStateItemPath().append(path));
    }

    public Collection<ObjectReferenceType> getWorkStateReferenceRealValues(ItemPath path) {
        return getRunningTask()
                .getReferenceRealValues(getWorkStateItemPath().append(path));
    }

    public void setWorkStateItemRealValues(ItemPath path, Object... values) throws SchemaException {
        setWorkStateItemRealValues(path, isSingleNull(values) ? List.of() : Arrays.asList(values));
    }

    public void setWorkStateItemRealValues(ItemPath path, Collection<?> values) throws SchemaException {
        RunningTask task = getRunningTask();
        LOGGER.trace("setWorkStateItemRealValues: path={}, values={} in {}", path, values, task);

        task.modify(
                getPrismContext().deltaFor(TaskType.class)
                        .item(getWorkStateItemPath().append(path), getWorkStateItemDefinition(path))
                        .replaceRealValues(values)
                        .asItemDelta());
    }

    @NotNull
    private ItemDefinition<?> getWorkStateItemDefinition(ItemPath path) throws SchemaException {
        return MiscUtil.<ItemDefinition<?>, SchemaException>requireNonNull(
                workStateDefinition.findItemDefinition(path),
                () -> new SchemaException("Definition for " + path + " couldn't be found in " + workStateDefinition));
    }

    @NotNull ItemPath getWorkStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_WORK_STATE);
    }
    //endregion

    //region Combined operations
    private void setCombined(Wrapper<ActivityRealizationStateType> realizationState, Wrapper<OperationResultStatus> resultStatus,
            OperationResult result) throws ActivityExecutionException {
        try {
            if (realizationState != null) {
                setRealizationStateWithoutCommit(realizationState.value);
            }
            if (resultStatus != null) {
                setResultStateWithoutCommit(resultStatus.value);
            }
            flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't update activity state", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    static class Wrapper<T> {
        final T value;

        private Wrapper(T value) {
            this.value = value;
        }

        static <T> Wrapper<T> w(T value) {
            return new Wrapper<>(value);
        }
    }
    //endregion

    //region Misc
    private @NotNull Activity<?, ?> getActivity() {
        return activityExecution.getActivity();
    }

    private CommonTaskBeans getBeans() {
        return activityExecution.getBeans();
    }

    public ItemPath getItemPath() {
        return stateItemPath;
    }

    private RunningTask getRunningTask() {
        return activityExecution.getRunningTask();
    }

    private PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    public void flushPendingModifications(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        getRunningTask().flushPendingModifications(result);
    }

    @NotNull ActivityHandler<?, ?> getActivityHandler() {
        return activityExecution.getActivityHandler();
    }
    //endregion

    //region debugDump + toString
    @Override
    public String toString() {
        return "ActivityState{" +
                "stateItemPath=" + stateItemPath +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getEnhancedClassName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Item path", String.valueOf(stateItemPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Realization state", String.valueOf(getRealizationState()), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Result status", String.valueOf(getResultStatusRaw()), indent + 1);
        // Consider adding work state here, maybe bucketing etc
        return sb.toString();
    }

    @NotNull
    private String getEnhancedClassName() {
        return getClass().getSimpleName() + "<" + workStateDefinition.getTypeName().getLocalPart() + ">";
    }
    //endregion
}
