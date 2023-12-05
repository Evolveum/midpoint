/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.util.cleanup.CleanupActionProcessor;
import com.evolveum.midpoint.schema.util.cleanup.CleanupEvent;
import com.evolveum.midpoint.schema.util.cleanup.CleanupPath;
import com.evolveum.midpoint.schema.util.cleanup.CleanupPathAction;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

public class CleanupActionProcessorTest extends AbstractSchemaTest {

    private static final Trace LOG = TraceManager.getTrace(CleanupActionProcessorTest.class);

    private PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }

    @Test
    public void test100SimpleTest() throws Exception {
        final ItemPath CAPABILITY_ACTIVATION = ItemPath.create(
                ResourceType.F_CAPABILITIES,
                CapabilitiesType.F_CONFIGURED,
                CapabilityCollectionType.F_ACTIVATION);

        PrismObject<ResourceType> resource = getPrismContext().parseObject(new File("./src/test/resources/diff/resource-after-const.xml"));

        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)))
                .isNotNull();
        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION)))
                .isNotNull();
        Assertions.assertThat(
                        resource.findItem(CAPABILITY_ACTIVATION))
                .isNotNull();

        LOG.info("BEFORE \n{}", resource.debugDump());

        CleanupActionProcessor processor = new CleanupActionProcessor();
        processor.setPaths(List.of(
                new CleanupPath(SchemaHandlingType.COMPLEX_TYPE, SchemaHandlingType.F_OBJECT_TYPE, CleanupPathAction.REMOVE),
                new CleanupPath(XmlSchemaType.COMPLEX_TYPE, XmlSchemaType.F_DEFINITION, CleanupPathAction.IGNORE),
                new CleanupPath(ResourceType.COMPLEX_TYPE, CAPABILITY_ACTIVATION, CleanupPathAction.ASK)
        ));

        List<CleanupEvent> events = new ArrayList<>();
        processor.setListener(event -> {
            LOG.info("Asking about path {}", event.getPath());
            events.add(event);

            return true;
        });
        processor.process(resource);

        LOG.info("AFTER \n{}", resource.debugDump());

        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)))
                .isNull();
        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION)))
                .isNotNull();
        Assertions.assertThat(resource.findItem(CAPABILITY_ACTIVATION))
                .isNull();

        Assertions.assertThat(events)
                .hasSize(1);
        Assertions.assertThat(events.get(0).getPath())
                .isEqualTo(CAPABILITY_ACTIVATION);
    }
}
