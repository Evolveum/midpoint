/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.icf.dummy.connector.DummyConnectorLegacyUpdate;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Same as TestDummyPrioritiesAndReadReplaceLegacyUpdate, but for a connector with legacy update methods.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyPrioritiesAndReadReplaceLegacyUpdate extends TestDummyPrioritiesAndReadReplace {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-legacy-update.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected String getDummyConnectorType() {
        return IntegrationTestTools.DUMMY_CONNECTOR_LEGACY_UPDATE_TYPE;
    }

    @Override
    protected Class<?> getDummyConnectorClass() {
        return  DummyConnectorLegacyUpdate.class;
    }

    @Override
    protected void assertTest123ModifyObjectReplaceResult(OperationResult result) {
        // BEWARE: very brittle!
        List<OperationResult> updatesExecuted = TestUtil.selectSubresults(result, ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".update");
        assertEquals("Wrong number of updates executed", 3, updatesExecuted.size());
        checkAttributesUpdated(updatesExecuted.get(0), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
        checkAttributesUpdated(updatesExecuted.get(1), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
        checkAttributesUpdated(updatesExecuted.get(2), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
    }

    @Override
    protected void assertTest150ModifyObjectAddDeleteResult(OperationResult result) {
        // BEWARE: very brittle!
        List<OperationResult> updatesExecuted = TestUtil.selectSubresults(result,
                ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".update",
                ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".addAttributeValues",
                ProvisioningTestUtil.CONNID_CONNECTOR_FACADE_CLASS_NAME + ".removeAttributeValues");
        assertEquals("Wrong number of updates executed", 5, updatesExecuted.size());
        checkAttributesUpdated(updatesExecuted.get(0), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);        // prio 0, read-replace
        checkAttributesUpdated(updatesExecuted.get(1), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);            // prio 1, read-replace
        checkAttributesUpdated(updatesExecuted.get(2), "addAttributeValues", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME); // prio none, not read-replace
        checkAttributesUpdated(updatesExecuted.get(3), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);    // prio none, read-replace + real replace
        checkAttributesUpdated(updatesExecuted.get(4), "removeAttributeValues", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);     // prio none, not read-replace
    }

}
