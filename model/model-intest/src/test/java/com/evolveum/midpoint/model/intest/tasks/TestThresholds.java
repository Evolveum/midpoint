/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.icf.dummy.resource.DummyAccount.ATTR_DESCRIPTION_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Objects;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.test.DummyResourceContoller;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Tests the thresholds functionality.
 *
 * UNFINISHED - currently single-thread only
 *
 * The purpose of this class is _not_ to test thresholds in any specific activity handler.
 * For simplicity we concentrate on the import activity (plus reconciliation when accounts are deleted).
 *
 * We need to test basic functionality of thresholds in single threaded, multi threaded, multi node setup;
 * with different kinds of policy rules (add, modify, delete).
 *
 * Threshold manipulation in specific activities (e.g. reconciliation) is checked in tests devoted to these activities.
 *
 * Performance of thresholds is also tested in separate class (run on demand).
 *
 * Yet another thresholds-related tests are in the `story` module.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholds extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestResource<TaskType> TASK_IMPORT_SINGLE_THREAD_SIMULATE = new TestResource<>(TEST_DIR, "task-import-simulate-single-thread.xml", "c615aa46-a890-45e6-ab4a-94f14fbd204f");
    private static final TestResource<TaskType> TASK_IMPORT_SINGLE_THREAD_SIMULATE_EXECUTE = new TestResource<>(TEST_DIR, "task-import-simulate-execute-single-thread.xml", "046ee785-2b23-4ceb-ba41-7a183045be24");
    private static final TestResource<TaskType> TASK_IMPORT_SINGLE_THREAD_EXECUTE = new TestResource<>(TEST_DIR, "task-import-execute-single-thread.xml", "8576985e-79e4-4d0c-bedd-72652db3c760");
    private static final TestResource<TaskType> TASK_RECONCILIATION_SINGLE_THREAD_SIMULATE = new TestResource<>(TEST_DIR, "task-reconciliation-simulate-single-thread.xml", "4f0c53e1-c10e-486f-9552-d2db4bfc1240");
    private static final TestResource<TaskType> TASK_RECONCILIATION_SINGLE_THREAD_SIMULATE_EXECUTE = new TestResource<>(TEST_DIR, "task-reconciliation-simulate-execute-single-thread.xml", "29d2a62c-6c31-42a4-9364-ecfb0dad0825");
    private static final TestResource<TaskType> TASK_RECONCILIATION_SINGLE_THREAD_EXECUTE = new TestResource<>(TEST_DIR, "task-reconciliation-execute-single-thread.xml", "7652ea69-c8bc-4320-a03e-ab37bb0accc7");

    private static final TestResource<TaskType> TASK_IMPORT_MULTIPLE_THREADS = new TestResource<>(TEST_DIR, "task-import-multiple-threads.xml", "");
    private static final TestResource<TaskType> TASK_IMPORT_MULTIPLE_WORKERS = new TestResource<>(TEST_DIR, "task-import-multiple-workers.xml", "");

    private static final TestResource<RoleType> ROLE_ADD_10 = new TestResource<>(TEST_DIR, "role-add-10.xml", "8f91bccf-fc4b-4987-8232-2e06d174dc37");
    private static final TestResource<RoleType> ROLE_MODIFY_5 = new TestResource<>(TEST_DIR, "role-modify-5.xml", "6b0003a4-65bf-471d-af2c-ed575deaf199");
    private static final TestResource<RoleType> ROLE_DELETE_5 = new TestResource<>(TEST_DIR, "role-delete-5.xml", "9447439f-e6fd-4fb8-bf30-e918e16b42be");

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(TEST_DIR, "resource-dummy-source.xml",
            "40f8fb21-a473-4da7-bbd0-7019d3d450a5", "source", DummyResourceContoller::populateWithDefaultSchema);

    private static final int ACCOUNTS = 100;
    private static final String ACCOUNT_NAME_PATTERN = "a%04d";

    private static final int USER_ADD_ALLOWED = 9;
    private static final int USER_MODIFY_ALLOWED = 4;
    private static final int USER_DELETE_ALLOWED = 4;

    private static final long TIMEOUT = 10000;
    private static final long SLEEP = 200;

    private static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    private static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private int usersBefore;

    private String ruleAddId;
    private String ruleModifyId;
    private String ruleDeleteId;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(ROLE_ADD_10, initResult);
        ruleAddId = determineSingleInducedRuleId(ROLE_ADD_10.oid, initResult);

        repoAdd(ROLE_MODIFY_5, initResult);
        ruleModifyId = determineSingleInducedRuleId(ROLE_MODIFY_5.oid, initResult);

        repoAdd(ROLE_DELETE_5, initResult);
        ruleDeleteId = determineSingleInducedRuleId(ROLE_DELETE_5.oid, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);

        usersBefore = getObjectCount(UserType.class);
        createAccounts();
    }

    private void createAccounts() throws Exception {
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(ACCOUNT_NAME_PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();
    }

    /**
     * Imports accounts from the source in simulate mode. Should stop on 10th added user.
     */
    @Test
    public void test100ImportAdd10Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getSimulateTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_ADD_10.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertCreated(0);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleAddId, USER_ADD_ALLOWED + 1)
                    .end()
                    .progress()
                        .display()
                        .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(UserType.COMPLEX_TYPE, 0, 0)
                        .end()
                    .end();
        // @formatter:on

        when("repeated execution");

        taskManager.resumeTask(importTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then("repeated execution");

        assertCreated(0);

        // @formatter:off
        assertTaskTree(importTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleAddId, USER_ADD_ALLOWED + 2)
                    .end()
                    .progress()
                        .display()
                        .assertUncommitted(0, 1, 0) // fails immediately because of persistent counters
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_ADD_ALLOWED, 2, 0)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(UserType.COMPLEX_TYPE, 0, 0)
                        .end()
                    .end();
        // @formatter:on
    }


    /**
     * Imports accounts from the source in "simulate then execute" mode. Should stop on 10th added user.
     */
    @Test
    public void test110ImportAdd10SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_ADD_10.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertCreated(0);

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
                        .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
                    .end()
                .end()
                .activityState(EXECUTE)
                    .display()
                    .assertRealizationState(null) // this should not even start
                .end();
        // @formatter:on
    }

    /**
     * Imports accounts from the source in "execute" mode. Should create 9 users and then stop on 10th user.
     */
    @Test
    public void test120ImportAdd10Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_ADD_10.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertCreated(USER_ADD_ALLOWED);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, USER_ADD_ALLOWED, 0)
                        .end()
                    .end();
        // @formatter:on
    }

    /**
     * Imports all accounts. This sets the scene for testing modification policy rules.
     */
    @Test
    public void test130ImportWithoutLimits() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        TestResource<TaskType> importTask = getExecuteTask();
        deleteIfPresent(importTask, result);
        addObject(importTask, task, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, 10 * getTimeout(), 10 * getSleep());

        then();

        assertCreated(ACCOUNTS);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(ACCOUNTS, 0, 0)
                    .end();
        // @formatter:on
    }

    /**
     * Modifies each 4th account, to trigger modification policy rules.
     */
    @Test
    public void test199ModifyAccounts() throws Exception {
        for (int i = 0; i < ACCOUNTS; i += 4) {
            String accountName = String.format(ACCOUNT_NAME_PATTERN, i);
            DummyAccount account = Objects.requireNonNull(
                    RESOURCE_SOURCE.controller.getDummyResource().getAccountByUsername(accountName),
                    () -> "No account named " + accountName);
            account.replaceAttributeValue(ATTR_DESCRIPTION_NAME, "changed");
            System.out.println("Changed: " + accountName);
        }
    }

    /**
     * Re-imports accounts from the source in simulate mode. Should stop on 5th modified user.
     */
    @Test
    public void test200ImportModify5Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getSimulateTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_MODIFY_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertModified(0, result);

        // @formatter:off
        assertTaskTree(importTask.oid, "task after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleModifyId, USER_MODIFY_ALLOWED + 1)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_MODIFY_ALLOWED*4, 1, 0)
                    .end();
        // @formatter:on

        when("repeated execution");

        taskManager.resumeTask(importTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then("repeated execution");

        assertModified(0, result);

        // @formatter:off
        assertTaskTree(importTask.oid, "task after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleModifyId, USER_MODIFY_ALLOWED + 2)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_MODIFY_ALLOWED*4, 2, 0)
                    .end();
        // @formatter:on
    }

    /**
     * Re-imports accounts from the source in "simulate then execute" mode. Should stop on 5th modified user.
     */
    @Test
    public void test210ImportModify5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_MODIFY_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertModified(0, result);

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
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_MODIFY_ALLOWED*4, 1, 0)
                    .end()
                .end()
                .activityState(EXECUTE)
                    .display()
                    .assertRealizationState(null) // this should not even start
                .end();
        // @formatter:on
    }

    /**
     * Re-imports accounts from the source in "execute" mode. Should modify 4 users and then stop on 5th user.
     */
    @Test
    public void test220ImportModify5Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_MODIFY_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertModified(USER_MODIFY_ALLOWED, result);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_MODIFY_ALLOWED*4, 1, 0)
                    .end();
        // @formatter:on
    }

    /**
     * Deletes each 4th account, to trigger deletion policy rules.
     */
    @Test
    public void test299DeleteSomeAccounts() throws Exception {
        for (int i = 0; i < ACCOUNTS; i += 4) {
            String accountName = String.format(ACCOUNT_NAME_PATTERN, i);
            RESOURCE_SOURCE.controller.deleteAccount(accountName);
            System.out.println("Deleted: " + accountName);
        }
    }

    /**
     * Reconciliation that deletes owners of missing accounts (simulated). Should stop on 5th, no actions done.
     */
    @Test(enabled = false)
    public void test300ReconcileDelete5Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> reconTask = getReconciliationSimulateTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result, roleAssignmentCustomizer(ROLE_DELETE_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout(), getSleep());

        then();

        assertDeleted(0);

        // @formatter:off
        assertTaskTree(reconTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleDeleteId, USER_DELETE_ALLOWED + 1)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.DELETE, UserType.COMPLEX_TYPE, 0, 0)
                        .end()
                    .end();
        // @formatter:on

        when("repeated execution");

        taskManager.resumeTask(reconTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout(), getSleep());

        then("repeated execution");

        assertDeleted(0);

        // @formatter:off
        assertTaskTree(reconTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .policyRulesCounters()
                        .display()
                        .assertCounter(ruleAddId, USER_DELETE_ALLOWED + 2)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.DELETE, UserType.COMPLEX_TYPE, 0, 0)
                        .end()
                    .end();
        // @formatter:on
    }

    /**
     * Reconciliation that deletes owners of missing accounts (simulate, then execute). Should stop on 5th, no actions done.
     */
    @Test(enabled = false)
    public void test310ReconcileDelete5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getReconciliationSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_DELETE_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertDeleted(0);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_SIMULATION_ID)
                        .assertInProgressLocal()
                        .assertFatalError()
                        .itemProcessingStatistics()
                            .display()
                            .assertTotalCounts(USER_DELETE_ALLOWED, 1, 0)
                        .end()
                    .end()
                    .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID)
                        .display()
                        .assertRealizationState(null) // this should not even start
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Reconciliation that deletes owners of missing accounts (execute). Should stop on 5th after deleting four users.
     */
    @Test(enabled = false)
    public void test320ReconcileDelete5Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestResource<TaskType> importTask = getReconciliationExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result, roleAssignmentCustomizer(ROLE_DELETE_5.oid));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout(), getSleep());

        then();

        assertDeleted(USER_DELETE_ALLOWED);

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .activityState(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertUncommitted(USER_DELETE_ALLOWED, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(USER_DELETE_ALLOWED, 1, 0)
                    .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.DELETE, UserType.COMPLEX_TYPE, USER_DELETE_ALLOWED, 0)
                        .end()
                    .end();
        // @formatter:on
    }

    private TestResource<TaskType> getSimulateTask() {
        return TASK_IMPORT_SINGLE_THREAD_SIMULATE;
    }

    private TestResource<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SINGLE_THREAD_SIMULATE_EXECUTE;
    }

    private TestResource<TaskType> getExecuteTask() {
        return TASK_IMPORT_SINGLE_THREAD_EXECUTE;
    }

    private TestResource<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SINGLE_THREAD_SIMULATE;
    }

    private TestResource<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SINGLE_THREAD_SIMULATE_EXECUTE;
    }

    private TestResource<TaskType> getReconciliationExecuteTask() {
        return TASK_RECONCILIATION_SINGLE_THREAD_EXECUTE;
    }

    long getTimeout() {
        return TIMEOUT;
    }

    long getSleep() {
        return SLEEP;
    }

    private void assertModified(int expected, OperationResult result) throws SchemaException {
        int changed = repositoryService.countObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_COST_CENTER).eq("changed")
                        .build(), null, result);
        assertThat(changed).as("changed users").isEqualTo(expected);
    }

    private void assertCreated(int expected) throws CommonException {
        int usersCreated = getObjectCount(UserType.class) - usersBefore;
        assertThat(usersCreated).as("users created").isEqualTo(expected);
    }

    private void assertDeleted(int expected) throws CommonException {
        int usersDeleted = usersBefore + ACCOUNTS - getObjectCount(UserType.class);
        assertThat(usersDeleted).as("users deleted").isEqualTo(expected);
    }
}
