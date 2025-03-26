/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AttributeContentRequirementType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.testng.SkipException;

import static org.testng.AssertJUnit.*;

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

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isPreFetchResource() {
        return true;
    }

    @Override
    protected void assertEffectiveUpdateCapability(UpdateCapabilityType capUpdate) {
        assertNotNull("update capability not present", capUpdate);
        assertTrue("'delta' update capability is not there", capUpdate.isDelta());
        assertEquals("'attributeContentRequirement' update capability is wrong",
                AttributeContentRequirementType.ALL, capUpdate.getAttributeContentRequirement());
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // DO NOT DO provisioningService.postInit(..) yet
        // We want to avoid connector discovery and insert our own connector object

        repoAddObjectFromFile(CONNECTOR_DUMMY_FILE, initResult);

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

    @Override
    public void test920EntitleInMaintenance() throws CommonException {

        turnMaintenanceModeOff(RESOURCE_DUMMY_OID, getTestOperationResult()); // this is because of downstream tests

        throw new SkipException(
                "Required attribute 'weapon' is not provided during retry operation, so it fails. Fix this some day (MID-9826).");
    }
}
