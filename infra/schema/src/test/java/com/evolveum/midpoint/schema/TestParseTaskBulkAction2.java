/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class TestParseTaskBulkAction2 extends AbstractSchemaTest {

    private static final File TASK_FILE = new File("src/test/resources/common/task-bulk-action-2.xml");

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

        task.checkConsistence();

        ExecuteScriptType executeScript = task.asObjectable()
                .getActivity()
                .getWork()
                .getNonIterativeScripting()
                .getScriptExecutionRequest();
        Object o = executeScript.getInput().getValue().get(0);
        System.out.println(o);
        assertTrue("Raw value is not parsed", o instanceof RawType && ((RawType) o).getAlreadyParsedValue() != null);
    }

    @Test
    public void testParseTaskRoundtrip() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<TaskType> task = prismContext.parseObject(TASK_FILE);

        System.out.println("Parsed task:");
        System.out.println(task.debugDump());

        task.checkConsistence();

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
        reparsedTask.checkConsistence();

        ObjectDelta<TaskType> objectDelta = task.diff(reparsedTask);
        System.out.println("Delta:");
        System.out.println(objectDelta.debugDump());
        assertTrue("Delta is not empty", objectDelta.isEmpty());

        PrismAsserts.assertEquivalent("Task re-parsed equivalence", task, reparsedTask);

        ExecuteScriptType executeScript = reparsedTask.asObjectable()
                .getActivity()
                .getWork()
                .getNonIterativeScripting()
                .getScriptExecutionRequest();
        Object o = executeScript.getInput().getValue().get(0);
        System.out.println(o);
        assertTrue("Raw value is not parsed", o instanceof RawType && ((RawType) o).getAlreadyParsedValue() != null);
    }
}
