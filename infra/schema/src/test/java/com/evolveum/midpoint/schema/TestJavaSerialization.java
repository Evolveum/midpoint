/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Tests Java serialization of various objects using SerializationUtil.
 *
 * @author semancik
 */
public class TestJavaSerialization extends AbstractSchemaTest {

    @Test
    public void testSerializeResource() throws Exception {
        serializationRoundTrip(TestConstants.RESOURCE_FILE);
    }

    @Test
    public void testSerializeUser() throws Exception {
        serializationRoundTrip(TestConstants.USER_FILE);
    }

    @Test
    public void testSerializeRole() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismObject<RoleType> parsedObject = prismContext.parseObject(TestConstants.ROLE_FILE);

        System.out.println("Parsed object:");
        System.out.println(parsedObject.debugDump(1));

        RoleType parsedRoleType = parsedObject.asObjectable();
        AdminGuiConfigurationType adminGuiConfiguration = parsedRoleType.getAdminGuiConfiguration();
        String defaultTimezone = adminGuiConfiguration.getDefaultTimezone();
        assertEquals("Wrong defaultTimezone", "Europe/Bratislava", defaultTimezone);

        // WHEN
        serializationRoundTripPrismObject(parsedObject);
        serializationRoundTripObjectType(parsedRoleType);
    }

    private <O extends ObjectType> void serializationRoundTrip(File file) throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismObject<O> parsedObject = prismContext.parseObject(file);

        System.out.println("\nParsed object:");
        System.out.println(parsedObject.debugDump());

        serializationRoundTripPrismObject(parsedObject);
        serializationRoundTripObjectType(parsedObject.asObjectable());
    }

    private <O extends ObjectType> void serializationRoundTripPrismObject(PrismObject<O> parsedObject) throws Exception {

        // WHEN
        String serializedObject = SerializationUtil.toString(parsedObject);

        // THEN
        System.out.println("\nSerialized object:");
        System.out.println(serializedObject);
        PrismObject<O> deserializedObject = SerializationUtil.fromString(serializedObject);

        System.out.println("\nDeserialized object (PrismObject):");
        System.out.println(deserializedObject.debugDump());
        deserializedObject.revive(getPrismContext());

        ObjectDelta<O> diff = parsedObject.diff(deserializedObject);
        assertTrue("Something changed in serialization of " + parsedObject + " (PrismObject): " + diff, diff.isEmpty());

        ItemDefinition nameDef = deserializedObject.getDefinition().findLocalItemDefinition(ObjectType.F_NAME);
        assertNotNull("No 'name' definition in deserialized object", nameDef);
    }

    private <O extends ObjectType> void serializationRoundTripObjectType(O parsedObject) throws Exception {

        // WHEN
        String serializedObject = SerializationUtil.toString(parsedObject);

        // THEN
        O deserializedObject = SerializationUtil.fromString(serializedObject);

        deserializedObject.asPrismObject().revive(getPrismContext());

        System.out.println("Deserialized object (ObjectType):");
        System.out.println(deserializedObject.asPrismObject().debugDump());

        ObjectDelta<O> diff = parsedObject.asPrismObject().diff((PrismObject) deserializedObject.asPrismObject());
        assertTrue("Something changed in serialization of " + parsedObject + " (ObjectType): " + diff, diff.isEmpty());
    }
}
