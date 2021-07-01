/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.isLocal;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.*;

public abstract class ActivityState<WS extends AbstractActivityWorkStateType> implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityState.class);

    private static final @NotNull ItemPath BUCKETING_ROLE_PATH = ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKETS_PROCESSING_ROLE);
    private static final @NotNull ItemPath SCAVENGER_PATH = ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_SCAVENGER);

    private static final int MAX_TREE_DEPTH = 30;

    /**
     * Path to the work state container value related to this execution. Can be null if the state was not
     * yet initialized (for the {@link CurrentActivityState}) or if the state does not exist in a given
     * task (for the {@link OtherActivityState}).
     */
    protected ItemPath stateItemPath;

    //region Specific getters

    public ActivityRealizationStateType getRealizationState() {
        return getTask().getPropertyRealValue(getRealizationStateItemPath(), ActivityRealizationStateType.class);
    }

    @NotNull ItemPath getRealizationStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_REALIZATION_STATE);
    }

    public boolean isComplete() {
        return getRealizationState() == ActivityRealizationStateType.COMPLETE;
    }

    public OperationResultStatusType getResultStatusRaw() {
        return getTask().getPropertyRealValue(getResultStatusItemPath(), OperationResultStatusType.class);
    }

    public OperationResultStatus getResultStatus() {
        return OperationResultStatus.parseStatusType(getResultStatusRaw());
    }

    @NotNull ItemPath getResultStatusItemPath() {
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

    public <T> T getItemRealValueClone(ItemPath path, Class<T> expectedType) {
        return getTask()
                .getItemRealValueOrClone(stateItemPath.append(path), expectedType);
    }
    // FIXME exception handling

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    public void setItemRealValues(ItemPath path, Object... values) throws ActivityExecutionException {
        convertException(
                () -> setItemRealValues(path, isSingleNull(values) ? List.of() : Arrays.asList(values)));
    }

    /**
     * DO NOT use for setting work state items because of dynamic typing of the work state container value.
     */
    public void setItemRealValues(ItemPath path, Collection<?> values) throws SchemaException {
        Task task = getTask();
        LOGGER.trace("setItemRealValues: path={}, values={} in {}", path, values, task);

        task.modify(
                PrismContext.get().deltaFor(TaskType.class)
                        .item(stateItemPath.append(path))
                        .replaceRealValues(values)
                        .asItemDelta());
    }

    public void flushPendingModifications(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        getTask().flushPendingModifications(result);
    }

    public void flushPendingModificationsChecked(OperationResult result) throws ActivityExecutionException {
        convertException("Couldn't update the task",
                () -> flushPendingModifications(result));
    }
    //endregion

    //region Work state

    @NotNull ComplexTypeDefinition determineWorkStateDefinition(@NotNull QName typeName) {
        return requireNonNull(
                PrismContext.get().getSchemaRegistry()
                        .findComplexTypeDefinitionByType(typeName),
                () -> new SystemException("Couldn't find definition for " + typeName));
    }

    public abstract @NotNull ComplexTypeDefinition getWorkStateComplexTypeDefinition();

    public <T> T getWorkStatePropertyRealValue(ItemPath path, Class<T> expectedType) {
        return getPropertyRealValue(ActivityStateType.F_WORK_STATE.append(path), expectedType);
    }

    public <T> T getWorkStateItemRealValueClone(ItemPath path, Class<T> expectedType) {
        return getItemRealValueClone(ActivityStateType.F_WORK_STATE.append(path), expectedType);
    }

    public ObjectReferenceType getWorkStateReferenceRealValue(ItemPath path) {
        return getTask()
                .getReferenceRealValue(getWorkStateItemPath().append(path));
    }

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
    public void setWorkStateItemRealValues(ItemPath path, ItemDefinition<?> explicitDefinition, Collection<?> values)
            throws SchemaException {
        Task task = getTask();
        LOGGER.trace("setWorkStateItemRealValues: path={}, values={} in {}", path, values, task);

        task.modify(
                PrismContext.get().deltaFor(TaskType.class)
                        .item(getWorkStateItemPath().append(path), getWorkStateItemDefinition(path, explicitDefinition))
                        .replaceRealValues(values)
                        .asItemDelta());
    }

    private @NotNull ItemDefinition<?> getWorkStateItemDefinition(ItemPath path, ItemDefinition<?> definition) throws SchemaException {
        if (definition != null) {
            return definition;
        }
        ComplexTypeDefinition workStateTypeDef = getWorkStateComplexTypeDefinition();
        return MiscUtil.<ItemDefinition<?>, SchemaException>requireNonNull(
                workStateTypeDef.findItemDefinition(path),
                () -> new SchemaException("Definition for " + path + " couldn't be found in " + workStateTypeDef));
    }

    private @NotNull ItemPath getWorkStateItemPath() {
        return stateItemPath.append(ActivityStateType.F_WORK_STATE);
    }
    //endregion

    //region Misc
    protected abstract @NotNull Task getTask();

    void convertException(CheckedRunnable runnable) throws ActivityExecutionException {
        convertException("Couldn't update activity state", runnable);
    }

    private void convertException(String message, CheckedRunnable runnable) throws ActivityExecutionException {
        try {
            runnable.run();
        } catch (CommonException e) {
            throw new ActivityExecutionException(message, FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }
    //endregion

    //region debugDump + toString
    @Override
    public String toString() {
        return getEnhancedClassName() + "{" +
                "stateItemPath=" + stateItemPath +
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
     * Note: the caller must know the work state type name. This can be resolved somehow in the future, e.g. by requiring
     * that the work state already exists.
     */
    public @NotNull ActivityState<?> getParentActivityState(@NotNull QName workStateTypeName, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ActivityPath activityPath = getActivityPath();
        argCheck(!activityPath.isEmpty(), "Root activity has no parent");
        return getActivityState(activityPath.allExceptLast(), getTask(), workStateTypeName, 0, result);
    }

    public static @NotNull ActivityState<?> getActivityState(@NotNull ActivityPath activityPath, @NotNull Task task,
            @NotNull QName workStateTypeName, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getActivityState(activityPath, task, workStateTypeName, 0, result);
    }

    private static @NotNull ActivityState<?> getActivityState(@NotNull ActivityPath activityPath, @NotNull Task task,
            @NotNull QName workStateTypeName, int level, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskActivityStateType taskActivityState =
                Objects.requireNonNull(
                        task.getActivitiesStateOrClone(),
                        () -> "No task activity state in " + task);
        if (isLocal(activityPath, taskActivityState)) {
            return new OtherActivityState<>(task, taskActivityState, activityPath, workStateTypeName);
        }
        if (level >= MAX_TREE_DEPTH) {
            throw new IllegalStateException("Maximum tree depth reached while looking for activity state in " + task);
        }
        Task parentTask = task.getParentTask(result);
        return getActivityState(activityPath, parentTask, workStateTypeName, level + 1, result);
    }

    public abstract @NotNull ActivityPath getActivityPath();
    //endregion
}
