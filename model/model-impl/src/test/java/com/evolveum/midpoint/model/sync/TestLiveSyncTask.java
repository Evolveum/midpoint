/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.sync;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
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
public class TestLiveSyncTask extends AbstractInitializedModelIntegrationTest {
		
	private static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
	private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

	public TestLiveSyncTask() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		dummyResourceBlue.setSyncStyle(DummySyncStyle.SMART);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
	}
	
	@Test
    public void test100ImportLiveSyncTaskDummy() throws Exception {
		final String TEST_NAME = "test100ImportLiveSyncTaskDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_FILENAME);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_OID, false);
               
	}
	
	@Test
    public void test110AddDummyAccountMancomb() throws Exception {
		final String TEST_NAME = "test110AddDummyAccountMancomb";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Preconditions
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before sync", users);
        assertEquals("Unexpected number of users", 5, users.size());
                
		/// WHEN
        displayWhen(TEST_NAME);
        addDummyAccount(dummyResource, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", "Melee Island");
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_OID, false);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummy);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_OID, 
        		accountMancomb.asObjectable().getResourceRef().getOid());
        
        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertAccounts(userMancomb, 1);
        
        assertLinked(userMancomb, accountMancomb);
        
        users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after sync", users);
        assertEquals("Unexpected number of users", 6, users.size());
               
	}
	
	@Test
    public void test200ImportLiveSyncTaskDummyBlue() throws Exception {
		final String TEST_NAME = "test200ImportLiveSyncTaskDummyBlue";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_BLUE_OID, false);
               
	}
	
	@Test
    public void test210AddDummyBlueAccountWally() throws Exception {
		final String TEST_NAME = "test210AddDummyBlueAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		/// WHEN
        displayWhen(TEST_NAME);
        addDummyAccount(dummyResourceBlue, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");
        
        // Wait for sync task to pick up the change
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_BLUE_OID, false);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountWally = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummyBlue);
        display("Account wally", accountWally);
        assertNotNull("No wally account shadow", accountWally);
        assertEquals("Wrong resourceRef in wally account", RESOURCE_DUMMY_BLUE_OID, 
        		accountWally.asObjectable().getResourceRef().getOid());
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        assertAccounts(userWally, 1);
        
        assertLinked(userWally, accountWally);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after sync", users);
        assertEquals("Unexpected number of users", 7, users.size());
	}
	
	/**
	 * Add wally also to the other (default) dummy resource. This account should be linked to the existing user.
	 */
	@Test
    public void test310AddDummyAccountWally() throws Exception {
		final String TEST_NAME = "test310AddDummyAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		/// WHEN
        displayWhen(TEST_NAME);
        addDummyAccount(dummyResource, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");
        
        // Wait for sync task to pick up the change
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_OID, false);
        
        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_BLUE_OID, false);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_OID, false);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountWallyBlue = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummyBlue);
        display("Account shadow wally (blue)", accountWallyBlue);
        
        assertNotNull("No wally account shadow (blue)", accountWallyBlue);
        assertEquals("Wrong resourceRef in wally account (blue)", RESOURCE_DUMMY_BLUE_OID, 
        		accountWallyBlue.asObjectable().getResourceRef().getOid());
        
        PrismObject<AccountShadowType> accountWallyDummy = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummy);
        display("Account shadow wally (dummy)", accountWallyDummy);
        
        assertNotNull("No wally account shadow (dummy)", accountWallyDummy);
        assertEquals("Wrong resourceRef in wally account (dummy)", RESOURCE_DUMMY_OID, 
        		accountWallyDummy.asObjectable().getResourceRef().getOid());
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertAccounts(userWally, 2);

        assertLinked(userWally, accountWallyDummy);
        assertLinked(userWally, accountWallyBlue);
        
        
                
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after sync", users);
        assertEquals("Unexpected number of users", 7, users.size());
               
	}
	
	/**
	 * Change fullname on the blue account. There is an inbound mapping to the user so it should propagate.
	 * There is also outbound mapping from the user to dummy account, therefore it should propagate there as well.
	 */
	@Test
    public void test320ModifyDummyBlueAccountWally() throws Exception {
		final String TEST_NAME = "test320ModifyDummyBlueAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestLiveSyncTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount wallyDummyAccount = dummyResourceBlue.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
                
		/// WHEN
        displayWhen(TEST_NAME);
        wallyDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");
        
        // Wait for sync task to pick up the change
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_BLUE_OID, false);
        
        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_BLUE_OID, false);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_OID, false);
		
        // THEN
        displayThen(TEST_NAME);
                
        PrismObject<AccountShadowType> accountWallyBlue = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummyBlue);
        display("Account shadow wally (blue)", accountWallyBlue);
        
        DummyAccount dummyAccountBlue = dummyResourceBlue.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("Account wally (blue)", dummyAccountBlue);
        
        PrismObject<AccountShadowType> accountWallyDummy = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummy);
        display("Account shadow wally (dummy)", accountWallyDummy);
        
        DummyAccount dummyAccountDummy = dummyResource.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("Account wally (dummy)", dummyAccountDummy);
        
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);

        assertNotNull("No wally account shadow (blue)", accountWallyBlue);
        assertEquals("Wrong resourceRef in wally account (blue)", RESOURCE_DUMMY_BLUE_OID, 
        		accountWallyBlue.asObjectable().getResourceRef().getOid());
        IntegrationTestTools.assertAttribute(accountWallyBlue.asObjectable(),  resourceDummyBlue.asObjectable(),
				DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");
        
        assertNotNull("No wally account shadow (dummy)", accountWallyDummy);
        assertEquals("Wrong resourceRef in wally account (dummy)", RESOURCE_DUMMY_OID, 
        		accountWallyDummy.asObjectable().getResourceRef().getOid());
		IntegrationTestTools.assertAttribute(accountWallyDummy.asObjectable(),  resourceDummy.asObjectable(),
				DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");
                PrismAsserts.assertPropertyValue(userWally, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Wally B. Feed"));
        
        
        assertAccounts(userWally, 2);

        assertLinked(userWally, accountWallyDummy);
        assertLinked(userWally, accountWallyBlue);
                
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after sync", users);
        assertEquals("Unexpected number of users", 7, users.size());
        
	}

}
