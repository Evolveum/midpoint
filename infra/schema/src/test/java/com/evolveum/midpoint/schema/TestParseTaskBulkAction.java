/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
public class TestParseTaskBulkAction {

	public static final File TASK_FILE = new File("src/test/resources/common/task-bulk-action-1.xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testParseTaskFileToXNode() throws Exception {
		System.out.println("===[ testParseTaskFileToXNode ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		RootXNode node = prismContext.parserFor(TASK_FILE).xml().parseToXNode();

		// THEN
		System.out.println("Parsed task (XNode):");
		System.out.println(node.debugDump());

		System.out.println("XML -> XNode -> JSON:\n" + prismContext.jsonSerializer().serialize(node));
		System.out.println("XML -> XNode -> YAML:\n" + prismContext.yamlSerializer().serialize(node));
	}

	@Test
	public void testParseTaskFile() throws Exception {
		System.out.println("===[ testParseTaskFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<TaskType> task = prismContext.parserFor(TASK_FILE).xml().parse();

		// THEN
		System.out.println("Parsed task:");
		System.out.println(task.debugDump());

		assertTask(task);
	}

    @Test
    public void testParseTaskRoundtrip() throws Exception {
        System.out.println("===[ testParseTaskRoundtrip ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<TaskType> task = prismContext.parseObject(TASK_FILE);

        System.out.println("Parsed task:");
        System.out.println(task.debugDump());

        assertTask(task);

        // SERIALIZE

        String serializedTask = prismContext.serializeObjectToString(task, PrismContext.LANG_XML);

        System.out.println("serialized task:");
        System.out.println(serializedTask);

        // RE-PARSE

		RootXNode reparsedToXNode = prismContext.parserFor(serializedTask).xml().parseToXNode();
		System.out.println("Re-parsed task (to XNode):");
		System.out.println(reparsedToXNode.debugDump());

		// real reparse

		PrismObject<TaskType> reparsedTask = prismContext.parseObject(serializedTask);

        System.out.println("Re-parsed task:");
        System.out.println(reparsedTask.debugDump());

        // Cannot assert here. It will cause parsing of some of the raw values and diff will fail
        assertTask(reparsedTask);

        ObjectDelta<TaskType> objectDelta = task.diff(reparsedTask);
        System.out.println("Delta:");
        System.out.println(objectDelta.debugDump());
        assertTrue("Delta is not empty", objectDelta.isEmpty());

        PrismAsserts.assertEquivalent("Task re-parsed equivalence", task, reparsedTask);
    }


    private void assertTask(PrismObject<TaskType> task) {

		task.checkConsistence();

		assertEquals("Wrong oid", "44444444-4444-4444-4444-000000001111", task.getOid());
		PrismObjectDefinition<TaskType> usedDefinition = task.getDefinition();
		assertNotNull("No task definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "task"),
				TaskType.COMPLEX_TYPE, TaskType.class);
		assertEquals("Wrong class in task", TaskType.class, task.getCompileTimeClass());
		TaskType taskType = task.asObjectable();
		assertNotNull("asObjectable resulted in null", taskType);

		assertPropertyValue(task, "name", PrismTestUtil.createPolyString("Task2"));
		assertPropertyDefinition(task, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

		assertPropertyValue(task, "taskIdentifier", "44444444-4444-4444-4444-000000001111");
		assertPropertyDefinition(task, "taskIdentifier", DOMUtil.XSD_STRING, 0, 1);

		assertPropertyDefinition(task, "executionStatus", JAXBUtil.getTypeQName(TaskExecutionStatusType.class), 1, 1);
		PrismProperty<TaskExecutionStatusType> executionStatusProperty = task.findProperty(TaskType.F_EXECUTION_STATUS);
		PrismPropertyValue<TaskExecutionStatusType> executionStatusValue = executionStatusProperty.getValue();
		TaskExecutionStatusType executionStatus = executionStatusValue.getValue();
		assertEquals("Wrong execution status", TaskExecutionStatusType.RUNNABLE, executionStatus);

		PrismContainer extension = task.getExtension();
		PrismContainerValue extensionValue = extension.getValue();
		assertTrue("Extension parent", extensionValue.getParent() == extension);
		assertNull("Extension ID", extensionValue.getId());

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
