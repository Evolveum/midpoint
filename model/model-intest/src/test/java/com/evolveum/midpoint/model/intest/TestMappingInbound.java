/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests inbound mappings. Uses live sync to do that.
 * These tests are much simpler and more focused than those in AbstractSynchronizationStoryTest.
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingInbound extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/mapping-inbound");

    protected static final File RESOURCE_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "resource-dummy-tea-green.xml");
    protected static final String RESOURCE_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-0000-00000000c404";
    protected static final String RESOURCE_DUMMY_TEA_GREEN_NAME = "tea-green";

    protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

    protected static final String TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILENAME = TEST_DIR + "/task-dumy-tea-green-livesync.xml";
    protected static final String TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-5555-55550000c404";

    protected PrismObject<ResourceType> resourceDummyTeaGreen;
    protected DummyResourceContoller dummyResourceCtlTeaGreen;
    protected DummyResource dummyResourceTeaGreen;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        dummyResourceCtlTeaGreen = DummyResourceContoller.create(RESOURCE_DUMMY_TEA_GREEN_NAME, null);
        dummyResourceCtlTeaGreen.extendSchemaPirate();
        dummyResourceTeaGreen = dummyResourceCtlTeaGreen.getDummyResource();
        dummyResourceTeaGreen.setSyncStyle(DummySyncStyle.SMART);
        resourceDummyTeaGreen = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_TEA_GREEN_FILE, RESOURCE_DUMMY_TEA_GREEN_OID, initTask, initResult);
        dummyResourceCtlTeaGreen.setResource(resourceDummyTeaGreen);
	}

    @Test
    public void test100ImportLiveSyncTaskDummyTeaGreen() throws Exception {
        final String TEST_NAME = "test100ImportLiveSyncTaskDummyTeaGreen";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestMappingInbound.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importSyncTask();

        // THEN
        TestUtil.displayThen(TEST_NAME);

        waitForSyncTaskStart();
    }

    @Test
    public void test110AddDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test110AddDummyTeaGreenAccountMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestMappingInbound.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);

        dummyResourceTeaGreen.addAccount(account);

        waitForSyncTaskNextRun();

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyTeaGreen);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);

        assertLinked(userMancomb, accountMancomb);

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
    }

    @Test
    public void test300DeleteDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test300DeleteDummyTeaGreenAccountMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestMappingInbound.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        dummyResourceTeaGreen.deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        display("Dummy (tea green) resource", dummyResourceTeaGreen.debugDump());

        // Make sure we have steady state
        waitForSyncTaskNextRun();

        // THEN
        TestUtil.displayThen(TEST_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_TEA_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNoShadow(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyTeaGreen, task, result);

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", user);
        assertNotNull("User mancomb disappeared", user);
        assertUser(user, null, ACCOUNT_MANCOMB_DUMMY_USERNAME, "Mancomb Seepgood", null, null);
        assertLinks(user, 0);

//        assertUsers(7 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
    }


    protected void importSyncTask() throws FileNotFoundException {
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILENAME);
    }

    protected void waitForSyncTaskStart() throws Exception {
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

    protected void waitForSyncTaskNextRun() throws Exception {
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

//	/**
//	 * Blue dummy has WEAK mappings. Let's play a bit with that.
//	 */
//	@Test
//    public void test100ModifyUserAssignAccountDummyBlue() throws Exception {
//		final String TEST_NAME = "test100ModifyUserJackAssignAccountDummyBlue";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
//        		RESOURCE_DUMMY_BLUE_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        String accountOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyBlueType);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyBlueType);
//
//        // Check account in dummy resource
//        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, "jack", "Jack Sparrow", true);
//        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, "jack", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
//        		"SystemConfiguration");
//        DummyAccount accountJackBlue = dummyResourceBlue.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
//        String drinkBlue = accountJackBlue.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
//        assertNotNull("No blue drink", drinkBlue);
//        UUID drinkUuidBlue = UUID.fromString(drinkBlue);
//        assertNotNull("No drink UUID", drinkUuidBlue);
//        display("Drink UUID", drinkUuidBlue.toString());
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test101ModifyUserFullName() throws Exception {
//		final String TEST_NAME = "test101ModifyUserFullName";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
//        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", null, dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test102ModifyUserOrganizationalUnit() throws Exception {
//		final String TEST_NAME = "test102ModifyUserOrganizationalUnit";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
//        		PrismTestUtil.createPolyString("Black Pearl"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test103ModifyAccountShip() throws Exception {
//		final String TEST_NAME = "test103ModifyAccountShip";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext, "Flying Dutchman");
//        deltas.add(accountDelta);
//
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", "Flying Dutchman", dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	/**
//	 * There is a weak mapping for ship attribute.
//	 * Therefore try to remove the value. The weak mapping should be applied.
//	 */
//	@Test
//    public void test104ModifyAccountShipReplaceEmpty() throws Exception {
//		final String TEST_NAME = "test104ModifyAccountShipReplaceEmpty";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext);
//        deltas.add(accountDelta);
//
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test105ModifyAccountShipAgain() throws Exception {
//		final String TEST_NAME = "test105ModifyAccountShipAgain";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext, "HMS Dauntless");
//        deltas.add(accountDelta);
//
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", "HMS Dauntless", dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	/**
//	 * There is a weak mapping for ship attribute.
//	 * Therefore try to remove the value. The weak mapping should be applied.
//	 */
//	@Test
//    public void test106ModifyAccountShipDelete() throws Exception {
//		final String TEST_NAME = "test106ModifyAccountShipDelete";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext, "HMS Dauntless");
//        deltas.add(accountDelta);
//
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Jack Sparrow", "Black Pearl", dummyResourceCtlBlue, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test109ModifyUserUnassignAccountBlue() throws Exception {
//		final String TEST_NAME = "test109ModifyUserUnassignAccountBlue";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, false);
//        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
//        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
//        deltas.add(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//
//	/**
//	 * Red dummy has STRONG mappings.
//	 */
//	@Test
//    public void test120ModifyUserAssignAccountDummyRed() throws Exception {
//		final String TEST_NAME = "test120ModifyUserAssignAccountDummyRed";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
//        		RESOURCE_DUMMY_RED_OID, null, true);
//        deltas.add(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        String accountOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyRedType);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyRedType);
//
//        // Check account in dummy resource
//        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);
//
// 		assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
// 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "mouth", "pistol");
// 		assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
// 				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- red resource");
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test121ModifyUserFullName() throws Exception {
//		final String TEST_NAME = "test121ModifyUserFullName";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
//        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", null, dummyResourceCtlRed, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test122ModifyUserOrganizationalUnit() throws Exception {
//		final String TEST_NAME = "test122ModifyUserOrganizationalUnit";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
//        		PrismTestUtil.createPolyString("Black Pearl"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test123ModifyAccountShip() throws Exception {
//		final String TEST_NAME = "test123ModifyAccountShip";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext, "Flying Dutchman");
//        deltas.add(accountDelta);
//
//		// WHEN
//        try {
//        	modelService.executeChanges(deltas, null, task, result);
//
//        	AssertJUnit.fail("Unexpected success");
//        } catch (SchemaException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//	}
//
//	/**
//	 * This test will not fail. It will splice the strong mapping into an empty replace delta.
//	 * That still results in a single value and is a valid operation, although it really changes nothing
//	 * (replace with the same value that was already there).
//	 */
//	@Test
//    public void test124ModifyAccountShipReplaceEmpty() throws Exception {
//		final String TEST_NAME = "test124ModifyAccountShipReplaceEmpty";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext);
//        deltas.add(accountDelta);
//
//        // WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//        // THEN
// 		result.computeStatus();
//         TestUtil.assertSuccess(result);
//
// 		userJack = getUser(USER_JACK_OID);
// 		display("User after change execution", userJack);
// 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
// 		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);
//
//         // Check audit
//         display("Audit", dummyAuditService);
//         dummyAuditService.assertSimpleRecordSanity();
//         dummyAuditService.assertRecords(2);
//         dummyAuditService.assertAnyRequestDeltas();
//         dummyAuditService.assertExecutionDeltas(1);
//         dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//         dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test126ModifyAccountShipDelete() throws Exception {
//		final String TEST_NAME = "test126ModifyAccountShipDelete";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
//        		accountOid, dummyResourceCtlRed.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		prismContext, "Black Pearl");
//        deltas.add(accountDelta);
//
//     // WHEN
//        try {
//        	modelService.executeChanges(deltas, null, task, result);
//
//        	AssertJUnit.fail("Unexpected success");
//        } catch (PolicyViolationException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", dummyResourceCtlRed, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//	}
//
//
//	/**
//	 * Note: red resource disables account on unsassign, does NOT delete it
//	 */
//	@Test
//    public void test128ModifyUserUnassignAccountRed() throws Exception {
//		final String TEST_NAME = "test128ModifyUserUnassignAccountRed";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//
//        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		String accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
//		PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
//
//		XMLGregorianCalendar trigStart = clock.currentTimeXMLGregorianCalendar();
//        trigStart.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
//        XMLGregorianCalendar trigEnd = clock.currentTimeXMLGregorianCalendar();
//        trigEnd.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
//		assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, trigStart, trigEnd);
//
//		XMLGregorianCalendar disableTimestamp = accountRed.asObjectable().getActivation().getDisableTimestamp();
//		IntegrationTestTools.assertBetween("Wrong disableTimestamp", start, end, disableTimestamp);
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", "Black Pearl", false, dummyResourceCtlRed, task);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	/**
//	 * Note: red resource disables account on unsassign, does NOT delete it
//	 * So let's delete the account explicitly to make room for the following tests
//	 */
//	@Test
//    public void test129DeleteAccountRed() throws Exception {
//		final String TEST_NAME = "test129DeleteAccountRed";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String acccountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createDeleteDelta(ShadowType.class, acccountRedOid, prismContext);
//        deltas.add(shadowDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//		assertNoLinkedAccount(userJack);
//
//        // Check if dummy resource accounts are gone
//        assertNoDummyAccount("jack");
//        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//
//	/**
//	 * Default dummy has combination of NORMAL, WEAK and STRONG mappings.
//	 */
//	@Test
//    public void test140ModifyUserAssignAccountDummyDefault() throws Exception {
//		final String TEST_NAME = "test140ModifyUserAssignAccountDummyDefault";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
//        		RESOURCE_DUMMY_OID, null, true);
//        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
//        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
//        deltas.add(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        String accountOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyType);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyType);
//
//        // Check account in dummy resource
//        assertDummyAccount(null, "jack", "Jack Sparrow", true);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	/**
//	 * fullName mapping is NORMAL, the change should go through
//	 */
//	@Test
//    public void test141ModifyUserFullName() throws Exception {
//		final String TEST_NAME = "test141ModifyUserFullName";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
//        		PrismTestUtil.createPolyString("Captain Jack Sparrow"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow");
//
//		assertAccountShip(userJack, "Captain Jack Sparrow", null, dummyResourceCtl, task);
//
//		// Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	/**
//	 * location mapping is STRONG
//	 */
//	@Test
//    public void test142ModifyUserLocality() throws Exception {
//		final String TEST_NAME = "test142ModifyUserLocality";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result,
//        		PrismTestUtil.createPolyString("Fountain of Youth"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//
//		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test143ModifyAccountLocation() throws Exception {
//		final String TEST_NAME = "test143ModifyAccountLocation";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
//        		prismContext, "Davie Jones Locker");
//        deltas.add(accountDelta);
//
//		// WHEN
//        try {
//        	modelService.executeChanges(deltas, null, task, result);
//
//        	AssertJUnit.fail("Unexpected success");
//        } catch (SchemaException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//
//		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//	}
//
//	/**
//	 * This test will not fail. It will splice the strong mapping into an empty replace delta.
//	 * That still results in a single value and is a valid operation, although it really changes nothing
//	 * (replace with the same value that was already there).
//	 */
//	@Test
//    public void test144ModifyAccountLocationReplaceEmpty() throws Exception {
//		final String TEST_NAME = "test144ModifyAccountLocationReplaceEmpty";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
//        		prismContext);
//        deltas.add(accountDelta);
//
//        // WHEN
//        modelService.executeChanges(deltas, null, task, result);
//
//        // THEN
// 		result.computeStatus();
//         TestUtil.assertSuccess(result);
//
// 		userJack = getUser(USER_JACK_OID);
// 		display("User after change execution", userJack);
// 		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//
// 		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);
//
//         // Check audit
//         display("Audit", dummyAuditService);
//         dummyAuditService.assertSimpleRecordSanity();
//         dummyAuditService.assertRecords(2);
//         dummyAuditService.assertAnyRequestDeltas();
//         dummyAuditService.assertExecutionDeltas(1);
//         dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//         dummyAuditService.assertExecutionSuccess();
//	}
//
//	@Test
//    public void test145ModifyAccountLocationDelete() throws Exception {
//		final String TEST_NAME = "test145ModifyAccountLocationDelete";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        String accountOid = getSingleLinkOid(userJack);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationDeleteProperty(ShadowType.class,
//        		accountOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME),
//        		prismContext, "Fountain of Youth");
//        deltas.add(accountDelta);
//
//     // WHEN
//        try {
//        	modelService.executeChanges(deltas, null, task, result);
//
//        	AssertJUnit.fail("Unexpected success");
//        } catch (PolicyViolationException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//
//		userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//
//		assertAccountLocation(userJack, "Captain Jack Sparrow", "Fountain of Youth", dummyResourceCtl, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//	}
//
//	@Test
//    public void test148ModifyUserRename() throws Exception {
//		final String TEST_NAME = "test148ModifyUserRename";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result,
//        		PrismTestUtil.createPolyString("renamedJack"));
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "renamedJack", "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//
//		assertAccountRename(userJack, "renamedJack", "Captain Jack Sparrow", dummyResourceCtl, task);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}
//
//
//	@Test
//    public void test149ModifyUserUnassignAccountDummy() throws Exception {
//		final String TEST_NAME = "test149ModifyUserUnassignAccountDummy";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestMappingInbound.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        dummyAuditService.clear();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "renamedJack", "Captain Jack Sparrow", "Jack", "Sparrow", "Fountain of Youth");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("renamedJack");
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.asserHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertExecutionSuccess();
//	}

	
	private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, true, resourceCtl, task);
	}
	
	private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, expectedEnabled, resourceCtl, task);
	}
	
	private void assertAccountLocation(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, expectedShip, true, resourceCtl, task);
	}
	
	private void assertAccountRename(PrismObject<UserType> userJack, String name, String expectedFullName,
			DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAccount(userJack, name, expectedFullName, null, null, true, resourceCtl, task);
	}
	
	private void assertAccount(PrismObject<UserType> userJack, String name, String expectedFullName, String shipAttributeName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		// ship inbound mapping is used, it is strong 
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, task.getResult());
        display("Repo shadow", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, name, resourceCtl.getResource().asObjectable());
        
        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, task.getResult());
        display("Model shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, name, resourceCtl.getResource().asObjectable());
        PrismAsserts.assertPropertyValue(accountModel, 
        		resourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
        		expectedFullName);
        if (shipAttributeName != null) {
	        if (expectedShip == null) {
	        	PrismAsserts.assertNoItem(accountModel, 
	            		resourceCtl.getAttributePath(shipAttributeName));        	
	        } else {
	        	PrismAsserts.assertPropertyValue(accountModel, 
	        		resourceCtl.getAttributePath(shipAttributeName),
	        		expectedShip);
	        }
        }
        
        // Check account in dummy resource
        assertDummyAccount(resourceCtl.getName(), name, expectedFullName, expectedEnabled);
	}
	
	private void assertAccount(PrismObject<UserType> userJack, String expectedFullName, String attributeName, String expectedShip,
			boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAccount(userJack, "jack", expectedFullName, attributeName, expectedShip, expectedEnabled, resourceCtl, task);
	}
	

}
