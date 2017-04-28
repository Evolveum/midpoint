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
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUuid extends AbstractInitializedModelIntegrationTest {
	
	private static final File TEST_DIR = new File("src/test/resources/sync");

	protected static final File RESOURCE_DUMMY_UUID_FILE = new File(TEST_DIR, "resource-dummy-uuid.xml");
	protected static final String RESOURCE_DUMMY_UUID_OID = "9792acb2-0b75-11e5-b66e-001e8c717e5b";
	protected static final String RESOURCE_DUMMY_UUID_NAME = "uuid";
	protected static final String RESOURCE_DUMMY_UUID_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File TASK_RECONCILE_DUMMY_UUID_FILE = new File(TEST_DIR, "task-reconcile-dummy-uuid.xml");
	protected static final String TASK_RECONCILE_DUMMY_UUID_OID = "98ae26fc-0b76-11e5-b943-001e8c717e5b";
	
	private static final String USER_AUGUSTUS_NAME = "augustus";
	
	private static final String ACCOUNT_AUGUSTUS_NAME = "augustus";
	private static final String ACCOUNT_AUGUSTUS_FULLNAME = "Augustus DeWaat";
	
	private static final String ACCOUNT_AUGUSTINA_NAME = "augustina";
	private static final String ACCOUNT_AUGUSTINA_FULLNAME = "Augustina LeWhat";
	
	
	protected DummyResource dummyResourceUuid;
	protected DummyResourceContoller dummyResourceCtlUuid;
	protected ResourceType resourceDummyUuidType;
	protected PrismObject<ResourceType> resourceDummyUuid;
	
	private String augustusShadowOid;
	

	@Autowired(required=true)
	private ReconciliationTaskHandler reconciliationTaskHandler;
	
	private DebugReconciliationTaskResultListener reconciliationTaskResultListener;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		reconciliationTaskResultListener = new DebugReconciliationTaskResultListener();
		reconciliationTaskHandler.setReconciliationTaskResultListener(reconciliationTaskResultListener);
		
		dummyResourceCtlUuid = DummyResourceContoller.create(RESOURCE_DUMMY_UUID_NAME, resourceDummyUuid);
		dummyResourceCtlUuid.extendSchemaPirate();
		dummyResourceUuid = dummyResourceCtlUuid.getDummyResource();
		resourceDummyUuid = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UUID_FILE, RESOURCE_DUMMY_UUID_OID, initTask, initResult); 
		resourceDummyUuidType = resourceDummyUuid.asObjectable();
		dummyResourceCtlUuid.setResource(resourceDummyUuid);	
				
		InternalMonitor.reset();
		InternalMonitor.setTraceShadowFetchOperation(true);
		
//		DebugUtil.setDetailedDebugDump(true);
	}
	

	@Test
    public void test200ReconcileDummyUuid() throws Exception {
		final String TEST_NAME = "test200ReconcileDummyUuid";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestUuid.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // TODO
        
        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_RECONCILE_DUMMY_UUID_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_RECONCILE_DUMMY_UUID_OID, false);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 0, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        
        assertEquals("Unexpected number of users", 5, users.size());
        
        display("Dummy resource", getDummyResource().debugDump());
        
        assertReconAuditModifications(0, TASK_RECONCILE_DUMMY_UUID_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
	}

	@Test
    public void test210ReconcileDummyUuidAddAugustus() throws Exception {
		final String TEST_NAME = "test210ReconcileDummyUuidAddAugustus";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestUuid.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount account = new DummyAccount(ACCOUNT_AUGUSTUS_NAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTUS_FULLNAME);
		dummyResourceUuid.addAccount(account);
        
		getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
        Task taskBefore = taskManager.getTask(TASK_RECONCILE_DUMMY_UUID_OID, result);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskNextRunAssertSuccess(taskBefore, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        
        assertImportedUserByUsername(ACCOUNT_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTUS_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		ACCOUNT_AUGUSTUS_FULLNAME);
        
        assertEquals("Unexpected number of users", 6, users.size());
        
        display("Dummy resource", getDummyResource().debugDump());
                
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
        
        PrismObject<UserType> user = findUserByUsername(USER_AUGUSTUS_NAME);
        display("Augustus after recon", user);
        augustusShadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, augustusShadowOid, null, result);
        
	}

	/**
	 * Augustus is deleted and re-added with the same username. New shadow needs to be created.
	 */
	@Test
    public void test220ReconcileDummyUuidDeleteAddAugustus() throws Exception {
		final String TEST_NAME = "test220ReconcileDummyUuidDeleteAddAugustus";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestUuid.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount oldAccount = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
        dummyResourceUuid.deleteAccountByName(ACCOUNT_AUGUSTUS_NAME);
        assertNoDummyAccount(ACCOUNT_AUGUSTUS_NAME, ACCOUNT_AUGUSTUS_NAME);
        
        DummyAccount account = new DummyAccount(ACCOUNT_AUGUSTUS_NAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTUS_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, USER_AUGUSTUS_NAME);
		dummyResourceUuid.addAccount(account);
		account = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
		
		assertFalse("Account IDs not changed", oldAccount.getId().equals(account.getId()));
		
		display("Old shadow OID", augustusShadowOid);
		display("Account ID "+ oldAccount.getId() + " -> " + account.getId());
		
		Task taskBefore = taskManager.getTask(TASK_RECONCILE_DUMMY_UUID_OID, result);
        
		getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskNextRunAssertSuccess(taskBefore, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        
        assertImportedUserByUsername(ACCOUNT_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTUS_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		ACCOUNT_AUGUSTUS_FULLNAME);
        
        assertEquals("Unexpected number of users", 6, users.size());
        
        display("Dummy resource", getDummyResource().debugDump());
                
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
        
        PrismObject<UserType> user = findUserByUsername(USER_AUGUSTUS_NAME);
        display("Augustus after recon", user);
        String newAugustusShadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, newAugustusShadowOid, null, result);
        assertFalse("Shadow OID is not changed", augustusShadowOid.equals(newAugustusShadowOid));
        augustusShadowOid = newAugustusShadowOid;
	}
	
	/**
	 * Augustus is deleted and Augustina re-added. They correlate to the same user.
	 * New shadow needs to be created.
	 */
	@Test
    public void test230ReconcileDummyUuidDeleteAugustusAddAugustina() throws Exception {
		final String TEST_NAME = "test230ReconcileDummyUuidDeleteAugustusAddAugustina";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestUuid.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount oldAccount = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTUS_NAME);
        dummyResourceUuid.deleteAccountByName(ACCOUNT_AUGUSTUS_NAME);
        assertNoDummyAccount(ACCOUNT_AUGUSTUS_NAME, ACCOUNT_AUGUSTUS_NAME);
        
        DummyAccount account = new DummyAccount(ACCOUNT_AUGUSTINA_NAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_AUGUSTINA_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, USER_AUGUSTUS_NAME);
		dummyResourceUuid.addAccount(account);
		account = dummyResourceUuid.getAccountByUsername(ACCOUNT_AUGUSTINA_NAME);
		
		assertFalse("Account IDs not changed", oldAccount.getId().equals(account.getId()));
		
		display("Old shadow OID", augustusShadowOid);
		display("Account ID "+ oldAccount.getId() + " -> " + account.getId());
		
		Task taskBefore = taskManager.getTask(TASK_RECONCILE_DUMMY_UUID_OID, result);
        
		getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberShadowFetchOperationCount();
        reconciliationTaskResultListener.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        restartTask(TASK_RECONCILE_DUMMY_UUID_OID);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskNextRunAssertSuccess(taskBefore, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);        
        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_UUID_OID, 0, 1, 0, 0);
        
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        
        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_UUID_OID);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UUID_NAME, ACCOUNT_AUGUSTINA_NAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
        		ACCOUNT_AUGUSTINA_FULLNAME);
        
        assertEquals("Unexpected number of users", 6, users.size());
        
        display("Dummy resource", getDummyResource().debugDump());
                
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_UUID_OID);
        
        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_UUID_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
        
        PrismObject<UserType> user = findUserByUsername(USER_AUGUSTUS_NAME);
        display("Augustus after recon", user);
        String newAugustusShadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, newAugustusShadowOid, null, result);
        assertFalse("Shadow OID is not changed", augustusShadowOid.equals(newAugustusShadowOid));
	}

	private void assertReconAuditModifications(int expectedModifications, String taskOid) {
		// Check audit
        display("Audit", dummyAuditService);
        
        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();

        Iterator<AuditEventRecord> iterator = auditRecords.iterator();
        while (iterator.hasNext()) {
        	AuditEventRecord record = iterator.next();
	    	if (record.getTaskOID() != null && !record.getTaskOID().equals(taskOid)) {
	    		// Record from some other task, skip it
	    		iterator.remove();
	    	}
        }

    	int i=0;
    	while (i < (auditRecords.size() - 1)) {
    		AuditEventRecord reconStartRecord = auditRecords.get(i);
    		if (reconStartRecord.getEventType() == AuditEventType.EXECUTE_CHANGES_RAW) {
    			i++;
    			continue;
    		}
            assertNotNull("No reconStartRecord audit record", reconStartRecord);
        	assertEquals("Wrong stage in reconStartRecord audit record: "+reconStartRecord, AuditEventStage.REQUEST, reconStartRecord.getEventStage());
        	assertEquals("Wrong type in reconStartRecord audit record: "+reconStartRecord, AuditEventType.RECONCILIATION, reconStartRecord.getEventType());
        	assertTrue("Unexpected delta in reconStartRecord audit record "+reconStartRecord, reconStartRecord.getDeltas() == null || reconStartRecord.getDeltas().isEmpty());
        	i++;
        	break;
    	}

    	int modifications = 0;
    	for (; i < (auditRecords.size() - 1); i+=2) {
        	AuditEventRecord requestRecord = auditRecords.get(i);
        	assertNotNull("No request audit record ("+i+")", requestRecord);

            if (requestRecord.getEventStage() == AuditEventStage.EXECUTION && requestRecord.getEventType() == AuditEventType.RECONCILIATION) {
                // end of audit records;
                break;
            }
        	        	
        	assertEquals("Got this instead of request audit record ("+i+"): "+requestRecord, AuditEventStage.REQUEST, requestRecord.getEventStage());
        	// Request audit may or may not have a delta. Usual records will not have a delta. But e.g. disableAccount reactions will have.

        	AuditEventRecord executionRecord = auditRecords.get(i+1);
        	assertNotNull("No execution audit record (" + i + ")", executionRecord);
        	assertEquals("Got this instead of execution audit record (" + i + "): " + executionRecord, AuditEventStage.EXECUTION, executionRecord.getEventStage());
        	
        	assertTrue("Empty deltas in execution audit record "+executionRecord, executionRecord.getDeltas() != null && ! executionRecord.getDeltas().isEmpty());
        	modifications++;

            while (i+2 < auditRecords.size()) {
                AuditEventRecord nextRecord = auditRecords.get(i+2);
                if (nextRecord.getEventStage() == AuditEventStage.EXECUTION && nextRecord.getEventType() == requestRecord.getEventType()) {
                    // this is an additional EXECUTION record due to changes in clockwork
                    i++;
                } else {
                    break;
                }
            }
        }
        assertEquals("Unexpected number of audit modifications", expectedModifications, modifications);

        AuditEventRecord reconStopRecord = auditRecords.get(i);
        assertNotNull("No reconStopRecord audit record", reconStopRecord);
    	assertEquals("Wrong stage in reconStopRecord audit record: "+reconStopRecord, AuditEventStage.EXECUTION, reconStopRecord.getEventStage());
    	assertEquals("Wrong type in reconStopRecord audit record: "+reconStopRecord, AuditEventType.RECONCILIATION, reconStopRecord.getEventType());
    	assertTrue("Unexpected delta in reconStopRecord audit record "+reconStopRecord, reconStopRecord.getDeltas() == null || reconStopRecord.getDeltas().isEmpty());
	}

	private void assertNoImporterUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(username);
        assertNull("User "+username+" sneaked in", user);
	}

	private void assertImportedUserByOid(String userOid, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = getUser(userOid);
		assertNotNull("No user "+userOid, user);
		assertImportedUser(user, resourceOids);
	}
		
	private void assertImportedUserByUsername(String username, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(username);
		assertNotNull("No user "+username, user);
		assertImportedUser(user, resourceOids);
	}
		
	private void assertImportedUser(PrismObject<UserType> user, String... resourceOids) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        assertLinks(user, resourceOids.length);
        for (String resourceOid: resourceOids) {
        	assertAccount(user, resourceOid);
        }
        assertAdministrativeStatusEnabled(user);
	}

}
