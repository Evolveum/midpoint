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

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestActivation extends AbstractInitializedModelIntegrationTest {

	private static final File TEST_DIR = new File("src/test/resources/activation");
	
	protected static final File RESOURCE_DUMMY_KHAKI_FILE = new File(TEST_DIR, "resource-dummy-khaki.xml");
	protected static final String RESOURCE_DUMMY_KHAKI_OID = "10000000-0000-0000-0000-0000000a1004";
	protected static final String RESOURCE_DUMMY_KHAKI_NAME = "khaki";
	protected static final String RESOURCE_DUMMY_KHAKI_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
	private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);
	
	private String accountOid;
	private String accountRedOid;
    private String accountYellowOid;
	private XMLGregorianCalendar lastValidityChangeTimestamp;
	private String accountMancombOid;
	private String userMancombOid;
	private XMLGregorianCalendar manana;
	
	protected DummyResource dummyResourceKhaki;
	protected DummyResourceContoller dummyResourceCtlKhaki;
	protected ResourceType resourceDummyKhakiType;
	protected PrismObject<ResourceType> resourceDummyKhaki;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlKhaki = DummyResourceContoller.create(RESOURCE_DUMMY_KHAKI_NAME, resourceDummyKhaki);
		dummyResourceCtlKhaki.extendSchemaPirate();
		dummyResourceKhaki = dummyResourceCtlKhaki.getDummyResource();
		resourceDummyKhaki = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_KHAKI_FILE, RESOURCE_DUMMY_KHAKI_OID, initTask, initResult); 
		resourceDummyKhakiType = resourceDummyKhaki.asObjectable();
		dummyResourceCtlKhaki.setResource(resourceDummyKhaki);
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
        TestUtil.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusDisabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);
		assertDisableTimestampFocus(userJack, start, end);
		
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
        TestUtil.assertSuccess("executeChanges result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertValidity(userJack, null);
		assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
		assertEnableTimestampFocus(userJack, start, end);
		
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
        
        dummyAuditService.clear();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        IntegrationTestTools.assertCreateTimestamp(accountShadow, start, end);
        assertEnableTimestampShadow(accountShadow, start, end);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        IntegrationTestTools.assertCreateTimestamp(accountModel, start, end);
        assertEnableTimestampShadow(accountModel, start, end);
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
        
        assertDummyEnabled("jack");
        
        IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executionDeltas = dummyAuditService.getExecutionDeltas();
        boolean found = false;
        for (ObjectDeltaOperation<? extends ObjectType> executionDelta: executionDeltas) {
        	ObjectDelta<? extends ObjectType> objectDelta = executionDelta.getObjectDelta();
        	if (objectDelta.getObjectTypeClass() == ShadowType.class) {
        		PropertyDelta<Object> enableTimestampDelta = objectDelta.findPropertyDelta(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP));
        		display("Audit enableTimestamp delta", enableTimestampDelta);
        		assertNotNull("EnableTimestamp delta vanished from audit record", enableTimestampDelta);
        		found = true;
        	}
        }
        assertTrue("Shadow delta not found", found);
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
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusDisabled(userJack);
		assertDummyDisabled("jack");
		assertDisableTimestampFocus(userJack, startTime, endTime);
		
		String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
		assertDisableTimestampShadow(accountShadow, startTime, endTime);
		assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);
	}
	
	@Test
    public void test102ModifyUserJackEnable() throws Exception {
        TestUtil.displayTestTile(this, "test102ModifyUserJackEnable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyEnabled("jack");
		assertEnableTimestampFocus(userJack, startTime, endTime);
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
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
		String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
		assertAdministrativeStatusDisabled(accountShadow);
		assertDisableTimestampShadow(accountShadow, startTime, endTime);
		assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyDisabled("jack");
	}
	
	/**
	 * Make sure that recompute does not destroy anything.
	 */
	@Test
    public void test112UserJackRecompute() throws Exception {
		final String TEST_NAME = "test112UserJackRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        dummyAuditService.clear();
        
		// WHEN
        recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("recompute result", result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
		String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
		assertAdministrativeStatusDisabled(accountShadow);
		assertDisableTimestampShadow(accountShadow, null, startTime);
		assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyDisabled("jack");
		
		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0);
//        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Re-enabling the user should enable the account as well. Even if the user is already enabled.
	 */
	@Test
    public void test114ModifyUserJackEnable() throws Exception {
		final String TEST_NAME = "test114ModifyUserJackEnable";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
        
		assertAdministrativeStatusEnabled(userJack);
		assertDummyEnabled("jack");
		assertEnableTimestampFocus(userJack, startTime, endTime);
		
		assertAccounts(USER_JACK_OID, 1);
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyType);
        assertAdministrativeStatusEnabled(account);
        assertEnableTimestampShadow(account, startTime, endTime);
	}
	
	/**
	 * Modify both user and account activation. As password outbound mapping is weak the user should have its own state
	 * and account should have its own state.
	 */
	@Test
    public void test115ModifyJackActivationUserAndAccount() throws Exception {
		final String TEST_NAME = "test115ModifyJackActivationUserAndAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountOid, resourceDummy, 
        		ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
		
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		assertEnableTimestampFocus(userJack, startTime, endTime);
		assertAdministrativeStatusEnabled(userJack);
		
		assertDummyDisabled("jack");
		assertAccounts(USER_JACK_OID, 1);
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyType);
        assertAdministrativeStatusDisabled(account);
        assertDisableTimestampShadow(account, startTime, endTime);
        assertDisableReasonShadow(account, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
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
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
		accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
 
		PrismObject<ShadowType> accountRedRepo = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Account red (repo)", accountRedRepo);
        assertEnableTimestampShadow(accountRedRepo, startTime, endTime);
	 
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Account red (model)", accountRed);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyRedType);
        assertAdministrativeStatusEnabled(accountRed);
        assertEnableTimestampShadow(accountRed, startTime, endTime);
                
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
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
                        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");
		
        assertAdministrativeStatusDisabled(userJack);
		assertDummyDisabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");
		
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyRedType);
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableTimestampShadow(accountRed, startTime, endTime);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);
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
        TestUtil.assertSuccess("executeChanges result", result);
        
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
        TestUtil.assertSuccess("executeChanges result", result);
        
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
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyRedType);
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION);
                
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
        
        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
		assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}

    /**
     * Assign yellow account.
     */
    @Test
    public void test150ModifyUserJackAssignYellowAccount() throws Exception {
        final String TEST_NAME = "test150ModifyUserJackAssignYellowAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, "jack", "Jack Sparrow", true);

        // Check shadow
        PrismObject<ShadowType> accountShadowYellow = getShadowModel(accountYellowOid);
        assertAccountShadowModel(accountShadowYellow, accountYellowOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyYellowType);
        assertAdministrativeStatusEnabled(accountShadowYellow);
        IntegrationTestTools.assertCreateTimestamp(accountShadowYellow, start, end);
        assertEnableTimestampShadow(accountShadowYellow, start, end);

        // Check user
        IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
        assertAdministrativeStatusEnabled(userJack);
    }


    /**
     * Disable default & yellow accounts and check them after reconciliation.
     */
    @Test(enabled = true)
    public void test152ModifyAccountsJackDisable() throws Exception {
        final String TEST_NAME = "test152ModifyAccountsJackDisable";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        ObjectDelta<ShadowType> yellowDelta = createModifyAccountShadowReplaceDelta(accountYellowOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(dummyDelta, yellowDelta);

        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        checkAdminStatusFor15x(userJack, true, false, false);

        // WHEN (2) - now let's do a reconciliation on both resources

        ObjectDelta innocentDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_LOCALITY, 
        		userJack.asObjectable().getLocality().toPolyString());
        modelService.executeChanges(MiscSchemaUtil.createCollection(innocentDelta), ModelExecuteOptions.createReconcile(), task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result (after reconciliation)", result);

        checkAdminStatusFor15x(userJack, true, false, true);        // yellow has a STRONG mapping for adminStatus, therefore it should be replaced by the user's adminStatus
    }

    /**
     * Disable default & yellow accounts and check them after reconciliation.
     */
    @Test(enabled = true)
    public void test153ModifyAccountsJackEnable() throws Exception {
        final String TEST_NAME = "test153ModifyAccountsJackEnable";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> yellowDelta = createModifyAccountShadowReplaceDelta(accountYellowOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(dummyDelta, yellowDelta);

        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        checkAdminStatusFor15x(userJack, true, true, true);

        // WHEN (2) - now let's do a reconciliation on both resources

        ObjectDelta innocentDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_LOCALITY, 
        		userJack.asObjectable().getLocality().toPolyString());
        modelService.executeChanges(MiscSchemaUtil.createCollection(innocentDelta), ModelExecuteOptions.createReconcile(), task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result (after reconciliation)", result);

        checkAdminStatusFor15x(userJack, true, true, true);
    }

    private void checkAdminStatusFor15x(PrismObject user, boolean userStatus, boolean accountStatus, boolean accountStatusYellow) throws Exception {
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        PrismObject<ShadowType> accountYellow = getShadowModel(accountYellowOid);
        
        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyType);
        assertAdministrativeStatus(account, accountStatus ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
        if (!accountStatus) {
        	assertDisableReasonShadow(account, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        }
        
        assertAccountShadowModel(accountYellow, accountYellowOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyYellowType);
        assertAdministrativeStatus(accountYellow, accountStatusYellow ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
        if (!accountStatusYellow) {
        	assertDisableReasonShadow(accountYellow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        }

        assertAdministrativeStatus(user, userStatus ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);

        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", accountStatus);
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", accountStatusYellow);
    }

    /**
     * Khaki resource has simulated activation capability.
     */
	@Test
    public void test160ModifyUserJackAssignAccountKhaki() throws Exception {
		final String TEST_NAME = "test160ModifyUserJackAssignAccountKhaki";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_KHAKI_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyAuditService.clear();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_KHAKI_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyKhakiType);
        IntegrationTestTools.assertCreateTimestamp(accountShadow, start, end);
        assertEnableTimestampShadow(accountShadow, start, end);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyKhakiType);
        IntegrationTestTools.assertCreateTimestamp(accountModel, start, end);
        assertEnableTimestampShadow(accountModel, start, end);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_KHAKI_NAME, "jack", "Jack Sparrow", true);
        
        assertDummyEnabled(RESOURCE_DUMMY_KHAKI_NAME, "jack");
        
        IntegrationTestTools.assertModifyTimestamp(userJack, start, end);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executionDeltas = dummyAuditService.getExecutionDeltas();
        boolean found = false;
        for (ObjectDeltaOperation<? extends ObjectType> executionDelta: executionDeltas) {
        	ObjectDelta<? extends ObjectType> objectDelta = executionDelta.getObjectDelta();
        	if (objectDelta.getObjectTypeClass() == ShadowType.class) {
        		PropertyDelta<Object> enableTimestampDelta = objectDelta.findPropertyDelta(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP));
        		display("Audit enableTimestamp delta", enableTimestampDelta);
        		assertNotNull("EnableTimestamp delta vanished from audit record", enableTimestampDelta);
        		found = true;
        	}
        }
        assertTrue("Shadow delta not found", found);
	}
	
	@Test
    public void test170GetAccountUnlocked() throws Exception {
        final String TEST_NAME = "test170GetAccountUnlocked";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        PrismAsserts.assertNoItem(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
	}
	
	@Test
    public void test172GetAccountLocked() throws Exception {
        final String TEST_NAME = "test172GetAccountLocked";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        DummyAccount dummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setLockout(true);

        // WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.LOCKED);
	}
	
	@Test
    public void test174ModifyAccountUnlock() throws Exception {
        final String TEST_NAME = "test174ModifyAccountUnlock";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null, 
        		SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, LockoutStatusType.NORMAL);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(dummyDelta);

        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        
        DummyAccount dummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        assertFalse("Dummy account was not unlocked", dummyAccount.isLockout());

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.NORMAL);

        checkAdminStatusFor15x(userJack, true, true, true);
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
        TestUtil.assertSuccess("executeChanges result", result);
        
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
        
        PrismObject<UserType> userLargo = PrismTestUtil.parseObject(USER_LARGO_FILE);
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
        TestUtil.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
		display("User after change execution", userLargo);
		assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
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
        accountOid = getSingleLinkOid(userLargo);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");
        
        // Check account in dummy resource
        assertDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
	}
	
	/**
	 * Delete assignment from repo. Model should not notice. The change should be applied after recompute.
	 */
	@Test
    public void test230JackUnassignRepoRecompute() throws Exception {
		final String TEST_NAME = "test230JackUnassignRepoRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        long startMillis = clock.currentTimeMillis();
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(USER_JACK_FILE);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // Delete the assignment from the repo. Really use the repo directly. We do not want the model to notice.
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        repositoryService.modifyObject(UserType.class, userDelta.getOid(), userDelta.getModifications(), result);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
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
        assertUsers(6);
        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNull("Unexpected user mancomb before import", userMancomb);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_GREEN_OID, new QName(dummyResourceCtlGreen.getNamespace(), "AccountObjectClass"), task, result);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 40000);
        
        TestUtil.displayThen(TEST_NAME);
        
        userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No user mancomb after import", userMancomb);
        userMancombOid = userMancomb.getOid();
        
        assertUsers(7);
        
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
        TestUtil.assertSuccess(result);
        
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
	
	/**
	 * Herman has validTo/validFrom. Khaki resource has strange mappings for these.
	 */
	@Test
    public void test410AssignHermanKhakiAccount() throws Exception {
		final String TEST_NAME = "test410AssignHermanKhakiAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        assignAccount(USER_HERMAN_OID, RESOURCE_DUMMY_KHAKI_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
		display("User after change execution", user);
		assertLinks(user, 1);
        
		DummyAccount khakiAccount = getDummyAccount(RESOURCE_DUMMY_KHAKI_NAME, USER_HERMAN_USERNAME);
		assertNotNull("No khaki account", khakiAccount);
		assertTrue("khaki account not enabled", khakiAccount.isEnabled());
		assertEquals("Wrong quote (validFrom) in khaki account", "from: 1700-05-30T11:00:00Z", 
				khakiAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME));
		assertEquals("Wrong drink (validTo) in khaki account", "to: 2233-03-23T18:30:00Z", 
				khakiAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
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
