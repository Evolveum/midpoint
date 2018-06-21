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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPrismSchemaConstruction {

	private static final String NS_MY_SCHEMA = "http://midpoint.evolveum.com/xml/ns/test/my-1";
	private static final String WEAPON_TYPE_LOCAL_NAME = "WeaponType";
	private static final QName WEAPON_TYPE_QNAME = new QName(NS_MY_SCHEMA, WEAPON_TYPE_LOCAL_NAME);
	private static final QName WEAPON_KIND_QNAME = new QName(NS_MY_SCHEMA, "kind");
	private static final QName WEAPON_CREATE_TIMESTAMP_QNAME = new QName(NS_MY_SCHEMA, "createTimestamp");
	private static final String WEAPON_LOCAL_NAME = "weapon";
	private static final String WEAPON_BRAND_LOCAL_NAME = "brand";
	private static final int SCHEMA_ROUNDTRIP_LOOP_ATTEMPTS = 10;
	private static final String WEAPON_PASSWORD_LOCAL_NAME = "password";
	private static final String WEAPON_BLADE_LOCAL_NAME = "blade";


	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}

	@Test
	public void testConstructSchema() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testConstructSchema ]===");

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();

		// WHEN
		PrismSchema schema = constructSchema(ctx);

		// THEN
		System.out.println("Constructed schema");
		System.out.println(schema.debugDump());
		assertSchema(schema);
	}

	@Test
	public void testSchemaRoundtrip() throws Exception {
		System.out.println("===[ testSchemaRoundtrip ]===");

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();

		schemaRoundtrip(ctx);
	}

	@Test
	public void testSchemaRoundtripLoopShareContext() throws Exception {
		System.out.println("===[ testSchemaRoundtripLoopShareContext ]===");

		PrismContext ctx = constructInitializedPrismContext();
		for(int i=0; i < SCHEMA_ROUNDTRIP_LOOP_ATTEMPTS; i++) {
			System.out.println("\n--- attempt "+i+"---");
			schemaRoundtrip(ctx);
		}
	}

	@Test
	public void testSchemaRoundtripLoopNewContext() throws Exception {
		System.out.println("===[ testSchemaRoundtripLoopNewContext ]===");

		for(int i=0; i < SCHEMA_ROUNDTRIP_LOOP_ATTEMPTS; i++) {
			System.out.println("\n--- attempt "+i+"---");
			PrismContext ctx = constructInitializedPrismContext();
			schemaRoundtrip(ctx);
		}
	}


	private void schemaRoundtrip(PrismContext ctx) throws SchemaException, SAXException, IOException {

		PrismSchema schema = constructSchema(ctx);
		assertSchema(schema);

		// WHEN
		Document xsdDocument = schema.serializeToXsd();

		// THEN
		Element xsdElement = DOMUtil.getFirstChildElement(xsdDocument);
		System.out.println("Serialized schema");
		System.out.println(DOMUtil.serializeDOMToString(xsdElement));

		assertPrefix("xsd", xsdElement);
		Element displayNameElement = DOMUtil.findElementRecursive(xsdElement, PrismConstants.A_DISPLAY_NAME);
		assertPrefix(PrismConstants.PREFIX_NS_ANNOTATION, displayNameElement);

		// re-parse
		PrismSchema reparsedSchema = PrismSchemaImpl.parse(xsdElement, true, "serialized schema", ctx);
		System.out.println("Re-parsed schema");
		System.out.println(reparsedSchema.debugDump());
		assertSchema(reparsedSchema);
	}

	private PrismSchema constructSchema(PrismContext prismContext) {
		PrismSchemaImpl schema = new PrismSchemaImpl(NS_MY_SCHEMA, prismContext);

		ComplexTypeDefinitionImpl weaponTypeDef = (ComplexTypeDefinitionImpl) schema.createComplexTypeDefinition(WEAPON_TYPE_QNAME);
		PrismPropertyDefinitionImpl kindPropertyDef = weaponTypeDef.createPropertyDefinition(WEAPON_KIND_QNAME, DOMUtil.XSD_STRING);
		kindPropertyDef.setDisplayName("Weapon kind");
		weaponTypeDef.createPropertyDefinition(WEAPON_BRAND_LOCAL_NAME, PrismInternalTestUtil.WEAPONS_WEAPON_BRAND_TYPE_QNAME);
		weaponTypeDef.createPropertyDefinition(WEAPON_PASSWORD_LOCAL_NAME, PrismInternalTestUtil.DUMMY_PROTECTED_STRING_TYPE);
		weaponTypeDef.createPropertyDefinition(WEAPON_BLADE_LOCAL_NAME, PrismInternalTestUtil.EXTENSION_BLADE_TYPE_QNAME);
		PrismPropertyDefinitionImpl createTimestampPropertyDef = weaponTypeDef.createPropertyDefinition(WEAPON_CREATE_TIMESTAMP_QNAME, DOMUtil.XSD_DATETIME);
		createTimestampPropertyDef.setDisplayName("Create timestamp");
		createTimestampPropertyDef.setOperational(true);

		schema.createPropertyContainerDefinition(WEAPON_LOCAL_NAME, WEAPON_TYPE_LOCAL_NAME);

		return schema;
	}

	private void assertSchema(PrismSchema schema) {
		assertNotNull("Schema is null", schema);
		assertEquals("Wrong schema namespace", NS_MY_SCHEMA, schema.getNamespace());
		Collection<Definition> definitions = schema.getDefinitions();
		assertNotNull("Null definitions", definitions);
		assertFalse("Empty definitions", definitions.isEmpty());
		assertEquals("Unexpected number of definitions in schema", 2, definitions.size());

		Iterator<Definition> schemaDefIter = definitions.iterator();
		ComplexTypeDefinition weaponTypeDef = (ComplexTypeDefinition)schemaDefIter.next();
		assertEquals("Unexpected number of definitions in weaponTypeDef", 5, weaponTypeDef.getDefinitions().size());
		Iterator<? extends ItemDefinition> weaponTypeDefIter = weaponTypeDef.getDefinitions().iterator();
		PrismPropertyDefinition kindPropertyDef = (PrismPropertyDefinition) weaponTypeDefIter.next();
		PrismAsserts.assertDefinition(kindPropertyDef, WEAPON_KIND_QNAME, DOMUtil.XSD_STRING, 1, 1);
		assertEquals("Wrong kindPropertyDef displayName", "Weapon kind", kindPropertyDef.getDisplayName());
		assertFalse("kindPropertyDef IS operational", kindPropertyDef.isOperational());

		PrismPropertyDefinition brandPropertyDef = (PrismPropertyDefinition) weaponTypeDefIter.next();
		PrismAsserts.assertDefinition(brandPropertyDef, new QName(NS_MY_SCHEMA, WEAPON_BRAND_LOCAL_NAME),
				PrismInternalTestUtil.WEAPONS_WEAPON_BRAND_TYPE_QNAME, 1, 1);

		PrismPropertyDefinition passwordPropertyDef = (PrismPropertyDefinition) weaponTypeDefIter.next();
		PrismAsserts.assertDefinition(passwordPropertyDef, new QName(NS_MY_SCHEMA, WEAPON_PASSWORD_LOCAL_NAME),
				PrismInternalTestUtil.DUMMY_PROTECTED_STRING_TYPE, 1, 1);

		PrismPropertyDefinition bladePropertyDef = (PrismPropertyDefinition) weaponTypeDefIter.next();
		PrismAsserts.assertDefinition(bladePropertyDef, new QName(NS_MY_SCHEMA, WEAPON_BLADE_LOCAL_NAME),
				PrismInternalTestUtil.EXTENSION_BLADE_TYPE_QNAME, 1, 1);

		PrismPropertyDefinition createTimestampPropertyDef = (PrismPropertyDefinition) weaponTypeDefIter.next();
		PrismAsserts.assertDefinition(createTimestampPropertyDef, WEAPON_CREATE_TIMESTAMP_QNAME, DOMUtil.XSD_DATETIME, 1, 1);
		assertEquals("Wrong createTimestampPropertyDef displayName", "Create timestamp", createTimestampPropertyDef.getDisplayName());
		assertTrue("createTimestampPropertyDef not operational", createTimestampPropertyDef.isOperational());

		PrismContainerDefinition<?> weaponContDef = (PrismContainerDefinition<?>)schemaDefIter.next();
		assertEquals("Wrong complex type def in weaponContDef", weaponTypeDef, weaponContDef.getComplexTypeDefinition());
	}

	private void assertPrefix(String expectedPrefix, Element element) {
		assertEquals("Wrong prefix on element "+DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
	}

}
