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
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportRecon extends AbstractInitializedModelIntegrationTest {
		
	public TestImportRecon() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// Create an account that midPoint does not know about yet
		dummyResourceCtl.addAccount(USER_RAPP_USERNAME, "Rapp Scallion", "Scabb Island");
		
		// And a user that will be correlated to that account
		addObjectFromFile(USER_RAPP_FILENAME, UserType.class, initResult);
	}



	@Test
    public void test100ImportFromResourceDummy() throws Exception {
		final String TEST_NAME = "test100ImportFromResourceDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Preconditions
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before import", users);
        assertEquals("Unexpected number of users", 6, users.size());
        
        PrismObject<UserType> rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp has dummy account but it is not linked
        assertAccounts(rapp, 0);
        
		// WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_OID, new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        IntegrationTestTools.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        displayThen(TEST_NAME);
        
        users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertEquals("Unexpected number of users", 7, users.size());
        
        PrismObject<UserType> admin = getUser(USER_ADMINISTRATOR_OID);
        assertNotNull("No admin", admin);
        assertAccounts(admin, 0);
        assertEnabled(admin);
        
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        assertNotNull("No jack", jack);
        assertAccounts(jack, 0);
        assertEnabled(jack);
        
        PrismObject<UserType> barbossa = getUser(USER_BARBOSSA_OID);
        assertNotNull("No barbossa", barbossa);
        assertAccounts(barbossa, 1);
        // Barbossa had opendj account before
        assertAccount(barbossa, RESOURCE_OPENDJ_OID);
        assertEnabled(barbossa);
        
        PrismObject<UserType> guybrush = getUser(USER_GUYBRUSH_OID);
        assertNotNull("No guybrush", guybrush);
        assertAccounts(guybrush, 1);
        assertAccount(guybrush, RESOURCE_DUMMY_OID);
        assertEnabled(guybrush);
        
        rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp account should be linked
        assertAccounts(rapp, 1);
        assertAccount(rapp, RESOURCE_DUMMY_OID);
        assertEnabled(rapp);
        
        PrismObject<UserType> herman = findUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertNotNull("No herman", herman);
        assertAccounts(herman, 1);
        assertAccount(herman, RESOURCE_DUMMY_OID);
        assertEnabled(herman);
        
        // These are protected accounts, they should not be imported
        PrismObject<UserType> daviejones = findUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNull("Jones sneaked in", daviejones);
        PrismObject<UserType> calypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertNull("Calypso sneaked in", calypso);
	}
	
	@Test
    public void test200ReconcileDummy() throws Exception {
		final String TEST_NAME = "test200ReconcileDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource
        
        // fullname has a strong outbound mapping, this change should be corrected
        DummyAccount guybrushDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Dubrish Freepweed");
        
        // Weapon has a weak mapping, this change should be left as it is
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Feather duster");
        
        // Drink is not tolerant. The extra values should be removed
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        // Quote is tolerant. The extra values should stay as it is
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I want to be a pirate!");

        
        // Calypso is protected, this should not reconcile
        DummyAccount calypsoDummyAccount = dummyResource.getAccountByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        calypsoDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
        
		// WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(TASK_RECONCILE_DUMMY_FILENAME);
		
        // THEN
        displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, false);
        
        displayThen(TEST_NAME);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertEquals("Unexpected number of users", 7, users.size());
        
        PrismObject<UserType> admin = getUser(USER_ADMINISTRATOR_OID);
        assertNotNull("No admin", admin);
        assertAccounts(admin, 0);
        assertEnabled(admin);
        
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        assertNotNull("No jack", jack);
        assertAccounts(jack, 0);
        assertEnabled(jack);
        
        PrismObject<UserType> barbossa = getUser(USER_BARBOSSA_OID);
        assertNotNull("No barbossa", barbossa);
        assertAccounts(barbossa, 1);
        assertAccount(barbossa, RESOURCE_OPENDJ_OID);
        assertEnabled(barbossa);
        
        PrismObject<UserType> guybrush = getUser(USER_GUYBRUSH_OID);
        assertNotNull("No guybrush", guybrush);
        assertAccounts(guybrush, 1);
        assertAccount(guybrush, RESOURCE_DUMMY_OID);
        assertEnabled(guybrush);
        // Guybrushes fullname should be corrected back to real fullname
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Guybrush Threepwood");
        // Guybrushes weapon should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, 
        		"Feather duster");
        // Guybrushes drink should be corrected
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		"rum");
        // Guybrushes quotes should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, 
        		"Arr!", "I want to be a pirate!");
        
        PrismObject<UserType> rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp account should be linked
        assertAccounts(rapp, 1);
        assertAccount(rapp, RESOURCE_DUMMY_OID);
        assertEnabled(rapp);
        
        PrismObject<UserType> herman = findUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertNotNull("No herman", herman);
        assertAccounts(herman, 1);
        assertAccount(herman, RESOURCE_DUMMY_OID);
        assertEnabled(herman);
        
        // These are protected accounts, they should not be imported
        PrismObject<UserType> daviejones = findUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNull("Jones sneaked in", daviejones);
        PrismObject<UserType> calypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertNull("Calypso sneaked in", calypso);
        
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		"Calypso");
	}
		

}
