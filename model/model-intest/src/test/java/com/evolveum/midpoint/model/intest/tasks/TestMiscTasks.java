/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests miscellaneous kinds of tasks that do not deserve their own test class.
 *
 * Besides that, checks the "affected objects" management when a task changes.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMiscTasks extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/misc");

    private static final TestObject<TaskType> TASK_DELETE_REPORT_DATA =
            TestObject.file(TEST_DIR, "task-delete-report-data.xml", "d3351dff-4c72-4985-8a9c-f8d46ffb328f");
    private static final TestObject<TaskType> TASK_DELETE_MISSING_QUERY_LEGACY =
            TestObject.file(TEST_DIR, "task-delete-missing-query.xml", "c637b877-efe8-43ae-b87f-738bff9062fb");
    private static final TestObject<TaskType> TASK_DELETE_MISSING_TYPE =
            TestObject.file(TEST_DIR, "task-delete-missing-type.xml", "889d1313-2a7f-4112-a996-2b84f1f000a7");
    private static final TestObject<TaskType> TASK_DELETE_INCOMPLETE_RAW =
            TestObject.file(TEST_DIR, "task-delete-incomplete-raw.xml", "d0053e62-9d48-4c1e-ace8-a8feb1f35f91");
    private static final TestObject<TaskType> TASK_DELETE_SELECTED_USERS =
            TestObject.file(TEST_DIR, "task-delete-selected-users.xml", "623f261c-4c63-445b-a714-dcde118f227c");
    private static final TestObject<TaskType> TASK_CLEANUP_SUBTASKS_AFTER_COMPLETION =
            TestObject.file(TEST_DIR, "task-cleanup-subtasks-after-completion.xml",
                    "7f5eb732-9ffc-42c3-a416-ee1754f1b764");
    private static final TestTask TASK_EXECUTE_CHANGES_LEGACY =
            new TestTask(TEST_DIR, "task-execute-changes.xml", "1dce894e-e76c-4db5-9318-0fa5b55261da");
    private static final TestTask TASK_EXECUTE_CHANGES_SINGLE =
            new TestTask(TEST_DIR, "task-execute-changes-single.xml", "300370ad-eb92-4b52-8db3-d5820e1366fa");
    private static final TestTask TASK_EXECUTE_CHANGES_MULTI =
            new TestTask(TEST_DIR, "task-execute-changes-multi.xml", "8427d4a9-f0cb-4771-ae51-c6979630068a");
    private static final TestTask TASK_ROLE_MEMBERSHIP_MANAGEMENT_BASIC =
            new TestTask(TEST_DIR, "task-role-membership-management-basic.xml", "07e8c51b-e5ae-496e-9b24-4f8701621c0d");
    private static final TestTask TASK_ROLE_ANALYSIS_CLUSTERING_BASIC =
            new TestTask(TEST_DIR, "task-role-analysis-clustering-basic.xml", "908e8dd3-66ba-4568-80aa-75c1a4e06b4a");
    private static final TestTask TASK_ROLE_ANALYSIS_PATTERN_DETECTION_BASIC =
            new TestTask(TEST_DIR, "task-role-analysis-pattern-detection-basic.xml", "a3429097-52c4-4474-80b7-fc1667584831");

    private static final TestObject<RoleAnalysisSessionType> SESSION_ROLE_BASED = TestObject.file(
            TEST_DIR, "session-role-based.xml", "c0d74ad2-f92a-40f0-b661-3c0a6a5dc225");
    private static final TestObject<RoleAnalysisClusterType> CLUSTER_ROLE_BASED = TestObject.file(
            TEST_DIR, "cluster-role-based.xml", "16a8b95a-8a37-4f91-b835-bb77670c2899");
    private static final TestObject<RoleType> ROLE_BUSINESS_1 = TestObject.file(
            TEST_DIR, "role-business-1.xml", "b48628a2-a032-47c2-947d-adc51940e920");
    private static final TestObject<RoleType> ROLE_APPLICATION_1 = TestObject.file(
            TEST_DIR, "role-application-1.xml", "875e287f-dc5b-49d0-a9db-b792947868db");
    private static final TestObject<RoleType> ROLE_APPLICATION_2 = TestObject.file(
            TEST_DIR, "role-application-2.xml", "345bf8c5-3be8-4783-8cfc-4b9ae18c1daf");
    private static final TestObject<UserType> USER_1 = TestObject.file(
            TEST_DIR, "user-1.xml", "f1c3fbc4-85d6-4559-9a1e-647e3caa25df");
    private static final TestObject<UserType> USER_2 = TestObject.file(
            TEST_DIR, "user-2.xml", "3fc7d5bb-d1a6-446b-925e-5680afaa9df6");

    private static final DummyTestResource RESOURCE_DUMMY_REFRESHED = new DummyTestResource(
            TEST_DIR, "resource-dummy-refreshed.xml", "e5eeccc7-a0bb-4961-9edf-a8693c6e9004", "refreshed");
    private static final TestTask TASK_SHADOW_REFRESH_ALL = new TestTask(
            TEST_DIR, "task-shadow-refresh-all.xml", "8c359fe6-a643-49b1-819c-c9f3351baae4");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                ROLE_APPLICATION_1, ROLE_APPLICATION_2, ROLE_BUSINESS_1,
                USER_1, USER_2);

        if (isNativeRepository()) {
            initTestObjects(initTask, initResult, SESSION_ROLE_BASED, CLUSTER_ROLE_BASED);
        }

        RESOURCE_DUMMY_REFRESHED.initAndTest(this, initTask, initResult);
        TASK_SHADOW_REFRESH_ALL.init(this, initTask, initResult);
    }

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

    /**
     * Tests if cleanup after task finish removes also subtasks, not just parent task
     *
     * Test for MID-10272
     */
    @Test
    public void test140CleanSubtasksAfterFinish() throws Exception {
        given("Noop task with more workers per node is added.");
        OperationResult result = getTestOperationResult();
        addTask(TASK_CLEANUP_SUBTASKS_AFTER_COMPLETION, result);
        waitForTaskCloseOrSuspend(TASK_CLEANUP_SUBTASKS_AFTER_COMPLETION.oid, 10000);

        Task taskTree = this.taskManager.getTaskTree(TASK_CLEANUP_SUBTASKS_AFTER_COMPLETION.oid, result);
        List<String> subtasksOids = taskTree.listSubtasksDeeply(true, result).stream()
                .map(Task::getOid)
                .toList();

        when("Task trigger scanner executes.");
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID);

        then("After task trigger scanner executes, Noop task should be deleted as well as all its worker "
                + "subtasks");
        // Just to make sure, that we have correct task definition which results in subtasks creation.
        assertThat(subtasksOids)
                .withFailMessage(() -> "There are no subtasks, but at least one is expected. Check task definition xml")
                .hasSizeGreaterThan(0);

        assertNoObject(TaskType.class, TASK_CLEANUP_SUBTASKS_AFTER_COMPLETION.oid);
        for (String subtaskOid : subtasksOids) {
            assertNoObject(TaskType.class, subtaskOid);
        }
    }

    /** Tests the legacy form of "execute changes" task. */
    @Test
    public void test150ExecuteChangesLegacy() throws CommonException {
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
    public void test160ExecuteChangesSingle() throws CommonException {
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

    /**
     * Manipulates the activity definition and checks whether affected objects are set appropriately.
     */
    @Test
    public void test200TestAffectedObjectsSimple() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("a task is created");

        ShadowKindType kind = ACCOUNT;
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
                        RESOURCE_DUMMY_OID, kind, intent, ocName1Qualified,
                        ExecutionModeType.FULL, PredefinedConfigurationType.PRODUCTION);

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

        when("task is changed to import one with development simulation mode");
        executeChanges(
                deltaFor(TaskType.class)
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK, WorkDefinitionsType.F_IMPORT)
                        .add(new ImportWorkDefinitionType()
                                .resourceObjects(new ResourceObjectSetType()
                                        .objectclass(ocName1Qualified)))
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION, ActivityExecutionModeDefinitionType.F_MODE)
                        .replace(ExecutionModeType.PREVIEW)
                        .item(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION,
                                ActivityExecutionModeDefinitionType.F_CONFIGURATION_TO_USE,
                                ConfigurationSpecificationType.F_PREDEFINED)
                        .replace(PredefinedConfigurationType.DEVELOPMENT)
                        .asObjectDelta(aTask.getOid()),
                null, task, result);

        then("affected objects are correct");
        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertAffectedObjects(
                        WorkDefinitionsType.F_IMPORT,
                        null, null, null, ocName1Qualified,
                        ExecutionModeType.PREVIEW, PredefinedConfigurationType.DEVELOPMENT);

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
                        null, null, null, ocName2Qualified,
                        ExecutionModeType.PREVIEW, PredefinedConfigurationType.DEVELOPMENT);

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

    /**
     * Checks affected objects management for composite activity.
     */
    @Test
    public void test210TestAffectedObjectsComposite() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("a task is created");

        ShadowKindType kind = ACCOUNT;
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
                                                                .objectclass(ocName1))))
                                        .execution(new ActivityExecutionModeDefinitionType()
                                                .mode(ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                                                .configurationToUse(new ConfigurationSpecificationType()
                                                        .predefined(PredefinedConfigurationType.DEVELOPMENT))))
                                .activity(new ActivityDefinitionType()
                                        .id(300L)
                                        .work(new WorkDefinitionsType()
                                                .recomputation(new RecomputationWorkDefinitionType()
                                                        .objects(new ObjectSetType()
                                                                .type(QNameUtil.unqualify(UserType.COMPLEX_TYPE)))))
                                        .executionMode(ExecutionModeType.PREVIEW) // legacy way
                                )
                        )
                );
        addObject(aTask.asPrismObject(), task, result);

        then("affected objects are correct");
        var expectedAffectedObjects = new TaskAffectedObjectsType()
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECONCILIATION)
                        .resourceObjects(new BasicResourceObjectSetType()
                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                .kind(kind)
                                .intent(intent))
                        .executionMode(ExecutionModeType.FULL)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.PRODUCTION)
                )
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_IMPORT)
                        .resourceObjects(new BasicResourceObjectSetType()
                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                .objectclass(ocName1Qualified))
                        .executionMode(ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.DEVELOPMENT)
                )
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .objects(new ObjectSetType()
                                .type(UserType.COMPLEX_TYPE))
                        .executionMode(ExecutionModeType.PREVIEW)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.PRODUCTION)
                );

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
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECONCILIATION)
                        .resourceObjects(new BasicResourceObjectSetType()
                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                .kind(kind)
                                .intent(intent))
                        .executionMode(ExecutionModeType.FULL)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.PRODUCTION)
                )
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_IMPORT)
                        .resourceObjects(new BasicResourceObjectSetType()
                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                .objectclass(ocName2Qualified))
                        .executionMode(ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.DEVELOPMENT)
                )
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .objects(new ObjectSetType()
                                .type(UserType.COMPLEX_TYPE))
                        .executionMode(ExecutionModeType.PREVIEW)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.PRODUCTION)
                )
                .activity(new ActivityAffectedObjectsType()
                        .activityType(WorkDefinitionsType.F_RECOMPUTATION)
                        .objects(new ObjectSetType()
                                .type(RoleType.COMPLEX_TYPE))
                        .executionMode(ExecutionModeType.FULL)
                        .predefinedConfigurationToUse(PredefinedConfigurationType.PRODUCTION)
                );

        assertTask(aTask.getOid(), "after modification")
                .display()
                .assertAffectedObjects(expectedAffectedObjects2);
    }

    /** Just a basic run of roleMembershipManagement task, migrating members from application roles to a business role. */
    @Test
    public void test300RoleMembershipManagementBasic() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        TASK_ROLE_MEMBERSHIP_MANAGEMENT_BASIC.init(this, task, result);
        TASK_ROLE_MEMBERSHIP_MANAGEMENT_BASIC.rerun(result); // asserts success

        then("users have business role but no application roles");
        assertBusinessRoleAndNoApplicationRoles(USER_1.oid);
        assertBusinessRoleAndNoApplicationRoles(USER_2.oid);

        and("task is OK");
        TASK_ROLE_MEMBERSHIP_MANAGEMENT_BASIC.assertAfter()
                .display()
                .assertProgress(2)
                .rootActivityState()
                .itemProcessingStatistics()
                .assertTotalCounts(2, 0, 0);
    }

    private void assertBusinessRoleAndNoApplicationRoles(String oid) throws CommonException {
        // @formatter:off
        assertUserAfter(oid)
                .assignments()
                    .single()
                        .assertRole(ROLE_BUSINESS_1.oid)
                    .end()
                .end()
                .roleMembershipRefs()
                    .assertRole(ROLE_BUSINESS_1.oid)
                    .assertRole(ROLE_APPLICATION_1.oid)
                    .assertRole(ROLE_APPLICATION_2.oid);
        // @formatter:on
    }

    @Test
    public void test400RoleAnalysisClusteringBasic() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("task is run");
        TASK_ROLE_ANALYSIS_CLUSTERING_BASIC.init(this, task, result);
        TASK_ROLE_ANALYSIS_CLUSTERING_BASIC.rerun(result); // asserts success

        then("task is OK");
        TASK_ROLE_ANALYSIS_CLUSTERING_BASIC.assertAfter()
                .display()
                .assertProgress(7);
    }

    @Test
    public void test410RoleAnalysisPatternDetectionBasic() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        initTestObjects(task, result, CLUSTER_ROLE_BASED);

        when("task is run");
        TASK_ROLE_ANALYSIS_PATTERN_DETECTION_BASIC.init(this, task, result);
        TASK_ROLE_ANALYSIS_PATTERN_DETECTION_BASIC.rerun(result); // asserts success

        then("task is OK");
        TASK_ROLE_ANALYSIS_PATTERN_DETECTION_BASIC.assertAfter()
                .display()
                .assertProgress(6);
    }

    /**
     * Here we check that the shadow refresh task really refreshes the cached password.
     *
     * it uses the simplest scenario: cached -> not cached.
     * Other relevant transitions are tested in `TestDummyPasswordCaching` in `provisioning-impl`.
     */
    @Test
    public void test500RefreshCachedPasswordWithChangedCaching() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();
        var password = "AbcABC1234";

        given("a user with a shadow with cached password");
        var user = new UserType()
                .name(userName)
                .assignment(RESOURCE_DUMMY_REFRESHED.assignmentTo(ACCOUNT, INTENT_DEFAULT))
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .value(new ProtectedStringType().clearValue(password))));
        var userOid = addObject(user, task, result);
        var shadowAtStart = assertUser(userOid, "at start")
                .singleLink()
                .resolveTarget()
                .getObject();
        assertEncryptedShadowPassword(shadowAtStart, password);

        when("caching is turned off and the shadow is refreshed");
        executeChanges(
                deltaFor(ResourceType.class)
                        .item(ResourceType.F_CACHING, CachingPolicyType.F_CACHING_STRATEGY)
                        .replace(CachingStrategyType.NONE)
                        .asObjectDelta(RESOURCE_DUMMY_REFRESHED.oid),
                null, task, result);

        TASK_SHADOW_REFRESH_ALL.rerun(result);

        then("there is no cached password in the shadow any more");
        assertUser(userOid, "at end")
                .singleLink()
                .resolveTarget()
                .assertNoPassword();
    }
}
