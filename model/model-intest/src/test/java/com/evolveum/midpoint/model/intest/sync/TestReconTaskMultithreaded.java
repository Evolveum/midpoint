/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.FileNotFoundException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * The same as TestReconTask but this one uses partitioned reconciliation task handler.
 * I.e. each reconciliation task is divided into three subtasks (for stage 1, 2, 3).
 *
 * Cannot be run under H2 because of too much contention.
 * Also, it takes a little longer than standard TestReconTask because of the overhead.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTaskMultithreaded extends TestReconTask {

    private static final String TASK_RECONCILE_DUMMY_MULTITHREADED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-multithreaded.xml";
    private static final String TASK_RECONCILE_DUMMY_MULTITHREADED_OID = "74d4297d-cdeb-43e6-a7f9-0af38d36de12";

    private static final String TASK_RECONCILE_DUMMY_BLUE_MULTITHREADED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-blue-multithreaded.xml";
    private static final String TASK_RECONCILE_DUMMY_BLUE_MULTITHREADED_OID = "6aea358b-2043-42fa-b2db-1cfdccb26725";

    private static final String TASK_RECONCILE_DUMMY_GREEN_MULTITHREADED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-green-multithreaded.xml";
    private static final String TASK_RECONCILE_DUMMY_GREEN_MULTITHREADED_OID = "36a53692-3324-443e-a683-3c23dd48a276";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        taskManager.setFreeBucketWaitInterval(100L);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_GREEN_MULTITHREADED_FILENAME);
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_BLUE_MULTITHREADED_FILENAME);
        } else if (resource == getDummyResourceObject()) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_MULTITHREADED_FILENAME);
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            return TASK_RECONCILE_DUMMY_GREEN_MULTITHREADED_OID;
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            return TASK_RECONCILE_DUMMY_BLUE_MULTITHREADED_OID;
        } else if (resource == getDummyResourceObject()) {
            return TASK_RECONCILE_DUMMY_MULTITHREADED_OID;
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }
}
