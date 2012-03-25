/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.opends.server.types.Attribute;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UriStack;

/**
 * @author Radovan Semancik
 */

@ContextConfiguration(locations = {"classpath:application-context-task.xml",
        "classpath:application-context-task-test.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-configuration-test.xml"})
public class TestQuartzTaskManagerContract extends AbstractTestNGSpringContextTests {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestQuartzTaskManagerContract.class);

    private static final String TASK_OWNER_FILENAME = "src/test/resources/repo/owner.xml";
    private static final String NS_WHATEVER = "http://myself.me/schemas/whatever";
    
    private static String taskFilename(String test) {
    	return "src/test/resources/repo/task-" + test + ".xml";
    }
    
    private static String taskOid(String test) {
    	return "91919191-76e0-59e2-86d6-556655660" + test.substring(0, 3);
    }
    
    private static OperationResult createResult(String test) {
    	System.out.println("===[ "+test+" ]===");
    	LOGGER.info("===[ "+test+" ]===");
    	return new OperationResult(TestQuartzTaskManagerContract.class.getName() + ".test" + test);
    }

    private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
    private static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
    private static final String SINGLE_TASK_HANDLER_2_URI = "http://midpoint.evolveum.com/test/single-task-handler-2";
    private static final String SINGLE_TASK_HANDLER_3_URI = "http://midpoint.evolveum.com/test/single-task-handler-3";

    @Autowired(required = true)
    private RepositoryService repositoryService;
    private static boolean repoInitialized = false;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private PrismContext prismContext;

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
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

    @PostConstruct
    public void initHandlers() throws Exception {
        MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler();
        taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);
        singleHandler1 = new MockSingleTaskHandler("1");
        taskManager.registerHandler(SINGLE_TASK_HANDLER_URI, singleHandler1);
        singleHandler2 = new MockSingleTaskHandler("2");
        taskManager.registerHandler(SINGLE_TASK_HANDLER_2_URI, singleHandler2);
        singleHandler3 = new MockSingleTaskHandler("3");
        taskManager.registerHandler(SINGLE_TASK_HANDLER_3_URI, singleHandler3);
        
        addObjectFromFile(TASK_OWNER_FILENAME);
    }

    /**
     * Test integrity of the test setup.
     *
     * @throws SchemaException
     * @throws ObjectNotFoundException
     */
    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }
    
    /**
     * Here we only test setting various task properties.
     */

    @Test(enabled = true)
    public void test004TaskProperties() throws Exception {
 
    	String test = "004TaskProperties";
        OperationResult result = createResult(test);

        addObjectFromFile(taskFilename(test));
        
        logger.trace("Retrieving the task and setting its token...");
        
        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);

        task.setBindingImmediate(TaskBinding.LOOSE, result);
        
        // other properties will be set in batched mode
        String newname = "Test task, name changed";
        task.setName(newname);
        task.setProgress(10);
        long currentTime = System.currentTimeMillis();
        long currentTime1 = currentTime + 10000;
        long currentTime2 = currentTime + 25000;
        task.setLastRunStartTimestamp(currentTime);
        task.setLastRunFinishTimestamp(currentTime1);
        task.setNextRunStartTime(currentTime2);
        task.setExclusivityStatus(TaskExclusivityStatus.CLAIMED);
        task.setExecutionStatus(TaskExecutionStatus.SUSPENDED);
        task.setHandlerUri("http://no-handler.org/");
        task.pushHandlerUri("http://no-handler.org/1");
        task.pushHandlerUri("http://no-handler.org/2");
        task.setRecurrenceStatus(TaskRecurrence.RECURRING);
                
        OperationResultType ort = result.createOperationResultType();			// to be compared with later
        
        task.setResult(result);
        
        logger.trace("Saving modifications...");
        
        task.savePendingModifications(result);
        
        logger.trace("Retrieving the task (second time) and comparing its properties...");
        
        Task task001 = taskManager.getTask(taskOid(test), result);
        AssertJUnit.assertEquals(TaskBinding.LOOSE, task001.getBinding());
        AssertJUnit.assertEquals(newname, task001.getName());
        AssertJUnit.assertTrue(10 == task001.getProgress());
        AssertJUnit.assertNotNull(task001.getLastRunStartTimestamp());
        AssertJUnit.assertTrue(currentTime == task001.getLastRunStartTimestamp());
        AssertJUnit.assertNotNull(task001.getLastRunFinishTimestamp());
        AssertJUnit.assertTrue(currentTime1 == task001.getLastRunFinishTimestamp());        
        AssertJUnit.assertNotNull(task001.getNextRunStartTime());
        AssertJUnit.assertTrue(currentTime2 == task001.getNextRunStartTime());
        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task001.getExclusivityStatus());
        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task001.getExecutionStatus());
        AssertJUnit.assertEquals("http://no-handler.org/2", task001.getHandlerUri());
        AssertJUnit.assertEquals("Number of handlers is not OK", 3, task.getHandlersCount());
        UriStack us = task.getOtherHandlersUriStack();
        AssertJUnit.assertEquals("First handler from the handler stack does not match", "http://no-handler.org/", us.getUri().get(0));
        AssertJUnit.assertEquals("Second handler from the handler stack does not match", "http://no-handler.org/1", us.getUri().get(1));
        AssertJUnit.assertTrue(task001.isCycle());
        OperationResult r001 = task001.getResult();
        AssertJUnit.assertNotNull(r001);
        
        OperationResultType ort1 = r001.createOperationResultType();
        
        // handling of operation result in tasks is extremely fragile now... 
        // in case of problems, just uncomment the following line ;)
        AssertJUnit.assertEquals(ort, ort1);
        
    }


    /*
     * Execute a single-run task.
     */

    @Test(enabled = true)
    public void test005Single() throws Exception {

    	String test = "005Single";
        OperationResult result = createResult(test);
    	
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
       	
       	((TaskManagerQuartzImpl) taskManager).scan();
       	
        logger.info("Waiting for task manager to pick up the task and run it");
        Thread.sleep(2000);
        logger.info("... done");

        // Check task status
        
        Task task1 = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task1);
        System.out.println("getTask returned: " + task1.dump());

        PrismObject<TaskType> po = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println("getObject returned: " + po.dump());

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task1.getExecutionStatus());

        // .. and released
        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task1.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task1.getLastRunStartTimestamp());
        AssertJUnit.assertFalse("LastRunStartTimestamp is 0", task1.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("LastRunFinishTimestamp is null", task1.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse("LastRunFinishTimestamp is 0", task1.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue(task1.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task1.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());

        // Test for no presence of handlers
        AssertJUnit.assertNull("Handler is still present", task1.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present", 
        		task1.getOtherHandlersUriStack() == null || task1.getOtherHandlersUriStack().getUri().isEmpty());
        
        // Test whether handler has really run
        AssertJUnit.assertTrue(singleHandler1.hasRun());
    }
    
    /*
     * Executes a cyclic task
     */

    @Test(enabled = true)
    public void test006Cycle() throws Exception {
    	String test = "006Cycle";
        OperationResult result = createResult(test);
    	
        // But before that check sanity ... a known problem with xsi:type
    	PrismObject<ObjectType> object = addObjectFromFile(taskFilename(test));
    	
        ObjectType objectType = object.asObjectable();
        TaskType addedTask = (TaskType) objectType;
        System.out.println("Added task");
        System.out.println(object.dump());
        
        PrismContainer<?> extensionContainer = object.getExtension();
        PrismProperty<Object> deadProperty = extensionContainer.findProperty(new QName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (add result)", DOMUtil.XSD_INTEGER, deadProperty.getDefinition().getTypeName());
        
//        Element ext2 = (Element) addedTask.getExtension().getAny().get(0);
//        if (!ext2.getLocalName().equals("dead"))		// not a very nice code...
//        	ext2 = (Element) addedTask.getExtension().getAny().get(1);
//        QName xsiType = DOMUtil.resolveXsiType(ext2, "d");
//        System.out.println("######################1# " + xsiType);
//        AssertJUnit.assertEquals("Bad xsi:type before adding task", DOMUtil.XSD_INTEGER, xsiType);

        // Read from repo
        
        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
        TaskType repoTaskType = repoTask.asObjectable();
        
        extensionContainer = repoTask.getExtension();
        deadProperty = extensionContainer.findProperty(new QName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (from repo)", DOMUtil.XSD_INTEGER, deadProperty.getDefinition().getTypeName());

        
//        ext2 = (Element) addedTask.getExtension().getAny().get(0);
//        if (!ext2.getLocalName().equals("dead"))		// not a very nice code...
//        	ext2 = (Element) addedTask.getExtension().getAny().get(1);
//        xsiType = DOMUtil.resolveXsiType(ext2, "d");
//        System.out.println("######################2# " + xsiType);
//        AssertJUnit.assertEquals("Bad xsi:type after adding task", DOMUtil.XSD_INTEGER, xsiType);

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task
        ((TaskManagerQuartzImpl) taskManager).scan();
        
        LOGGER.trace("Waiting for task manager to pick up the task");
        Thread.sleep(3000);
        LOGGER.trace("... done");

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(t.dump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

        // .. and claimed
        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more at least 2 (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue(task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());
        
        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);
    	
    }

    
    
    /*
     * Single-run task with more handlers.
     */

    @Test(enabled = true)
    public void test008MoreHandlers() throws Exception {

    	String test = "008MoreHandlers";
        OperationResult result = createResult(test);
    	
        // reset 'has run' flag on handlers
        singleHandler1.resetHasRun();
        singleHandler2.resetHasRun();
        singleHandler3.resetHasRun();

        addObjectFromFile(taskFilename(test));

        ((TaskManagerQuartzImpl) taskManager).scan();
        logger.info("Waiting for task manager to pick up the task and run it");
        Thread.sleep(2000);
        logger.info("... done");

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and released
        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue(task.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());
        
        // Test for no presence of handlers
        
        AssertJUnit.assertNull("Handler is still present", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present", 
        		task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUri().isEmpty());

        // Test if all three handlers were run

        AssertJUnit.assertTrue(singleHandler1.hasRun());
        AssertJUnit.assertTrue(singleHandler2.hasRun());
        AssertJUnit.assertTrue(singleHandler3.hasRun());
    }

    @Test(enabled = true)
    public void test009CycleLoose() throws Exception {
    	String test = "009CycleLoose";
        OperationResult result = createResult(test);
    	
    	PrismObject<ObjectType> object = addObjectFromFile(taskFilename(test));
    	
        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this task
        
        LOGGER.trace("Waiting for task manager to pick up the task");
        ((TaskManagerQuartzImpl) taskManager).scan();
        Thread.sleep(5000);
        LOGGER.trace("... done");

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(test), null, result);
        System.out.println(t.dump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

        // .. and released
        
        // if task is claimed, wait a while and check again
        if (TaskExclusivityStatus.CLAIMED.equals(task.getExclusivityStatus())) {
        	Thread.sleep(1000);
        	task = taskManager.getTask(taskOid(test), result);	// now it should not be claimed for sure!
            AssertJUnit.assertNotNull(task);
            System.out.println(task.dump());
        }

//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be more at least 2 (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue("Progress is none or too small", task.getProgress() >= 2);

        // The progress should not be too big (indicates fault in scheduling)
        AssertJUnit.assertTrue("Progress is too big (fault in scheduling?)", task.getProgress() <= 5);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());
        
        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);
    	
    }

    @Test(enabled = false)			// takes ~130 seconds to run
    public void test010CycleCronLoose() throws Exception {
    	
    	String test = "010CycleCronLoose";
    	OperationResult result = createResult(test);
    	
        addObjectFromFile(taskFilename(test));

        // We have to wait sufficiently long in order for the task to be processed at least twice
        ((TaskManagerQuartzImpl) taskManager).scan();
        
        System.out.println("Waiting for task manager to pick up the task");
        Thread.sleep(130000);
        System.out.println("... done");

        // Check task status

        Task task = taskManager.getTask(taskOid(test), result);
        
        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());

        // if task is claimed, wait a while and check again
        if (TaskExclusivityStatus.CLAIMED.equals(task.getExclusivityStatus())) {
        	Thread.sleep(20000);
        	task = taskManager.getTask(taskOid(test), result);	// now it should not be claimed for sure!
            AssertJUnit.assertNotNull(task);
            System.out.println(task.dump());
        }

        TaskType t = repositoryService.getObject(TaskType.class, taskOid(test), null, result).getValue().getValue();
        System.out.println(ObjectTypeUtil.dump(t));

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());		// should be released, as it is loosely bound one

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be at least 2 as the task has run at least twice
        AssertJUnit.assertTrue("Task has not been executed at least twice", task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());
        
        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);
        
    }
    
    /*
     * This task should NOT be processed (more handlers with recurrent tasks are not supported, because can lead to unpredictable results)
     */

    @Test(enabled = false)
    public void test011CycleMoreHandlers() throws Exception {
    	
    	String test = "011CycleMoreHandlers";
    	OperationResult result = createResult(test);
    	
    	addObjectFromFile(taskFilename(test));
    	
        TaskQuartzImpl task = (TaskQuartzImpl) taskManager.getTask(taskOid(test), result);
        
        System.out.println("Waiting for task manager to pick up the task");
        ((TaskManagerQuartzImpl) taskManager).scan();
        Thread.sleep(2000);
        System.out.println("... done");

        // Check task status

        task.refresh(result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());
        
        // Check whether there are really 2 handlers
        AssertJUnit.assertEquals("There are not 2 task handlers", 2, task.getHandlersCount());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

        // Task manager should reject this task
        AssertJUnit.assertNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertTrue(task.getProgress() == 0);

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTask(task, 100, result);

    }
    
    /*
     * Suspends a running task.
     */

    @Test(enabled = true)
    public void test012Suspend() throws Exception {
    	
    	String test = "012Suspend";
        OperationResult result = createResult(test);

      	addObjectFromFile(taskFilename(test));

        System.out.println("Waiting for task manager to pick up the task");
        ((TaskManagerQuartzImpl) taskManager).scan();
        Thread.sleep(2000);
        System.out.println("... done");

        // Check task status (task is running 5 iterations where each takes 2000 ms)

        Task task = taskManager.getTask(taskOid(test), result);
        
        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());
        
        AssertJUnit.assertEquals("Task is not running", TaskExecutionStatus.RUNNING, task.getExecutionStatus());
        AssertJUnit.assertEquals("Task is not claimed", TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());
        
        // Now suspend the task

        boolean stopped = taskManager.suspendTask(task, 0, result);
        
        task.refresh(result);
        System.out.println("After suspend and refresh: " + task.dump());
        
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
        
        AssertJUnit.assertNotNull("Task last start time is null", task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse("Task last start time is 0", task.getLastRunStartTimestamp().longValue() == 0);

        // The progress should be more than 0
        AssertJUnit.assertTrue("Task has not reported any progress", task.getProgress() > 0);

        Thread.sleep(200);		// give the scheduler a chance to release the task
        
        task.refresh(result);
        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    @Test(enabled = true)
    public void test013ReleaseAndSuspendLooselyBound() throws Exception {
    	
    	String test = "013ReleaseAndSuspendLooselyBound";
        OperationResult result = createResult(test);

    	addObjectFromFile(taskFilename(test));
        
        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.dump());
        
        // let us resume (i.e. start the task)
        taskManager.resumeTask(task, result);

        // task is executing for 1000 ms, so we need to wait slightly longer, in order for the execution to be done
        System.out.println("Waiting for task manager to pick up the task");
        ((TaskManagerQuartzImpl) taskManager).scan();
        Thread.sleep(3000);
        System.out.println("... done");

        task.refresh(result);
        
        System.out.println("After refresh: " + task.dump());
        
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());		// task cycle is 1000 ms, so it should be released now 

        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);
        
        // now let us suspend it (occurs during wait cycle, so we can put short timeout here)
        
        boolean stopped = taskManager.suspendTask(task, 300, result);
        
        task.refresh(result);
        
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        
        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
        
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);
	    
        Thread.sleep(200);		// give the scheduler a chance to release the task
        task.refresh(result);
        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

    }

    @Test(enabled = true)
    public void test014SuspendLongRunning() throws Exception {

    	String test = "014SuspendLongRunning";
    	OperationResult result = createResult(test);
    	
    	addObjectFromFile(taskFilename(test));
        
        
        Task task = taskManager.getTask(taskOid(test), result);
        System.out.println("After setup: " + task.dump());
        
        System.out.println("Waiting for task manager to pick up the task");
        ((TaskManagerQuartzImpl) taskManager).scan();
        Thread.sleep(2000);		// task itself takes 8 seconds to finish
        System.out.println("... done");

        task.refresh(result);
        
        System.out.println("After refresh: " + task.dump());
        
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        
        // now let us suspend it, without long waiting
        
        boolean stopped = taskManager.suspendTask(task, 1000, result);
        
        task.refresh(result);
        
        AssertJUnit.assertFalse("Task is stopped (it should be running for now)", stopped);
        
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
        AssertJUnit.assertEquals("Task should be still claimed, as it is not definitely stopped", TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertTrue("There should be no progress reported", task.getProgress() == 0);
        
        // now let us wait for the finish
        
        stopped = taskManager.suspendTask(task, 0, result);
        
        task.refresh(result);
        
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertNotNull("Last run finish time is null", task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse("Last run finish time is zero", task.getLastRunStartTimestamp().longValue() == 0);
        AssertJUnit.assertTrue("Progress is not reported", task.getProgress() > 0);

        Thread.sleep(200);		// give the scheduler a chance to release the task
        task.refresh(result);
        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    // UTILITY METHODS

    // TODO: maybe we should move them to a common utility class

    private void assertAttribute(AccountShadowType repoShadow, ResourceType resource, String name, String value) {
        assertAttribute(repoShadow, new QName(resource.getNamespace(), name), value);
    }

    private void assertAttribute(AccountShadowType repoShadow, QName name, String value) {
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

    private <T extends ObjectType> PrismObject<T> unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException, JAXBException, SchemaException {
        File file = new File(filePath);
        return PrismTestUtil.parseObject(file);
    }
    
    private PrismObject<ObjectType> addObjectFromFile(String filePath) throws Exception {
    	return addObjectFromFile(filePath, false);
    }

    private PrismObject<ObjectType> addObjectFromFile(String filePath, boolean deleteIfExists) throws Exception {
        PrismObject<ObjectType> object = unmarshallJaxbFromFile(filePath, ObjectType.class);
        System.out.println("obj: " + object.getName());
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

	private void add(PrismObject<ObjectType> object, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException {
		if (object.canRepresent(TaskType.class)) {
            taskManager.addTask((PrismObject)object, result);
        } else {
            repositoryService.addObject(object, result);
        }
	}

	private void delete(PrismObject<ObjectType> object, OperationResult result) throws ObjectNotFoundException {
		if (object.canRepresent(TaskType.class)) {
			taskManager.deleteTask(object.getOid(), result);
		} else {
			repositoryService.deleteObject(ObjectType.class, object.getOid(), result);			// correct?
		}
}
    private void display(SearchResultEntry response) {
        // TODO Auto-generated method stub
        System.out.println(response.toLDIFString());
    }

}
