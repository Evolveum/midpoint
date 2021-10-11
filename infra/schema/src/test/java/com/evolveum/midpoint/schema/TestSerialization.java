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
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.DefaultReferencableImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;

/**
 * @author semancik
 */
public class TestSerialization extends AbstractSchemaTest {

    @Test
    public void testSerializeTrace() throws Exception {
        PrismContext prismContext = getPrismContext();
        ValueTransformationTraceType trace = new ValueTransformationTraceType(prismContext);
        NamedValueType input1 = new NamedValueType();
        input1.setName(new QName("myRef"));
        input1.setValue(new ObjectReferenceType().oid("123").type(UserType.COMPLEX_TYPE));
        trace.getInput().add(input1);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "trace");
        String xml = prismContext.xmlSerializer().serializeAnyData(trace, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeDeltaSetTripleType() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismReferenceValue refValue = new PrismReferenceValueImpl("123456");
        DefaultReferencableImpl referencable = new DefaultReferencableImpl(refValue);
        DeltaSetTripleType triple = new DeltaSetTripleType();
        triple.getPlus().add(referencable);

        QName fakeQName = new QName(PrismConstants.NS_TYPES, "triple");
        String xml = prismContext.xmlSerializer().serializeAnyData(triple, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeNamedReference() throws Exception {
        PrismContext prismContext = getPrismContext();
        ObjectReferenceType reference = new ObjectReferenceType()
                .oid("66662a3b-76eb-4465-8374-742f6e2f54b4")
                .type(UserType.COMPLEX_TYPE)
                .targetName("joe");
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "ref");
        String xml = prismContext.xmlSerializer().serializeAnyData(reference, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeFullReference() throws Exception {
        PrismContext prismContext = getPrismContext();

        QName fakeQName = new QName(PrismConstants.NS_TYPES, "ref");
        MutablePrismReferenceDefinition definition = prismContext.definitionFactory().createReferenceDefinition(fakeQName, ObjectReferenceType.COMPLEX_TYPE);
        definition.setComposite(true);
        PrismReference reference = definition.instantiate();

        UserType joe = new UserType(prismContext)
                .name("joe")
                .oid("66662a3b-76eb-4465-8374-742f6e2f54b4")
                .description("description");
        ObjectReferenceType referenceRealValue = ObjectTypeUtil.createObjectRefWithFullObject(joe, prismContext);
        reference.add(referenceRealValue.asReferenceValue());

        String xml = prismContext.xmlSerializer().serialize(reference);
        System.out.println(xml);
    }

    @Test
    public void testSerializeMessage() throws Exception {
        PrismContext prismContext = getPrismContext();
        SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
        LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");
        String xml = prismContext.xmlSerializer().serializeAnyData(localizableMessageBean, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeExecuteCredentialResetResponseType() throws Exception {
        PrismContext prismContext = getPrismContext();

        SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
        LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
        ExecuteCredentialResetResponseType response = new ExecuteCredentialResetResponseType();
        response.setMessage(localizableMessageBean);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");

        prismContext.adopt(response);

        String xml = prismContext.xmlSerializer().serializeAnyData(response, fakeQName);
        System.out.println(xml);
    }

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
        assertTrue("Something changed in serializetion of " + parsedObject + " (ObjectType): " + diff, diff.isEmpty());
    }

}
