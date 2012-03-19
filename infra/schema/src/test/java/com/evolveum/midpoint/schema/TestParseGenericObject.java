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
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertTrue;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class TestParseGenericObject {
	
	public static final File GENERIC_FILE = new File("src/test/resources/common/generic-sample-configuration.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseGenericFile() throws SchemaException {
		System.out.println("===[ testParseGenericFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<GenericObjectType> generic = prismContext.parseObject(GENERIC_FILE);
		
		// THEN
		System.out.println("Parsed generic object:");
		System.out.println(generic.dump());
		
		assertGenericObject(generic);
	}

	@Test
	public void testParseGenericDom() throws SchemaException {
		System.out.println("===[ testParseGenericDom ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(GENERIC_FILE);
		Element resourceElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<GenericObjectType> generic = prismContext.parseObject(resourceElement);
		
		// THEN
		System.out.println("Parsed generic object:");
		System.out.println(generic.dump());
		
		assertGenericObject(generic);
	}

	@Test
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		GenericObjectType genericType = jaxbProcessor.unmarshalObject(GENERIC_FILE, GenericObjectType.class);
		
		// THEN
		assertGenericObject(genericType.asPrismObject());
	}
	
	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 */
	@Test
	public void testPrismParseJaxbObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ObjectType genericType = jaxbProcessor.unmarshalObject(GENERIC_FILE, ObjectType.class);
		
		// THEN
		assertGenericObject(genericType.asPrismObject());
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
	@Test
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElement ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<GenericObjectType> jaxbElement = jaxbProcessor.unmarshalElement(GENERIC_FILE, GenericObjectType.class);
		GenericObjectType genericType = jaxbElement.getValue();
		
		// THEN
		assertGenericObject(genericType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
	@Test
	public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(GENERIC_FILE, ObjectType.class);
		ObjectType genericType = jaxbElement.getValue();
		
		// THEN
		assertGenericObject(genericType.asPrismObject());
	}

	
	@Test
	public void testParseGenericRoundtrip() throws SchemaException {
		System.out.println("===[ testParseGenericRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<GenericObjectType> generic = prismContext.parseObject(GENERIC_FILE);
		
		System.out.println("Parsed generic object:");
		System.out.println(generic.dump());
		
		assertGenericObject(generic);
		
		// SERIALIZE
		
		String serializedGeneric = prismContext.getPrismDomProcessor().serializeObjectToString(generic);
		
		System.out.println("serialized generic object:");
		System.out.println(serializedGeneric);
		
		// RE-PARSE
		
		PrismObject<GenericObjectType> reparsedGeneric = prismContext.parseObject(serializedGeneric);
		
		System.out.println("Re-parsed generic object:");
		System.out.println(reparsedGeneric.dump());
		
		assertGenericObject(generic);
				
		ObjectDelta<GenericObjectType> objectDelta = generic.diff(reparsedGeneric);
		System.out.println("Delta:");
		System.out.println(objectDelta.dump());
		assertTrue("Delta is not empty", objectDelta.isEmpty());
		
		PrismAsserts.assertEquivalent("generic object re-parsed quivalence", generic, reparsedGeneric);
		
//		// Compare schema container
//		
//		PrismContainer<?> originalSchemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
//		PrismContainer<?> reparsedSchemaContainer = reparsedResource.findContainer(ResourceType.F_SCHEMA);
	}
	
	private void assertGenericObject(PrismObject<GenericObjectType> generic) {
		
		assertEquals("Wrong oid", "c0c010c0-d34d-b33f-f00d-999111111111", generic.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<GenericObjectType> resourceDefinition = generic.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstants.NS_COMMON, "genericObject"), 
				GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
		assertEquals("Wrong class in resource", GenericObjectType.class, generic.getCompileTimeClass());
		GenericObjectType genericType = generic.asObjectable();
		assertNotNull("asObjectable resulted in null", genericType);

		assertPropertyValue(generic, "name", "My Sample Config Object");
		assertPropertyDefinition(generic, "name", DOMUtil.XSD_STRING, 0, 1);		
		assertPropertyValue(generic, "objectType", "http://myself.me/schemas/objects#SampleConfigType");
		assertPropertyDefinition(generic, "objectType", DOMUtil.XSD_ANYURI, 1, 1);
				
		PrismContainer<?> extensionContainer = generic.findContainer(GenericObjectType.F_EXTENSION);
		assertContainerDefinition(extensionContainer, "extension", ExtensionType.COMPLEX_TYPE, 0, 1);
		PrismContainerValue<?> extensionContainerValue = extensionContainer.getValue();
		List<Item<?>> configItems = extensionContainerValue.getItems();
		assertEquals("Wrong number of extension items", 1, configItems.size());

		// TODO
						
	}
	
	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstants.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstants.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}
	
	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstants.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}

}
