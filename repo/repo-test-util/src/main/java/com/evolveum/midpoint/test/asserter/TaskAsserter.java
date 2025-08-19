/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.FULL_STATE_ONLY;
import static com.evolveum.midpoint.schema.util.task.TaskResolver.empty;
import static com.evolveum.midpoint.util.MiscUtil.assertCheck;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.ActivityTreeUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("UnusedReturnValue")
public class TaskAsserter<RA> extends AssignmentHolderAsserter<TaskType, RA> {

    private static final String OP_CLOCKWORK_RUN = "com.evolveum.midpoint.model.impl.lens.Clockwork.run";

    private TaskAsserter(PrismObject<TaskType> object) {
        super(object);
    }

    private TaskAsserter(PrismObject<TaskType> object, String details) {
        super(object, details);
    }

    private TaskAsserter(PrismObject<TaskType> object, RA returnAsserter, String details) {
        super(object, returnAsserter, details);
    }

    @SuppressWarnings("unused")
    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object) {
        return new TaskAsserter<>(object);
    }

    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object, String details) {
        return new TaskAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public TaskAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public TaskAsserter<RA> display() {
        super.display();
        return this;
    }

    public TaskAsserter<RA> displayOperationResult() {
        OperationResultType resultBean = getTaskBean().getResult();
        if (resultBean != null) {
            IntegrationTestTools.display(desc() + " operation result:\n" + OperationResult.createOperationResult(resultBean).debugDump(1));
        } else {
            IntegrationTestTools.display(desc() + " has no operation result");
        }
        return this;
    }

    public TaskAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        return (TaskAsserter<RA>) super.assertArchetypeRef(expectedArchetypeOid);
    }

    @Override
    public TaskAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    public TaskAsserter<RA> assertProgress(long expected) {
        long actual = defaultIfNull(getObject().asObjectable().getProgress(), 0L);
        assertEquals("Wrong progress", expected, actual);
        return this;
    }

    public TaskAsserter<RA> assertToken(Object expected) {
        Object token;
        try {
            token = ActivityStateUtil.getRootSyncTokenRealValue(getObjectable());
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        assertThat(token).as("token").isEqualTo(expected);
        return this;
    }

    public TaskAsserter<RA> assertTaskRunHistorySize(int expected) {
        assertThat(getTaskBean().getTaskRunRecord())
                .as("task run record")
                .hasSize(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        return (TaskAsserter<RA>) super.assertPolyStringProperty(propName, expectedOrig);
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> rootSynchronizationInformation() {
        return synchronizationInformation(ActivityPath.empty());
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> synchronizationInformation(ActivityPath activityPath) {
        ActivityStatisticsType statistics = getStatisticsOrNew(activityPath);
        ActivitySynchronizationStatisticsType syncStatistics = requireNonNullElseGet(
                statistics.getSynchronization(), () -> new ActivitySynchronizationStatisticsType());

        SynchronizationInfoAsserter<TaskAsserter<RA>> asserter = new SynchronizationInfoAsserter<>(syncStatistics, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    /** Assumes single primitive activity */
    public ActivityItemProcessingStatisticsAsserter<TaskAsserter<RA>> rootItemProcessingInformation() {
        ActivityStatisticsType statistics = getStatisticsOrNew(ActivityPath.empty());
        ActivityItemProcessingStatisticsType itemProcessingStatistics = requireNonNullElseGet(
                statistics.getItemProcessing(), () -> new ActivityItemProcessingStatisticsType());

        ActivityItemProcessingStatisticsAsserter<TaskAsserter<RA>> asserter =
                new ActivityItemProcessingStatisticsAsserter<>(itemProcessingStatistics, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskActivityStateAsserter<TaskAsserter<RA>> activityState() {
        TaskActivityStateType activityState = Objects.requireNonNull(
                getObject().asObjectable().getActivityState(), "no activity state");
        TaskActivityStateAsserter<TaskAsserter<RA>> asserter = new TaskActivityStateAsserter<>(activityState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<TaskAsserter<RA>> rootActivityState() {
        return activityState(ActivityPath.empty());
    }

    public ActivityStateAsserter<TaskAsserter<RA>> activityState(ActivityPath activityPath) {
        ActivityStateType state = getActivityStateRequired(activityPath);
        ActivityStateAsserter<TaskAsserter<RA>> asserter = new ActivityStateAsserter<>(state, this, "activity state for " + activityPath.toDebugName() + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @SuppressWarnings("unused")
    public ActivityStateOverviewAsserter<TaskAsserter<RA>> rootActivityStateOverview() {
        var overview =
                Objects.requireNonNull(
                        Objects.requireNonNull(
                                Objects.requireNonNull(
                                                getTaskBean().getActivityState(), "no activities state")
                                        .getTree(), "no tree")
                                .getActivity(), "no root activity overview");

        ActivityStateOverviewAsserter<TaskAsserter<RA>> asserter =
                new ActivityStateOverviewAsserter<>(overview, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    /**
     * Assumes that the whole task tree is fully loaded!
     */
    public ActivityProgressInformationAsserter<TaskAsserter<RA>> progressInformation() {
        ActivityProgressInformationAsserter<TaskAsserter<RA>> asserter =
                new ActivityProgressInformationAsserter<>(
                        ActivityProgressInformation.fromRootTask(getObjectable(), FULL_STATE_ONLY),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertClosed() {
        assertExecutionState(TaskExecutionStateType.CLOSED);
        assertSchedulingState(TaskSchedulingStateType.CLOSED);
        return this;
    }

    public TaskAsserter<RA> assertSuspended() {
        assertExecutionState(TaskExecutionStateType.SUSPENDED);
        assertSchedulingState(TaskSchedulingStateType.SUSPENDED);
        return this;
    }

    public TaskAsserter<RA> assertExecutionState(TaskExecutionStateType status) {
        assertEquals("Wrong execution status", status, getTaskBean().getExecutionState());
        return this;
    }

    public TaskAsserter<RA> assertSchedulingState(TaskSchedulingStateType state) {
        assertEquals("Wrong scheduling state", state, getTaskBean().getSchedulingState());
        return this;
    }

    private TaskType getTaskBean() {
        return getObject().asObjectable();
    }

    public TaskAsserter<RA> assertSuccess() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertSuccess(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isIn(OperationResultStatusType.SUCCESS,
                            OperationResultStatusType.NOT_APPLICABLE,
                            OperationResultStatusType.HANDLED_ERROR);
        }
        return this;
    }

    @SuppressWarnings("unused")
    public TaskAsserter<RA> assertHandledError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertStatus(result, OperationResultStatusType.HANDLED_ERROR);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.HANDLED_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertWarning() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertStatus(result, OperationResultStatusType.WARNING);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.WARNING);
        }
        return this;
    }

    public TaskAsserter<RA> assertPartialError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertPartialError(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.PARTIAL_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertStatus(OperationResultStatusType status) {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertStatus(result, status);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(status);
        }
        return this;
    }

    public TaskAsserter<RA> assertInProgress() {
        return assertStatus(OperationResultStatusType.IN_PROGRESS);
    }

    public TaskAsserter<RA> assertFatalError() {
        return assertStatus(OperationResultStatusType.FATAL_ERROR);
    }

    public TaskAsserter<RA> assertResultMessageContains(String fragment) {
        OperationResultType result = getTaskBean().getResult();
        assertThat(result).as("operation result").isNotNull();
        assertThat(result.getMessage()).as("operation result message").contains(fragment);
        return this;
    }

    public TaskAsserter<RA> assertBinding(TaskBindingType binding) {
        assertEquals(binding, getTaskBean().getBinding());
        return this;
    }

    @Override
    public AssignmentsAsserter<TaskType, TaskAsserter<RA>, RA> assignments() {
        AssignmentsAsserter<TaskType, TaskAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectReferenceAsserter<UserType, RA> owner() {
        ObjectReferenceAsserter<UserType, RA> ownerAsserter = new ObjectReferenceAsserter<>(getTaskBean().getOwnerRef().asReferenceValue(), UserType.class);
        copySetupTo(ownerAsserter);
        return ownerAsserter;
    }

    public TaskAsserter<TaskAsserter<RA>> subtaskForPath(ActivityPath activityPath) {
        TaskType subtask =
                MiscUtil.extractSingletonRequired(
                        ActivityTreeUtil.getSubtasksForPath(getObjectable(), activityPath, empty()),
                        () -> new AssertionError("More than one subtask for activity path '" + activityPath + "'"),
                        () -> new AssertionError("No subtask for activity path '" + activityPath + "' found"));

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this, "subtask for path '" +
                activityPath + "' in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertSubtasks(int count) {
        assertThat(getObjectable().getSubtaskRef()).as("subtasks").hasSize(count);
        return this;
    }

    public TaskAsserter<TaskAsserter<RA>> subtask(int index) {
        List<ObjectReferenceType> subtasks = getObjectable().getSubtaskRef();
        assertCheck(subtasks.size() > index, "Expected to see at least %s subtask(s), but only %s are present",
                index + 1, subtasks.size());

        return subtask(subtasks, index);
    }

    public TaskAsserter<TaskAsserter<RA>> subtask(String name) {
        List<String> otherNames = new ArrayList<>();

        List<ObjectReferenceType> subtasks = getObjectable().getSubtaskRef();
        for (int i = 0; i < subtasks.size(); i++) {
            TaskType subtask = subtaskFromRef(subtasks, i);
            String subtaskName = subtask.getName().getOrig();
            if (subtaskName.equals(name)) {
                return subtask(subtasks, i);
            } else {
                otherNames.add(subtaskName);
            }
        }
        throw new AssertionError("No subtask with the name '" + name + "' found. Subtasks: " + otherNames);
    }

    private @NotNull TaskAsserter<TaskAsserter<RA>> subtask(List<ObjectReferenceType> subtasks, int index) {
        TaskType subtask = subtaskFromRef(subtasks, index);

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this,
                "subtask #" + index + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull TaskType subtaskFromRef(List<ObjectReferenceType> subtasks, int index) {
        ObjectReferenceType subtaskRef = subtasks.get(index);
        TaskType subtask = (TaskType) ObjectTypeUtil.getObjectFromReference(subtaskRef);
        assertThat(subtask).withFailMessage(() -> "Reference for subtask #" + index + " contains no object").isNotNull();
        return subtask;
    }

    public TaskAsserter<RA> assertLastTriggerScanTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        // Trigger Scan is running as a root activity.
        TestUtil.assertBetween("last scan timestamp in " + desc(), start, end, getLastScanTimestamp(ActivityPath.empty()));
        return this;
    }

    public TaskAsserter<RA> assertLastScanTimestamp(ActivityPath activityPath, XMLGregorianCalendar start,
            XMLGregorianCalendar end) {
        TestUtil.assertBetween("last scan timestamp in " + desc(), start, end, getLastScanTimestamp(activityPath));
        return this;
    }

    public XMLGregorianCalendar getLastScanTimestamp(ActivityPath activityPath) {
        return getActivityWorkState(activityPath, ScanWorkStateType.class)
                .getLastScanTimestamp();
    }

    public TaskAsserter<RA> assertCachingProfiles(String... expected) {
        assertThat(getCachingProfiles()).as("caching profiles").containsExactlyInAnyOrder(expected);
        return this;
    }

    private Collection<String> getCachingProfiles() {
        TaskExecutionEnvironmentType env = getObjectable().getExecutionEnvironment();
        return env != null ? env.getCachingProfile() : List.of();
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends AbstractActivityWorkStateType> T getActivityWorkState(ActivityPath activityPath, Class<T> expectedClass) {
        AbstractActivityWorkStateType workState = getActivityStateRequired(activityPath).getWorkState();
        assertThat(workState).as("work state").isInstanceOf(expectedClass);
        //noinspection unchecked
        return (T) workState;
    }

    private @NotNull ActivityStateType getActivityStateRequired(ActivityPath activityPath) {
        ActivityStateType state = ActivityStateUtil.getActivityState(getTaskBean(), activityPath);
        assertThat(state).withFailMessage("No task activity state").isNotNull();
        return state;
    }

    private ActivityStatisticsType getStatisticsOrNew(ActivityPath activityPath) {
        ActivityStateType state = getActivityStateRequired(activityPath);
        return requireNonNullElseGet(
                state.getStatistics(),
                () -> new ActivityStatisticsType());
    }

    /**
     * Loads immediate subtasks, if they are not loaded yet.
     */
    public TaskAsserter<RA> loadImmediateSubtasks(OperationResult result) throws SchemaException {
        TaskType task = getObjectable();
        if (!task.getSubtaskRef().isEmpty()) {
            return this; // assuming subtasks are already loaded
        }

        doLoadImmediateSubtasks(task, result);
        return this;
    }

    private void doLoadImmediateSubtasks(TaskType task, OperationResult result) throws SchemaException {
        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_PARENT).eq(task.getTaskIdentifier())
                .build();
        SearchResultList<PrismObject<TaskType>> children =
                getRepositoryService().searchObjects(TaskType.class, query, null, result);

        task.getSubtaskRef().clear();
        children.forEach(child ->
                task.getSubtaskRef().add(
                        ObjectTypeUtil.createObjectRefWithFullObject(child)));
    }

    /**
     * Loads all subtasks i.e. the whole subtree.
     */
    public TaskAsserter<RA> loadSubtasksDeeply(OperationResult result) throws SchemaException {
        doLoadSubtasksDeeply(getObjectable(), result);
        return this;
    }

    private void doLoadSubtasksDeeply(TaskType task, OperationResult result) throws SchemaException {
        doLoadImmediateSubtasks(task, result);
        List<ObjectReferenceType> subtaskRefList = task.getSubtaskRef();
        for (int i = 0; i < subtaskRefList.size(); i++) {
            TaskType subtask = subtaskFromRef(subtaskRefList, i);
            doLoadSubtasksDeeply(subtask, result);
        }
    }

    public TaskAsserter<RA> assertExecutionGroup(String expected) {
        assertThat(getExecutionGroup()).as("execution group").isEqualTo(expected);
        return this;
    }

    private String getExecutionGroup() {
        TaskType task = getObjectable();
        return task.getExecutionConstraints() != null ? task.getExecutionConstraints().getGroup() : null;
    }

    public TaskAsserter<RA> sendOid(Consumer<String> consumer) {
        super.sendOid(consumer);
        return this;
    }

    public TaskAsserter<RA> assertObjectRef(@NotNull String expectedOid, @NotNull QName expectedType) {
        ObjectReferenceType objectRef = getObjectable().getObjectRef();
        assertThat(objectRef).as("objectRef").isNotNull();
        assertThat(objectRef.getOid()).as("objectRef.oid").isEqualTo(expectedOid);
        assertThat(objectRef.getType()).as("objectRef.type").isEqualTo(expectedType);
        return this;
    }

    public TaskAsserter<RA> assertClockworkRunCount(int expected) {
        return assertInternalOperationExecutionCount(OP_CLOCKWORK_RUN, expected);
    }

    // Simple version until more elaborate asserter is created
    @SuppressWarnings("WeakerAccess")
    public TaskAsserter<RA> assertInternalOperationExecutionCount(String operation, int expected) {
        assertThat(getInternalOperationExecutionCount(operation))
                .as("operation '" + operation + "' exec count")
                .isEqualTo(expected);
        return this;
    }

    private int getInternalOperationExecutionCount(String operation) {
        OperationStatsType operationStats = getTaskBean().getOperationStats();
        if (operationStats == null) {
            return 0;
        }
        OperationsPerformanceInformationType opPerformanceInfo = operationStats.getOperationsPerformanceInformation();
        if (opPerformanceInfo == null) {
            return 0;
        }
        return opPerformanceInfo.getOperation().stream()
                .filter(op -> operation.equals(op.getName()))
                .mapToInt(op -> or0(op.getInvocationCount()))
                .sum();
    }

    public TaskAsserter<RA> assertNoAffectedObjects() {
        assertThat(getObjectable().getAffectedObjects()).as("affected objects").isNull();
        return this;
    }

    public TaskAsserter<RA> assertAffectedObjects(QName activityTypeName, QName objectType, @Nullable String archetypeOid) {
        ActivityAffectedObjectsType activityAffected = getSingleActivityAffectedObjects();
        assertThat(activityAffected.getResourceObjects()).as("resource objects").isNull();
        BasicObjectSetType objects = activityAffected.getObjects();
        assertThat(activityAffected.getActivityType()).as("activity").isEqualTo(activityTypeName);
        assertThat(objects).as("objects").isNotNull();
        assertThat(objects.getType()).as("objects type").isEqualTo(objectType);
        assertThat(Referencable.getOid(objects.getArchetypeRef()))
                .as("archetype OID")
                .isEqualTo(archetypeOid);
        return this;
    }

    private ActivityAffectedObjectsType getSingleActivityAffectedObjects() {
        var affected = getObjectable().getAffectedObjects();
        assertThat(affected).as("task-affected objects").isNotNull();
        assertThat(affected.getActivity()).as("activity-affected records").hasSize(1);
        return affected.getActivity().get(0);
    }

    public TaskAsserter<RA> assertAffectedObjects(
            QName activityTypeName, String resourceOid, ShadowKindType kind, String intent, QName objectClassName,
            ExecutionModeType executionMode, PredefinedConfigurationType predefinedConfiguration) {
        ActivityAffectedObjectsType activityAffected = getSingleActivityAffectedObjects();
        assertThat(activityAffected.getObjects()).as("objects").isNull();
        var resourceObjects = activityAffected.getResourceObjects();
        assertThat(activityAffected.getActivityType()).as("activity").isEqualTo(activityTypeName);
        assertThat(activityAffected.getExecutionMode()).as("executionMode").isEqualTo(executionMode);
        assertThat(activityAffected.getPredefinedConfigurationToUse())
                .as("predefined configuration")
                .isEqualTo(predefinedConfiguration);
        assertThat(Referencable.getOid(resourceObjects.getResourceRef()))
                .as("resource OID")
                .isEqualTo(resourceOid);
        assertThat(resourceObjects.getKind()).as("kind").isEqualTo(kind);
        assertThat(resourceObjects.getIntent()).as("intent").isEqualTo(intent);
        assertThat(resourceObjects.getObjectclass()).as("OC name").isEqualTo(objectClassName);
        return this;
    }

    public TaskAsserter<RA> assertAffectedObjects(TaskAffectedObjectsType expected) {
        var affected = getObjectable().getAffectedObjects();
        assertThat(affected).as("affected objects").isEqualTo(expected);
        return this;
    }
}
