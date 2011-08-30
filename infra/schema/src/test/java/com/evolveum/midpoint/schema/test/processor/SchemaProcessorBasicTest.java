/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.test.processor;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class SchemaProcessorBasicTest {
	private static final String SCHEMA1_FILENAME = "src/test/resources/processor/schema1.xsd";
	private static final String SCHEMA_NAMESPACE = "http://schema.foo.com/bar";
	private static final String OBJECT1_FILENAME = "src/test/resources/processor/object1.xml";
	
	public SchemaProcessorBasicTest() {
	}
	
	@BeforeMethod
	public void setUp() {
	}
	
	@AfterMethod
	public void tearDown() {
	}

	@Test
	public void parseSchemaTest() throws SchemaProcessorException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		
		// WHEN
		
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		
		// THEN
		
		AssertJUnit.assertNotNull(schema);
		
		System.out.println("Parsed schema from "+SCHEMA1_FILENAME+":");
		System.out.println(schema.dump());
		
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
	}

	@Test
	public void instantiationTest() throws SchemaProcessorException, JAXBException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		AssertJUnit.assertNotNull(schema);
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
		
		// WHEN
		
		// Instantiate PropertyContainer (XSD type)
		PropertyContainer type1Inst = type1Def.instantiate(new QName(SCHEMA_NAMESPACE,"first"));
		AssertJUnit.assertNotNull(type1Inst);
		AssertJUnit.assertNotNull(type1Inst.getDefinition());
		
		// Instantiate Property (XSD element)
		Property prop1Inst = prop1Def.instantiate();
		AssertJUnit.assertNotNull(prop1Inst);
		AssertJUnit.assertNotNull(prop1Inst.getDefinition());
		
		// Set some value
		prop1Inst.setValue("FOOBAR");
		type1Inst.getProperties().add(prop1Inst);

		// Same thing with the prop2 property (type int)
		PropertyDefinition prop2Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop2"));
		Property prop2Inst = prop2Def.instantiate();
		prop2Inst.setValue(321);
		type1Inst.getProperties().add(prop2Inst);

		
		System.out.println("FirstType INST: "+type1Inst);
		// Serialize to DOM
		
		List<Object> elements = type1Inst.serializePropertiesToDom(DOMUtil.getDocument());

		// TODO: Serialize to XML and check
		
		System.out.println("Serialized: ");
		for (Object o : elements) {
			System.out.println(JAXBUtil.serializeElementToString(o));
			assertTrue(o instanceof Element);
			Element e = (Element)o;
			if (e.getLocalName().equals("prop1")) {
				assertEquals("FOOBAR",e.getTextContent());
			} else if (e.getLocalName().equals("prop2")) {
				assertEquals("321",e.getTextContent());
			}
		}
	}
	
	@Test
	public void valueParseTest() throws SchemaProcessorException, SchemaException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		AssertJUnit.assertNotNull(schema);
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
		
		// WHEN
		
		Document dataDom = DOMUtil.parseFile(OBJECT1_FILENAME);
		Set<Property> properties = type1Def.parseProperties(DOMUtil.getSubelementList(DOMUtil.getFirstChildElement(dataDom)));

		// THEN
		
		System.out.println("PROPS: "+properties);
		
		AssertJUnit.assertEquals(2,properties.size());
		
		for (Property prop : properties) {
			if (prop.getName().getLocalPart().equals("prop1")) {
				AssertJUnit.assertEquals("Barbar", prop.getValue(String.class));
			}
			if (prop.getName().getLocalPart().equals("prop2")) {
				int val = prop.getValue(int.class);
				AssertJUnit.assertEquals(123456, val);
			}
		}
	}
	
}
