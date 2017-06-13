/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotifications extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/notifications");
	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	private String accountJackOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Test
	public void test100ModifyUserAddAccount() throws Exception {
		final String TEST_NAME = "test100ModifyUserAddAccount";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test100ModifyUserAddAccount");
		task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
		OperationResult result = task.getResult();
		preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
		assertShadowFetchOperationCountIncrement(0);

		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		assertUserJack(userJack);
		UserType userJackType = userJack.asObjectable();
		assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
		ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
		accountJackOid = accountRefType.getOid();
		assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
		PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
		assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
		assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

		// Check shadow
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
		assertEnableTimestampShadow(accountShadow, startTime, endTime);

		// Check account
		PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
		assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

		// Check account in dummy resource
		assertDefaultDummyAccount("jack", "Jack Sparrow", true);

		assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

		notificationManager.setDisabled(true);

		// Check notifications
		display("Dummy transport messages", dummyTransport);

		checkDummyTransportMessages("accountPasswordNotifier", 1);
		checkDummyTransportMessages("userPasswordNotifier", 0);
		checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
		checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
		checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
		checkDummyTransportMessages("simpleUserNotifier", 0);
		checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

		List<Message> pwdMessages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
		Message pwdMessage = pwdMessages.get(0);          // number of messages was already checked
		assertEquals("Invalid list of recipients", Collections.singletonList("recipient@evolveum.com"), pwdMessage.getTo());
		assertEquals("Wrong message body", "Password for account jack on Dummy Resource is: deadmentellnotales", pwdMessage.getBody());

		List<Message> addMessages = dummyTransport.getMessages("dummy:simpleAccountNotifier-ADD-SUCCESS");
		Message addMessage = addMessages.get(0);          // number of messages was already checked
		assertEquals("Invalid list of recipients", Collections.singletonList("recipient@evolveum.com"), addMessage.getTo());
		assertEquals("Wrong message body", "Notification about account-related operation\n"
				+ "\n"
				+ "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
				+ "\n"
				+ "An account has been successfully created on the resource with attributes:\n"
				+ " - UID: jack\n"
				+ " - Username: jack\n"
				+ " - Location: Caribbean\n"
				+ " - Quote: Arr!\n"
				+ " - Drink: rum\n"
				+ " - Weapon: rum\n"
				+ " - Full Name: Jack Sparrow\n"
				+ " - Password:\n"
				+ "    - value: (protected string)\n"
				+ " - Administrative status: ENABLED\n"
				+ "\n"
				+ "Channel: http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#user", addMessage.getBody());

		assertSteadyResources();
	}

	@Test
    public void test119ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test119ModifyUserDeleteAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);

		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);
        assertShadowFetchOperationCountIncrement(0);

		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());

		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        	AssertJUnit.fail("Shadow "+accountJackOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check notifications
		display("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

		String expected = "Notification about account-related operation\n"
				+ "\n"
				+ "Owner: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "Resource: Dummy Resource (oid 10000000-0000-0000-0000-000000000004)\n"
				+ "Account: jack\n"
				+ "\n"
				+ "The account has been successfully removed from the resource.\n"
				+ "\n"
				+ "Channel: ";
		assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleAccountNotifier-DELETE-SUCCESS").get(0).getBody());

        assertSteadyResources();
    }

	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME="test131ModifyUserJackAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertShadowFetchOperationCountIncrement(0);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertAssignments(userJack, 1);

        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check notifications
		display("Notifications", dummyTransport);

        notificationManager.setDisabled(true);
        checkDummyTransportMessages("accountPasswordNotifier", 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();

		String expected = "Notification about user-related operation (status: SUCCESS)\n"
				+ "\n"
				+ "User: Jack Sparrow (jack, oid c0c010c0-d34d-b33f-f00d-111111111111)\n"
				+ "\n"
				+ "The user record was modified. Modified attributes are:\n"
				+ " - Assignment:\n"
				+ "   - ADD: \n"
				+ "      - Construction:\n"
				+ "         - kind: ACCOUNT\n"
				+ "         - resourceRef: Dummy Resource (resource)\n"
				+ "\n"
				+ "Channel: ";
		assertEquals("Wrong message body", expected, dummyTransport.getMessages("dummy:simpleUserNotifier").get(0).getBody());

    }
//
//	/**
//	 * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
//	 */
//	@Test
//    public void test132ModifyAccountJackDummy() throws Exception {
//		final String TEST_NAME = "test132ModifyAccountJackDummy";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountJackOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
//        accountDelta.addModificationReplaceProperty(
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		"Queen Anne's Revenge");
//        deltas.add(accountDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // There is strong mapping. Complete account is fetched.
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		// Fullname inbound mapping is not used because it is weak
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// ship inbound mapping is used, it is strong
//		assertEquals("Wrong user locality (orig)", "The crew of Queen Anne's Revenge",
//				userJack.asObjectable().getOrganizationalUnit().iterator().next().getOrig());
//		assertEquals("Wrong user locality (norm)", "the crew of queen annes revenge",
//				userJack.asObjectable().getOrganizationalUnit().iterator().next().getNorm());
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        // All the changes should be reflected to the account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");
//        PrismAsserts.assertPropertyValue(accountModel,
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		"Queen Anne's Revenge");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
//        		"Queen Anne's Revenge");
//
//        assertDummyScriptsModify(userJack);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(3);
//        dummyAuditService.assertAnyRequestDeltas();
//
//        dummyAuditService.assertExecutionDeltas(0, 1);
//        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//        		dummyResourceCtl.getAttributeFullnamePath(), "Jack Sparrow");
////        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
////        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
//
//        dummyAuditService.assertExecutionDeltas(1, 1);
//        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
//
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertSteadyResources();
//    }
//
//	/**
//	 * MID-3080
//	 */
//	@Test
//    public void test135ModifyUserJackAssignAccountAgain() throws Exception {
//		final String TEST_NAME="test135ModifyUserJackAssignAccountAgain";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);
//
//		// THEN
//		TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//		assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
//        assertAssignments(userJack, 1);
//
//        accountJackOid = getSingleLinkOid(userJack);
//
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test139ModifyUserJackUnassignAccount() throws Exception {
//		final String TEST_NAME = "test139ModifyUserJackUnassignAccount";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
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
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertSteadyResources();
//    }
//
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test. The account should be added.
//	 */
//	@Test
//    public void test141ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test141ModifyUserJackAssignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//
//        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
//        // be compiled ... check this.
//        rememberScriptCompileCount();
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
//        // be compiled ... check this.
//        assertScriptCompileIncrement(0);
//
//        assertSteadyResources();
//    }
//
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test. The account should remain as it is.
//	 */
//	@Test
//    public void test148ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test148ModifyUserJackUnassignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName()
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//
//        //change resource assigment policy to be positive..if they are not applied by projector, the test will fail
//        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
//        // the previous command changes resource, therefore let's explicitly re-read it before test
//        // to refresh the cache and not affect the performance results (monitor).
//        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
//        assertResourceSchemaParseCountIncrement(1);
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // There is strong mapping. Complete account is fetched.
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//		assertLinked(userJack, accountJackOid);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//
//        // return resource to the previous state..delete assignment enforcement to prevent next test to fail..
//        deleteResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
//        // the previous command changes resource, therefore let's explicitly re-read it before test
//        // to refresh the cache and not affect the performance results (monitor).
//        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
//        assertResourceSchemaParseCountIncrement(1);
//    }
//
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
//	 * Now we will explicitly delete the account.
//	 */
//	@Test
//    public void test149ModifyUserJackDeleteAccount() throws Exception {
//		final String TEST_NAME = "test149ModifyUserJackDeleteAccount";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setOid(accountJackOid);
//		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
//		userDelta.addModification(accountRefDelta);
//
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);
//
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	/**
//	 * Assignment enforcement is set to RELATTIVE for this test. The account should be added.
//	 */
//	@Test
//    public void test151ModifyUserJackAssignAccountRelativeEnforcement() throws Exception {
//		final String TEST_NAME = "test151ModifyUserJackAssignAccountRelativeEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	/**
//	 * Assignment enforcement is set to RELATIVE for this test. The account should be gone.
//	 */
//	@Test
//    public void test158ModifyUserJackUnassignAccountRelativeEnforcement() throws Exception {
//		final String TEST_NAME = "test158ModifyUserJackUnassignAccountRelativeEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName()
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//
//        // WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	/**
//	 * Assignment enforcement is set to NONE for this test.
//	 */
//	@Test
//    public void test161ModifyUserJackAssignAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test161ModifyUserJackAssignAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.NONE);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        deltas.add(accountAssignmentUserDelta);
//
//        // Let's break the delta a bit. Projector should handle this anyway
//        breakAssignmentDelta(deltas);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsNone();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test163ModifyUserJackAddAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test163ModifyUserJackAddAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.NONE);
//
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//
//		dummyAuditService.clear();
//        dummyTransport.clearMessages();
//        notificationManager.setDisabled(false);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        notificationManager.setDisabled(true);
//
//        // Check notifications
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//	}
//
//	@Test
//    public void test164ModifyUserJackUnassignAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test164ModifyUserJackUnassignAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName()
//        		+ "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.NONE);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        deltas.add(accountAssignmentUserDelta);
//
//        // WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // Strong mappings
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        assertDummyScriptsNone();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        notificationManager.setDisabled(true);
//
//        // Check notifications
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test169ModifyUserJackDeleteAccountNoneEnforcement() throws Exception {
//		final String TEST_NAME = "test169ModifyUserJackDeleteAccountNoneEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.NONE);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setOid(accountJackOid);
//		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
//		userDelta.addModification(accountRefDelta);
//
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);
//
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 0);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test180ModifyUserAddAccountFullEnforcement() throws Exception {
//		final String TEST_NAME = "test180ModifyUserAddAccountFullEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//
//		try {
//
//			// WHEN
//			modelService.executeChanges(deltas, null, task, result);
//
//			AssertJUnit.fail("Unexpected executeChanges success");
//		} catch (PolicyViolationException e) {
//			// This is expected
//			display("Expected exception", e);
//		}
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check that shadow was not created
//        assertNoShadow(accountJackOid);
//
//        // Check that dummy resource account was not created
//        assertNoDummyAccount("jack");
//
//        assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0, 0);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//	}
//
//	@Test
//    public void test182ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
//		final String TEST_NAME = "test182ModifyUserAddAndAssignAccountPositiveEnforcement";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
//
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
//
//		XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//		assertShadowFetchOperationCountIncrement(0);
//		// Check accountRef
//		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//        assertUserJack(userJack);
//        UserType userJackType = userJack.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
//        accountJackOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
//        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
//        assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
//        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
//
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//	}
//
//	/**
//	 * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
//	 * Now we will explicitly delete the account.
//	 */
//	@Test
//    public void test189ModifyUserJackUnassignAndDeleteAccount() throws Exception {
//        TestUtil.displayTestTile(this, "test189ModifyUserJackUnassignAndDeleteAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test149ModifyUserJackUnassignAccount");
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
//        // Explicit unlink is not needed here, it should work without it
//
//		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackOid, prismContext);
//
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//	}
//
//	/**
//	 * We try to both assign an account and modify that account in one operation.
//	 * Some changes should be reflected to account (e.g.  weapon) as the mapping is weak, other should be
//	 * overridded (e.g. fullname) as the mapping is strong.
//	 */
//	@Test
//    public void test190ModifyUserJackAssignAccountAndModify() throws Exception {
//        TestUtil.displayTestTile(this, "test190ModifyUserJackAssignAccountAndModify");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test190ModifyUserJackAssignAccountAndModify");
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
//        ShadowDiscriminatorObjectDelta<ShadowType> accountDelta = ShadowDiscriminatorObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
//        accountDelta.addModificationAddProperty(
//        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
//        		"smell");
//        deltas.add(accountDelta);
//        deltas.add(accountAssignmentUserDelta);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Jack Sparrow");
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "smell");
//        assertNull("Unexpected loot", dummyAccount.getAttributeValue("loot", Integer.class));
//
//        assertDummyScriptsAdd(userJack, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//    /**
//     * We try to modify an assignment of the account and see whether changes will be recorded in the account itself.
//     *
//     */
//    @Test
//    public void test191ModifyUserJackModifyAssignment() throws Exception {
//    	final String TEST_NAME = "test191ModifyUserJackModifyAssignment";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//
//        //PrismPropertyDefinition definition = getAssignmentDefinition().findPropertyDefinition(new QName(SchemaConstantsGenerated.NS_COMMON, "accountConstruction"));
//
//        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
//
//        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(dummyResource, prismContext);
//        // This explicitly parses the schema, therefore ...
//        assertResourceSchemaParseCountIncrement(1);
//
//        RefinedObjectClassDefinition accountDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String) null);
//        PrismPropertyDefinition gossipDefinition = accountDefinition.findPropertyDefinition(new QName(
//                "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004",
//                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
//        assertNotNull("gossip attribute definition not found", gossipDefinition);
//
//        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
//        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
//        radt.setRef(new ItemPathType(new ItemPath(gossipDefinition.getName())));
//        MappingType outbound = new MappingType();
//        radt.setOutbound(outbound);
//
//        ExpressionType expression = new ExpressionType();
//        outbound.setExpression(expression);
//
//        MappingType value = new MappingType();
//
//        PrismProperty<String> property = gossipDefinition.instantiate();
//        property.add(new PrismPropertyValue<String>("q"));
//
//        List evaluators = expression.getExpressionEvaluator();
//        Collection<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property, null);
//        ObjectFactory of = new ObjectFactory();
//        for (JAXBElement<RawType> obj : collection) {
//            evaluators.add(of.createValue(obj.getValue()));
//        }
//
//        value.setExpression(expression);
//        radt.setOutbound(value);
//        accountConstruction.getAttribute().add(radt);
//        ObjectDelta<UserType> accountAssignmentUserDelta =
//                createReplaceAccountConstructionUserDelta(USER_JACK_OID, 1L, accountConstruction);
//        deltas.add(accountAssignmentUserDelta);
//
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        PrismObject<UserType> userJackOld = getUser(USER_JACK_OID);
//        display("User before change execution", userJackOld);
//        display("Deltas to execute execution", deltas);
//
//        // WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        modelService.executeChanges(deltas, null, task, result);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//        result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//
//        // First fetch: initial account read
//        // Second fetch: fetchback after modification to correctly process inbound
//        assertShadowFetchOperationCountIncrement(2);
//
//        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        display("User after change execution", userJack);
//        assertUserJack(userJack, "Jack Sparrow");
//        accountJackOid = getSingleLinkOid(userJack);
//
//        // Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
//        display(dummyAccount.debugDump());
//        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "q");
//        //assertEquals("Missing or incorrect attribute value", "soda", dummyAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class));
//
//        assertDummyScriptsModify(userJack, true);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        Collection<ObjectDeltaOperation<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
//        assertEquals("Wrong number of execution deltas", 2, auditExecutionDeltas.size());
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//    @Test
//    public void test195ModifyUserJack() throws Exception {
//    	final String TEST_NAME = "test195ModifyUserJack";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
//        		PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));
//
//		// THEN
//        TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // Strong mappings
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//
//        assertDummyScriptsModify(userJack);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
//
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
//        		UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
//        // We have full account here. It is loaded because of strong mapping.
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
//        		dummyResourceCtl.getAttributeFullnamePath(), "Cpt. Jack Sparrow");
//
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//    @Test
//    public void test196ModifyUserJackLocationEmpty() throws Exception {
//    	final String TEST_NAME = "test196ModifyUserJackLocationEmpty";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // Strong mappings
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");
//        IntegrationTestTools.assertNoAttribute(accountModel, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//
//        assertDummyScriptsModify(userJack);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//    @Test
//    public void test197ModifyUserJackLocationNull() throws Exception {
//    	final String TEST_NAME = "test197ModifyUserJackLocationNull";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        try {
//			// WHEN
//	        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, (PolyString)null);
//
//	        AssertJUnit.fail("Unexpected success");
//        } catch (IllegalStateException e) {
//        	// This is expected
//        }
//		// THEN
//		result.computeStatus();
//        TestUtil.assertFailure(result);
//        assertShadowFetchOperationCountIncrement(0);
//
//        assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        // This should fail even before the request record is created
//        dummyAuditService.assertRecords(0);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//	}
//
//	@Test
//    public void test198ModifyUserJackRaw() throws Exception {
//        TestUtil.displayTestTile(this, "test198ModifyUserJackRaw");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test196ModifyUserJackRaw");
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
//        		PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
//        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(objectDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", null);
//        accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
//
//        // Check account - the original fullName should not be changed
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");
//
//        // Check account in dummy resource - the original fullName should not be changed
//        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//
//        assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID); // MID-2451
//        dummyAuditService.assertExecutionSuccess();
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test199DeleteUserJack() throws Exception {
//        TestUtil.displayTestTile(this, "test199DeleteUserJack");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + ".test199DeleteUserJack");
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//		try {
//			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//			AssertJUnit.fail("Jack is still alive!");
//		} catch (ObjectNotFoundException ex) {
//			// This is OK
//		}
//
//        // Check is shadow is gone
//        assertNoShadow(accountJackOid);
//
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//
//        assertDummyScriptsDelete();
//
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 1);
//
//        assertScriptCompileIncrement(0);
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test200AddUserBlackbeardWithAccount() throws Exception {
//		final String TEST_NAME = "test200AddUserBlackbeardWithAccount";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        // Use custom channel to trigger a special outbound mapping
//        task.setChannel("http://pirates.net/avast");
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
//
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
//        UserType userBlackbeardType = userBlackbeard.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
//        String accountOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
//
//        assertEncryptedUserPassword(userBlackbeard, "QueenAnne");
//        assertPasswordMetadata(userBlackbeard, true, startTime, endTime, USER_ADMINISTRATOR_OID, "http://pirates.net/avast");
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
//        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
//        assertEquals("Wrong loot", (Integer)10000, dummyAccount.getAttributeValue("loot", Integer.class));
//
//        assertDummyScriptsAdd(userBlackbeard, accountModel, resourceDummyType);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(0, 3);
//        dummyAuditService.assertHasDelta(0, ChangeType.ADD, UserType.class);
//        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(0, ChangeType.ADD, ShadowType.class);
//        // this one was redundant
////        dummyAuditService.assertExecutionDeltas(1, 1);
////        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//        assertSteadyResources();
//    }
//
//
//	@Test
//    public void test210AddUserMorganWithAssignment() throws Exception {
//		final String TEST_NAME = "test210AddUserMorganWithAssignment";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
//        String accountOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
//
//        assertEncryptedUserPassword(userMorgan, "rum");
//        assertPasswordMetadata(userMorgan, true, startTime, endTime);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("morgan", "Sir Henry Morgan", true);
//
//        assertDummyScriptsAdd(userMorgan, accountModel, resourceDummyType);
//
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(3);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_MORGAN_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 1);
//        checkDummyTransportMessages("userPasswordNotifier", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//        assertSteadyResources();
//    }
//
//	@Test
//    public void test212RenameUserMorgan() throws Exception {
//		final String TEST_NAME = "test212RenameUserMorgan";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        modifyUserReplace(USER_MORGAN_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("sirhenry"));
//
//		// THEN
//        TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // Strong mappings
//        assertShadowFetchOperationCountIncrement(1);
//
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
//        String accountOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadowRepo = repositoryService.getObject(ShadowType.class, accountOid, null, result);
//        display("Shadow repo", accountShadowRepo);
//        assertDummyAccountShadowRepo(accountShadowRepo, accountOid, "sirhenry");
//
//        // Check account
//        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
//        display("Shadow model", accountShadowModel);
//        assertDummyAccountShadowModel(accountShadowModel, accountOid, "sirhenry", "Sir Henry Morgan");
//
//        // Check account in dummy resource
//        assertDefaultDummyAccount("sirhenry", "Sir Henry Morgan", true);
//
//        assertDummyScriptsModify(userMorgan);
//
//     // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(2);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        ObjectDeltaOperation<ShadowType> auditShadowDelta = dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
//
//        assertEquals("Unexpected number of modifications in shadow audit delta: "+auditShadowDelta.debugDump(), 3, auditShadowDelta.getObjectDelta().getModifications().size());
//
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
//        		UserType.F_NAME, PrismTestUtil.createPolyString("morgan"));
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
//        		new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), "morgan");
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
//        		new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_UID), "morgan");
//        // This is a side-effect change. It is silently done by provisioning. It is not supposed to
//        // appear in audit log.
////        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
////        		new ItemPath(ShadowType.F_NAME), PrismTestUtil.createPolyString("morgan"));
//
//        dummyAuditService.assertTarget(USER_MORGAN_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // Check notifications
//        notificationManager.setDisabled(true);
//        checkDummyTransportMessages("accountPasswordNotifier", 0);
//        checkDummyTransportMessages("userPasswordNotifier", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
//        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
//        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
//        checkDummyTransportMessages("simpleUserNotifier", 1);
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
//        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);
//
//        assertSteadyResources();
//    }
//
//	/**
//	 * This basically tests for correct auditing.
//	 */
//	@Test
//    public void test240AddUserCharlesRaw() throws Exception {
//		final String TEST_NAME = "test240AddUserCharlesRaw";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        PrismObject<UserType> user = createUser("charles", "Charles L. Charles");
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//        PrismObject<UserType> userAfter = findUserByUsername("charles");
//        assertNotNull("No charles", userAfter);
//        userCharlesOid = userAfter.getOid();
//
//        assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(userAfter.getOid());
//        dummyAuditService.assertExecutionSuccess();
//
//        assertSteadyResources();
//	}
//
//	/**
//	 * This basically tests for correct auditing.
//	 */
//	@Test
//    public void test241DeleteUserCharlesRaw() throws Exception {
//		final String TEST_NAME = "test241DeleteUserCharlesRaw";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, userCharlesOid, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//
//		// WHEN
//		modelService.executeChanges(deltas, ModelExecuteOptions.createRaw(), task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        assertShadowFetchOperationCountIncrement(0);
//
//        PrismObject<UserType> userAfter = findUserByUsername("charles");
//        assertNull("Charles is not gone", userAfter);
//
//		assertNoProvisioningScripts();
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1);
//        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
//     // raw operation, no target
////        dummyAuditService.assertTarget(userCharlesOid);
//        dummyAuditService.assertExecutionSuccess();
//
//        assertSteadyResources();
//	}
//
//	@Test
//    public void test300AddUserJackWithAssignmentBlue() throws Exception {
//		final String TEST_NAME="test300AddUserJackWithAssignmentBlue";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);
//
//        PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);
//        AssignmentType assignmentBlue = createAssignment(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null);
//		userJack.asObjectable().getAssignmentNew().add(assignmentBlue);
//
//        ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(userJack);
//
//        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
//
//		// WHEN
//        TestUtil.displayWhen(TEST_NAME);
//		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
//
//		// THEN
//		TestUtil.displayThen(TEST_NAME);
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
//        assertShadowFetchOperationCountIncrement(0);
//
//		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
//		display("User after change execution", userJackAfter);
//		assertUserJack(userJackAfter);
//        accountJackBlueOid = getSingleLinkOid(userJackAfter);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackBlueOid, null, result);
//        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, resourceDummyBlueType);
//        assertEnableTimestampShadow(accountShadow, startTime, endTime);
//
//        // Check account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackBlueOid, null, task, result);
//        assertShadowModel(accountModel, accountJackBlueOid, USER_JACK_USERNAME, resourceDummyBlueType, getAccountObjectClass(resourceDummyBlueType));
//        assertEnableTimestampShadow(accountModel, startTime, endTime);
//
//        // Check account in dummy resource
//        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_JACK_FULL_NAME, true);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(3);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(3);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        assertSteadyResources();
//    }
//
//	/**
//	 * modify account blue directly + request reconcile. check old value in delta.
//	 */
//	@Test
//    public void test302ModifyAccountJackDummyBlue() throws Exception {
//		final String TEST_NAME = "test302ModifyAccountJackDummyBlue";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestNotifications.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
//
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
//        		accountJackBlueOid, dummyResourceCtlBlue.getAttributeFullnamePath(), prismContext, "Cpt. Jack Sparrow");
//        accountDelta.addModificationReplaceProperty(
//        		dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
//        		"Queen Anne's Revenge");
//        deltas.add(accountDelta);
//
//		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();
//
//		// WHEN
//		modelService.executeChanges(deltas, options, task, result);
//
//		// THEN
//		result.computeStatus();
//        TestUtil.assertSuccess("executeChanges result", result);
//        // Not sure why 2 ... but this is not a big problem now
//        assertShadowFetchOperationCountIncrement(2);
//
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		// Fullname inbound mapping is not used because it is weak
//		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
//        String accountJackOid = getSingleLinkOid(userJack);
//
//		// Check shadow
//        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
//        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, resourceDummyBlueType);
//
//        // Check account
//        // All the changes should be reflected to the account
//        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
//        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, resourceDummyBlueType);
//
//        // Check account in dummy resource
//        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//
//        dummyAuditService.assertExecutionDeltas(0, 1);
//        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//        		dummyResourceCtlBlue.getAttributeFullnamePath(), "Jack Sparrow");
////        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
////        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
//
//        dummyAuditService.assertTarget(USER_JACK_OID);
//        dummyAuditService.assertExecutionSuccess();
//
//        // First time the blue resource is used
//        assertResourceSchemaParseCountIncrement(1);
//        assertSteadyResources();
//    }
	
	
	private void assertMessageContains(String message, String string) {
		assert message.contains(string) : "Expected message to contain '"+string+"' but it does not; message: " + message;
	}
	
	private void purgeScriptHistory() {
		getDummyResource().purgeScriptHistory();
	}

	private void assertNoProvisioningScripts() {
		if (!getDummyResource().getScriptHistory().isEmpty()) {
			IntegrationTestTools.displayScripts(getDummyResource().getScriptHistory());
			AssertJUnit.fail(getDummyResource().getScriptHistory().size()+" provisioning scripts were executed while not expected any");
		}
	}

	private void assertDummyScriptsAdd(PrismObject<UserType> user, PrismObject<? extends ShadowType> account, ResourceType resource) {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("\nto spiral :size\n" +
				"   if  :size > 30 [stop]\n   fd :size rt 15\n   spiral :size *1.02\nend\n			");
		
		String userName = null;
		if (user != null) {
			userName = user.asObjectable().getName().getOrig();
		}
		script.addArgSingle("usr", "user: "+userName);
		
		// Note: We cannot test for account name as name is only assigned in provisioning
		String accountEnabled = null;
		if (account != null && account.asObjectable().getActivation() != null 
				&& account.asObjectable().getActivation().getAdministrativeStatus() != null) {
			accountEnabled = account.asObjectable().getActivation().getAdministrativeStatus().toString();
		}
		script.addArgSingle("acc", "account: "+accountEnabled);

		String resourceName = null;
		if (resource != null) {
			resourceName = resource.getName().getOrig();
		}
		script.addArgSingle("res", "resource: "+resourceName);

		script.addArgSingle("size", "3");
		script.setLanguage("Logo");
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
	}

	private void assertDummyScriptsModify(PrismObject<UserType> user) {
		assertDummyScriptsModify(user, false);
	}
	
	private void assertDummyScriptsModify(PrismObject<UserType> user, boolean recon) {
		ProvisioningScriptSpec modScript = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
		String name = null;
		String fullName = null;
		String costCenter = null;
		if (user != null) {
			name = user.asObjectable().getName().getOrig();
			fullName = user.asObjectable().getFullName().getOrig();
			costCenter = user.asObjectable().getCostCenter();
		}
		modScript.addArgSingle("howMuch", costCenter);
		modScript.addArgSingle("howLong", "from here to there");
		modScript.addArgSingle("who", name);
		modScript.addArgSingle("whatchacallit", fullName);
		if (recon) {
			ProvisioningScriptSpec reconBeforeScript = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
			reconBeforeScript.addArgSingle("who", name);
			ProvisioningScriptSpec reconAfterScript = new ProvisioningScriptSpec("He left it dead, and with its head");
			reconAfterScript.addArgSingle("how", "enabled");
			IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), reconBeforeScript, modScript, reconAfterScript);
		} else {
			IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), modScript);
		}
	}

	private void assertDummyScriptsDelete() {
		ProvisioningScriptSpec script = new ProvisioningScriptSpec("The Jabberwock, with eyes of flame");
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
	}
	
	private void assertDummyScriptsNone() {
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
	}
	
	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeScriptHistory();
        rememberShadowFetchOperationCount();
	}

}
