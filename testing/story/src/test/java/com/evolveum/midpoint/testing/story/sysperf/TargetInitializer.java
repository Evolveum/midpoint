/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGETS_CONFIGURATION;

public class TargetInitializer {

    private final TestSystemPerformance test;
    private final List<DummyTestResource> resources;
    private final Task initTask;

    TargetInitializer(TestSystemPerformance test, List<DummyTestResource> resources, Task initTask) {
        this.test = test;
        this.resources = resources;
        this.initTask = initTask;
    }

    public void run(OperationResult result) throws Exception {
        for (DummyTestResource resource : resources) {
            test.initDummyResource(resource, initTask, result);
            TARGETS_CONFIGURATION.getOperationDelay().applyTo(resource);
        }
    }
}
