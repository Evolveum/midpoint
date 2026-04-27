/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * TODO [viliam]
 *  - lots of assertions are copied, will be same/simliar to other activity/focus policy tests
 *  - policies copied from roles to proper place in policy, figure out how to do it without creating almost the same files over and over again
 */
public class TestTask000FocusPolicyInActivity extends TestThresholds {

    private static final Trace LOGGER = TraceManager.getTrace(TestTask000FocusPolicyInActivity.class);

    private static final long TIMEOUT = 60000;

    private static final int WORKER_THREADS = 1;

    private static final TestObject<RoleType> ROLE_ADD_10_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-add-10-notification.xml", "79d1d4e7-408c-4255-9386-5697892691df");

    private static final TestObject<RoleType> ROLE_MODIFY_5_COST_CENTER_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-cost-center-5-notification.xml", "719eb6bc-6a89-4baf-a125-2482b9502ad1");

    private static final TestObject<RoleType> ROLE_MODIFY_5_FULL_NAME_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-modify-full-name-5-notification.xml", "6b76ce6f-0ca0-4e86-9209-8d734851348d");

    private static final TestObject<RoleType> ROLE_DELETE_5_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-delete-5-notification.xml", "552d2465-6531-4742-8626-7c9559ec8ff7");

    private static final TestTask TASK_IMPORT =
            TestTask.file(TEST_DIR, "task-000-import.xml", "385b2498-bf8b-4e31-a807-71c312cc2e29");

    private static final TestTask TASK_IMPORT_SIMULATE =
            TestTask.file(TEST_DIR, "task-000-import-simulate.xml", "4ba70403-424c-410d-a3b0-e3b7719063dc");

    private static final TestTask TASK_IMPORT_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-000-import-simulate-execute.xml", "c865c889-238b-47f2-b05b-445d1c21259f");

    private static final TestTask TASK_RECONCILIATION =
            TestTask.file(TEST_DIR, "task-000-reconciliation.xml", "385b2498-bf8b-4e31-a807-71c312cc2e29");

    private static final TestTask TASK_RECONCILIATION_SIMULATE =
            TestTask.file(TEST_DIR, "task-000-reconciliation-simulate.xml", "7534f9eb-6139-4db5-a1d5-8e699a057e8a");

    private static final TestTask TASK_RECONCILIATION_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-000-reconciliation-simulate-execute.xml", "53734bf9-7068-4ee6-8804-2be3f4fe31ee");

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "dummy:policyRuleNotifier";

    String ruleAddNotificationId;
    String ruleModifyCostCenterNotificationId;
    String ruleModifyFullNameNotificationId;
    String ruleDeleteNotificationId;

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        SimpleActivityPolicyRuleNotifierType activityPolicyNotifier = new SimpleActivityPolicyRuleNotifierType();
        activityPolicyNotifier.getTransport().add("dummy:activityPolicyNotifier");

        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add("dummy:policyNotifier");

        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(activityPolicyNotifier);
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);

        repoAdd(ROLE_ADD_10_NOTIFICATION, initResult);
        ruleAddNotificationId = determineSingleInducedRuleId(ROLE_ADD_10_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION, initResult);
        ruleModifyCostCenterNotificationId = determineSingleInducedRuleId(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION, initResult);
        ruleModifyFullNameNotificationId = determineSingleInducedRuleId(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION.oid, initResult);

        repoAdd(ROLE_DELETE_5_NOTIFICATION, initResult);
        ruleDeleteNotificationId = determineSingleInducedRuleId(ROLE_DELETE_5_NOTIFICATION.oid, initResult);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentImportAddCustomizer() {
        return roleAssignmentCustomizer(ROLE_ADD_10_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyCostCenter() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyFullName() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyFullName5SimulateExecute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconModifyFullName5SimulateExecute() {
        return task ->
                transplantRolePolicy(
                        ROLE_MODIFY_5_FULL_NAME_NOTIFICATION,
                        new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                        task,
                        ActivityPath.fromId("second")
                );
    }

    /**
     * Transplant for simulate OR execute task. If task have simulate-execute,
     * please use {@link #transplantRolePolicyForSimulateExecuteTask(TestObject)}.
     */
    private Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateOrExecuteTask(TestObject<RoleType> source) {
        return task ->
                transplantRolePolicy(
                        source,
                        new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                        task,
                        ActivityPath.empty());
    }

    /**
     * Transplant role policy for simulate-execute task. If task has simulate OR execute ONLY
     * please use {@link #transplantRolePolicyForSimulateOrExecuteTask(TestObject)}.
     */
    private Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateExecuteTask(TestObject<RoleType> source) {
        return task -> {
            transplantRolePolicy(
                    source,
                    new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                    task,
                    ActivityPath.fromId("simulate"));

            transplantRolePolicy(
                    source,
                    new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                    task,
                    ActivityPath.fromId("execute"));
        };
    }

    private void transplantRolePolicy(
            TestObject<RoleType> source,
            PolicyActionsType actionOverrides,
            PrismObject<TaskType> target,
            ActivityPath activityPath) {

        int count = 0;

        List<PolicyRuleType> rules = source.getObjectable().getInducement().stream()
                .filter(i -> Objects.equals(2, i.getOrder()))
                .filter(inducement -> inducement.getPolicyRule() != null)
                .map(AssignmentType::getPolicyRule)
                .toList();

        ActivityDefinitionType def = target.asObjectable().getActivity();
        ActivityDefinitionType myDef = ActivityDefinitionUtil.findActivityDefinition(def, activityPath);
        Assertions.assertThat(myDef)
                .withFailMessage("No activity definition found for path %s in task %s", activityPath, target.getOid())
                .isNotNull();

        for (PolicyRuleType rule : rules) {
            PolicyRuleType newRule = rule.clone();
            if (actionOverrides != null) {
                newRule.setPolicyActions(actionOverrides);
            }

            ActivityPoliciesType policies = myDef.getPolicies();
            if (policies == null) {
                policies = new ActivityPoliciesType();
                myDef.setPolicies(policies);
            }

            policies.getPolicy().add(newRule);

            count++;
        }

        LOGGER.info("Transplanted {} policy rules from {} to {}, {}", count, source, target, activityPath);
    }

    @Override
    int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    long getTimeout() {
        return TIMEOUT;
    }

    @Override
    TestObject<TaskType> getSimulateTask() {
        return TASK_IMPORT_SIMULATE;
    }

    @Override
    TestObject<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SIMULATE_EXECUTE;
    }

    @Override
    TestObject<TaskType> getExecuteTask() {
        return TASK_IMPORT;
    }

    @Override
    TestObject<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SIMULATE;
    }

    @Override
    TestObject<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SIMULATE_EXECUTE;
    }

    @Override
    TestObject<TaskType> getReconciliationExecuteTask() {
        return TASK_RECONCILIATION;
    }

    @Override
    void assertTest100Task(TestObject<TaskType> importTask) throws Exception {
        // todo assert notifications

        PrismObject<TaskType> task = getTask(importTask.oid);
        var suspendPolicyIdentifier = ActivityPolicyUtils.buildPolicyIdentifier(task, ActivityPath.empty(), "add-10");

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounterMinMax(ruleAddNotificationId, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .assertCounterMinMax(suspendPolicyIdentifier, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .assertCounterCount(2)
                .end()
                .progress()
                    .display()
                    .end()
                .itemProcessingStatistics()
                    .display()
                    .end()
            .progress()
                .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                .end()
                .itemProcessingStatistics()
                .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
            .end();
        // @formatter:on
    }

    @Override
    void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws Exception {
        // todo assert notifications

        PrismObject<TaskType> task = getTask(importTask.oid);
        var suspendPolicyIdentifier = ActivityPolicyUtils.buildPolicyIdentifier(task, ActivityPath.empty(), "add-10");

        // @formatter:off
        assertTaskTree(importTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display()
                        .end()
                    .progress()
                        .display()
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .end()
                    .previewModePolicyRulesCounters()
                        .assertCounter(ruleAddNotificationId, USER_ADD_ALLOWED + 2 )
                        .assertCounter(suspendPolicyIdentifier, USER_ADD_ALLOWED + 2)
                        .assertCounterCount(2)
                        .end()
                    .progress()
                        .assertUncommitted(0, 1, 0) // fails immediately because of persistent counters
                        .end()
                    .itemProcessingStatistics()
                        .assertTotalCounts(USER_ADD_ALLOWED, 2, 0)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest110TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .end()
                .activityState(SIMULATE)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_ADD_ALLOWED, true)
                        .assertFailureCount(1, getThreads(), true)
                        .end()
                    .end()
                .activityState(EXECUTE)
                    .display()
                    .assertRealizationState(null) // this should not even start
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest120TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_ADD_ALLOWED, true)
                        .assertFailureCount(1, getThreads(), true)
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, USER_ADD_ALLOWED, 0)
                            .end()
                        .end();
        // @formatter:on
    }

    @Override
    void assertTest200TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        dumpSimplifiedTaskActivityState(importTask);

        // @formatter:off
        var asserter = assertTaskTree(importTask.oid, "task after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                .display()
                .assertCounterMinMax(ruleModifyCostCenterNotificationId, USER_MODIFY_ALLOWED + 1, USER_MODIFY_ALLOWED + getThreads())
                .end()
                .itemProcessingStatistics().display().end()
                .progress()
                .display()
                .assertSuccessCount(USER_MODIFY_ALLOWED, ACCOUNTS, true) // this is quite a broad range :)
                .assertFailureCount(1, getThreads(), true)
                .end();
        asserter
                .itemProcessingStatistics()
                .assertTotalCounts(USER_MODIFY_ALLOWED*4, 1, 0)
                .end();
        // @formatter:on
    }

    @Override
    void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        var asserter = assertTaskTree(importTask.oid, "task after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .previewModePolicyRulesCounters().display().end()
                .itemProcessingStatistics().display().end();
        asserter
                .previewModePolicyRulesCounters()
                .assertCounter(ruleModifyCostCenterNotificationId, USER_MODIFY_ALLOWED + 2)
                .end()
                .itemProcessingStatistics()
                .assertTotalCounts(USER_MODIFY_ALLOWED*4, 2, 0)
                .end();
        // @formatter:on
    }

    @Override
    void assertTest210TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .assertInProgressLocal()
                .assertFatalError()
                .end()
                .activityState(SIMULATE)
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                .display()
                .assertFailureCount(1, getThreads(), true)
                .end()
                .end()
                .activityState(EXECUTE)
                .display()
                .assertRealizationState(null) // this should not even start
                .end();
        // @formatter:on
    }

    @Override
    void assertTest220TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                .display()
                .assertFailureCount(1, getThreads(), true)
                .end();
        // @formatter:on
    }

    @Override
    void assertTest300TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        dumpSimplifiedTaskActivityState(importTask);

// @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertClosed()
                .assertSuccess()
                .activityState(SIMULATE)
                .assertComplete()
                .assertSuccess()
                .previewModePolicyRulesCounters()
                .display()
                .assertCounter(ruleModifyFullNameNotificationId, 4)
                .end()
                .itemProcessingStatistics()
                .display()
                .assertTotalCounts(ACCOUNTS, 0, 0)
                .end()
                .end()
                .activityState(EXECUTE)
                .assertComplete()
                .assertSuccess()
                .fullExecutionModePolicyRulesCounters()
                .display()
                .assertCounter(ruleModifyFullNameNotificationId, 4)
                .end()
                .itemProcessingStatistics()
                .display()
                .assertTotalCounts(ACCOUNTS, 0, 0)
                .end()
                .end();
        // @formatter:on
    }

    @Override
    void assertTest310TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(reconTask.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .previewModePolicyRulesCounters()
                        .display()
                        .assertCounter(ruleModifyFullNameNotificationId, 4)
                        .end()
                    .fullExecutionModePolicyRulesCounters()
                        .display()
                        .assertCounter(ruleModifyFullNameNotificationId, 4)
                        .end()
                    .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PREVIEW_ID)
                        .assertComplete()
                        .assertSuccess()
                        .itemProcessingStatistics()
                            .display()
                            .assertTotalCounts(ACCOUNTS, 0, 0)
                            .end()
                        .end()
                    .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                        .assertComplete()
                        .assertSuccess()
                        .itemProcessingStatistics()
                            .display()
                            .assertTotalCounts(ACCOUNTS, 0, 0)
                            .end()
                        .end()
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest400TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        assertTaskTree(reconTask.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                .display()
                .assertCounterMinMax(ruleDeleteNotificationId, USER_DELETE_ALLOWED + 1, USER_DELETE_ALLOWED + getThreads())
                .end();
        // @formatter:on
    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        var asserter = assertTaskTree(reconTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .previewModePolicyRulesCounters().display().end();
        asserter
                .previewModePolicyRulesCounters()
                .display()
                .assertCounter(ruleDeleteNotificationId, USER_DELETE_ALLOWED + 2)
                .end();
        // @formatter:on
    }

    @Override
    void assertTest410TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        assertTaskTree(importTask.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .assertInProgressLocal()
                .assertFatalError()
                .previewModePolicyRulesCounters()
                .display()
                .end()
                .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PREVIEW_ID)
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                .display()
                .assertSuccessCount(USER_DELETE_ALLOWED, true)
                .assertFailureCount(1, getThreads(), true)
                .end()
                .end()
                .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                .display()
                .assertRealizationState(null) // this should not even start
                .end()
                .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID)
                .display()
                .assertRealizationState(null) // this should not even start
                .end()
                .end();
        // @formatter:on
    }

    @Override
    void assertTest420TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
// @formatter:off
        assertTaskTree(importTask.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .fullExecutionModePolicyRulesCounters()
                .display()
                .end()
                .end()
                .activityState(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH)
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                .display()
                .assertSuccessCount(USER_DELETE_ALLOWED, true)
                .assertFailureCount(1, getThreads(), true)
                .end();
        // @formatter:on
    }
}
