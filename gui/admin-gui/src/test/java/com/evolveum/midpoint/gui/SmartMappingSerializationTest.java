/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table.SmartMappingTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Checks that the mapping table and its helpers keep their references after serialization.
 * Using records for the helpers previously left these references null, causing NPEs.
 */
public class SmartMappingSerializationTest {

    @DataProvider
    public Object[][] serializationRoots() {
        return new Object[][] { { "actions" }, { "columns" }, { "menus" } };
    }

    @Test(dataProvider = "serializationRoots")
    public void preservesHelperBackReferences(String rootField) throws Exception {
        WicketTester tester = new WicketTester();
        try {
            TestMappingTable table = new TestMappingTable();
            Serializable root = (Serializable) field(table, SmartMappingTable.class, rootField);
            var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
            byte[] bytes = serializer.serialize(root);
            assertNotNull(bytes, "The helper graph must serialize");
            Object restored = serializer.deserialize(bytes);
            assertNotNull(restored, "The helper graph must deserialize");

            var restoredTable = (SmartMappingTable<?>) field(restored, restored.getClass(), "table");
            assertSame(field(restoredTable, SmartMappingTable.class, rootField), restored,
                    "The table must retain its reference to the helper used as the serialization root");
            for (String helperName : new String[] { "actions", "columns", "menus" }) {
                Object helper = field(restoredTable, SmartMappingTable.class, helperName);
                assertNotNull(helper, helperName + " must survive deserialization");
                assertSame(field(helper, helper.getClass(), "table"), restoredTable);
            }
            Object menus = field(restoredTable, SmartMappingTable.class, "menus");
            assertSame(field(menus, menus.getClass(), "actions"),
                    field(restoredTable, SmartMappingTable.class, "actions"));
        } finally {
            tester.destroy();
        }
    }

    private static Object field(Object object, Class<?> declaringClass, String name) throws Exception {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static final class TestMappingTable extends SmartMappingTable<ResourceObjectTypeDefinitionType> {
        private TestMappingTable() {
            super("mappings", Model.of(MappingDirection.INBOUND), Model.of(true), Model.of(), "");
        }

        @Override
        protected ResourceType getResourceType() {
            return new ResourceType();
        }
    }
}
