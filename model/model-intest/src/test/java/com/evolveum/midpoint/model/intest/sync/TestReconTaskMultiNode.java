/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.FileNotFoundException;

/**
 * The same as TestReconTaskPartitioned but the second partition (resource reconciliation) is executed in a set of worker tasks.
 * (Currently there is only a single bucket, but multiple bucket processing will be implemented shortly.)
 *
 * Cannot be run under H2 because of too much contention.
 * Also, it takes a little longer than standard TestReconTask because of the overhead.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTaskMultiNode extends TestReconTaskPartitioned {

    protected static final String TASK_RECONCILE_DUMMY_MULTINODE_FILENAME = COMMON_DIR + "/task-reconcile-dummy-multinode.xml";
    protected static final String TASK_RECONCILE_DUMMY_MULTINODE_OID = "10000000-0000-0000-565M-565600000004";

    protected static final String TASK_RECONCILE_DUMMY_BLUE_MULTINODE_FILENAME = COMMON_DIR + "/task-reconcile-dummy-blue-multinode.xml";
    protected static final String TASK_RECONCILE_DUMMY_BLUE_MULTINODE_OID = "10000000-0000-0000-565M-565600000204";

    protected static final String TASK_RECONCILE_DUMMY_GREEN_MULTINODE_FILENAME = COMMON_DIR + "/task-reconcile-dummy-green-multinode.xml";
    protected static final String TASK_RECONCILE_DUMMY_GREEN_MULTINODE_OID = "10000000-0000-0000-565M-565600000404";

    @SuppressWarnings("Duplicates")
    @Override
    protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_GREEN_MULTINODE_FILENAME);
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_BLUE_MULTINODE_FILENAME);
        } else if (resource == getDummyResourceObject()) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_MULTINODE_FILENAME);
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            return TASK_RECONCILE_DUMMY_GREEN_MULTINODE_OID;
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            return TASK_RECONCILE_DUMMY_BLUE_MULTINODE_OID;
        } else if (resource == getDummyResourceObject()) {
            return TASK_RECONCILE_DUMMY_MULTINODE_OID;
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }

    protected int getWaitTimeout() {
        return 300000;
    }
}
