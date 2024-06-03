/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.evolveum.midpoint.prism.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

public class ObjectCleanerTest extends AbstractUnitTest {

    private static final Trace LOG = TraceManager.getTrace(ObjectCleanerTest.class);

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

    @Test()
    public void test100Resource() throws Exception {
        final ItemPath CAPABILITY_ACTIVATION = ItemPath.create(
                ResourceType.F_CAPABILITIES,
                CapabilitiesType.F_CONFIGURED,
                CapabilityCollectionType.F_ACTIVATION);

        File file = new File(TEST_DIR, "resource.xml");

        PrismObject<ResourceType> resource = getPrismContext().parseObject(file);

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

        ObjectCleaner processor = new ObjectCleaner();
        processor.setPaths(List.of(
                new CleanupPath(SchemaHandlingType.COMPLEX_TYPE, SchemaHandlingType.F_OBJECT_TYPE, CleanupPathAction.REMOVE),
                new CleanupPath(XmlSchemaType.COMPLEX_TYPE, XmlSchemaType.F_DEFINITION, CleanupPathAction.IGNORE),
                new CleanupPath(ResourceType.COMPLEX_TYPE, CAPABILITY_ACTIVATION, CleanupPathAction.ASK)
        ));

        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
        CleanupResult result = processor.process(resource);

        LOG.info("AFTER \n{}", resource.debugDump());

        SearchFilterType filter = resource.asObjectable().getConnectorRef().getFilter();
        Assertions.assertThat(filter.getText())
                .isEqualTo("connectorType = 'testconnector' and connectorVersion = '99.0' and available = true");
        PrismNamespaceContext ctx = filter.getFilterClauseXNode().namespaceContext();

        // FIXME: This assertion should take into account that prefixes could come from different place.
        // Assertions.assertThat(ctx.localPrefixes().size()).isEqualTo(1);

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
        Assertions.assertThat(listener.getOptionalCleanupEvents().get(0).path())
                .isEqualTo(CAPABILITY_ACTIVATION);
    }

    @Test
    public void test200User() throws Exception {
        File file = new File(TEST_DIR, "user.xml");
        PrismObject<ResourceType> user = getPrismContext().parseObject(file);

        Assertions.assertThat(user.findItem(UserType.F_METADATA))
                .isNotNull();
        Assertions.assertThat(user.findItem(UserType.F_OPERATION_EXECUTION))
                .isNotNull();

        ObjectCleaner processor = new ObjectCleaner();
        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
        processor.process(user);

        Assertions.assertThat(user.findItem(UserType.F_METADATA))
                .isNull();
        Assertions.assertThat(user.findItem(UserType.F_OPERATION_EXECUTION))
                .isNull();
    }

    @Test
    public void test300ResourceObjectType() throws Exception {
        File file = new File(TEST_DIR, "resource.xml");
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        PrismObject<ResourceType> resource = getPrismContext().parseObject(file);

        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)))
                .isNotNull();

        PrismContainer<ResourceObjectTypeDefinitionType> container =
                resource.findContainer(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));

        LOG.info("BEFORE \n{}", container.debugDump());

        ObjectCleaner processor = new ObjectCleaner();
        processor.setRemoveContainerIds(true);
        processor.setPaths(List.of(
                new CleanupPath(ResourceObjectFocusSpecificationType.COMPLEX_TYPE, ResourceObjectFocusSpecificationType.F_TYPE, CleanupPathAction.REMOVE)
        ));

        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
        processor.process(container);

        LOG.info("AFTER \n{}", container.debugDump());

        Assertions.assertThat(
                        container.getValue().findItem(
                                ItemPath.create(ResourceObjectTypeDefinitionType.F_FOCUS, ResourceObjectFocusSpecificationType.F_TYPE)))
                .isNull();

        ResourceObjectTypeDefinitionType rObjectType = container.getValue().asContainerable();
        SynchronizationReactionsType synchronization = rObjectType.getSynchronization();
        AssertJUnit.assertNotNull(synchronization);

        AssertJUnit.assertEquals(2, synchronization.getReaction().size());
        synchronization.getReaction().forEach(reaction -> {
            SynchronizationActionsType actions = reaction.getActions();
            AssertJUnit.assertNotNull(actions);

            if ("unmatched-add".equals(reaction.getName())) {
                AssertJUnit.assertEquals(1, actions.getAddFocus().size());
            } else if ("linked-synchronize".equals(reaction.getName())) {
                AssertJUnit.assertEquals(1, actions.getSynchronize().size());
            }
        });
    }

    @Test
    public void test400ResourceCapabilities() throws Exception {
        File file = new File(TEST_DIR, "resource.xml");

        PrismObject<ResourceType> resource = getPrismContext().parseObject(file);

        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CACHING_METADATA)))
                .isNotNull();

        PrismContainer<CapabilitiesType> container = resource.findContainer(ResourceType.F_CAPABILITIES);

        @NotNull PrismContainerValue<CapabilitiesType> value = container.getValue();

        LOG.info("BEFORE \n{}", value.debugDump());

        ObjectCleaner processor = new ObjectCleaner();
        TestCleanupListener listener = new TestCleanupListener();
        processor.setListener(listener);
        processor.process(container);

        LOG.info("AFTER \n{}", value.debugDump());

        Assertions.assertThat(
                        resource.findItem(
                                ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CACHING_METADATA)))
                .isNull();
    }
}
