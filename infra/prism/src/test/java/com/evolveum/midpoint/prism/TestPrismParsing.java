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

import static com.evolveum.midpoint.prism.PrismTestUtil.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class TestPrismParsing {
	
	private static final String TEST_DIRECTORY = "src/test/resources/parsing";
	private static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	private static final String NS_BAR = "http://www.example.com/bar";
	
	@BeforeSuite
	public void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix("http://midpoint.evolveum.com/xml/ns");
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
		
		PrismContainer extension = user.getExtension();
		assertPropertyValue(extension, new QName(NS_BAR, "bar"), "BAR");
		assertPropertyValue(extension, new QName(NS_BAR, "num"), 42);
		Set<PrismPropertyValue<Object>> multiPVals = extension.findProperty(new QName(NS_BAR, "multi")).getValues();
		assertEquals("Multi",3,multiPVals.size());
		
		PropertyPath barPath = new PropertyPath(new QName(NS_FOO,"extension"), new QName(NS_BAR,"bar"));
		PrismProperty barProperty = user.findProperty(barPath);
		assertNotNull("Property "+barPath+" not found", barProperty);
		assertPropertyValue(barProperty, "BAR");
		
		PropertyPath enabledPath = new PropertyPath(new QName(NS_FOO,"activation"), new QName(NS_FOO,"enabled"));
		PrismProperty enabledProperty1 = user.findProperty(enabledPath);
		assertNotNull("Property "+enabledPath+" not found", enabledProperty1);
		assertPropertyValue(enabledProperty1, true);
				
		QName actName = new QName(NS_FOO,"activation");
		// Use path
		PropertyPath actPath = new PropertyPath(actName);
		PrismContainer actContainer1 = user.findContainer(actPath);
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
		assertPropertyValue(enabledProperty2, true);
		assertEquals("Eh?",enabledProperty1,enabledProperty2);
		
		QName assName = new QName(NS_FOO,"assignment");
		QName descriptionName = new QName(NS_FOO,"description");
		PrismContainer assContainer = user.findContainer(assName);
		assertEquals("Wrong assignement values", 2, assContainer.getValues().size());
		PrismProperty a2DescProperty = assContainer.getValue("1112").findProperty(descriptionName);
		assertEquals("Wrong assigment 2 description", "Assignment 2", a2DescProperty.getValue().getValue());
		
		PropertyPath a1Path = new PropertyPath(new PropertyPathSegment(assName, "1111"),
				new PropertyPathSegment(descriptionName));
		PrismProperty a1Property = user.findProperty(a1Path);
		assertNotNull("Property "+a1Path+" not found", a1Property);
		assertPropertyValue(a1Property, "Assignment 1");

	}
	
	private void assertPropertyValue(PrismContainer container, String propName, Object propValue) {
		QName propQName = new QName(NS_FOO, propName);
		assertPropertyValue(container, propQName, propValue);
	}
		
	private void assertPropertyValue(PrismContainer container, QName propQName, Object propValue) {
		PrismProperty property = container.getValue().findProperty(propQName);
		assertNotNull("Property "+propQName+" not found in "+container, property);
		assertPropertyValue(property, propValue);
	}
	
	private void assertPropertyValue(PrismProperty property, Object propValue) {
		Set<PrismPropertyValue<Object>> pvals = property.getValues();
		QName propQName = property.getName();
		assertFalse("Empty property "+propQName, pvals == null || pvals.isEmpty());
		assertEquals("Numver of values of property "+propQName, 1, pvals.size());
		PrismPropertyValue<Object> pval = pvals.iterator().next();
		assertEquals("Values of property "+propQName, propValue, pval.getValue());
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

	
}
