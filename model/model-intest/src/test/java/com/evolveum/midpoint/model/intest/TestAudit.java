/*
 * Copyright (c) 2016 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test of Model Audit Service.
 * 
 * Two users, interlaced events: jack and herman.
 * 
 * Jack already exists, but there is no create audit record for him. This simulates trimmed
 * audit log. There are several fresh modify operations.
 * 
 * Herman is properly created and modified. We have all the audit records.
 * 
 * The tests check if audit records are created and that they can be listed.
 * The other tests check that the state can be reconstructed by the time machine (object history).
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAudit extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/audit");
	
	public static final int INITIAL_NUMBER_OF_AUDIT_RECORDS = 24;
	
	private XMLGregorianCalendar initialTs;
	private XMLGregorianCalendar jackKidTs;
	private String jackKidEid;
	private XMLGregorianCalendar jackSailorTs;
	private String jackSailorEid;
	private XMLGregorianCalendar jackCaptainTs;
	private String jackCaptainEid;
	private XMLGregorianCalendar hermanInitialTs;
	private XMLGregorianCalendar hermanCreatedTs;
	private String hermanCreatedEid;
	private XMLGregorianCalendar hermanMaroonedTs;
	private String hermanMaroonedEid;
	private XMLGregorianCalendar hermanHermitTs;
	private String hermanHermitEid;
	private XMLGregorianCalendar hermanCivilisedHermitTs;
	private String hermanCivilisedHermitEid;
	
	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

	
    @Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);

        assertTrue(modelAuditService.supportsRetrieval());
        
        // WHEN
        List<AuditEventRecord> allRecords = modelAuditService.listRecords("from RAuditEventRecord as aer where 1=1 ", 
        		new HashMap<String,Object>());
        
        // THEN
        display("all records", allRecords);
        
        assertEquals("Wrong initial number of audit records", INITIAL_NUMBER_OF_AUDIT_RECORDS, allRecords.size());
	}

    @Test
    public void test010SanityJack() throws Exception {
		final String TEST_NAME = "test010SanityJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // WHEN
        List<AuditEventRecord> auditRecords = getObjectAuditRecords(USER_JACK_OID);
        
        // THEN
        display("Jack records", auditRecords);
        
        assertEquals("Wrong initial number of jack audit records", 0, auditRecords.size());
	}

    @Test
    public void test100ModifyUserJackKid() throws Exception {
		final String TEST_NAME = "test100ModifyUserJackKid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        initialTs = clock.currentTimeXMLGregorianCalendar();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_TITLE, task, result, createPolyString("Kid"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackKidTs = clock.currentTimeXMLGregorianCalendar();
        jackKidEid = assertObjectAuditRecords(USER_JACK_OID, 2);
        assertRecordsFromInitial(jackKidTs, 2);
    }
    
    /**
     * Let's interlace the history of two objects. So we make sure that the filtering
     * in the time machine works well.
     */
    @Test
    public void test105CreateUserHerman() throws Exception {
		final String TEST_NAME = "test105CreateUserHerman";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userHermanBefore = PrismTestUtil.parseObject(USER_HERMAN_FILE);
        userHermanBefore.asObjectable().setDescription("Unknown");
        userHermanBefore.asObjectable().setNickName(createPolyStringType("HT"));
        
        hermanInitialTs = clock.currentTimeXMLGregorianCalendar();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        addObject(userHermanBefore, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (created)", user);
        
        hermanCreatedTs = clock.currentTimeXMLGregorianCalendar();
        hermanCreatedEid = assertObjectAuditRecords(USER_HERMAN_OID, 2);
        assertRecordsFromInitial(hermanCreatedTs, 4);
    }

    @Test
    public void test110ModifyUserJackSailor() throws Exception {
		final String TEST_NAME = "test110ModifyUserJackSailor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_TITLE, 
        		createPolyString("Sailor"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		ActivationStatusType.DISABLED);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
                
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackSailorTs = clock.currentTimeXMLGregorianCalendar();
        jackSailorEid = assertObjectAuditRecords(USER_JACK_OID, 4);
        
        assertRecordsFromPrevious(hermanCreatedTs, jackSailorTs, 2);
        assertRecordsFromPrevious(jackKidTs, jackSailorTs, 4);
        assertRecordsFromInitial(jackSailorTs, 6);    
    }
    
    @Test
    public void test115ModifyUserHermanMarooned() throws Exception {
		final String TEST_NAME = "test115ModifyUserHermanMarooned";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE, 
        		createPolyString("Marooned"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Marooned on Monkey Island");
        objectDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, true));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
                
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (marooned)", user);
        
        hermanMaroonedTs = clock.currentTimeXMLGregorianCalendar();
        hermanMaroonedEid = assertObjectAuditRecords(USER_HERMAN_OID, 4);
        assertRecordsFromInitial(hermanMaroonedTs, 8);        
    }


	@Test
    public void test120ModifyUserJackCaptain() throws Exception {
		final String TEST_NAME = "test120ModifyUserJackCaptain";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_TITLE, 
        		createPolyString("Captain"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		ActivationStatusType.ENABLED);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
                
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);
                
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackCaptainTs = clock.currentTimeXMLGregorianCalendar();
        jackCaptainEid = assertObjectAuditRecords(USER_JACK_OID, 6);
        
        assertRecordsFromPrevious(hermanMaroonedTs, jackCaptainTs, 2);
        assertRecordsFromPrevious(jackSailorTs, jackCaptainTs, 4);
        assertRecordsFromInitial(jackCaptainTs, 10);    
    }
	
	@Test
    public void test125ModifyUserHermanHermit() throws Exception {
		final String TEST_NAME = "test125ModifyUserHermanHermit";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE, 
        		createPolyString("Hermit"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));
        objectDelta.addModificationReplaceProperty(UserType.F_NICK_NAME);
        objectDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, false));
        objectDelta.addModification(createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, 
        		null, null, null, true));
        objectDelta.addModification(createAssignmentModification(ROLE_RED_SAILOR_OID, RoleType.COMPLEX_TYPE, 
        		null, null, null, true));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
                
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (hermit)", user);
        
        hermanHermitTs = clock.currentTimeXMLGregorianCalendar();
        hermanHermitEid = assertObjectAuditRecords(USER_HERMAN_OID, 6);
        assertRecordsFromInitial(hermanHermitTs, 12);        
    }
	
	@Test
    public void test135ModifyUserHermanCivilisedHermit() throws Exception {
		final String TEST_NAME = "test135ModifyUserHermanCivilisedHermit";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE, 
        		createPolyString("Civilised Hermit"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Civilised Hermit on Monkey Island");
        objectDelta.addModification(createAssignmentModification(ROLE_RED_SAILOR_OID, RoleType.COMPLEX_TYPE, 
        		null, null, null, false));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
                
		modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (civilised hermit)", user);
        
        hermanCivilisedHermitTs = clock.currentTimeXMLGregorianCalendar();
        hermanCivilisedHermitEid = assertObjectAuditRecords(USER_HERMAN_OID, 8);
        assertRecordsFromInitial(hermanCivilisedHermitTs, 14);        
    }
    
    @Test
    public void test200ReconstructJackSailor() throws Exception {
		final String TEST_NAME = "test200ReconstructJackSailor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Captain"));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID, 
        		jackSailorEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);
        
        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, createPolyString("Sailor"));
        assertAdministrativeStatusDisabled(jackReconstructed);
        
        // TODO
    }
    
    @Test
    public void test210ReconstructJackKid() throws Exception {
		final String TEST_NAME = "test210ReconstructJackKid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Captain"));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID, 
        		jackKidEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);
        
        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, createPolyString("Kid"));
        
        // TODO
    }
    
    /**
     * This is supposed to get the objectToAdd directly from the create delta.
     */
    @Test
    public void test250ReconstructHermanCreated() throws Exception {
		final String TEST_NAME = "test250ReconstructHermanCreated";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_HERMAN_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Civilised Hermit"));
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));
        PrismAsserts.assertNoItem(userBefore, UserType.F_NICK_NAME);
        assertAssignments(userBefore, 1);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID, 
        		hermanCreatedEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);
        
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Unknown");
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_NICK_NAME, createPolyString("HT"));
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_TITLE);
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_HONORIFIC_PREFIX);
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));
        
        assertNoAssignments(hermanReconstructed);
    }
    
    /**
     * Rolling back the deltas from the current state.
     */
    @Test
    public void test252ReconstructHermanMarooned() throws Exception {
		final String TEST_NAME = "test252ReconstructHermanMarooned";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID, 
        		hermanMaroonedEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);
        
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_TITLE, createPolyString("Marooned"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Marooned on Monkey Island");
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_NICK_NAME, createPolyString("HT"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_HONORIFIC_PREFIX);
        
        assertAssignedAccount(hermanReconstructed, RESOURCE_DUMMY_OID);
        assertAssignments(hermanReconstructed, 1);
    }
    
    /**
     * Rolling back the deltas from the current state.
     */
    @Test
    public void test254ReconstructHermanHermit() throws Exception {
		final String TEST_NAME = "test254ReconstructHermanHermit";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestAudit.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID, 
        		hermanHermitEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);
        
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_TITLE, createPolyString("Hermit"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_NICK_NAME);
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));
        
        assertAssignedRole(hermanReconstructed, ROLE_RED_SAILOR_OID);
        assertAssignedRole(hermanReconstructed, ROLE_JUDGE_OID);
        assertAssignments(hermanReconstructed, 2);
    }
    
    
    

    private String assertObjectAuditRecords(String oid, int expectedNumberOfRecords) {
    	List<AuditEventRecord> auditRecords = getObjectAuditRecords(oid);
        display("Object records", auditRecords);
        assertEquals("Wrong number of jack audit records", expectedNumberOfRecords, auditRecords.size());
        return auditRecords.get(auditRecords.size() - 1).getEventIdentifier();
	}

	private void assertRecordsFromPrevious(XMLGregorianCalendar from, XMLGregorianCalendar to, 
			int expectedNumberOfRecords) {
        List<AuditEventRecord> auditRecordsSincePrevious = getAuditRecordsFromTo(from, to);
        display("From/to records (previous)", auditRecordsSincePrevious);
        assertEquals("Wrong number of audit records (previous)", expectedNumberOfRecords, auditRecordsSincePrevious.size());	
    }

	private void assertRecordsFromInitial(XMLGregorianCalendar to, int expectedNumberOfRecords) {
        List<AuditEventRecord> auditRecordsSincePrevious = getAuditRecordsFromTo(initialTs, to);
        display("From/to records (initial)", auditRecordsSincePrevious);
        assertEquals("Wrong number of audit records (initial)", expectedNumberOfRecords, auditRecordsSincePrevious.size());	
    }

	private List<AuditEventRecord> getObjectAuditRecords(String oid) {
		Map<String,Object> params = new HashMap<>();
		params.put("targetOid", oid);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.targetOid = :targetOid) order by aer.timestamp asc", 
        		params);
	}
	
	private List<AuditEventRecord> getAuditRecordsFromTo(XMLGregorianCalendar from, XMLGregorianCalendar to) {
		Map<String,Object> params = new HashMap<>();
		params.put("from", from);
		params.put("to", to);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.timestamp >= :from) and (aer.timestamp <= :to) order by aer.timestamp asc", 
        		params);
	}
}
