/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

public class CleanupActionProcessorTest extends AbstractUnitTest {

    private static final Trace LOG = TraceManager.getTrace(CleanupActionProcessorTest.class);

    private static final File TEST_DIR = new File("./src/test/resources/cleanup");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        SchemaDebugUtil.initialize(); // Make sure the pretty printer is activated
    }

    private PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }

    @Test
    public void test100Resource() throws Exception {
        final ItemPath CAPABILITY_ACTIVATION = ItemPath.create(
                ResourceType.F_CAPABILITIES,
                CapabilitiesType.F_CONFIGURED,
                CapabilityCollectionType.F_ACTIVATION);

        PrismObject<ResourceType> resource = getPrismContext().parseObject(new File(TEST_DIR, "resource.xml"));

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

        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
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

        Assertions.assertThat(listener.getOptionalCleanupEvents())
                .hasSize(1);
        Assertions.assertThat(listener.getOptionalCleanupEvents().get(0).getPath())
                .isEqualTo(CAPABILITY_ACTIVATION);
    }

    @Test
    public void test200User() throws Exception {
        PrismObject<ResourceType> user = getPrismContext().parseObject(new File(TEST_DIR, "user.xml"));

        CleanupActionProcessor processor = new CleanupActionProcessor();
        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
        processor.process(user);

        Assertions.assertThat(listener.getProtectedStringCleanupEvents())
                .hasSize(1);
        Assertions.assertThat(listener.getProtectedStringCleanupEvents().get(0).getPath())
                .isEqualTo(ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE));
    }
}
