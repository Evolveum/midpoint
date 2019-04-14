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
package com.evolveum.midpoint.model.intest.mapping;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;

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
public class TestMappingInbound extends AbstractMappingTest {

    protected static final File RESOURCE_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "resource-dummy-tea-green.xml");
    protected static final String RESOURCE_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-0000-00000000c404";
    protected static final String RESOURCE_DUMMY_TEA_GREEN_NAME = "tea-green";

    protected static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";

    protected static final File TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE = new File(TEST_DIR, "task-dumy-tea-green-livesync.xml");
    protected static final String TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID = "10000000-0000-0000-5555-55550000c404";
    
	private static final String LOCKER_BIG_SECRET = "BIG secret";
	
	private ProtectedStringType mancombLocker;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		initDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME, RESOURCE_DUMMY_TEA_GREEN_FILE, RESOURCE_DUMMY_TEA_GREEN_OID,
				controller -> {
					controller.extendSchemaPirate();
					controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
							DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, String.class, false, false)
						.setSensitive(true);
					controller.setSyncStyle(DummySyncStyle.SMART);
				},
				initTask, initResult);		
	}

    @Test
    public void test010SanitySchema() throws Exception {
        final String TEST_NAME = "test010SanitySchema";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);

        /// WHEN
        displayWhen(TEST_NAME);
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_TEA_GREEN_OID, task);

        // THEN
        displayThen(TEST_NAME);
        TestUtil.assertSuccess(testResult);

        ResourceType resourceType = getDummyResourceType(RESOURCE_DUMMY_TEA_GREEN_NAME);
        ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceType, prismContext);
		display("Parsed resource schema (tea-green)", returnedSchema);
		ObjectClassComplexTypeDefinition accountDef = getDummyResourceController(RESOURCE_DUMMY_TEA_GREEN_NAME)
				.assertDummyResourceSchemaSanityExtended(returnedSchema, resourceType, false,
						DummyResourceContoller.PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS + 1); // MID-5197
		
		ResourceAttributeDefinition<ProtectedStringType> lockerDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME);
		assertNotNull("No locker attribute definition", lockerDef);
		assertEquals("Wrong locker attribute definition type", ProtectedStringType.COMPLEX_TYPE, lockerDef.getTypeName());
    }
    
    @Test
    public void test100ImportLiveSyncTaskDummyTeaGreen() throws Exception {
        final String TEST_NAME = "test100ImportLiveSyncTaskDummyTeaGreen";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
        importSyncTask();

        // THEN
        displayThen(TEST_NAME);

        waitForSyncTaskStart();
    }

    @Test
    public void test110AddDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test110AddDummyTeaGreenAccountMancomb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // Preconditions
        //assertUsers(5);

        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCKER_NAME, LOCKER_BIG_SECRET); // MID-5197

        /// WHEN
        displayWhen(TEST_NAME);

        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).addAccount(account);

        waitForSyncTaskNextRun();

        // THEN
        displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        mancombLocker = assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
        	.links()
        		.single()
        			.assertOid(accountMancomb.getOid())
        			.end()
        		.end()
    		.assertAdministrativeStatus(ActivationStatusType.ENABLED)
    		.extension()
    			.property(PIRACY_LOCKER)
    				.singleValue()
    					.protectedString()
    						.assertIsEncrypted()
    						.assertCompareCleartext(LOCKER_BIG_SECRET)
    						.getProtectedString();

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);
    }

    /**
     * MID-5197
     */
    @Test
    public void test150UserReconcile() throws Exception {
        final String TEST_NAME = "test150UserReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Preconditions
        //assertUsers(5);

        /// WHEN
        displayWhen(TEST_NAME);

        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("User mancomb has disappeared", userMancomb);

        reconcileUser(userMancomb.getOid(), task, result);

        // THEN
        displayThen(TEST_NAME);

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_TEA_GREEN_NAME));
        display("Account mancomb", accountMancomb);
        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_TEA_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertShadowOperationalData(accountMancomb, SynchronizationSituationType.LINKED, null);

        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
	    	.links()
	    		.single()
	    			.assertOid(accountMancomb.getOid())
	    			.end()
	    		.end()
			.assertAdministrativeStatus(ActivationStatusType.ENABLED)
			.extension()
				.property(PIRACY_LOCKER)
					.singleValue()
						.protectedString()
							.assertIsEncrypted()
							.assertCompareCleartext(LOCKER_BIG_SECRET)
							// Make sure that this is exactly the same content of protected string
							// including all the randomized things (IV). If it is the same,
							// there is a good chance we haven't had any phantom changes
							// MID-5197
							.assertEquals(mancombLocker);

//        assertUsers(6);

        // notifications
        notificationManager.setDisabled(true);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
    }

    @Test
    public void test300DeleteDummyTeaGreenAccountMancomb() throws Exception {
        final String TEST_NAME = "test300DeleteDummyTeaGreenAccountMancomb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        /// WHEN
        displayWhen(TEST_NAME);
        getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).deleteAccountByName(ACCOUNT_MANCOMB_DUMMY_USERNAME);

        display("Dummy (tea green) resource", getDummyResource(RESOURCE_DUMMY_TEA_GREEN_NAME).debugDump());

        // Make sure we have steady state
        waitForSyncTaskNextRun();

        // THEN
        displayThen(TEST_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_TEA_GREEN_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        
        assertUserAfterByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME)
        	.assertFullName("Mancomb Seepgood")
        	.links()
        		.single()
        			.resolveTarget()
        				.assertTombstone()
        				.assertSynchronizationSituation(SynchronizationSituationType.DELETED);
        
//        assertUsers(7 + getNumberOfExtraDummyUsers());

        // notifications
        notificationManager.setDisabled(true);
    }


    protected void importSyncTask() throws FileNotFoundException {
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_FILE);
    }

    protected void waitForSyncTaskStart() throws Exception {
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

    protected void waitForSyncTaskNextRun() throws Exception {
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_TEA_GREEN_OID, false, 10000);
    }

}
