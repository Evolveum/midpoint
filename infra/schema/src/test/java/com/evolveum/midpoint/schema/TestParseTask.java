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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class TestParseTask {
	
	public static final File TASK_FILE = new File("src/test/resources/common/task-1.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseTaskFile() throws SchemaException {
		System.out.println("===[ testParseTaskFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<TaskType> task = prismContext.parseObject(TASK_FILE);
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(task.dump());
		
		assertTask(task);
	}

	@Test
	public void testParseTaskDom() throws SchemaException {
		System.out.println("===[ testParseTaskDom ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(TASK_FILE);
		Element taskElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<TaskType> task = prismContext.parseObject(taskElement);
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(task.dump());
		
		assertTask(task);
	}

	@Test
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		TaskType taskType = jaxbProcessor.unmarshalObject(TASK_FILE, TaskType.class);
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(taskType.asPrismObject().dump());
		
		assertTask(taskType.asPrismObject());
	}
	
	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 */
	@Test
	public void testPrismParseJaxbObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ObjectType taskType = jaxbProcessor.unmarshalObject(TASK_FILE, ObjectType.class);
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(taskType.asPrismObject().dump());
		
		assertTask(taskType.asPrismObject());
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
	@Test
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElement ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<TaskType> jaxbElement = jaxbProcessor.unmarshalElement(TASK_FILE, TaskType.class);
		TaskType taskType = jaxbElement.getValue();
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(taskType.asPrismObject().dump());
		
		assertTask(taskType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
	@Test
	public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(TASK_FILE, ObjectType.class);
		ObjectType taskType = jaxbElement.getValue();
		
		// THEN
		System.out.println("Parsed task:");
		System.out.println(taskType.asPrismObject().dump());
		
		assertTask(taskType.asPrismObject());
	}

	
	private void assertTask(PrismObject<TaskType> task) {
		
		task.checkConsistence();
		
		assertEquals("Wrong oid", "44444444-4444-4444-4444-000000001111", task.getOid());
//		assertEquals("Wrong version", "42", user.getVersion());
		PrismObjectDefinition<TaskType> usedDefinition = task.getDefinition();
		assertNotNull("No task definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "task"),
				TaskType.COMPLEX_TYPE, TaskType.class);
		assertEquals("Wrong class in task", TaskType.class, task.getCompileTimeClass());
		TaskType taskType = task.asObjectable();
		assertNotNull("asObjectable resulted in null", taskType);

		assertPropertyValue(task, "name", "Example Task");
		assertPropertyDefinition(task, "name", DOMUtil.XSD_STRING, 0, 1);
		
		assertPropertyValue(task, "taskIdentifier", "44444444-4444-4444-4444-000000001111");
		assertPropertyDefinition(task, "taskIdentifier", DOMUtil.XSD_STRING, 0, 1);
		
		assertPropertyDefinition(task, "executionStatus", JAXBUtil.getTypeQName(TaskExecutionStatusType.class), 1, 1);
		PrismProperty<TaskExecutionStatusType> executionStatusProperty = task.findProperty(TaskType.F_EXECUTION_STATUS);
		PrismPropertyValue<TaskExecutionStatusType> executionStatusValue = executionStatusProperty.getValue();
		TaskExecutionStatusType executionStatus = executionStatusValue.getValue();
		assertEquals("Wrong execution status", TaskExecutionStatusType.RUNNABLE, executionStatus);
		
		assertPropertyDefinition(task, "exclusivityStatus", JAXBUtil.getTypeQName(TaskExclusivityStatusType.class), 1, 1);
		
		// TODO: more tests
		
//		PrismContainer extension = user.getExtension();
//		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
//		PrismContainerValue extensionValue = extension.getValue();
//		assertTrue("Extension parent", extensionValue.getParent() == extension);
//		assertNull("Extension ID", extensionValue.getId());
		
//		PropertyPath enabledPath = new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED);
//		PrismProperty enabledProperty1 = task.findProperty(enabledPath);
//		PrismAsserts.assertDefinition(enabledProperty1.getDefinition(), ActivationType.F_ENABLED, DOMUtil.XSD_BOOLEAN, 0, 1);
//		assertNotNull("Property "+enabledPath+" not found", enabledProperty1);
//		PrismAsserts.assertPropertyValue(enabledProperty1, true);
		
//		PrismProperty validFromProperty = user.findProperty(new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_VALID_FROM));
//		assertNotNull("Property "+ActivationType.F_VALID_FROM+" not found", validFromProperty);
//		PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);
		
//		PrismReference accountRef = task.findReference(UserType.F_ACCOUNT_REF);
//		assertEquals("Wrong number of accountRef values", 3, accountRef.getValues().size());
//		PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000001111");
//		PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000002222");
//		PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000003333");
	}
	
	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

}
