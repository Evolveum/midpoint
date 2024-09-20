/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.List;

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

    @Test
    public void testUserDefinitionForOperationalAndAlwaysUseForEquals() {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);

        PrismContainerDefinition<PasswordType> pwdDef = userDef.findContainerDefinition(ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD));
        assertNotNull(pwdDef);
        assertFalse("definition should not be operational", pwdDef.isOperational());
        assertFalse("definition should not always use equals", pwdDef.isAlwaysUseForEquals());
        assertFalse(pwdDef.isAlwaysUseForEquals(PasswordType.F_SEQUENCE_IDENTIFIER));
        //assertNull(pwdDef.getAnnotation(PrismConstants.A_ALWAYS_USE_FOR_EQUALS));

        PrismPropertyDefinition seqIdentifierDef = pwdDef.findPropertyDefinition(PasswordType.F_SEQUENCE_IDENTIFIER);
        assertTrue("definition should be operational", seqIdentifierDef.isOperational());
        assertFalse("definition should always use equals", seqIdentifierDef.isAlwaysUseForEquals());

        PrismContainerDefinition<AuthenticationBehavioralDataType> authDef = userDef.findContainerDefinition(
                ItemPath.create(UserType.F_BEHAVIOR, BehaviorType.F_AUTHENTICATION));
        assertNotNull("No AuthenticationBehavioralDataType definition", authDef);
        assertFalse("definition should not be operational", authDef.isOperational());
        assertFalse("definition should not always use equals", authDef.isAlwaysUseForEquals());

//        List<QName> qnames = authDef.getAnnotation(PrismConstants.A_ALWAYS_USE_FOR_EQUALS);
//        assertNotNull(qnames);
//        assertEquals(1, qnames.size());
//        assertEquals(AuthenticationBehavioralDataType.F_SEQUENCE_IDENTIFIER, qnames.get(0));

        assertTrue(authDef.isAlwaysUseForEquals(AuthenticationBehavioralDataType.F_SEQUENCE_IDENTIFIER));

        seqIdentifierDef = authDef.findPropertyDefinition(AuthenticationBehavioralDataType.F_SEQUENCE_IDENTIFIER);
        assertTrue("definition should be operational", seqIdentifierDef.isOperational());
        assertTrue("definition should always use equals", seqIdentifierDef.isAlwaysUseForEquals());
    }

    @Test
    public void testResourceDefinitionForOperationalAndAlwaysUseForEquals() {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObjectDefinition<ResourceType> resourceDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
        PrismPropertyDefinition<?> nameDef = resourceDef.findPropertyDefinition(ResourceType.F_NAME);
        assertTrue(nameDef.isEmphasized());

        PrismPropertyDefinition<SchemaDefinitionType> definitionDef = resourceDef.findItemDefinition(ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION));
        assertNotNull("No definition", definitionDef);
        assertFalse("definition should be operational", definitionDef.isOperational());
        assertFalse("definition should always use equals", definitionDef.isAlwaysUseForEquals());
        assertTrue("definition should be optional cleanup", definitionDef.isOptionalCleanup());
    }
}
