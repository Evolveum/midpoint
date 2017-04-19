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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNegative extends AbstractDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyNegative.class);
	
	private static final String ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILENAME = TEST_DIR + "account-elaine-resource-not-found.xml";
	
	@Test
	public void test110GetResourceBrokenSchemaNetwork() throws Exception {
		testGetResourceBrokenSchema(BreakMode.NETWORK, "test110GetResourceBrokenSchemaNetwork");
	}
	
	@Test
	public void test111GetResourceBrokenSchemaGeneric() throws Exception {
		testGetResourceBrokenSchema(BreakMode.GENERIC, "test111GetResourceBrokenSchemaGeneric");
	}
	
	@Test
	public void test112GetResourceBrokenSchemaIo() throws Exception {
		testGetResourceBrokenSchema(BreakMode.IO, "test112GetResourceBrokenSchemaIO");
	}
	
	@Test
	public void test113GetResourceBrokenSchemaRuntime() throws Exception {
		testGetResourceBrokenSchema(BreakMode.RUNTIME, "test113GetResourceBrokenSchemaRuntime");
	}
	
	public void testGetResourceBrokenSchema(BreakMode breakMode, String testName) throws Exception {
		TestUtil.displayTestTile(testName);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ "."+testName);
		
		// precondition
		PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Repo resource (before)", repoResource);
		PrismContainer<Containerable> schema = repoResource.findContainer(ResourceType.F_SCHEMA);
		assertTrue("Schema found in resource before the test (precondition)", schema == null || schema.isEmpty());

		dummyResource.setSchemaBreakMode(breakMode);
		try {
			
			// WHEN
			PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, null, result);
			
			// THEN
			display("Resource with broken schema", resource);
			OperationResultType fetchResult = resource.asObjectable().getFetchResult();
			
			result.computeStatus();
			display("getObject result", result);
			assertEquals("Unexpected result of getObject operation", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
			
			assertNotNull("No fetch result", fetchResult);
			display("fetchResult", fetchResult);
			assertEquals("Unexpected result of fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
			
		} finally {
			dummyResource.setSchemaBreakMode(BreakMode.NONE);
		}
	}
	
	
	@Test
	public void test200AddAccountNullAttributes() throws Exception {
		final String TEST_NAME = "test200AddAccountNullAttributes";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, ShadowType.class);
		PrismObject<ShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		account.removeContainer(ShadowType.F_ATTRIBUTES);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		syncServiceMock.assertNotifyFailureOnly();
	}
	
	@Test
	public void test201AddAccountEmptyAttributes() throws Exception {
		TestUtil.displayTestTile("test201AddAccountEmptyAttributes");
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyNegative.class.getName()
				+ ".test201AddAccountEmptyAttributes");
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test201AddAccountEmptyAttributes");
		syncServiceMock.reset();

		ShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, ShadowType.class);
		PrismObject<ShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		account.findContainer(ShadowType.F_ATTRIBUTES).getValue().clear();

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		syncServiceMock.assertNotifyFailureOnly();
	}
	
	@Test
	public void test210AddAccountNoObjectclass() throws Exception {
		TestUtil.displayTestTile("test210AddAccountNoObjectclass");
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyNegative.class.getName()
				+ ".test210AddAccountNoObjectclass");
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test210AddAccountNoObjectclass");
		syncServiceMock.reset();

		ShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, ShadowType.class);
		PrismObject<ShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		// IMPORTANT: deliberately violating the schema
		accountType.setObjectClass(null);
		accountType.setKind(null);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

		syncServiceMock.assertNotifyFailureOnly();
	}
	
	@Test
	public void test220AddAccountNoResourceRef() throws Exception {
		final String TEST_NAME = "test220AddAccountNoResourceRef";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyNegative.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, ShadowType.class);
		PrismObject<ShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		accountType.setResourceRef(null);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, task, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

		//FIXME: not sure, if this check is needed..if the reosurce is not specified, provisioning probably will be not called. 
//		syncServiceMock.assertNotifyFailureOnly();
	}
	
	@Test
	public void test221DeleteAccountResourceNotFound() throws Exception {
		final String TEST_NAME = "test221DeleteAccountResourceNotFound";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyNegative.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ShadowType accountType = parseObjectTypeFromFile(ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILENAME, ShadowType.class);
		PrismObject<ShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
//		accountType.setResourceRef(null);

		display("Adding shadow", account);

		try {
			// WHEN
			String oid = repositoryService.addObject(account, null, result);
			ProvisioningOperationOptions options = ProvisioningOperationOptions.createForce(true);
			provisioningService.deleteObject(ShadowType.class, oid, options, null, task, result);
//			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

		//FIXME: is this really notify failure? the resource does not exist but shadow is deleted. maybe other case of notify?
//		syncServiceMock.assertNotifyFailureOnly();
	}

	/**
	 * Try to get an account when a shadow has been deleted (but the account exists). 
	 * Proper ObjectNotFoundException is expected, compensation should not run. 
	 */
	@Test
	public void test230GetAccountDeletedShadow() throws Exception {
		final String TEST_NAME = "test230GetAccountDeletedShadow";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyNegative.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_MORGAN_FILE);
		String shadowOid = provisioningService.addObject(account, null, null, task, result);
		
		repositoryService.deleteObject(ShadowType.class, shadowOid, result);
		
		// reset
		task = taskManager.createTaskInstance(TestDummyNegative.class.getName() + "." + TEST_NAME);
		result = task.getResult();
		syncServiceMock.reset();

		try {
			// WHEN
			provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
			
			AssertJUnit.fail("Unexpected success");
		} catch (ObjectNotFoundException e) {
			// this is expected
			display("Expected exception", e);
			result.computeStatus();
			display("Result", result);
			TestUtil.assertFailure(result);
			
		}

		syncServiceMock.assertNoNotifyChange();
	}

}
