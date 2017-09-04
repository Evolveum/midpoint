/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}


	@Test
	public void testParseGenericFile() throws Exception {
		System.out.println("===[ testParseGenericFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		PrismObject<GenericObjectType> generic = prismContext.parserFor(GENERIC_FILE).xml().parse();

		// THEN
		System.out.println("Parsed generic object:");
		System.out.println(generic.debugDump());

		assertGenericObject(generic);
	}

	@Test
	public void testParseGenericDom() throws SchemaException, DatatypeConfigurationException {
		System.out.println("===[ testParseGenericDom ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		Document document = DOMUtil.parseFile(GENERIC_FILE);
		Element resourceElement = DOMUtil.getFirstChildElement(document);

		// WHEN
		PrismObject<GenericObjectType> generic = prismContext.parserFor(resourceElement).parse();

		// THEN
		System.out.println("Parsed generic object:");
		System.out.println(generic.debugDump());

		assertGenericObject(generic);
	}

    @Deprecated
	@Test(enabled = false)
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException, DatatypeConfigurationException {
		System.out.println("===[ testPrismParseJaxb ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

        // WHEN
		GenericObjectType genericType = jaxbProcessor.unmarshalObject(GENERIC_FILE, GenericObjectType.class);

		// THEN
		assertGenericObject(genericType.asPrismObject());
	}

	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 * @throws DatatypeConfigurationException
	 */
    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbObjectType() throws JAXBException, SchemaException, SAXException, IOException, DatatypeConfigurationException {
		System.out.println("===[ testPrismParseJaxbObjectType ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

		// WHEN
		GenericObjectType genericType = jaxbProcessor.unmarshalObject(GENERIC_FILE, GenericObjectType.class);

		// THEN
		assertGenericObject(genericType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement
	 * @throws DatatypeConfigurationException
	 */
    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException, DatatypeConfigurationException {
		System.out.println("===[ testPrismParseJaxbElement ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

		// WHEN
		JAXBElement<GenericObjectType> jaxbElement = jaxbProcessor.unmarshalElement(GENERIC_FILE, GenericObjectType.class);
		GenericObjectType genericType = jaxbElement.getValue();

		// THEN
		assertGenericObject(genericType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 * @throws DatatypeConfigurationException
	 */
    @Deprecated
    @Test(enabled = false)
    public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException, DatatypeConfigurationException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

		// WHEN
		JAXBElement<GenericObjectType> jaxbElement = jaxbProcessor.unmarshalElement(GENERIC_FILE, GenericObjectType.class);
		GenericObjectType genericType = jaxbElement.getValue();

		// THEN
		assertGenericObject(genericType.asPrismObject());
	}


	@Test
	public void testParseGenericRoundtrip() throws Exception {
		System.out.println("===[ testParseGenericRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		PrismObject<GenericObjectType> generic = prismContext.parseObject(GENERIC_FILE);

		System.out.println("Parsed generic object:");
		System.out.println(generic.debugDump());

		assertGenericObject(generic);

		// SERIALIZE

		String serializedGeneric = prismContext.serializeObjectToString(generic, PrismContext.LANG_XML);

		System.out.println("serialized generic object:");
		System.out.println(serializedGeneric);

		// RE-PARSE

		PrismObject<GenericObjectType> reparsedGeneric = prismContext.parseObject(serializedGeneric);

		System.out.println("Re-parsed generic object:");
		System.out.println(reparsedGeneric.debugDump());

		assertGenericObject(generic);

		ObjectDelta<GenericObjectType> objectDelta = generic.diff(reparsedGeneric);
		System.out.println("Delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("Delta is not empty", objectDelta.isEmpty());

		PrismAsserts.assertEquivalent("generic object re-parsed quivalence", generic, reparsedGeneric);

//		// Compare schema container
//
//		PrismContainer<?> originalSchemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
//		PrismContainer<?> reparsedSchemaContainer = reparsedResource.findContainer(ResourceType.F_SCHEMA);
	}

	private void assertGenericObject(PrismObject<GenericObjectType> generic) throws DatatypeConfigurationException {

		generic.checkConsistence();

		assertEquals("Wrong oid", "c0c010c0-d34d-b33f-f00d-999111111111", generic.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<GenericObjectType> resourceDefinition = generic.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "genericObject"),
				GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
		assertEquals("Wrong class in resource", GenericObjectType.class, generic.getCompileTimeClass());
		GenericObjectType genericType = generic.asObjectable();
		assertNotNull("asObjectable resulted in null", genericType);

		assertPropertyValue(generic, "name", PrismTestUtil.createPolyString("My Sample Config Object"));
		assertPropertyDefinition(generic, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		assertPropertyValue(generic, "objectType", QNameUtil.qNameToUri(
				new QName(SchemaTestConstants.NS_EXTENSION, "SampleConfigType")));
		assertPropertyDefinition(generic, "objectType", DOMUtil.XSD_ANYURI, 1, 1);

		PrismContainer<?> extensionContainer = generic.findContainer(GenericObjectType.F_EXTENSION);
		assertContainerDefinition(extensionContainer, "extension", ExtensionType.COMPLEX_TYPE, 0, 1);
		PrismContainerDefinition<?> extensionContainerDefinition = extensionContainer.getDefinition();
		assertTrue("Extension container definition is NOT dynamic", extensionContainerDefinition.isDynamic());
		PrismContainerValue<?> extensionContainerValue = extensionContainer.getValue();
		List<Item<?,?>> extensionItems = extensionContainerValue.getItems();
		assertEquals("Wrong number of extension items", 5, extensionItems.size());

		// COMPLEX RUN TIME PROPERTIES...NOT SUPPORTED WITH NEW PARSERS..
//		Item<?> locationsItem = extensionContainerValue.findItem(SchemaTestConstants.EXTENSION_LOCATIONS_ELEMENT);
//		if (!(locationsItem instanceof PrismProperty)) {
//			AssertJUnit.fail("Expected the extension item to be of type "+PrismProperty.class+
//					"but it was of type "+locationsItem.getClass());
//		}
//		PrismProperty<?> locationsProperty = (PrismProperty<?>)locationsItem;
//		assertEquals("Wrong name of <locations>", SchemaTestConstants.EXTENSION_LOCATIONS_ELEMENT, locationsProperty.getElementName());
//		PrismPropertyDefinition locationsDefinition = locationsProperty.getDefinition();
//		assertNotNull("No definition for <locations>", locationsDefinition);
//		PrismAsserts.assertDefinition(locationsDefinition, SchemaTestConstants.EXTENSION_LOCATIONS_ELEMENT,
//				SchemaTestConstants.EXTENSION_LOCATIONS_TYPE, 0, -1);

		PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_STRING_TYPE_ELEMENT, "X marks the spot");
		PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT, 1234);
		PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_DOUBLE_TYPE_ELEMENT, 456.789D);
		PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT, 567890L);
		XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("2002-05-30T09:10:11");
		PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_DATE_TYPE_ELEMENT, calendar);

	}

	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}

}
