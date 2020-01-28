/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Various tests related to schema immutability, definition cloning, etc.
 *
 */
public class TestSchemaImmutability {

    private static final File TEST_DIR = new File("src/test/resources/schema-immutability");
    private static final File RESOURCE_DUMMY_VAULT_FILE = new File(TEST_DIR, "resource-dummy-vault.xml");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testUltraDeepCloning() throws Exception {
        System.out.println("===[ testUltraDeepCloning ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_DUMMY_VAULT_FILE);
        System.out.println("ResourceType definition:\n" + resource.getDefinition().debugDump());
        PrismAsserts.assertImmutable(resource.getDefinition());

        PrismContainer<?> schemaHandling = resource.findContainer(ResourceType.F_SCHEMA_HANDLING);
        assertNotNull("No schema handling", schemaHandling);
        assertNotNull("No schema handling definition", schemaHandling.getDefinition());
        System.out.println("schemaHandling definition:\n" + schemaHandling.getDefinition().debugDump());
        PrismAsserts.assertImmutable(schemaHandling.getDefinition());

        Item<?,?> testConnection = resource.findItem(ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_NATIVE, "testConnection"));
        assertNotNull("No testConnection capability", testConnection);
        assertNotNull("No testConnection capability definition", testConnection.getDefinition());
        System.out.println("testConnection capability definition:\n" + testConnection.getDefinition().debugDump());
        PrismAsserts.assertImmutable(testConnection.getDefinition());

        // WHEN
        PrismObjectDefinition<ResourceType> deepCloneResult = resource.deepCloneDefinition(true, null);
        System.out.println("Definition from deep clone:\n" + deepCloneResult.debugDump());

        // THEN
        PrismAsserts.assertMutable(deepCloneResult);

        System.out.println("Updated ResourceType definition:\n" + resource.getDefinition().debugDump());
        PrismAsserts.assertMutable(resource.getDefinition());

        PrismContainer<?> updatedSchemaHandling = resource.findContainer(ResourceType.F_SCHEMA_HANDLING);
        assertNotNull("No updated schema handling", updatedSchemaHandling);
        assertNotNull("No updated schema handling definition", updatedSchemaHandling.getDefinition());
        System.out.println("Updated schemaHandling definition:\n" + updatedSchemaHandling.getDefinition().debugDump());
        PrismAsserts.assertMutable(updatedSchemaHandling.getDefinition());

        Item<?,?> updatedTestConnection = resource.findItem(ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_NATIVE, "testConnection"));
        assertNotNull("No updated testConnection capability", updatedTestConnection);
        assertNotNull("No updated testConnection capability definition", updatedTestConnection.getDefinition());
        System.out.println("Updated testConnection capability definition:\n" + updatedTestConnection.getDefinition().debugDump());
        PrismAsserts.assertMutable(updatedTestConnection.getDefinition());
    }
}
