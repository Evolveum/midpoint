/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.UpdateActivityPoliciesOperation;
import com.evolveum.midpoint.repo.common.activity.run.state.counters.CountersIncrementOperation;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.CheckedCommonRunnable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.isLocal;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.*;

public abstract class ActivityState implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityState.class);

    private static final @NotNull ItemPath BUCKETING_ROLE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKETS_PROCESSING_ROLE);
    private static final @NotNull ItemPath SCAVENGER_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_SCAVENGER);
    private static final @NotNull ItemPath SIMULATION_RESULT_REF_PATH =
            ItemPath.create(ActivityStateType.F_SIMULATION, ActivitySimulationStateType.F_RESULT_REF);
    private static final @NotNull ItemPath SIMULATION_RESULT_CREATED_PATH =
            ItemPath.create(ActivityStateType.F_SIMULATION, ActivitySimulationStateType.F_RESULT_CREATED);

    private static final int MAX_TREE_DEPTH = 30;

    @NotNull protected final CommonTaskBeans beans;

    /**
     * Path to the work state container value related to this run. Can be null if the state was not
     * yet initialized (for the {@link CurrentActivityState}) or if the state does not exist in a given
     * task (for the {@link OtherActivityState}).
     */
    ItemPath stateItemPath;

    protected ActivityState(@NotNull CommonTaskBeans beans) {
        this.beans = beans;
    }

    //region Specific getters
    public ActivityRealizationStateType getRealizationState() {
        return getTask().getPropertyRealValue(getRealizationStateItemPath(), ActivityRealizationStateType.class);
    }

    private @NotNull ItemPath getRealizationStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_REALIZATION_STATE);
    }

    /** TODO */
    public boolean isComplete() {
        return getRealizationState() == ActivityRealizationStateType.COMPLETE;
    }

    public XMLGregorianCalendar getRealizationStartTimestamp() {
        return getTask().getPropertyRealValue(
                stateItemPath.append(ActivityStateType.F_REALIZATION_START_TIMESTAMP),
                XMLGregorianCalendar.class);
    }

    OperationResultStatusType getResultStatusRaw() {
        return getTask().getPropertyRealValue(getResultStatusItemPath(), OperationResultStatusType.class);
    }

    public OperationResultStatus getResultStatus() {
        return OperationResultStatus.parseStatusType(getResultStatusRaw());
    }

    private @NotNull ItemPath getResultStatusItemPath() {
        return stateItemPath.append(ActivityStateType.F_RESULT_STATUS);
    }
    //endregion

    //region Bucketing
    public BucketsProcessingRoleType getBucketingRole() {
        return getPropertyRealValue(BUCKETING_ROLE_PATH, BucketsProcessingRoleType.class);
    }

    public boolean isWorker() {
        return getBucketingRole() == BucketsProcessingRoleType.WORKER;
    }

    public boolean isScavenger() {
        return Boolean.TRUE.equals(getPropertyRealValue(SCAVENGER_PATH, Boolean.class));
    }
    //endregion

    //region Generic access
    public <T> T getPropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getTask()
                .getPropertyRealValue(stateItemPath.append(path), expectedType);
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectReferenceType getReferenceRealValue(ItemPath path) {
        return getTask()
                .getReferenceRealValue(stateItemPath.append(path));
    }

    <T> T getItemRealValueClone(ItemPath path, Class<T> expectedType) {
        return getTask()
                .getItemRealValueOrClone(stateItemPath.append(path), expectedType);
    }
    // FIXME exception handling

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    public void setItemRealValues(ItemPath path, Object... values) throws ActivityRunException {
        convertException(
                () -> setItemRealValuesInternal(path, isSingleNull(values) ? List.of() : Arrays.asList(values)));
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    @SuppressWarnings("unused") // maybe will be used in the future
    public void setItemRealValuesCollection(ItemPath path, Collection<?> values) throws ActivityRunException {
        convertException(
                () -> setItemRealValuesInternal(path, values));
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    private void setItemRealValuesInternal(ItemPath path, Collection<?> values) throws SchemaException {
        Task task = getTask();
        LOGGER.trace("setItemRealValuesInternal: path={}, values={} in {}", path, values, task);

        task.modify(
                PrismContext.get().deltaFor(TaskType.class)
                        .item(stateItemPath.append(path))
                        .replaceRealValues(values)
                        .asItemDelta());
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
     public void addDeleteItemRealValues(@NotNull ItemPath path, @NotNull Collection<?> valuesToAdd,
             @NotNull Collection<?> valuesToDelete)
             throws ActivityRunException {
        Task task = getTask();
        LOGGER.trace("addDeleteItemRealValues: path={}, valuesToAdd={}, valuesToDelete={} in {}",
                path, valuesToAdd, valuesToDelete, task);

        convertException(
                () -> task.modify(
                        PrismContext.get().deltaFor(TaskType.class)
                                .item(stateItemPath.append(path))
                                .deleteRealValues(valuesToDelete)
                                .addRealValues(valuesToAdd)
                                .asItemDelta()));
    }

    /**
     * Flushes pending task modifications.
     * Note for implementers: this method should be equivalent to a direct call to {@link Task#flushPendingModifications(OperationResult)}.
     */
    public void flushPendingTaskModifications(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        getTask().flushPendingModifications(result);
    }

    /**
     * Flushes pending task modifications.
     * Note for implementers: this method should be equivalent to a direct call to {@link Task#flushPendingModifications(OperationResult)}.
     */
    public void flushPendingTaskModificationsChecked(OperationResult result) throws ActivityRunException {
        convertException("Couldn't update the task",
                () -> flushPendingTaskModifications(result));
    }
    //endregion

    //region Work state

    @NotNull ComplexTypeDefinition determineWorkStateDefinition(@NotNull QName typeName) {
        return requireNonNull(
                PrismContext.get().getSchemaRegistry()
                        .findComplexTypeDefinitionByType(typeName),
                () -> new SystemException("Couldn't find definition for " + typeName));
    }

    public abstract @Nullable ComplexTypeDefinition getWorkStateComplexTypeDefinition();

    public <T> T getWorkStatePropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getPropertyRealValue(ActivityStateType.F_WORK_STATE.append(path), expectedType);
    }

    @SuppressWarnings("unused")
    public <T> T getWorkStateItemRealValueClone(ItemPath path, Class<T> expectedType) {
        return getItemRealValueClone(ActivityStateType.F_WORK_STATE.append(path), expectedType);
    }

    public ObjectReferenceType getWorkStateReferenceRealValue(ItemPath path) {
        return getTask()
                .getReferenceRealValue(getWorkStateItemPath().append(path));
    }

    @SuppressWarnings("unused")
    public Collection<ObjectReferenceType> getWorkStateReferenceRealValues(ItemPath path) {
        return getTask()
                .getReferenceRealValues(getWorkStateItemPath().append(path));
    }

    public void setWorkStateItemRealValues(ItemPath path, Object... values) throws SchemaException {
        setWorkStateItemRealValues(path, null, values);
    }

    /**
     * @param explicitDefinition If present, we do not try to derive the definition from work state CTD.
     */
    public void setWorkStateItemRealValues(ItemPath path, ItemDefinition<?> explicitDefinition, Object... values)
            throws SchemaException {
        setWorkStateItemRealValues(path, explicitDefinition, isSingleNull(values) ? List.of() : Arrays.asList(values));
    }

    /**
     * @param explicitDefinition If present, we do not try to derive the definition from work state CTD.
     */
    private void setWorkStateItemRealValues(ItemPath path, ItemDefinition<?> explicitDefinition, Collection<?> values)
            throws SchemaException {
        Task task = getTask();
        LOGGER.trace("setWorkStateItemRealValues: path={}, values={} in {}", path, values, task);

        ItemDefinition<?> workStateItemDefinition = getWorkStateItemDefinition(path, explicitDefinition);
        stateCheck(
                workStateItemDefinition != null,
                "Couldn't modify work state (path = '%s'), as the work state definition is not known. "
                        + "Has it been initialized?", path);
        task.modify(
                PrismContext.get().deltaFor(TaskType.class)
                        .item(getWorkStateItemPath().append(path), workStateItemDefinition)
                        .replaceRealValues(values)
                        .asItemDelta());
    }

    private @Nullable ItemDefinition<?> getWorkStateItemDefinition(ItemPath path, ItemDefinition<?> explicitDefinition)
            throws SchemaException {
        if (explicitDefinition != null) {
            return explicitDefinition;
        }
        ComplexTypeDefinition workStateTypeDef = getWorkStateComplexTypeDefinition();
        if (workStateTypeDef != null) {
            //noinspection RedundantTypeArguments
            return MiscUtil.<ItemDefinition<?>, SchemaException>requireNonNull(
                    workStateTypeDef.findItemDefinition(path),
                    () -> new SchemaException("Definition for " + path + " couldn't be found in " + workStateTypeDef));
        } else {
            return null;
        }
    }

    @NotNull ItemPath getWorkStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_WORK_STATE);
    }
    //endregion

    //region Misc
    protected abstract @NotNull Task getTask();

    private void convertException(CheckedCommonRunnable runnable) throws ActivityRunException {
        convertException("Couldn't update activity state", runnable);
    }

    private void convertException(String message, CheckedCommonRunnable runnable) throws ActivityRunException {
        try {
            runnable.run();
        } catch (CommonException e) {
            throw new ActivityRunException(message, FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public void setSimulationResultOid(String oid) throws ActivityRunException {
        if (oid != null) {
            setItemRealValues(SIMULATION_RESULT_REF_PATH, ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SIMULATION_RESULT));
        } else {
            setItemRealValues(SIMULATION_RESULT_REF_PATH);
        }
    }

    public void setSimulationResultCreated() throws ActivityRunException {
        setItemRealValues(SIMULATION_RESULT_CREATED_PATH, true);
    }

    public @Nullable ObjectReferenceType getSimulationResultRef() {
        return getReferenceRealValue(SIMULATION_RESULT_REF_PATH);
    }

    public @Nullable String getSimulationResultOid() {
        return getOid(getSimulationResultRef());
    }

    public boolean isSimulationResultCreated() {
        return Boolean.TRUE.equals(
                getPropertyRealValue(SIMULATION_RESULT_CREATED_PATH, Boolean.class));
    }
    //endregion

    //region debugDump + toString
    @Override
    public String toString() {
        return getEnhancedClassName() + "{" +
                "task=" + getTask().getOid() +
                ", stateItemPath=" + stateItemPath +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getEnhancedClassName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Item path", String.valueOf(stateItemPath), indent + 1);
        debugDumpExtra(sb, indent);
        return sb.toString();
    }

    protected abstract void debugDumpExtra(StringBuilder sb, int indent);

    protected @NotNull String getEnhancedClassName() {
        return getClass().getSimpleName();
    }
    //endregion

    //region Navigation

    /**
     * Returns the state of the _parent activity_, e.g. operations completion sub-activity -> reconciliation activity.
     */
    public @NotNull ActivityState getParentActivityState(@Nullable QName workStateTypeName, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ActivityPath activityPath = getActivityPath();
        argCheck(!activityPath.isEmpty(), "Root activity has no parent");
        return getActivityStateUpwards(activityPath.allExceptLast(), getTask(), workStateTypeName, beans, result);
    }

    /**
     * Returns an iterator over activity states, from the parent of the current activity to the root.
     * Note that the _work_ states must either exist, or must not be modified by the caller after obtaining
     * (as we don't know their type - at least for now).
     *
     * The idea is to encapsulate the crawling logic, while avoiding the (eventual) task fetch operations if they are not
     * really needed.
     *
     * BEWARE: The {@link OperationResult} instance is buried into the iterator, and used within {@link Iterator#next()} calls.
     * This is quite dangerous. Maybe we should not do this at all.
     *
     * The best use of this method is when the result is consumed (iterated through) just after it is obtained. That way
     * you avoid the risk of using {@link OperationResult} instance at an unexpected place.
     */
    @Experimental
    public Iterable<ActivityState> getActivityStatesUpwardsForParent(OperationResult result) {
        Iterator<ActivityState> iterator = getActivityStatesUpwardsIterator(result);
        iterator.next();
        return () -> iterator;
    }

    private Iterator<ActivityState> getActivityStatesUpwardsIterator(OperationResult result) {
        return new Iterator<>() {
            private ActivityState next = ActivityState.this;

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public ActivityState next() {
                var current = next;
                ActivityPath activityPath = current.getActivityPath();
                if (activityPath.isEmpty()) {
                    next = null;
                } else {
                    try {
                        next = getActivityStateUpwards(
                                activityPath.allExceptLast(),
                                current.getTask(),
                                null,
                                beans,
                                result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw SystemException.unexpected(e, "when obtaining parent activity state for " + current);
                    }
                }
                return current;
            }
        };
    }

    /**
     * Returns activity state for given path, crawling from the current task upwards.
     *
     * @param activityPath Path to activity for which to obtain activity state.
     * @param task Task where to start searching.
     * @param workStateTypeName Expected type of the work state.
     */
    public static @NotNull ActivityState getActivityStateUpwards(
            @NotNull ActivityPath activityPath,
            @NotNull Task task,
            @Nullable QName workStateTypeName,
            @NotNull CommonTaskBeans beans,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getActivityStateUpwards(activityPath, task, workStateTypeName, 0, beans, result);
    }

    private static @NotNull ActivityState getActivityStateUpwards(
            @NotNull ActivityPath activityPath,
            @NotNull Task task,
            @Nullable QName workStateTypeName,
            int level,
            @NotNull CommonTaskBeans beans,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        TaskActivityStateType taskActivityState = getTaskActivityStateRequired(task);
        if (isLocal(activityPath, taskActivityState)) {
            return new OtherActivityState(task, taskActivityState, activityPath, workStateTypeName, beans);
        }
        if (level >= MAX_TREE_DEPTH) {
            throw new IllegalStateException("Maximum tree depth reached while looking for activity state in " + task);
        }
        Task parentTask = task.getParentTask(result);
        return getActivityStateUpwards(activityPath, parentTask, workStateTypeName, level + 1, beans, result);
    }

    /**
     * Returns the state of the current activity in the parent task. Assumes that it exists.
     *
     * @param fresh true if we always need to load the parent task from repository; false if we can use
     * cached version (created when the running task started)
     *
     * @param result Can be null if we are 100% sure it will not be used.
     */
    public @NotNull ActivityState getCurrentActivityStateInParentTask(boolean fresh, @NotNull QName workStateTypeName,
            @Nullable OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task parentTask = getParentTask(fresh, result);
        return new OtherActivityState(
                parentTask,
                parentTask.getActivitiesStateOrClone(),
                getActivityPath(),
                workStateTypeName,
                beans);
    }

    /**
     * @param fresh False if we are OK with the cached version of the parent task (if current task is RunningTask).
     * @param result Can be null if we are 100% sure it will not be used.
     */
    private @NotNull Task getParentTask(boolean fresh, @Nullable OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task task = getTask();
        Task parentTask;
        if (!fresh && task instanceof RunningTask) {
            parentTask = ((RunningTask) task).getParentTask();
        } else {
            parentTask = task.getParentTask(result);
        }
        return java.util.Objects.requireNonNull(parentTask, () -> "No parent task for " + task);
    }

    /**
     * Gets the state of the given activity, starting from the `task` and going downwards.
     *
     * UNTESTED. Use with care.
     *
     * TODO cleanup and test thoroughly
     */
    public static @NotNull ActivityState getActivityStateDownwards(@NotNull ActivityPath activityPath, @NotNull Task task,
            @NotNull QName workStateTypeName, @NotNull CommonTaskBeans beans, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getActivityStateDownwards(activityPath, task, workStateTypeName, 0, beans, result);
    }

    private static @NotNull ActivityState getActivityStateDownwards(@NotNull ActivityPath activityPath, @NotNull Task task,
            @NotNull QName workStateTypeName, int level, @NotNull CommonTaskBeans beans, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        TaskActivityStateType taskActivityState = getTaskActivityStateRequired(task);
        if (level >= MAX_TREE_DEPTH) {
            throw new IllegalStateException("Maximum tree depth reached while looking for activity state in " + task);
        }

        ActivityPath localRootPath = ActivityStateUtil.getLocalRootPath(taskActivityState);
        stateCheck(activityPath.startsWith(localRootPath), "Activity (%s) is not within the local tree (%s)",
                activityPath, localRootPath);

        ActivityStateType currentWorkState = taskActivityState.getActivity();
        ItemPath currentWorkStatePath = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);
        List<String> localIdentifiers = activityPath.getIdentifiers().subList(localRootPath.size(), activityPath.size());
        for (String identifier : localIdentifiers) {
            stateCheck(currentWorkState != null, "Current work state is not present; path = %s", currentWorkStatePath);
            currentWorkState = ActivityStateUtil.findChildActivityStateRequired(currentWorkState, identifier);
            stateCheck(currentWorkState.getId() != null, "Activity work state without ID: %s", currentWorkState);
            currentWorkStatePath = currentWorkStatePath.append(ActivityStateType.F_ACTIVITY, currentWorkState.getId());
            if (currentWorkState.getWorkState() instanceof DelegationWorkStateType) {
                ObjectReferenceType childRef = ((DelegationWorkStateType) currentWorkState.getWorkState()).getTaskRef();
                Task child = beans.taskManager.getTaskPlain(childRef.getOid(), result);
                return getActivityStateDownwards(activityPath, child, workStateTypeName, level + 1, beans, result);
            }
        }
        LOGGER.trace(" -> resulting work state path: {}", currentWorkStatePath);
        return new OtherActivityState(task, taskActivityState, activityPath, workStateTypeName, beans);
    }

    private static TaskActivityStateType getTaskActivityStateRequired(@NotNull Task task) {
        return Objects.requireNonNull(
                task.getActivitiesStateOrClone(),
                () -> "No task activity state in " + task);
    }

    public abstract @NotNull ActivityPath getActivityPath();
    //endregion

    //region Counters (thresholds)
    public Map<String, Integer> incrementCounters(@NotNull ExecutionSupport.CountersGroup counterGroup,
            @NotNull Collection<String> countersIdentifiers, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemPath counterGroupItemPath = stateItemPath.append(ActivityStateType.F_COUNTERS, counterGroup.getItemName());
        return new CountersIncrementOperation(getTask(), counterGroupItemPath, countersIdentifiers, beans)
                .execute(result);
    }

    // todo make nicer [viliam]
    public void clearCounters(@NotNull ExecutionSupport.CountersGroup counterGroup, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ItemPath counterGroupItemPath = stateItemPath.append(ActivityStateType.F_COUNTERS, counterGroup.getItemName());
        beans.plainRepositoryService.modifyObjectDynamically(
                TaskType.class, getTask().getOid(), null,
                task -> PrismContext.get().deltaFor(TaskType.class)
                            .item(counterGroupItemPath)
                            .replace(List.of())
                            .asItemDeltas(), null, result);
    }
    //endregion

    //region Policies (thresholds)
    public Map<String, ActivityPolicyStateType> updatePolicies(
            @NotNull Collection<ActivityPolicyStateType> policies, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        ItemPath policiesItemPath = stateItemPath.append(ActivityStateType.F_POLICIES, ActivityPoliciesStateType.F_ACTIVITY_POLICIES);

        return new UpdateActivityPoliciesOperation(getTask(), policiesItemPath, policies, beans).execute(result);
    }
    //endregion

    public Integer getExecutionAttempt() {
        return getTask().getPropertyRealValue(
                stateItemPath.append(ActivityStateType.F_EXECUTION_ATTEMPT), Integer.class);
    }

    // todo make it cleaner, move to custom operation class together with preparation/store
    //  of new execution attempt (history) for state+overview/tree
    public void incrementExecutionAttempt(@NotNull OperationResult result)
        throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
            beans.plainRepositoryService.modifyObjectDynamically(
                    TaskType.class, getTask().getOid(), null, this::prepareModifications, null, result);
        // todo implement increment execution attempt + store previous execution in activity execution (also tree?)
    }

    private @NotNull Collection<? extends ItemDelta<?, ?>> prepareModifications(TaskType task) throws SchemaException {
        Integer executionAttempt = task.asPrismObject().getPropertyRealValue(
                stateItemPath.append(ActivityStateType.F_EXECUTION_ATTEMPT), Integer.class);
        if (executionAttempt == null) {
            executionAttempt = 0;
        }
        executionAttempt++;

        return PrismContext.get().deltaFor(TaskType.class)
                .item(stateItemPath.append(ActivityStateType.F_EXECUTION_ATTEMPT)).replace(executionAttempt)
                .asItemDeltas();
    }
}
