/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.intest.password;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractPasswordTest extends AbstractInitializedModelIntegrationTest {

	protected static final String USER_PASSWORD_1_CLEAR = "d3adM3nT3llN0Tal3s";
	protected static final String USER_PASSWORD_2_CLEAR = "bl4ckP3arl";
	protected static final String USER_PASSWORD_3_CLEAR = "wh3r3sTheRum?";
	protected static final String USER_PASSWORD_4_CLEAR = "sh1v3rM3T1mb3rs";
	protected static final String USER_PASSWORD_5_CLEAR = "s3tSa1al";
	protected static final String USER_PASSWORD_AA_CLEAR = "AA"; // too short
	protected static final String USER_PASSWORD_A_CLEAR = "A"; // too short
	protected static final String USER_PASSWORD_JACK_CLEAR = "12jAcK34"; // contains username
	protected static final String USER_PASSWORD_SPARROW_CLEAR = "spaRRow123"; // contains familyName
	protected static final String USER_PASSWORD_VALID_1 = "abcd123";
	protected static final String USER_PASSWORD_VALID_2 = "abcd223";
	protected static final String USER_PASSWORD_VALID_3 = "abcd323";
	protected static final String USER_PASSWORD_VALID_4 = "abcd423";
	
	protected static final String PASSWORD_ALLIGATOR = "all1g4t0r";
	protected static final String PASSWORD_CROCODILE = "cr0c0d1l3";
	protected static final String PASSWORD_GIANT_LIZARD = "G14NTl1z4rd";

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "password");

	protected static final File RESOURCE_DUMMY_UGLY_FILE = new File(TEST_DIR, "resource-dummy-ugly.xml");
	protected static final String RESOURCE_DUMMY_UGLY_OID = "10000000-0000-0000-0000-000000344104";
	protected static final String RESOURCE_DUMMY_UGLY_NAME = "ugly";

	protected static final File RESOURCE_DUMMY_LIFECYCLE_FILE = new File(TEST_DIR, "resource-dummy-lifecycle.xml");
	protected static final String RESOURCE_DUMMY_LIFECYCLE_OID = "519f131a-147b-11e7-a270-c38e2b225751";
	protected static final String RESOURCE_DUMMY_LIFECYCLE_NAME = "lifecycle";
	
	protected static final File RESOURCE_DUMMY_SOUVENIR_FILE = new File(TEST_DIR, "resource-dummy-souvenir.xml");
	protected static final String RESOURCE_DUMMY_SOUVENIR_OID = "f4fd7e90-ff6a-11e7-a504-4b84f92fec0e";
	protected static final String RESOURCE_DUMMY_SOUVENIR_NAME = "souvenir";
	
	protected static final File RESOURCE_DUMMY_MAVERICK_FILE = new File(TEST_DIR, "resource-dummy-maverick.xml");
	protected static final String RESOURCE_DUMMY_MAVERICK_OID = "72a928b6-ff7b-11e7-9643-7366d7749c31";
	protected static final String RESOURCE_DUMMY_MAVERICK_NAME = "maverick";

	protected static final File PASSWORD_POLICY_UGLY_FILE = new File(TEST_DIR, "password-policy-ugly.xml");
	protected static final String PASSWORD_POLICY_UGLY_OID = "cfb3fa9e-027a-11e7-8e2c-dbebaacaf4ee";
	
	protected static final File PASSWORD_POLICY_MAVERICK_FILE = new File(TEST_DIR, "password-policy-maverick.xml");
	protected static final String PASSWORD_POLICY_MAVERICK_OID = "b26d2bd4-ff83-11e7-94b3-8fa7a87aac6c";

	protected static final File SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE = new File(TEST_DIR, "security-policy-default-storage-hashing.xml");
	protected static final String SECURITY_POLICY_DEFAULT_STORAGE_HASHING_OID = "0ea3b93c-0425-11e7-bbc1-73566dc53d59";

	protected static final File SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE = new File(TEST_DIR, "security-policy-password-storage-none.xml");
	protected static final String SECURITY_POLICY_PASSWORD_STORAGE_NONE_OID = "2997a20a-0423-11e7-af65-a7ab7d19442c";

	protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_BAD = "No1";
	protected static final String USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD = "pir321";
	protected static final String USER_RAPP_EMAIL = "rapp.scallion@evolveum.com";

	protected String accountJackOid;
	protected String accountJackRedOid;
	protected String accountJackBlueOid;
	protected String accountJackUglyOid;
	protected String accountJackBlackOid;
	protected String accountJackYellowOid;
	protected String accountJackSouvenirOid;
	protected String accountJackMaverickOid;
	protected XMLGregorianCalendar lastPasswordChangeStart;
	protected XMLGregorianCalendar lastPasswordChangeEnd;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		InternalsConfig.setAvoidLoggingChange(true);
		super.initSystem(initTask, initResult);

		importObjectFromFile(PASSWORD_POLICY_UGLY_FILE);
		importObjectFromFile(SECURITY_POLICY_DEFAULT_STORAGE_HASHING_FILE);
		importObjectFromFile(SECURITY_POLICY_PASSWORD_STORAGE_NONE_FILE);

		setGlobalSecurityPolicy(getSecurityPolicyOid(), initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_UGLY_NAME, RESOURCE_DUMMY_UGLY_FILE, RESOURCE_DUMMY_UGLY_OID, initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_LIFECYCLE_NAME, RESOURCE_DUMMY_LIFECYCLE_FILE, RESOURCE_DUMMY_LIFECYCLE_OID, initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_SOUVENIR_NAME, RESOURCE_DUMMY_SOUVENIR_FILE, RESOURCE_DUMMY_SOUVENIR_OID, initTask, initResult);

		importObjectFromFile(PASSWORD_POLICY_MAVERICK_FILE);
		initDummyResourcePirate(RESOURCE_DUMMY_MAVERICK_NAME, RESOURCE_DUMMY_MAVERICK_FILE, RESOURCE_DUMMY_MAVERICK_OID, initTask, initResult);

		login(USER_ADMINISTRATOR_USERNAME);
	}

	protected abstract String getSecurityPolicyOid();

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);

        AccountActivationNotifierType accountActivationNotifier = null;
        SystemConfigurationType systemConfig = getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value()).asObjectable();
        IntegrationTestTools.displayXml("system config", systemConfig.asPrismObject());
        for (EventHandlerType handler: systemConfig.getNotificationConfiguration().getHandler()) {
        	display("Handler: ", handler);
        	List<AccountActivationNotifierType> accountActivationNotifiers = handler.getAccountActivationNotifier();
        	if (!accountActivationNotifiers.isEmpty()) {
        		accountActivationNotifier = accountActivationNotifiers.get(0);
        	}
        }

        display("Account activation notifier", accountActivationNotifier);
        assertNotNull("No accountActivationNotifier", accountActivationNotifier);
	}

	@Test
    public void test010AddPasswordPolicy() throws Exception {
		final String TEST_NAME = "test010AddPasswordPolicy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		// WHEN
        PrismObject<ObjectType> passwordPolicy = addObject(PASSWORD_POLICY_GLOBAL_FILE, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong OID after add", PASSWORD_POLICY_GLOBAL_OID, passwordPolicy.getOid());

		// Check object
        PrismObject<ValuePolicyType> valuePolicy = repositoryService.getObject(ValuePolicyType.class, PASSWORD_POLICY_GLOBAL_OID, null, result);

        // TODO: more asserts
	}

	@Test
    public void test050CheckJackPassword() throws Exception {
		final String TEST_NAME = "test050CheckJackPassword";
        displayTestTitle(TEST_NAME);

        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added

        // THEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

		// Password still encrypted. We haven't changed it yet.
		assertUserPassword(userJack, USER_JACK_PASSWORD, CredentialsStorageTypeType.ENCRYPTION);
	}


	@Test
    public void test051ModifyUserJackPassword() throws Exception {
		final String TEST_NAME = "test051ModifyUserJackPassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertPasswordMetadata(userJack, false, startCal, endCal);
		// Password policy is not active yet. No history should be kept.
		assertPasswordHistoryEntries(userJack);

		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
	}

	@Test
    public void test060CheckJackPasswordModelInteraction() throws Exception {
		final String TEST_NAME = "test060CheckJackPasswordModelInteraction";
        displayTestTitle(TEST_NAME);

        if (getPasswordStorageType() == CredentialsStorageTypeType.NONE) {
        	// Nothing to check in this case
        	return;
        }

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN, THEN
        ProtectedStringType userPasswordPsGood = new ProtectedStringType();
        userPasswordPsGood.setClearValue(USER_PASSWORD_1_CLEAR);
        assertTrue("Good password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsGood, task, result));

        ProtectedStringType userPasswordPsBad = new ProtectedStringType();
        userPasswordPsBad.setClearValue("this is not a password");
        assertFalse("Bad password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsBad, task, result));

        ProtectedStringType userPasswordPsEmpty = new ProtectedStringType();
        assertFalse("Empty password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, userPasswordPsEmpty, task, result));

        assertFalse("Null password check failed",
        		modelInteractionService.checkPassword(USER_JACK_OID, null, task, result));

	}

	@Test
    public void test070AddUserHerman() throws Exception {
		final String TEST_NAME = "test070AddUserHerman";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(USER_HERMAN_FILE, task, result);

		// THEN
        TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_HERMAN_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_HERMAN_OID, USER_HERMAN_USERNAME,
				USER_HERMAN_FULL_NAME, USER_HERMAN_GIVEN_NAME, USER_HERMAN_FAMILY_NAME);

		assertUserPassword(userAfter, USER_HERMAN_PASSWORD);
		assertPasswordMetadata(userAfter, true, startCal, endCal);
		// Password policy is not active yet. No history should be kept.
		assertPasswordHistoryEntries(userAfter);

		assertSingleUserPasswordNotification(USER_HERMAN_USERNAME, USER_HERMAN_PASSWORD);
	}

	@Test
    public void test100JackAssignAccountDummy() throws Exception {
		final String TEST_NAME = "test100JackAssignAccountDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        display("Repo shadow", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountJackOid, USER_JACK_USERNAME);
        // MID-3860
        assertShadowPasswordMetadata(accountShadowRepo, startCal, endCal, false, true);
        assertShadowLifecycle(accountShadowRepo, false);

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        display("Model shadow", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountJackOid, USER_JACK_USERNAME, USER_JACK_FULL_NAME);
        assertShadowPasswordMetadata(accountShadowModel, startCal, endCal, false, true);
        assertShadowLifecycle(accountShadowModel, false);

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        assertDummyPasswordConditional(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        assertSingleAccountPasswordNotificationConditional(null, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
        assertNoUserPasswordNotifications();
	}

	/**
	 * This time user has assigned account. Account password should be changed as well.
	 */
	@Test
    public void test110ModifyUserJackPassword() throws Exception {
		final String TEST_NAME = "test110ModifyUserJackPassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_2_CLEAR, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

		assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_2_CLEAR);
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		// Check shadow
        PrismObject<ShadowType> accountShadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        display("Repo shadow", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountJackOid, "jack");
        // MID-3860
        assertShadowPasswordMetadata(accountShadowRepo, lastPasswordChangeStart, lastPasswordChangeEnd, true, false);
        assertShadowLifecycle(accountShadowRepo, false);

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        display("Model shadow", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountJackOid, "jack", "Jack Sparrow");
        assertShadowPasswordMetadata(accountShadowModel, lastPasswordChangeStart, lastPasswordChangeEnd, true, false);
        assertShadowLifecycle(accountShadowModel, false);

        assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_2_CLEAR);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_2_CLEAR);
	}

	/**
	 * Modify account password. User's password should be unchanged
	 */
	@Test
    public void test111ModifyAccountJackPassword() throws Exception {
		final String TEST_NAME = "test111ModifyAccountJackPassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        modifyAccountChangePassword(accountJackOid, USER_PASSWORD_3_CLEAR, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_2_CLEAR);
		// Account has new password
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_3_CLEAR);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_3_CLEAR);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Modify both user and account password. As password outbound mapping is weak the user should have its own password
	 * and account should have its own password.
	 */
	@Test
    public void test112ModifyJackPasswordUserAndAccount() throws Exception {
		final String TEST_NAME = "test112ModifyJackPasswordUserAndAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs4 = new ProtectedStringType();
        userPasswordPs4.setClearValue(USER_PASSWORD_4_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs4);

        ProtectedStringType userPasswordPs5 = new ProtectedStringType();
        userPasswordPs5.setClearValue(USER_PASSWORD_5_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackOid, getDummyResourceObject(),
        		PASSWORD_VALUE_PATH, userPasswordPs5);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, "Jack Sparrow");

		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
		// Account has new password
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_5_CLEAR);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertSingleAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_5_CLEAR);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_4_CLEAR);
	}


	/**
	 * Add red and ugly dummy resource to the mix. This would be fun.
	 */
	@Test
    public void test120JackAssignAccountDummyRedAndUgly() throws Exception {
		final String TEST_NAME = "test120JackAssignAccountDummyRedAndUgly";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_UGLY_OID, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 3);
        accountJackRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        accountJackUglyOid = getLinkRefOid(userJack, RESOURCE_DUMMY_UGLY_OID);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_4_CLEAR);

		assertDummyAccount(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null, true);
		assertDummyPasswordConditional(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		// User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_4_CLEAR);
     	assertDummyPassword(USER_JACK_USERNAME, USER_PASSWORD_5_CLEAR);

     	assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

     	if (isPasswordEncryption()) {
	     	assertAccountPasswordNotifications(2);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_4_CLEAR);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER);
     	} else {
     		assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER);
     	}
		assertNoUserPasswordNotifications();
	}

	/**
	 * Modify both user and account password. Red dummy has a strong password mapping. User change should override account
	 * change.
	 */
	@Test
    public void test121ModifyJackPasswordUserAndAccountRed() throws Exception {
		final String TEST_NAME = "test121ModifyJackPasswordUserAndAccountRed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs1 = new ProtectedStringType();
        userPasswordPs1.setClearValue(USER_PASSWORD_1_CLEAR);
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, PASSWORD_VALUE_PATH, userPasswordPs1);

        ProtectedStringType userPasswordPs2 = new ProtectedStringType();
        userPasswordPs2.setClearValue(USER_PASSWORD_2_CLEAR);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountJackRedOid, getDummyResourceObject(),
        		PASSWORD_VALUE_PATH, userPasswordPs2);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, USER_JACK_FULL_NAME);

		// User should still have old password
		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		// Red account has the same account as user
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ... and default account has also the same password as user now. There was no other change on default dummy instance
		// so even the weak mapping took place.
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		assertLinks(userJack, 3);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		displayAccountPasswordNotifications();
		assertAccountPasswordNotifications(2);
		assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
	}

	/**
	 * MID-3682
	 */
	@Test
    public void test122ModifyAccountUglyJackPasswordBad() throws Exception {
		final String TEST_NAME = "test122ModifyAccountUglyJackPasswordBad";
        displayTestTitle(TEST_NAME);
        prepareTest();

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        try {
        	// WHEN
        	displayWhen(TEST_NAME);
        	modifyAccountChangePassword(accountJackUglyOid, "#badPassword!", task, result);

        	fail("Expected policy violation because password doesn't satisfy password policy but didn't get one.");
        } catch (PolicyViolationException ex) {
        	// THEN
        	displayThen(TEST_NAME);
        	assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);
        }

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}


	/**
	 * Jack employee number is mapped to ugly resource password.
	 * Change employee number to something that does NOT comply with ugly resource password policy.
	 * MID-3769
	 */
	@Test
    public void test125ModifyJackEmployeeNumberBad() throws Exception {
		final String TEST_NAME = "test125ModifyJackEmployeeNumberBad";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        try {
			// WHEN
	        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result,
	        		USER_JACK_EMPLOYEE_NUMBER_NEW_BAD);
	        assertNotReached();
        } catch (PolicyViolationException e) {
        	// this is expected
        	display("Expected exception", e);
        }

		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after", userJack);

		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ugly password should be changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER);

		assertLinks(userJack, 3);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Jack employee number is mapped to ugly resource password.
	 * Change employee number to something that does comply with ugly resource password policy.
	 * MID-3769
	 */
	@Test
    public void test128ModifyJackEmployeeNumberGood() throws Exception {
		final String TEST_NAME = "test128ModifyJackEmployeeNumberGood";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result,
        		USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after", userJack);

		assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		// ugly password should be changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertLinks(userJack, 3);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, USER_JACK_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Black resource has minimum password length constraint (enforced by midPoint).
	 */
	@Test
    public void test130JackAssignAccountDummyBlack() throws Exception {
		final String TEST_NAME = "test130JackAssignAccountDummyBlack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
        accountJackBlackOid = getLinkRefOid(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check account in dummy resource (black)
        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertPasswordHistoryEntries(userJack);

		assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_BLACK_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
		assertNoUserPasswordNotifications();
	}

	/**
	 * MID-3682
	 */
	@Test
    public void test132ModifyAccountBlackJackPasswordBad() throws Exception {
		final String TEST_NAME = "test132ModifyAccountBlackJackPasswordBad";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractPasswordTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        try {
        	// WHEN
        	displayWhen(TEST_NAME);
        	modifyAccountChangePassword(accountJackBlackOid, USER_PASSWORD_A_CLEAR, task, result);

        	fail("Expected policy violation because password doesn't satisfy password policy but didn't get one.");
        } catch (PolicyViolationException ex) {
        	// THEN
        	displayThen(TEST_NAME);
        	assertDummyPasswordConditional(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
        }

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	@Test
    public void test139JackUnassignAccountDummyBlack() throws Exception {
		final String TEST_NAME = "test139JackUnassignAccountDummyBlack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 3);
		assertNotLinked(userJack, accountJackBlackOid);

        // Check account in dummy resource (black)
        assertNoDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertPasswordHistoryEntries(userJack);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Yellow resource has minimum password length constraint (enforced by resource).
	 * But this time the password is OK.
	 */
	@Test
    public void test140JackAssignAccountDummyYellow() throws Exception {
		final String TEST_NAME = "test140JackAssignAccountDummyYellow";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

		// THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);
        accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource (yellow)
        DummyAccount dummyAccountYellow = assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        display("Yellow dummy account", dummyAccountYellow);
        assertDummyPasswordConditionalGenerated(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Other resources should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_1_CLEAR);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertPasswordHistoryEntries(userJack);

		displayAccountPasswordNotifications();
		assertSingleAccountPasswordNotificationConditionalGenerated(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME, USER_PASSWORD_1_CLEAR);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Yellow resource has minimum password length constraint. Change password to something shorter.
	 * MID-3033, MID-2134
	 */
	@Test
    public void test142ModifyUserJackPasswordAA() throws Exception {
		final String TEST_NAME = "test142ModifyUserJackPasswordAA";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_AA_CLEAR, task, result);

		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertPartialError(result);

		lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 4);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // Check account in dummy resource (red)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userJack, USER_PASSWORD_AA_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordHistoryEntries(userJack);

		displayAccountPasswordNotifications();
		displayUserPasswordNotifications();
		assertAccountPasswordNotifications(2);
		assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_AA_CLEAR);
	}

	@Test
    public void test200ApplyPasswordPolicyHistoryLength() throws Exception {
		final String TEST_NAME = "test200ApplyPasswordPolicyHistoryLength";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
		displayWhen(TEST_NAME);
		applyPasswordPolicy(PASSWORD_POLICY_GLOBAL_OID, getSecurityPolicyOid(), task, result);
		modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
				new ItemPath(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_LENGTH),
        		task, result, 3);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
	}

	// test202 is in subclasses. Different behavior for encryption and hashing

	/**
	 * Unassign red account. Red resource has a strong password mapping. That would cause a lot
	 * of trouble. We have tested that enough.
	 */
	@Test
    public void test204UnassignAccountRed() throws Exception {
		final String TEST_NAME = "test204UnassignAccountRed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertLinks(userBefore, 4);

		// Red resource has disable-instead-of-delete. So we need to be brutal to get rid of the red account
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
		ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountJackRedOid, prismContext);

		// WHEN
		displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta, shadowDelta), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);
		assertNotLinked(userAfter, accountJackRedOid);

		assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordHistoryEntries(userAfter);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Reconcile user after password policy change. Nothing should be changed in the user.
	 * Password history should still be empty. We haven't changed the password yet.
	 * MID-3567
	 */
	@Test
    public void test206ReconcileUserJack() throws Exception {
		final String TEST_NAME = "test206ReconcileUserJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertLinks(userBefore, 3);

		// WHEN
        reconcileUser(USER_JACK_OID, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_1_CLEAR);

        // User and default dummy account should have unchanged passwords
        assertUserPassword(userAfter, USER_PASSWORD_AA_CLEAR);
     	assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_AA_CLEAR);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordHistoryEntries(userAfter);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Change to password that complies with password policy.
	 */
	@Test
    public void test210ModifyUserJackPasswordGood() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test210ModifyUserJackPasswordGood",
				USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Reconcile user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test212ReconcileUserJack() throws Exception {
		final String TEST_NAME = "test212ReconcileUserJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        reconcileUser(USER_JACK_OID, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Recompute user. Nothing should be changed.
	 * MID-3567
	 */
	@Test
    public void test214RecomputeUserJack() throws Exception {
		final String TEST_NAME = "test214RecomputeUserJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Change to password that violates the password policy (but is still OK for yellow resource).
	 */
	@Test
    public void test220ModifyUserJackPasswordBadA() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test220ModifyUserJackPasswordBadA",
				USER_PASSWORD_1_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Change to password that violates the password policy (but is still OK for yellow resource).
	 * Use a different delta (container delta instead of property delta).
	 * MID-2857
	 */
	@Test
    public void test222ModifyUserJackPasswordBadContainer() throws Exception {
		final String TEST_NAME = "test222ModifyUserJackPasswordBadContainer";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
		userPasswordPs.setClearValue(USER_PASSWORD_1_CLEAR);
		PasswordType passwordType = new PasswordType();
		passwordType.setValue(userPasswordPs);

		ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceContainer(UserType.class, USER_JACK_OID,
				new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD),
						prismContext, passwordType);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

        try {
			// WHEN
    		modelService.executeChanges(deltas, null, task, result);

	        AssertJUnit.fail("Unexpected success");

        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Expected exception", e);
        }

		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);

        assertJackPasswordsWithHistory(USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Change to password that violates the password policy (contains username)
	 * MID-1657
	 */
	@Test
    public void test224ModifyUserJackPasswordBadJack() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test224ModifyUserJackPasswordBadJack",
				USER_PASSWORD_JACK_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Change to password that violates the password policy (contains family name)
	 * MID-1657
	 */
	@Test
    public void test226ModifyUserJackPasswordBadSparrow() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test226ModifyUserJackPasswordBadSparrow",
				USER_PASSWORD_SPARROW_CLEAR, USER_PASSWORD_VALID_1, USER_PASSWORD_AA_CLEAR);
	}

	/**
	 * Change to password that complies with password policy. Again. See that
	 * the change is applied correctly and that it is included in the history.
	 */
	@Test
    public void test230ModifyUserJackPasswordGoodAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test230ModifyUserJackPasswordGoodAgain",
				USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
	}

	/**
	 * Change to password that is good but it is the same as current password.
	 */
	@Test
    public void test235ModifyUserJackPasswordGoodSameAsCurrent() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test235ModifyUserJackPasswordGoodSameAsCurrent",
				USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
	}

	/**
	 * Change to password that is good but it is already in the history.
	 */
	@Test
    public void test236ModifyUserJackPasswordGoodInHistory() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test236ModifyUserJackPasswordGoodInHistory",
				USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
	}

	/**
	 * Change to password that is bad and it is already in the history.
	 */
	@Test
    public void test237ModifyUserJackPasswordBadInHistory() throws Exception {
		doTestModifyUserJackPasswordFailureWithHistory("test237ModifyUserJackPasswordBadInHistory",
				USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_2, USER_PASSWORD_AA_CLEAR, USER_PASSWORD_VALID_1);
	}

	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test240ModifyUserJackPasswordGoodAgainOverHistory() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test240ModifyUserJackPasswordGoodAgainOverHistory",
				USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_2);
	}

	/**
	 * Change to password that complies with password policy. Again.
	 * This time there are enough passwords in the history. So the history should
	 * be truncated.
	 */
	@Test
    public void test241ModifyUserJackPasswordGoodAgainOverHistoryAgain() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test241ModifyUserJackPasswordGoodAgainOverHistoryAgain",
				USER_PASSWORD_VALID_4, USER_PASSWORD_VALID_2, USER_PASSWORD_VALID_3);
	}

	/**
	 * Reuse old password. Now the password should be out of the history, so
	 * the system should allow its reuse.
	 */
	@Test
    public void test248ModifyUserJackPasswordGoodReuse() throws Exception {
		doTestModifyUserJackPasswordSuccessWithHistory("test248ModifyUserJackPasswordGoodReuse",
				USER_PASSWORD_VALID_1, USER_PASSWORD_VALID_3, USER_PASSWORD_VALID_4);
	}

	private void doTestModifyUserJackPasswordSuccessWithHistory(final String TEST_NAME,
			String newPassword, String... expectedPasswordHistory) throws Exception {
		displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        assertJackPasswordsWithHistory(newPassword, expectedPasswordHistory);

        displayAccountPasswordNotifications();
		assertAccountPasswordNotifications(2);
		assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, newPassword);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_YELLOW_NAME, USER_JACK_USERNAME, newPassword);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, newPassword);
	}

	private void doTestModifyUserJackPasswordFailureWithHistory(final String TEST_NAME,
			String newPassword, String oldPassword, String... expectedPasswordHistory) throws Exception {
		displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        try {
			// WHEN
	        modifyUserChangePassword(USER_JACK_OID, newPassword, task, result);

	        AssertJUnit.fail("Unexpected success");

        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Exected exception", e);
        }

		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);

        assertJackPasswordsWithHistory(oldPassword, expectedPasswordHistory);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
	}

	private void assertJackPasswordsWithHistory(String expectedCurrentPassword, String... expectedPasswordHistory) throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 3);
        accountJackYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        assertUserPassword(userJack, expectedCurrentPassword);
        assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

        assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, expectedCurrentPassword);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertPasswordHistoryEntries(userJack, expectedPasswordHistory);
	}

	// TODO: add user with password that violates the policy

	/**
	 * Create an org, and create two parentOrgRefs for jack (MID-3099).
	 * Change to password that violates the password policy.
	 */
	@Test
	public void test300TwoParentOrgRefs() throws Exception {
		final String TEST_NAME = "test300TwoParentOrgRefs";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null);
		assignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER);

		UserType jack = getUser(USER_JACK_OID).asObjectable();
		display("jack", jack);
		assertEquals("Wrong # of parentOrgRefs", 2, jack.getParentOrgRef().size());

		ObjectDelta<OrgType> orgDelta = (ObjectDelta<OrgType>) DeltaBuilder.deltaFor(OrgType.class, prismContext)
				.item(OrgType.F_PASSWORD_POLICY_REF).replace(new PrismReferenceValue(PASSWORD_POLICY_GLOBAL_OID))
				.asObjectDelta(ORG_GOVERNOR_OFFICE_OID);
		executeChanges(orgDelta, null, task, result);

		OrgType govOffice = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID).asObjectable();
		display("governor's office", govOffice);
		assertEquals("Wrong OID of password policy ref", PASSWORD_POLICY_GLOBAL_OID, govOffice.getPasswordPolicyRef().getOid());

		try {
			// WHEN
			modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_1_CLEAR, task, result);

			AssertJUnit.fail("Unexpected success");
		} catch (PolicyViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

		// THEN
		result.computeStatus();
		TestUtil.assertFailure(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertLinks(userJack, 3);

		// Make sure that the password is unchanged

		assertUserPassword(userJack, USER_PASSWORD_VALID_1);
		assertPasswordMetadata(userJack, false, lastPasswordChangeStart, lastPasswordChangeEnd);

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
		assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Prepare system for password strength tests that follow.
	 * Also unassign yellow resource (requires non-empty password), all orgs, and remove default password policy.
	 */
	@Test
	public void test310PreparePasswordStrengthTests() throws Exception {
		final String TEST_NAME = "test310PreparePasswordStrengthTests";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, null, task, result);
		unassignOrg(USER_JACK_OID, ORG_GOVERNOR_OFFICE_OID, SchemaConstants.ORG_MANAGER, task, result);
		modifyObjectReplaceReference(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, task, result);

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
		assertLinks(userAfter, 4);

		// password mapping is normal
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

		// password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_1);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterAssignment(userAfter);

		assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		if (isPasswordEncryption()) {
			assertAccountPasswordNotifications(2);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_1);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_1);

		} else {
			assertNoAccountPasswordNotifications();
		}
		assertNoUserPasswordNotifications();
	}

	@Test
	public void test312ChangeUserPassword() throws Exception {
		final String TEST_NAME = "test312ChangeUserPassword";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_2, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_2);
		assertLinks(userAfter, 4);

		// password mapping is normal
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_2);

		// password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_2);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

		assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		displayAccountPasswordNotifications();
		if (isPasswordEncryption()) {
			assertAccountPasswordNotifications(2);
			assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
	     	// not BLUE, it already has a password
		} else {
			assertAccountPasswordNotifications(3);
			assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
	     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
		}
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_2);
	}

	protected abstract void assert31xBluePasswordAfterAssignment(PrismObject<UserType> userAfter) throws Exception;

	protected abstract void assert31xBluePasswordAfterPasswordChange(PrismObject<UserType> userAfter) throws Exception;

	/*
	 *  Policy prevents the password from being removed.
	 */
	@Test
	public void test314RemovePasswordFail() throws Exception {
		final String TEST_NAME = "test314RemovePasswordFail";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		setPasswordMinOccurs(1, task, result);

		try {
			// WHEN+THEN
			TestUtil.displayWhen(TEST_NAME);
			try {
				modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result /*, no value */);
				fail("unexpected success");
			} catch (PolicyViolationException e) {
				assertMessage(e, "Provided password does not satisfy the policies: The value must be present.");
			}
		} finally {
			setPasswordMinOccurs(null, task, result);
		}
		assertNoUserPasswordNotifications();
	}

	private void setPasswordMinOccurs(Integer value, Task task, OperationResult result) throws CommonException {
		ObjectDelta<SecurityPolicyType> delta = DeltaBuilder.deltaFor(SecurityPolicyType.class, prismContext)
				.item(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_OCCURS)
						.replace(value)
				.asObjectDeltaCast(getSecurityPolicyOid());
		executeChanges(delta, null, task, result);
	}

	/*
	 *  Remove password. It should be removed from red resource as well. (MID-3111)
	 */
	@Test
	public void test315RemovePassword() throws Exception {
		final String TEST_NAME = "test315RemovePassword";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task, result /*, no value */);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertNull("User password is not null", userAfter.asObjectable().getCredentials().getPassword().getValue());
		assertLinks(userAfter, 4);

		// password mapping is normal
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, null);

		// password mapping is strong
		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is weak here - so no change is expected
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

		assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		displayAllNotifications();
		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/*
	 *  User has no password. There is an inbound/generate mapping from default dummy
	 *  resource. This should kick in now and set a random password.
	 */
	@Test
	public void test316UserRecompute() throws Exception {
		final String TEST_NAME = "test316UserRecompute";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_JACK_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertNotNull("User password is still null", userAfter.asObjectable().getCredentials().getPassword().getValue());
		assertLinks(userAfter, 4);

		// TODO: why are the resource passwords null ???

		// password mapping is normal
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, null);

		// password mapping is strong
		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

        // password mapping is weak here - so no change is expected
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

		assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		displayAccountPasswordNotifications();
		assertNoAccountPasswordNotifications();
		assertUserPasswordNotifications(1);
	}

	/**
	 * Change password again so we have predictable password instead of
	 * randomly-generated one.
	 */
	@Test
	public void test318ChangeUserPassword() throws Exception {
		final String TEST_NAME = "test318ChangeUserPassword";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_VALID_3, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		assertLinks(userAfter, 4);

		// password mapping is normal
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

		// password mapping is strong
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

        // password mapping is weak
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assert31xBluePasswordAfterPasswordChange(userAfter);

		// this one is not changed
		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_EMPLOYEE_NUMBER_NEW_GOOD);

		displayAccountPasswordNotifications();
		assertAccountPasswordNotifications(2);
		assertHasAccountPasswordNotification(null, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
     	// not BLUE, it already has a password
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
	}

	@Test
	public void test320ChangeEmployeeNumber() throws Exception {
		final String TEST_NAME = "test320ChangeEmployeeNumber";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result, "emp0000");

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		//assertUserJack(userJack, "Jack Sparrow");			// we changed employeeNumber, so this would fail
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
		assert31xBluePasswordAfterPasswordChange(userAfter);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "emp0000");

		assertSingleAccountPasswordNotification(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "emp0000");
		assertNoUserPasswordNotifications();
	}

	@Test
	public void test330RemoveEmployeeNumber() throws Exception {
		final String TEST_NAME = "test330RemoveEmployeeNumber";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modifyUserReplace(USER_JACK_OID, UserType.F_EMPLOYEE_NUMBER, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after change execution", userAfter);
		//assertUserJack(userJack, "Jack Sparrow");					// we changed employeeNumber, so this would fail
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
		assert31xBluePasswordAfterPasswordChange(userAfter);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);

		assertDummyPassword(RESOURCE_DUMMY_UGLY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, null);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * Add user with password and an assignment. check that the account is provisioned and has password.
	 * Tests proper initial cleartext password handling in all cases.
	 */
	@Test
	public void test400AddUserRappWithAssignment() throws Exception {
		final String TEST_NAME = "test400AddUserRappWithAssignment";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_RAPP_FILE);
		AssignmentType assignmentType = createConstructionAssignment(RESOURCE_DUMMY_OID, null, null);
		UserType userBeforeType = userBefore.asObjectable();
		userBeforeType
			.fullName(createPolyStringType(USER_RAPP_FULLNAME))
			.emailAddress(USER_RAPP_EMAIL); // Make sure Rapp has e-mail address otherwise the notifications will not be sent to transport
		userBeforeType.getAssignment().add(assignmentType);
		setPassword(userBefore, USER_PASSWORD_VALID_1);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		addObject(userBefore, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		String accountOid = getSingleLinkOid(userAfter);

		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, true);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, true);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountPasswordNotification(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
		assertSingleUserPasswordNotification(USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
	}

	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test401UserRappRecompute() throws Exception {
		final String TEST_NAME = "test401UserRappRecompute";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		String accountOid = getSingleLinkOid(userAfter);

		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, true);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, true);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
        assertNoUserPasswordNotifications();
	}

	/**
	 * add new assignment to the user, check that account is provisioned and has correct lifecycle
	 */
	@Test
	public void test402AssignRappDummyRed() throws Exception {
		final String TEST_NAME = "test402AssignRappDummyRed";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_RAPP_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 2);

		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, false);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, false);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertAccountActivationNotification(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test403UserRappRecompute() throws Exception {
		final String TEST_NAME = "test403UserRappRecompute";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 2);

		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, false);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, false);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * initialize the account (password and lifecycle delta), check account password and lifecycle
	 */
	@Test
	public void test404InitializeRappDummyRed() throws Exception {
		final String TEST_NAME = "test404InitializeRappDummyRed";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);
		String accountRedOid = getLinkRefOid(userBefore, RESOURCE_DUMMY_RED_OID);

		ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountRedOid, USER_PASSWORD_VALID_1);

		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(shadowDelta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 2);

		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Make sure recompute does not destroy the situation.
	 */
	@Test
	public void test405UserRappRecompute() throws Exception {
		final String TEST_NAME = "test405UserRappRecompute";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 2);

		String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);
		String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

        // Check account in dummy RED resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	/**
	 * add new assignment to the user. This resource has explicit lifecycle mapping.
	 */
	@Test
	public void test410AssignRappDummyLifecycle() throws Exception {
		final String TEST_NAME = "test410AssignRappDummyLifecycle";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		assignAccount(USER_RAPP_OID, RESOURCE_DUMMY_LIFECYCLE_OID, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);

		String accountLifecycleOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_LIFECYCLE_OID);

		assertDummyAccount(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPasswordConditional(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

		PrismObject<ShadowType> accountShadowLifecycle = repositoryService.getObject(ShadowType.class, accountLifecycleOid, null, result);
		display("Repo shadow LIFECYCLE", accountShadowLifecycle);
		assertAccountShadowRepo(accountShadowLifecycle, accountLifecycleOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_LIFECYCLE_NAME));
		assertShadowLifecycle(accountShadowLifecycle, false);
//        assertShadowLifecycle(accountShadowLifecycle, SchemaConstants.LIFECYCLE_ACTIVE);


        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
		assertNoUserPasswordNotifications();
	}

	@Test
	public void test412InitializeRappDummyLifecycle() throws Exception {
		final String TEST_NAME = "test412InitializeRappDummyLifecycle";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);
		String accountLifecycleOid = getLinkRefOid(userBefore, RESOURCE_DUMMY_LIFECYCLE_OID);

		ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountLifecycleOid, prismContext);
		ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(USER_PASSWORD_VALID_1);
        shadowDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, passwordPs);
        shadowDelta.addModificationReplaceProperty(ObjectType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		executeChanges(shadowDelta, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);

		accountLifecycleOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_LIFECYCLE_OID);

		assertDummyAccount(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

		PrismObject<ShadowType> accountShadowLifecycle = repositoryService.getObject(ShadowType.class, accountLifecycleOid, null, result);
		display("Repo shadow LIFECYCLE", accountShadowLifecycle);
		assertAccountShadowRepo(accountShadowLifecycle, accountLifecycleOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_LIFECYCLE_NAME));
        assertShadowLifecycle(accountShadowLifecycle, SchemaConstants.LIFECYCLE_ACTIVE);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // RED shadows
        String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
        String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
		assertNoUserPasswordNotifications();
	}

	@Test
	public void test414UserRappRecompute() throws Exception {
		final String TEST_NAME = "test414UserRappRecompute";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		displayWhen(TEST_NAME);
		recomputeUser(USER_RAPP_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);

		assertDummyAccount(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // LIFECYCLE shadows

        String accountLifecycleOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_LIFECYCLE_OID);

		PrismObject<ShadowType> accountShadowLifecycle = repositoryService.getObject(ShadowType.class, accountLifecycleOid, null, result);
		display("Repo shadow LIFECYCLE", accountShadowLifecycle);
		assertAccountShadowRepo(accountShadowLifecycle, accountLifecycleOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_LIFECYCLE_NAME));
        assertShadowLifecycle(accountShadowLifecycle, SchemaConstants.LIFECYCLE_ACTIVE);

        // RED shadows
        String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
        String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	@Test
	public void test416UserRappEmployeeTypeWreck() throws Exception {
		final String TEST_NAME = "test416UserRappEmployeeTypeWreck";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		PrismObject<UserType> userBefore = getUser(USER_RAPP_OID);
		display("User before", userBefore);

		// WHEN
		displayWhen(TEST_NAME);
		modifyUserReplace(USER_RAPP_OID, UserType.F_EMPLOYEE_TYPE, task, result, "WRECK");

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
		display("User after", userAfter);
		assertLinks(userAfter, 3);

		assertDummyAccount(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_LIFECYCLE_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_RED_NAME, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);
        assertUserPassword(userAfter, USER_PASSWORD_VALID_1);
        assertDefaultDummyAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyPassword(null, USER_RAPP_USERNAME, USER_PASSWORD_VALID_1);

        // LIFECYCLE shadows

        String accountLifecycleOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_LIFECYCLE_OID);

		PrismObject<ShadowType> accountShadowLifecycle = repositoryService.getObject(ShadowType.class, accountLifecycleOid, null, result);
		display("Repo shadow LIFECYCLE", accountShadowLifecycle);
		assertAccountShadowRepo(accountShadowLifecycle, accountLifecycleOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_LIFECYCLE_NAME));
        assertShadowLifecycle(accountShadowLifecycle, SchemaConstants.LIFECYCLE_ARCHIVED);

        // RED shadows
        String accountRedOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_RED_OID);

		PrismObject<ShadowType> accountShadowRed = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
		display("Repo shadow RED", accountShadowRed);
		assertAccountShadowRepo(accountShadowRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountShadowRed, SchemaConstants.LIFECYCLE_ACTIVE);

        PrismObject<ShadowType> accountModelRed = modelService.getObject(ShadowType.class, accountRedOid, null, task, result);
        display("Model shadow RED", accountModelRed);
        assertAccountShadowModel(accountModelRed, accountRedOid, USER_RAPP_USERNAME, getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertShadowLifecycle(accountModelRed, SchemaConstants.LIFECYCLE_ACTIVE);

        // DEFAULT shadows
        String accountDefaultOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountDefaultOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountDefaultOid, USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        assertShadowLifecycle(accountModel, null);

		PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountDefaultOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountDefaultOid, USER_RAPP_USERNAME);
        assertShadowLifecycle(accountShadow, null);

        assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();

	}
	// TODO: employeeType->WRECK
	
	/**
	 * MID-4397
	 */
	@Test
	public void test500JackAssignResourceSouvenir() throws Exception {
		final String TEST_NAME = "test500JackAssignResourceSouvenir";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// preconditions
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignedAccount(userBefore, RESOURCE_DUMMY_OID);
		assertAssignedAccount(userBefore, RESOURCE_DUMMY_RED_OID);
		assertAssignedAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);
		assertAssignedAccount(userBefore, RESOURCE_DUMMY_UGLY_OID);
		assertAssignments(userBefore, 4);
		assertAccount(userBefore, RESOURCE_DUMMY_OID);
		assertAccount(userBefore, RESOURCE_DUMMY_RED_OID);
		accountJackBlueOid = assertAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);
		assertAccount(userBefore, RESOURCE_DUMMY_UGLY_OID);
		assertLinks(userBefore, 4);
		assertUserPassword(userBefore, USER_PASSWORD_VALID_3);

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_SOUVENIR_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 5);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_RED_OID);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_UGLY_OID);
		assertAssignedAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
		assertLinks(userAfter, 5);
		assertAccount(userAfter, RESOURCE_DUMMY_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_RED_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
		assertAccount(userAfter, RESOURCE_DUMMY_UGLY_OID);
		accountJackSouvenirOid = assertAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		
		PrismObject<ShadowType> shadowPasswordCachingModel = getShadowModel(accountJackSouvenirOid);
		assertShadowLifecycle(shadowPasswordCachingModel, false);

		assertDummyPasswordConditional(RESOURCE_DUMMY_SOUVENIR_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
		
		displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_SOUVENIR_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        assertAccountActivationNotification(RESOURCE_DUMMY_SOUVENIR_NAME, USER_JACK_USERNAME);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * MID-4397
	 */
	@Test
	public void test502JackInitializeAccountSouvenir() throws Exception {
		final String TEST_NAME = "test502JackInitializeAccountSouvenir";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackSouvenirOid, PASSWORD_ALLIGATOR);
		
		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(shadowDelta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 5);
		assertLinks(userAfter, 5);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		
		PrismObject<ShadowType> shadowPasswordCachingRepo = getShadowRepo(accountJackSouvenirOid);
		display("Shadow repo", shadowPasswordCachingRepo);
		assertShadowLifecycle(shadowPasswordCachingRepo, SchemaConstants.LIFECYCLE_ACTIVE);
		assertCachedResourcePassword(shadowPasswordCachingRepo, PASSWORD_ALLIGATOR);

		assertDummyPassword(RESOURCE_DUMMY_SOUVENIR_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_ALLIGATOR);
		
		ItemComparisonResult comparisonResult = provisioningService.compare(ShadowType.class, accountJackSouvenirOid, SchemaConstants.PATH_PASSWORD_VALUE, 
				PASSWORD_ALLIGATOR, task, result);
		assertEquals("Wrong comparison result", ItemComparisonResult.MATCH, comparisonResult);

		// TODO
//		assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);

		assertNoUserPasswordNotifications();
	}
	
	/**
	 * MID-4397
	 */
	@Test
	public void test510JackAssignResourceMaverick() throws Exception {
		final String TEST_NAME = "test510JackAssignResourceMaverick";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// preconditions
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignments(userBefore, 5);
		assertLinks(userBefore, 5);
		assertUserPassword(userBefore, USER_PASSWORD_VALID_3);

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_MAVERICK_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 6);
		assertLinks(userAfter, 6);
		accountJackMaverickOid = assertAccount(userAfter, RESOURCE_DUMMY_MAVERICK_OID);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		
		assertDummyPasswordConditional(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
		
		displayAllNotifications();
        assertSingleAccountPasswordNotificationConditional(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, USER_PASSWORD_VALID_3);
        assertAccountActivationNotification(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * Attempt to initialize account with password which is the same as on the souvenir
	 * resource should fail. Maverick resource password policy states that the password
	 * must be different that the password on souvenir resource.
	 * MID-4397
	 */
	@Test
	public void test512JackInitializeAccountMaverickAlligator() throws Exception {
		final String TEST_NAME = "test512JackInitializeAccountMaverick";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackMaverickOid, PASSWORD_ALLIGATOR);
		
		try {
			// WHEN
			displayWhen(TEST_NAME);
			executeChanges(shadowDelta, null, task, result);
			
			assertNotReached();
		
		} catch (PolicyViolationException e) {
			display("Expected exception", e);
		}

		// THEN
		displayThen(TEST_NAME);
		assertFailure(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 6);
		assertLinks(userAfter, 6);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		
		assertDummyPasswordConditional(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_VALID_3);
		assertNoUserPasswordNotifications();
		
		// TODO
//		assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);
	}

	/**
	 * Attempt to initialize account with a different password should pass.
	 * MID-4397
	 */
	@Test
	public void test514JackInitializeAccountMaverickCrododile() throws Exception {
		final String TEST_NAME = "test514JackInitializeAccountMaverickCrododile";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		ObjectDelta<ShadowType> shadowDelta = createAccountInitializationDelta(accountJackMaverickOid, PASSWORD_CROCODILE);
		
		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(shadowDelta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 6);
		assertLinks(userAfter, 6);
		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);
		
		PrismObject<ShadowType> shadowMaverickRepo = getShadowRepo(accountJackMaverickOid);
		display("Shadow repo", shadowMaverickRepo);
		assertShadowLifecycle(shadowMaverickRepo, SchemaConstants.LIFECYCLE_ACTIVE);

		assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_CROCODILE);
		
		// TODO
//		assertSingleAccountInitializationPasswordNotification(RESOURCE_DUMMY_PASSWORD_CACHING_NAME, USER_JACK_USERNAME, PASSWORD_Alligator);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * Attempt to change account password to a conflicting one. It should fail.
	 * MID-4397
	 */
	@Test
	public void test516JackChangePasswordResourceMaverickAlligator() throws Exception {
		final String TEST_NAME = "test516JackChangePasswordResourceMaverickAlligator";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		try {
			// WHEN
			displayWhen(TEST_NAME);
			modifyAccountChangePassword(accountJackMaverickOid, PASSWORD_ALLIGATOR, task, result);
			
			assertNotReached();
			
		} catch (PolicyViolationException e) {
			display("Expected exception", e);
		}

		// THEN
		displayThen(TEST_NAME);
		assertFailure(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 6);
		assertLinks(userAfter, 6);
		
		assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, PASSWORD_CROCODILE);
		assertNoUserPasswordNotifications();
	}

	/**
	 * Attempt to change account password to non conflicting one. It should pass.
	 * MID-4397
	 */
	@Test
	public void test518JackChangePasswordResourceMaverickGiantLizard() throws Exception {
		final String TEST_NAME = "test518JackChangePasswordResourceMaverickGiantLizard";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// WHEN
		displayWhen(TEST_NAME);
		modifyAccountChangePassword(accountJackMaverickOid, PASSWORD_GIANT_LIZARD, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 6);
		assertLinks(userAfter, 6);
		
		assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, USER_JACK_USERNAME, PASSWORD_GIANT_LIZARD);
		assertNoUserPasswordNotifications();
	}

	/**
	 * MID-4397
	 */
	@Test
	public void test530JackUnassignResourceSouvenir() throws Exception {
		final String TEST_NAME = "test530JackUnassignResourceSouvenir";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// preconditions
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignments(userBefore, 6);
		assertLinks(userBefore, 6);

		// WHEN
		displayWhen(TEST_NAME);
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_SOUVENIR_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 5);
		assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
		assertLinks(userAfter, 5);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * Make it harder for the next test (test535ModifyUserJackPasswordAlligator).
	 * We set conflicting password on blue resource. But the password policy
	 * should NOT check blue resource. So the check should pass anyway.
	 * MID-4397
	 */
	@Test
	public void test532JackChangePasswordResourceBlueAlligator() throws Exception {
		final String TEST_NAME = "test532JackChangePasswordResourceBlueAlligator";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignments(userBefore, 5);
		assertLinks(userBefore, 5);
		accountJackBlueOid = assertAccount(userBefore, RESOURCE_DUMMY_BLUE_OID);

		// WHEN
		displayWhen(TEST_NAME);
		modifyAccountChangePassword(accountJackBlueOid, PASSWORD_ALLIGATOR, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 5);
		assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
		assertLinks(userAfter, 5);
		
		assertDummyPassword(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, PASSWORD_ALLIGATOR);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * Additional check: check that the Alligator password would be otherwise legal for user and
	 * maverick resource. This is additional check that it was really the presence of souvenir
	 * resource that caused the policy violation.
	 * MID-4397
	 */
	@Test
	public void test535ModifyUserJackPasswordAlligator() throws Exception {
		final String TEST_NAME = "test535ModifyUserJackPasswordAlligator";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// WHEN
		displayWhen(TEST_NAME);
		modifyUserChangePassword(USER_JACK_OID, PASSWORD_ALLIGATOR, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 5);
		assertLinks(userAfter, 5);
		assertUserPassword(userAfter, PASSWORD_ALLIGATOR);
		
		assertDummyPassword(RESOURCE_DUMMY_MAVERICK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, PASSWORD_ALLIGATOR);
		assertSingleUserPasswordNotification(USER_JACK_USERNAME, PASSWORD_ALLIGATOR);
	}
	
	/**
	 * MID-4397
	 */
	@Test
	public void test539JackUnassignResourceMaverick() throws Exception {
		final String TEST_NAME = "test539JackUnassignResourceMaverick";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();
		
		// preconditions
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignments(userBefore, 5);
		assertLinks(userBefore, 5);

		// WHEN
		displayWhen(TEST_NAME);
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_MAVERICK_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertAssignments(userAfter, 4);
		assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_MAVERICK_OID);
		assertAssignedNoAccount(userAfter, RESOURCE_DUMMY_SOUVENIR_OID);
		assertLinks(userAfter, 4);
		assertNoUserPasswordNotifications();
	}
	
	/**
	 * MID-4507
	 */
	@Test
	public void test550JackManyPasswordChangesClear() throws Exception {
		testJackManyPasswordChanges("test550JackManyPasswordChangesClear", "TesT550x", null);
	}

	/**
	 * MID-4507
	 */
	@Test
	public void test552JackManyPasswordChangesEncrypted() throws Exception {
		testJackManyPasswordChanges("test552JackManyPasswordChangesEncrypted", "TesT552x", CredentialsStorageTypeType.ENCRYPTION);
	}

	/**
	 * MID-4507
	 */
	public void testJackManyPasswordChanges(final String TEST_NAME, String passwordPrefix, CredentialsStorageTypeType storageType) throws Exception {
		displayTestTitle(TEST_NAME);

		// GIVEN
		prepareTest();
		
		// preconditions
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		display("User before", userBefore);
		assertAssignments(userBefore, 4);
		assertLinks(userBefore, 4);

		for (int i = 1; i < 10; i++) {
			testJackManyPasswordChangesAttempt(TEST_NAME, passwordPrefix, storageType, i);
		}
		
	}
	
	private void testJackManyPasswordChangesAttempt(String TEST_NAME, String passwordPrefix, CredentialsStorageTypeType storageType, int i) throws Exception {
		Task task = createTask(TEST_NAME + "-" + i);
		OperationResult result = task.getResult();

		String newPassword = passwordPrefix + i;
		ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        if (storageType == CredentialsStorageTypeType.ENCRYPTION) {
        	protector.encrypt(userPasswordPs);
        }
		
		// WHEN
		displayWhen(TEST_NAME + "-" + i);
        modifyUserReplace(USER_JACK_OID, PASSWORD_VALUE_PATH, task,  result, userPasswordPs);

		// THEN
		displayThen(TEST_NAME + "-" + i);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertUserPassword(userAfter, newPassword);
		assertAssignments(userAfter, 4);
		assertLinks(userAfter, 4);
		assertDummyPassword(ACCOUNT_JACK_DUMMY_USERNAME, newPassword);
		assertDummyPassword(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, newPassword);
	}

	protected ObjectDelta<ShadowType> createAccountInitializationDelta(String accountOid, String newAccountPassword) {
		ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
		ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newAccountPassword);
        shadowDelta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, passwordPs);
        shadowDelta.addModificationReplaceProperty(ObjectType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE);
        return shadowDelta;
	}

	protected void assertDummyPassword(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		assertDummyPassword(null, userId, expectedClearPassword);
	}

	protected void assertDummyPasswordConditional(String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		if (isPasswordEncryption()) {
			assertDummyPassword(null, userId, expectedClearPassword);
		}
	}

	protected void assertDummyPasswordConditional(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		if (isPasswordEncryption()) {
			super.assertDummyPassword(instance, userId, expectedClearPassword);
		}
	}

	protected void assertDummyPasswordConditionalGenerated(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		if (isPasswordEncryption()) {
			super.assertDummyPassword(instance, userId, expectedClearPassword);
		} else {
			assertDummyPasswordNotEmpty(instance, userId);
		}
	}

	protected void assertSingleAccountPasswordNotificationConditional(String dummyResourceName, String username, String password) {
		if (isPasswordEncryption()) {
			assertSingleAccountPasswordNotification(dummyResourceName, username, password);
		}
	}

	protected void assertSingleAccountPasswordNotificationConditionalGenerated(String dummyResourceName, String username, String password) {
		if (isPasswordEncryption()) {
			assertSingleAccountPasswordNotification(dummyResourceName, username, password);
		} else {
			assertSingleAccountPasswordNotificationGenerated(dummyResourceName, username);
		}
	}

	private void assertSingleAccountInitializationPasswordNotification(String dummyResourceName, String username, String password) {
		assertSingleAccountPasswordNotification(dummyResourceName, username, password);
	}

	protected abstract void assertAccountActivationNotification(String dummyResourceName, String username);

	protected abstract void assertShadowLifecycle(PrismObject<ShadowType> shadow, boolean focusCreated);

	protected void assertShadowLifecycle(PrismObject<ShadowType> shadow, String expectedLifecycle) {
		if (expectedLifecycle == null) {
			String actualLifecycle = shadow.asObjectable().getLifecycleState();
			if (actualLifecycle != null && !SchemaConstants.LIFECYCLE_ACTIVE.equals(actualLifecycle)) {
				fail("Expected default lifecycle for "+shadow+", but was "+actualLifecycle);
			}
		} else {
			PrismAsserts.assertPropertyValue(shadow, ObjectType.F_LIFECYCLE_STATE, expectedLifecycle);
		}
	}

	private void assertShadowPasswordMetadata(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar startCal, XMLGregorianCalendar endCal, boolean clearPasswordAvailable, boolean passwordCreated) {
		if (!clearPasswordAvailable && getPasswordStorageType() == CredentialsStorageTypeType.HASHING) {
			return;
		}
		assertShadowPasswordMetadata(shadow, passwordCreated, startCal, endCal, USER_ADMINISTRATOR_OID, SchemaConstants.CHANNEL_GUI_USER_URI);
	}
	
	// 9XX tests: Password minimal age, no password, etc.

	/**
	 * Let's have a baseline for other 90x tests.
	 */
	@Test
    public void test900ModifyUserElainePassword() throws Exception {
		final String TEST_NAME = "test900ModifyUserElainePassword";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();
        
        PrismObject<UserType> userBefore = getUser(USER_ELAINE_OID);
		display("User before", userBefore);

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_1, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
		display("User after", userAfter);

		assertUserPassword(userAfter, USER_PASSWORD_VALID_1);

		displayAccountPasswordNotifications();
		assertAccountPasswordNotifications(3);
		assertHasAccountPasswordNotification(null, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_BLUE_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_1);
	}

	@Test
    public void test902SetPasswordMinAge() throws Exception {
		final String TEST_NAME = "test900SetPasswordMinAge";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

		// WHEN
		modifyObjectReplaceProperty(SecurityPolicyType.class, getSecurityPolicyOid(),
				new ItemPath(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_AGE),
        		task, result, XmlTypeConverter.createDuration("PT10M"));

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<SecurityPolicyType> securityPolicy = getObject(SecurityPolicyType.class, getSecurityPolicyOid());
		display("Security policy after", securityPolicy);
		PrismAsserts.assertPropertyValue(securityPolicy,
				new ItemPath(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_MIN_AGE),
				XmlTypeConverter.createDuration("PT10M"));

		assertNoAccountPasswordNotifications();
	}

	/**
	 * Password modification is obviously before the password minAge has passed.
	 * Therefore this should fail.
	 */
	@Test
    public void test904ModifyUserElainePasswordAgain() throws Exception {
		final String TEST_NAME = "test904ModifyUserElainePasswordAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        try {

			// WHEN
	        modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_2, task, result);

	        assertNotReached();

        } catch (PolicyViolationException e) {

        }

		// THEN
		result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
		display("User after", userAfter);

		assertUserPassword(userAfter, USER_PASSWORD_VALID_1);

		assertNoAccountPasswordNotifications();
		assertNoUserPasswordNotifications();
	}

	@Test
    public void test906ModifyUserElainePasswordLater() throws Exception {
		final String TEST_NAME = "test906ModifyUserElainePasswordLater";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest();

        clock.overrideDuration("PT15M");

        lastPasswordChangeStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modifyUserChangePassword(USER_ELAINE_OID, USER_PASSWORD_VALID_3, task, result);

		// THEN
		assertSuccess(result);

        lastPasswordChangeEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userAfter = getUser(USER_ELAINE_OID);
		display("User after", userAfter);

		assertUserPassword(userAfter, USER_PASSWORD_VALID_3);

		displayAccountPasswordNotifications();
		assertAccountPasswordNotifications(2);
		assertHasAccountPasswordNotification(null, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);
     	assertHasAccountPasswordNotification(RESOURCE_DUMMY_RED_NAME, USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);
     	// BLUE resource already has a password
		assertSingleUserPasswordNotification(USER_ELAINE_USERNAME, USER_PASSWORD_VALID_3);

	}

	/*
	 *  Policy prevents creating user with no password.
	 */
	@Test
	public void test910AddUserWithNoPasswordFail() throws Exception {
		final String TEST_NAME = "test910AddUserWithNoPasswordFail";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		prepareTest();

		setPasswordMinOccurs(1, task, result);

		try {
			// WHEN+THEN
			TestUtil.displayWhen(TEST_NAME);
			try {
				UserType user = new UserType(prismContext).name("passwordless");
				addObject(user.asPrismObject(), task, result);
				fail("unexpected success");
			} catch (PolicyViolationException e) {
				assertMessage(e, "Provided password does not satisfy the policies: The value must be present.");
			}
		} finally {
			setPasswordMinOccurs(null, task, result);
		}
		assertNoUserPasswordNotifications();
	}


	protected void prepareTest() throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        prepareNotifications();
	}

	protected PrismObject<ShadowType> getBlueShadow(PrismObject<UserType> userAfter) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String accountBlueOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_BLUE_OID);
		Task task = taskManager.createTaskInstance(AbstractPasswordTest.class.getName() + ".getBlueShadow");
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
		options.add(SelectorOptions.create(SchemaConstants.PATH_PASSWORD_VALUE, GetOperationOptions.createRetrieve()));
		PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountBlueOid, options , task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Account) result not success", result);
		display("Blue shadow", shadow);
		return shadow;
	}

	protected boolean isPasswordEncryption() {
		return getPasswordStorageType() == CredentialsStorageTypeType.ENCRYPTION;
	}
	
	protected void assertCachedResourcePassword(PrismObject<ShadowType> shadow, String expectedPassword) throws Exception {
		CredentialsType credentials = shadow.asObjectable().getCredentials();
		if (expectedPassword == null && credentials == null) {
			return;
		}
		assertNotNull("Missing credentendials in repo shadow "+shadow, credentials);
		PasswordType passwordType = credentials.getPassword();
		if (expectedPassword == null && passwordType == null) {
			return;
		}
		assertNotNull("Missing password credential in repo shadow "+shadow, passwordType);
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertNotNull("No password value in repo shadow "+shadow, protectedStringType);
		assertProtectedString("Wrong password value in repo shadow "+shadow, expectedPassword, protectedStringType, CredentialsStorageTypeType.HASHING);
	}

}
