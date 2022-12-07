/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import java.io.File;

public class AbstractSimulationsTest extends AbstractEmptyModelIntegrationTest {

    private static final File SIM_TEST_DIR = new File("src/test/resources/simulation");

    static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_TARGET = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-target.xml",
            "3f8d6dee-9663-496f-a718-b3c27234aca7",
            "simple-production-target");
    private static final DummyTestResource RESOURCE_SIMPLE_PRODUCTION_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-production-source.xml",
            "c6caaa46-96c4-4244-883f-2771e18b82c9",
            "simple-production-source");
    private static final DummyTestResource RESOURCE_SIMPLE_DEVELOPMENT_SOURCE = new DummyTestResource(
            SIM_TEST_DIR,
            "resource-simple-development-source.xml",
            "6d8ba4fd-95ee-4d98-80c2-3a194b566f89",
            "simple-development-source");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SIMPLE_PRODUCTION_TARGET.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.initAndTest(this, initTask, initResult);
        RESOURCE_SIMPLE_DEVELOPMENT_SOURCE.initAndTest(this, initTask, initResult);
    }
}
