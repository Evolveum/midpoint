/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class SchemaProcessorBasicTest {

    private static final String TEST_DIR = "src/test/resources/processor/";

    private static final String SCHEMA1_FILENAME = TEST_DIR + "schema1.xsd";
    private static final String OBJECT1_FILENAME = TEST_DIR + "object1.xml";
    private static final String SCHEMA2_FILENAME = TEST_DIR + "schema2.xsd";
    private static final String OBJECT2_FILENAME = TEST_DIR + "object2.xml";
    private static final String SCHEMA_NAMESPACE = "http://schema.foo.com/bar";

    public SchemaProcessorBasicTest() {
    }

    @BeforeMethod
    public void setUp() {
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void parseSchemaTest() throws SchemaException {
        System.out.println("===[ parseSchemaTest ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);

        // WHEN

        Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));

        // THEN

        assertNotNull(schema);

        System.out.println("Parsed schema from " + SCHEMA1_FILENAME + ":");
        System.out.println(schema.dump());

        PropertyContainerDefinition accDef = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
        assertTrue("Expected ResourceObjectDefinition but got " + accDef.getClass().getName(), accDef instanceof ResourceObjectDefinition);
        assertTrue("Not a default account", ((ResourceObjectDefinition) accDef).isDefaultAccountType());
        PropertyDefinition loginDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "login"), loginDef.getName());
    }

    @Test
    public void instantiationTest() throws SchemaException, JAXBException {
        System.out.println("===[ instantiationTest ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
        Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
        assertNotNull(schema);
        System.out.println("Parsed schema:");
        System.out.println(schema.dump());
        PropertyContainerDefinition accDef = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
        PropertyDefinition loginDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "login"), loginDef.getName());

        // WHEN

        // Instantiate PropertyContainer (XSD type)
        PropertyContainer accInst = accDef.instantiate(new QName(SCHEMA_NAMESPACE, "first"));
        assertNotNull(accInst);
        assertNotNull(accInst.getDefinition());
        // as the definition is ResourceObjectDefinition, the instance should be of ResoureceObject type
        assertTrue(accInst instanceof ResourceObject);

        // Instantiate Property (XSD element)
        Property loginInst = loginDef.instantiate();
        assertNotNull(loginInst);
        assertNotNull(loginInst.getDefinition());
        assertTrue(loginInst instanceof ResourceObjectAttribute);

        // Set some value
        loginInst.setValue(new PropertyValue("FOOBAR"));
        accInst.getItems().add(loginInst);

        // Same thing with the prop2 property (type int)
        PropertyDefinition groupDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "group"));
        Property groupInst = groupDef.instantiate();
        groupInst.setValue(new PropertyValue(321));
        accInst.getItems().add(groupInst);


        System.out.println("AccountObjectClass INST: " + accInst);
        // Serialize to DOM

        Document doc = DOMUtil.getDocument();
        accInst.serializeToDom(doc);

        // TODO: Serialize to XML and check

        System.out.println("Serialized: ");
        System.out.println(DOMUtil.serializeDOMToString(doc));
    }

    @Test
    public void valueParseTest() throws SchemaException, SchemaException {
        System.out.println("===[ valueParseTest ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
        Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
        AssertJUnit.assertNotNull(schema);
        PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), type1Def.getTypeName());
        PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
        AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE, "login"), prop1Def.getName());

        // WHEN

        Document dataDom = DOMUtil.parseFile(OBJECT1_FILENAME);
        PropertyContainer container = type1Def.parseItem(DOMUtil.getFirstChildElement(dataDom));

        // THEN

        System.out.println("container: " + container);

        assertEquals(2, container.getItems().size());

        for (Item item : container.getItems()) {
            ResourceObjectAttribute prop = (ResourceObjectAttribute) item;
            if (prop.getName().getLocalPart().equals("login")) {
                AssertJUnit.assertEquals("barbar", prop.getValue(String.class).getValue());
            }
            if (prop.getName().getLocalPart().equals("group")) {
                PropertyValue<Integer> val = prop.getValue(Integer.class);
                AssertJUnit.assertEquals(Integer.valueOf(123456), val.getValue());
            }
        }
    }

    /**
     * Take prepared XSD schema, parse it and use it to parse property container instance.
     */
    @Test
    public void testParsePropertyContainer() throws SchemaException, SchemaException {
        System.out.println("===[ testParsePropertyContainer ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(SCHEMA2_FILENAME);
        Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
        assertNotNull(schema);
        System.out.println(SchemaProcessorBasicTest.class.getSimpleName() + ".testParsePropertyContainer parsed schema: ");
        System.out.println(schema.dump());
        PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE, "PropertyContainerType"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "PropertyContainerType"), type1Def.getTypeName());
        PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "prop1"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "prop1"), prop1Def.getName());

        // WHEN

        Document dataDom = DOMUtil.parseFile(OBJECT2_FILENAME);
        PropertyContainer propertyContainer = schema.parsePropertyContainer(DOMUtil.getFirstChildElement(dataDom));

        // THEN
        assertNotNull(propertyContainer);
        System.out.println(SchemaProcessorBasicTest.class.getSimpleName() + ".testParsePropertyContainer parsed container: ");
        System.out.println(propertyContainer.dump());
        assertEquals(new QName(SCHEMA_NAMESPACE, "propertyContainer"), propertyContainer.getName());
        assertEquals(new QName(SCHEMA_NAMESPACE, "propertyContainer"), propertyContainer.getDefinition().getName());
        assertEquals(new QName(SCHEMA_NAMESPACE, "PropertyContainerType"), propertyContainer.getDefinition().getTypeName());

    }

    @Test
    public void testParseAndSerializeUser() {


    }

}
