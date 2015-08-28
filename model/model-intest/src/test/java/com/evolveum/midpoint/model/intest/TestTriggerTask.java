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
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.util.MockTriggerHandler;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTriggerTask extends AbstractInitializedModelIntegrationTest {

	private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 00, 00);

	private MockTriggerHandler testTriggerHandler;
	
	private XMLGregorianCalendar drakeValidFrom;
	private XMLGregorianCalendar drakeValidTo;
	
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
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Make sure there is an object with a trigger set to a long time ago.
        // That trigger should be invoked on first run.
        addTrigger(USER_JACK_OID, LONG_LONG_TIME_AGO, MockTriggerHandler.HANDLER_URI);
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
		
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
        
        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);
        
        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test105NoTrigger() throws Exception {
		final String TEST_NAME = "test105NoTrigger";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
                
		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test110TriggerCalledAgain() throws Exception {
		final String TEST_NAME = "test110TriggerCalledAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        
        addTrigger(USER_JACK_OID, startCal, MockTriggerHandler.HANDLER_URI);
                
		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
	@Test
    public void test115NoTriggerAgain() throws Exception {
		final String TEST_NAME = "test115NoTriggerAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestTriggerTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
                
		/// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastRecomputeTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
	}
	
}
