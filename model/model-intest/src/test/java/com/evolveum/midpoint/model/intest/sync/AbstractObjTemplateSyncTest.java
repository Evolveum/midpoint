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
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractObjTemplateSyncTest extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/sync");

	protected static final File RESOURCE_DUMMY_BYZANTINE_FILE = new File(TEST_DIR, "resource-dummy-byzantine.xml");
	protected static final String RESOURCE_DUMMY_BYZANTINE_OID = "10000000-0000-0000-0000-00000000f904";
	protected static final String RESOURCE_DUMMY_BYZANTINE_NAME = "byzantine";

	protected static final File TASK_LIVE_SYNC_DUMMY_BYZANTINE_FILE = new File(TEST_DIR, "task-dummy-byzantine-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_BYZANTINE_OID = "10000000-0000-0000-5555-55550000f904";

	protected static final File TASK_RECON_DUMMY_BYZANTINE_FILE = new File(TEST_DIR, "task-dummy-byzantine-recon.xml");
	protected static final String TASK_RECON_DUMMY_BYZANTINE_OID = "10000000-0000-0000-5656-56560000f904";

	protected static DummyResource dummyResourceByzantine;
	protected static DummyResourceContoller dummyResourceCtlByzantine;
	protected ResourceType resourceDummyByzantineType;
	protected PrismObject<ResourceType> resourceDummyByzantine;

	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
	private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);

	protected long timeBeforeSync;

	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration-byzantine.xml");

	protected static final String ORG_F0001_OID = "00000000-8888-6666-0000-100000000001";

	public static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template-byzantine.xml");


	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// templates
		repoAddObjectFromFile(USER_TEMPLATE_FILE, initResult);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		dummyResourceCtlByzantine = DummyResourceContoller.create(RESOURCE_DUMMY_BYZANTINE_NAME, resourceDummyByzantine);
		dummyResourceCtlByzantine.extendSchemaPirate();
		dummyResourceByzantine = dummyResourceCtlByzantine.getDummyResource();
		resourceDummyByzantine = importAndGetObjectFromFile(ResourceType.class, getResourceDummyByzantineFile(), RESOURCE_DUMMY_BYZANTINE_OID, initTask, initResult);
		resourceDummyByzantineType = resourceDummyByzantine.asObjectable();
		dummyResourceCtlByzantine.setResource(resourceDummyByzantine);
	}

	protected File getResourceDummyByzantineFile() {
		return RESOURCE_DUMMY_BYZANTINE_FILE;
	}

	protected abstract void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException;

	protected abstract String getSyncTaskOid(PrismObject<ResourceType> resource);

	protected int getWaitTimeout() {
		return DEFAULT_TASK_WAIT_TIMEOUT;
	}

	@Test
    public void test100ImportLiveSyncTaskDummyByzantine() throws Exception {
		final String TEST_NAME = "test100ImportLiveSyncTaskDummyByzantine";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractObjTemplateSyncTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importSyncTask(resourceDummyByzantine);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        waitForSyncTaskStart(resourceDummyByzantine);
	}

	// MID-2149
	@Test
    public void test110AddDummyByzantineAccountMancomb() throws Exception {
		final String TEST_NAME = "test110AddDummyByzantineAccountMancomb";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(AbstractObjTemplateSyncTest.class.getName() + "." + TEST_NAME);
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

		dummyResourceByzantine.addAccount(account);

        waitForSyncTaskNextRun(resourceDummyByzantine);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyByzantine);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_BYZANTINE_OID,
        		accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);
        assertNotNull("User mancomb was not created", userMancomb);
        assertLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);

        assertLinked(userMancomb, accountMancomb);

		assertEquals("Wrong e-mail address for mancomb", "mancomb.Mr@test.com", userMancomb.asObjectable().getEmailAddress());
		assertAssignedOrg(userMancomb, ORG_F0001_OID);
		assertHasOrg(userMancomb, ORG_F0001_OID);

        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
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
