/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.namespace.QName;

public class SchemaProcessorTest extends AbstractSchemaTest {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testAccessList() throws Exception {
        final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";

        String filename = "src/test/resources/processor/resource-schema-complex.xsd";
        Document schemaDom = DOMUtil.parseFile(filename);
        ResourceSchema schema = ResourceSchemaParser.parse(DOMUtil.getFirstChildElement(schemaDom), filename);

        ResourceObjectDefinition objectDef = schema.findDefinitionForObjectClass(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("AccountObjectClass definition not found", objectDef);

        ResourceAttributeDefinition attrDef = objectDef.findAttributeDefinition(new ItemName(icfNS, "uid"));
        AssertJUnit.assertTrue("uid readability", attrDef.canRead());
        AssertJUnit.assertFalse("uid updateability", attrDef.canModify());
        AssertJUnit.assertFalse("uid createability", attrDef.canAdd());

        attrDef = objectDef.findAttributeDefinition(new ItemName(MidPointConstants.NS_RI, "title"));
        AssertJUnit.assertTrue(attrDef.canRead());
        AssertJUnit.assertTrue(attrDef.canModify());
        AssertJUnit.assertTrue(attrDef.canAdd());

        attrDef = objectDef.findAttributeDefinition(new ItemName(MidPointConstants.NS_RI, "photo"));
        AssertJUnit.assertFalse(attrDef.canRead());
        AssertJUnit.assertTrue(attrDef.canModify());
        AssertJUnit.assertTrue(attrDef.canAdd());
    }

    @Test
    public void testRoundTripGeneric() throws SchemaException {
        // GIVEN
        PrismSchemaImpl schema = new PrismSchemaImpl(MidPointConstants.NS_RI);
        // Ordinary property
        schema.createPropertyDefinition("number1", DOMUtil.XSD_INT);

        // Property container
        MutablePrismContainerDefinition<?> containerDefinition = schema.createContainerDefinition("ContainerType");
        // ... in it ordinary property
        containerDefinition.createPropertyDefinition("login", DOMUtil.XSD_STRING);
        // ... and local property with a type from another schema
        containerDefinition.createPropertyDefinition("password", ProtectedStringType.COMPLEX_TYPE);
        // ... property reference
        containerDefinition.createPropertyDefinition(SchemaConstants.C_CREDENTIALS, CredentialsType.COMPLEX_TYPE);
        // ... read-only int property
        PrismPropertyDefinition counterProperty = containerDefinition.createPropertyDefinition("counter", DOMUtil.XSD_INT);
        counterProperty.toMutable().toMutable().setReadOnly();

        System.out.println("Generic schema before serializing to XSD: ");
        System.out.println(schema.debugDump());
        System.out.println();

        // WHEN

        Document xsd = schema.serializeToXsd();

        String stringXmlSchema = DOMUtil.serializeDOMToString(xsd);

        System.out.println("Generic schema after serializing to XSD: ");
        System.out.println(stringXmlSchema);
        System.out.println();

        Document parsedXsd = DOMUtil.parseDocument(stringXmlSchema);

        PrismSchema newSchema = PrismSchemaImpl.parse(DOMUtil.getFirstChildElement(parsedXsd), true, "serialized schema", PrismTestUtil.getPrismContext());

        System.out.println("Generic schema after parsing from XSD: ");
        System.out.println(newSchema.debugDump());
        System.out.println();

        // THEN

        PrismPropertyDefinition number1def = newSchema.findItemDefinitionByElementName(new ItemName(MidPointConstants.NS_RI, "number1"), PrismPropertyDefinition.class);
        assertEquals(new ItemName(MidPointConstants.NS_RI, "number1"), number1def.getItemName());
        assertEquals(DOMUtil.XSD_INT, number1def.getTypeName());

        PrismContainerDefinition newContainerDef = schema.findContainerDefinitionByType(new ItemName(MidPointConstants.NS_RI, "ContainerType"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "ContainerType"), newContainerDef.getTypeName());

        PrismPropertyDefinition loginDef = newContainerDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "login"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "login"), loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
        assertTrue("Read flag is wrong", loginDef.canRead());
        assertTrue("Create flag is wrong", loginDef.canAdd());
        assertTrue("Update flag is wrong", loginDef.canModify());

        PrismPropertyDefinition passwdDef = newContainerDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "password"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "password"), passwdDef.getItemName());
        assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());

        PrismPropertyDefinition credDef = newContainerDef.findPropertyDefinition(new ItemName(SchemaConstants.NS_C, "credentials"));
        assertEquals(new ItemName(SchemaConstants.NS_C, "credentials"), credDef.getItemName());
        assertEquals(new ItemName(SchemaConstants.NS_C, "CredentialsType"), credDef.getTypeName());

        PrismPropertyDefinition countDef = newContainerDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "counter"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "counter"), countDef.getItemName());
        assertEquals(DOMUtil.XSD_INT, countDef.getTypeName());
        assertTrue("Read flag is wrong", countDef.canRead());
        assertFalse("Create flag is wrong", countDef.canAdd());
        assertFalse("Update flag is wrong", countDef.canModify());
    }

    @Test
    public void testRoundTripResource() throws SchemaException {
        // GIVEN
        ResourceSchemaImpl schema = new ResourceSchemaImpl();

        // Property container
        ResourceObjectClassDefinitionImpl objectClassDef = (ResourceObjectClassDefinitionImpl)
                schema.createObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        objectClassDef.setDefaultAccountDefinition(true);
        objectClassDef.setNativeObjectClass("ACCOUNT");

        // ... in it ordinary attribute - an identifier
        ResourceAttributeDefinition<?> xLoginDef =
                objectClassDef.createAttributeDefinition("login", DOMUtil.XSD_STRING,
                        def -> def.setNativeAttributeName("LOGIN"));
        objectClassDef.addPrimaryIdentifierName(xLoginDef.getItemName());
        objectClassDef.setDisplayNameAttributeName(xLoginDef.getItemName());

        // ... and local property with a type from another schema
        objectClassDef.createAttributeDefinition("password", ProtectedStringType.COMPLEX_TYPE,
                def -> def.setNativeAttributeName("PASSWORD"));

        // ... ignored attribute
        objectClassDef.createAttributeDefinition("sep", DOMUtil.XSD_STRING,
                def -> def.setProcessing(ItemProcessing.IGNORE));

        System.out.println("Resource schema before serializing to XSD: ");
        System.out.println(schema.debugDump());
        System.out.println();

        // WHEN

        Document xsd = schema.serializeToXsd();

        String stringXmlSchema = DOMUtil.serializeDOMToString(xsd);

        System.out.println("Resource schema after serializing to XSD: ");
        System.out.println(stringXmlSchema);
        System.out.println();

        Document parsedXsd = DOMUtil.parseDocument(stringXmlSchema);

        ResourceSchema newSchema = ResourceSchemaParser.parse(DOMUtil.getFirstChildElement(parsedXsd), "serialized schema");

        System.out.println("Resource schema after parsing from XSD: ");
        System.out.println(newSchema.debugDump());
        System.out.println();

        // THEN

        ResourceObjectClassDefinition newObjectClassDef =
                newSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        assertThat(newObjectClassDef.getTypeName())
                .isEqualTo(RI_ACCOUNT_OBJECT_CLASS);
        assertThat(newObjectClassDef.isDefaultAccountDefinition())
                .isTrue();
        assertThat(newObjectClassDef.getNativeObjectClass())
                .isEqualTo("ACCOUNT");
        assertThat(newObjectClassDef.getPrimaryIdentifiersNames())
                .containsExactly(xLoginDef.getItemName());
        assertThat(newObjectClassDef.getDisplayNameAttributeName())
                .isEqualTo(xLoginDef.getItemName());

        PrismPropertyDefinition<?> loginDef =
                newObjectClassDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "login"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "login"), loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
        assertFalse(loginDef.isIgnored());

        PrismPropertyDefinition<?> passwdDef =
                newObjectClassDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "password"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "password"), passwdDef.getItemName());
        assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());
        assertFalse(passwdDef.isIgnored());

        PrismPropertyDefinition<?> sepDef =
                newObjectClassDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "sep"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "sep"), sepDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, sepDef.getTypeName());
        assertTrue(sepDef.isIgnored());
    }

}
