/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.repo.common.activity.run.reports.BucketsReport.Kind.ANALYSIS;
import static com.evolveum.midpoint.repo.common.activity.run.reports.BucketsReport.Kind.EXECUTION;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.COMPLETE;

import java.util.Objects;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.reports.BucketsReport;
import com.evolveum.midpoint.repo.common.activity.run.reports.ConnIdOperationsReport;
import com.evolveum.midpoint.repo.common.activity.run.reports.InternalOperationsReport;
import com.evolveum.midpoint.repo.common.activity.run.reports.ItemsReport;
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

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Activity state for the current activity run. Provides the full functionality, including creation of the work state
 * and maintenance of live progress and statistics.
 *
 * @param <WS> Work (business) state of the activity.
 */
public class CurrentActivityState<WS extends AbstractActivityWorkStateType>
        extends ActivityState {

    private static final Trace LOGGER = TraceManager.getTrace(CurrentActivityState.class);

    private static final @NotNull ItemPath ROOT_ACTIVITY_STATE_PATH =
            ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);

    @NotNull private final AbstractActivityRun<?, ?, WS> activityRun;

    @NotNull private final ActivityStateDefinition<WS> activityStateDefinition;

    @NotNull private final ComplexTypeDefinition workStateComplexTypeDefinition;

    @NotNull private final ActivityProgress liveProgress;

    @NotNull private final ActivityStatistics liveStatistics;

    /** Report on buckets processed. */
    @NotNull private final BucketsReport bucketsReport;

    /** Report on items processed. */
    @NotNull private final ItemsReport itemsReport;

    /** Report on ConnId operations executed. */
    @NotNull private final ConnIdOperationsReport connIdOperationsReport;

    /** Report on internal operations executed. */
    @NotNull private final InternalOperationsReport internalOperationsReport;

    private boolean initialized;

    public CurrentActivityState(@NotNull AbstractActivityRun<?, ?, WS> activityRun) {
        super(activityRun.getBeans());
        this.activityRun = activityRun;
        this.activityStateDefinition = activityRun.getActivityStateDefinition();
        this.workStateComplexTypeDefinition = determineWorkStateDefinition(activityStateDefinition.getWorkStateTypeName());
        this.liveProgress = new ActivityProgress(this);
        this.liveStatistics = new ActivityStatistics(this);

        ActivityReportingDefinition reportingDef = activityRun.getActivity().getReportingDefinition();
        this.bucketsReport = new BucketsReport(reportingDef.getBucketsReportDefinition(), this,
                activityRun.isBucketAnalysis() ? ANALYSIS : EXECUTION);
        this.itemsReport = new ItemsReport(reportingDef.getItemsReportDefinition(), this);
        this.connIdOperationsReport = new ConnIdOperationsReport(reportingDef.getConnIdOperationsReportDefinition(), this);
        this.internalOperationsReport = new InternalOperationsReport(reportingDef.getInternalOperationsReportDefinition(), this);
    }

    //region Initialization and closing
    /**
     * Puts the activity state into operation:
     *
     * 1. finds/creates activity and optionally also work state container values;
     * 2. initializes live structures - currently that means progress and statistics objects.
     *
     * This method may or may not be called just before the real run. For example, there can be a need to initialize
     * the state of all child activities before starting their real run.
     */
    public void initialize(OperationResult result) throws ActivityRunException {
        if (initialized) {
            return;
        }
        try {
            stateItemPath = findOrCreateActivityState(result);
            updatePersistenceType(result);
            if (activityRun.shouldCreateWorkStateOnInitialization()) {
                createWorkStateIfNeeded(result);
            }
            liveProgress.initialize(getStoredProgress());
            liveStatistics.initialize();
            initialized = true;
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | RuntimeException e) {
            // We consider all such exceptions permanent. There's basically nothing that could resolve "by itself".
            throw new ActivityRunException("Couldn't initialize activity state for " + getActivity() + ": " + e.getMessage(),
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
                activityRun.getActivityPath(), getTask());
        AbstractActivityRun<?, ?, ?> localParentRun = activityRun.getLocalParentRun();
        if (localParentRun == null) {
            LOGGER.trace("No local parent run, checking or creating root work state");
            findOrCreateRootActivityState(result);
            return ROOT_ACTIVITY_STATE_PATH;
        } else {
            ItemPath parentWorkStatePath = localParentRun.getActivityState().findOrCreateActivityState(result);
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
                .add(new ActivityStateType()
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
        PrismContainerDefinition<?> def = getPrismContext().definitionFactory().newContainerDefinition(
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

    private void updatePersistenceType(OperationResult result) throws ActivityRunException {
        ActivityStatePersistenceType storedValue =
                getPropertyRealValue(ActivityStateType.F_PERSISTENCE, ActivityStatePersistenceType.class);
        ActivityStatePersistenceType requiredValue = activityStateDefinition.getPersistence();
        if (requiredValue != storedValue) {
            setItemRealValues(ActivityStateType.F_PERSISTENCE, requiredValue);
            flushPendingTaskModificationsChecked(result);
        }
    }

    /** Closes the activity state. Currently, this means closing the reports. */
    public void close() {
        bucketsReport.close();
        itemsReport.close();
        connIdOperationsReport.close();
        internalOperationsReport.close();
    }
    //endregion

    //region Realization state and realization/run timestamps
    public void recordRunStart(Long startTimestamp) throws ActivityRunException {
        setRunStartTimestamp(startTimestamp);
        setRunEndTimestamp(null);
    }

    public void recordRunEnd(Long endTimestamp) throws ActivityRunException {
        setRunEndTimestamp(endTimestamp);
    }

    public void recordRealizationStart(long startTimestamp) throws ActivityRunException {
        setRealizationStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(startTimestamp));
        setRealizationEndTimestamp(null);
    }

    public void recordRealizationStart(XMLGregorianCalendar startTimestamp) throws ActivityRunException {
        setRealizationStartTimestamp(startTimestamp);
        setRealizationEndTimestamp(null);
    }

    public void markSkipped(OperationResultStatus resultStatus, Long endTimestamp) throws ActivityRunException {
        setRealizationState(COMPLETE);  // todo maybe custom realization state for skipped
        setRealizationEndTimestamp(endTimestamp);
        setResultStatus(resultStatus);
    }

    public void markComplete(OperationResultStatus resultStatus, Long endTimestamp) throws ActivityRunException {
        setRealizationState(COMPLETE);
        setRealizationEndTimestamp(endTimestamp);
        setResultStatus(resultStatus);
    }

    public void setRealizationState(ActivityRealizationStateType value) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_REALIZATION_STATE, value);
    }

    private void setRealizationStartTimestamp(XMLGregorianCalendar value) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_REALIZATION_START_TIMESTAMP, value);
    }

    private void setRealizationEndTimestamp(Long value) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_REALIZATION_END_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    private void setRunStartTimestamp(Long value) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_RUN_START_TIMESTAMP, createXMLGregorianCalendar(value));
    }

    public void setRunEndTimestamp(Long value) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_RUN_END_TIMESTAMP, createXMLGregorianCalendar(value));
    }
    //endregion

    //region Result status
    public void setResultStatus(@NotNull OperationResultStatus status) throws ActivityRunException {
        setItemRealValues(ActivityStateType.F_RESULT_STATUS, createStatusType(status));
    }
    //endregion

    //region Misc
    @NotNull Activity<?, ?> getActivity() {
        return activityRun.getActivity();
    }

    @Override
    public @NotNull ActivityPath getActivityPath() {
        return getActivity().getPath();
    }

    public @NotNull AbstractActivityRun<?, ?, WS> getActivityRun() {
        return activityRun;
    }

    CommonTaskBeans getBeans() {
        return activityRun.getBeans();
    }

    public ItemPath getItemPath() {
        return stateItemPath;
    }

    protected @NotNull Task getTask() {
        return activityRun.getRunningTask();
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

    private ActivityProgressType getStoredProgress() {
        return getItemRealValueClone(ActivityStateType.F_PROGRESS, ActivityProgressType.class);
    }

    public @NotNull ActivityStatistics getLiveStatistics() {
        return liveStatistics;
    }

    public @NotNull ActivityItemProcessingStatistics getLiveItemProcessingStatistics() {
        return liveStatistics.getLiveItemProcessing();
    }

    public void updateProgressAndStatisticsNoCommit() throws ActivityRunException {
        updateProgressNoCommit();
        updateStatisticsNoCommit();
    }

    public void updateProgressNoCommit() throws ActivityRunException {
        if (activityRun.isProgressSupported()) {
            liveProgress.writeToTaskAsPendingModification();
            LegacyProgressUpdater.update(this);
        }
    }

    private void updateStatisticsNoCommit() throws ActivityRunException {
        if (activityRun.areStatisticsSupported()) {
            liveStatistics.writeToTaskAsPendingModifications();
        }
    }
    //endregion

    //region Reports

    public @NotNull BucketsReport getBucketsReport() {
        return bucketsReport;
    }

    public @NotNull ItemsReport getItemsReport() {
        return itemsReport;
    }

    public @NotNull ConnIdOperationsReport getConnIdOperationsReport() {
        return connIdOperationsReport;
    }

    public @NotNull InternalOperationsReport getInternalOperationsReport() {
        return internalOperationsReport;
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
