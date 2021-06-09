/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getExtensionItemRealValue;

import static com.evolveum.midpoint.util.MiscUtil.assertCheck;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

public class TaskAsserter<RA> extends AssignmentHolderAsserter<TaskType, RA> {

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
        assertPropertyEquals(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN), expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        return (TaskAsserter<RA>) super.assertPolyStringProperty(propName, expectedOrig);
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> synchronizationInformation() {
        OperationStatsType operationStats = getObject().asObjectable().getOperationStats();
        SynchronizationInformationType information = operationStats != null ?
                operationStats.getSynchronizationInformation() : new SynchronizationInformationType();
        SynchronizationInfoAsserter<TaskAsserter<RA>> asserter = new SynchronizationInfoAsserter<>(information, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public IterationInformationAsserter<TaskAsserter<RA>> iterativeTaskInformation() {
        OperationStatsType operationStats = getObject().asObjectable().getOperationStats();
        ActivityIterationInformationType information = operationStats != null ?
                operationStats.getIterationInformation() : new ActivityIterationInformationType();
        IterationInformationAsserter<TaskAsserter<RA>> asserter = new IterationInformationAsserter<>(information, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskActivityStateAsserter<TaskAsserter<RA>> activityState() {
        TaskActivityStateType activityState = getObject().asObjectable().getActivityState();
        TaskActivityStateAsserter<TaskAsserter<RA>> asserter = new TaskActivityStateAsserter<>(activityState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public StructuredTaskProgressAsserter<TaskAsserter<RA>> structuredProgress() {
        throw new UnsupportedOperationException();
//        StructuredTaskProgressType progress = getObject().asObjectable().getStructuredProgress();
//        if (progress == null) {
//            progress = new StructuredTaskProgressType(getPrismContext());
//        }
//        StructuredTaskProgressAsserter<TaskAsserter<RA>> asserter = new StructuredTaskProgressAsserter<>(progress, this, getDetails());
//        copySetupTo(asserter);
//        return asserter;
    }

    public ActionsExecutedInfoAsserter<TaskAsserter<RA>> actionsExecutedInformation() {
        OperationStatsType operationStats = getObject().asObjectable().getOperationStats();
        ActionsExecutedInformationType information = operationStats != null ?
                operationStats.getActionsExecutedInformation() : new ActionsExecutedInformationType();
        ActionsExecutedInfoAsserter<TaskAsserter<RA>> asserter = new ActionsExecutedInfoAsserter<>(information, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertClosed() {
        return assertExecutionStatus(TaskExecutionStateType.CLOSED);
    }

    public TaskAsserter<RA> assertExecutionStatus(TaskExecutionStateType status) {
        assertEquals("Wrong execution status", status, getTaskBean().getExecutionStatus());
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

    public TaskAsserter<RA> assertFatalError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertFatalError(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.FATAL_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertCategory(String category) {
        assertEquals(category, getTaskBean().getCategory());
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

    public TaskAsserter<TaskAsserter<RA>> subtaskForPart(int number) {
        TaskType subtask = TaskTreeUtil.getAllTasksStream(getObjectable())
                .filter(t -> Integer.valueOf(number).equals(ActivityStateUtil.getPartitionSequentialNumber(t)))
                .findAny().orElse(null);
        assertThat(subtask).withFailMessage(() -> "No subtask for part " + number + " found").isNotNull();

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this, "subtask for part " +
                number + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<TaskAsserter<RA>> subtaskForPath(ActivityPath activityPath) {
        TaskType subtask = TaskTreeUtil.getAllTasksStream(getObjectable())
                .filter(t -> activityPath.equalsBean(ActivityStateUtil.getLocalRootPathBean(t.getActivityState())))
                .findAny().orElse(null);
        assertThat(subtask).withFailMessage(() -> "No subtask for activity path '" + activityPath + "' found").isNotNull();

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this, "subtask for path '" +
                activityPath + "' in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<TaskAsserter<RA>> subtask(int index) {
        List<ObjectReferenceType> subtasks = getObjectable().getSubtaskRef();
        assertCheck(subtasks.size() > index, "Expected to see at least %s subtask(s), but only %s are present",
                index + 1, subtasks.size());

        ObjectReferenceType subtaskRef = subtasks.get(index);
        TaskType subtask = (TaskType) ObjectTypeUtil.getObjectFromReference(subtaskRef);
        assertThat(subtask).withFailMessage(() -> "No subtask #" + index + " found").isNotNull();

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this,
                "subtask #" + index + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertLastScanTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        XMLGregorianCalendar lastScanTime =
                getExtensionItemRealValue(getObject(), SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        TestUtil.assertBetween("last scan timestamp in " + desc(), start, end, lastScanTime);
        return this;
    }
}
