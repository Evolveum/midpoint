/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.schema.PrismSchemaBuildingUtil.addNewContainerDefinition;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.EnumerationTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.impl.EnumerationTypeDefinitionImpl;
import com.evolveum.midpoint.prism.impl.EnumerationTypeDefinitionImpl.ValueDefinitionImpl;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchemaBuildingUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class SchemaProcessorTest extends AbstractSchemaTest {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testAccessList() throws Exception {
        final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";

        String filename = "src/test/resources/processor/resource-schema-complex.xsd";
        Document schemaDom = DOMUtil.parseFile(filename);
        var schema = ResourceSchemaFactory.parseNativeSchemaAsBare(schemaDom);

        var objectDef = schema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("AccountObjectClass definition not found", objectDef);

        ShadowSimpleAttributeDefinition<?> attrDef = objectDef.findSimpleAttributeDefinitionRequired(new ItemName(icfNS, "uid"));
        AssertJUnit.assertTrue("uid readability", attrDef.canRead());
        AssertJUnit.assertFalse("uid updateability", attrDef.canModify());
        AssertJUnit.assertFalse("uid createability", attrDef.canAdd());

        attrDef = objectDef.findSimpleAttributeDefinitionRequired(new ItemName(NS_RI, "title"));
        AssertJUnit.assertTrue(attrDef.canRead());
        AssertJUnit.assertTrue(attrDef.canModify());
        AssertJUnit.assertTrue(attrDef.canAdd());

        attrDef = objectDef.findSimpleAttributeDefinitionRequired(new ItemName(NS_RI, "photo"));
        AssertJUnit.assertFalse(attrDef.canRead());
        AssertJUnit.assertTrue(attrDef.canModify());
        AssertJUnit.assertTrue(attrDef.canAdd());
    }

    @Test
    public void testRoundTripGeneric() throws SchemaException {
        // GIVEN
        PrismSchemaImpl schema = new PrismSchemaImpl(NS_RI);
        // Ordinary property
        PrismSchemaBuildingUtil.addNewPropertyDefinition(schema, "number1", DOMUtil.XSD_INT);

        // Enum type
        var red = new ValueDefinitionImpl("red", "Please stop here", "RED");
        var yellow = new ValueDefinitionImpl("yellow", "Like a sun", "YELLOW");
        var green = new ValueDefinitionImpl("green", "The color of grass", "GREEN");
        var colorTypeName = new QName(NS_RI, "ColorType");
        var colorTypeDef = new EnumerationTypeDefinitionImpl(colorTypeName, DOMUtil.XSD_STRING, List.of(red, yellow, green));
        schema.add((Definition) colorTypeDef);

        // A container
        var containerDefinition = addNewContainerDefinition(schema, "test", "ContainerType");
        // ... in it ordinary property
        containerDefinition.mutator().createPropertyDefinition("login", DOMUtil.XSD_STRING);
        // ... and local property with a type from another schema
        containerDefinition.mutator().createPropertyDefinition("password", ProtectedStringType.COMPLEX_TYPE);
        // ... property reference
        containerDefinition.mutator().createPropertyDefinition(SchemaConstants.C_CREDENTIALS, CredentialsType.COMPLEX_TYPE);
        // ... read-only int property
        var counterProperty = containerDefinition.mutator().createPropertyDefinition("counter", DOMUtil.XSD_INT);
        counterProperty.mutator().setReadOnly();
        // ... custom enum property
        containerDefinition.mutator().createPropertyDefinition("color", colorTypeName);

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

        PrismSchema parsedSchema = SchemaParsingUtil.createAndParse(
                DOMUtil.getFirstChildElement(parsedXsd), true, "serialized schema");

        System.out.println("Generic schema after parsing from XSD: ");
        System.out.println(parsedSchema.debugDump());
        System.out.println();

        // THEN

        PrismPropertyDefinition<?> number1def = parsedSchema.findItemDefinitionByElementName(new ItemName(NS_RI, "number1"), PrismPropertyDefinition.class);
        assertEquals(new ItemName(NS_RI, "number1"), number1def.getItemName());
        assertEquals(DOMUtil.XSD_INT, number1def.getTypeName());

        PrismContainerDefinition<?> newContainerDef = schema.findContainerDefinitionByType(new ItemName(NS_RI, "ContainerType"));
        assertEquals(new ItemName(NS_RI, "ContainerType"), newContainerDef.getTypeName());

        PrismPropertyDefinition<?> loginDef = newContainerDef.findPropertyDefinition(new ItemName(NS_RI, "login"));
        assertEquals(new ItemName(NS_RI, "login"), loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
        assertTrue("Read flag is wrong", loginDef.canRead());
        assertTrue("Create flag is wrong", loginDef.canAdd());
        assertTrue("Update flag is wrong", loginDef.canModify());

        PrismPropertyDefinition<?> passwdDef = newContainerDef.findPropertyDefinition(new ItemName(NS_RI, "password"));
        assertEquals(new ItemName(NS_RI, "password"), passwdDef.getItemName());
        assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());

        PrismPropertyDefinition<?> credDef = newContainerDef.findPropertyDefinition(new ItemName(SchemaConstants.NS_C, "credentials"));
        assertEquals(new ItemName(SchemaConstants.NS_C, "credentials"), credDef.getItemName());
        assertEquals(new ItemName(SchemaConstants.NS_C, "CredentialsType"), credDef.getTypeName());

        PrismPropertyDefinition<?> countDef = newContainerDef.findPropertyDefinition(new ItemName(NS_RI, "counter"));
        assertEquals(new ItemName(NS_RI, "counter"), countDef.getItemName());
        assertEquals(DOMUtil.XSD_INT, countDef.getTypeName());
        assertTrue("Read flag is wrong", countDef.canRead());
        assertFalse("Create flag is wrong", countDef.canAdd());
        assertFalse("Update flag is wrong", countDef.canModify());

        PrismPropertyDefinition<?> colorDef = newContainerDef.findPropertyDefinition(new ItemName(NS_RI, "color"));
        assertEquals(new ItemName(NS_RI, "color"), colorDef.getItemName());
        assertEquals(colorTypeName, colorDef.getTypeName());

        var parsedColorDef = (EnumerationTypeDefinition) parsedSchema.findTypeDefinitionByType(colorTypeName);
        var values = parsedColorDef.getValues();
        assertThat(values).as("color values").hasSize(3);
        var parsedYellowDef = values.stream()
                .filter(v -> "yellow".equals(v.getValue()))
                .findFirst()
                .orElseThrow();
        assertThat(parsedYellowDef.getConstantName().orElseThrow())
                .as("YELLOW constant name")
                .isEqualTo("YELLOW");
        assertThat(parsedYellowDef.getDocumentation().orElseThrow())
                .as("YELLOW documentation")
                .contains("Like a sun"); // FIXME the real text is the XML serialization
    }

    @Test
    public void testRoundTripResource() throws SchemaException, ConfigurationException {
        // GIVEN
        var schema = new NativeResourceSchemaImpl();

        ItemName loginAttrName = new ItemName(NS_RI, "login");
        ItemName passwordAttrName = new ItemName(NS_RI, "password");
        ItemName sepAttrName = new ItemName(NS_RI, "sep");

        // Property container
        var objectClassBuilder = schema.newComplexTypeDefinitionLikeBuilder(ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
        objectClassBuilder.setDefaultAccountDefinition(true);
        objectClassBuilder.setNativeObjectClassName("ACCOUNT");

        // ... in it ordinary attribute - an identifier
        var loginAttrBuilder = objectClassBuilder.newPropertyLikeDefinition(loginAttrName, DOMUtil.XSD_STRING);
        loginAttrBuilder.setNativeAttributeName("LOGIN");
        objectClassBuilder.add(loginAttrBuilder);

        objectClassBuilder.setPrimaryIdentifierName(loginAttrName);
        objectClassBuilder.setDisplayNameAttributeName(loginAttrName);

        // ... and local property with a type from another schema
        var passwordAttrBuilder = objectClassBuilder.newPropertyLikeDefinition(passwordAttrName, ProtectedStringType.COMPLEX_TYPE);
        passwordAttrBuilder.setNativeAttributeName("PASSWORD");
        objectClassBuilder.add(passwordAttrBuilder);

        // ... unreadable attribute
        var sepAttrBuilder = objectClassBuilder.newPropertyLikeDefinition(sepAttrName, DOMUtil.XSD_STRING);
        sepAttrBuilder.setCanRead(true);
        objectClassBuilder.add(sepAttrBuilder);

        schema.add(objectClassBuilder);

        System.out.println("Resource schema before serializing to XSD: ");
        System.out.println(schema.debugDump());
        System.out.println();

        when("schema is serialized to XSD");

        Document xsd = schema.serializeToXsd();

        String stringXmlSchema = DOMUtil.serializeDOMToString(xsd);

        System.out.println("Resource schema after serializing to XSD: ");
        System.out.println(stringXmlSchema);
        System.out.println();

        and("schema is parsed again");

        Document parsedXsd = DOMUtil.parseDocument(stringXmlSchema);

        var parsedAsNative = ResourceSchemaFactory.parseNativeSchema(DOMUtil.getFirstChildElement(parsedXsd), "native");

        System.out.println("Native resource schema after parsing from XSD: ");
        System.out.println(parsedAsNative.debugDump());
        System.out.println();

        and("it is parsed as 'bare'");

        var parsedAsBare = ResourceSchemaFactory.nativeToBare(parsedAsNative);

        ResourceObjectClassDefinition newObjectClassDef = parsedAsBare.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        assertThat(newObjectClassDef.getTypeName()).isEqualTo(RI_ACCOUNT_OBJECT_CLASS);
        assertThat(newObjectClassDef.isDefaultAccountDefinition()).isTrue();
        assertThat(newObjectClassDef.getNativeObjectClassName()).isEqualTo("ACCOUNT");
        assertThat(newObjectClassDef.getPrimaryIdentifiersNames()).containsExactly(loginAttrName);
        assertThat(newObjectClassDef.getDisplayNameAttributeName()).isEqualTo(loginAttrName);

        PrismPropertyDefinition<?> loginDef = newObjectClassDef.findPropertyDefinition(loginAttrName);
        assertEquals(loginAttrName, loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
        assertFalse(loginDef.isIgnored());

        PrismPropertyDefinition<?> passwdDef = newObjectClassDef.findPropertyDefinition(passwordAttrName);
        assertEquals(passwordAttrName, passwdDef.getItemName());
        assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());
        assertFalse(passwdDef.isIgnored());

        PrismPropertyDefinition<?> sepDef = newObjectClassDef.findPropertyDefinition(sepAttrName);
        assertEquals(sepAttrName, sepDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, sepDef.getTypeName());
        assertTrue(sepDef.canRead());
    }
}
