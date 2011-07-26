package com.evolveum.midpoint.schema.processor;

import static org.junit.Assert.*;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.util.DOMUtil;

public class DomToSchemaProcessorTest {

	@Test
	public void testAccessList() throws Exception {
		Document schemaDom = DOMUtil.parseFile("src/test/resources/processor/schema.xsd");
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		
		final String defaultNS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
		final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/resource/icf/schema-1.xsd";
		ResourceObjectDefinition objectDef = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(defaultNS, "AccountObjectClass"));
		
		ResourceObjectAttributeDefinition attrDef = objectDef.findAttributeDefinition(new QName(icfNS, "uid"));
		assertTrue(attrDef.canRead());
		assertFalse(attrDef.canUpdate());
		assertFalse(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "title"));
		assertTrue(attrDef.canRead());
		assertTrue(attrDef.canUpdate());
		assertTrue(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "photo"));
		assertFalse(attrDef.canRead());
		assertTrue(attrDef.canUpdate());
		assertTrue(attrDef.canCreate());
	}
}
