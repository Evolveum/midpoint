/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getSchemaRegistry;

import java.io.File;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestParseTask extends AbstractSchemaTest {

    public static final File TASK_FILE = new File("src/test/resources/common/task-1.xml");

    @Test
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
        task.getValue().applyDefinitionLegacy(taskDef);
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

        assertPropertyValue(task, "name", PrismTestUtil.createPolyString("Example Task"));
        assertPropertyDefinition(task, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        assertPropertyValue(task, "taskIdentifier", "44444444-4444-4444-4444-000000001111");
        assertPropertyDefinition(task, "taskIdentifier", DOMUtil.XSD_STRING, 0, 1);

        assertPropertyDefinition(task, "executionState", JAXBUtil.getTypeQName(TaskExecutionStateType.class), 0, 1);
        PrismProperty<TaskExecutionStateType> executionStateProperty = task.findProperty(TaskType.F_EXECUTION_STATE);
        PrismPropertyValue<TaskExecutionStateType> executionStatusValue = executionStateProperty.getValue();
        TaskExecutionStateType executionState = executionStatusValue.getValue();
        assertEquals("Wrong execution state", TaskExecutionStateType.RUNNABLE, executionState);

        // TODO: more tests

        Object tokenRealValue =
                ((LiveSyncWorkStateType) task.asObjectable().getActivityState().getActivity().getWorkState()).getToken();
        assertThat(tokenRealValue).as("token real value").isEqualTo(1002);

        PrismProperty<Integer> tokenProperty = task.findProperty(
                ItemPath.create(
                        TaskType.F_ACTIVITY_STATE,
                        TaskActivityStateType.F_ACTIVITY,
                        ActivityStateType.F_WORK_STATE,
                        LiveSyncWorkStateType.F_TOKEN));
        // TODO
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

        PrismPropertyDefinition<QueryType> queryDef = getPrismContext().definitionFactory().newPropertyDefinition(
                SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.COMPLEX_TYPE);
        PrismProperty<QueryType> queryProp = queryDef.instantiate();
        queryProp.setRealValue(queryType);

        TaskType taskBean = getPrismContext().createObject(TaskType.class).asObjectable();
        taskBean.setExtension(new ExtensionType(getPrismContext()));
        taskBean.getExtension().asPrismContainerValue().add(queryProp);
        taskBean.setName(PolyStringType.fromOrig("Test task"));

        LiveSyncWorkStateType workState = new LiveSyncWorkStateType(PrismContext.get());
        workState.setToken(111);

        taskBean.beginActivityState()
                .beginActivity()
                .setWorkState(workState);

        String xml = getPrismContext().xmlSerializer().serialize(taskBean.asPrismObject());
        System.out.println("Task serialized:\n" + xml);

        PrismObject<TaskType> taskParsed = getPrismContext().parserFor(xml).parse();

        Object tokenRealValue =
                ((LiveSyncWorkStateType) taskParsed.asObjectable().getActivityState().getActivity().getWorkState()).getToken();
        assertThat(tokenRealValue).as("token real value").isEqualTo(111);

        String xmlSerializedAgain = getPrismContext().xmlSerializer().serialize(taskParsed);
        System.out.println("Task serialized again:\n" + xmlSerializedAgain);
    }
}
