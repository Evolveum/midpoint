/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.processor;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class SchemaProcessorTest {

	private static final String SCHEMA_NS = "http://foo.com/xml/ns/schema";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testAccessList() throws Exception {
		String filename = "src/test/resources/processor/resource-schema-complex.xsd";
		Document schemaDom = DOMUtil.parseFile(filename);
		ResourceSchema schema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(schemaDom), filename, PrismTestUtil.getPrismContext());

		final String defaultNS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
		final String icfNS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";
		ObjectClassComplexTypeDefinition objectDef = schema.findObjectClassDefinition(new QName(defaultNS, "AccountObjectClass"));
		assertNotNull("AccountObjectClass definition not found", objectDef);

		ResourceAttributeDefinition attrDef = objectDef.findAttributeDefinition(new QName(icfNS, "uid"));
		AssertJUnit.assertTrue("uid readability", attrDef.canRead());
		AssertJUnit.assertFalse("uid updateability", attrDef.canModify());
		AssertJUnit.assertFalse("uid createability", attrDef.canAdd());

		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "title"));
		AssertJUnit.assertTrue(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canModify());
		AssertJUnit.assertTrue(attrDef.canAdd());

		attrDef = objectDef.findAttributeDefinition(new QName(defaultNS, "photo"));
		AssertJUnit.assertFalse(attrDef.canRead());
		AssertJUnit.assertTrue(attrDef.canModify());
		AssertJUnit.assertTrue(attrDef.canAdd());
	}

	@Test
	public void testRoundTripGeneric() throws SchemaException {
		// GIVEN
		PrismSchemaImpl schema = new PrismSchemaImpl(SCHEMA_NS, PrismTestUtil.getPrismContext());
		// Ordinary property
		schema.createPropertyDefinition("number1", DOMUtil.XSD_INT);

		// Property container
		PrismContainerDefinitionImpl containerDefinition = schema.createPropertyContainerDefinition("ContainerType");
		// ... in it ordinary property
		containerDefinition.createPropertyDefinition("login", DOMUtil.XSD_STRING);
		// ... and local property with a type from another schema
		containerDefinition.createPropertyDefinition("password", ProtectedStringType.COMPLEX_TYPE);
		// ... property reference
		containerDefinition.createPropertyDefinition(SchemaConstants.C_CREDENTIALS, CredentialsType.COMPLEX_TYPE);
		// ... read-only int property
		PrismPropertyDefinition counterProperty = containerDefinition.createPropertyDefinition("counter", DOMUtil.XSD_INT);
		((PrismPropertyDefinitionImpl) counterProperty).setReadOnly();

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

		PrismPropertyDefinition number1def = newSchema.findItemDefinitionByElementName(new QName(SCHEMA_NS,"number1"), PrismPropertyDefinition.class);
		assertEquals(new QName(SCHEMA_NS,"number1"),number1def.getName());
		assertEquals(DOMUtil.XSD_INT,number1def.getTypeName());

		PrismContainerDefinition newContainerDef = schema.findContainerDefinitionByType(new QName(SCHEMA_NS,"ContainerType"));
		assertEquals(new QName(SCHEMA_NS,"ContainerType"),newContainerDef.getTypeName());

		PrismPropertyDefinition loginDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"login"));
		assertEquals(new QName(SCHEMA_NS,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
		assertTrue("Read flag is wrong",loginDef.canRead());
		assertTrue("Create flag is wrong",loginDef.canAdd());
		assertTrue("Update flag is wrong",loginDef.canModify());

		PrismPropertyDefinition passwdDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"password"));
		assertEquals(new QName(SCHEMA_NS,"password"), passwdDef.getName());
		assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());

		PrismPropertyDefinition credDef = newContainerDef.findPropertyDefinition(new QName(SchemaConstants.NS_C,"credentials"));
		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());

		PrismPropertyDefinition countDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"counter"));
		assertEquals(new QName(SCHEMA_NS,"counter"), countDef.getName());
		assertEquals(DOMUtil.XSD_INT, countDef.getTypeName());
		assertTrue("Read flag is wrong",countDef.canRead());
		assertFalse("Create flag is wrong",countDef.canAdd());
		assertFalse("Update flag is wrong",countDef.canModify());
	}


	@Test
	public void testRoundTripResource() throws SchemaException {
		// GIVEN
		ResourceSchemaImpl schema = new ResourceSchemaImpl(SCHEMA_NS, PrismTestUtil.getPrismContext());

		// Property container
		ObjectClassComplexTypeDefinitionImpl containerDefinition = (ObjectClassComplexTypeDefinitionImpl) schema.createObjectClassDefinition("AccountObjectClass");
		containerDefinition.setKind(ShadowKindType.ACCOUNT);
		containerDefinition.setDefaultInAKind(true);
		containerDefinition.setNativeObjectClass("ACCOUNT");
		// ... in it ordinary attribute - an identifier
		ResourceAttributeDefinitionImpl xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
		containerDefinition.addPrimaryIdentifier(xloginDef);
		xloginDef.setNativeAttributeName("LOGIN");
		containerDefinition.setDisplayNameAttribute(xloginDef.getName());
		// ... and local property with a type from another schema
		ResourceAttributeDefinitionImpl xpasswdDef = containerDefinition.createAttributeDefinition("password", ProtectedStringType.COMPLEX_TYPE);
		xpasswdDef.setNativeAttributeName("PASSWORD");
		// ... property reference
		// TODO this is not a ResourceAttributeDefinition, it cannot be placed here!
		//containerDefinition.createAttributeDefinition(SchemaConstants.C_CREDENTIALS, SchemaConstants.C_CREDENTIALS_TYPE);
		// ... ignored attribute
		ResourceAttributeDefinitionImpl xSepDef = containerDefinition.createAttributeDefinition("sep", DOMUtil.XSD_STRING);
		xSepDef.setIgnored(true);

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

		ResourceSchema newSchema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(parsedXsd), "serialized schema", PrismTestUtil.getPrismContext());

		System.out.println("Resource schema after parsing from XSD: ");
		System.out.println(newSchema.debugDump());
		System.out.println();

		// THEN

		ObjectClassComplexTypeDefinition newObjectClassDef = newSchema.findObjectClassDefinition(new QName(SCHEMA_NS,"AccountObjectClass"));
		assertEquals(new QName(SCHEMA_NS,"AccountObjectClass"),newObjectClassDef.getTypeName());
		assertEquals(ShadowKindType.ACCOUNT, newObjectClassDef.getKind());
		assertTrue(newObjectClassDef.isDefaultInAKind());

		PrismPropertyDefinition loginDef = newObjectClassDef.findPropertyDefinition(new QName(SCHEMA_NS,"login"));
		assertEquals(new QName(SCHEMA_NS,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
		assertFalse(loginDef.isIgnored());

		PrismPropertyDefinition passwdDef = newObjectClassDef.findPropertyDefinition(new QName(SCHEMA_NS,"password"));
		assertEquals(new QName(SCHEMA_NS,"password"), passwdDef.getName());
		assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());
		assertFalse(passwdDef.isIgnored());

//		PrismContainerDefinition credDef = newObjectClassDef.findContainerDefinition(new QName(SchemaConstants.NS_C,"credentials"));
//		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
//		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());
//		assertFalse(credDef.isIgnored());

		PrismPropertyDefinition sepDef = newObjectClassDef.findPropertyDefinition(new QName(SCHEMA_NS,"sep"));
		assertEquals(new QName(SCHEMA_NS,"sep"), sepDef.getName());
		assertEquals(DOMUtil.XSD_STRING, sepDef.getTypeName());
		assertTrue(sepDef.isIgnored());

	}

}
