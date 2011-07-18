/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.impl;


import static org.junit.Assert.*;

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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opends.server.types.Attribute;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * @author Radovan Semancik
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-task.xml", "classpath:application-context-task-test.xml" })
public class TestTaskManagerContract {
	
	private static final String TASK_CYCLE_FILENAME = "src/test/resources/repo/cycle-task.xml";
	private static final String TASK_CYCLE_OID = "91919191-76e0-59e2-86d6-998877665544";

	private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
	
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
	@Before
	public void initRepository() throws Exception {
		if (!repoInitialized) {
			//addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME);
			repoInitialized = true;
		}
	}
	
	@PostConstruct
	public void initHandlers() {
		MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler();
		taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);
	}

	/**
	 * Test integrity of the test setup.
	 * 
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 */
	@Test
	public void test000Integrity() {
		assertNotNull(repositoryService);
		assertNotNull(taskManager);

//		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName() + ".test000Integrity");
//		ObjectType object = repositoryService.getObject(RESOURCE_OPENDJ_OID, null, result);
//		assertTrue(object instanceof ResourceType);
//		assertEquals(RESOURCE_OPENDJ_OID, object.getOid());
	}
	
	@Test
	public void test002Cycle() throws Exception {
		// Add cycle task. This will get picked by task scanner and executed
		addObjectFromFile(TASK_CYCLE_FILENAME);
		
		// We need to wait for a sync interval, so the task scanner has a chance to pick up this
		// task
		System.out.println("Waining for task manager to pick up the task");
		Thread.sleep(10000);
		System.out.println("... done");
		
		// Check task status
		
		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName()+".test002Cycle");
		Task task = taskManager.getTask(TASK_CYCLE_OID, result);
		
		assertNotNull(task);
		System.out.println(task.dump());
		
		ObjectType o = repositoryService.getObject(TASK_CYCLE_OID,null, result);
		System.out.println(ObjectTypeUtil.dump(o));
		
		// .. it should be running
		assertEquals(TaskExecutionStatus.RUNNING,task.getExecutionStatus());
		
		// .. and claimed
		assertEquals(TaskExclusivityStatus.CLAIMED,task.getExclusivityStatus());
		
		// .. and last run should not be zero
		assertNotNull(task.getLastRunStartTimestamp());
		assertFalse(task.getLastRunStartTimestamp().longValue()==0);
		assertNotNull(task.getLastRunFinishTimestamp());
		assertFalse(task.getLastRunFinishTimestamp().longValue()==0);

		// The progress should be more than 0 as the task has run at least once
		assertTrue(task.getProgress()>0);
		
		// Test for presence of a result. It should be there and it should indicate success
		OperationResult taskResult = task.getResult();
		assertNotNull(taskResult);		
		assertTrue(taskResult.isSuccess());
	}
	
	@Test
	public void test003Extension() throws Exception {
		
		OperationResult result = new OperationResult(TestTaskManagerContract.class.getName()+".test003Extension");
		Task task = taskManager.getTask(TASK_CYCLE_OID, result);
		assertNotNull(task);
		
		// Test for extension. This will also roughly test extension processor and schema processor
		PropertyContainer taskExtension = task.getExtension();
		assertNotNull(taskExtension);
		System.out.println(taskExtension.dump());
		
		Property shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","shipState"));
		assertEquals("capsized",shipStateProp.getValue(String.class));
		
		Property deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","dead"));
		assertEquals(Integer.class,deadProp.getValues().iterator().next().getClass());
		assertEquals(Integer.valueOf(42),deadProp.getValue(Integer.class));
		
		List<PropertyModification> mods = new ArrayList<PropertyModification>();
		// One more mariner drowned
		int newDead = deadProp.getValue(Integer.class).intValue()+1;
		mods.add(deadProp.createModification(PropertyModification.ModificationType.REPLACE, Integer.valueOf(newDead)));
		// ... then the ship was lost
		mods.add(shipStateProp.createModification(PropertyModification.ModificationType.REPLACE, "sunk"));
		// ... so remember the date
		Property dateProp = new Property(new QName("http://myself.me/schemas/whatever","sinkTimestamp"));
		// This has no type information or schema. The type has to be determined from the java type
		GregorianCalendar sinkDate = new GregorianCalendar();
		mods.add(dateProp.createModification(PropertyModification.ModificationType.REPLACE, sinkDate));
		
		task.modifyExtension(mods, result);
		
		// Debug: display the real repository state
		ObjectType o = repositoryService.getObject(TASK_CYCLE_OID,null, result);
		System.out.println(ObjectTypeUtil.dump(o));
		
		// Refresh the task
		task.refresh(result);
		
		// get the extension again ... and test it ... again
		taskExtension = task.getExtension();
		assertNotNull(taskExtension);
		System.out.println(taskExtension.dump());
		
		shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","shipState"));
		assertEquals("sunk",shipStateProp.getValue(String.class));
		
		deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","dead"));
		assertEquals(Integer.class,deadProp.getValues().iterator().next().getClass());
		assertEquals(Integer.valueOf(43),deadProp.getValue(Integer.class));
		
		dateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","sinkTimestamp"));
		assertEquals(GregorianCalendar.class,dateProp.getValues().iterator().next().getClass());
		GregorianCalendar fetchedDate = dateProp.getValue(GregorianCalendar.class);
		assertTrue(fetchedDate.compareTo(sinkDate)==0);
		
	}
	
	// UTILITY METHODS
	
	// TODO: maybe we should move them to a common utility class
	
	private void assertAttribute(AccountShadowType repoShadow, ResourceType resource, String name, String value) {
		assertAttribute(repoShadow,new QName(resource.getNamespace(),name),value);
	}
		
	private void assertAttribute(AccountShadowType repoShadow, QName name, String value) {
		boolean found = false;
		List<Element> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Element element : xmlAttributes) {
			if (element.getNamespaceURI().equals(name.getNamespaceURI())
					&& element.getLocalName().equals(name.getLocalPart())) {
				if (found) {
					fail("Multiple values for "+name+" attribute in shadow attributes");
				} else {
					assertEquals(value,element.getTextContent());
					found = true;
				}
			}
		}
	}

	protected void assertAttribute(SearchResultEntry response, String name, String value) {
		Assert.assertNotNull(response.getAttribute(name.toLowerCase()));
		Assert.assertEquals(1, response.getAttribute(name.toLowerCase()).size());
		Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
		Assert.assertEquals(value, attribute.iterator().next().getValue().toString());
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
		repositoryService.addObject(object, result);
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
