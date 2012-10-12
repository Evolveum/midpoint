/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
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
	private static final String WEAPON_LOCAL_NAME = "weapon";
	
	
	
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
		System.out.println(schema.dump());
		assertSchema(schema);
	}
	
	@Test
	public void testSchemaRoundtrip() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testSchemaRoundtrip ]===");
		
		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
	
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
		PrismSchema reparsedSchema = PrismSchema.parse(xsdElement, "serialized schema", ctx);
		System.out.println("Re-parsed schema");
		System.out.println(reparsedSchema.dump());
		assertSchema(reparsedSchema);
	}

	private PrismSchema constructSchema(PrismContext prismContext) {
		PrismSchema schema = new PrismSchema(NS_MY_SCHEMA, prismContext);
		
		ComplexTypeDefinition weaponTypeDef = schema.createComplexTypeDefinition(WEAPON_TYPE_QNAME);
		PrismPropertyDefinition kindPropertyDef = weaponTypeDef.createPropertyDefinifion(WEAPON_KIND_QNAME, DOMUtil.XSD_STRING);
		kindPropertyDef.setDisplayName("Weapon kind");
		
		schema.createPropertyContainerDefinition(WEAPON_LOCAL_NAME, WEAPON_TYPE_LOCAL_NAME);
		
		return schema;
	}
	
	private void assertSchema(PrismSchema schema) {
		assertNotNull("Schema is null", schema);		
		assertEquals("Wrong schema namespace", NS_MY_SCHEMA, schema.getNamespace());
		Collection<Definition> definitions = schema.getDefinitions();
		assertNotNull("Null definitions", definitions);
		assertFalse("Empty definitions", definitions.isEmpty());
		assertEquals("Unexpected number of definitions", 2, definitions.size());
		
		// TODO: more asserts
		
	}
	
	private void assertPrefix(String expectedPrefix, Element element) {
		assertEquals("Wrong prefix on element "+DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
	}	
	
}
