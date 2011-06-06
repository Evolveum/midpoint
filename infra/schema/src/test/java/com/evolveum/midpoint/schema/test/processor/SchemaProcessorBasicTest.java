/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.test.processor;

import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import java.util.Set;
import javax.xml.namespace.QName;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

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
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	@Test
	public void parseSchemaTest() throws SchemaProcessorException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		
		// WHEN
		
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		
		// THEN
		
		assertNotNull(schema);
		
		System.out.println("Parsed schema from "+SCHEMA1_FILENAME+":");
		System.out.println(schema.debugDump());
		
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
	}

	@Test
	public void instantiationTest() throws SchemaProcessorException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		assertNotNull(schema);
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
		
		// WHEN
		
		// Instantiate PropertyContainer (XSD type)
		PropertyContainer type1Inst = type1Def.instantiate(new QName(SCHEMA_NAMESPACE,"first"));
		assertNotNull(type1Inst);
		assertNotNull(type1Inst.getDefinition());
		
		// Instantiate Property (XSD element)
		Property prop1Inst = prop1Def.instantiate();
		assertNotNull(prop1Inst);
		assertNotNull(prop1Inst.getDefinition());
		
		// Set some value
		prop1Inst.setValue("FOOBAR");
		type1Inst.getProperties().add(prop1Inst);
		
		System.out.println("FirstType INST: "+type1Inst);
		
		// TODO: Serialize to XML and check
	}
	
	@Test
	public void valueParseTest() throws SchemaProcessorException {
		// GIVEN
		
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		assertNotNull(schema);
		PropertyContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE,"FirstType"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"FirstType"), type1Def.getTypeName());
		PropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"prop1"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"prop1"), prop1Def.getName());
		
		// WHEN
		
		Document dataDom = DOMUtil.parseFile(OBJECT1_FILENAME);
		Set<Property> properties = type1Def.parseProperties(DOMUtil.getSubelementList(DOMUtil.getFirstChildElement(dataDom)));

		// THEN
		
		System.out.println("PROPS: "+properties);
		
		assertEquals(2,properties.size());
		
		for (Property prop : properties) {
			if (prop.getName().getLocalPart().equals("prop1")) {
				assertEquals("Barbar", prop.getValue(String.class));
			}
			if (prop.getName().getLocalPart().equals("prop2")) {
				int val = prop.getValue(int.class);
				assertEquals(123456, val);
			}
		}
	}
	
}
