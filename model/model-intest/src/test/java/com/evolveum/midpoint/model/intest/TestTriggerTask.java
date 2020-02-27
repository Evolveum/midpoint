/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static java.util.Collections.singleton;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.intest.util.MockMultipleTriggersHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.model.intest.util.MockTriggerHandler;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Radovan Semancik
 *
 * PMed: In theory, the assertions counting # of invocations could fail even if trigger task handler works well
 * - if the scanner task would run more than once. If that occurs in reality, we'll deal with it.
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTriggerTask extends AbstractInitializedModelIntegrationTest {

    private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 0, 0);

    private MockTriggerHandler testTriggerHandler;
    private MockMultipleTriggersHandler testMultipleTriggersHandler;

    private XMLGregorianCalendar drakeValidFrom;
    private XMLGregorianCalendar drakeValidTo;

    @Autowired
    private TriggerHandlerRegistry triggerHandlerRegistry;

    @Override
    protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
        // addTrigger call can overlap with trigger execution on slower machines, leading to positive conflict check result
        return ConflictResolutionActionType.NONE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // TODO Auto-generated method stub
        super.initSystem(initTask, initResult);

        testTriggerHandler = new MockTriggerHandler();
        triggerHandlerRegistry.register(MockTriggerHandler.HANDLER_URI, testTriggerHandler);

        testMultipleTriggersHandler = new MockMultipleTriggersHandler();
        triggerHandlerRegistry.register(MockMultipleTriggersHandler.HANDLER_URI, testMultipleTriggersHandler);
    }

    @Test
    public void test100ImportScannerTask() throws Exception {
        final String TEST_NAME = "test100ImportScannerTask";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
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
        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called incorrect number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test105NoTrigger() throws Exception {
        final String TEST_NAME = "test105NoTrigger";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
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

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test110TriggerCalledAgain() throws Exception {
        final String TEST_NAME = "test110TriggerCalledAgain";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
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
        assertEquals("Trigger was called incorrect number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test120TwoTriggers() throws Exception {
        final String TEST_NAME = "test120TwoTriggers";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        // Originally here was only one execution expected. But why? There are two triggers
        // with different triggering times! So the handler should be really called two times.
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test130TwoTriggersSame() throws Exception {
        final String TEST_NAME = "test130TwoTriggersSame";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal), MockTriggerHandler.HANDLER_URI, true);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test135TwoTriggersSamePlusOne() throws Exception {
        final String TEST_NAME = "test135TwoTriggersSamePlusOne";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, true);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 3, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test140TwoTriggersSameAggregable() throws Exception {
        final String TEST_NAME = "test140TwoTriggersSameAggregable";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal), MockMultipleTriggersHandler.HANDLER_URI, true);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testMultipleTriggersHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testMultipleTriggersHandler.getInvocationCount());
        assertEquals("Wrong # of triggers last executed", 2, testMultipleTriggersHandler.getLastTriggers().size());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test145TwoTriggersSamePlusOneAggregable() throws Exception {
        final String TEST_NAME = "test145TwoTriggersSamePlusOneAggregable";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testMultipleTriggersHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal, startCalPlus5ms), MockMultipleTriggersHandler.HANDLER_URI,
                true);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testMultipleTriggersHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testMultipleTriggersHandler.getInvocationCount());
        assertEquals("Wrong # of triggers last executed", 1, testMultipleTriggersHandler.getLastTriggers().size());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test147TwoTriggersIdempotent() throws Exception {
        final String TEST_NAME = "test147TwoTriggersIdempotent";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();
        testTriggerHandler.setIdempotent(true);

        try {
            XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
            XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
            startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
            addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

            /// WHEN
            TestUtil.displayWhen(TEST_NAME);
            waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

            // THEN
            TestUtil.displayThen(TEST_NAME);

            // THEN
            XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

            assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
            assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
            assertNoTrigger(UserType.class, USER_JACK_OID);

            assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
        } finally {
            testTriggerHandler.setIdempotent(false);
        }
    }

    @Test
    public void test150NoTriggerAgain() throws Exception {
        final String TEST_NAME = "test115NoTriggerAgain";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
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

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    // MID-4610
    @Test
    public void test160TwoTriggersFirstFails() throws Exception {
        final String TEST_NAME = "test160TwoTriggersFirstFails";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

        testTriggerHandler.setFailOnNextInvocation(true);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 10000);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertEquals("Wrong # of triggers found", 1, userAfter.asObjectable().getTrigger().size());
        assertTrigger(userAfter, MockTriggerHandler.HANDLER_URI, startCal, 1);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);

        // -------------------- re-run the handler

        // remove all traces of failures in order to "waitForTaskNextRunAssertSuccess" be happy
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_RESULT).replace()
                .item(TaskType.F_RESULT_STATUS).replace()
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, TASK_TRIGGER_SCANNER_OID, modifications, result);

        testTriggerHandler.reset();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);
        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test200TwoDistantTriggers() throws Exception {
        final String TEST_NAME = "test200TwoDistantTriggers";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5days = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5days.add(XmlTypeConverter.createDuration("P5D"));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5days), MockTriggerHandler.HANDLER_URI, false);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        assertTrigger(getUser(USER_JACK_OID), MockTriggerHandler.HANDLER_URI, startCalPlus5days, 100L);

        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);
    }

    // MID-4474
    @Test
    public void test210InterruptedScanner() throws Exception {
        final String TEST_NAME = "test210InterruptedScanner";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        TestTriggerTask.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        // to avoid unexpected runs of this task that would move lastScanTimestamp
        boolean suspended = taskManager.suspendTasks(singleton(TASK_TRIGGER_SCANNER_OID), 20000L, result);
        assertTrue("trigger scanner task was not suspended (before operation)", suspended);

        XMLGregorianCalendar lastScanTimestampBefore = getLastScanTimestamp(TASK_TRIGGER_SCANNER_OID);
        assertNotNull(lastScanTimestampBefore);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5days = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5days.add(XmlTypeConverter.createDuration("P5D"));
        replaceTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5days), MockTriggerHandler.HANDLER_URI);

        final long ONE_DAY = 86400L * 1000L;
        testTriggerHandler.setDelay(ONE_DAY);

        taskManager.resumeTasks(singleton(TASK_TRIGGER_SCANNER_OID), result);

        /// WHEN
        TestUtil.displayWhen(TEST_NAME);
        IntegrationTestTools.waitFor("Waiting for trigger handler invocation", () -> testTriggerHandler.getInvocationCount() > 0, 60000);
        suspended = taskManager.suspendTasks(singleton(TASK_TRIGGER_SCANNER_OID), 20000L, result);
        assertTrue("trigger scanner task was not suspended (after operation)", suspended);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        // THEN
        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        PrismObject<UserType> jackAfter = getUser(USER_JACK_OID);
        display("jack after", jackAfter);
        assertTrigger(jackAfter, MockTriggerHandler.HANDLER_URI, startCalPlus5days, 100L);
        assertEquals("Wrong # of triggers on jack", 1, jackAfter.asObjectable().getTrigger().size());

        XMLGregorianCalendar lastScanTimestampAfter = getLastScanTimestamp(TASK_TRIGGER_SCANNER_OID);

        // this assert may fail occasionally if the trigger scanner would start in between (we'll see how often)
        assertEquals("Last scan timestamp was changed", lastScanTimestampBefore, lastScanTimestampAfter);
    }

    // trigger scanner task is suspended here; and handler is set to a delay of one day (reset will clear that)
}
