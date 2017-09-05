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
package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
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
		TestUtil.displayTestTitle(this, TEST_NAME);

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
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

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

	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
	}

}
