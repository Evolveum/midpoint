/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.test.DummyResourceCollection;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.File;

/**
 * Configuration and behavior-controlling data for resource-dummy-interrupted-sync resources.
 * EXPERIMENTAL
 */
public class DummyInterruptedSyncResource extends AbstractResourceDummyInterruptedSync {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final File FILE = new File(TEST_DIR, "resource-dummy-interrupted-sync.xml");
    private static final String OID = "7a58233a-1cfb-46d1-a404-08cdf4626ebb";
    private static final String NAME = "interruptedSync";

    @Override
    void init(DummyResourceCollection collection, Task task, OperationResult result) throws Exception {
        controller = collection.initDummyResource(NAME, FILE, OID, null, task, result);
        controller.setSyncStyle(DummySyncStyle.DUMB);
    }

    // behavior control, referenced from Groovy code in the resource

    public static long delay = 1;
    public static String errorOn = null;
}
