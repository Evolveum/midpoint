/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public class TestObjectConstruction extends AbstractSchemaTest {

    @Test
    public void testUserConstruction() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<UserType> user = prismContext.createObject(UserType.class);

        // THEN
        assertNotNull(user);
        SchemaTestUtil.assertUserDefinition(user.getDefinition());
    }

    @Test
    public void testObjectTypeConstruction() {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        try {
            // WHEN
            prismContext.createObject(ObjectType.class);

            fail("unexpected success");
        } catch (SchemaException e) {
            // This is expected, abstract object types cannot be instantiated
            assertTrue(e.getMessage().contains("abstract"));
        }
    }
}
