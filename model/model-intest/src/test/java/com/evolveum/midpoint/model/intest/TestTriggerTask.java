/*
 * Copyright (c) 2013 Evolveum
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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.ModelConstants;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.util.MockTriggerHandler;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTriggerTask extends AbstractInitializedModelIntegrationTest {

	private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 00, 00);

	private MockTriggerHandler testTriggerHandler;
	
	@Autowired(required=true)
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// TODO Auto-generated method stub
		super.initSystem(initTask, initResult);
		
		testTriggerHandler = new MockTriggerHandler();
		
		triggerHandlerRegistry.register(MockTriggerHandler.HANDLER_URI, testTriggerHandler);
	}

	@Test
    public void test100ImportScannerTask() throws Exception {
		final String TEST_NAME = "test100ImportScannerTask";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Make sure there is an object with a trigger set to a long time ago.
        // That trigger should be invoked on first run.
        addTrigger(USER_JACK_OID, LONG_LONG_TIME_AGO, MockTriggerHandler.HANDLER_URI);
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
		/// WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
		
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID, true);
        
        // THEN
        displayThen(TEST_NAME);
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
        
        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);
        
        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test105NoTrigger() throws Exception {
		final String TEST_NAME = "test105NoTrigger";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
                
		/// WHEN
        displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test110TriggerCalledAgain() throws Exception {
		final String TEST_NAME = "test110TriggerCalledAgain";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
        addTrigger(USER_JACK_OID, startCal, MockTriggerHandler.HANDLER_URI);
                
		/// WHEN
        displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test115NoTriggerAgain() throws Exception {
		final String TEST_NAME = "test115NoTriggerAgain";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
                
		/// WHEN
        displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}

	/**
	 * Note: red resource disables account on unsassign, does NOT delete it.
	 * Just the recompute trigger is set
	 */
	@Test
    public void test200JackAssignAndUnassignAccountRed() throws Exception {
		final String TEST_NAME = "test200JackAssignAndUnassignAccountRed";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // assign
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, 
        		RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
		// unassign
        deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        // Let's wait for the task to give it a change to screw up
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		
		String accountRedOid = getAccountRef(userJack, RESOURCE_DUMMY_RED_OID);
		PrismObject<ShadowType> accountRed = getAccount(accountRedOid);
		
		XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
		assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, start, end);
		assertAdministrativeStatusDisabled(accountRed);

		assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
	}
	
	/**
	 * Move time a month ahead. The account that was disabled in a previous test should be
	 * deleted now. 
	 */
	@Test
    public void test210JackDummyAccountDeleteAfterMonth() throws Exception {
		final String TEST_NAME = "test210JackDummyAccountDeleteAfterMonth";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMapping.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        XMLGregorianCalendar time = clock.currentTimeXMLGregorianCalendar();
        // A month and a day, to make sure we move past the trigger
        time.add(XmlTypeConverter.createDuration(true, 0, 1, 1, 0, 0, 0));
        
        // WHEN
        displayWhen(TEST_NAME);
        clock.override(time);
        
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
        // THEN
        displayThen(TEST_NAME);
        
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
}
