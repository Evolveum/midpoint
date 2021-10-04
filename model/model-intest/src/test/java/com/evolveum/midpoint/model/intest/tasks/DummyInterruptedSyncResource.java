/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.function.Function;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.test.DummyResourceCollection;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Configuration and behavior-controlling data for resource-dummy-interrupted-sync resources.
 * EXPERIMENTAL
 */
public class DummyInterruptedSyncResource extends AbstractResourceDummyInterruptedSync {

    private static final File TEST_DIR = new File("src/test/resources/tasks/livesync"); // TODO

    private static final File FILE = new File(TEST_DIR, "resource-dummy-interrupted-sync.xml");
    static final String OID = "7a58233a-1cfb-46d1-a404-08cdf4626ebb";
    private static final String NAME = "interruptedSync";

    public static DummyInterruptedSyncResource create(DummyResourceCollection collection, Task task, OperationResult result)
            throws Exception {
        DummyInterruptedSyncResource resource = new DummyInterruptedSyncResource();
        resource.init(collection, task, result);
        return resource;
    }

    @Override
    public void init(DummyResourceCollection collection, Task task, OperationResult result) throws Exception {
        controller = collection.initDummyResource(NAME, FILE, OID, null, task, result);
        controller.setSyncStyle(DummySyncStyle.DUMB);
    }

    // behavior control, referenced from Groovy code in the resource

    public static long delay;
    public static String errorOn;
    private static Runnable executionListener;

    static {
        reset();
    }

    public static void reset() {
        delay = 1;
        errorOn = null;
        executionListener = null;
    }

    public void createAccounts(int users, Function<Integer, String> userNameFormatter) throws Exception {
        for (int i = 0; i < users; i++) {
            controller.addAccount(userNameFormatter.apply(i));
        }
    }

    /**
     * Called from inbound mapping.
     */
    public static void onMappingExecution() {
        if (executionListener != null) {
            executionListener.run();
        }
    }

    public static void setExecutionListener(Runnable executionListener) {
        DummyInterruptedSyncResource.executionListener = executionListener;
    }
}
