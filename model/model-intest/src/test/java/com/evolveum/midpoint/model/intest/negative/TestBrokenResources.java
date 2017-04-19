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
package com.evolveum.midpoint.model.intest.negative;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBrokenResources extends AbstractConfiguredModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/negative";
	private static final String TEST_TARGET_DIR = "target/test/negative";
	
	private static final File CONNECTOR_DUMMY_NOJARS_FILE = new File (TEST_DIR, "connector-dummy-nojars.xml");
	private static final String CONNECTOR_DUMMY_NOJARS_OID = "cccccccc-cccc-cccc-cccc-666600660004";

	private static final String RESOURCE_CSVFILE_BROKEN_FILENAME = TEST_DIR + "/resource-csvfile-broken.xml";
	private static final String RESOURCE_CSVFILE_BROKEN_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3bbbb";
	
	private static final String RESOURCE_CSVFILE_NOTFOUND_FILENAME = TEST_DIR + "/resource-csvfile-notfound.xml";
	private static final String RESOURCE_CSVFILE_NOTFOUND_OID = "ef2bc95b-76e0-48e2-86d6-f0f002d3f0f0";
	
	private static final String RESOURCE_DUMMY_NOJARS_FILENAME = TEST_DIR + "/resource-dummy-nojars.xml";
	private static final String RESOURCE_DUMMY_NOJARS_OID = "10000000-0000-0000-0000-666600660004";

	private static final File RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE = new File (TEST_DIR, "resource-dummy-wrong-connector-oid.xml");
	private static final String RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID = "10000000-0000-0000-0000-666600660005";

	private static final File RESOURCE_DUMMY_NO_CONFIGURATION_FILE = new File (TEST_DIR, "resource-dummy-no-configuration.xml");
	private static final String RESOURCE_DUMMY_NO_CONFIGURATION_OID = "10000000-0000-0000-0000-666600660006";
	
	private static final File RESOURCE_DUMMY_UNACCESSIBLE_FILE = new File (TEST_DIR, "resource-dummy-unaccessible.xml");
	private static final String RESOURCE_DUMMY_UNACCESSIBLE_NAME = "unaccessible";
	private static final String RESOURCE_DUMMY_UNACCESSIBLE_OID = "10000000-0000-0000-0000-666600660007";

	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-jack-csvfile.xml";
	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120001";
	
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-murray-csvfile.xml";
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120666";
	
	private static final String BROKEN_CSV_FILE_NAME = "broken.csv";
	private static final String BROKEN_CSV_SOURCE_FILE_NAME = TEST_DIR + "/" + BROKEN_CSV_FILE_NAME;
	private static final String BROKEN_CSV_TARGET_FILE_NAME = TEST_TARGET_DIR + "/" + BROKEN_CSV_FILE_NAME;
	
	protected static final Trace LOGGER = TraceManager.getTrace(TestBrokenResources.class);
			
	protected UserType userTypeJack;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		LOGGER.trace("initSystem");
		
		// Resources
		File targetDir = new File(TEST_TARGET_DIR);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		MiscUtil.copyFile(new File(BROKEN_CSV_SOURCE_FILE_NAME), new File(BROKEN_CSV_TARGET_FILE_NAME));
		
		repoAddObjectFromFile(CONNECTOR_DUMMY_NOJARS_FILE, initResult);
		
		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_UNACCESSIBLE_NAME, null, null, initTask, initResult);
		
		importObjectFromFile(RESOURCE_CSVFILE_BROKEN_FILENAME, initResult);
		importObjectFromFile(RESOURCE_CSVFILE_NOTFOUND_FILENAME, initResult);
		importObjectFromFile(RESOURCE_DUMMY_NOJARS_FILENAME, initResult);

		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
		
		// Accounts
		repoAddObjectFromFile(ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME, initResult);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult).asObjectable();
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
	}
	
	@Override
	public void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
		super.postInitSystem(initTask, initResult);
		// Break it only after resource are reset in super.postInitSystem()
		getDummyResource(RESOURCE_DUMMY_UNACCESSIBLE_NAME).setBreakMode(BreakMode.NETWORK);
	}
	
	@Test
    public void test010TestResourceBroken() throws Exception {
		final String TEST_NAME = "test010TestResourceBroken";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_BROKEN_OID, task);
		
		// THEN
		display("testResource result", testResult);
        TestUtil.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test020GetResourceBroken() throws Exception {
		final String TEST_NAME = "test020GetResourceBroken";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess("getObject result", result);
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		TestUtil.assertSuccess("resource.fetchResult", fetchResult);
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}
	
	@Test
    public void test030ListResources() throws Exception {
		final String TEST_NAME = "test030ListResources";
		testListResources(TEST_NAME, 4, null);
	}

	@Test
    public void test100GetAccountMurray() throws Exception {
		final String TEST_NAME = "test100GetAccountMurray";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        try {

        	// WHEN
	        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
	        		null, task, result);

	        display("Account (unexpected)", account);
	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
        	display("Expected exception", e);
        	result.computeStatus();
    		display("getObject result", result);
            TestUtil.assertFailure("getObject result", result);
        }
		
	}
	
	@Test
    public void test101GetAccountMurrayNoFetch() throws Exception {
		final String TEST_NAME = "test101GetAccountMurrayNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        
		// WHEN
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
        		options, task, result);

        display("getObject account", account);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess("getObject result", result);
        // TODO: better asserts
		assertNotNull("Null resource", account);
	}
	
	@Test
    public void test102GetAccountMurrayRaw() throws Exception {
        TestUtil.displayTestTile(this, "test102GetAccountMurrayRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test102GetAccountMurrayRaw");
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        
		// WHEN
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
        		options, task, result);

        display("getObject account", account);
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess("getObject result", result);
        // TODO: better asserts
		assertNotNull("Null resource", account);
	}

	
	@Test
    public void test120SearchAccountByUsernameJack() throws Exception {
        TestUtil.displayTestTile(this, "test120SearchAccountByUsernameJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test120SearchAccountByUsernameJack");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);
        
        try {

        	// WHEN
	        PrismObject<ShadowType> account = findAccountByUsername("jack", resource, task, result);

	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
        	result.computeStatus();
    		display("findAccountByUsername result", result);
            TestUtil.assertFailure("findAccountByUsername result", result);
        }
		
	}
	
	@Test
    public void test210TestResourceNotFound() throws Exception {
        TestUtil.displayTestTile(this, "test210TestResourceNotFound");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test210TestResourceNotFound");
        OperationResult result = task.getResult();
        
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_NOTFOUND_OID, task);
		
		// THEN
		display("testResource result", testResult);
        TestUtil.assertFailure("testResource result", testResult);
	}
	
	@Test
    public void test220GetResourceNotFound() throws Exception {
		final String TEST_NAME = "test220GetResourceNotFound";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "."+TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_NOTFOUND_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}
	
	@Test
    public void test221GetResourceNotFoundResolveConnector() throws Exception {
		final String TEST_NAME = "test221GetResourceNotFoundResolveConnector";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "."+TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
        		ResourceType.F_CONNECTOR_REF, GetOperationOptions.createResolve());
		
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_NOTFOUND_OID, options, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
		
		assertNotNull("Connector was not resolved", resource.asObjectable().getConnector());
	}
		
	
	@Test
    public void test310TestResourceNoJars() throws Exception {
        TestUtil.displayTestTile(this, "test310TestResourceNoJars");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + ".test310TestResourceNoJars");
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_NOJARS_OID, task);
		
		// THEN
		display("testResource result", testResult);
        TestUtil.assertFailure("testResource result", testResult);
	}
	
	@Test
    public void test320GetResourceNoJars() throws Exception {
		final String TEST_NAME = "test320GetResourceNoJars";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NOJARS_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}

	@Test
    public void test350AddResourceWrongConnectorOid() throws Exception {
		final String TEST_NAME = "test350AddResourceWrongConnectorOid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);
		ObjectDelta<ResourceType> delta = ObjectDelta.createAddDelta(resource);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		try {
			// WHEN
	        modelService.executeChanges(deltas, null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertFailure(result);
	}

	/**
	 * Even "raw" add should fail. No connector object means no connector schema which means no
	 * definitions for configuration properties which means we are not able to store them.
	 */
	@Test
    public void test352AddResourceWrongConnectorOidRaw() throws Exception {
		final String TEST_NAME = "test352AddResourceWrongConnectorOidRaw";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);
		ObjectDelta<ResourceType> delta = ObjectDelta.createAddDelta(resource);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		try {
			// WHEN
	        modelService.executeChanges(deltas, null, task, result);
	        
	        AssertJUnit.fail("Unexpected success");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		// THEN
		result.computeStatus();
		display(result);
		TestUtil.assertFailure(result);
	}
	

	/**
	 * Store directly to repo. This is not really a test, it is more like a hack to prepare
	 * environment for next tests.
	 */
	@Test
    public void test355AddResourceWrongConnectorOidRepo() throws Exception {
		final String TEST_NAME = "test355AddResourceWrongConnectorOidRepo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_WRONG_CONNECTOR_OID_FILE);
		
		// WHEN
		repositoryService.addObject(resource, null, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);				
	}

	@Test
    public void test358GetResourceWrongConnectorOid() throws Exception {
		final String TEST_NAME = "test358GetResourceWrongConnectorOid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}

	@Test
    public void test359DeleteResourceWrongConnectorOid() throws Exception {
		final String TEST_NAME = "test359DeleteResourceWrongConnectorOid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		ObjectDelta<ResourceType> delta = ObjectDelta.createDeleteDelta(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		assertNoObject(ResourceType.class, RESOURCE_DUMMY_WRONG_CONNECTOR_OID_OID, task, result);
	}

	@Test
    public void test360AddResourceNoConfiguration() throws Exception {
		final String TEST_NAME = "test360AddResourceNoConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_NO_CONFIGURATION_FILE);
        PrismObject<ConnectorType> connectorDummy = findConnectorByTypeAndVersion(CONNECTOR_DUMMY_TYPE, CONNECTOR_DUMMY_VERSION, result);
        resource.asObjectable().getConnectorRef().setOid(connectorDummy.getOid());

        ObjectDelta<ResourceType> delta = ObjectDelta.createAddDelta(resource);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
	        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	@Test
    public void test362GetResourceNoConfiguration() throws Exception {
		final String TEST_NAME = "test362GetResourceNoConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}

	@Test
    public void test368ListResources() throws Exception {
		final String TEST_NAME = "test368ListResources";
		testListResources(TEST_NAME, 5, null);
	}
	
	@Test
    public void test369DeleteResourceNoConfiguration() throws Exception {
		final String TEST_NAME = "test369DeleteResourceNoConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		ObjectDelta<ResourceType> delta = ObjectDelta.createDeleteDelta(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		assertNoObject(ResourceType.class, RESOURCE_DUMMY_NO_CONFIGURATION_OID, task, result);
	}

	@Test
    public void test370ListResources() throws Exception {
		final String TEST_NAME = "test370ListResources";
		testListResources(TEST_NAME, 4, null);
	}

	@Test
    public void test371ImportUnaccessibleResource() throws Exception {
		final String TEST_NAME = "test371ImportUnaccessibleResource";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        importObjectFromFile(RESOURCE_DUMMY_UNACCESSIBLE_FILE, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	/**
	 * No fetch operation should NOT try to read the schema.
	 * MID-3509
	 */
	@Test
    public void test372GetUnaccessibleResourceNoFetch() throws Exception {
		final String TEST_NAME = "test372GetUnaccessibleResourceNoFetch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        rememberResourceSchemaFetchCount();
        rememberConnectorInitializationCount();
        rememberConnectorOperationCount();
        rememberConnectorSchemaParseCount();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UNACCESSIBLE_OID, 
        		GetOperationOptions.createNoFetchCollection(), task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		display("Resource after", resource);
		assertNotNull("No resource", resource);
		
		assertResourceSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertConnectorOperationIncrement(0);
		assertConnectorSchemaParseIncrement(0);
	}
	
	/**
	 * No fetch operation should NOT try to read the schema.
	 * MID-3509
	 */
	@Test
    public void test374ListResourcesNoFetch() throws Exception {
		final String TEST_NAME = "test374ListResourcesNoFetch";
		
		rememberResourceSchemaFetchCount();
        rememberConnectorInitializationCount();
        rememberConnectorOperationCount();
        rememberConnectorSchemaParseCount();
		
		testListResources(TEST_NAME, 5, GetOperationOptions.createNoFetchCollection());
		
		assertResourceSchemaFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertConnectorOperationIncrement(0);
		assertConnectorSchemaParseIncrement(0);
	}
	
	@Test
    public void test375ListResources() throws Exception {
		final String TEST_NAME = "test375ListResources";
		testListResources(TEST_NAME, 5, null);
	}
	
	@Test
    public void test377GetResourceNoConfiguration() throws Exception {
		final String TEST_NAME = "test377GetResourceNoConfiguration";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_UNACCESSIBLE_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected partial errror in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		display("resource.fetchResult", fetchResult);
		assertEquals("Expected partial errror in fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
		
		assertNotNull("Null resource", resource);
	}
	
	/**
	 * Assign two resources to a user. One of them is looney, the other is not. The result should be that
	 * the account on the good resource is created.
	 * 
	 * This one dies on the lack of schema.
	 */
	// MID-1248
	@Test
    public void test400AssignTwoResouresNotFound() throws Exception {
		testAssignTwoResoures("test400AssignTwoResoures", RESOURCE_CSVFILE_NOTFOUND_OID);
	}

	/**
	 * Assign two resources to a user. One of them is looney, the other is not. The result should be that
	 * the account on the good resource is created.
	 * 
	 * This one dies on connector error.
	 */
	@Test
    public void test401AssignTwoResouresBroken() throws Exception {
		testAssignTwoResoures("test401AssignTwoResouresBroken", RESOURCE_CSVFILE_BROKEN_OID);
	}
	
	/**
	 * Assign two resources to a user. One of them is looney, the other is not. The result should be that
	 * the account on the good resource is created.
	 */
	private void testAssignTwoResoures(final String TEST_NAME, String badResourceOid) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, badResourceOid, null, true);
        userDelta.addModification(createAccountAssignmentModification(RESOURCE_DUMMY_OID, null, true));
        display("input delta", userDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
		display("executeChanges result", result);
		
		//TODO: ugly hack, see MID-1248 
		if ("test401AssignTwoResouresBroken".equals(TEST_NAME)){
			assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		} 
			
        DummyAccount jackDummyAccount = getDummyResource().getAccountByUsername(USER_JACK_USERNAME);
        assertNotNull("No jack dummy account", jackDummyAccount);
	}

	public void testListResources(final String TEST_NAME, int expectedNumber, Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN (1)
        Task task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN (1)
		final SearchResultList<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, null, options, task, result);
		
		// THEN (1)
		result.computeStatus();
		display("getObject result", result);
		if (options == null) {
			assertEquals("Expected partial error (search)", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		} else if (GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options))) {
			TestUtil.assertSuccess(result);
		} else {
			AssertJUnit.fail("unexpected");
		}
		display("Got resources: "+resources);
		assertEquals("Wrong number of resources", expectedNumber, resources.size());
		
		// GIVEN (2)
		resources.clear();
		task = taskManager.createTaskInstance(TestBrokenResources.class.getName() + "." + TEST_NAME);
        result = task.getResult();
        
		ResultHandler<ResourceType> handler = new ResultHandler<ResourceType>() {
			@Override
			public boolean handle(PrismObject<ResourceType> object, OperationResult parentResult) {
				resources.add(object);
				return true;
			}
		};
		
		// WHEN (2)
		modelService.searchObjectsIterative(ResourceType.class, null, handler, options, task, result);
		
		// THEN (2)
		result.computeStatus();
		display("getObject result", result);
		if (options == null) {
			assertEquals("Expected partial error (searchIterative)", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		} else if (GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options))) {
			TestUtil.assertSuccess(result);
		} else {
			AssertJUnit.fail("unexpected");
		}
		display("Got resources: "+resources);
		assertEquals("Wrong number of resources", expectedNumber, resources.size());
        
	}
	
}
