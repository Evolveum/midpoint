/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.task.quartzimpl;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.task.quartzimpl.handlers.NoOpTaskHandler;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.opends.server.types.Attribute;
import org.opends.server.types.SearchResultEntry;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 */

@ContextConfiguration(locations = {"classpath:ctx-task.xml",
        "classpath:ctx-task-test.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-expression-test.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-configuration-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestQuartzTaskManagerContract extends AbstractTestNGSpringContextTests {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestQuartzTaskManagerContract.class);

    private static final String TASK_OWNER_FILENAME = "src/test/resources/repo/owner.xml";
    private static final String TASK_OWNER2_FILENAME = "src/test/resources/repo/owner2.xml";
    private static final String TASK_OWNER2_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
    private static final String NS_WHATEVER = "http://myself.me/schemas/whatever";

    private static String taskFilename(String test) {
    	return "src/test/resources/repo/task-" + test + ".xml";
    }

    private static String taskOid(String test, String subId) {
        return "91919191-76e0-59e2-86d6-55665566" + subId + test.substring(0, 3);
    }

    private static String taskOid(String test) {
    	return taskOid(test, "0");
    }

    private static OperationResult createResult(String test) {
    	System.out.println("===[ test"+test+" ]===");
    	LOGGER.info("===[ test"+test+" ]===");
    	return new OperationResult(TestQuartzTaskManagerContract.class.getName() + ".test" + test);
    }

    private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
    private static final String CYCLE_FINISHING_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-finishing-task-handler";
    public static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
    public static final String SINGLE_TASK_HANDLER_2_URI = "http://midpoint.evolveum.com/test/single-task-handler-2";
    public static final String SINGLE_TASK_HANDLER_3_URI = "http://midpoint.evolveum.com/test/single-task-handler-3";
    public static final String L1_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l1-task-handler";
    public static final String L2_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l2-task-handler";
    public static final String L3_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l3-task-handler";
    public static final String WAIT_FOR_SUBTASKS_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/wait-for-subtasks-task-handler";
    public static final String PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";
    public static final String LONG_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/long-task-handler";

    @Autowired
    private RepositoryService repositoryService;
    private static boolean repoInitialized = false;

    @Autowired
    private TaskManagerQuartzImpl taskManager;

    @Autowired
    private PrismContext prismContext;

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        ClusterManager.setUpdateNodeExecutionLimitations(false);
	}

    // We need this complicated init as we want to initialize repo only once.
    // JUnit will
    // create new class instance for every test, so @Before and @PostInit will
    // not work
    // directly. We also need to init the repo after spring autowire is done, so
    // @BeforeClass won't work either.
    @BeforeMethod
    public void initRepository() throws Exception {
        if (!repoInitialized) {
            // addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME);
            repoInitialized = true;
        }
    }

    MockSingleTaskHandler singleHandler1, singleHandler2, singleHandler3;
    MockSingleTaskHandler l1Handler, l2Handler, l3Handler;
    MockSingleTaskHandler waitForSubtasksTaskHandler;
    MockCycleTaskHandler cycleFinishingHandler;
    MockParallelTaskHandler parallelTaskHandler;
    MockLongTaskHandler longTaskHandler;

    @PostConstruct
    public void initHandlers() throws Exception {
        MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler(false);    // ordinary recurring task
        taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);
        cycleFinishingHandler = new MockCycleTaskHandler(true);                 // finishes the handler
        taskManager.registerHandler(CYCLE_FINISHING_TASK_HANDLER_URI, cycleFinishingHandler);

        singleHandler1 = new MockSingleTaskHandler("1", taskManager);
        taskManager.registerHandler(SINGLE_TASK_HANDLER_URI, singleHandler1);
        singleHandler2 = new MockSingleTaskHandler("2", taskManager);
        taskManager.registerHandler(SINGLE_TASK_HANDLER_2_URI, singleHandler2);
        singleHandler3 = new MockSingleTaskHandler("3", taskManager);
        taskManager.registerHandler(SINGLE_TASK_HANDLER_3_URI, singleHandler3);

        l1Handler = new MockSingleTaskHandler("L1", taskManager);
        l2Handler = new MockSingleTaskHandler("L2", taskManager);
        l3Handler = new MockSingleTaskHandler("L3", taskManager);
        taskManager.registerHandler(L1_TASK_HANDLER_URI, l1Handler);
        taskManager.registerHandler(L2_TASK_HANDLER_URI, l2Handler);
        taskManager.registerHandler(L3_TASK_HANDLER_URI, l3Handler);

        waitForSubtasksTaskHandler = new MockSingleTaskHandler("WFS", taskManager);
        taskManager.registerHandler(WAIT_FOR_SUBTASKS_TASK_HANDLER_URI, waitForSubtasksTaskHandler);
        parallelTaskHandler = new MockParallelTaskHandler("1", taskManager);
        taskManager.registerHandler(PARALLEL_TASK_HANDLER_URI, parallelTaskHandler);
        longTaskHandler = new MockLongTaskHandler("1", taskManager);
        taskManager.registerHandler(LONG_TASK_HANDLER_URI, longTaskHandler);

        addObjectFromFile(TASK_OWNER_FILENAME);
        addObjectFromFile(TASK_OWNER2_FILENAME);
    }

    /**
     * Test integrity of the test setup.
     *
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    @Test(enabled = true)
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    /**
     * Here we only test setting various task properties.
     */

    @Test(enabled = true)
    public void test003GetProgress() throws Exception {

        String test = "003GetProgress";
        OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        logger.trace("Retrieving the task and getting its progress...");

        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);
        AssertJUnit.assertEquals("Progress is not 0", 0, task.getProgress());
    }


    @Test(enabled=false)          // this is probably OK to fail, so do not enable it (at least for now)
    public void test004aTaskBigProperty() throws Exception {
        String test = "004aTaskBigProperty";
        OperationResult result = createResult(test);

        String string300 = "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";
        String string300a = "AAAAAAAAA-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";

        addObjectFromFile(taskFilename(test));

        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

        // property definition
        QName bigStringQName = new QName("http://midpoint.evolveum.com/repo/test", "bigString");
        PrismPropertyDefinitionImpl bigStringDefinition = new PrismPropertyDefinitionImpl(bigStringQName, DOMUtil.XSD_STRING, taskManager.getPrismContext());
        bigStringDefinition.setIndexed(false);
        bigStringDefinition.setMinOccurs(0);
        bigStringDefinition.setMaxOccurs(1);
        System.out.println("bigstring property definition = " + bigStringDefinition);

        PrismProperty<String> bigStringProperty = (PrismProperty<String>) bigStringDefinition.instantiate();
        bigStringProperty.setRealValue(string300);
        task.setExtensionProperty(bigStringProperty);

        task.savePendingModifications(result);

        System.out.println("1st round: Task = " + task.debugDump());

        logger.trace("Retrieving the task and comparing its properties...");

        Task task001 = taskManager.getTask(taskOid(test), result);
        System.out.println("1st round: Task from repo: " + task001.debugDump());

        PrismProperty<String> bigString001 = task001.getExtensionProperty(bigStringQName);
        assertEquals("Big string not retrieved correctly (1st round)", bigStringProperty.getRealValue(), bigString001.getRealValue());

        // second round

        bigStringProperty.setRealValue(string300a);
        task001.setExtensionProperty(bigStringProperty);

        // brutal hack, because task extension property has no "indexed" flag when retrieved from repo
        ((PrismPropertyDefinitionImpl) task001.getExtensionProperty(bigStringQName).getDefinition()).setIndexed(false);

        System.out.println("2nd round: Task before save = " + task001.debugDump());
        task001.savePendingModifications(result);   // however, this does not work, because 'modifyObject' in repo first reads object, overwriting any existing definitions ...

        Task task002 = taskManager.getTask(taskOid(test), result);
        System.out.println("2nd round: Task from repo: " + task002.debugDump());

        PrismProperty<String> bigString002 = task002.getExtensionProperty(bigStringQName);
        assertEquals("Big string not retrieved correctly (2nd round)", bigStringProperty.getRealValue(), bigString002.getRealValue());
    }

    @Test(enabled = true)
    public void test004bTaskBigProperty() throws Exception {
        String test = "004aTaskBigProperty";
        OperationResult result = createResult(test);

        String string300 = "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";
        String string300a = "AAAAAAAAA-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";

        addObjectFromFile(taskFilename(test));

        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

        // property definition
        QName shipStateQName = new QName("http://myself.me/schemas/whatever", "shipState");
        PrismPropertyDefinition shipStateDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(shipStateQName);
        assertNotNull("Cannot find property definition for shipState", shipStateDefinition);

        PrismProperty<String> shipStateProperty = (PrismProperty<String>) shipStateDefinition.instantiate();
        shipStateProperty.setRealValue(string300);
        task.setExtensionProperty(shipStateProperty);

        task.savePendingModifications(result);

        System.out.println("1st round: Task = " + task.debugDump());

        logger.trace("Retrieving the task and comparing its properties...");

        Task task001 = taskManager.getTask(taskOid(test), result);
        System.out.println("1st round: Task from repo: " + task001.debugDump());

        PrismProperty<String> shipState001 = task001.getExtensionProperty(shipStateQName);
        assertEquals("Big string not retrieved correctly (1st round)", shipStateProperty.getRealValue(), shipState001.getRealValue());

        // second round

        shipStateProperty.setRealValue(string300a);
        task001.setExtensionProperty(shipStateProperty);

        System.out.println("2nd round: Task before save = " + task001.debugDump());
        task001.savePendingModifications(result);

        Task task002 = taskManager.getTask(taskOid(test), result);
        System.out.println("2nd round: Task from repo: " + task002.debugDump());

        PrismProperty<String> bigString002 = task002.getExtensionProperty(shipStateQName);
        assertEquals("Big string not retrieved correctly (2nd round)", shipStateProperty.getRealValue(), bigString002.getRealValue());
    }

    @Test(enabled = false)
    public void test004cReferenceInExtension() throws Exception {               // ok to fail

        String test = "004cReferenceInExtension";
        OperationResult result = createResult(test);
        addObjectFromFile(taskFilename(test));

        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

        System.out.println("Task extension = " + task.getExtension());

        //PrismObject<UserType> requestee = task.getOwner();
        //task.setRequesteeRef(requestee);

        //logger.trace("Saving modifications...");
        //task.savePendingModifications(result);          // here it crashes

        //logger.trace("Retrieving the task and comparing its properties...");
        //Task task001 = taskManager.getTask(taskOid(test), result);
        //logger.trace("Task from repo: " + task001.debugDump());
        //AssertJUnit.assertEquals("RequesteeRef was not stored/retrieved correctly", requestee.getOid(), task001.getRequesteeRef().getOid());
    }

    @Test(enabled = false)
    public void test004TaskProperties() throws Exception {

    	String test = "004TaskProperties";
        OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

        System.out.println("Task extension = " + task.getExtension());

        PrismPropertyDefinition delayDefinition = new PrismPropertyDefinitionImpl(SchemaConstants.NOOP_DELAY_QNAME, DOMUtil.XSD_INT, taskManager.getPrismContext());
        System.out.println("property definition = " + delayDefinition);

        PrismProperty<Integer> property = (PrismProperty<Integer>) delayDefinition.instantiate();
        property.setRealValue(100);

        PropertyDelta delta = new PropertyDelta(new ItemPath(TaskType.F_EXTENSION, property.getElementName()), property.getDefinition(), prismContext);
        //delta.addV(property.getValues());
        delta.setValuesToReplace(PrismValue.cloneCollection(property.getValues()));

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>(1);
        modifications.add(delta);

        // TODO fix this code
//        Collection<ItemDeltaType> idts = DeltaConvertor.toPropertyModificationTypes(delta);
//        for (ItemDeltaType idt : idts) {
//            String idtxml = prismContext.getParserDom().marshalElementToString(idt, new QName("http://a/", "A"));
//            System.out.println("item delta type = " + idtxml);
//
//            ItemDeltaType idt2 = prismContext.getPrismJaxbProcessor().unmarshalObject(idtxml, ItemDeltaType.class);
//            ItemDelta id2 = DeltaConvertor.createItemDelta(idt2, TaskType.class, prismContext);
//            System.out.println("unwrapped item delta = " + id2.debugDump());
//
//            task.modifyExtension(id2);
//        }

        task.savePendingModifications(result);
        System.out.println("Task = " + task.debugDump());

        PrismObject<UserType> owner2 = repositoryService.getObject(UserType.class, TASK_OWNER2_OID, null, result);

        task.setBindingImmediate(TaskBinding.LOOSE, result);

        // other properties will be set in batched mode
        String newname = "Test task, name changed";
        task.setName(PrismTestUtil.createPolyStringType(newname));
        task.setProgress(10);
        long currentTime = System.currentTimeMillis();
        long currentTime1 = currentTime + 10000;
        long currentTime2 = currentTime + 25000;
        task.setLastRunStartTimestamp(currentTime);
        task.setLastRunFinishTimestamp(currentTime1);
        task.setExecutionStatus(TaskExecutionStatus.SUSPENDED);
        task.setHandlerUri("http://no-handler.org/");
        //task.setOwner(owner2);

        ScheduleType st0 = task.getSchedule();

        ScheduleType st1 = new ScheduleType();
        st1.setInterval(1);
        st1.setMisfireAction(MisfireActionType.RESCHEDULE);
        task.pushHandlerUri("http://no-handler.org/1", st1, TaskBinding.TIGHT, ((TaskQuartzImpl) task).createExtensionDelta(delayDefinition, 1));

        ScheduleType st2 = new ScheduleType();
        st2.setInterval(2);
        st2.setMisfireAction(MisfireActionType.EXECUTE_IMMEDIATELY);
        task.pushHandlerUri("http://no-handler.org/2", st2, TaskBinding.LOOSE, ((TaskQuartzImpl) task).createExtensionDelta(delayDefinition, 2));

        task.setRecurrenceStatus(TaskRecurrence.RECURRING);

        OperationResultType ort = result.createOperationResultType();			// to be compared with later

        task.setResult(result);

        //PrismObject<UserType> requestee = task.getOwner();
        //task.setRequesteeRef(requestee);      does not work
        //task.setRequesteeOid(requestee.getOid());

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(UserType.COMPLEX_TYPE);
        String objectOid = "some-oid...";
        objectReferenceType.setOid(objectOid);
        task.setObjectRef(objectReferenceType);

        logger.trace("Saving modifications...");

        task.savePendingModifications(result);

        logger.trace("Retrieving the task (second time) and comparing its properties...");

        Task task001 = taskManager.getTask(taskOid(test), result);
        logger.trace("Task from repo: " + task001.debugDump());
        AssertJUnit.assertEquals(TaskBinding.LOOSE, task001.getBinding());
        PrismAsserts.assertEqualsPolyString("Name not", newname, task001.getName());
//        AssertJUnit.assertEquals(newname, task001.getName());
        AssertJUnit.assertTrue(10 == task001.getProgress());
        AssertJUnit.assertNotNull(task001.getLastRunStartTimestamp());
        AssertJUnit.assertEquals("Start time is not correct", (Long) (currentTime / 1000L), (Long) (task001.getLastRunStartTimestamp() / 1000L));   // e.g. MySQL cuts off millisecond information
        AssertJUnit.assertNotNull(task001.getLastRunFinishTimestamp());
        AssertJUnit.assertEquals("Finish time is not correct", (Long) (currentTime1 / 1000L), (Long) (task001.getLastRunFinishTimestamp() / 1000L));
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task001.getExclusivityStatus());
        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task001.getExecutionStatus());
        AssertJUnit.assertEquals("Handler after 2xPUSH is not OK", "http://no-handler.org/2", task001.getHandlerUri());
        AssertJUnit.assertEquals("Schedule after 2xPUSH is not OK", st2, task001.getSchedule());
        AssertJUnit.assertEquals("Number of handlers is not OK", 3, task.getHandlersCount());
        UriStack us = task.getOtherHandlersUriStack();
        AssertJUnit.assertEquals("First handler from the handler stack does not match", "http://no-handler.org/", us.getUriStackEntry().get(0).getHandlerUri());
        AssertJUnit.assertEquals("First schedule from the handler stack does not match", st0, us.getUriStackEntry().get(0).getSchedule());
        AssertJUnit.assertEquals("Second handler from the handler stack does not match", "http://no-handler.org/1", us.getUriStackEntry().get(1).getHandlerUri());
        AssertJUnit.assertEquals("Second schedule from the handler stack does not match", st1, us.getUriStackEntry().get(1).getSchedule());
        AssertJUnit.assertTrue(task001.isCycle());
        OperationResult r001 = task001.getResult();
        AssertJUnit.assertNotNull(r001);
        //AssertJUnit.assertEquals("Owner OID is not correct", TASK_OWNER2_OID, task001.getOwner().getOid());

        PrismProperty<?> d = task001.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME);
        AssertJUnit.assertNotNull("delay extension property was not found", d);
        AssertJUnit.assertEquals("delay extension property has wrong value", (Integer) 100, d.getRealValue(Integer.class));

        OperationResultType ort1 = r001.createOperationResultType();

        // handling of operation result in tasks is extremely fragile now...
        // in case of problems, just uncomment the following line ;)
        AssertJUnit.assertEquals(ort, ort1);

        //AssertJUnit.assertEquals("RequesteeRef was not stored/retrieved correctly", requestee.getOid(), task001.getRequesteeRef().getOid());
        //AssertJUnit.assertEquals("RequesteeOid was not stored/retrieved correctly", requestee.getOid(), task001.getRequesteeOid());

        AssertJUnit.assertEquals("ObjectRef OID was not stored/retrieved correctly", objectReferenceType.getOid(), task001.getObjectRef().getOid());
        AssertJUnit.assertEquals("ObjectRef ObjectType was not stored/retrieved correctly", objectReferenceType.getType(), task001.getObjectRef().getType());

        // now pop the handlers

        ((TaskQuartzImpl) task001).finishHandler(result);
        task001.refresh(result);
        AssertJUnit.assertEquals("Handler URI after first POP is not correct", "http://no-handler.org/1", task001.getHandlerUri());
        AssertJUnit.assertEquals("Schedule after first POP is not correct", st1, task001.getSchedule());
        AssertJUnit.assertEquals("Binding after first POP is not correct", TaskBinding.TIGHT, task001.getBinding());
        AssertJUnit.assertNotSame("Task state after first POP should not be CLOSED", TaskExecutionStatus.CLOSED, task001.getExecutionStatus());
        AssertJUnit.assertEquals("Extension element value is not correct after first POP", (Integer) 2, task001.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME).getRealValue(Integer.class));

        ((TaskQuartzImpl) task001).finishHandler(result);
        task001.refresh(result);
        AssertJUnit.assertEquals("Handler URI after second POP is not correct", "http://no-handler.org/", task001.getHandlerUri());
        AssertJUnit.assertEquals("Schedule after second POP is not correct", st0, task001.getSchedule());
        AssertJUnit.assertEquals("Binding after second POP is not correct", TaskBinding.LOOSE, task001.getBinding());
        AssertJUnit.assertNotSame("Task state after second POP should not be CLOSED", TaskExecutionStatus.CLOSED, task001.getExecutionStatus());
        AssertJUnit.assertEquals("Extension element value is not correct after second POP", (Integer) 1, task001.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME).getRealValue(Integer.class));

        ((TaskQuartzImpl) task001).finishHandler(result);
        task001.refresh(result);
        //AssertJUnit.assertNull("Handler URI after third POP is not null", task001.getHandlerUri());
        AssertJUnit.assertEquals("Handler URI after third POP is not correct", "http://no-handler.org/", task001.getHandlerUri());
        AssertJUnit.assertEquals("Task state after third POP is not CLOSED", TaskExecutionStatus.CLOSED, task001.getExecutionStatus());

    }


    /*
     * Execute a single-run task.
     */

    @Test(enabled = true)
    public void test005Single() throws Exception {

    	final String test = "005Single";
        final OperationResult result = createResult(test);

        // reset 'has run' flag on the handler
        singleHandler1.resetHasRun();

        // Add single task. This will get picked by task scanner and executed
        addObjectFromFile(taskFilename(test));

        logger.trace("Retrieving the task...");
        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

       	AssertJUnit.assertNotNull(task);
       	logger.trace("Task retrieval OK.");

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 1000);

        logger.info("... done");

        // Check task status

        Task task1 = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task1);
        System.out.println("getTask returned: " + task1.debugDump());

        PrismObject<TaskType> po = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println("getObject returned: " + po.debugDump());

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task1.getExecutionStatus());

        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task1.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task1.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task1.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("LastRunFinishTimestamp is null", task1.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task1.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue("Task reported no progress", task1.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task1.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Test for no presence of handlers
        //AssertJUnit.assertNull("Handler is still present", task1.getHandlerUri());
        AssertJUnit.assertNotNull("Handler is gone", task1.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
        		task1.getOtherHandlersUriStack() == null || task1.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test whether handler has really run
        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
    }

    /*
     * Executes a cyclic task
     */

    @Test(enabled = true)
    public void test006Cycle() throws Exception {
    	final String test = "006Cycle";
        final OperationResult result = createResult(test);

        // But before that check sanity ... a known problem with xsi:type
    	PrismObject<? extends ObjectType> object = addObjectFromFile(taskFilename(test));

        ObjectType objectType = object.asObjectable();
        TaskType addedTask = (TaskType) objectType;
        System.out.println("Added task");
        System.out.println(object.debugDump());

        PrismContainer<?> extensionContainer = object.getExtension();
        PrismProperty<Object> deadProperty = extensionContainer.findProperty(new QName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (add result)", DOMUtil.XSD_INT, deadProperty.getDefinition().getTypeName());

        // Read from repo

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
        TaskType repoTaskType = repoTask.asObjectable();

        extensionContainer = repoTask.getExtension();
        deadProperty = extensionContainer.findProperty(new QName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (from repo)", DOMUtil.XSD_INT, deadProperty.getDefinition().getTypeName());

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task
        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() > 0;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(t.debugDump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and claimed
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("LastRunFinishTimestamp is null", task.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more at least 1 - so small because of lazy testing machine ... (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue("Task progress is too small (should be at least 1)", task.getProgress() >= 1);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);

    }

    /*
     * Single-run task with more handlers.
     */

    @Test(enabled = true)
    public void test008MoreHandlers() throws Exception {

    	final String test = "008MoreHandlers";
        final OperationResult result = createResult(test);

        // reset 'has run' flag on handlers
        singleHandler1.resetHasRun();
        singleHandler2.resetHasRun();
        singleHandler3.resetHasRun();

        addObjectFromFile(taskFilename(test));

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue("Task reported no progress", task.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
        		task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if all three handlers were run

        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
        AssertJUnit.assertTrue("Handler2 has not run", singleHandler2.hasRun());
        AssertJUnit.assertTrue("Handler3 has not run", singleHandler3.hasRun());
    }

    @Test(enabled = true)
    public void test009CycleLoose() throws Exception {
    	final String test = "009CycleLoose";
        final OperationResult result = createResult(test);

    	PrismObject<? extends ObjectType> object = addObjectFromFile(taskFilename(test));

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this task

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() >= 1;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(t.debugDump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more at least 1 - lazy neptunus... (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue("Progress is none or too small", task.getProgress() >= 1);

        // The progress should not be too big (indicates fault in scheduling)
        AssertJUnit.assertTrue("Progress is too big (fault in scheduling?)", task.getProgress() <= 7);

        // Test for presence of a result. It should be there and it should
        // indicate success or in-progress
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' or 'in-progress' status: it is " + taskResult.getStatus(),
                taskResult.isSuccess() || taskResult.isInProgress());

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);
    }

    @Test(enabled = true)
    public void test010CycleCronLoose() throws Exception {

    	final String test = "010CycleCronLoose";
    	final OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() >= 2;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        TaskType t = repositoryService.getObject(TaskType.class, taskOid(test), null, result).getValue().getValue();
        System.out.println(ObjectTypeUtil.dump(t));

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be at least 2 as the task has run at least twice
        AssertJUnit.assertTrue("Task has not been executed at least twice", task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);
    }

    @Test(enabled = true)
    public void test011MoreHandlersAndSchedules() throws Exception {

        final String test = "011MoreHandlersAndSchedules";
        final OperationResult result = createResult(test);

        // reset 'has run' flag on handlers
        l1Handler.resetHasRun();
        l2Handler.resetHasRun();
        l3Handler.resetHasRun();

        addObjectFromFile(taskFilename(test));

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }

            @Override
            public void timeout() {
            }
        }, 30000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp().longValue() == 0);

        /*
         * Here the execution should be as follows:
         *   progress: 0->1 on first execution of L1 handler
         *   progress: 1->2 on first execution of L2 handler (ASAP after finishing L1)
         *   progress: 2->3 on second execution of L2 handler (2 seconds later)
         *   progress: 3->4 on third execution of L2 handler (2 seconds later)
         *   progress: 4->5 on fourth execution of L2 handler (2 seconds later)
         *   progress: 5->6 on first (and therefore last) execution of L3 handler
         *   progress: 6->7 on last execution of L2 handler (2 seconds later, perhaps)
         *   progress: 7->8 on last execution of L1 handler
         */

        AssertJUnit.assertEquals("Task reported wrong progress", 8, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if all three handlers were run

        AssertJUnit.assertTrue("L1 handler has not run", l1Handler.hasRun());
        AssertJUnit.assertTrue("L2 handler has not run", l2Handler.hasRun());
        AssertJUnit.assertTrue("L3 handler has not run", l3Handler.hasRun());
    }

    /*
    * Suspends a running task.
    */

    @Test(enabled = true)
    public void test012Suspend() throws Exception {

    	final String test = "012Suspend";
        final OperationResult result = createResult(test);

      	addObjectFromFile(taskFilename(test));

        // check if we can read the extension (xsi:type issue)

        Task taskTemp = taskManager.getTask(taskOid(test), result);
        PrismProperty delay = taskTemp.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME);
        AssertJUnit.assertEquals("Delay was not read correctly", 2000, delay.getRealValue());

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() >= 1;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

        // Check task status (task is running 5 iterations where each takes 2000 ms)

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        AssertJUnit.assertEquals("Task is not running", TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // Now suspend the task

        boolean stopped = taskManager.suspendTask(task, 0, result);

        task.refresh(result);
        System.out.println("After suspend and refresh: " + task.debugDump());

        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        AssertJUnit.assertNotNull("Task last start time is null", task.getLastRunStartTimestamp());
        assertFalse("Task last start time is 0", task.getLastRunStartTimestamp().longValue() == 0);

        // The progress should be more than 0
        AssertJUnit.assertTrue("Task has not reported any progress", task.getProgress() > 0);

//        Thread.sleep(200);		// give the scheduler a chance to release the task

//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    @Test(enabled = true)
    public void test013ReleaseAndSuspendLooselyBound() throws Exception {

    	final String test = "013ReleaseAndSuspendLooselyBound";
        final OperationResult result = createResult(test);

    	addObjectFromFile(taskFilename(test));

        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.debugDump());

        // check if we can read the extension (xsi:type issue)

        PrismProperty delay = task.getExtensionProperty(SchemaConstants.NOOP_DELAY_QNAME);
        AssertJUnit.assertEquals("Delay was not read correctly", 1000, delay.getRealValue());

        // let us resume (i.e. start the task)
        taskManager.resumeTask(task, result);

        // task is executing for 1000 ms, so we need to wait slightly longer, in order for the execution to be done
        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() >= 1;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

        task.refresh(result);

        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());		// task cycle is 1000 ms, so it should be released now

        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);

        // now let us suspend it (occurs during wait cycle, so we can put short timeout here)

        boolean stopped = taskManager.suspendTask(task, 300, result);

        task.refresh(result);

        AssertJUnit.assertTrue("Task is not stopped", stopped);

        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);

//        Thread.sleep(200);		// give the scheduler a chance to release the task
//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

    }

    @Test(enabled = true)
    public void test014SuspendLongRunning() throws Exception {

    	final String test = "014SuspendLongRunning";
    	final OperationResult result = createResult(test);

    	addObjectFromFile(taskFilename(test));

        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.debugDump());

        waitFor("Waiting for task manager to start the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to start the task", task);
                return task.getLastRunStartTimestamp() != null;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

        task.refresh(result);

        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);

        // now let us suspend it, without long waiting

        boolean stopped = taskManager.suspendTask(task, 1000, result);

        task.refresh(result);

        assertFalse("Task is stopped (it should be running for now)", stopped);

        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
//        AssertJUnit.assertEquals("Task should be still claimed, as it is not definitely stopped", TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertEquals("There should be no progress reported", 0, task.getProgress());

        // now let us wait for the finish

        stopped = taskManager.suspendTask(task, 0, result);

        task.refresh(result);

        AssertJUnit.assertTrue("Task is not stopped", stopped);

        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("Last run finish time is null", task.getLastRunStartTimestamp());
        assertFalse("Last run finish time is zero", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertTrue("Progress is not reported", task.getProgress() > 0);

//        Thread.sleep(200);		// give the scheduler a chance to release the task
//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    @Test(enabled = true)
    public void test015DeleteTaskFromRepo() throws Exception {
        final String test = "015DeleteTaskFromRepo";
        final OperationResult result = createResult(test);

        PrismObject<? extends ObjectType> object = addObjectFromFile(taskFilename(test));
        String oid = taskOid(test);

        // is the task in Quartz?

        final JobKey key = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);
        AssertJUnit.assertTrue("Job in Quartz does not exist", taskManager.getExecutionManager().getQuartzScheduler().checkExists(key));

        // Remove task from repo

        repositoryService.deleteObject(TaskType.class, taskOid(test), result);

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this task

        waitFor("Waiting for the job to disappear from Quartz Job Store", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                try {
                    return !taskManager.getExecutionManager().getQuartzScheduler().checkExists(key);
                } catch (SchedulerException e) {
                    throw new SystemException(e);
                }
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

    }

    @Test(enabled = true)
    public void test016WaitForSubtasks() throws Exception {
        final String test = "016WaitForSubtasks";
        final OperationResult result = createResult(test);

        //taskManager.getClusterManager().startClusterManagerThread();

        try {

            Task rootTask = taskManager.createTaskInstance((PrismObject<TaskType>) (PrismObject) addObjectFromFile(taskFilename(test)), result);
            Task firstChildTask = taskManager.createTaskInstance((PrismObject<TaskType>) (PrismObject) addObjectFromFile(taskFilename(test + "-child-1")), result);

            Task firstReloaded = taskManager.getTaskByIdentifier(firstChildTask.getTaskIdentifier(), result);
            assertEquals("Didn't get correct task by identifier", firstChildTask.getOid(), firstReloaded.getOid());

            Task secondChildTask = rootTask.createSubtask();
            secondChildTask.setName("Second child");
            secondChildTask.setOwner(rootTask.getOwner());
            secondChildTask.pushHandlerUri(SINGLE_TASK_HANDLER_URI, new ScheduleType(), null);
            secondChildTask.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);           // will resume it after root starts waiting for tasks
            taskManager.switchToBackground(secondChildTask, result);

            Task firstPrerequisiteTask = taskManager.createTaskInstance((PrismObject<TaskType>) (PrismObject) addObjectFromFile(taskFilename(test + "-prerequisite-1")), result);

            List<Task> prerequisities = rootTask.listPrerequisiteTasks(result);
            assertEquals("Wrong # of prerequisite tasks", 1, prerequisities.size());
            assertEquals("Wrong OID of prerequisite task", firstPrerequisiteTask.getOid(), prerequisities.get(0).getOid());

            Task secondPrerequisiteTask = taskManager.createTaskInstance();
            secondPrerequisiteTask.setName("Second prerequisite");
            secondPrerequisiteTask.setOwner(rootTask.getOwner());
            secondPrerequisiteTask.addDependent(rootTask.getTaskIdentifier());
            secondPrerequisiteTask.pushHandlerUri(NoOpTaskHandler.HANDLER_URI, new ScheduleType(), null);
            secondPrerequisiteTask.setExtensionPropertyValue(SchemaConstants.NOOP_DELAY_QNAME, 1500);
            secondPrerequisiteTask.setExtensionPropertyValue(SchemaConstants.NOOP_STEPS_QNAME, 1);
            secondPrerequisiteTask.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);           // will resume it after root starts waiting for tasks
            secondPrerequisiteTask.addDependent(rootTask.getTaskIdentifier());
            taskManager.switchToBackground(secondPrerequisiteTask, result);

            LOGGER.info("Starting waiting for child/prerequisite tasks");
            rootTask.startWaitingForTasksImmediate(result);

            firstChildTask.refresh(result);
            assertEquals("Parent is not set correctly on 1st child task", rootTask.getTaskIdentifier(), firstChildTask.getParent());
            secondChildTask.refresh(result);
            assertEquals("Parent is not set correctly on 2nd child task", rootTask.getTaskIdentifier(), secondChildTask.getParent());
            firstPrerequisiteTask.refresh(result);
            assertEquals("Dependents are not set correctly on 1st prerequisite task (count differs)", 1, firstPrerequisiteTask.getDependents().size());
            assertEquals("Dependents are not set correctly on 1st prerequisite task (value differs)", rootTask.getTaskIdentifier(), firstPrerequisiteTask.getDependents().get(0));
            List<Task> deps = firstPrerequisiteTask.listDependents(result);
            assertEquals("Dependents are not set correctly on 1st prerequisite task - listDependents - (count differs)", 1, deps.size());
            assertEquals("Dependents are not set correctly on 1st prerequisite task - listDependents - (value differs)", rootTask.getOid(), deps.get(0).getOid());
            secondPrerequisiteTask.refresh(result);
            assertEquals("Dependents are not set correctly on 2nd prerequisite task (count differs)", 1, secondPrerequisiteTask.getDependents().size());
            assertEquals("Dependents are not set correctly on 2nd prerequisite task (value differs)", rootTask.getTaskIdentifier(), secondPrerequisiteTask.getDependents().get(0));
            deps = secondPrerequisiteTask.listDependents(result);
            assertEquals("Dependents are not set correctly on 2nd prerequisite task - listDependents - (count differs)", 1, deps.size());
            assertEquals("Dependents are not set correctly on 2nd prerequisite task - listDependents - (value differs)", rootTask.getOid(), deps.get(0).getOid());

            LOGGER.info("Resuming suspended child/prerequisite tasks");
            taskManager.resumeTask(secondChildTask, result);
            taskManager.resumeTask(secondPrerequisiteTask, result);

            final String rootOid = taskOid(test);

            waitFor("Waiting for task manager to execute the task", new Checker() {
                public boolean check() throws ObjectNotFoundException, SchemaException {
                    Task task = taskManager.getTask(rootOid, result);
                    IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                    return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
                }

                @Override
                public void timeout() {
                }
            }, 60000, 3000);

            firstChildTask.refresh(result);
            secondChildTask.refresh(result);
            firstPrerequisiteTask.refresh(result);
            secondPrerequisiteTask.refresh(result);

            assertEquals("1st child task should be closed", TaskExecutionStatus.CLOSED, firstChildTask.getExecutionStatus());
            assertEquals("2nd child task should be closed", TaskExecutionStatus.CLOSED, secondChildTask.getExecutionStatus());
            assertEquals("1st prerequisite task should be closed", TaskExecutionStatus.CLOSED, firstPrerequisiteTask.getExecutionStatus());
            assertEquals("2nd prerequisite task should be closed", TaskExecutionStatus.CLOSED, secondPrerequisiteTask.getExecutionStatus());

        } finally {
//            taskManager.getClusterManager().stopClusterManagerThread(10000L, result);
        }
    }

    @Test(enabled = true)
    public void test017WaitForSubtasksEmpty() throws Exception {
        final String test = "017WaitForSubtasksEmpty";
        final OperationResult result = createResult(test);

        taskManager.getClusterManager().startClusterManagerThread();

        try {
            Task rootTask = taskManager.createTaskInstance((PrismObject<TaskType>) (PrismObject) addObjectFromFile(taskFilename(test)), result);

            display("root task", rootTask);

            final String rootOid = taskOid(test);

            waitFor("Waiting for task manager to execute the task", new Checker() {
                public boolean check() throws ObjectNotFoundException, SchemaException {
                    Task task = taskManager.getTask(rootOid, result);
                    IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                    return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
                }

                @Override
                public void timeout() {
                }
            }, 40000, 3000);
        } finally {
            taskManager.getClusterManager().stopClusterManagerThread(10000L, result);
        }
    }

    @Test(enabled = true)
    public void test018TaskResult() throws Exception {
        final String test = "018RefreshingResult";
        final OperationResult result = createResult(test);

        Task task = taskManager.createTaskInstance();
        task.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);
        PrismObject<UserType> owner2 = repositoryService.getObject(UserType.class, TASK_OWNER2_OID, null, result);
        task.setOwner(owner2);
        AssertJUnit.assertEquals("Task result for new task is not correct", OperationResultStatus.UNKNOWN, task.getResult().getStatus());

        taskManager.switchToBackground(task, result);
        AssertJUnit.assertEquals("Background task result is not correct (in memory)", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
        PrismObject<TaskType> task1 = repositoryService.getObject(TaskType.class, task.getOid(), null, result);
        AssertJUnit.assertEquals("Background task result is not correct (in repo)", OperationResultStatusType.IN_PROGRESS, task1.asObjectable().getResult().getStatus());

        // now change task's result and check the refresh() method w.r.t. result handling
        task.getResult().recordFatalError("");
        AssertJUnit.assertEquals(OperationResultStatus.FATAL_ERROR, task.getResult().getStatus());
        task.refresh(result);
        AssertJUnit.assertEquals("Refresh does not update task's result", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
    }

    /*
     * Recurring task returning FINISHED_HANDLER code.
     */

    @Test(enabled = true)
    public void test019FinishedHandler() throws Exception {

        final String test = "019FinishedHandler";
        final OperationResult result = createResult(test);

        // reset 'has run' flag on handlers
        singleHandler1.resetHasRun();

        addObjectFromFile(taskFilename(test));

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 2000);

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be at least 2 as the task has run at least twice (once in each handler)
        AssertJUnit.assertTrue("Task reported progress lower than 2", task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status", taskResult.isSuccess());

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if "outer" handler has run as well

        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
    }

    @Test
    public void test020QueryByExecutionStatus() throws Exception {
        final String test = "020QueryByExecutionStatus";
        final OperationResult result = createResult(test);

        taskManager.getClusterManager().startClusterManagerThread();

        Task rootTask = taskManager.createTaskInstance((PrismObject<TaskType>) (PrismObject) addObjectFromFile(taskFilename(test)), result);
        String oid = rootTask.getOid();

        ObjectFilter filter1 = QueryBuilder.queryFor(TaskType.class, prismContext).item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING).buildFilter();
        ObjectFilter filter2 = QueryBuilder.queryFor(TaskType.class, prismContext).item(TaskType.F_WAITING_REASON).eq(TaskWaitingReasonType.WORKFLOW).buildFilter();
        ObjectFilter filter3 = AndFilter.createAnd(filter1, filter2);

        List<PrismObject<TaskType>> prisms1 = repositoryService.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(filter1), null, result);
        List<PrismObject<TaskType>> prisms2 = repositoryService.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(filter2), null, result);
        List<PrismObject<TaskType>> prisms3 = repositoryService.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(filter3), null, result);

        assertFalse("There were no tasks with executionStatus == WAITING found", prisms1.isEmpty());
        assertFalse("There were no tasks with waitingReason == WORKFLOW found", prisms2.isEmpty());
        assertFalse("There were no tasks with executionStatus == WAITING and waitingReason == WORKFLOW found", prisms3.isEmpty());
    }

    @Test(enabled = true)
    public void test021DeleteTaskTree() throws Exception {
        final String test = "021DeleteTaskTree";
        final OperationResult result = createResult(test);

        PrismObject<TaskType> parentTaskPrism = addObjectFromFile(taskFilename(test));
        PrismObject<TaskType> childTask1Prism = addObjectFromFile(taskFilename(test+"-child1"));
        PrismObject<TaskType> childTask2Prism = addObjectFromFile(taskFilename(test+"-child2"));

        AssertJUnit.assertEquals(TaskExecutionStatusType.WAITING, parentTaskPrism.asObjectable().getExecutionStatus());
        AssertJUnit.assertEquals(TaskExecutionStatusType.SUSPENDED, childTask1Prism.asObjectable().getExecutionStatus());
        AssertJUnit.assertEquals(TaskExecutionStatusType.SUSPENDED, childTask2Prism.asObjectable().getExecutionStatus());

        Task parentTask = taskManager.createTaskInstance(parentTaskPrism, result);
        Task childTask1 = taskManager.createTaskInstance(childTask1Prism, result);
        Task childTask2 = taskManager.createTaskInstance(childTask2Prism, result);

        IntegrationTestTools.display("parent", parentTask);
        IntegrationTestTools.display("child1", childTask1);
        IntegrationTestTools.display("child2", childTask2);

        taskManager.resumeTask(childTask1, result);
        taskManager.resumeTask(childTask2, result);
        parentTask.startWaitingForTasksImmediate(result);

        LOGGER.info("Deleting task {} and its subtasks", parentTask);

        taskManager.suspendAndDeleteTasks(Arrays.asList(parentTask.getOid()), 2000L, true, result);

        IntegrationTestTools.display("after suspendAndDeleteTasks", result.getLastSubresult());
        TestUtil.assertSuccessOrWarning("suspendAndDeleteTasks result is not success/warning", result.getLastSubresult());

        try {
            repositoryService.getObject(TaskType.class, childTask1.getOid(), null, result);
            assertTrue("Task " + childTask1 + " was not deleted from the repository", false);
        } catch (ObjectNotFoundException e) {
            // ok!
        }

        try {
            repositoryService.getObject(TaskType.class, childTask2.getOid(), null, result);
            assertTrue("Task " + childTask2 + " was not deleted from the repository", false);
        } catch (ObjectNotFoundException e) {
            // ok!
        }

        try {
            repositoryService.getObject(TaskType.class, parentTask.getOid(), null, result);
            assertTrue("Task " + parentTask + " was not deleted from the repository", false);
        } catch (ObjectNotFoundException e) {
            // ok!
        }

    }

    @Test(enabled = true)
    public void test022ExecuteRecurringOnDemand() throws Exception {

        final String test = "022ExecuteRecurringOnDemand";
        final OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.debugDump());

        System.out.println("Waiting to see if the task would not start...");
        Thread.sleep(5000L);

        // check the task HAS NOT started
        task.refresh(result);
        System.out.println("After initial wait: " + task.debugDump());

        assertEquals("task is not RUNNABLE", TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
        assertNull("task was started", task.getLastRunStartTimestamp());
        assertEquals("task was achieved some progress", 0L, task.getProgress());

        // now let's start the task
        taskManager.scheduleRunnableTaskNow(task, result);

        // task is executing for 1000 ms, so we need to wait slightly longer, in order for the execution to be done
        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getProgress() >= 1;
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);

        task.refresh(result);
        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
        AssertJUnit.assertTrue("no progress", task.getProgress() > 0);

        // now let us suspend it (occurs during wait cycle, so we can put short timeout here)

        boolean stopped = taskManager.suspendTask(task, 10000L, result);
        task.refresh(result);
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
    }

    @Test(enabled = true)
    public void test100LightweightSubtasks() throws Exception {

        final String test = "100LightweightSubtasks";
        final OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.debugDump());

        waitFor("Waiting for task manager to execute the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 500);

        task.refresh(result);
        System.out.println("After refresh (task was executed): " + task.debugDump());

        Collection<? extends Task> subtasks = parallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (Task subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStatus.CLOSED, subtask.getExecutionStatus());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run", handler.hasRun());
            assertTrue("Handler has not exited", handler.hasExited());
        }
    }

    @Test(enabled = true)
    public void test105LightweightSubtasksSuspension() throws Exception {

        final String test = "105LightweightSubtasksSuspension";
        final OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));

        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.debugDump());

        waitFor("Waiting for task manager to start the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(taskOid(test), result);
                IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
                return task.getLastRunStartTimestamp() != null && task.getLastRunStartTimestamp() != 0L;
            }

            @Override
            public void timeout() {
            }
        }, 15000, 500);

        task.refresh(result);
        System.out.println("After refresh (task was started; and it should run now): " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // check the thread
        List<JobExecutionContext> jobExecutionContexts = taskManager.getExecutionManager().getQuartzScheduler().getCurrentlyExecutingJobs();
        JobExecutionContext found = null;
        for (JobExecutionContext jobExecutionContext : jobExecutionContexts) {
            if (task.getOid().equals(jobExecutionContext.getJobDetail().getKey().getName())) {
                found = jobExecutionContext; break;
            }
        }
        assertNotNull("Job for the task was not found", found);
        JobExecutor executor = (JobExecutor) found.getJobInstance();
        assertNotNull("No job executor", executor);
        Thread thread = executor.getExecutingThread();
        assertNotNull("No executing thread", thread);

        // now let us suspend it - the handler should stop, as well as the subtasks

        boolean stopped = taskManager.suspendTask(task, 10000L, result);
        task.refresh(result);
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        Collection<? extends Task> subtasks = parallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (Task subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStatus.CLOSED, subtask.getExecutionStatus());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run", handler.hasRun());
            assertTrue("Handler has not exited", handler.hasExited());
        }
    }

    @Test
    public void test108SecondaryGroupLimit() throws Exception {

        final String TEST_NAME = "108SecondaryGroupLimit";
        final OperationResult result = createResult(TEST_NAME);

        TaskType task1 = (TaskType) addObjectFromFile(taskFilename(TEST_NAME)).asObjectable();
        waitForTaskStart(task1.getOid(), result);

        // import second task with the same group (expensive)
		TaskType task2 = (TaskType) addObjectFromFile(taskFilename(TEST_NAME + "-2")).asObjectable();

		Thread.sleep(10000);
        task1 = getTaskType(task1.getOid(), result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

		task2 = getTaskType(task2.getOid(), result);
		assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
		assertNotNull("Next retry time is not set for second task", task2.getNextRetryTimestamp());

		// now finish first task and check the second one is started
		boolean stopped = taskManager.suspendTasks(Collections.singleton(task1.getOid()), 20000L, result);
		assertTrue("Task 1 was not suspended successfully", stopped);

		waitForTaskStart(task2.getOid(), result);

		// import third task that has another collision (large-ram) with the second one
        TaskType task3 = (TaskType) addObjectFromFile(taskFilename(TEST_NAME + "-3")).asObjectable();

        Thread.sleep(10000);
        task2 = getTaskType(task2.getOid(), result);
        assertNull("Second task should have no retry time", task2.getNextRetryTimestamp());

        task3 = getTaskType(task3.getOid(), result);
        assertNull("Third task was started even if it should not be", task3.getLastRunStartTimestamp());
        assertNotNull("Next retry time is not set for third task", task3.getNextRetryTimestamp());

        // now finish second task and check the third one is started
        stopped = taskManager.suspendTasks(Collections.singleton(task2.getOid()), 20000L, result);
        assertTrue("Task 2 was not suspended successfully", stopped);

        waitForTaskStart(task3.getOid(), result);

		taskManager.suspendTasks(Collections.singleton(task3.getOid()), 20000L, result);
    }

    @Test
    public void test110GroupLimit() throws Exception {

        final String TEST_NAME = "110GroupLimit";
        final OperationResult result = createResult(TEST_NAME);

        taskManager.getExecutionManager().setLocalExecutionLimitations((TaskExecutionLimitationsType) null);

        TaskType task1 = (TaskType) addObjectFromFile(taskFilename(TEST_NAME)).asObjectable();
        waitForTaskStart(task1.getOid(), result);

        // import second task with the same group
		TaskType task2 = (TaskType) addObjectFromFile(taskFilename(TEST_NAME + "-2")).asObjectable();

		Thread.sleep(10000);
        task1 = getTaskType(task1.getOid(), result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

		task2 = getTaskType(task2.getOid(), result);
		assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
		assertNotNull("Next retry time is not set for second task", task2.getNextRetryTimestamp());

		// now finish first task and check the second one is started
		boolean stopped = taskManager.suspendTasks(Collections.singleton(task1.getOid()), 20000L, result);
		assertTrue("Task 1 was not suspended successfully", stopped);

		waitForTaskStart(task2.getOid(), result);
		taskManager.suspendTasks(Collections.singleton(task2.getOid()), 20000L, result);
    }

    private TaskType getTaskType(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.retrieveItemsNamed(
                TaskType.F_SUBTASK,
                TaskType.F_NODE_AS_OBSERVED,
                TaskType.F_NEXT_RUN_START_TIMESTAMP,
                TaskType.F_NEXT_RETRY_TIMESTAMP);
        return taskManager.getObject(TaskType.class, oid, options, result).asObjectable();
    }

    @Test
    public void test120NodeAllowed() throws Exception {
        final String TEST_NAME = "120NodeAllowed";
        final OperationResult result = createResult(TEST_NAME);

        taskManager.getExecutionManager().setLocalExecutionLimitations(
                new TaskExecutionLimitationsType()
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName("lightweight-tasks"))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(null))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(TaskConstants.LIMIT_FOR_OTHER_GROUPS).limit(0)));

        TaskType task = (TaskType) addObjectFromFile(taskFilename(TEST_NAME)).asObjectable();
        waitForTaskStart(task.getOid(), result);
        task = getTaskType(task.getOid(), result);
        assertNotNull("Task was not started even if it should be", task.getLastRunStartTimestamp());
    }

	@Test
	public void test130NodeNotAllowed() throws Exception {
		final String TEST_NAME = "130NodeNotAllowed";
		final OperationResult result = createResult(TEST_NAME);

		TaskType task = (TaskType) addObjectFromFile(taskFilename(TEST_NAME)).asObjectable();
		Thread.sleep(10000);
		task = getTaskType(task.getOid(), result);
		assertNull("Task was started even if it shouldn't be", task.getLastRunStartTimestamp());
		taskManager.suspendTasks(Collections.singleton(task.getOid()), 1000L, result);
	}

	@Test(enabled = true)
    public void test999CheckingLeftovers() throws Exception {

        String test = "999CheckingLeftovers";
        OperationResult result = createResult(test);

        ArrayList<String> leftovers = new ArrayList<String>();
        checkLeftover(leftovers, "005", result);
        checkLeftover(leftovers, "006", result);
        checkLeftover(leftovers, "008", result);
        checkLeftover(leftovers, "009", result);
        checkLeftover(leftovers, "010", result);
        checkLeftover(leftovers, "011", result);
        checkLeftover(leftovers, "012", result);
        checkLeftover(leftovers, "013", result);
        checkLeftover(leftovers, "014", result);
        checkLeftover(leftovers, "015", result);
        checkLeftover(leftovers, "016", result);
        checkLeftover(leftovers, "017", result);
        checkLeftover(leftovers, "019", result);
        checkLeftover(leftovers, "021", result);
        checkLeftover(leftovers, "021", "1", result);
        checkLeftover(leftovers, "021", "2", result);
        checkLeftover(leftovers, "022", result);
        checkLeftover(leftovers, "100", result);
        checkLeftover(leftovers, "105", result);
        checkLeftover(leftovers, "108", result);
        checkLeftover(leftovers, "108", "a", result);
        checkLeftover(leftovers, "108", "b", result);
        checkLeftover(leftovers, "110", result);
        checkLeftover(leftovers, "110", "a", result);
		checkLeftover(leftovers, "120", result);
		checkLeftover(leftovers, "130", result);

        String message = "Leftover task(s) found:";
        for (String leftover : leftovers) {
            message += " " + leftover;
        }

        AssertJUnit.assertTrue(message, leftovers.isEmpty());
    }

    private void checkLeftover(ArrayList<String> leftovers, String testNumber, OperationResult result) throws Exception {
        checkLeftover(leftovers, testNumber, "0", result);
    }

    private void checkLeftover(ArrayList<String> leftovers, String testNumber, String subId, OperationResult result) throws Exception {
        String oid = taskOid(testNumber, subId);
        Task t;
        try {
            t = taskManager.getTask(oid, result);
        } catch (ObjectNotFoundException e) {
            // this is OK, test probably did not start
            LOGGER.info("Check leftovers: Task " + oid + " does not exist.");
            return;
        }

        LOGGER.info("Check leftovers: Task " + oid + " state: " + t.getExecutionStatus());

        if (t.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
            LOGGER.info("Leftover task: {}", t);
            leftovers.add(t.getOid());
        }
    }


    // UTILITY METHODS

    // TODO: maybe we should move them to a common utility class

    private void assertAttribute(ShadowType repoShadow, ResourceType resource, String name, String value) {
        assertAttribute(repoShadow, new QName(ResourceTypeUtil.getResourceNamespace(resource), name), value);
    }

    private void assertAttribute(ShadowType repoShadow, QName name, String value) {
        boolean found = false;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (name.equals(JAXBUtil.getElementQName(element))) {
                if (found) {
                    Assert.fail("Multiple values for " + name + " attribute in shadow attributes");
                } else {
                    AssertJUnit.assertEquals(value, ((Element) element).getTextContent());
                    found = true;
                }
            }
        }
    }

    protected void assertAttribute(SearchResultEntry response, String name, String value) {
        AssertJUnit.assertNotNull(response.getAttribute(name.toLowerCase()));
        AssertJUnit.assertEquals(1, response.getAttribute(name.toLowerCase()).size());
        Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
        AssertJUnit.assertEquals(value, attribute.iterator().next().getValue().toString());
    }

    private <T extends ObjectType> PrismObject<T> unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws IOException, JAXBException, SchemaException {
        File file = new File(filePath);
        return PrismTestUtil.parseObject(file);
    }

    private <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath) throws Exception {
    	return addObjectFromFile(filePath, false);
    }

    private <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath, boolean deleteIfExists) throws Exception {
        PrismObject<T> object = (PrismObject<T>) unmarshallJaxbFromFile(filePath, ObjectType.class);
        System.out.println("obj: " + object.getElementName());
        OperationResult result = new OperationResult(TestQuartzTaskManagerContract.class.getName() + ".addObjectFromFile");
        try {
        	add(object, result);
        } catch(ObjectAlreadyExistsException e) {
        	delete(object, result);
        	add(object, result);
        }
        logger.trace("Object from " + filePath + " added to repository.");
        return object;
    }

	private void add(PrismObject<? extends ObjectType> object, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException {
		if (object.canRepresent(TaskType.class)) {
            taskManager.addTask((PrismObject)object, result);
        } else {
            repositoryService.addObject(object, null, result);
        }
	}

	private void delete(PrismObject<? extends ObjectType> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (object.canRepresent(TaskType.class)) {
			taskManager.deleteTask(object.getOid(), result);
		} else {
			repositoryService.deleteObject(ObjectType.class, object.getOid(), result);			// correct?
		}
    }

    private void waitForTaskStart(String oid, OperationResult result) throws CommonException {
		waitFor("Waiting for task manager to start the task", new Checker() {
			public boolean check() throws ObjectNotFoundException, SchemaException {
				Task task = taskManager.getTask(oid, result);
				IntegrationTestTools.display("Task while waiting for task manager to start the task", task);
				return task.getLastRunStartTimestamp() != null;
			}
			@Override
			public void timeout() {
				fail("Timeout while waiting for task " + oid + " to start.");
			}
		}, 10000, 500);
	}
}
