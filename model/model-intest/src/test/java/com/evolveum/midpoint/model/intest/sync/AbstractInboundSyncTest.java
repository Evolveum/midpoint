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
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
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
	
	protected static final File RESOURCE_DUMMY_EMERALD_FILE = new File(TEST_DIR, "resource-dummy-emerald.xml");
	protected static final File RESOURCE_DUMMY_EMERALD_DEPRECATED_FILE = new File(TEST_DIR, "resource-dummy-emerald-deprecated.xml");
	protected static final String RESOURCE_DUMMY_EMERALD_OID = "10000000-0000-0000-0000-00000000e404";
	protected static final String RESOURCE_DUMMY_EMERALD_NAME = "emerald";
	protected static final String RESOURCE_DUMMY_EMERALD_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File TASK_LIVE_SYNC_DUMMY_EMERALD_FILE = new File(TEST_DIR, "task-dummy-emerald-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_EMERALD_OID = "10000000-0000-0000-5555-55550000e404";
		
	protected static final File TASK_RECON_DUMMY_EMERALD_FILE = new File(TEST_DIR, "task-dummy-emerald-recon.xml");
	protected static final String TASK_RECON_DUMMY_EMERALD_OID = "10000000-0000-0000-5656-56560000e404";
	
	protected static DummyResource dummyResourceEmerald;
	protected static DummyResourceContoller dummyResourceCtlEmerald;
	protected ResourceType resourceDummyEmeraldType;
	protected PrismObject<ResourceType> resourceDummyEmerald;
		
	protected static final String ACCOUNT_WALLY_DUMMY_USERNAME = "wally";
	protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
	private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
	private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);
	
	protected static String userWallyOid;
	
	protected boolean allwaysCheckTimestamp = false;
	protected long timeBeforeSync;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		dummyResourceCtlEmerald = DummyResourceContoller.create(RESOURCE_DUMMY_EMERALD_NAME, resourceDummyEmerald);
		dummyResourceCtlEmerald.extendSchemaPirate();
		dummyResourceEmerald = dummyResourceCtlEmerald.getDummyResource();
		resourceDummyEmerald = importAndGetObjectFromFile(ResourceType.class, getResourceDummyEmeraldFile(), RESOURCE_DUMMY_EMERALD_OID, initTask, initResult); 
		resourceDummyEmeraldType = resourceDummyEmerald.asObjectable();
		dummyResourceCtlEmerald.setResource(resourceDummyEmerald);
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
        TestUtil.displayTestTile(this, TEST_NAME);

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
        TestUtil.displayTestTile(this, TEST_NAME);

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
    public abstract void test199DeleteDummyEmeraldAccountMancomb() throws Exception;

	protected void waitForSyncTaskStart(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskStart(getSyncTaskOid(resource), false, getWaitTimeout());
	}
	
	protected void waitForSyncTaskNextRun(PrismObject<ResourceType> resource) throws Exception {
		waitForTaskNextRun(getSyncTaskOid(resource), false, getWaitTimeout());
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
