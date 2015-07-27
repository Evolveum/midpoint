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
    public void test150UserReconcile() throws Exception {
        final String TEST_NAME = "test150UserReconcile";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestMappingInbound.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, resourceDummyTeaGreen);
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        display("User mancomb", userMancomb);

        assertLinks(userMancomb, 1);
        assertAdministrativeStatusEnabled(userMancomb);

        assertLinked(userMancomb, accountMancomb);

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
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
	
}
