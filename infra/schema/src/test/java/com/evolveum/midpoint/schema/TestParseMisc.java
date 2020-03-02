/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestParseMisc extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");

    protected static final File ROLE_FILTERS_FILE = new File(TEST_DIR, "role-filters.xml");
    protected static final String ROLE_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";

    @Test
    public void testParseRoleFilters() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<RoleType> object = prismContext.parseObject(ROLE_FILTERS_FILE);

        // THEN
        System.out.println("Parsed object:");
        System.out.println(object.debugDump());

        assertEquals("Wrong oid", ROLE_FILTERS_OID, object.getOid());
        PrismObjectDefinition<RoleType> objectDefinition = object.getDefinition();
        assertNotNull("No object definition", objectDefinition);
        QName elementName = new QName(SchemaConstantsGenerated.NS_COMMON, "role");
        PrismAsserts.assertObjectDefinition(objectDefinition, elementName,
                RoleType.COMPLEX_TYPE, RoleType.class);
        assertEquals("Wrong class", RoleType.class, object.getCompileTimeClass());
        assertEquals("Wrong object item name", elementName, object.getElementName());
        RoleType objectType = object.asObjectable();
        assertNotNull("asObjectable resulted in null", objectType);

        assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Role with import filters"));
        assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        List<AssignmentType> inducements = objectType.getInducement();
        assertEquals("Wrong number of inducements", 2, inducements.size());
    }

    private void assertPropertyDefinition(PrismContainer<?> container,
            String propName, QName xsdType, int minOccurs, int maxOccurs) {
        QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }
}
