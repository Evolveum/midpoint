/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for delayed HR enable/disable.
 * 
 * HR system has enabled/disabled status. We want to synchronize that status to midPoint,
 * but we want the information to be delayed by one day.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDelayedEnable extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "delayed-enable");
	
	protected static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/story/delayedEnable/ext";
	protected static final QName EXT_HR_STATUS_QNAME = new QName(NS_EXTENSION, "hrStatus");
	protected static final String EXT_HR_STATUS_ENABLED = "enabled";
	protected static final String EXT_HR_STATUS_DISABLED = "disabled";
	protected static final QName EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME = new QName(NS_EXTENSION, "hrStatusChangeTimestamp");
	
	protected static final String SUBTYPE_EMPLOYEE = "employee";
	
	protected static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
	protected static final String USER_MANCOMB_OID = "8e3a3770-cc60-11e8-8354-a7bb150473c1";
	protected static final String USER_MANCOMB_USERNAME = "mancomb";

	protected static final File ROLE_PRIVACY_END_USER_FILE = new File(TEST_DIR, "role-privacy-end-user.xml");
	protected static final String ROLE_PRIVACY_END_USER_OID = "d6f2c30a-b816-11e8-88c5-4f735c761a81";
	
	protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
	protected static final String RESOURCE_DUMMY_HR_OID = "eed4209c-cc5f-11e8-95de-a7d866db5e67";
	protected static final String RESOURCE_DUMMY_HR_NAME = "HR";
	
	protected static final File TASK_DUMMY_HR_FILE = new File(TEST_DIR, "task-dumy-hr-livesync.xml");
	protected static final String TASK_DUMMY_HR_OID = "d0341fbe-cc84-11e8-8af1-1329734dd152";
	
	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "ef638872-cc69-11e8-8ee2-333f3bf7747f";

	private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
	private static final String ACCOUNT_GUYBRUSH_FULLNAME = "Guybrush Threepwood";

	XMLGregorianCalendar hrCreateTsStart;
	XMLGregorianCalendar hrCreateTsEnd;
	XMLGregorianCalendar hrModifyTsStart;
	XMLGregorianCalendar hrModifyTsEnd;

	private String userGuybrushOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_HR_NAME, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, initTask, initResult);
		getDummyResourceHr().setSyncStyle(DummySyncStyle.SMART);
		
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		// subtype==employee: Make sure that this is not applied to administrator or other non-person accounts.
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_EMPLOYEE, OBJECT_TEMPLATE_USER_OID, initResult);
		
		modifyObjectReplaceProperty(TaskType.class, TASK_TRIGGER_SCANNER_OID, TaskType.F_RECURRENCE, null, initTask, initResult, TaskRecurrenceType.SINGLE);
		rememberCounter(InternalCounters.TRIGGER_FIRED_COUNT);
	}
	
	// Tests 1xx are basic tests, adding and modifying user directly.

	@Test
	public void test100AddUserMancomb() throws Exception {
		final String TEST_NAME = "test100AddUserMancomb";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        hrCreateTsStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        
        addObject(USER_MANCOMB_FILE, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		hrCreateTsEnd = clock.currentTimeXMLGregorianCalendar();
		
		runTriggerScanner();
		
		assertMancombCreated();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	/**
	 * Make sure that trigger scanner won't do anything bad when run one more time.
	 */
	@Test
	public void test102UserMancombTriggerScannerAgain() throws Exception {
		final String TEST_NAME = "test102UserMancombTriggerScannerAgain";
		displayTestTitle(TEST_NAME);
		displayCurrentTime();
		
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertMancombCreated();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}

	/**
	 * Make sure that recompute does not change anything.
	 * Especially that the hrStatusChangeTimestamp is not moved.
	 */
	@Test
	public void test104UserMancombRecompute() throws Exception {
		final String TEST_NAME = "test104UserMancombRecompute";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        displayCurrentTime();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombCreated();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertMancombCreated() throws Exception {
		assertUserAfter(USER_MANCOMB_OID)
			.assertName(USER_MANCOMB_USERNAME)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_ENABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrCreateTsStart, hrCreateTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.DISABLED)
				.assertAdministrativeStatus(ActivationStatusType.DISABLED)
				.end()
			.triggers()
				.single()
					.assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
					.assertTimestampFutureBetween(hrCreateTsStart, hrCreateTsEnd, "P1D");
	}
	
	/**
	 * Move time ahead. The time-constrained mapping in the object template should
	 * prevail now and it should override administrative status.
	 */
	@Test
	public void test110UserMancombRunTriggerScannerDay1() throws Exception {
		final String TEST_NAME = "test110UserMancombRunTriggerScannerDay1";
		displayTestTitle(TEST_NAME);
		
        clockForward("P1D");
        
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertMancombEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 1);
	}
	
	/**
	 * Move time ahead. The time-constrained mapping in the object template should
	 * prevail now and it should override administrative status.
	 */
	@Test
	public void test112UserMancombRecomputeDay1() throws Exception {
		final String TEST_NAME = "test112UserMancombRecomputeDay1";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("P1D");
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertMancombEnabled() throws Exception {
		assertUserAfter(USER_MANCOMB_OID)
			.assertName(USER_MANCOMB_USERNAME)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_ENABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrCreateTsStart, hrCreateTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.ENABLED)
				.assertAdministrativeStatus(ActivationStatusType.ENABLED)
				.end()
			.triggers()
				.assertNone();
	}
	
	@Test
	public void test114UserMancombRunTriggerScannerDay1Again() throws Exception {
		final String TEST_NAME = "test114UserMancombRunTriggerScannerDay1Again";
		displayTestTitle(TEST_NAME);
		
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertMancombEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	/**
	 * Make sure recompute does not ruin anything.
	 */
	@Test
	public void test116UserMancombRecomputeDay1Again() throws Exception {
		final String TEST_NAME = "test116UserMancombRecomputeDay1Again";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	/**
	 * Change hrStatus to disable. The change should NOT be reflected to administrative
	 * status immediately.
	 */
	@Test
	public void test120UserMancombHrDisable() throws Exception {
		final String TEST_NAME = "test120UserMancombHrDisable";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        hrModifyTsStart = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        modifyUserReplace(USER_MANCOMB_OID, getExtensionPath(EXT_HR_STATUS_QNAME), task, result, EXT_HR_STATUS_DISABLED);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		hrModifyTsEnd = clock.currentTimeXMLGregorianCalendar();
		
		assertMancombHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertMancombHalfDisabled() throws Exception {
		assertUserAfter(USER_MANCOMB_OID)
			.assertName(USER_MANCOMB_USERNAME)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_DISABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrModifyTsStart, hrModifyTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.ENABLED)
				.assertAdministrativeStatus(ActivationStatusType.ENABLED);
	}
	
	/**
	 * Make sure recompute does not ruin anything.
	 */
	@Test
	public void test122UserMancombRecompute() throws Exception {
		final String TEST_NAME = "test122UserMancombRecompute";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test124UserMancombDay1TriggerScanner() throws Exception {
		final String TEST_NAME = "test124UserMancombDay1TriggerScanner";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	/**
	 * Move time ahead. The time-constrained mapping in the object template should
	 * prevail now and it should override administrative status.
	 */
	@Test
	public void test130UserMancombTriggerScannerDay2() throws Exception {
		final String TEST_NAME = "test130UserMancombTriggerScannerDay2";
		displayTestTitle(TEST_NAME);
		
		clockForward("P1D");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 1);
	}
	
	private void assertMancombDisabled() throws Exception {
		assertUserAfter(USER_MANCOMB_OID)
			.assertName(USER_MANCOMB_USERNAME)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_DISABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrModifyTsStart, hrModifyTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.DISABLED)
				.assertAdministrativeStatus(ActivationStatusType.DISABLED);
	}
	
	/**
	 * Move time ahead. The time-constrained mapping in the object template should
	 * prevail now and it should override administrative status.
	 */
	@Test
	public void test132UserMancombRecomputeDay2() throws Exception {
		final String TEST_NAME = "test132UserMancombRecomputeDay2";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test132UserMancombRecomputeDay2Again() throws Exception {
		final String TEST_NAME = "test132UserMancombRecomputeDay2Again";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	/**
	 * No change today. Make sure that the triggers won't fire again and that
	 * nothing is changed.
	 */
	@Test
	public void test140UserMancombRecomputeDay3() throws Exception {
		final String TEST_NAME = "test140UserMancombRecomputeDay3";
		displayTestTitle(TEST_NAME);
		
		clockForward("P1D");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_MANCOMB_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertMancombDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	// Tests 2xx are testing the whole synchronization stack. Changes are initiated in HR.
	
	@Test
	public void test200HrLivesyncTask() throws Exception {
		final String TEST_NAME = "test200HrLivesyncTask";
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);

        addObject(TASK_DUMMY_HR_FILE, task, result);
        
        // THEN
 		displayThen(TEST_NAME);
 		assertSuccess(result);
 		
 		waitForTaskStart(TASK_DUMMY_HR_OID, true);
 		
 		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test210HrAddUserGuybrush() throws Exception {
		final String TEST_NAME = "test210HrAddUserGuybrush";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount accountGuybrushBefore = new DummyAccount(ACCOUNT_GUYBRUSH_USERNAME);
        accountGuybrushBefore.replaceAttributeValue(
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_GUYBRUSH_FULLNAME);
        accountGuybrushBefore.replaceAttributeValue(
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, EXT_HR_STATUS_ENABLED);
		getDummyResourceHr().addAccount(accountGuybrushBefore);
        
        hrCreateTsStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		hrCreateTsEnd = clock.currentTimeXMLGregorianCalendar();
		
		userGuybrushOid = assertGuybrushCreated(assertUserAfterByUsername(ACCOUNT_GUYBRUSH_USERNAME))
				.getOid();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test212HrUserGuybrushSyncAgain() throws Exception {
		final String TEST_NAME = "test212HrUserGuybrushSyncAgain";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushCreated(assertUserAfter(userGuybrushOid));
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test214HrUserGuybrushRunTriggers() throws Exception {
		final String TEST_NAME = "test214HrUserGuybrushRunTriggers";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushCreated(assertUserAfter(userGuybrushOid));
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test216HrUserGuybrushRecompute() throws Exception {
		final String TEST_NAME = "test216HrUserGuybrushRecompute";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushCreated(assertUserAfter(userGuybrushOid));
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test218HrUserGuybrushReconcile() throws Exception {
		final String TEST_NAME = "test218HrUserGuybrushReconcile";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushCreated(assertUserAfter(userGuybrushOid));
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private UserAsserter<Void> assertGuybrushCreated(UserAsserter<Void> userAsserter) throws Exception {
		return userAsserter
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_ENABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrCreateTsStart, hrCreateTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.DISABLED)
				.assertAdministrativeStatus(ActivationStatusType.DISABLED)
				.end()
			.triggers()
				.single()
					.assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
					.assertTimestampFutureBetween(hrCreateTsStart, hrCreateTsEnd, "P1D")
					.end()
				.end();
	}
	
	@Test
	public void test220HrUserGuybrushDay1() throws Exception {
		final String TEST_NAME = "test220HrUserGuybrushDay1";
		displayTestTitle(TEST_NAME);
		
		clockForward("P1D");
		
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertGuybrushEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 1);
	}
	
	@Test
	public void test222HrUserGuybrushDay1SyncAgain() throws Exception {
		final String TEST_NAME = "test222HrUserGuybrushDay1SyncAgain";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test224HrUserGuybrushDay1TriggerScanAgain() throws Exception {
		final String TEST_NAME = "test224HrUserGuybrushDay1TriggerScanAgain";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test226HrUserGuybrushDay1Recompute() throws Exception {
		final String TEST_NAME = "test226HrUserGuybrushDay1Recompute";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test228HrUserGuybrushDay1Reconcile() throws Exception {
		final String TEST_NAME = "test228HrUserGuybrushDay1Reconcile";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushEnabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertGuybrushEnabled() throws Exception {
		assertUserAfter(userGuybrushOid)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_ENABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrCreateTsStart, hrCreateTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.ENABLED)
				.assertAdministrativeStatus(ActivationStatusType.ENABLED)
				.end()
			.triggers()
				.assertNone();
	}

	@Test
	public void test230HrDisableGuybrush() throws Exception {
		final String TEST_NAME = "test230HrDisableGuybrush";
		displayTestTitle(TEST_NAME);
		
        getDummyResourceHr()
        	.getAccountByUsername(ACCOUNT_GUYBRUSH_USERNAME)
        		.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, EXT_HR_STATUS_DISABLED);
        
        hrModifyTsStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		
		hrModifyTsEnd = clock.currentTimeXMLGregorianCalendar();
		
		assertGuybrushHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test232GuybrushHrSyncAgain() throws Exception {
		final String TEST_NAME = "test232GuybrushHrSyncAgain";
		displayTestTitle(TEST_NAME);
		
		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		
		assertGuybrushHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test234GuybrushRecompute() throws Exception {
		final String TEST_NAME = "test232GuybrushHrSyncAgain";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test236GuybrushReconcile() throws Exception {
		final String TEST_NAME = "test236GuybrushReconcile";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test238GuybrushrunTriggersAgain() throws Exception {
		final String TEST_NAME = "test238GuybrushrunTriggersAgain";
		displayTestTitle(TEST_NAME);
		
		clockForward("PT1H");
        
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertGuybrushHalfDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertGuybrushHalfDisabled() throws Exception {
		assertUserAfter(userGuybrushOid)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_DISABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrModifyTsStart, hrModifyTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.ENABLED)
				.assertAdministrativeStatus(ActivationStatusType.ENABLED)
				.end()
			.triggers()
				.single()
					.assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
					.assertTimestampFutureBetween(hrModifyTsStart, hrModifyTsEnd, "P1D");
	}
	
	@Test
	public void test240HrUserGuybrushDay2() throws Exception {
		final String TEST_NAME = "test240HrUserGuybrushDay2";
		displayTestTitle(TEST_NAME);
		
		clockForward("P1D");
		
		// WHEN
        displayWhen(TEST_NAME);
        
        runTriggerScanner();

		// THEN
		displayThen(TEST_NAME);
		
		assertGuybrushDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 1);
	}
	
	@Test
	public void test242GuybrushRecompute() throws Exception {
		final String TEST_NAME = "test242GuybrushRecompute";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test244GuybrushHrSyncAgain() throws Exception {
		final String TEST_NAME = "test244GuybrushHrSyncAgain";
		displayTestTitle(TEST_NAME);
		
		// WHEN
        displayWhen(TEST_NAME);
        
        syncWithHr();

		// THEN
		displayThen(TEST_NAME);
		
		assertGuybrushDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	@Test
	public void test246GuybrushReconcile() throws Exception {
		final String TEST_NAME = "test246GuybrushReconcile";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(userGuybrushOid, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertGuybrushDisabled();
		
		assertCounterIncrement(InternalCounters.TRIGGER_FIRED_COUNT, 0);
	}
	
	private void assertGuybrushDisabled() throws Exception {
		assertUserAfter(userGuybrushOid)
			.assertSubtype(SUBTYPE_EMPLOYEE)
			.extension()
				.assertPropertyValue(EXT_HR_STATUS_QNAME, EXT_HR_STATUS_DISABLED)
				.assertTimestampBetween(EXT_HR_STATUS_CHANGE_TIMESTAMP_QNAME, hrModifyTsStart, hrModifyTsEnd)
				.end()
			.activation()
				.assertEffectiveStatus(ActivationStatusType.DISABLED)
				.assertAdministrativeStatus(ActivationStatusType.DISABLED)
				.end()
			.triggers()
				.assertNone();
	}
	
	private void syncWithHr() throws Exception {
		restartTask(TASK_DUMMY_HR_OID);		
		waitForTaskNextRunAssertSuccess(TASK_DUMMY_HR_OID, true);
	}
	
	private void runTriggerScanner() throws Exception {
		restartTask(TASK_TRIGGER_SCANNER_OID);		
		waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);
	}

	private DummyResource getDummyResourceHr() {
		return getDummyResource(RESOURCE_DUMMY_HR_NAME);
	}

}
