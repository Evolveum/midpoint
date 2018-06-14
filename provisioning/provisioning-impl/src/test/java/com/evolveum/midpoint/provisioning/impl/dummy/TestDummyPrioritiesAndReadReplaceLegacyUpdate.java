/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.icf.dummy.connector.DummyConnectorLegacyUpdate;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Same as TestDummyPrioritiesAndReadReplaceLegacyUpdate, but for a connector with legacy update methods.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyPrioritiesAndReadReplaceLegacyUpdate extends TestDummyPrioritiesAndReadReplace {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyPrioritiesAndReadReplaceLegacyUpdate.class);

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
		checkAttributesUpdated(updatesExecuted.get(0), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);		// prio 0, read-replace
		checkAttributesUpdated(updatesExecuted.get(1), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);			// prio 1, read-replace
		checkAttributesUpdated(updatesExecuted.get(2), "addAttributeValues", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME); // prio none, not read-replace
		checkAttributesUpdated(updatesExecuted.get(3), "update", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);	// prio none, read-replace + real replace
		checkAttributesUpdated(updatesExecuted.get(4), "removeAttributeValues", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);	 // prio none, not read-replace
	}

}
