/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PATH;

/**
 * Tests basic functionality of reconciliation tasks.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTask extends AbstractInitializedModelIntegrationTest {

    static final File TEST_DIR = new File("src/test/resources/tasks/recon");

    @SuppressWarnings("FieldCanBeLocal")
    private DummyInterruptedSyncResource interruptedSyncResource;

    private static final TestObject<TaskType> TASK_RECONCILIATION =
            TestObject.file(TEST_DIR, "task-reconciliation.xml", "1cf4e4fd-7648-4f83-bed4-78bd5d30d2a3");

    private static final DummyTestResource RESOURCE_DUMMY_HARSH = new DummyTestResource(
            TEST_DIR, "resource-dummy-harsh.xml", "faa1d45c-12bb-44d9-b157-bd99c786d39c", "harsh");
    private static final TestTask TASK_RECONCILIATION_HARSH =
            TestTask.file(TEST_DIR, "task-reconciliation-harsh.xml", "0a53b8f1-f91a-4a5b-a454-4985f73c3330");

    private static final String USER_FORMAT = "user-";

    private static final int USERS = 5;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        interruptedSyncResource = DummyInterruptedSyncResource.create(dummyResourceCollection, initTask, initResult);

        addObject(getReconciliationTask(), initTask, initResult, tailoringWorkerThreadsCustomizer(getWorkerThreads()));

        assertUsers(getNumberOfUsers());
        interruptedSyncResource.createAccounts(USERS, this::getUserName);

        RESOURCE_DUMMY_HARSH.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_HARSH.addAccount("account1");
        TASK_RECONCILIATION_HARSH.init(this, initTask, initResult);
    }

    TestObject<TaskType> getReconciliationTask() {
        return TASK_RECONCILIATION;
    }

    private String getReconciliationTaskOid() {
        return getReconciliationTask().oid;
    }

    // TODO seems to be unused
    private int getWorkerThreads() {
        return 0;
    }

    boolean isMultiNode() {
        return false;
    }

    private String getUserName(int i) {
        return String.format("%s%06d", USER_FORMAT, i);
    }

    @Test
    public void test100FullRun() throws Exception {
        when();

        runTaskTreeAndWaitForFinish(getReconciliationTaskOid(), 30000);

        then();

        assertTaskTree(getReconciliationTaskOid(), "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertObjectRef(DummyInterruptedSyncResource.OID, ResourceType.COMPLEX_TYPE); // MID-7312

        assertPerformance(getReconciliationTaskOid(), "after")
                .display()
                .child(ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_ID)
                    .assertProgress(0)
                    .assertItemsProcessed(0)
                .end()
                .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertProgress(USERS)
                    .assertItemsProcessed(USERS)
                .end()
                .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID)
                    .assertProgress(0)
                    .assertItemsProcessed(0)
                .end();
    }

    /** Accounts that are deleted because of sync reaction should be reported correctly in the task. MID-9217. */
    @Test
    public void test200ReportingOnDeletedAccounts() throws Exception {
        skipTestIf(isMultiNode(), "It is sufficient to run this test once");

        var task = getTestTask();
        var result = task.getResult();

        when("harsh resource is reconciled");
        TASK_RECONCILIATION_HARSH.rerun(result);

        then("actions executed are correct");
        TASK_RECONCILIATION_HARSH.assertAfter()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                .actionsExecuted()
                .all()
                .display()
                .assertFailureCount(ChangeTypeType.MODIFY, ShadowType.COMPLEX_TYPE, 0, 0);
    }
}
