/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests reporting of task state, progress, and errors.
 */
@Deprecated // TODO move to task-specific tests
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskReporting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reporting"); // TODO move

    private static final String ACCOUNT_NAME_PATTERN = "u-%06d";

    private static final String ATTR_NUMBER = "number";
    private static final String ATTR_FAILURE_MODE = "failureMode";

    public static boolean failuresEnabled;

    private static final String SHADOW_CREATION_FAILURE = "shadow-creation-failure";
    public static final String PROJECTOR_FATAL_ERROR = "projector-fatal-error";

    // Numbers of accounts with various kinds of errors

    /** u-000000: good account */
    private static final int IDX_GOOD_ACCOUNT = 0;

    /** u-000001: null resource object name in {@link #test090ImportWithSearchFailing()} */
    private static final int IDX_MALFORMED_SHADOW = 1;

    /** u-000002: fatal error in projector */
    private static final int IDX_PROJECTOR_FATAL_ERROR = 2;

    /** u-000003: uid too long to be stored in the repository */
    private static final int IDX_LONG_UID = 3;

    private static final String MALFORMED_SHADOW_NAME = formatAccountName(IDX_MALFORMED_SHADOW);

    private static final DummyTestResource RESOURCE_DUMMY_SOURCE = new DummyTestResource(TEST_DIR,
            "resource-source.xml", "a1c7dcb8-07f8-4626-bea7-f10d9df7ec9f", "source",
            controller -> {
                // This is extra secondary identifier. We use it to induce schema exceptions during shadow pre-processing.
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_NUMBER, Integer.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_FAILURE_MODE, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DummyAccount.ATTR_FULLNAME_NAME, String.class, true, false);
            });

    private static final DummyTestResource RESOURCE_DUMMY_TARGET = new DummyTestResource(TEST_DIR,
            "resource-target.xml", "f1859897-0c10-430e-aefe-7ced49d14a23", "target");
    private static final TestResource<RoleType> ROLE_TARGET = new TestResource<>(TEST_DIR, "role-target.xml", "fdcd5c7a-86c0-4a0e-8b22-dda79183fcf3");
    private static final TestResource<TaskType> TASK_IMPORT = new TestResource<>(TEST_DIR, "task-import.xml", "e06f3f5c-4acc-4c6a-baa3-5c7a954ce4e9");
    private static final TestResource<TaskType> TASK_IMPORT_RETRY_BY_FILTERING = new TestResource<>(TEST_DIR, "task-import-retry-by-filtering.xml", "e06f3f5c-4acc-4c6a-baa3-5c7a954ce4e9");
    private static final TestResource<TaskType> TASK_IMPORT_RETRY_BY_FETCHING = new TestResource<>(TEST_DIR, "task-import-retry-by-fetching.xml", "e06f3f5c-4acc-4c6a-baa3-5c7a954ce4e9");
    private static final TestResource<TaskType> TASK_RECONCILIATION = new TestResource<>(TEST_DIR, "task-reconciliation.xml", "566c822c-5db4-4879-a159-3749fef11c7a");
    private static final TestResource<TaskType> TASK_RECONCILIATION_PARTITIONED_MULTINODE = new TestResource<>(TEST_DIR, "task-reconciliation-partitioned-multinode.xml", "0e818ebb-1fd8-4d89-a4f4-aa42ce8ac475");

    private static final int USERS = 10;

    private static final DummyTestResource RESOURCE_DUMMY_HACKED = new DummyTestResource(TEST_DIR, "resource-hacked.xml", "8aad610b-0e35-4604-9139-2f864ac4eac2", "hacked");

    private static final TestResource<TaskType> TASK_RECONCILIATION_HACKED = new TestResource<>(TEST_DIR, "task-reconciliation-hacked.xml", "9e2887cd-3f45-46e7-82ee-20454ba25d94");
    private boolean isNewRepo;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        // Some tests differ for new and old repo for good reasons, so we need this switch.
        isNewRepo = plainRepositoryService instanceof SqaleRepositoryService;

        initDummyResource(RESOURCE_DUMMY_SOURCE, initTask, initResult);
        initDummyResource(RESOURCE_DUMMY_TARGET, initTask, initResult);
        addObject(ROLE_TARGET, initTask, initResult);

        assertSuccess(modelService.testResource(RESOURCE_DUMMY_SOURCE.oid, initTask, initResult));
        assertSuccess(modelService.testResource(RESOURCE_DUMMY_TARGET.oid, initTask, initResult));

        for (int i = 0; i < USERS; i++) {
            if (i != IDX_LONG_UID) {
                createAccount(i);
            } else {
                // That one will be added later
            }
        }

        initDummyResource(RESOURCE_DUMMY_HACKED, initTask, initResult);
        assertSuccess(modelService.testResource(RESOURCE_DUMMY_HACKED.oid, initTask, initResult));
    }

    private void createAccount(int i) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException,
            FileNotFoundException, ConflictException, InterruptedException {
        String name = formatAccountName(i);
        DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.addAccount(name);
        account.addAttributeValue(ATTR_NUMBER, i);
        account.addAttributeValue(ATTR_FAILURE_MODE, getFailureMode(i));
    }

    private static String formatAccountName(int i) {
        return String.format(ACCOUNT_NAME_PATTERN, i) +
                (i == IDX_LONG_UID ? StringUtils.repeat("-123456789", 30) : "");
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private String getFailureMode(int index) {
        switch (index) {
            case IDX_GOOD_ACCOUNT:
                return null;
            case IDX_MALFORMED_SHADOW:
                return SHADOW_CREATION_FAILURE;
            case IDX_PROJECTOR_FATAL_ERROR:
                return PROJECTOR_FATAL_ERROR;
            default:
                return null;
        }
    }

    /**
     * Tests the state when the searchObject call itself fails.
     * The task has basically nothing to do here: it cannot continue processing objects.
     * (Except for modifying the query to avoid poisoned object or objects.)
     */
    @Test
    public void test090ImportWithSearchFailing() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.getDummyResource().getAccountByUsername(MALFORMED_SHADOW_NAME);
        account.setName(null); // This causes a failure during query execution (not even in the results handler).
        try {

            when();
            addObject(TASK_IMPORT, task, result);
            Task importTask = waitForTaskFinish(TASK_IMPORT.oid, builder -> builder.errorOk(true));

            then();
            // @formatter:off
            assertTask(importTask, "import task after")
                    .display()
                    .assertFatalError()
                    .assertSuspended()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                    .rootSynchronizationInformation()
                        .display();
            // @formatter:on
        } finally {
            account.setName(MALFORMED_SHADOW_NAME);
        }
    }

    @Test
    public void test100Import() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        rerunTask(TASK_IMPORT.oid, result);
        Task importTask = waitForTaskFinish(TASK_IMPORT.oid, true);

        then();
        stabilize();
        // @formatter:off
        assertTask(importTask, "import task after")
                .display()
                .assertSuccess()
                .rootItemProcessingInformation()
                    .display()
                    .end()
                .rootSynchronizationInformation()
                    .display();
        // @formatter:on

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display();
    }

    @Test
    public void test110ImportWithSingleMalformedAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // This will cause problem when updating shadow
        DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.getDummyResource().getAccountByUsername(MALFORMED_SHADOW_NAME);
        account.replaceAttributeValue(ATTR_NUMBER, "WRONG");

        // Other kinds of failures are still disabled, to check last failed object name in case of malformed accounts

        when();
        rerunTaskErrorsOk(TASK_IMPORT.oid, result);

        then();
        stabilize();
        // @formatter:off
        assertTask(TASK_IMPORT.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(9)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(8)
                    .assertFailureCount(1)
                    .assertLastFailureObjectName("u-000001")
                    .end()
                .rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 8, 0, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                    .assertTransitions(2)
                    .end();
        // @formatter:on

        // TODO assert redirected errors in the task
    }

    @Test
    public void test120ImportWithAllFailuresEnabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // This enables other kinds of failures (e.g. those in mappings)
        failuresEnabled = true;

        createAccount(IDX_LONG_UID);

        when();
        rerunTaskErrorsOk(TASK_IMPORT.oid, result);

        then();
        stabilize();
        TaskAsserter<Void> taskAsserter = assertTask(TASK_IMPORT.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(10);

        if (isNewRepo) {
            // super-long name does not fail in new repo
            taskAsserter.rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(8)
                    .assertFailureCount(2);
            taskAsserter.rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 7, 1, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                    .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0) // Long-name is OK for new repo
                    .assertTransitions(3);
        } else {
            taskAsserter.rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(7)
                    .assertFailureCount(3);
            taskAsserter.rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 7, 1, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                    .assertTransition(null, null, null, null, 0, 1, 0) // Long UID account
                    .assertTransitions(3);
        }

        List<OperationExecutionType> taskExecRecords = getTask(TASK_IMPORT.oid).asObjectable().getOperationExecution();
        List<OperationExecutionType> redirected = taskExecRecords.stream()
                .filter(r -> r.getRealOwner() != null)
                .collect(Collectors.toList());
        if (isNewRepo) {
            assertThat(redirected).as("redirected operation execution records").isEmpty();
        } else {
            assertThat(redirected).as("redirected operation execution records").hasSize(1);
            assertThat(redirected.get(0).getRealOwner().getIdentification())
                    .as("identification")
                    .isEqualTo(formatAccountName(IDX_LONG_UID));
        }

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display()
                .assertHasComplexOperationExecution(TASK_IMPORT.oid, OperationResultStatusType.SUCCESS);
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.get())
                .display()
                .assertHasComplexOperationExecution(TASK_IMPORT.oid, OperationResultStatusType.FATAL_ERROR);
    }

    @Test
    public void test130RetryImportByFiltering() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        taskManager.deleteTask(TASK_IMPORT.oid, result);
        addObject(TASK_IMPORT_RETRY_BY_FILTERING, task, result);
        waitForTaskFinish(TASK_IMPORT_RETRY_BY_FILTERING.oid, builder -> builder.errorOk(true));

        then();
        stabilize();
        // @formatter:off
        TaskAsserter<Void> taskAsserter = assertTask(TASK_IMPORT_RETRY_BY_FILTERING.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(10) // From the resource we get all 10 accounts.
                .rootItemProcessingInformation()
                    .display()
                    .assertSkipCount(8)
                    .assertFailureCount(2)
                    .end();
        if (isNewRepo) {
            taskAsserter.rootSynchronizationInformation()
                    .display()
                    .assertTransitions(2)
                    // That record was already linked and remain so.
                    .assertTransition(LINKED, LINKED, LINKED, null, 0, 1, 0)
                    // Malformed account has a LINKED shadow - skipped count based on previous test
                    .assertTransition(LINKED, null, null, null, 0, 1, 8);
        } else {
            taskAsserter.rootSynchronizationInformation()
                    .display()
                    .assertTransitions(3)
                    // That record was already linked and remain so.
                    .assertTransition(LINKED, LINKED, LINKED, null, 0, 1, 0)
                    // Malformed account has a LINKED shadow - skipped count based on previous test
                    .assertTransition(LINKED, null, null, null, 0, 1, 7)
                    .assertTransition(null, null, null, null, 0, 0, 1); // No shadow here
        }
        // @formatter:on

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display();
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.get())
                .display();
    }

    @Test
    public void test140RetryImportByFetching() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        taskManager.deleteTask(TASK_IMPORT_RETRY_BY_FILTERING.oid, result);
        addObject(TASK_IMPORT_RETRY_BY_FETCHING, task, result);
        waitForTaskFinish(TASK_IMPORT_RETRY_BY_FETCHING.oid, builder -> builder.errorOk(true));

        then();
        stabilize();
        // @formatter:off
        assertTask(TASK_IMPORT_RETRY_BY_FETCHING.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(2)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(0)
                    .assertFailureCount(2)
                    .end()
                .rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 0, 1, 0) // That record was already linked and remain so.
                    .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                    .assertTransitions(2);
        // @formatter:on

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display();
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.get())
                .display();
    }

    @Test
    public void test200ReconciliationWithAllFailuresEnabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // 3rd account is already broken

        when();
        addObject(TASK_RECONCILIATION, task, result);
        waitForTaskFinish(TASK_RECONCILIATION.oid, builder -> builder.errorOk(true));

        then();
        stabilize();

        if (isNewRepo) {
            // @formatter:off
            assertTask(TASK_RECONCILIATION.oid, "reconciliation task after")
                    .display()
                    .displayOperationResult()
                    .assertPartialError()
                    .assertClosed()
                    .assertProgress(12)
                    .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .assertSuccessCount(8)
                            .assertFailureCount(2) // u-000001 failed once in 2nd part, and once in 3rd part
                        .end()
                        .synchronizationStatistics()
                            .display()
                            .assertTransitions(2)
                            .assertTransition(LINKED, LINKED, LINKED, null, 8, 1, 0) // Those 9 records were already linked and remain so.
                            .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                        .end()
                    .end()
                    .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .assertFailureCount(1) // u-000001 failed once in 2nd part, and once in 3rd part
                            .assertSkipCount(1) // u-000002 - repeated because fullSynchronizationTimestamp is still null
                            .assertLastFailureObjectName(MALFORMED_SHADOW_NAME)
                        .end()
                    .end();
            // @formatter:on
        } else {
            // @formatter:off
            assertTask(TASK_RECONCILIATION.oid, "reconciliation task after")
                    .display()
                    .displayOperationResult()
                    .assertPartialError()
                    .assertClosed()
                    .assertProgress(12)
                    .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .assertSuccessCount(7)
                            .assertFailureCount(3) // u-000001 failed once in 2nd part, and once in 3rd part
                        .end()
                        .synchronizationStatistics()
                            .display()
                            .assertTransitions(3)
                            .assertTransition(LINKED, LINKED, LINKED, null, 7, 1, 0) // Those 9 records were already linked and remain so.
                            .assertTransition(LINKED, null, null, null, 0, 1, 0) // Malformed account has a LINKED shadow
                            .assertTransition(null, null, null, null, 0, 1, 0) // Long UID account
                        .end()
                    .end()
                    .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .assertFailureCount(1) // u-000001 failed once in 2nd part, and once in 3rd part
                            .assertSkipCount(1) // u-000002 - repeated because fullSynchronizationTimestamp is still null
                            .assertLastFailureObjectName(MALFORMED_SHADOW_NAME)
                        .end()
                    .end();
            // @formatter:on
        }

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display();
        assertShadow(formatAccountName(IDX_MALFORMED_SHADOW), RESOURCE_DUMMY_SOURCE.get())
                .display();
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.get())
                .display();
    }

    @Test
    public void test210PartitionedMultinodeReconciliationWithAllFailuresEnabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // 3rd account is already broken

        when();
        addTask(TASK_RECONCILIATION_PARTITIONED_MULTINODE, result);
        waitForRootActivityCompletion(TASK_RECONCILIATION_PARTITIONED_MULTINODE.oid, 60000);

        then();
        // @formatter:off
        assertTaskTree(TASK_RECONCILIATION_PARTITIONED_MULTINODE.oid, "reconciliation task after")
                .display()
                .subtaskForPath(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .display()
                    .end()
                .subtaskForPath(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .display()
                    .subtask(0)
                        .display()
                        .end()
                    .subtask(1)
                        .display()
                        .end()
                    .end()
                .subtaskForPath(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .display();
        // @formatter:on

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.get())
                .display()
                .assertHasComplexOperationExecution(TASK_RECONCILIATION_PARTITIONED_MULTINODE.oid, OperationResultStatusType.SUCCESS);
        assertShadow(formatAccountName(IDX_MALFORMED_SHADOW), RESOURCE_DUMMY_SOURCE.get())
                .display()
                .assertHasComplexOperationExecution(TASK_RECONCILIATION_PARTITIONED_MULTINODE.oid, OperationResultStatusType.FATAL_ERROR);
        // MID-7113
//        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.getResource())
//                .display()
//                .assertHasComplexOperationExecution(TASK_RECONCILIATION_PARTITIONED_MULTINODE.oid, OperationResultStatusType.FATAL_ERROR);
    }

    /**
     * Checks if deletion of unmatched shadow (by reconciliation) is correctly reported. See MID-6877.
     */
    @Test
    public void test300DeleteUnmatchedShadowByReconciliation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> hacker = createHackerShadow();
        provisioningService.addObject(hacker, null, null, task, result);
        assertShadow("hacker", RESOURCE_DUMMY_HACKED.get())
                .display();

        when();

        addTask(TASK_RECONCILIATION_HACKED, result);
        waitForTaskFinish(TASK_RECONCILIATION_HACKED.oid, false);

        then();

        // @formatter:off
        assertTask(TASK_RECONCILIATION_HACKED.oid, "after")
                .display()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(1)
                    .end()
                    .progress()
                        .display()
                        .assertSuccessCount(1, false)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 1, 0, 0)
                        .assertTransitions(1)
                    .end()
                .end()
                .assertClosed()
                .assertSuccess();
        // @formatter:on
    }

    @NotNull
    private PrismObject<ShadowType> createHackerShadow() throws SchemaException, ConfigurationException {
        var hacker = new ShadowType(prismContext)
                .resourceRef(RESOURCE_DUMMY_HACKED.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .beginAttributes()
                .<ShadowType>end()
                .asPrismObject();
        ResourceAttribute<String> nameAttr = RESOURCE_DUMMY_HACKED.controller.createAccountAttribute(SchemaConstants.ICFS_NAME);
        nameAttr.setRealValue("hacker");
        hacker.findContainer(ShadowType.F_ATTRIBUTES).getValue().add(nameAttr);
        return hacker;
    }
}
