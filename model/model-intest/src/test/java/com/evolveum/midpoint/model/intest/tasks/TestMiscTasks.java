/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;

import javax.xml.namespace.QName;

/**
 * Tests miscellaneous kinds of tasks that do not deserve their own test class.
 *
 * Besides that, checks the "affected objects" management when a task changes.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMiscTasks extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/misc");

    private static final TestResource<TaskType> TASK_DELETE_REPORT_DATA =
            new TestResource<>(TEST_DIR, "task-delete-report-data.xml", "d3351dff-4c72-4985-8a9c-f8d46ffb328f");
    private static final TestResource<TaskType> TASK_DELETE_MISSING_QUERY_LEGACY =
            new TestResource<>(TEST_DIR, "task-delete-missing-query.xml", "c637b877-efe8-43ae-b87f-738bff9062fb");
    private static final TestResource<TaskType> TASK_DELETE_MISSING_TYPE =
            new TestResource<>(TEST_DIR, "task-delete-missing-type.xml", "889d1313-2a7f-4112-a996-2b84f1f000a7");
    private static final TestResource<TaskType> TASK_DELETE_INCOMPLETE_RAW =
            new TestResource<>(TEST_DIR, "task-delete-incomplete-raw.xml", "d0053e62-9d48-4c1e-ace8-a8feb1f35f91");
    private static final TestResource<TaskType> TASK_DELETE_SELECTED_USERS =
            new TestResource<>(TEST_DIR, "task-delete-selected-users.xml", "623f261c-4c63-445b-a714-dcde118f227c");
    private static final TestTask TASK_EXECUTE_CHANGES_LEGACY =
            new TestTask(TEST_DIR, "task-execute-changes.xml", "1dce894e-e76c-4db5-9318-0fa5b55261da");
    private static final TestTask TASK_EXECUTE_CHANGES_SINGLE =
            new TestTask(TEST_DIR, "task-execute-changes-single.xml", "300370ad-eb92-4b52-8db3-d5820e1366fa");
    private static final TestTask TASK_EXECUTE_CHANGES_MULTI =
            new TestTask(TEST_DIR, "task-execute-changes-multi.xml", "8427d4a9-f0cb-4771-ae51-c6979630068a");

    /**
     * MID-7277
     */
    @Test
    public void test100DeleteReportData() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ReportDataType reportData = new ReportDataType()
                .name("waste data");
        repoAddObject(reportData.asPrismObject(), result);

        when();

        addTask(TASK_DELETE_REPORT_DATA, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_REPORT_DATA.oid, 10000);

        then();

        TaskType taskAfter = assertTask(TASK_DELETE_REPORT_DATA.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(1)
                .getObjectable();

        TaskInformation information = TaskInformation.createForTask(taskAfter, taskAfter);
        assertThat(information).isInstanceOf(ActivityBasedTaskInformation.class);
        assertThat(information.getProgressDescriptionShort()).as("progress description").isEqualTo("100.0%");

        assertNoRepoObject(ReportDataType.class, reportData.getOid());
    }

    /**
     * MID-7277
     */
    @Test
    public void test110DeleteReportDataAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        suspendAndDeleteTasks(TASK_DELETE_REPORT_DATA.oid);
        addTask(TASK_DELETE_REPORT_DATA, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_REPORT_DATA.oid, 10000);

        then();

        TaskType taskAfter = assertTask(TASK_DELETE_REPORT_DATA.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(0)
                .getObjectable();

        TaskInformation information = TaskInformation.createForTask(taskAfter, taskAfter);
        assertThat(information).isInstanceOf(ActivityBasedTaskInformation.class);
        assertThat(information.getProgressDescriptionShort()).as("progress description").isEqualTo("0");
    }

    /**
     * Checks that deletion activity does not allow running without type and/or query.
     */
    @Test
    public void test120DeleteWithoutQueryOrType() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("no query");
        addTask(TASK_DELETE_MISSING_QUERY_LEGACY, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_MISSING_QUERY_LEGACY.oid, 10000);

        then("no query");
        assertTask(TASK_DELETE_MISSING_QUERY_LEGACY.oid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Object query must be specified");

        when("no type");
        addTask(TASK_DELETE_MISSING_TYPE, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_MISSING_TYPE.oid, 10000);

        then("no type");
        assertTask(TASK_DELETE_MISSING_TYPE.oid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Object type must be specified");
    }

    /**
     * Checks that deletion activity really requires none or both "raw" flags being set.
     */
    @Test
    public void test125DeleteWithIncompleteRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addTask(TASK_DELETE_INCOMPLETE_RAW, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_INCOMPLETE_RAW.oid, 10000);

        then("no query");
        assertTask(TASK_DELETE_INCOMPLETE_RAW.oid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Neither both search and execution raw mode should be defined, or none");
    }

    /**
     * Checks that deletion activity really deletes something, and avoids deleting indestructible objects.
     *
     * - user-to-delete-1-normal will be deleted,
     * - user-to-delete-2-indestructible will be kept.
     */
    @Test
    public void test130DeleteObjects() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user1 = new UserType()
                .name("user-to-delete-1-normal");
        repoAddObject(user1.asPrismObject(), result);

        UserType user2 = new UserType()
                .name("user-to-delete-2-indestructible")
                .indestructible(true);
        repoAddObject(user2.asPrismObject(), result);

        when();
        addTask(TASK_DELETE_SELECTED_USERS, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_SELECTED_USERS.oid, 10000);

        then();
        // @formatter:off
        assertTask(TASK_DELETE_SELECTED_USERS.oid, "after")
                .display()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 1)
                    .end()
                    .itemProcessingStatistics()
                        .assertTotalCounts(1, 0, 1);
        // @formatter:on

        assertNoRepoObject(UserType.class, user1.getOid());
        assertUser(user2.getOid(), "after")
                .display()
                .assertName("user-to-delete-2-indestructible");
    }

    /** Tests the legacy form of "execute changes" task. */
    @Test
    public void test150ExecuteChangesLegacy() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        TASK_EXECUTE_CHANGES_LEGACY.init(this, task, result);
        TASK_EXECUTE_CHANGES_LEGACY.rerun(result);

        then("user is created in raw mode");
        assertUserAfterByUsername("user-legacy")
                .assertAssignments(1)
                .assertRoleMembershipRefs(0); // to check the raw mode

        and("task is OK");
        // @formatter:off
        TASK_EXECUTE_CHANGES_LEGACY.assertAfter()
                .assertClosed()
                .assertSuccess()
                .assertProgress(1)
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(1, 0, 0);
        // @formatter:on
    }

    /** Tests the "single-request" form of "execute changes" task. */
    @Test
    public void test160ExecuteChangesSingle() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        TASK_EXECUTE_CHANGES_SINGLE.init(this, task, result);
        TASK_EXECUTE_CHANGES_SINGLE.rerun(result);

        then("user is created in raw mode");
        assertUserAfterByUsername("user-single")
                .assertAssignments(1)
                .assertRoleMembershipRefs(0); // to check the raw mode

        and("task is OK");
        // @formatter:off
        TASK_EXECUTE_CHANGES_SINGLE.assertAfter()
                .assertClosed()
                .assertSuccess()
                .assertProgress(1)
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(1, 0, 0)
                        .assertLastSuccessObjectName("#1");
        // @formatter:on
    }

    /** Tests the "multiple requests" form of "execute changes" task. */
    @Test
    public void test170ExecuteChangesMulti() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        TASK_EXECUTE_CHANGES_MULTI.init(this, task, result);
        TASK_EXECUTE_CHANGES_MULTI.rerun(result);

        then("user 1 is created in raw mode");
        assertUserAfterByUsername("user-multi-1")
                .assertAssignments(1)
                .assertRoleMembershipRefs(0); // to check the raw mode

        and("user 2 is created in non-raw mode");
        assertUserAfterByUsername("user-multi-2")
                .assertAssignments(1)
                .assertRoleMembershipRefs(1);

        and("task is OK");
        // @formatter:off
        TASK_EXECUTE_CHANGES_MULTI.assertAfter()
                .assertClosed()
                .assertSuccess()
                .assertProgress(3)
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(2, 0, 1)
                        .assertLastSuccessObjectName("multi-2");
        // @formatter:on
    }

    /** Manipulates the activity definition and checks whether affected objects are set appropriately. */
    @Test
    public void test200TestAffectedObjectsSimple() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("a task is created");

        ShadowKindType kind = ShadowKindType.ACCOUNT;
        String intent = "funny";
        QName ocName1 = new QName("oc1");
        QName ocName1Qualified = QNameUtil.qualifyIfNeeded(ocName1, NS_RI);
        QName ocName2 = new QName("oc2");
        QName ocName2Qualified = QNameUtil.qualifyIfNeeded(ocName2, NS_RI);

        TaskType aTask = new TaskType()
                .name(getTestNameShort())
                .executionState(TaskExecutionStateType.CLOSED)
                .activity(new ActivityDefinitionType()
                        .work(new WorkDefinitionsType()
                                .reconciliation(new ReconciliationWorkDefinitionType()
                                        .resourceObjects(new ResourceObjectSetType()
                                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                                .kind(kind)
                                                .intent(intent)
                                                .objectclass(ocName1)))));
        addObject(aTask.asPrismObject(), task, result);

        then("affected objects are correct");
        assertTask(aTask.getOid(), "after creation")
                .display()
                .assertAffectedObjects(
                        WorkDefinitionsType.F_RECONCILIATION,
                        RESOURCE_DUMMY_OID, kind, intent, ocName1Qualified);

        when("task definition is removed");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY)
                        .replace()
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        then("affected objects are empty");
        assertTask(aTask.getOid(), "after removal")
                .display()
                .assertNoAffectedObjects();

        when("task is changed to import one");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK, WorkDefinitionsType.F_IMPORT)
                        .add(new ImportWorkDefinitionType()
                                .resourceObjects(new ResourceObjectSetType()
                                        .objectclass(ocName1Qualified)))
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        then("affected objects are correct");
        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertAffectedObjects(
                        WorkDefinitionsType.F_IMPORT,
                        null, null, null, ocName1Qualified);

        when("object class is changed");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY,
                                ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_IMPORT,
                                ImportWorkDefinitionType.F_RESOURCE_OBJECTS,
                                ResourceObjectSetType.F_OBJECTCLASS)
                        .replace(ocName2)
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        then("affected objects are correct");
        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertAffectedObjects(
                        WorkDefinitionsType.F_IMPORT,
                        null, null, null, ocName2Qualified);

        when("the definition is made invalid by addition of conflicting work definition");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY,
                                ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_RECONCILIATION)
                        .add(new ReconciliationWorkDefinitionType()
                                .resourceObjects(new ResourceObjectSetType()))
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        then("affected objects are empty");
        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertNoAffectedObjects();

        and("the operation result is a warning");
        assertWarning(result);
        assertThatOperationResult(result)
                .isWarning()
                .hasMessageContaining("Couldn't compute affected objects");
    }

    /** Checks affected objects management for composite activity. */
    @Test
    public void test200TestAffectedObjectsComposite() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("a task is created");

        ShadowKindType kind = ShadowKindType.ACCOUNT;
        String intent = "funny";
        QName ocName1 = new QName("oc1");
        QName ocName1Qualified = QNameUtil.qualifyIfNeeded(ocName1, NS_RI);
        QName ocName2 = new QName("oc2");
        QName ocName2Qualified = QNameUtil.qualifyIfNeeded(ocName2, NS_RI);

        TaskType aTask = new TaskType()
                .name(getTestNameShort())
                .executionState(TaskExecutionStateType.CLOSED)
                .activity(new ActivityDefinitionType()
                        .composition(new ActivityCompositionType()
                                .activity(new ActivityDefinitionType()
                                        .id(100L)
                                        .work(new WorkDefinitionsType()
                                                .reconciliation(new ReconciliationWorkDefinitionType()
                                                        .resourceObjects(new ResourceObjectSetType()
                                                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                                                .kind(kind)
                                                                .intent(intent)))))
                                .activity(new ActivityDefinitionType()
                                        .id(200L)
                                        .work(new WorkDefinitionsType()
                                                ._import(new ImportWorkDefinitionType()
                                                        .resourceObjects(new ResourceObjectSetType()
                                                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                                                .objectclass(ocName1)))))
                                .activity(new ActivityDefinitionType()
                                        .id(300L)
                                        .work(new WorkDefinitionsType()
                                                .recomputation(new RecomputationWorkDefinitionType()
                                                        .objects(new ObjectSetType()
                                                                .type(QNameUtil.unqualify(UserType.COMPLEX_TYPE))))))));
        addObject(aTask.asPrismObject(), task, result);

        then("affected objects are correct");
        var expectedAffectedObjects = new TaskAffectedObjectsType()
                .resourceObjects(new ActivityAffectedResourceObjectsType()
                        .activityType(WorkDefinitionsType.F_RECONCILIATION)
                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                        .kind(kind)
                        .intent(intent))
                .resourceObjects(new ActivityAffectedResourceObjectsType()
                        .activityType(WorkDefinitionsType.F_IMPORT)
                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                        .objectclass(ocName1Qualified))
                .objects(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .type(UserType.COMPLEX_TYPE));

        assertTask(aTask.getOid(), "after creation")
                .display()
                .assertAffectedObjects(expectedAffectedObjects);

        when("task definition is changed");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_COMPOSITION, ActivityCompositionType.F_ACTIVITY)
                        .add(new ActivityDefinitionType()
                                .id(400L)
                                .work(new WorkDefinitionsType()
                                        .recomputation(new RecomputationWorkDefinitionType()
                                                .objects(new ObjectSetType()
                                                        .type(QNameUtil.unqualify(RoleType.COMPLEX_TYPE))))))
                        .item(TaskType.F_ACTIVITY,
                                ActivityDefinitionType.F_COMPOSITION,
                                ActivityCompositionType.F_ACTIVITY, 200L,
                                ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_IMPORT,
                                ImportWorkDefinitionType.F_RESOURCE_OBJECTS,
                                ResourceObjectSetType.F_OBJECTCLASS)
                        .replace(ocName2)
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        var expectedAffectedObjects2 = new TaskAffectedObjectsType()
                .resourceObjects(new ActivityAffectedResourceObjectsType()
                        .activityType(WorkDefinitionsType.F_RECONCILIATION)
                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                        .kind(kind)
                        .intent(intent))
                .resourceObjects(new ActivityAffectedResourceObjectsType()
                        .activityType(WorkDefinitionsType.F_IMPORT)
                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                        .objectclass(ocName2Qualified))
                .objects(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .type(UserType.COMPLEX_TYPE))
                .objects(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .type(RoleType.COMPLEX_TYPE));

        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertAffectedObjects(expectedAffectedObjects2);
    }
}
