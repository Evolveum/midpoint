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
package com.evolveum.midpoint.task.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author Radovan Semancik
 */

@ContextConfiguration(locations = {"classpath:application-context-task.xml",
        "classpath:application-context-task-test.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-configuration-test.xml"})
public class TestTaskManagerContract extends AbstractTestNGSpringContextTests {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestTaskManagerContract.class);

    private static final String TASK_OWNER_FILENAME = "src/test/resources/repo/owner.xml";
    private static final String TASK_CYCLE_FILENAME = "src/test/resources/repo/cycle-task.xml";
    private static final String TASK_CYCLE_OID = "91919191-76e0-59e2-86d6-998877665544";
    private static final String TASK_WAITING_FILENAME = "src/test/resources/repo/waiting-task.xml";
    private static final String TASK_WAITING_OID = "91919190-76e0-59e2-86d6-556655665566";
    private static final String TASK_SINGLE_FILENAME = "src/test/resources/repo/single-task.xml";
    private static final String TASK_SINGLE_OID = "91919191-76e0-59e2-86d6-556655665566";
    private static final String TASK_SINGLE_MORE_HANDLERS_FILENAME = "src/test/resources/repo/single-task-more-handlers.xml";
    private static final String TASK_SINGLE_MORE_HANDLERS_OID = "91919191-76e0-59e2-86d6-556655665567";
    private static final String TASK_CYCLE_CRON_FILENAME = "src/test/resources/repo/cycle-cron-task.xml";
    private static final String TASK_CYCLE_CRON_OID = "91919191-76e0-59e2-86d6-9988776655aa";
    private static final String TASK_CYCLE_CRON_LOOSE_FILENAME = "src/test/resources/repo/cycle-cron-loose-task.xml";
    private static final String TASK_CYCLE_CRON_LOOSE_OID = "91919191-76e0-59e2-86d6-9988776655bb";

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

        // OperationResult result = new
        // OperationResult(TestTaskManagerContract.class.getName() +
        // ".test000Integrity");
        // ObjectType object = repositoryService.getObject(RESOURCE_OPENDJ_OID,
        // null, result);
        // assertTrue(object instanceof ResourceType);
        // assertEquals(RESOURCE_OPENDJ_OID, object.getOid());
    }

    /**
     * Here we only test setting various task properties.
     */
    
    @Test(enabled = true)
    public void test001TaskToken() throws Exception {

        // Add single task. 
        addObjectFromFile(TASK_WAITING_FILENAME);
        
        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test001TaskToken");

        System.out.println("Retrieving the task and setting its token...");
        
        TaskImpl task = (TaskImpl) taskManager.getTask(TASK_WAITING_OID, result);
        
        // Create the token and insert it as an extension

        PrismPropertyDefinition propDef = new PrismPropertyDefinition(SchemaConstants.SYNC_TOKEN,
        	    SchemaConstants.SYNC_TOKEN, DOMUtil.XSD_INTEGER, prismContext);
        PrismProperty token = propDef.instantiate();
        	  
        token.setValue(new PrismPropertyValue<Integer>(100));

        PropertyDelta<?> tokenDelta = new PropertyDelta(new PropertyPath(TaskType.F_EXTENSION, token.getName()), token.getDefinition());
        tokenDelta.setValuesToReplace(token.getValues());
        task.modify(tokenDelta, result);
        
        // Check the extension
        
        PrismContainer pc = task.getExtension();
        AssertJUnit.assertNotNull("The task extension was not read back", pc);
        
        PrismProperty token2 = pc.findProperty(SchemaConstants.SYNC_TOKEN);
        AssertJUnit.assertNotNull("Token in task extension was not read back", token2);
        AssertJUnit.assertEquals("Token in task extension has an incorrect value", (Integer) 100, token2.getRealValue()); 

//        PrismProperty<Integer> token = new PrismProperty<Integer>(SchemaConstants.SYNC_TOKEN);
//        PrismContainer<?> ext = task000.getExtension();
//        ext.add(token);
        
//		  PrismProperty<?> p = ext.findOrCreateProperty(SchemaConstants.SYNC_TOKEN);
    }
    
//    @Test(enabled = true)
//    public void test002TaskProperties() throws Exception {
//
//        task000.setBindingPersistent(TaskBinding.LOOSE, result);
//        String newname = "Test task, name changed";
//        task000.setNamePersistentBatched(newname);
//        task000.setProgressPersistentBatched(10);
//        long currentTime = System.currentTimeMillis();
//        long currentTime1 = currentTime + 10000;
//        long currentTime2 = currentTime + 25000;
//        task000.setLastRunStartTimestampPersistentBatched(currentTime);
//        task000.setLastRunFinishTimestampPersistentBatched(currentTime1);
//        task000.setNextRunStartTimePersistentBatched(currentTime2);
//        task000.setExclusivityStatusPersistentBatched(TaskExclusivityStatus.CLAIMED);
//        task000.setExecutionStatusPersistentBatched(TaskExecutionStatus.SUSPENDED);
//        task000.setHandlerUriPersistentBatched("http://no-handler.org/");
//        task000.setRecurrenceStatusPersistentBatched(TaskRecurrence.RECURRING);
//                
//        OperationResultType ort = result.createOperationResultType();
//        
//        task000.setResultPersistentBatched(result);
//        
//        logger.trace("Saving modifications...");
//        task000.savePendingModifications(result);
//        
//        logger.trace("Retrieving the task (second time) and comparing its properties...");
//        
//        Task task001 = taskManager.getTask(TASK_WAITING_OID, result);
//        AssertJUnit.assertEquals(TaskBinding.LOOSE, task001.getBinding());
//        AssertJUnit.assertEquals(newname, task001.getName());
//        AssertJUnit.assertTrue(10 == task001.getProgress());
//        AssertJUnit.assertNotNull(task001.getLastRunStartTimestamp());
//        AssertJUnit.assertTrue(currentTime == task001.getLastRunStartTimestamp());
//        AssertJUnit.assertNotNull(task001.getLastRunFinishTimestamp());
//        AssertJUnit.assertTrue(currentTime1 == task001.getLastRunFinishTimestamp());        
//        AssertJUnit.assertNotNull(task001.getNextRunStartTime());
//        AssertJUnit.assertTrue(currentTime2 == task001.getNextRunStartTime());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task001.getExclusivityStatus());
//        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task001.getExecutionStatus());
//        AssertJUnit.assertEquals("http://no-handler.org/", task001.getHandlerUri());
//        AssertJUnit.assertTrue(task001.isCycle());
//        OperationResult r001 = task001.getResult();
//        AssertJUnit.assertNotNull(r001);
//        
//        OperationResultType ort1 = r001.createOperationResultType();
//        
//        // handling of operation result in tasks is extremely fragile now... 
//        // in case of problems, just uncomment the following line ;)
//        AssertJUnit.assertEquals(ort, ort1);
//        
//    }

    @Test(enabled = false)
    public void test002Single() throws Exception {

        // reset 'has run' flag on the handler
        singleHandler1.resetHasRun();

        // Add single task. This will get picked by task scanner and executed
        addObjectFromFile(TASK_SINGLE_FILENAME);
        
        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test002Single");

        logger.trace("Retrieving the task...");
        Task task000 = taskManager.getTask(TASK_SINGLE_OID, result);

        task000.setProgressPersistent(10, result);
        long currentTime = System.currentTimeMillis();
        task000.setLastRunStartTimestampPersistent(currentTime, result);
        
        Task task001 = taskManager.getTask(TASK_SINGLE_OID, result);
        AssertJUnit.assertTrue(10 == task001.getProgress());
        AssertJUnit.assertNotNull(task001.getLastRunStartTimestamp());
        AssertJUnit.assertTrue(currentTime == task001.getLastRunStartTimestamp());
        
        AssertJUnit.assertNotNull(task000);
        logger.trace("Task retrieval OK.");

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task
        logger.info("Waiting for task manager to pick up the task and run it");
        Thread.sleep(2000);
        logger.info("... done");

        // Check task status
        
        Task task = taskManager.getTask(TASK_SINGLE_OID, result);

        AssertJUnit.assertNotNull(task);
        logger.trace("getTask returned: " + task.dump());

        PrismObject<TaskType> po = repositoryService.getObject(TaskType.class, TASK_SINGLE_OID, null, result);
        logger.trace("getObject returned: " + po.dump());

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

        // Test whether handler has really run
        AssertJUnit.assertTrue(singleHandler1.hasRun());
    }

    @Test(enabled = false)
    public void test003Cycle() throws Exception {
        // Add cycle task. This will get picked by task scanner and executed

        // But before that check sanity ... a known problem with xsi:type
    	PrismObject<ObjectType> object = addObjectFromFile(TASK_CYCLE_FILENAME);
        ObjectType objectType = object.asObjectable();
        TaskType addedTask = (TaskType) objectType;
        Element ext2 = (Element) addedTask.getExtension().getAny().get(1);
        QName xsiType = DOMUtil.resolveXsiType(ext2, "d");
        System.out.println("######################1# " + xsiType);
        AssertJUnit.assertEquals("Bad xsi:type before adding task", DOMUtil.XSD_INTEGER, xsiType);

        // Read from repo

        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test003Cycle");

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
        TaskType repoTaskType = repoTask.asObjectable();
        ext2 = (Element) repoTaskType.getExtension().getAny().get(1);
        xsiType = DOMUtil.resolveXsiType(ext2, "d");
        System.out.println("######################2# " + xsiType);
        AssertJUnit.assertEquals("Bad xsi:type after adding task", DOMUtil.XSD_INTEGER, xsiType);

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task
        System.out.println("Waiting for task manager to pick up the task");
        Thread.sleep(2000);
        System.out.println("... done");

        // Check task status

        Task task = taskManager.getTask(TASK_CYCLE_OID, result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.dump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, TASK_CYCLE_OID, null, result);
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

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue(task.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);
        AssertJUnit.assertTrue(taskResult.isSuccess());
    }

//    @Test(enabled = false)
//    public void test004Extension() throws Exception {
//
//        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test004Extension");
//        Task task = taskManager.getTask(TASK_CYCLE_OID, result);
//        AssertJUnit.assertNotNull(task);
//
//        // Test for extension. This will also roughly test extension processor
//        // and schema processor
//        PrismContainer taskExtension = task.getExtension();
//        AssertJUnit.assertNotNull(taskExtension);
//        System.out.println(taskExtension.dump());
//
//        PrismProperty shipStateProp = taskExtension
//                .findProperty(new QName("http://myself.me/schemas/whatever", "shipState"));
//        AssertJUnit.assertEquals("capsized", shipStateProp.getValue(String.class).getValue());
//
//        PrismProperty deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
//        AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getValue().getClass());
//        AssertJUnit.assertEquals(Integer.valueOf(42), deadProp.getValue(Integer.class).getValue());
//
//        List<PropertyModification> mods = new ArrayList<PropertyModification>();
//        // One more mariner drowned
//        int newDead = deadProp.getValue(Integer.class).getValue().intValue() + 1;
//        mods.add(deadProp.createModification(PropertyModification.ModificationType.REPLACE, new PrismPropertyValue<Object>(Integer.valueOf(newDead))));
//        // ... then the ship was lost
//        mods.add(shipStateProp.createModification(PropertyModification.ModificationType.REPLACE, new PrismPropertyValue<Object>("sunk")));
//        // ... so remember the date
//        // This has no type information or schema. The type has to be determined
//        // from the java type
//        GregorianCalendar sinkDate = new GregorianCalendar();
//        PrismProperty dateProp = taskExtension.createProperty(new QName("http://myself.me/schemas/whatever", "sinkTimestamp"), sinkDate.getClass());
//        mods.add(dateProp.createModification(PropertyModification.ModificationType.REPLACE, new PrismPropertyValue<Object>(sinkDate)));
//
//        task.modifyExtension(mods, result);
//
//        // Debug: display the real repository state
//        ObjectType o = repositoryService.getObject(ObjectType.class, TASK_CYCLE_OID, null, result);
//        System.out.println(ObjectTypeUtil.dump(o));
//
//        // Refresh the task
//        task.refresh(result);
//
//        // get the extension again ... and test it ... again
//        taskExtension = task.getExtension();
//        AssertJUnit.assertNotNull(taskExtension);
//        System.out.println(taskExtension.dump());
//
//        shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "shipState"));
//        AssertJUnit.assertEquals("sunk", shipStateProp.getValue(String.class).getValue());
//
//        deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
//        AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getValue().getClass());
//        AssertJUnit.assertEquals(Integer.valueOf(43), deadProp.getValue(Integer.class).getValue());
//
//        dateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "sinkTimestamp"));
//        AssertJUnit.assertNotNull("sinkTimestamp is null", dateProp);
//        AssertJUnit.assertEquals(GregorianCalendar.class, dateProp.getValues().iterator().next().getValue().getClass());
//        PrismPropertyValue<GregorianCalendar> fetchedDate = dateProp.getValue(GregorianCalendar.class);
//        AssertJUnit.assertTrue(fetchedDate.getValue().compareTo(sinkDate) == 0);
//
//        // stop the task to keep the log clean
//        task.close(result);
//        task.shutdown();
//        LOGGER.info("Cycle Task (15sec) has been shut down.");
//    }

//    @Test(enabled = false)
//    public void test005MoreHandlers() throws Exception {
//
//        // reset 'has run' flag on handlers
//        singleHandler1.resetHasRun();
//        singleHandler2.resetHasRun();
//        singleHandler3.resetHasRun();
//
//        // Add single task. This will get picked by task scanner and executed
//        addObjectFromFile(TASK_SINGLE_MORE_HANDLERS_FILENAME);
//
//        // We need to wait for a sync interval, so the task scanner has a chance
//        // to pick up this
//        // task
//        System.out.println("Waiting for task manager to pick up the task and run it");
//        Thread.sleep(2000);
//        System.out.println("... done");
//
//        // Check task status
//
//        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test005MoreHandlers");
//        Task task = taskManager.getTask(TASK_SINGLE_MORE_HANDLERS_OID, result);
//
//        AssertJUnit.assertNotNull(task);
//        System.out.println(task.dump());
//
//        ObjectType o = repositoryService.getObject(ObjectType.class, TASK_SINGLE_MORE_HANDLERS_OID, null, result);
//        System.out.println(ObjectTypeUtil.dump(o));
//
//        // .. it should be closed
//        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());
//
//        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
//
//        // .. and last run should not be zero
//        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
//        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
//
//        // The progress should be more than 0 as the task has run at least once
//        AssertJUnit.assertTrue(task.getProgress() > 0);
//
//        // Test for presence of a result. It should be there and it should
//        // indicate success
//        OperationResult taskResult = task.getResult();
//        AssertJUnit.assertNotNull(taskResult);
//        AssertJUnit.assertTrue(taskResult.isSuccess());
//
//        // Test if all three handlers were run
//
//        AssertJUnit.assertTrue(singleHandler1.hasRun());
//        AssertJUnit.assertTrue(singleHandler2.hasRun());
//        AssertJUnit.assertTrue(singleHandler3.hasRun());
//    }
//
//    @Test(enabled = false)
//    public void test006CycleCron() throws Exception {
//    	
//        // Add cycle task. This will get picked by task scanner and executed
//
//        // But before that check sanity ... a known problem with xsi:type
//        ObjectType objectType = addObjectFromFile(TASK_CYCLE_CRON_FILENAME);
//        TaskType addedTask = (TaskType) objectType;
//
//        // Read from repo
//
//        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test006CycleCron");
//
//        TaskType repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
//
//        // We need to wait for a sync interval, so the task scanner has a chance
//        // to pick up this
//        // task
//        System.out.println("Waiting for task manager to pick up the task");
//        Thread.sleep(100000);
//        System.out.println("... done");
//
//        // Check task status
//
//        Task task = taskManager.getTask(TASK_CYCLE_CRON_OID, result);
//        	
//        AssertJUnit.assertNotNull(task);
//        System.out.println(task.dump());
//
//        TaskType t = repositoryService.getObject(TaskType.class, TASK_CYCLE_CRON_OID, null, result);
//        System.out.println(ObjectTypeUtil.dump(t));
//
//        // .. it should be running
//        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
//
//        // .. and claimed
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());
//
//        // .. and last run should not be zero
//        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
//        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
//
//        // The progress should be more than 0 as the task has run at least once
//        AssertJUnit.assertTrue(task.getProgress() > 0);
//
//        // Test for presence of a result. It should be there and it should
//        // indicate success
//        OperationResult taskResult = task.getResult();
//        AssertJUnit.assertNotNull(taskResult);
//        AssertJUnit.assertTrue(taskResult.isSuccess());
//    }
//
//    @Test(enabled = false)
//    public void test007CycleCronLoose() throws Exception {
//    	
//        // Add cycle task. This will get picked by task scanner and executed
//
//        // But before that check sanity ... a known problem with xsi:type
//        ObjectType objectType = addObjectFromFile(TASK_CYCLE_CRON_LOOSE_FILENAME);
//        TaskType addedTask = (TaskType) objectType;
//
//        // Read from repo
//
//        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test007CycleCronLoose");
//
//        TaskType repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
//
//        // We need to wait for a sync interval, so the task scanner has a chance
//        // to pick up this
//        // task
//        System.out.println("Waiting for task manager to pick up the task");
//        Thread.sleep(100000);
//        System.out.println("... done");
//
//        // Check task status
//
//        Task task = taskManager.getTask(TASK_CYCLE_CRON_LOOSE_OID, result);
//        // if task is claimed, wait a while and check again
//        if (TaskExclusivityStatus.CLAIMED.equals(task.getExclusivityStatus())) {
//        	Thread.sleep(20000);
//        	task = taskManager.getTask(TASK_CYCLE_CRON_LOOSE_OID, result);	// now it should not be claimed for sure!
//        }
//
//        AssertJUnit.assertNotNull(task);
//        System.out.println(task.dump());
//
//        TaskType t = repositoryService.getObject(TaskType.class, TASK_CYCLE_CRON_LOOSE_OID, null, result);
//        System.out.println(ObjectTypeUtil.dump(t));
//
//        // .. it should be running
//        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
//
//        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
//
//        // .. and last run should not be zero
//        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
//        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
//        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);
//
//        // The progress should be more than 0 as the task has run at least once
//        AssertJUnit.assertTrue(task.getProgress() > 0);
//
//        // Test for presence of a result. It should be there and it should
//        // indicate success
//        OperationResult taskResult = task.getResult();
//        AssertJUnit.assertNotNull(taskResult);
//        AssertJUnit.assertTrue(taskResult.isSuccess());
//    }

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
        PrismObject<ObjectType> object = unmarshallJaxbFromFile(filePath, ObjectType.class);
        System.out.println("obj: " + object.getName());
        OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".addObjectFromFile");
        if (object.canRepresent(TaskType.class)) {
            taskManager.addTask((PrismObject)object, result);
        } else {
            repositoryService.addObject(object, result);
        }
        logger.trace("Object from " + filePath + " added to repository.");
        return object;
    }

    private void display(SearchResultEntry response) {
        // TODO Auto-generated method stub
        System.out.println(response.toLDIFString());
    }

}
