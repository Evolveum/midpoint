/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.icf.dummy.resource.DummyAccount.ATTR_DESCRIPTION_NAME;

import static com.evolveum.icf.dummy.resource.DummyAccount.ATTR_FULLNAME_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Objects;
import java.util.function.Consumer;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.test.DummyResourceContoller;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
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
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Tests the thresholds functionality.
 *
 * The purpose of this class is _not_ to test thresholds in any specific activity handler.
 * For simplicity we concentrate on the import and reconciliation activity.
 *
 * We need to test basic functionality of thresholds in single threaded, multi threaded, multi node setup;
 * with different kinds of policy rules (add, modify, delete).
 *
 * Threshold manipulation in specific activities (e.g. live sync) will be checked in tests devoted to these activities.
 *
 * Performance of thresholds will be also tested in separate class (run on demand).
 *
 * Yet another thresholds-related tests are in the `story` module.
 */
@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestThresholds extends AbstractEmptyModelIntegrationTest {

    static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    /** Used also for "import without limits" in {@link #test130ImportWithoutLimits()}. */
    static final TestObject<TaskType> TASK_IMPORT_EXECUTE_SINGLE = TestObject.file(TEST_DIR, "task-import-execute-single.xml", "8576985e-79e4-4d0c-bedd-72652db3c760");

    private static final TestObject<RoleType> ROLE_ADD_10 = TestObject.file(TEST_DIR, "role-add-10.xml", "8f91bccf-fc4b-4987-8232-2e06d174dc37");
    private static final TestObject<RoleType> ROLE_MODIFY_COST_CENTER_5 = TestObject.file(TEST_DIR, "role-modify-cost-center-5.xml", "6b0003a4-65bf-471d-af2c-ed575deaf199");
    private static final TestObject<RoleType> ROLE_MODIFY_FULL_NAME_5 = TestObject.file(TEST_DIR, "role-modify-full-name-5.xml", "11562df4-c3e7-4e9e-a8f9-0b7f1bb7df75");
    private static final TestObject<RoleType> ROLE_DELETE_5 = TestObject.file(TEST_DIR, "role-delete-5.xml", "9447439f-e6fd-4fb8-bf30-e918e16b42be");

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(TEST_DIR, "resource-dummy-source.xml",
            "40f8fb21-a473-4da7-bbd0-7019d3d450a5", "source", DummyResourceContoller::populateWithDefaultSchema);

    private static final DummyTestResource RESOURCE_SOURCE_SLOW = new DummyTestResource(TEST_DIR, "resource-dummy-source-slow.xml",
            "1645e542-d034-4118-b8af-97c4d22d81d6", "source-slow", DummyResourceContoller::populateWithDefaultSchema);

    static final int ACCOUNTS = 100;
    private static final String ACCOUNT_NAME_PATTERN = "a%02d";

    static final int USER_ADD_ALLOWED = 9;
    static final int USER_MODIFY_ALLOWED = 4;
    static final int USER_DELETE_ALLOWED = 4;

    static final ActivityPath SIMULATE = ActivityPath.fromId("simulate");
    static final ActivityPath EXECUTE = ActivityPath.fromId("execute");

    private int usersBefore;
    private int accountsDeleted;

    String ruleAddId;
    String ruleModifyCostCenterId;
    String ruleModifyFullNameId;
    String ruleDeleteId;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(ROLE_ADD_10, initResult);
        ruleAddId = determineSingleInducedRuleId(ROLE_ADD_10.oid, initResult);

        repoAdd(ROLE_MODIFY_COST_CENTER_5, initResult);
        ruleModifyCostCenterId = determineSingleInducedRuleId(ROLE_MODIFY_COST_CENTER_5.oid, initResult);

        repoAdd(ROLE_MODIFY_FULL_NAME_5, initResult);
        ruleModifyFullNameId = determineSingleInducedRuleId(ROLE_MODIFY_FULL_NAME_5.oid, initResult);

        repoAdd(ROLE_DELETE_5, initResult);
        ruleDeleteId = determineSingleInducedRuleId(ROLE_DELETE_5.oid, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        initDummyResource(RESOURCE_SOURCE_SLOW, initTask, initResult);

        usersBefore = getObjectCount(UserType.class);
        displayValue("users before", usersBefore);

        createAccounts();
    }

    private void createAccounts() throws Exception {
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(ACCOUNT_NAME_PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();
    }

    abstract int getWorkerThreads();

    /** Total threads of processing (1 for single-thread scenario). */
    int getThreads() {
        return Math.max(1, getWorkerThreads());
    }

    /** Implants worker threads to the root activity definition. */
    private Consumer<PrismObject<TaskType>> getRootWorkerThreadsCustomizer() {
        return rootActivityWorkerThreadsCustomizer(getWorkerThreads());
    }

    /** Implants worker threads to the component activities definitions. */
    private Consumer<PrismObject<TaskType>> getCompositeWorkerThreadsCustomizer() {
        return compositeActivityWorkerThreadsCustomizer(getWorkerThreads());
    }

    /** Implants worker threads to embedded activities definitions (via tailoring). */
    private Consumer<PrismObject<TaskType>> getReconWorkerThreadsCustomizer() {
        return tailoringWorkerThreadsCustomizer(getWorkerThreads());
    }

    /**
     * Imports accounts from the source in simulate mode. Should stop on 10th added user.
     */
    @Test
    public void test100ImportAdd10Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getSimulateTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_ADD_10.oid),
                        getRootWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertCreated(0);
        assertTest100Task(importTask);

        when("repeated execution");

        taskManager.resumeTaskTree(importTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then("repeated execution");

        assertCreated(0);
        assertTest100TaskAfterRepeatedExecution(importTask);

        if (isNativeRepository()) {
            and("there are no simulation results"); // MID-8936
            assertNoRepoObjects(SimulationResultType.class);
        }
    }

    abstract void assertTest100Task(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;
    abstract void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Imports accounts from the source in "simulate then execute" mode. Should stop on 10th added user.
     */
    @Test
    public void test110ImportAdd10SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_ADD_10.oid),
                        getCompositeWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertCreated(0);
        assertTest110TaskAfter(importTask);
    }

    abstract void assertTest110TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Imports accounts from the source in "execute" mode. Should create 9 users and then stop on 10th user.
     */
    @Test
    public void test120ImportAdd10Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_ADD_10.oid),
                        getRootWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertCreated(USER_ADD_ALLOWED);
        assertTest120TaskAfter(importTask);
    }

    abstract void assertTest120TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Imports all accounts. This sets the scene for testing modification policy rules.
     */
    @Test
    public void test130ImportWithoutLimits() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        deleteIfPresent(TASK_IMPORT_EXECUTE_SINGLE, result);
        addObject(TASK_IMPORT_EXECUTE_SINGLE, task, result,
                getRootWorkerThreadsCustomizer());
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_IMPORT_EXECUTE_SINGLE.oid, result, 10 * getTimeout());

        then();

        assertCreated(ACCOUNTS);

        // @formatter:off
        assertTaskTree(TASK_IMPORT_EXECUTE_SINGLE.oid, "after")
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
     * Modifies description (-> cost center) of each 4th account, to trigger cost center modification policy rule.
     * - fullName of each 25th, to allow full name modification policy rule to pass (4 users are changed).
     */
    @Test
    public void test199ModifyAccountDescription() throws Exception {
        for (int i = 0; i < ACCOUNTS; i += 4) {
            DummyAccount account = getAccount(i);
            account.replaceAttributeValue(ATTR_DESCRIPTION_NAME, "changed");
            System.out.println("Changed description (cost center): " + account.getName());
        }
    }

    /**
     * Re-imports accounts from the source in simulate mode. Should stop on 5th modified user.
     */
    @Test
    public void test200ImportModifyCostCenter5Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getSimulateTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_MODIFY_COST_CENTER_5.oid),
                        getRootWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertModifiedCostCenter(0, result);
        assertTest200TaskAfter(importTask);

        when("repeated execution");

        taskManager.resumeTaskTree(importTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then("repeated execution");

        assertModifiedCostCenter(0, result);
        assertTest200TaskAfterRepeatedExecution(importTask);
    }

    abstract void assertTest200TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;
    abstract void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Re-imports accounts from the source in "simulate then execute" mode. Should stop on 5th modified user.
     */
    @Test
    public void test210ImportModifyCostCenter5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_MODIFY_COST_CENTER_5.oid),
                        getCompositeWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertModifiedCostCenter(0, result);
        assertTest210TaskAfter(importTask);
    }

    abstract void assertTest210TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Re-imports accounts from the source in "execute" mode. Should modify 4 users and then stop on 5th user.
     */
    @Test
    public void test220ImportModifyCostCenter5Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> importTask = getExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_MODIFY_COST_CENTER_5.oid),
                        getRootWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertModifiedCostCenter(USER_MODIFY_ALLOWED, result);
        assertTest220TaskAfter(importTask);
    }

    abstract void assertTest220TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Re-imports accounts from the source in "simulate then execute" mode.
     *
     * This time there are only 4 users with changed full name, so both simulate and execute
     * activities should proceed.
     */
    @Test
    public void test300ImportModifyFullName5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Modifies description (-> cost center) of each 25th account, to trigger cost center modification policy rule.
        // 4 accounts are changed in total.
        changeFullNameOnResource(1);

        TestObject<TaskType> importTask = getSimulateExecuteTask();

        when();

        deleteIfPresent(importTask, result);
        addObject(importTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_MODIFY_FULL_NAME_5.oid),
                        getCompositeWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(importTask.oid, result, getTimeout());

        then();

        assertModifiedFullName(1, 4, result);
        assertTest300TaskAfter(importTask);
    }

    abstract void assertTest300TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Re-imports accounts from the source in "simulate then execute" mode.
     * This time the reconciliation is used, because of its implementation (5 sub-activities, shared state).
     *
     * There are only 4 users with changed full name, so both simulate and execute
     * activities should proceed.
     */
    @Test
    public void test310ReconModifyFullName5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        changeFullNameOnResource(2);

        TestObject<TaskType> reconTask = getReconciliationSimulateExecuteTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_MODIFY_FULL_NAME_5.oid),
                        getReconWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, 5*getTimeout());

        then();

        assertModifiedFullName(2, 4, result);
        assertTest310TaskAfter(reconTask);
    }

    abstract void assertTest310TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Deletes each 4th account, to trigger deletion policy rules.
     */
    @Test
    public void test399DeleteSomeAccounts() throws Exception {
        for (int i = 0; i < ACCOUNTS; i += 4) {
            String accountName = String.format(ACCOUNT_NAME_PATTERN, i);
            RESOURCE_SOURCE.controller.deleteAccount(accountName);
            System.out.println("Deleted: " + accountName);
            accountsDeleted++;
        }
        displayValue("Accounts deleted", accountsDeleted);
        displayValue("Users", getObjectCount(UserType.class));
    }

    /**
     * Reconciliation that deletes owners of missing accounts (simulated). Should stop on 5th, no actions done.
     */
    @Test
    public void test400ReconcileDelete5Simulate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> reconTask = getReconciliationSimulateTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_DELETE_5.oid),
                        getReconWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout());

        then();

        assertDeleted(0);
        assertTest400TaskAfter(reconTask);

        when("repeated execution");

        taskManager.resumeTaskTree(reconTask.oid, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout());

        then("repeated execution");

        assertDeleted(0);
        assertTest400TaskAfterRepeatedExecution(reconTask);

        displayValue("Users", getObjectCount(UserType.class));
    }

    abstract void assertTest400TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException;
    abstract void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Reconciliation that deletes owners of missing accounts (simulate, then execute). Should stop on 5th, no actions done.
     */
    @Test
    public void test410ReconcileDelete5SimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> reconTask = getReconciliationSimulateExecuteTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_DELETE_5.oid),
                        getReconWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout());

        then();

        assertDeleted(0);
        assertTest410TaskAfter(reconTask);

        displayValue("Users", getObjectCount(UserType.class));
    }

    abstract void assertTest410TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    /**
     * Reconciliation that deletes owners of missing accounts (execute). Should stop on 5th after deleting four users.
     */
    @Test
    public void test420ReconcileDelete5Execute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> reconTask = getReconciliationExecuteTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_DELETE_5.oid),
                        getReconWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout());

        then();

        assertDeleted(USER_DELETE_ALLOWED);
        assertTest420TaskAfter(reconTask);

        displayValue("Users", getObjectCount(UserType.class));
    }

    /**
     * Here we try to delete all users, without any thresholds.
     * We try to run simulation first, then the execution.
     */
    @Test
    public void test430ReconcileDeleteAllSimulateExecute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> reconTask = getReconciliationSimulateExecuteTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer( // No role with a policy rule here
                        getReconWorkerThreadsCustomizer()));
        waitForTaskTreeCloseCheckingSuspensionWithError(reconTask.oid, result, getTimeout());

        then();

        displayValue("Users", getObjectCount(UserType.class));

        assertDeleted(accountsDeleted);
        assertTaskTree(reconTask.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess();
    }

    @Test
    public void test520ReconcileWithExecutionTime() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TestObject<TaskType> reconTask = getReconciliationWithExecutionTimeTask();

        when();

        deleteIfPresent(reconTask, result);
        addObject(reconTask, task, result,
                aggregateCustomizer(
                        executionTimeCustomizer("PT2S")));
        waitForTaskSuspend(reconTask.oid, result, getTimeout(), 500);

        then();

        assertTest520TaskAfter(reconTask);
    }

    protected Consumer<PrismObject<TaskType>> executionTimeCustomizer(String exceedsExecutionTimeThreshold) {
        return object -> {
            if (exceedsExecutionTimeThreshold == null) {
                return;
            }

            object.asObjectable().getActivity()
                    .beginPolicies()
                        .beginPolicy()
                            .name("Max. execution time is " + exceedsExecutionTimeThreshold)
                            .beginPolicyConstraints()
                                .beginExecutionTime()
                                    .exceeds(XmlTypeConverter.createDuration(exceedsExecutionTimeThreshold))
                                .<ActivityPolicyConstraintsType>end()
                            .<ActivityPolicyType>end()
                            .beginPolicyActions()
                                .beginSuspendTask()
                                    // no parameters
                                .<ActivityPolicyActionsType>end()
                            .<ActivityPolicyType>end()
                        .<ActivityPoliciesType>end()
                    .<ActivityDefinitionType>end();
        };
    }

    abstract void assertTest520TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException;

    abstract void assertTest420TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException;

    abstract TestObject<TaskType> getSimulateTask();

    abstract TestObject<TaskType> getSimulateExecuteTask();

    abstract TestObject<TaskType> getExecuteTask();

    abstract TestObject<TaskType> getReconciliationSimulateTask();

    abstract TestObject<TaskType> getReconciliationSimulateExecuteTask();

    abstract TestObject<TaskType> getReconciliationExecuteTask();

    abstract TestObject<TaskType> getReconciliationWithExecutionTimeTask();

    abstract long getTimeout();

    private void assertModifiedCostCenter(int expected, OperationResult result) throws SchemaException {
        int changed = repositoryService.countObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_COST_CENTER).eq("changed")
                        .build(), null, result);
        assertThat(changed).as("changed users").isEqualTo(expected);
    }

    private void changeFullNameOnResource(int wave)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        for (int i = 0; i < ACCOUNTS; i += 25) {
            DummyAccount account = getAccount(i);
            account.replaceAttributeValue(ATTR_FULLNAME_NAME, getValueForWave(wave));
            System.out.println("Changed full name: " + account.getName());
        }
    }

    private @NotNull String getValueForWave(int wave) {
        return "changed" + wave;
    }

    private void assertModifiedFullName(int wave, int expected, OperationResult result) throws SchemaException {
        int changed = repositoryService.countObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_FULL_NAME).eq(getValueForWave(wave))
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

    private @NotNull DummyAccount getAccount(int i) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        String accountName = String.format(ACCOUNT_NAME_PATTERN, i);
        return Objects.requireNonNull(
                RESOURCE_SOURCE.controller.getDummyResource().getAccountByName(accountName),
                () -> "No account named " + accountName);
    }
}
