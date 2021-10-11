/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskGroupExecutionLimitationType;

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractTaskManagerTest {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testParsingTaskExecutionLimitations() throws TaskManagerConfigurationException, SchemaException {
        assertLimitationsParsed(" ", emptyList());
        assertLimitationsParsed("_   ", singletonList(new TaskGroupExecutionLimitationType().groupName("_")));
        assertLimitationsParsed("#,   _   ", Arrays.asList(new TaskGroupExecutionLimitationType().groupName("#"), new TaskGroupExecutionLimitationType().groupName("_")));
        assertLimitationsParsed(":    0", singletonList(new TaskGroupExecutionLimitationType().groupName("").limit(0)));
        assertLimitationsParsed("_:0", singletonList(new TaskGroupExecutionLimitationType().groupName("_").limit(0)));
        assertLimitationsParsed("_:*", singletonList(new TaskGroupExecutionLimitationType().groupName("_").limit(null)));
        assertLimitationsParsed("admin-node : 2 , sync-jobs    : 4    ", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4)));
        assertLimitationsParsed("admin-node:2,sync-jobs:4,#:0,_:0,*:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName("#").limit(0),
                new TaskGroupExecutionLimitationType().groupName("_").limit(0),
                new TaskGroupExecutionLimitationType().groupName("*").limit(null)));
    }

    @Test
    public void testComputingLimitations() throws TaskManagerConfigurationException, SchemaException {
        assertLimitationsComputed("", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:1", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(1),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("#,_", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed(":0", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:0", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("admin-node:2,sync-jobs:4", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("admin-node:2,sync-jobs:4,#:0,_:0,*:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName("NODE").limit(0),
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("*")));
    }

    private void assertLimitationsParsed(String value, List<TaskGroupExecutionLimitationType> expected) throws TaskManagerConfigurationException, SchemaException {
        TaskExecutionLimitationsType parsed = TaskManagerConfiguration.parseExecutionLimitations(value);
        displayValue("parsed value of '" + value + "'", serialize(parsed));
        assertEquals("Wrong parsed value for '" + value + "'", expected, parsed.getGroupLimitation());
    }

    private void assertLimitationsComputed(String value, List<TaskGroupExecutionLimitationType> expected) throws TaskManagerConfigurationException, SchemaException {
        TaskExecutionLimitationsType parsed = TaskManagerConfiguration.parseExecutionLimitations(value);
        displayValue("parsed value of '" + value + "'", serialize(parsed));
        TaskExecutionLimitationsType computed = NodeRegistrar.computeTaskExecutionLimitations(parsed, "NODE");
        assertEquals("Wrong computed value for '" + value + "'", expected, computed.getGroupLimitation());
    }

    private String serialize(TaskExecutionLimitationsType parsed) throws SchemaException {
        return getPrismContext().xmlSerializer().serializeRealValue(parsed, new QName(SchemaConstants.NS_C, "value"));
    }
}
