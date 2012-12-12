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
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
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
import com.evolveum.midpoint.schema.SelectorOptions;
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
@ContextConfiguration(locations = {"classpath:ctx-model.xml",
        "classpath:ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-task.xml",
		"classpath:ctx-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractSynchronizationStoryTest extends AbstractInitializedModelIntegrationTest {
		
	protected static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	
	protected static String userWallyOid;

	public AbstractSynchronizationStoryTest() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceGreen.setSyncStyle(DummySyncStyle.SMART);
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		dummyResourceBlue.setSyncStyle(DummySyncStyle.SMART);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
	}
	
	protected abstract void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException;

	protected abstract String getSyncTaskOid(PrismObject<ResourceType> resource);
	
	protected int getWaitTimeout() {
		return DEFAULT_TASK_WAIT_TIMEOUT;
	}
	
	protected int getNumberOfExtraDummyUsers() {
		return 0;
	}


	@Test
    public void test100ImportLiveSyncTaskDummyGreen() throws Exception {
		final String TEST_NAME = "test100ImportLiveSyncTaskDummyGreen";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importSyncTask(resourceDummyGreen);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForSyncTaskStart(resourceDummyGreen);
	}
	
	@Test
    public void test110AddDummyGreenAccountMancomb() throws Exception {
		final String TEST_NAME = "test110AddDummyGreenAccountMancomb";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Preconditions
        assertUsers(5);
                
		/// WHEN
        displayWhen(TEST_NAME);
        
        addDummyAccount(dummyResourceGreen, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", "Melee Island");
        
        waitForSyncTaskNextRun(resourceDummyGreen);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyGreen);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_GREEN_OID, 
        		accountMancomb.asObjectable().getResourceRef().getOid());
        
        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertAccounts(userMancomb, 1);
        
        assertLinked(userMancomb, accountMancomb);
        
        assertUsers(6);
	}
	
	@Test
    public void test200ImportLiveSyncTaskDummyBlue() throws Exception {
		final String TEST_NAME = "test200ImportLiveSyncTaskDummyBlue";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importSyncTask(resourceDummyBlue);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForSyncTaskStart(resourceDummyBlue);
	}
	
	@Test
    public void test210AddDummyBlueAccountWally() throws Exception {
		final String TEST_NAME = "test210AddDummyBlueAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		/// WHEN
        displayWhen(TEST_NAME);
        addDummyAccount(dummyResourceBlue, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");
        
        // Wait for sync task to pick up the change
        waitForSyncTaskNextRun(resourceDummyBlue);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Wally Feed");
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally was not created", userWally);
        userWallyOid = userWally.getOid();
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertAccounts(userWally, 1);
        
        assertLinked(userWally, accountWallyBlue);
        
        assertUsers(7);
	}
	
	/**
	 * Add wally also to the green dummy resource. This account should be linked to the existing user.
	 */
	@Test
    public void test310AddDummyGreenAccountWally() throws Exception {
		final String TEST_NAME = "test310AddDummyGreenAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		/// WHEN
        displayWhen(TEST_NAME);
        addDummyAccount(dummyResourceGreen, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", "Scabb Island");
        
        // Wait for sync task to pick up the change
        waitForSyncTaskNextRun(resourceDummyGreen);
        
        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
		
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Wally Feed");
        PrismObject<AccountShadowType> accountWallyGreen = checkWallyAccount(resourceDummyGreen, "green", "Wally Feed");
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertAccounts(userWally, 2);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        
        assertUsers(7);               
	}
	
	/**
	 * Import sync task for default dummy resource as well. This does not do much as we will no be manipulating
	 * the default dummy account directly. Just make sure that it does not do anything bad.
	 */
	@Test
    public void test350ImportLiveSyncTaskDummyDefault() throws Exception {
		final String TEST_NAME = "test350ImportLiveSyncTaskDummyDefault";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importSyncTask(resourceDummy);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForSyncTaskStart(resourceDummy);
        
        // Dummy resource has some extra users that may be created in recon, so let's give it a chance to do it now
        waitForSyncTaskNextRun(resourceDummy);
        
        assertUsers(7 + getNumberOfExtraDummyUsers());
	}
	
	/**
	 * Import sync task for default dummy resource as well. This does not do much as we will no be manipulating
	 * the default dummy account directly. Just make sure that it does not do anything bad.
	 */
	@Test
    public void test360ModifyUserAddDummyDefaultAccount() throws Exception {
		final String TEST_NAME = "test360ModifyUserAddDummyDefaultAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        assertEquals("OID of user wally have changed", userWallyOid, userWally.getOid());
        
        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(userWally.getOid(), resourceDummy);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
        
		/// WHEN
        displayWhen(TEST_NAME);
     	modelService.executeChanges(deltas, null, task, result);
		
        // THEN
        displayThen(TEST_NAME);
        
        // Make sure we have steady state
        waitForSyncTaskNextRun(resourceDummy);
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
        
        PrismObject<AccountShadowType> accountWallyDefault = checkWallyAccount(resourceDummy, "default", "Wally Feed");
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Wally Feed");
        PrismObject<AccountShadowType> accountWallyGreen = checkWallyAccount(resourceDummyGreen, "green", "Wally Feed");
        
        userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally Feed", null, null);
        assertAccounts(userWally, 3);

        assertLinked(userWally, accountWallyDefault);
        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        
        assertUsers(7 + getNumberOfExtraDummyUsers());
	}
	
	/**
	 * Change fullname on the green account. There is an inbound mapping to the user so it should propagate.
	 * There is also outbound mapping from the user to dummy account, therefore it should propagate there as well.
	 */
	@Test
    public void test370ModifyDummyGreenAccountWally() throws Exception {
		final String TEST_NAME = "test370ModifyDummyGreenAccountWally";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount wallyDummyAccount = dummyResourceGreen.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
                
		/// WHEN
        displayWhen(TEST_NAME);
        wallyDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Wally B. Feed");
        
        // Wait for sync task to pick up the change
        waitForSyncTaskNextRun(resourceDummyBlue);
        
        // Make sure that the "kickback" sync cycle of the other resource runs to completion
        // We want to check the state after it gets stable
        // and it could spoil the next test
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
        // Make sure we have steady state
        waitForSyncTaskNextRun(resourceDummy);
		
        // THEN
        displayThen(TEST_NAME);
        
        // FIXME this is wrong. The value should not be there as the mapping is WEAK.
        // see MID-1068
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Wally B. Feed");
        
        PrismObject<AccountShadowType> accountWallyGreen = checkWallyAccount(resourceDummyGreen, "green", "Wally B. Feed");
        PrismObject<AccountShadowType> accountWallyDefault = checkWallyAccount(resourceDummy, "default", "Wally B. Feed");
        
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Wally B. Feed", null, null);
       
        assertAccounts(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);
                
        assertUsers(7 + getNumberOfExtraDummyUsers());
	}

	/**
	 * Change user fullname. See if the change propagates correctly. Also see that there are no side-effects.
	 */
	@Test
    public void test380ModifyUserWallyFullName() throws Exception {
		final String TEST_NAME = "test380ModifyUserWallyFullName";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount wallyDummyAccount = dummyResourceGreen.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
                
		/// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(userWallyOid, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString("Bloodnose"));
        
        // Wait for sync tasks to pick up the change and have some chance to screw things
        waitForSyncTaskNextRun(resourceDummy);
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
		
        // THEN
        displayThen(TEST_NAME);
                
        // FIXME this is wrong. The value should not be there as the mapping is WEAK.
        // see MID-1068
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Bloodnose");
        
        PrismObject<AccountShadowType> accountWallyGreen = checkWallyAccount(resourceDummyGreen, "green", "Bloodnose");
        PrismObject<AccountShadowType> accountWallyDefault = checkWallyAccount(resourceDummy, "default", "Bloodnose");
        
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, null);
       
        assertAccounts(userWally, 3);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        assertLinked(userWally, accountWallyDefault);
                
        assertUsers(7 + getNumberOfExtraDummyUsers());
	}
	
	/**
	 * Delete default dummy account.
	 * Dummy resource has unlinkAccount sync reaction for deleted situation. The account should be unlinked
	 * but the user and other accounts should remain as they were.
	 */
	@Test
    public void test400DeleteDummyDefaultAccount() throws Exception {
		final String TEST_NAME = "test400DeleteDummyDefaultAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
     	dummyResource.deleteAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
        
        // Make sure we have steady state
     	waitForSyncTaskNextRun(resourceDummy);
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
        
        // THEN
        displayThen(TEST_NAME);
        
        assertNoDummyAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
//        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummy, task, result);
        
        PrismObject<AccountShadowType> accountWallyBlue = checkWallyAccount(resourceDummyBlue, "blue", "Bloodnose");
        PrismObject<AccountShadowType> accountWallyGreen = checkWallyAccount(resourceDummyGreen, "green", "Bloodnose");
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNotNull("User wally disappeared", userWally);
        assertUser(userWally, userWallyOid, ACCOUNT_WALLY_DUMMY_USERNAME, "Bloodnose", null, null);
        assertAccounts(userWally, 2);

        assertLinked(userWally, accountWallyGreen);
        assertLinked(userWally, accountWallyBlue);
        
        assertUsers(7 + getNumberOfExtraDummyUsers());
	}
	
	/**
	 * Delete green dummy account.
	 * Green dummy resource has deleteUser sync reaction for deleted situation. This should delete the user
	 * and all other accounts.
	 */
	@Test
    public void test410DeleteDummyGreentAccount() throws Exception {
		final String TEST_NAME = "test410DeleteDummyGreentAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
     	dummyResourceGreen.deleteAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
		
     	// Make sure we have steady state
     	waitForSyncTaskNextRun(resourceDummy);
        waitForSyncTaskNextRun(resourceDummyBlue);
        waitForSyncTaskNextRun(resourceDummyGreen);
     	
        // THEN
        displayThen(TEST_NAME);
        
        assertNoDummyAccount(ACCOUNT_WALLY_DUMMY_USERNAME);
//        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummy, task, result);
        
        assertNoDummyAccount(RESOURCE_DUMMY_GREEN_NAME, ACCOUNT_WALLY_DUMMY_USERNAME);
//        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummyGreen, task, result);
        
        PrismObject<UserType> userWally = findUserByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("User wally", userWally);
        assertNull("User wally is not gone", userWally);
        
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_WALLY_DUMMY_USERNAME);
//        assertNoShadow(ACCOUNT_WALLY_DUMMY_USERNAME, resourceDummyBlue, task, result);
        
        assertUsers(6 + getNumberOfExtraDummyUsers());
	}
	
	protected void waitForSyncTaskStart(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskStart(getSyncTaskOid(resource), false, getWaitTimeout());
	}
	
	protected void waitForSyncTaskNextRun(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskNextRun(getSyncTaskOid(resource), false, getWaitTimeout());
	}
	
	private PrismObject<AccountShadowType> checkWallyAccount(PrismObject<ResourceType> resource, String resourceDesc,
			String expectedFullName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<AccountShadowType> accountShadowWally = findAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME, resource);
        display("Account shadow wally ("+resourceDesc+")", accountShadowWally);
        assertEquals("Wrong resourceRef in wally account ("+resourceDesc+")", resource.getOid(), 
        		accountShadowWally.asObjectable().getResourceRef().getOid());
        IntegrationTestTools.assertAttribute(accountShadowWally.asObjectable(),  resource.asObjectable(),
				DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, expectedFullName);
        
        DummyAccount dummyAccount = dummyResourceBlue.getAccountByUsername(ACCOUNT_WALLY_DUMMY_USERNAME);
        display("Account wally ("+resourceDesc+")", dummyAccount);
        assertNotNull("No dummy account ("+resourceDesc+")", dummyAccount);
        assertEquals("Wrong dummy account fullname ("+resourceDesc+")", expectedFullName, 
        		dummyAccount.getAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        
        return accountShadowWally;
	}

}
