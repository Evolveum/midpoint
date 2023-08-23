/*
 * Copyright (C) 2013-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static java.util.Collections.singleton;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.testng.AssertJUnit.*;

import java.util.Arrays;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.model.intest.util.MockMultipleTriggersHandler;
import com.evolveum.midpoint.model.intest.util.MockTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * In theory, the assertions counting # of invocations could fail even if trigger task handler works well
 * - if the scanner task would run more than once. If that occurs in reality, we'll deal with it.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTriggerTask extends AbstractInitializedModelIntegrationTest {

    private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 0, 0);

    private MockTriggerHandler testTriggerHandler;
    private MockMultipleTriggersHandler testMultipleTriggersHandler;

    @Autowired
    private TriggerHandlerRegistry triggerHandlerRegistry;
    private boolean isNewRepo;

    @Override
    protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
        // addTrigger call can overlap with trigger execution on slower machines, leading to positive conflict check result
        return ConflictResolutionActionType.NONE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        // Some tests differ for new and old repo for good reasons, so we need this switch.
        isNewRepo = plainRepositoryService instanceof SqaleRepositoryService;

        testTriggerHandler = new MockTriggerHandler();
        triggerHandlerRegistry.register(MockTriggerHandler.HANDLER_URI, testTriggerHandler);

        testMultipleTriggersHandler = new MockMultipleTriggersHandler();
        triggerHandlerRegistry.register(MockMultipleTriggersHandler.HANDLER_URI, testMultipleTriggersHandler);
    }

    /**
     * Creates an old trigger and imports the trigger task. The trigger should get processed.
     */
    @Test
    public void test100ImportScannerTask() throws Exception {
        given();

        // Make sure there is an object with a trigger set to a long time ago.
        // That trigger should be invoked on first run.
        addTrigger(USER_JACK_OID, LONG_LONG_TIME_AGO, MockTriggerHandler.HANDLER_URI);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        when();

        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID);
        Task taskAfter = waitForTaskFinish(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called incorrect number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(taskAfter, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(1);
        // @formatter:on
    }

    /**
     * Waits for the task next run. No triggers should be processed.
     */
    @Test
    public void test105NoTrigger() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(1); // From previous test
        // @formatter:on
    }

    /**
     * Creates a trigger and waits for task next run. The trigger should get processed.
     */
    @Test
    public void test110TriggerCalledAgain() throws Exception {
        given();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        addTrigger(USER_JACK_OID, startCal, MockTriggerHandler.HANDLER_URI);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called incorrect number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(2); // One from test100, one newly added.
        // @formatter:on
    }

    /**
     * Creates two triggers in fast succession. Both should be processed, because the handler is not declared as idempotent.
     *
     * Note regarding the iterative task information: The increments should be:
     *
     * - 1 success
     * - 1 skip
     *
     * Even if there were two triggers processed.
     *
     * It is because we count _objects_ processed by _task handler_, not _triggers_ processed by _trigger handler_.
     * In this light, jack was processed only once, although with two triggers.
     */
    @Test
    public void test120TwoTriggers() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(3)
                    // New repo uses EXISTS, no duplicate objects, nothing to skip.
                    .assertSkipCount(isNewRepo ? 0 : 1);
        // @formatter:on
    }

    /**
     * Creates two (distinct) triggers with the same timestamp. Both should be processed, because the handler
     * is not declared as idempotent.
     */
    @Test
    public void test130TwoTriggersSame() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal), MockTriggerHandler.HANDLER_URI, true);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(4) // +1 real execution
                    .assertSkipCount(isNewRepo ? 0 : 2); // +1 skipped occurrence for old repo
        // @formatter:on
    }

    /**
     * Creates two (distinct) triggers with the same timestamp PLUS one trigger a little bit later.
     * All three should be processed, because the handler is not declared as idempotent.
     */
    @Test
    public void test135TwoTriggersSamePlusOne() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, true);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 3, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(5) // +1 real execution
                    .assertSkipCount(isNewRepo ? 0 : 4); // +2 skipped occurrences for old repo
        // @formatter:on
    }

    /**
     * Creates two distinct triggers with the same timestamp. But the handler should be executed only once (with 2 triggers),
     * because now we use {@link MockMultipleTriggersHandler} that accepts multiple triggers with the same timestamp.
     */
    @Test
    public void test140TwoTriggersSameUsingMultiHandler() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal), MockMultipleTriggersHandler.HANDLER_URI, true);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testMultipleTriggersHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testMultipleTriggersHandler.getInvocationCount());
        assertEquals("Wrong # of triggers last executed", 2, testMultipleTriggersHandler.getLastTriggers().size());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(6) // +1 real execution
                    .assertSkipCount(isNewRepo ? 0 : 5); // +1 skipped occurrence for old repo
        // @formatter:on
    }

    /**
     * Creates two (distinct) triggers with the same timestamp PLUS one trigger a little bit later.
     * The handler should be executed only twice:
     * (1) first with two triggers having the same timestamp,
     * (2) second with the third trigger that fires a bit later,
     * because we use {@link MockMultipleTriggersHandler} that accepts multiple triggers with the same timestamp.
     */
    @Test
    public void test145TwoTriggersSamePlusOneUsingMultiHandler() throws Exception {
        given();

        testMultipleTriggersHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCal, startCalPlus5ms),
                MockMultipleTriggersHandler.HANDLER_URI, true);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testMultipleTriggersHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testMultipleTriggersHandler.getInvocationCount());
        assertEquals("Wrong # of triggers last executed", 1, testMultipleTriggersHandler.getLastTriggers().size());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(7) // +1 real execution
                    .assertSkipCount(isNewRepo ? 0 : 7); // +2 skipped occurrences for old repo
        // @formatter:on
    }

    /**
     * Creates two triggers in fast succession. The (now idempotent) handler should get called only once.
     */
    @Test
    public void test147TwoTriggersIdempotent() throws Exception {
        given();

        testTriggerHandler.reset();
        testTriggerHandler.setIdempotent(true);

        try {
            XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
            XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
            startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
            addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

            when();

            waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

            then();

            XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

            assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
            assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
            assertNoTrigger(UserType.class, USER_JACK_OID);

            // @formatter:off
            assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                    .assertLastTriggerScanTimestamp(startCal, endCal)
                    .rootItemProcessingInformation()
                        .display()
                        .assertSuccessCount(8) // +1 real execution
                        .assertSkipCount(isNewRepo ? 0 : 8); // +1 skipped occurrence for old repo
            // @formatter:on
        } finally {
            testTriggerHandler.setIdempotent(false);
        }
    }

    /**
     * Just a no-op run. No triggers present -> no triggers processed.
     */
    @Test
    public void test150NoTriggerAgain() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNull("Trigger was called while not expecting it", testTriggerHandler.getLastObject());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(8) // no change
                    .assertSkipCount(isNewRepo ? 0 : 8); // no change
        // @formatter:on
    }

    /**
     * Creates two triggers in fast succession. However, the first one fails during processing.
     *
     * The handler should try to execute both. Only the second should get deleted.
     *
     * During second run the failed trigger should be re-processed.
     *
     * MID-4610
     */
    @Test
    public void test160TwoTriggersFirstFails() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5ms = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5ms.add(XmlTypeConverter.createDuration(5L));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5ms), MockTriggerHandler.HANDLER_URI, false);

        testTriggerHandler.setFailOnNextInvocation(true);

        when("first run (first trigger fails)");

        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, 10000);

        then("first run (first trigger fails)");

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 2, testTriggerHandler.getInvocationCount());
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertEquals("Wrong # of triggers found", 1, userAfter.asObjectable().getTrigger().size());
        assertTrigger(userAfter, MockTriggerHandler.HANDLER_URI, startCal, 1);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after 1st run")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertFailureCount(1) // +1 failed run (2 triggers: one success, one failure)
                    .assertSuccessCount(8) // no new successful runs
                    .assertSkipCount(isNewRepo ? 0 : 9); // +1 skipped occurrence for old repo
        // @formatter:on

        when("second run (retries failed trigger)");

        // remove all traces of failures in order to "waitForTaskNextRunAssertSuccess" be happy
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_RESULT).replace()
                .item(TaskType.F_RESULT_STATUS).replace()
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, TASK_TRIGGER_SCANNER_OID, modifications, result);

        testTriggerHandler.reset();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then("second run (retries failed trigger)");

        endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        assertNoTrigger(UserType.class, USER_JACK_OID);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after 2nd run")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertFailureCount(1) // no new failures
                    .assertSuccessCount(9) // +1 success
                    .assertSkipCount(isNewRepo ? 0 : 9); // no new skips
        // @formatter:on
    }

    /**
     * Creates two distant triggers (second is in 5 days). Only the first one should get executed.
     */
    @Test
    public void test200TwoDistantTriggers() throws Exception {
        given();

        testTriggerHandler.reset();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5days = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5days.add(XmlTypeConverter.createDuration("P5D"));
        addTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5days), MockTriggerHandler.HANDLER_URI, false);

        when();

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID);

        then();

        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        assertTrigger(getUser(USER_JACK_OID), MockTriggerHandler.HANDLER_URI, startCalPlus5days, 100L);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .assertLastTriggerScanTimestamp(startCal, endCal)
                .rootItemProcessingInformation()
                    .display()
                    .assertFailureCount(1) // no new failures
                    .assertSuccessCount(10) // +1 success
                    .assertSkipCount(isNewRepo ? 0 : 9); // no new skips
        // @formatter:on
    }

    /**
     * Checks that no trigger gets lost if the task is interrupted (MID-4474).
     *
     * Creates two distant triggers (second is in 5 days).
     * Makes the handler run for very long.
     *
     * When the handler starts, suspends the task. Ensures that the last scan timestamp was not updated.
     *
     * TODO Why we do remove the trigger that was not completely executed? This looks like it's wrong.
     *  However, for normal operations (involving clockwork) the interruption usually means that the object
     *  in question is processed. In this way it makes sense. Also it makes sense that such objects
     *  are counted as "successfully processed".
     */
    @Test
    public void test210InterruptedScanner() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        testTriggerHandler.reset();

        // to avoid unexpected runs of this task that would move lastScanTimestamp
        boolean suspended = taskManager.suspendTasks(singleton(TASK_TRIGGER_SCANNER_OID), 20000L, result);
        assertTrue("trigger scanner task was not suspended (before operation)", suspended);

        XMLGregorianCalendar lastScanTimestampBefore = getLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, ActivityPath.empty());
        assertNotNull(lastScanTimestampBefore);

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar startCalPlus5days = XmlTypeConverter.createXMLGregorianCalendar(startCal);
        startCalPlus5days.add(XmlTypeConverter.createDuration("P5D"));
        replaceTriggers(USER_JACK_OID, Arrays.asList(startCal, startCalPlus5days), MockTriggerHandler.HANDLER_URI);

        final long ONE_DAY = 86400L * 1000L;
        testTriggerHandler.setDelay(ONE_DAY);

        taskManager.resumeTasks(singleton(TASK_TRIGGER_SCANNER_OID), result);

        when();

        IntegrationTestTools.waitFor("Waiting for trigger handler invocation", () -> testTriggerHandler.getInvocationCount() > 0, 60000);
        suspended = taskManager.suspendTasks(singleton(TASK_TRIGGER_SCANNER_OID), 20000L, result);
        assertTrue("trigger scanner task was not suspended (after operation)", suspended);

        then();

        assertNotNull("Trigger was not called", testTriggerHandler.getLastObject());
        assertEquals("Trigger was called wrong number of times", 1, testTriggerHandler.getInvocationCount());
        PrismObject<UserType> jackAfter = getUser(USER_JACK_OID);
        display("jack after", jackAfter);
        assertTrigger(jackAfter, MockTriggerHandler.HANDLER_URI, startCalPlus5days, 100L);
        assertEquals("Wrong # of triggers on jack", 1, jackAfter.asObjectable().getTrigger().size());

        XMLGregorianCalendar lastScanTimestampAfter = getLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, ActivityPath.empty());

        // this assert may fail occasionally if the trigger scanner would start in between (we'll see how often)
        assertEquals("Last scan timestamp was changed", lastScanTimestampBefore, lastScanTimestampAfter);

        // @formatter:off
        assertTask(TASK_TRIGGER_SCANNER_OID, "after")
                .rootItemProcessingInformation()
                .display()
                    .assertFailureCount(1) // no new failures
                    .assertSuccessCount(11) // +1 success (see the note in method javadoc)
                    .assertSkipCount(isNewRepo ? 0 : 9); // no new skips
        // @formatter:on
    }

    // trigger scanner task is suspended here; and handler is set to a delay of one day (reset will clear that)
}
