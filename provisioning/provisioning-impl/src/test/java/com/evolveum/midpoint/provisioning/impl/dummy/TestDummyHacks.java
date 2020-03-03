/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * The test of Provisioning service on the API level. The test is using dummy resource for speed and flexibility.
 *
 * This is a test for resource with hacks and various dirty solutions.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyHacks extends TestDummy {

    private static final File TEST_DIR = new File(AbstractDummyTest.TEST_DIR_DUMMY, "dummy-hacks");

    private static final File CONNECTOR_DUMMY_FILE = new File(TEST_DIR, "connector-dummy.xml");

    private static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";

    private static final Trace LOGGER = TraceManager.getTrace(TestDummyHacks.class);

    private PrismObject<ConnectorType> connector;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isPreFetchResource() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // DO NOT DO provisioningService.postInit(..) yet
        // We want to avoid connector discovery and insert our own connector object
//        provisioningService.postInit(initResult);

        connector = repoAddObjectFromFile(CONNECTOR_DUMMY_FILE, initResult);

        super.initSystem(initTask, initResult);
    }

    @Override
    protected void assertResourceAfterTest() {
        // The useless configuration variables should be reflected to the resource now
        assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
        assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());
    }

    @Override
    protected void assertWillDummyGossipRecord(PlusMinusZero plusminus, String... expectedValues) {
        // This will not really work here. The attributeContentRequirement will ruin it.
    }

}
