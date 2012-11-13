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
package com.evolveum.midpoint.model.negative;

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

import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.TestModelServiceContract;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBrokenCSV extends AbstractModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/negative";
	private static final String TEST_TARGET_DIR = "target/test/negative";
	
	private static final String RESOURCE_CSVFILE_FILENAME = TEST_DIR + "/resource-csvfile-broken.xml";
	private static final String RESOURCE_CSVFILE_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3bbbb";
	
	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-jack-csvfile.xml";
	private static final String ACCOUNT_SHADOW_JACK_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120001";
	
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME = TEST_DIR + "/account-shadow-murray-csvfile.xml";
	private static final String ACCOUNT_SHADOW_MURRAY_CSVFILE_OID = "ef2bc95b-76e0-1111-d3ad-3d4f12120666";
	
	private static final String BROKEN_CSV_FILE_NAME = "broken.csv";
	private static final String BROKEN_CSV_SOURCE_FILE_NAME = TEST_DIR + "/" + BROKEN_CSV_FILE_NAME;
	private static final String BROKEN_CSV_TARGET_FILE_NAME = TEST_TARGET_DIR + "/" + BROKEN_CSV_FILE_NAME;
	
	protected static final Trace LOGGER = TraceManager.getTrace(TestBrokenCSV.class);
	
	private PrismObject<ResourceType> resource;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		LOGGER.trace("initSystem");
		
		modelService.postInit(initResult);
		
		File targetDir = new File(TEST_TARGET_DIR);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		MiscUtil.copyFile(new File(BROKEN_CSV_SOURCE_FILE_NAME), new File(BROKEN_CSV_TARGET_FILE_NAME));
		
		importObjectFromFile(RESOURCE_CSVFILE_FILENAME, initResult);
		
		addObjectFromFile(ACCOUNT_SHADOW_MURRAY_CSVFILE_FILENAME, AccountShadowType.class, initResult);
		
	}
	
	@Test
    public void test010TestResource() throws Exception {
        displayTestTile(this, "test010TestResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test010TestResource");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
		OperationResult testResult = modelService.testResource(RESOURCE_CSVFILE_OID, task);
		
		// THEN
		display("testResource result", testResult);
        IntegrationTestTools.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test020GetResource() throws Exception {
        displayTestTile(this, "test020GetResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test020GetResource");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
		resource = modelService.getObject(ResourceType.class, RESOURCE_CSVFILE_OID, null, task, result);
		
		// THEN
		display("getObject resource", resource);
		result.computeStatus();
		display("getObject result", result);
		IntegrationTestTools.assertSuccess("getObject result", result);
        // TODO: better asserts
		assertNotNull("Null resource", resource);
	}

	@Test
    public void test100GetAccountMurray() throws Exception {
        displayTestTile(this, "test100GetAccountMurray");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + ".test100GetAccountMurray");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        try {

        	// WHEN
	        PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
	        		null, task, result);

	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
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
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        Collection<ObjectOperationOptions> options = ObjectOperationOptions.createCollectionRoot(ObjectOperationOption.NO_FETCH);
        
		// WHEN
        PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
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
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        Collection<ObjectOperationOptions> options = ObjectOperationOptions.createCollectionRoot(ObjectOperationOption.RAW);
        
		// WHEN
        PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, ACCOUNT_SHADOW_MURRAY_CSVFILE_OID,
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
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        try {

        	// WHEN
	        PrismObject<AccountShadowType> account = findAccountByUsername("jack", resource, task, result);

	        AssertJUnit.fail("Expected SystemException but the operation was successful");
        } catch (SystemException e) {
        	// This is expected
        	result.computeStatus();
    		display("findAccountByUsername result", result);
            IntegrationTestTools.assertFailure("findAccountByUsername result", result);
        }
		
	}
		

}
