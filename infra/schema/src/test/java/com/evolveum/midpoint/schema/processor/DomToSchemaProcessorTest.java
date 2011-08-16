package com.evolveum.midpoint.schema.processor;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import com.evolveum.midpoint.util.DOMUtil;

public class DomToSchemaProcessorTest {

	@Test
	public void testAccessList() throws Exception {
		Document schemaDom = DOMUtil.parseFile("src/test/resources/processor/schema.xsd");
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		
		final String defaultNS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
		final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd";
		ResourceObjectDefinition objectDef = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(defaultNS, "AccountObjectClass"));
		
		ResourceObjectAttributeDefinition attrDef = objectDef.findAttributeDefinition(new QName(icfNS, "uid"));
		AssertJUnit.assertTrue(attrDef.canRead());
		AssertJUnit.assertFalse(attrDef.canUpdate());
		AssertJUnit.assertFalse(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "title"));
		AssertJUnit.assertTrue(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canUpdate());
		AssertJUnit.assertTrue(attrDef.canCreate());
		
		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "photo"));
		AssertJUnit.assertFalse(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canUpdate());
		AssertJUnit.assertTrue(attrDef.canCreate());
	}
}
