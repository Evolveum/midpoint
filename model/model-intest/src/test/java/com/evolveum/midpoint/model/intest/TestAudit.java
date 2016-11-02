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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test of Model Audit Service
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
        
        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        initialTs = clock.currentTimeXMLGregorianCalendar();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_TITLE, task, result, PrismTestUtil.createPolyString("Kid"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackKidTs = clock.currentTimeXMLGregorianCalendar();
        jackKidEid = assertObjectAuditRecords(USER_JACK_OID, 2);
        assertRecordsFromInitial(jackKidTs, 2);
    }

    @Test
    public void test110ModifyUserJackSailor() throws Exception {
		final String TEST_NAME = "test110ModifyUserJackSailor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_TITLE, task, result, PrismTestUtil.createPolyString("Sailor"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackSailorTs = clock.currentTimeXMLGregorianCalendar();
        jackSailorEid = assertObjectAuditRecords(USER_JACK_OID, 4);
        
        assertRecordsFromPrevious(jackKidTs, jackSailorTs, 2);
        assertRecordsFromInitial(jackSailorTs, 4);    
    }


	@Test
    public void test120ModifyUserJack() throws Exception {
		final String TEST_NAME = "test120ModifyUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_TITLE, task, result, PrismTestUtil.createPolyString("Captain"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        jackCaptainTs = clock.currentTimeXMLGregorianCalendar();
        jackCaptainEid = assertObjectAuditRecords(USER_JACK_OID, 6);
        
        assertRecordsFromPrevious(jackSailorTs, jackCaptainTs, 2);
        assertRecordsFromInitial(jackCaptainTs, 6);    
    }
    
    @Test
    public void test200ReconstructJackSailor() throws Exception {
		final String TEST_NAME = "test200ReconstructJackSailor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, PrismTestUtil.createPolyString("Captain"));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID, 
        		jackSailorEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);
        
        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, PrismTestUtil.createPolyString("Sailor"));
        
        // TODO
    }
    
    @Test
    public void test210ReconstructJackKid() throws Exception {
		final String TEST_NAME = "test210ReconstructJackKid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, PrismTestUtil.createPolyString("Captain"));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID, 
        		jackKidEid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);
        
        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, PrismTestUtil.createPolyString("Kid"));
        
        // TODO
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
