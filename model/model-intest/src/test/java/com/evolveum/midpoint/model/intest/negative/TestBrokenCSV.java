/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.model.intest.negative;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestModelServiceContract;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBrokenCSV extends AbstractConfiguredModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/negative";
	private static final String TEST_TARGET_DIR = "target/test/negative";
	
	private static final String CONNECTOR_DUMMY_NOJARS_FILENAME = TEST_DIR + "/connector-dummy-nojars.xml";
	private static final String CONNECTOR_DUMMY_NOJARS_OID = "cccccccc-cccc-cccc-cccc-666600660004";
	
	private static final String RESOURCE_CSVFILE_BROKEN_FILENAME = TEST_DIR + "/resource-csvfile-broken.xml";
	private static final String RESOURCE_CSVFILE_BROKEN_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3bbbb";
	
	private static final String RESOURCE_CSVFILE_NOTFOUND_FILENAME = TEST_DIR + "/resource-csvfile-notfound.xml";
	private static final String RESOURCE_CSVFILE_NOTFOUND_OID = "ef2bc95b-76e0-48e2-86d6-f0f002d3f0f0";
	
	private static final String RESOURCE_DUMMY_NOJARS_FILENAME = TEST_DIR + "/resource-dummy-nojars.xml";
	private static final String RESOURCE_DUMMY_NOJARS_OID = "10000000-0000-0000-0000-666600660004";
	
	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-jack-csvfile.xml";
	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120001";
	
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-murray-csvfile.xml";
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120666";
	
	private static final String BROKEN_CSV_FILE_NAME = "broken.csv";
	private static final String BROKEN_CSV_SOURCE_FILE_NAME = TEST_DIR + "/" + BROKEN_CSV_FILE_NAME;
	private static final String BROKEN_CSV_TARGET_FILE_NAME = TEST_TARGET_DIR + "/" + BROKEN_CSV_FILE_NAME;
	
	protected static final Trace LOGGER = TraceManager.getTrace(TestBrokenCSV.class);
	
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	
	protected UserType userTypeJack;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		LOGGER.trace("initSystem");
		
		modelService.postInit(initResult);
		
		// Resources
		File targetDir = new File(TEST_TARGET_DIR);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		MiscUtil.copyFile(new File(BROKEN_CSV_SOURCE_FILE_NAME), new File(BROKEN_CSV_TARGET_FILE_NAME));
		
		addObjectFromFile(CONNECTOR_DUMMY_NOJARS_FILENAME, ConnectorType.class, initResult);
		
		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.extendDummySchema();
		dummyResource = dummyResourceCtl.getDummyResource();
		
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILENAME, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		dummyResourceCtl.setResource(resourceDummy);
		
		importObjectFromFile(RESOURCE_CSVFILE_BROKEN_FILENAME, initResult);
		importObjectFromFile(RESOURCE_CSVFILE_NOTFOUND_FILENAME, initResult);
		importObjectFromFile(RESOURCE_DUMMY_NOJARS_FILENAME, initResult);
		
		// Accounts
		addObjectFromFile(ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME, ResourceObjectShadowType.class, initResult);
		
		// Users
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
	}
	
	@Test
    public void test010TestResourceBroken() throws Exception {
        displayTestTile(this, "test010TestResourceBroken");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test010TestResourceBroken");
        OperationResult result = task.getResult();
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_BROKEN_OID, task);
		
		// THEN
		display("testResource result", testResult);
        IntegrationTestTools.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test020GetResourceBroken() throws Exception {
        displayTestTile(this, "test020GetResourceBroken");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test020GetResourceBroken");
        OperationResult result = task.getResult();
        
		// WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		IntegrationTestTools.assertSuccess("getObject result", result);
		
		OperationResultType fetchResult = resource.asObjectable().getFetchResult();
		IntegrationTestTools.assertSuccess("resource.fetchResult", fetchResult);
		
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}

	@Test
    public void test100GetAccountMurray() throws Exception {
        displayTestTile(this, "test100GetAccountMurray");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test100GetAccountMurray");
        OperationResult result = task.getResult();
        
        try {

        	// WHEN
	        PrismObject<ResourceObjectShadowType> account = modelService.getObject(ResourceObjectShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
	        		null, task, result);

	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
        	display("Expected exception", e);
        	result.computeStatus();
    		display("getObject result", result);
            IntegrationTestTools.assertFailure("getObject result", result);
        }
		
	}
	
	@Test
    public void test101GetAccountMurrayNoFetch() throws Exception {
        displayTestTile(this, "test101GetAccountMurrayNoFetch");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test101GetAccountMurrayNoFetch");
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        
		// WHEN
        PrismObject<ResourceObjectShadowType> account = modelService.getObject(ResourceObjectShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
        		options, task, result);

        display("getObject account", account);
		result.computeStatus();
		display("getObject result", result);
		IntegrationTestTools.assertSuccess("getObject result", result);
        // TODO: better asserts
		assertNotNull("Null resource", account);
	}
	
	@Test
    public void test102GetAccountMurrayRaw() throws Exception {
        displayTestTile(this, "test102GetAccountMurrayRaw");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test102GetAccountMurrayRaw");
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        
		// WHEN
        PrismObject<ResourceObjectShadowType> account = modelService.getObject(ResourceObjectShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
        		options, task, result);

        display("getObject account", account);
		result.computeStatus();
		display("getObject result", result);
		IntegrationTestTools.assertSuccess("getObject result", result);
        // TODO: better asserts
		assertNotNull("Null resource", account);
	}

	
	@Test
    public void test120SearchAccountByUsernameJack() throws Exception {
        displayTestTile(this, "test120SearchAccountByUsernameJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test120SearchAccountByUsernameJack");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_BROKEN_OID, null, task, result);
        
        try {

        	// WHEN
	        PrismObject<ResourceObjectShadowType> account = findAccountByUsername("jack", resource, task, result);

	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
        	result.computeStatus();
    		display("findAccountByUsername result", result);
            IntegrationTestTools.assertFailure("findAccountByUsername result", result);
        }
		
	}
	
	@Test
    public void test210TestResourceNotFound() throws Exception {
        displayTestTile(this, "test210TestResourceNotFound");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test210TestResourceNotFound");
        OperationResult result = task.getResult();
        
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_NOTFOUND_OID, task);
		
		// THEN
		display("testResource result", testResult);
        IntegrationTestTools.assertFailure("testResource result", testResult);
	}
	
	@Test
    public void test220GetResourceNotFound() throws Exception {
		final String TEST_NAME = "test220GetResourceNotFound";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "."+TEST_NAME);
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
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "."+TEST_NAME);
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
        displayTestTile(this, "test310TestResourceNoJars");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test310TestResourceNoJars");
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_NOJARS_OID, task);
		
		// THEN
		display("testResource result", testResult);
        IntegrationTestTools.assertFailure("testResource result", testResult);
	}
	
	@Test
    public void test320GetResourceNoJars() throws Exception {
        displayTestTile(this, "test320GetResourceNoJars");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test320GetResourceNoJars");
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
	
	/**
	 * Assign two resources to a user. One of them is looney, the other is not. The result should be that
	 * the account on the good resource is created.
	 * 
	 * This one dies on the lack of schema.
	 */
	// MID-1248
	@Test(enabled=false)
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
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
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
		assertEquals("Expected partial error in result", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
        
        DummyAccount jackDummyAccount = dummyResource.getAccountByUsername(USER_JACK_USERNAME);
        assertNotNull("No jack dummy account", jackDummyAccount);
	}

	
}
