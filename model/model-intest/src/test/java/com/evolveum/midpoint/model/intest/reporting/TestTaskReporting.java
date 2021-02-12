/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.reporting;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;

/**
 * Tests reporting of task state, progress, and errors.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskReporting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reporting");

    private static final String ACCOUNT_NAME_PATTERN = "u-%06d";

    private static final String ATTR_NUMBER = "number";
    private static final String ATTR_FAILURE_MODE = "failureMode";

    public static boolean failuresEnabled;

    private static final String SHADOW_CREATION_FAILURE = "shadow-creation-failure";
    public static final String PROJECTOR_FATAL_ERROR = "projector-fatal-error";

    // Numbers of accounts with various kinds of errors
    private static final int IDX_GOOD_ACCOUNT = 0;
    private static final int IDX_MALFORMED_SHADOW = 1;
    private static final int IDX_PROJECTOR_FATAL_ERROR = 2;

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
    private static final TestResource<TaskType> TASK_RECONCILIATION = new TestResource<>(TEST_DIR, "task-reconciliation.xml", "566c822c-5db4-4879-a159-3749fef11c7a");

    private static final int USERS = 10;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_SOURCE, initTask, initResult);
        initDummyResource(RESOURCE_DUMMY_TARGET, initTask, initResult);
        addObject(ROLE_TARGET, initTask, initResult);

        assertSuccess(modelService.testResource(RESOURCE_DUMMY_SOURCE.oid, initTask));
        assertSuccess(modelService.testResource(RESOURCE_DUMMY_TARGET.oid, initTask));

        for (int i = 0; i < USERS; i++) {
            String name = formatAccountName(i);
            DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.addAccount(name);
            account.addAttributeValue(ATTR_NUMBER, i);
            account.addAttributeValue(ATTR_FAILURE_MODE, getFailureMode(i));
       }
    }

    private static String formatAccountName(int i) {
        return String.format(ACCOUNT_NAME_PATTERN, i);
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
            assertTask(importTask, "import task after")
                    .display()
                    .assertFatalError()
                    .assertClosed()
                    .iterativeTaskInformation()
                        .display()
                        .end()
                    .synchronizationInformation()
                        .display();

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
        assertTask(importTask, "import task after")
                .display()
                .assertSuccess()
                .iterativeTaskInformation()
                    .display()
                    .end()
                .synchronizationInformation()
                    .display();

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.getResource())
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
        assertTask(TASK_IMPORT.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(10)
                .iterativeTaskInformation()
                    .display()
                    .assertSuccessCount(9)
                    .assertFailureCount(1)
                    .assertLastFailureObjectName("u-000001")
                    .end()
                .synchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 9, 0, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(null, null, null, null, 0, 1, 0) // Malformed account has no shadow, so no situation
                    .assertTransitions(2)
                    .end();

        // TODO assert redirected errors in the task
    }

    @Test
    public void test120ImportWithAllFailuresEnabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // This enables other kinds of failures (e.g. those in mappings)
        failuresEnabled = true;

        when();
        rerunTaskErrorsOk(TASK_IMPORT.oid, result);

        then();
        stabilize();
        assertTask(TASK_IMPORT.oid, "import task after")
                .display()
                .assertPartialError()
                .assertClosed()
                .assertProgress(10)
                .iterativeTaskInformation()
                    .display()
                    .assertSuccessCount(8)
                    .assertFailureCount(2)
                    .end()
                .synchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 8, 1, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(null, null, null, null, 0, 1, 0) // Malformed account has no shadow, so no situation
                    .assertTransitions(2);

        // TODO assert redirected errors in the task

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.getResource())
                .display();
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.getResource())
                .display();
    }

    @Test
    public void test130ReconciliationWithAllFailuresEnabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // 3rd account is already broken

        when();
        addObject(TASK_RECONCILIATION, task, result);
        waitForTaskFinish(TASK_RECONCILIATION.oid, builder -> builder.errorOk(true));

        then();
        stabilize();
        assertTask(TASK_RECONCILIATION.oid, "reconciliation task after")
                .display()
                .displayOperationResult()
                .assertPartialError()
                .assertClosed()
                .assertProgress(1) // may change in the future
                .iterativeTaskInformation()
                    .display()
                    .assertSuccessCount(8)
                    .assertFailureCount(3) // u-000003 failed once in 2nd part, and once in 3rd part
                    .assertLastFailureObjectName(MALFORMED_SHADOW_NAME)
                    .end()
                .synchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 8, 1, 0) // Those 9 records were already linked and remain so.
                    .assertTransition(null, null, null, null, 0, 1, 0) // Malformed account has no shadow, so no situation
                    ;
        //.assertTransitions(2);

        assertShadow(formatAccountName(IDX_GOOD_ACCOUNT), RESOURCE_DUMMY_SOURCE.getResource())
                .display();
        assertShadow(formatAccountName(IDX_PROJECTOR_FATAL_ERROR), RESOURCE_DUMMY_SOURCE.getResource())
                .display();
        // TODO assert redirected errors in the task
    }
}
