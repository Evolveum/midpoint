/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getSchemaRegistry;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestParseTask extends AbstractSchemaTest {

    public static final File TASK_FILE = new File("src/test/resources/common/task-1.xml");

    @Test(enabled = false)
    public void testParseTaskFile() throws Exception {
        // GIVEN
        PrismContext prismContext = getPrismContext();

        // WHEN
        PrismObject<TaskType> task = prismContext.parserFor(TASK_FILE).xml().parse();

        // THEN
        System.out.println("Parsed task:");
        System.out.println(task.debugDump());

        assertTask(task);

        PrismObjectDefinition<TaskType> taskDef = getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
        task.applyDefinition(taskDef);
        task.getValue().applyDefinition(taskDef);
    }

    @Test(enabled = false)
    public void testParseTaskDom() throws SchemaException {
        // GIVEN
        PrismContext prismContext = getPrismContext();

        Document document = DOMUtil.parseFile(TASK_FILE);
        Element taskElement = DOMUtil.getFirstChildElement(document);

        // WHEN
        PrismObject<TaskType> task = prismContext.parserFor(taskElement).parse();

        // THEN
        System.out.println("Parsed task:");
        System.out.println(task.debugDump());

        assertTask(task);
    }

    private void assertTask(PrismObject<TaskType> task) {

        task.checkConsistence();

        assertEquals("Wrong oid", "44444444-4444-4444-4444-000000001111", task.getOid());
//        assertEquals("Wrong version", "42", user.getVersion());
        PrismObjectDefinition<TaskType> usedDefinition = task.getDefinition();
        assertNotNull("No task definition", usedDefinition);
        PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "task"),
                TaskType.COMPLEX_TYPE, TaskType.class);
        assertEquals("Wrong class in task", TaskType.class, task.getCompileTimeClass());
        TaskType taskType = task.asObjectable();
        assertNotNull("asObjectable resulted in null", taskType);

        assertPropertyValue(task, "name", PrismTestUtil.createPolyString("Example Task"));
        assertPropertyDefinition(task, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        assertPropertyValue(task, "taskIdentifier", "44444444-4444-4444-4444-000000001111");
        assertPropertyDefinition(task, "taskIdentifier", DOMUtil.XSD_STRING, 0, 1);

        assertPropertyDefinition(task, "executionStatus", JAXBUtil.getTypeQName(TaskExecutionStatusType.class), 1, 1);
        PrismProperty<TaskExecutionStatusType> executionStatusProperty = task.findProperty(TaskType.F_EXECUTION_STATUS);
        PrismPropertyValue<TaskExecutionStatusType> executionStatusValue = executionStatusProperty.getValue();
        TaskExecutionStatusType executionStatus = executionStatusValue.getValue();
        assertEquals("Wrong execution status", TaskExecutionStatusType.RUNNABLE, executionStatus);

        // TODO: more tests

//        PrismContainer extension = user.getExtension();
//        assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
//        PrismContainerValue extensionValue = extension.getValue();
//        assertTrue("Extension parent", extensionValue.getParent() == extension);
//        assertNull("Extension ID", extensionValue.getId());

//        PropertyPath enabledPath = new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED);
//        PrismProperty enabledProperty1 = task.findProperty(enabledPath);
//        PrismAsserts.assertDefinition(enabledProperty1.getDefinition(), ActivationType.F_ENABLED, DOMUtil.XSD_BOOLEAN, 0, 1);
//        assertNotNull("Property "+enabledPath+" not found", enabledProperty1);
//        PrismAsserts.assertPropertyValue(enabledProperty1, true);

//        PrismProperty validFromProperty = user.findProperty(new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_VALID_FROM));
//        assertNotNull("Property "+ActivationType.F_VALID_FROM+" not found", validFromProperty);
//        PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);

//        PrismReference accountRef = task.findReference(UserType.F_ACCOUNT_REF);
//        assertEquals("Wrong number of accountRef values", 3, accountRef.getValues().size());
//        PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000001111");
//        PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000002222");
//        PrismAsserts.assertReferenceValue(accountRef, "2f9b9299-6f45-498f-aaaa-000000003333");
    }

    private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
            int maxOccurs) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

    @Test
    public static void testSerializeTask() throws Exception {
        ObjectQuery query = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
                .build();

        QueryType queryType = getPrismContext().getQueryConverter().createQueryType(query);

        PrismPropertyDefinition queryDef = getPrismContext().definitionFactory().createPropertyDefinition(
                SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.COMPLEX_TYPE);
        PrismProperty<QueryType> queryProp = queryDef.instantiate();
        queryProp.setRealValue(queryType);

        TaskType taskType = getPrismContext().createObject(TaskType.class).asObjectable();
        taskType.setExtension(new ExtensionType(getPrismContext()));
        taskType.getExtension().asPrismContainerValue().add(queryProp);
        taskType.setName(PolyStringType.fromOrig("Test task"));

        String xml = getPrismContext().xmlSerializer().serialize(taskType.asPrismObject());
        System.out.println("Task serialized:\n" + xml);

        PrismObject<TaskType> taskParsed = getPrismContext().parserFor(xml).parse();
        String xmlSerializedAgain = getPrismContext().xmlSerializer().serialize(taskParsed);
        System.out.println("Task serialized again:\n" + xmlSerializedAgain);
    }

}
