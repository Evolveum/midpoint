/**
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 *
 */
package com.evolveum.midpoint.task.impl;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.AssertJUnit.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.opends.server.types.Attribute;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author Radovan Semancik
 * 
 */

@ContextConfiguration(locations = { "classpath:application-context-task.xml",
		"classpath:application-context-task-test.xml", 
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class TestTaskManagerContract extends AbstractTestNGSpringContextTests {

	private static final String TASK_CYCLE_FILENAME = "src/test/resources/repo/cycle-task.xml";
	private static final String TASK_CYCLE_OID = "91919191-76e0-59e2-86d6-998877665544";
	private static final String TASK_SINGLE_FILENAME = "src/test/resources/repo/single-task.xml";
	private static final String TASK_SINGLE_OID = "91919191-76e0-59e2-86d6-556655665566";
	private static final String TASK_SINGLE_MORE_HANDLERS_FILENAME = "src/test/resources/repo/single-task-more-handlers.xml";
	private static final String TASK_SINGLE_MORE_HANDLERS_OID = "91919191-76e0-59e2-86d6-556655665567";
	
	private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
	private static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
	private static final String SINGLE_TASK_HANDLER_2_URI = "http://midpoint.evolveum.com/test/single-task-handler-2";
	private static final String SINGLE_TASK_HANDLER_3_URI = "http://midpoint.evolveum.com/test/single-task-handler-3";

	private static JAXBContext jaxbctx;
	private static Unmarshaller unmarshaller;

	@Autowired(required = true)
	private RepositoryService repositoryService;
	private static boolean repoInitialized = false;

	@Autowired(required = true)
	private TaskManager taskManager;

	public TestTaskManagerContract() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
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
	public void initHandlers() {
		MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler();
		taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);
		singleHandler1 = new MockSingleTaskHandler("1");
		taskManager.registerHandler(SINGLE_TASK_HANDLER_URI, singleHandler1);
		singleHandler2 = new MockSingleTaskHandler("2");
		taskManager.registerHandler(SINGLE_TASK_HANDLER_2_URI, singleHandler2);
		singleHandler3 = new MockSingleTaskHandler("3");
		taskManager.registerHandler(SINGLE_TASK_HANDLER_3_URI, singleHandler3);
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

	@Test(enabled=false)
	public void test002Single() throws Exception {
		
		// reset 'has run' flag on the handler
		singleHandler1.resetHasRun();
		
		// Add single task. This will get picked by task scanner and executed
		addObjectFromFile(TASK_SINGLE_FILENAME);

		// We need to wait for a sync interval, so the task scanner has a chance
		// to pick up this
		// task
		System.out.println("Waiting for task manager to pick up the task and run it");
		Thread.sleep(2000);
		System.out.println("... done");

		// Check task status

		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test002Single");
		Task task = taskManager.getTask(TASK_SINGLE_OID, result);

		AssertJUnit.assertNotNull(task);
		System.out.println(task.dump());

		ObjectType o = repositoryService.getObject(ObjectType.class, TASK_SINGLE_OID, null, result);
		System.out.println(ObjectTypeUtil.dump(o));

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

	@Test(enabled=false)
	public void test003Cycle() throws Exception {
		// Add cycle task. This will get picked by task scanner and executed

		// But before that check sanity ... a known problem with xsi:type
		ObjectType objectType = addObjectFromFile(TASK_CYCLE_FILENAME);
		TaskType addedTask = (TaskType) objectType;
		Element ext2 = (Element) addedTask.getExtension().getAny().get(1);
		QName xsiType = DOMUtil.resolveXsiType(ext2, "d");
		System.out.println("######################1# " + xsiType);
		AssertJUnit.assertEquals("Bad xsi:type before adding task", DOMUtil.XSD_INTEGER, xsiType);

		// Read from repo

		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test003Cycle");

		TaskType repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);
		ext2 = (Element) repoTask.getExtension().getAny().get(1);
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

		TaskType t = repositoryService.getObject(TaskType.class, TASK_CYCLE_OID, null, result);
		System.out.println(ObjectTypeUtil.dump(t));

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

	@Test(enabled=false)
	public void test004Extension() throws Exception {

		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test004Extension");
		Task task = taskManager.getTask(TASK_CYCLE_OID, result);
		AssertJUnit.assertNotNull(task);

		// Test for extension. This will also roughly test extension processor
		// and schema processor
		PropertyContainer taskExtension = task.getExtension();
		AssertJUnit.assertNotNull(taskExtension);
		System.out.println(taskExtension.dump());

		Property shipStateProp = taskExtension
				.findProperty(new QName("http://myself.me/schemas/whatever", "shipState"));
		AssertJUnit.assertEquals("capsized", shipStateProp.getValue(String.class));

		Property deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
		AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getClass());
		AssertJUnit.assertEquals(Integer.valueOf(42), deadProp.getValue(Integer.class));

		List<PropertyModification> mods = new ArrayList<PropertyModification>();
		// One more mariner drowned
		int newDead = deadProp.getValue(Integer.class).intValue() + 1;
		mods.add(deadProp.createModification(PropertyModification.ModificationType.REPLACE, Integer.valueOf(newDead)));
		// ... then the ship was lost
		mods.add(shipStateProp.createModification(PropertyModification.ModificationType.REPLACE, "sunk"));
		// ... so remember the date
		Property dateProp = new Property(new QName("http://myself.me/schemas/whatever", "sinkTimestamp"));
		// This has no type information or schema. The type has to be determined
		// from the java type
		GregorianCalendar sinkDate = new GregorianCalendar();
		mods.add(dateProp.createModification(PropertyModification.ModificationType.REPLACE, sinkDate));

		task.modifyExtension(mods, result);

		// Debug: display the real repository state
		ObjectType o = repositoryService.getObject(ObjectType.class, TASK_CYCLE_OID, null, result);
		System.out.println(ObjectTypeUtil.dump(o));

		// Refresh the task
		task.refresh(result);

		// get the extension again ... and test it ... again
		taskExtension = task.getExtension();
		AssertJUnit.assertNotNull(taskExtension);
		System.out.println(taskExtension.dump());

		shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "shipState"));
		AssertJUnit.assertEquals("sunk", shipStateProp.getValue(String.class));

		deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
		AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getClass());
		AssertJUnit.assertEquals(Integer.valueOf(43), deadProp.getValue(Integer.class));

		dateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever", "sinkTimestamp"));
		AssertJUnit.assertNotNull("sinkTimestamp is null", dateProp);
		AssertJUnit.assertEquals(GregorianCalendar.class, dateProp.getValues().iterator().next().getClass());
		GregorianCalendar fetchedDate = dateProp.getValue(GregorianCalendar.class);
		AssertJUnit.assertTrue(fetchedDate.compareTo(sinkDate) == 0);

	}
	
	@Test(enabled=false)
	public void test005MoreHandlers() throws Exception {
		
		// reset 'has run' flag on handlers
		singleHandler1.resetHasRun();
		singleHandler2.resetHasRun();
		singleHandler3.resetHasRun();
		
		// Add single task. This will get picked by task scanner and executed
		addObjectFromFile(TASK_SINGLE_MORE_HANDLERS_FILENAME);

		// We need to wait for a sync interval, so the task scanner has a chance
		// to pick up this
		// task
		System.out.println("Waiting for task manager to pick up the task and run it");
		Thread.sleep(2000);
		System.out.println("... done");

		// Check task status

		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test005MoreHandlers");
		Task task = taskManager.getTask(TASK_SINGLE_MORE_HANDLERS_OID, result);

		AssertJUnit.assertNotNull(task);
		System.out.println(task.dump());

		ObjectType o = repositoryService.getObject(ObjectType.class, TASK_SINGLE_MORE_HANDLERS_OID, null, result);
		System.out.println(ObjectTypeUtil.dump(o));

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
		
		// Test if all three handlers were run
		
		AssertJUnit.assertTrue(singleHandler1.hasRun());
		AssertJUnit.assertTrue(singleHandler2.hasRun());
		AssertJUnit.assertTrue(singleHandler3.hasRun());
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
					AssertJUnit.assertEquals(value, ((Element)element).getTextContent());
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

	private <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		T objectType = ((JAXBElement<T>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = unmarshallJaxbFromFile(filePath, ObjectType.class);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".addObjectFromFile");
		if (object instanceof TaskType) {
			taskManager.addTask((TaskType) object, result);
		} else {
			repositoryService.addObject(object, result);
		}
		return object;
	}

	private void displayJaxb(Object o, QName qname) throws JAXBException {
		Document doc = DOMUtil.getDocument();
		Element element = JAXBUtil.jaxbToDom(o, qname, doc);
		System.out.println(DOMUtil.serializeDOMToString(element));
	}

	private void display(SearchResultEntry response) {
		// TODO Auto-generated method stub
		System.out.println(response.toLDIFString());
	}

}
