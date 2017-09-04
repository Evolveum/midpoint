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
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractInboundSyncTest extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/sync");

	protected static final File TASK_LIVE_SYNC_DUMMY_EMERALD_FILE = new File(TEST_DIR, "task-dummy-emerald-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_EMERALD_OID = "10000000-0000-0000-5555-55550000e404";

	protected static final File TASK_RECON_DUMMY_EMERALD_FILE = new File(TEST_DIR, "task-dummy-emerald-recon.xml");
	protected static final String TASK_RECON_DUMMY_EMERALD_OID = "10000000-0000-0000-5656-56560000e404";

	protected static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	protected static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
	protected static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

	protected static final String ACCOUNT_POSIXUSER_DUMMY_USERNAME = "posixuser";

	protected static String userWallyOid;

	protected boolean allwaysCheckTimestamp = false;
	protected long timeBeforeSync;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

	}

	protected File getResourceDummyEmeraldFile() {
		return RESOURCE_DUMMY_EMERALD_FILE;
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
    public void test100ImportLiveSyncTaskDummyEmerald() throws Exception {
		final String TEST_NAME = "test100ImportLiveSyncTaskDummyEmerald";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importSyncTask(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        waitForSyncTaskStart(resourceDummyEmerald);
	}

	@Test
    public void test110AddDummyEmeraldAccountMancomb() throws Exception {
		final String TEST_NAME = "test110AddDummyEmeraldAccountMancomb";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
		long loot = ACCOUNT_MANCOMB_VALID_FROM_DATE.getTime();
		account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, loot);
		String gossip = XmlTypeConverter.createXMLGregorianCalendar(ACCOUNT_MANCOMB_VALID_TO_DATE).toXMLFormat();
		account.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, gossip);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        display("Adding dummy account", account.debugDump());

		dummyResourceEmerald.addAccount(account);

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userMancomb, accountMancomb);

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test120ModifyDummyEmeraldAccountMancombSeepbad() throws Exception {
		final String TEST_NAME = "test120ModifyDummyEmeraldAccountMancombSeepbad";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepbad");

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepbad");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Mancomb Seepbad"));

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test122ModifyDummyEmeraldAccountMancombSeepNULL() throws Exception {
		final String TEST_NAME = "test122ModifyDummyEmeraldAccountMancombSeepNULL";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb SeepNULL");

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb SeepNULL");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_FULL_NAME);

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test124ModifyDummyEmeraldAccountMancombSeepevil() throws Exception {
		final String TEST_NAME = "test124ModifyDummyEmeraldAccountMancombSeepevil";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepevil");

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepevil");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Mancomb Seepevil"));

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test126ModifyDummyEmeraldAccountMancombTitlePirate() throws Exception {
		final String TEST_NAME = "test126ModifyDummyEmeraldAccountMancombTitlePirate";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Mancomb Seepevil"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TITLE, PrismTestUtil.createPolyString("Pirate"));

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test127ModifyDummyEmeraldAccountMancombTitlePirateNull() throws Exception {
		final String TEST_NAME = "test127ModifyDummyEmeraldAccountMancombTitlePirateNull";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Mancomb Seepevil"));
        PrismAsserts.assertNoItem(userAfter, UserType.F_TITLE);

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}

	@Test
    public void test129ModifyDummyEmeraldAccountMancombSeepgood() throws Exception {
		final String TEST_NAME = "test129ModifyDummyEmeraldAccountMancombSeepgood";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberTimeBeforeSync();
        prepareNotifications();

        // Preconditions
        assertUsers(6);

        DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");

        display("Modified dummy account", account.debugDump());

        waitForSyncTaskNextRun(resourceDummyEmerald);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyEmerald);
        display("Account mancomb", accountAfter);
        assertNotNull("No mancomb account shadow", accountAfter);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_EMERALD_OID,
        		accountAfter.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);
        assertDummyAccountAttribute(RESOURCE_DUMMY_EMERALD_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userAfter);
        assertNotNull("User mancomb was not created", userAfter);
        assertLinks(userAfter, 1);
        assertAdministrativeStatusEnabled(userAfter);
        assertValidFrom(userAfter, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userAfter, ACCOUNT_MANCOMB_VALID_TO_DATE);

        assertLinked(userAfter, accountAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Mancomb Seepgood"));

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
	}


	@Test
	public void test180NoChange() throws Exception {
		// default = no op
		// (method is here to be executed before test199)
	}

	@Test
    public abstract void test199DeleteDummyEmeraldAccountMancomb() throws Exception;

	@Test
	public void test300AddDummyEmeraldAccountPosixUser() throws Exception {
		final String TEST_NAME = "test300AddDummyEmeraldAccountPosixUser";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		rememberTimeBeforeSync();
		prepareNotifications();

		// Preconditions
		assertUsers(6);

		DummyAccount account = new DummyAccount(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Posix User");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
		account.addAuxiliaryObjectClassName(DummyResourceContoller.DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, Collections.<Object>singleton(1001));
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_GID_NUMBER, Collections.<Object>singleton(10001));

		/// WHEN
		TestUtil.displayWhen(TEST_NAME);

		display("Adding dummy account", account.debugDump());

		dummyResourceEmerald.addAccount(account);

		waitForSyncTaskNextRun(resourceDummyEmerald);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		PrismObject<ShadowType> accountPosixUser = findAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME, resourceDummyEmerald);
		display("Account posixuser", accountPosixUser);
		assertNotNull("No posixuser account shadow", accountPosixUser);
		assertEquals("Wrong resourceRef in posixuser account", RESOURCE_DUMMY_EMERALD_OID,
				accountPosixUser.asObjectable().getResourceRef().getOid());
		assertShadowOperationalData(accountPosixUser, SynchronizationSituationType.LINKED);

		PrismObject<UserType> userPosixUser = findUserByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
		display("User posixuser", userPosixUser);
		assertNotNull("User posixuser was not created", userPosixUser);
		assertLinks(userPosixUser, 1);
		assertAdministrativeStatusEnabled(userPosixUser);
		assertLinked(userPosixUser, accountPosixUser);

		assertUsers(7);

		// notifications
		notificationManager.setDisabled(true);

		// TODO create and test inbounds for uid and gid numbers; also other attributes
		// (Actually I'm not sure it will work, as even now the auxiliary object class is
		// removed right during the livesync. This has to be solved somehow...)
	}

	@Test
	public void test310ModifyDummyEmeraldAccountPosixUserUidNumber() throws Exception {
		final String TEST_NAME = "test310ModifyDummyEmeraldAccountPosixUserUidNumber";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = createTask(AbstractInboundSyncTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		rememberTimeBeforeSync();
		prepareNotifications();

		// Preconditions
		assertUsers(7);

		DummyAccount account = dummyResourceEmerald.getAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);

		/// WHEN
		TestUtil.displayWhen(TEST_NAME);

		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, 1002);

		display("Modified dummy account", account.debugDump());

		waitForSyncTaskNextRun(resourceDummyEmerald);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		PrismObject<ShadowType> accountAfter = findAccountByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME, resourceDummyEmerald);
		display("Account posixuser", accountAfter);
		assertNotNull("No posixuser account shadow", accountAfter);
		assertEquals("Wrong resourceRef in posixuser account", RESOURCE_DUMMY_EMERALD_OID,
				accountAfter.asObjectable().getResourceRef().getOid());
		assertShadowOperationalData(accountAfter, SynchronizationSituationType.LINKED);

		PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_POSIXUSER_DUMMY_USERNAME);
		display("User posixuser", userAfter);
		assertNotNull("User posixuser was not created", userAfter);
		assertLinks(userAfter, 1);
		assertAdministrativeStatusEnabled(userAfter);

		assertLinked(userAfter, accountAfter);

		assertUsers(7);

		// notifications
		notificationManager.setDisabled(true);

		// TODO create and test inbounds for uid and gid numbers; also other attributes
	}


	protected void waitForSyncTaskStart(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskStart(getSyncTaskOid(resource), false, getWaitTimeout());
	}

	protected void waitForSyncTaskNextRun(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskNextRunAssertSuccess(getSyncTaskOid(resource), false, getWaitTimeout());
	}


	protected void rememberTimeBeforeSync() {
		timeBeforeSync = System.currentTimeMillis();
	}

	protected void assertShadowOperationalData(PrismObject<ShadowType> shadow, SynchronizationSituationType expectedSituation) {
		ShadowType shadowType = shadow.asObjectable();
		SynchronizationSituationType actualSituation = shadowType.getSynchronizationSituation();
		assertEquals("Wrong situation in shadow "+shadow, expectedSituation, actualSituation);
		XMLGregorianCalendar actualTimestampCal = shadowType.getSynchronizationTimestamp();
		assert actualTimestampCal != null : "No synchronization timestamp in shadow "+shadow;
		long actualTimestamp = XmlTypeConverter.toMillis(actualTimestampCal);
		assert actualTimestamp >= timeBeforeSync : "Synchronization timestamp was not updated in shadow "+shadow;
		// TODO: assert sync description
	}

}
