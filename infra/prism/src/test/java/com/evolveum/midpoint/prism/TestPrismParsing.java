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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPrismParsing {
		
	@BeforeSuite
	public void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}
	
	@Test
	public void testPrismParseFile() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseFile ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(USER_JACK_FILE);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		assertUser(user);
	}
	
	@Test
	public void testPrismParseFileObject() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseFileObject ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(USER_JACK_OBJECT_FILE);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		assertUser(user);
	}
	
	@Test
	public void testPrismParseDom() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseDom ]===");
		
		// GIVEN
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
		
		PrismContext prismContext = constructInitializedPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(userElement);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		assertUser(user);
	}

	
	@Test
	public void testRoundTrip() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testRoundTrip ]===");
		
		roundTrip(USER_JACK_FILE);
	}

	@Test
	public void testRoundTripObject() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testRoundTripObject ]===");
		
		roundTrip(USER_JACK_OBJECT_FILE);
	}

	private void roundTrip(File file) throws SchemaException, SAXException, IOException {
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		PrismObject<UserType> originalUser = prismContext.parseObject(file);
	
		System.out.println("Input parsed user:");
		System.out.println(originalUser.dump());
		assertNotNull(originalUser);
		
		// precondition
		assertUser(originalUser);
		
		// WHEN
		String userXml = prismContext.getPrismDomProcessor().serializeObjectToString(originalUser);
	
		// THEN
		System.out.println("Serialized user:");
		System.out.println(userXml);
		assertNotNull(userXml);
		
		// WHEN
		PrismObject<UserType> parsedUser = prismContext.parseObject(userXml);
		System.out.println("Re-parsed user:");
		System.out.println(parsedUser.dump());
		assertNotNull(parsedUser);

		assertUser(parsedUser);
		
		assertTrue("Users not equal", originalUser.equals(parsedUser));
	}

	
//  Cannot be tested here, as the JAXB classes are not properly generated. It is tested in "schema" component now.
//	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
//	}
	
	private void assertUser(PrismObject<UserType> user) {
		assertUserContent(user);
		assertVisitor(user);
	}
	
	private void assertUserContent(PrismObject<UserType> user) {
		
		assertEquals("Wrong oid", USER_JACK_OID, user.getOid());
		assertEquals("Wrong version", "42", user.getVersion());
		PrismAsserts.assertObjectDefinition(user.getDefinition(), USER_QNAME, USER_TYPE_QNAME, UserType.class);
		PrismAsserts.assertParentConsistency(user);
		
		assertPropertyValue(user, "fullName", "cpt. Jack Sparrow");
		assertPropertyDefinition(user, "fullName", DOMUtil.XSD_STRING, 1, 1);
		assertPropertyValue(user, "givenName", "Jack");
		assertPropertyDefinition(user, "givenName", DOMUtil.XSD_STRING, 1, 1);
		assertPropertyValue(user, "familyName", "Sparrow");
		assertPropertyDefinition(user, "familyName", DOMUtil.XSD_STRING, 1, 1);
		assertPropertyValue(user, "name", "jack");
		assertPropertyDefinition(user, "name", DOMUtil.XSD_STRING, 0, 1);
		
		PrismContainer extension = user.getExtension();
		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
		PrismContainerValue extensionValue = extension.getValue();
		assertTrue("Extension parent", extensionValue.getParent() == extension);
		assertNull("Extension ID", extensionValue.getId());
		PrismAsserts.assertPropertyValue(extension, new QName(NS_USER_EXT, "bar"), "BAR");
		PrismAsserts.assertPropertyValue(extension, new QName(NS_USER_EXT, "num"), 42);
		Collection<PrismPropertyValue<Object>> multiPVals = extension.findProperty(new QName(NS_USER_EXT, "multi")).getValues();
		assertEquals("Multi",3,multiPVals.size());
		
		PropertyPath barPath = new PropertyPath(new QName(NS_FOO,"extension"), new QName(NS_USER_EXT,"bar"));
		PrismProperty barProperty = user.findProperty(barPath);
		assertNotNull("Property "+barPath+" not found", barProperty);
		PrismAsserts.assertPropertyValue(barProperty, "BAR");
		
		PropertyPath enabledPath = USER_ENABLED_PATH;
		PrismProperty enabledProperty1 = user.findProperty(enabledPath);
		PrismAsserts.assertDefinition(enabledProperty1.getDefinition(), USER_ENABLED_QNAME, DOMUtil.XSD_BOOLEAN, 1, 1);
		assertNotNull("Property "+enabledPath+" not found", enabledProperty1);
		PrismAsserts.assertPropertyValue(enabledProperty1, true);
		
		PrismProperty validFromProperty = user.findProperty(USER_VALID_FROM_PATH);
		assertNotNull("Property "+USER_VALID_FROM_PATH+" not found", validFromProperty);
		PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);
				
		QName actName = new QName(NS_FOO,"activation");
		// Use path
		PropertyPath actPath = new PropertyPath(actName);
		PrismContainer actContainer1 = user.findContainer(actPath);
		assertContainerDefinition(actContainer1, "activation", ACTIVATION_TYPE_QNAME, 0, 1);
		assertNotNull("Property "+actPath+" not found", actContainer1);
		assertEquals("Wrong activation name",actName,actContainer1.getName());
		// Use name
		PrismContainer actContainer2 = user.findContainer(actName);
		assertNotNull("Property "+actName+" not found", actContainer2);
		assertEquals("Wrong activation name",actName,actContainer2.getName());
		// Compare
		assertEquals("Eh?",actContainer1,actContainer2);
		
		PrismProperty enabledProperty2 = actContainer1.findProperty(new QName(NS_FOO,"enabled"));
		assertNotNull("Property enabled not found", enabledProperty2);
		PrismAsserts.assertPropertyValue(enabledProperty2, true);
		assertEquals("Eh?",enabledProperty1,enabledProperty2);
		
		QName assName = new QName(NS_FOO,"assignment");
		QName descriptionName = new QName(NS_FOO,"description");
		PrismContainer assContainer = user.findContainer(assName);
		assertEquals("Wrong assignement values", 2, assContainer.getValues().size());
		PrismProperty a2DescProperty = assContainer.getValue(USER_ASSIGNMENT_2_ID).findProperty(descriptionName);
		assertEquals("Wrong assigment 2 description", "Assignment 2", a2DescProperty.getValue().getValue());
		
		PropertyPath a1Path = new PropertyPath(new PropertyPathSegment(assName, USER_ASSIGNMENT_1_ID),
				new PropertyPathSegment(descriptionName));
		PrismProperty a1Property = user.findProperty(a1Path);
		assertNotNull("Property "+a1Path+" not found", a1Property);
		PrismAsserts.assertPropertyValue(a1Property, "Assignment 1");
		
		PrismReference accountRef = user.findReference(USER_ACCOUNTREF_QNAME);
		assertNotNull("Reference "+USER_ACCOUNTREF_QNAME+" not found", accountRef);
		assertEquals("Wrong number of accountRef values", 3, accountRef.getValues().size());
		PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1111");
		PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1112");
		PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1113");
		PrismReferenceValue accountRefVal2 = accountRef.findValueByOid("c0c010c0-d34d-b33f-f00d-aaaaaaaa1112");
		assertEquals("Wrong oid for accountRef", "c0c010c0-d34d-b33f-f00d-aaaaaaaa1112", accountRefVal2.getOid());
		assertEquals("Wrong accountRef description", "This is a reference with a filter", accountRefVal2.getDescription());
		assertNotNull("No filter in accountRef", accountRefVal2.getFilter());
	}
	
	private void assertVisitor(PrismObject<UserType> user) {
		final List<Visitable> visits = new ArrayList<Visitable>();
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				visits.add(visitable);
				System.out.println("Visiting: "+visitable);
			}
		};
		user.accept(visitor);
		assertEquals("Wrong number of visits", 42, visits.size());
	}
	
	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(NS_FOO, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}

	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(NS_FOO, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(NS_FOO, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	
}
