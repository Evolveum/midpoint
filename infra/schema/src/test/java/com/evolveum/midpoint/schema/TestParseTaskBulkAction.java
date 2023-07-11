/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import java.io.File;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestParseTaskBulkAction extends AbstractSchemaTest {

    private static final File TASK_FILE = new File("src/test/resources/common/task-bulk-action-1.xml");

    @Test
    public void testParseTaskFileToXNode() throws Exception {
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
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<TaskType> task = prismContext.parseObject(TASK_FILE);

        System.out.println("Parsed task:");
        System.out.println(task.debugDump());

        assertTask(task);

        // SERIALIZE

        String serializedTask = prismContext.xmlSerializer().serialize(task);

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

        assertPropertyDefinition(task, "executionState", JAXBUtil.getTypeQName(TaskExecutionStateType.class), 0, 1);
        PrismProperty<TaskExecutionStateType> executionStateProperty = task.findProperty(TaskType.F_EXECUTION_STATE);
        PrismPropertyValue<TaskExecutionStateType> executionStateValue = executionStateProperty.getValue();
        TaskExecutionStateType executionState = executionStateValue.getValue();
        assertEquals("Wrong execution state", TaskExecutionStateType.RUNNABLE, executionState);

    }

    private void assertPropertyDefinition(
            PrismContainer<?> container, String propName, QName xsdType, int minOccurs, int maxOccurs) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(
            PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

}
