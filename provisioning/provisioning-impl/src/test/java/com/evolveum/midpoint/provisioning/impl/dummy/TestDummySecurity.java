/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummySecurity extends AbstractDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummySecurity.class);
	private String willIcfUid;

	@Test
	public void test100AddAccountDrink() throws Exception {
		final String TEST_NAME = "test100AddAccountDrink";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_WILL_FILE);
		account.checkConsistence();

		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, syncTask, result);

			AssertJUnit.fail("Unexpected success");

		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

	}

	private <T> void setAttribute(PrismObject<ShadowType> account, String attrName, T val) throws SchemaException {
		PrismContainer<Containerable> attrsCont = account.findContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttribute<T> attr = new ResourceAttribute<T>(
				dummyResourceCtl.getAttributeQName(attrName), null, prismContext);
		attr.setRealValue(val);
		attrsCont.add(attr);
	}

	@Test
	public void test199AddAccount() throws Exception {
		final String TEST_NAME = "test199AddAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_WILL_FILE);
		account.checkConsistence();

		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Eunuch");

		display("Adding shadow", account);

		// WHEN
		provisioningService.addObject(account, null, null, syncTask, result);

		// THEN
		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, syncTask, result);
		display("Account provisioning", accountProvisioning);
		willIcfUid = getIcfUid(accountProvisioning);

	}

	@Test
	public void test200ModifyAccountDrink() throws Exception {
		final String TEST_NAME = "test200ModifyAccountDrink";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME),
				prismContext, "RUM");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");

		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test201ModifyAccountGossip() throws Exception {
		final String TEST_NAME = "test201ModifyAccountGossip";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
				prismContext, "pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "pirate");

		syncServiceMock.assertNotifySuccessOnly();
	}

	@Test
	public void test210ModifyAccountQuote() throws Exception {
		final String TEST_NAME = "test210ModifyAccountQuote";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
				prismContext, "eh?");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		try {
			// WHEN
			provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
					new OperationProvisioningScriptsType(), null, task, result);

			AssertJUnit.fail("Unexpected success");

		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
	}

	@Test
	public void test300GetAccount() throws Exception {
		final String TEST_NAME = "test300GetAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, result);

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountWill(shadow, result);

		checkConsistency(shadow);
	}

	@Test
	public void test310SearchAllShadows() throws Exception {
		final String TEST_NAME = "test310SearchAllShadows";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, null, null, result);

		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		TestUtil.assertSuccess(result);

		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());

		checkConsistency(allShadows);

		for (PrismObject<ShadowType> shadow: allShadows) {
			assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
			assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
		}

		assertEquals("Wrong number of results", 2, allShadows.size());
	}

	// TODO: search

	private void checkAccountWill(PrismObject<ShadowType> shadow, OperationResult result) {
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Will Turner");
		assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
		assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
		assertEquals("Unexpected number of attributes", 8, attributes.size());
	}
}
