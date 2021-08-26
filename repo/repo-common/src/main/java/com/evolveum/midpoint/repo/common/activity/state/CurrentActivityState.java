/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import static com.evolveum.midpoint.repo.common.activity.state.CurrentActivityState.Wrapper.w;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity state for the current activity execution. Provides the full functionality, including creation of the work state
 * and maintenance of live progress and statistics.
 *
 * @param <WS> Work (business) state of the activity.
 */
public class CurrentActivityState<WS extends AbstractActivityWorkStateType>
        extends ActivityState {

    private static final Trace LOGGER = TraceManager.getTrace(CurrentActivityState.class);

    private static final @NotNull ItemPath ROOT_ACTIVITY_STATE_PATH = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);

    @NotNull private final AbstractActivityExecution<?, ?, WS> activityExecution;

    @NotNull private final ActivityStateDefinition<WS> activityStateDefinition;

    @NotNull private final ComplexTypeDefinition workStateComplexTypeDefinition;

    @NotNull private final ActivityProgress liveProgress;

    @NotNull private final ActivityStatistics liveStatistics;

    private boolean initialized;

    public CurrentActivityState(@NotNull AbstractActivityExecution<?, ?, WS> activityExecution) {
        super(activityExecution.getBeans());
        this.activityExecution = activityExecution;
        this.activityStateDefinition = activityExecution.getActivityStateDefinition();
        this.workStateComplexTypeDefinition = determineWorkStateDefinition(this.activityStateDefinition.getWorkStateTypeName());
        this.liveProgress = new ActivityProgress(this);
        this.liveStatistics = new ActivityStatistics(this);
    }

    //region Initialization
    /**
     * Puts the activity state into operation:
     *
     * 1. finds/creates activity and optionally also work state container values;
     * 2. initializes live structures - currently that means progress and statistics objects.
     *
     * This method may or may not be called just before the real execution. For example, there can be a need to initialize
     * the state of all child activities before starting their real execution.
     */
    public void initialize(OperationResult result) throws ActivityExecutionException {
        if (initialized) {
            return;
        }
        try {
            stateItemPath = findOrCreateActivityState(result);
            updatePersistenceType(result);
            if (activityExecution.shouldCreateWorkStateOnInitialization()) {
                createWorkStateIfNeeded(result);
            }
            liveProgress.initialize(getStoredProgress());
            liveStatistics.initialize();
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
        LOGGER.trace("findOrCreateActivityWorkState starting in activity with path '{}' in {}",
                activityExecution.getActivityPath(), getTask());
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
        Task task = getTask();
        ActivityStateType value = task.getActivityStateOrClone(ROOT_ACTIVITY_STATE_PATH);
        if (value == null) {
            addActivityState(TaskType.F_ACTIVITY_STATE, result, task, getActivity().getIdentifier());
        }
    }

    @NotNull
    private ItemPath findOrCreateChildActivityState(ItemPath parentWorkStatePath, String identifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Task task = getTask();
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
    private ItemPath findChildState(Task task, ItemPath parentWorkStatePath, String identifier) {
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

    private void addActivityState(ItemPath stateItemPath, OperationResult result, Task task, String identifier)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(stateItemPath.append(ActivityStateType.F_ACTIVITY))
                .add(new ActivityStateType(getPrismContext())
                        .identifier(identifier)
                        .persistence(activityStateDefinition.getPersistence()))
                .asItemDelta();
        task.modify(itemDelta);
        task.flushPendingModifications(result);
        task.refresh(result);
        LOGGER.debug("Activity state created for activity identifier={} in {} in {}", identifier, stateItemPath, task);
    }

    private void createWorkStateIfNeeded(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemPath path = stateItemPath.append(ActivityStateType.F_WORK_STATE);
        if (!getTask().doesItemExist(path)) {
            createWorkState(path, result);
        }
    }

    private void createWorkState(ItemPath path, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Task task = getTask();
        PrismContainerDefinition<?> def = getPrismContext().definitionFactory().createContainerDefinition(
                ActivityStateType.F_WORK_STATE, workStateComplexTypeDefinition);
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

    private void updatePersistenceType(OperationResult result) throws ActivityExecutionException {
        ActivityStatePersistenceType storedValue =
                getPropertyRealValue(ActivityStateType.F_PERSISTENCE, ActivityStatePersistenceType.class);
        ActivityStatePersistenceType requiredValue = activityStateDefinition.getPersistence();
        if (requiredValue != storedValue) {
            setItemRealValues(ActivityStateType.F_PERSISTENCE, requiredValue);
            flushPendingModificationsChecked(result);
        }
    }
    //endregion

    //region Realization state
    public void markInProgressLocal(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_LOCAL), w(IN_PROGRESS), result);
    }

    public void markInProgressDelegated(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_DELEGATED), w(IN_PROGRESS), result);
    }

    public void markInProgressDistributed(OperationResult result) throws ActivityExecutionException {
        setCombined(w(ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED), w(IN_PROGRESS), result);
    }

    public void markCompleteNoCommit(OperationResultStatus resultStatus) throws ActivityExecutionException {
        setCombinedNoCommit(w(ActivityRealizationStateType.COMPLETE), w(resultStatus));
    }

    private void setRealizationStateWithoutCommit(ActivityRealizationStateType value) throws SchemaException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(getRealizationStateItemPath()).replace(value)
                .asItemDelta();
        getTask().modify(itemDelta);
        LOGGER.trace("Setting realization state to {} for {}", value, activityExecution);
    }
    //endregion

    //region Result status
    public void setResultStatusNoCommit(@NotNull OperationResultStatus status) throws ActivityExecutionException {
        convertException(
                () -> setResultStateNoCommit(status));
    }

    private void setResultStateNoCommit(OperationResultStatus resultStatus) throws SchemaException {
        ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                .item(getResultStatusItemPath()).replace(OperationResultStatus.createStatusType(resultStatus))
                .asItemDelta();
        getTask().modify(itemDelta);
        LOGGER.trace("Setting result status to {} for {}", resultStatus, activityExecution);
    }
    //endregion

    //region Combined operations
    private void setCombined(Wrapper<ActivityRealizationStateType> realizationState, Wrapper<OperationResultStatus> resultStatus,
            OperationResult result) throws ActivityExecutionException {
        setCombinedNoCommit(realizationState, resultStatus);
        flushPendingModificationsChecked(result);
    }

    private void setCombinedNoCommit(Wrapper<ActivityRealizationStateType> realizationState,
            Wrapper<OperationResultStatus> resultStatus) throws ActivityExecutionException {
        try {
            if (realizationState != null) {
                setRealizationStateWithoutCommit(realizationState.value);
            }
            if (resultStatus != null) {
                setResultStateNoCommit(resultStatus.value);
            }
        } catch (SchemaException e) {
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
    @NotNull Activity<?, ?> getActivity() {
        return activityExecution.getActivity();
    }

    @Override
    public @NotNull ActivityPath getActivityPath() {
        return getActivity().getPath();
    }

    public @NotNull AbstractActivityExecution<?, ?, WS> getActivityExecution() {
        return activityExecution;
    }

    CommonTaskBeans getBeans() {
        return activityExecution.getBeans();
    }

    public ItemPath getItemPath() {
        return stateItemPath;
    }

    protected @NotNull Task getTask() {
        return activityExecution.getRunningTask();
    }

    private PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    @Override
    public @NotNull ComplexTypeDefinition getWorkStateComplexTypeDefinition() {
        return workStateComplexTypeDefinition;
    }

    //endregion

    //region Progress & statistics
    public @NotNull ActivityProgress getLiveProgress() {
        return liveProgress;
    }

    public ActivityProgressType getStoredProgress() {
        return getItemRealValueClone(ActivityStateType.F_PROGRESS, ActivityProgressType.class);
    }

    public @NotNull ActivityStatistics getLiveStatistics() {
        return liveStatistics;
    }

    public @NotNull ActivityItemProcessingStatistics getLiveItemProcessingStatistics() {
        return liveStatistics.getLiveItemProcessing();
    }

    public ActivityItemProcessingStatisticsType getStoredItemProcessingStatistics() {
        return liveStatistics.getStoredItemProcessing();
    }

    public ActivityBucketManagementStatisticsType getStoredBucketManagementStatistics() {
        return liveStatistics.getStoredBucketManagement();
    }

    public void updateProgressAndStatisticsNoCommit() throws ActivityExecutionException {
        if (activityExecution.doesSupportProgress()) {
            liveProgress.writeToTaskAsPendingModification();
            LegacyProgressUpdater.update(this);
        }
        if (activityExecution.doesSupportStatistics()) {
            liveStatistics.writeToTaskAsPendingModifications();
        }
    }
    //endregion

    //region toString + debugDump

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "Realization state", String.valueOf(getRealizationState()), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Result status", String.valueOf(getResultStatusRaw()), indent + 1);
        // Consider adding work state here, maybe bucketing etc
    }

    @Override
    protected @NotNull String getEnhancedClassName() {
        return getClass().getSimpleName() + "<" + workStateComplexTypeDefinition.getTypeName().getLocalPart() + ">";
    }
    //endregion
}
