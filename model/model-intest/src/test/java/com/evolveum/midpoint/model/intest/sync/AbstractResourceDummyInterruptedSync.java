/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.test.DummyResourceCollection;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;

/**
 * Wrapper for specific configured dummy resource.
 *
 * Unfinished. EXPERIMENTAL.
 */
abstract class AbstractResourceDummyInterruptedSync {

    DummyResourceContoller controller;

    abstract void init(DummyResourceCollection collection, Task task, OperationResult result) throws Exception;

    DummyResource getDummyResource() {
        return controller.getDummyResource();
    }

    DummyResourceContoller getController() {
        return controller;
    }
}
