/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestActivation extends AbstractInitializedModelIntegrationTest {
			
	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
	private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);
	
	private String accountOid;
	private String accountRedOid;
	private XMLGregorianCalendar lastValidityChangeTimestamp;
	private String accountMancombOid;
	private String userMancombOid;
	private XMLGregorianCalendar manana;

	public TestActivation() throws JAXBException {
		super();
	}
			
	@Test
    public void test050CheckJackEnabled() throws Exception {
        TestUtil.displayTestTile(this, "test050CheckJackEnabled");

        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added
        
        // THEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test050CheckJackEnabled");
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		// Cannot assert validity or effective status here. The user was added through repo and was not recomputed yet.
	}

	@Test
    public void test051ModifyUserJackDisable() throws Exception {
        TestUtil.displayTestTile(this, "test051ModifyUserJackDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test051ModifyUserJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusDisabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);
		IntegrationTestTools.assertBetween("disable timestamp", start, end, userJack.asObjectable().getActivation().getDisableTimestamp());
		
		IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
	}
	
	@Test
    public void test052ModifyUserJackEnable() throws Exception {
        TestUtil.displayTestTile(this, "test052ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
		IntegrationTestTools.assertBetween("enable timestamp", start, end, userJack.asObjectable().getActivation().getEnableTimestamp());
		
		IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
	}
	
	@Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        IntegrationTestTools.assertCreateTimestamp(accountShadow, start, end);
        IntegrationTestTools.assertBetween("shadow enable timestamp", start, end, accountShadow.asObjectable().getActivation().getEnableTimestamp());
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        IntegrationTestTools.assertCreateTimestamp(accountModel, start, end);
        IntegrationTestTools.assertBetween("shadow enable timestamp", start, end, accountModel.asObjectable().getActivation().getEnableTimestamp());
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertDummyEnabled("jack");
        
        IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
	}
	
	@Test
    public void test101ModifyUserJackDisable() throws Exception {
		final String TEST_NAME = "test101ModifyUserJackDisable";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusDisabled(userJack);
		assertDummyDisabled("jack");
		
		XMLGregorianCalendar userDisableTimestamp = userJack.asObjectable().getActivation().getDisableTimestamp();
		IntegrationTestTools.assertBetween("Wrong user disableTimestamp", 
				startTime, endTime, userDisableTimestamp);
		
		String accountOid = getAccountRef(userJack, RESOURCE_DUMMY_OID);
		PrismObject<ShadowType> accountShadow = getAccount(accountOid);
		XMLGregorianCalendar accountDisableTimestamp = accountShadow.asObjectable().getActivation().getDisableTimestamp();
		IntegrationTestTools.assertBetween("Wrong account disableTimestamp", 
				startTime, endTime, accountDisableTimestamp);
	}
	
	@Test
    public void test102ModifyUserJackEnable() throws Exception {
        TestUtil.displayTestTile(this, "test052ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyEnabled("jack");
	}
	

	/**
	 * Modify account activation. User's activation should be unchanged
	 */
	@Test
    public void test111ModifyAccountJackDisable() throws Exception {
        TestUtil.displayTestTile(this, "test111ModifyAccountJackDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test111ModifyAccountJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyAccountShadowReplace(accountOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
		String accountOid = getAccountRef(userJack, RESOURCE_DUMMY_OID);
		PrismObject<ShadowType> accountShadow = getAccount(accountOid);
		XMLGregorianCalendar accountDisableTimestamp = accountShadow.asObjectable().getActivation().getDisableTimestamp();
		IntegrationTestTools.assertBetween("Wrong account disableTimestamp", 
				startTime, clock.currentTimeXMLGregorianCalendar(), accountDisableTimestamp);
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyDisabled("jack");
	}
	
	/**
	 * Re-enabling the user should enable the account sa well. Even if the user is already enabled.
	 */
	@Test
    public void test112ModifyUserJackEnable() throws Exception {
        TestUtil.displayTestTile(this, "test112ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test112ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyEnabled("jack");
	}
	
	/**
	 * Modify both user and account activation. As password outbound mapping is weak the user should have its own state
	 * and account should have its own state.
	 */
	@Test
    public void test113ModifyJackActivationUserAndAccount() throws Exception {
        TestUtil.displayTestTile(this, "test113ModifyJackActivationUserAndAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test113ModifyJackActivationUserAndAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountOid, resourceDummy, 
        		ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);        
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
		assertAdministrativeStatusEnabled(userJack);
		assertDummyDisabled("jack");
	}
	
	/**
	 * Add red dummy resource to the mix. This would be fun.
	 */
	@Test
    public void test120ModifyUserJackAssignAccountDummyRed() throws Exception {
        TestUtil.displayTestTile(this, "test120ModifyUserJackAssignAccountDummyRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test120ModifyUserJackAssignAccountDummyRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getAccount(accountRedOid);
        assertShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyRedType);
        assertAdministrativeStatusEnabled(accountRed);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
        
        assertAdministrativeStatusEnabled(userJack);
		assertDummyDisabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Modify both user and account activation. Red dummy has a strong mapping. User change should override account
	 * change.
	 */
	@Test
    public void test121ModifyJackUserAndAccountRed() throws Exception {
		final String TEST_NAME = "test121ModifyJackUserAndAccountRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountRedOid, resourceDummy, 
        		ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
        assertAdministrativeStatusDisabled(userJack);
		assertDummyDisabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	@Test
    public void test130ModifyAccountDefaultAndRed() throws Exception {
        TestUtil.displayTestTile(this, "test130ModifyAccountDefaultAndRed");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test121ModifyJackPasswordUserAndAccountRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ObjectDelta<ShadowType> accountDeltaDefault = createModifyAccountShadowReplaceDelta(accountOid, 
        		resourceDummy, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDeltaRed = createModifyAccountShadowReplaceDelta(accountRedOid, 
        		resourceDummyRed, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDeltaDefault, accountDeltaRed);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusDisabled(userJack);
		assertDummyEnabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Let's make a clean slate for the next test
	 */
	@Test
    public void test138ModifyJackEnabled() throws Exception {
		final String TEST_NAME = "test138ModifyJackEnabled";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
		assertDummyEnabled("jack");
		assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	/**
	 * Red dummy resource disables account instead of deleting it.
	 */
	@Test
    public void test139ModifyUserJackUnAssignAccountDummyRed() throws Exception {
		final String TEST_NAME = "test139ModifyUserJackUnAssignAccountDummyRed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getAccount(accountRedOid);
        assertShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyRedType);
        assertAdministrativeStatusDisabled(accountRed);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
        
        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	@Test
    public void test199DeleteUserJack() throws Exception {
		final String TEST_NAME = "test199DeleteUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
        try {
			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
			AssertJUnit.fail("Jack is still alive!");
		} catch (ObjectNotFoundException ex) {
			// This is OK
		}
                
        // Check that the accounts are gone
        assertNoDummyAccount(null, "jack");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");
	}
	
	@Test
    public void test200AddUserLargo() throws Exception {
		final String TEST_NAME = "test200AddUserLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLargo = PrismTestUtil.parseObject(new File(USER_LARGO_FILENAME));
        ObjectDelta<UserType> addDelta = userLargo.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);
        
        // WHEN
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
	}
	
	@Test
    public void test205ModifyUserLargoAssignAccount() throws Exception {
		final String TEST_NAME = "test205ModifyUserLargoAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_LARGO_OID, RESOURCE_DUMMY_OID, null, true);
        accountAssignmentUserDelta.addModificationAddProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Largo LaGrande"));
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	@Test
    public void test210ModifyLargoValidTo10MinsAgo() throws Exception {
		final String TEST_NAME = "test210ModifyLargoValidTo10MinsAgo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar tenMinutesAgo = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() - 10 * 60 * 1000);
        
        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, tenMinutesAgo);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.AFTER);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	@Test
    public void test211ModifyLargoValidToManana() throws Exception {
		final String TEST_NAME = "test211ModifyLargoValidToManana";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        manana = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000);
        
        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, manana);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
		lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	/**
	 * Move time to tomorrow. Nothing should change yet. It is not yet manana.
	 */
	@Test
    public void test212SeeLargoTomorrow() throws Exception {
		final String TEST_NAME = "test212SeeLargoTomorrow";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Let's play with the clock, move the time to tomorrow
        long crrentNow = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        clock.override(crrentNow);
        
        // WHEN
        modelService.recompute(UserType.class, USER_LARGO_OID, task, result);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
		assertValidityTimestamp(userLargo, lastValidityChangeTimestamp);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	/**
	 * Move time after manana. Largo should be invalid.
	 */
	@Test
    public void test213HastaLaMananaLargo() throws Exception {
		final String TEST_NAME = "test213HastaLaMananaLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Let's play with the clock, move the time forward 20 days
        long crrentNow = System.currentTimeMillis() + 20 *24 * 60 * 60 * 1000;
        clock.override(crrentNow);
        
        // WHEN
        modelService.recompute(UserType.class, USER_LARGO_OID, task, result);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.AFTER);
		assertValidityTimestamp(userLargo, crrentNow);
		assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	@Test
    public void test215ModifyLargoRemoveValidTo() throws Exception {
		final String TEST_NAME = "test215ModifyLargoRemoveValidTo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = clock.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        modifyUserDelete(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, manana);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, TimeIntervalStatusType.IN);
		assertValidityTimestamp(userLargo, startMillis, clock.currentTimeMillis());
		lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
		assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
	}
	
	@Test
    public void test217ModifyLargoRemoveValidFrom() throws Exception {
		final String TEST_NAME = "test217ModifyLargoRemoveValidFrom";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = clock.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_FROM_PATH, task, result);
        
        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
        
		assertValidity(userLargo, null);
		assertValidityTimestamp(userLargo, startMillis, clock.currentTimeMillis());
		lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
		assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);
		
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleUserAccountRef(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	/**
	 * Test reading of validity data through shadow
	 */
	@Test
    public void test300AddDummyGreenAccountMancomb() throws Exception {
		final String TEST_NAME = "test300AddDummyGreenAccountMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
		account.setEnabled(true);
		account.setValidFrom(ACCOUNT_MANCOMB_VALID_FROM_DATE);
		account.setValidTo(ACCOUNT_MANCOMB_VALID_TO_DATE);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        
		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        
		dummyResourceGreen.addAccount(account);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyGreen);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        accountMancombOid = accountMancomb.getOid();
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_GREEN_OID, 
        		accountMancomb.asObjectable().getResourceRef().getOid());
        assertValidFrom(accountMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(accountMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);        
	}
	
	/**
	 * Testing of inbound mappings for validity data. Done by initiating an import of accouts from green resource. 
	 */
	@Test
    public void test310ImportAccountsFromDummyGreen() throws Exception {
		final String TEST_NAME = "test310ImportAccountsFromDummyGreen";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Preconditions
        assertUsers(5);
        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNull("Unexpected user mancomb before import", userMancomb);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_GREEN_OID, new QName(dummyResourceCtlGreen.getNamespace(), "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        IntegrationTestTools.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        TestUtil.displayThen(TEST_NAME);
        
        userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No user mancomb after import", userMancomb);
        userMancombOid = userMancomb.getOid();
        
        assertUsers(6);
        
        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);
	}
	
	@Test
    public void test350AssignMancombBlueAccount() throws Exception {
		final String TEST_NAME = "test350AssignMancombBlueAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignAccount(userMancombOid, RESOURCE_DUMMY_BLUE_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> userMancomb = getUser(userMancombOid);
		display("User after change execution", userMancomb);
		assertAccounts(userMancombOid, 2);
        
		DummyAccount mancombBlueAccount = getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
		assertNotNull("No mancomb blue account", mancombBlueAccount);
		assertTrue("mancomb blue account not enabled", mancombBlueAccount.isEnabled());
		assertEquals("Wrong validFrom in mancomb blue account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlueAccount.getValidFrom());
		assertEquals("Wrong validTo in mancomb blue account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlueAccount.getValidTo());
	}
	
	@Test
    public void test400AddHerman() throws Exception {
		final String TEST_NAME = "test400AddHerman";
        TestUtil.displayTestTile(this, TEST_NAME);

		// WHEN
        addObject(USER_HERMAN_FILE);
        
        // THEN
        // Make sure that it is effectivelly enabled
        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.ENABLED);
	}
		
	private void assertDummyActivationEnabledState(String userId, boolean expectedEnabled) {
		assertDummyActivationEnabledState(null, userId, expectedEnabled);
	}
	
	private void assertDummyActivationEnabledState(String instance, String userId, boolean expectedEnabled) {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		assertEquals("Wrong enabled flag in dummy '"+instance+"' account "+userId, expectedEnabled, account.isEnabled());
	}
	
	private void assertDummyEnabled(String userId) {
		assertDummyActivationEnabledState(userId, true);
	}
	
	private void assertDummyDisabled(String userId) {
		assertDummyActivationEnabledState(userId, false);
	}
	
	private void assertDummyEnabled(String instance, String userId) {
		assertDummyActivationEnabledState(instance, userId, true);
	}
	
	private void assertDummyDisabled(String instance, String userId) {
		assertDummyActivationEnabledState(instance, userId, false);
	}
	
	private void assertValidity(PrismObject<UserType> user, TimeIntervalStatusType expectedValidityStatus) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		assertEquals("Unexpected validity status in "+user, expectedValidityStatus, activation.getValidityStatus());
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, long expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		assertEquals("wrong validityChangeTimestamp", expected, XmlTypeConverter.toMillis(validityChangeTimestamp));
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, XMLGregorianCalendar expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		assertEquals("wrong validityChangeTimestamp", expected, validityChangeTimestamp);
	}
	
	private void assertValidityTimestamp(PrismObject<UserType> user, long lowerBound, long upperBound) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
		assertNotNull("No validityChangeTimestamp in "+user, validityChangeTimestamp);
		long validityMillis = XmlTypeConverter.toMillis(validityChangeTimestamp);
		if (validityMillis >= lowerBound && validityMillis <= upperBound) {
			return;
		}
		AssertJUnit.fail("Expected validityChangeTimestamp to be between "+lowerBound+" and "+upperBound+", but it was "+validityMillis);
	}
	
	private void assertEffectiveStatus(PrismObject<UserType> user, ActivationStatusType expected) {
		ActivationType activation = user.asObjectable().getActivation();
		assertNotNull("No activation in "+user, activation);
		assertEquals("Unexpected effective activation status in "+user, expected, activation.getEffectiveStatus());
	}

}
