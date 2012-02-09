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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.PrismObject;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * @author semancik
 *
 */
public class TestPrismParsing {
	
	private static final String TEST_DIRECTORY = "src/test/resources/parsing";
	private static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	private static final String NS_BAR = "http://www.example.com/bar";

	@Test
	public void testPrismContextConstruction() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseDom ]===");
		
		// WHEN
		PrismContext prismContext = constructPrismContext();
		
		// THEN
		assertNotNull("No prism context", prismContext);
		
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);
		
		System.out.println("Schema registry:");
		System.out.println(schemaRegistry.dump());

		Schema objectSchema = schemaRegistry.getObjectSchema();
		System.out.println("Object schema:");
		System.out.println(objectSchema.dump());
		
		ObjectDefinition<UserType> userDefinition = objectSchema.findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		assertNotNull("No user definition", userDefinition);

	}
	
	@Test
	public void testPrismParseDom() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseDom ]===");
		
		// GIVEN
		Document document = DOMUtil.parseFile(new File(TEST_DIRECTORY, "user-jack.xml"));
		Element userElement = DOMUtil.getFirstChildElement(document);
		
		PrismContext prismContext = constructPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(userElement);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		assertEquals("Wrong oid", "c0c010c0-d34d-b33f-f00d-111111111111", user.getOid());
		assertEquals("Wrong version", "42", user.getVersion());
		assertPropertyValue(user, "fullName", "cpt. Jack Sparrow");
		assertPropertyValue(user, "givenName", "Jack");
		assertPropertyValue(user, "familyName", "Sparrow");
		assertPropertyValue(user, "name", "jack");
		
		PropertyContainer extension = user.getExtension();
		assertPropertyValue(extension, new QName(NS_BAR, "bar"), "BAR");
		assertPropertyValue(extension, new QName(NS_BAR, "num"), 42);
		Set<PropertyValue<Object>> multiPVals = extension.findProperty(new QName(NS_BAR, "multi")).getValues();
		assertEquals("Multi",3,multiPVals.size());
	}
	
	private void assertPropertyValue(PropertyContainer container, String propName, Object propValue) {
		QName propQName = new QName(NS_FOO, propName);
		assertPropertyValue(container, propQName, propValue);
	}
		
	private void assertPropertyValue(PropertyContainer container, QName propQName, Object propValue) {
		Property property = container.findProperty(propQName);
		assertNotNull("Property "+propQName+" not found in "+container, property);
		Set<PropertyValue<Object>> pvals = property.getValues();
		assertFalse("Empty property "+propQName+" in "+container, pvals == null || pvals.isEmpty());
		assertEquals("Numver of values of property "+propQName+" in "+container, 1, pvals.size());
		PropertyValue<Object> pval = pvals.iterator().next();
		assertEquals("Values of property "+propQName+" in "+container, propValue, pval.getValue());
	}

//	@Test
//	public void testParseFromJaxb() throws SchemaException, SAXException, IOException {
//		PrismContext prismContext = constructPrismContext();
//		
//		UserType userType = new UserType();
//		userType.setOid("01d");
//		userType.setName("jack");
//		userType.setGivenName("Jack");
//		userType.setFamilyName("Sparrow");
//		userType.setFullName("Cpt. Jack Sparrow");
//		
//		PrismObject<UserType> user = prismContext.parseJaxb(userType);
//		assertNotNull(user);
//		
//	}

	private PrismContext constructPrismContext() throws SchemaException, SAXException, IOException {
		
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		DynamicNamespacePrefixMapper prefixMapper = new GlobalDynamicNamespacePrefixMapper();
		// Set default namespace?
		schemaRegistry.setNamespacePrefixMapper(prefixMapper);
		schemaRegistry.registerPrismSchemaResource("xml/ns/test/foo-1.xsd", "foo", ObjectFactory.class.getPackage());
		schemaRegistry.setObjectSchemaNamespace(NS_FOO);
		schemaRegistry.initialize();
		
		PrismContext context = PrismContext.create(schemaRegistry);
		return context;
	}
	
}
