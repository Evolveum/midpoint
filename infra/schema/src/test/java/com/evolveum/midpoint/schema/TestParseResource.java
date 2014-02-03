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
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType.Definition;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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
public class TestParseResource {
	
	public static final File RESOURCE_FILE = new File("src/test/resources/common/resource-opendj.xml");
	public static final File RESOURCE_SIMPLE_FILE = new File("src/test/resources/common/resource-opendj-simple.xml");
	private static final String RESOURCE_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String RESOURCE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseResourceFile() throws Exception {
		System.out.println("===[ testParseResourceFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true, true, false);
	}
	
	@Test
	public void testParseResourceFileSimple() throws Exception {
		System.out.println("===[ testParseResourceFileSimple ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_SIMPLE_FILE);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true, true, true);
	}

	@Test
	public void testParseResourceDom() throws Exception {
		System.out.println("===[ testParseResourceDom ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(RESOURCE_FILE);
		Element resourceElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(resourceElement);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true, true, false);
	}
	
	@Test
	public void testParseResourceDomSimple() throws Exception {
		System.out.println("===[ testParseResourceDomSimple ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(RESOURCE_SIMPLE_FILE);
		Element resourceElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(resourceElement);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true, true, true);
	}

	@Test
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ResourceType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_FILE, ResourceType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}
	
	@Test
	public void testPrismParseJaxbSimple() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbSimple ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ResourceType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_SIMPLE_FILE, ResourceType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, true);
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
		JaxbTestUtil jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ObjectType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_FILE, ObjectType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
	@Test
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElement ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<ResourceType> jaxbElement = jaxbProcessor.unmarshalElement(RESOURCE_FILE, ResourceType.class);
		ResourceType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
	@Test
	public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(RESOURCE_FILE, ObjectType.class);
		ObjectType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}

	
	@Test
	public void testParseResourceRoundtrip() throws Exception {
		System.out.println("===[ testParseResourceRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
		
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true, false, false);
		
		// SERIALIZE
		
		String serializedResource = prismContext.getPrismDomProcessor().serializeObjectToString(resource);
		
		System.out.println("serialized resource:");
		System.out.println(serializedResource);
		
		// RE-PARSE
		
		PrismObject<ResourceType> reparsedResource = prismContext.parseObject(serializedResource);
		
		System.out.println("Re-parsed resource:");
		System.out.println(reparsedResource.dump());
		
		// Cannot assert here. It will cause parsing of some of the raw values and diff will fail
		assertResource(resource, true, false, false);
		
		PrismProperty<Element> definitionProperty = reparsedResource.findContainer(ResourceType.F_SCHEMA).findProperty(XmlSchemaType.F_DEFINITION);
		Element definitionElement = definitionProperty.getValue().getValue();
		System.out.println("Re-parsed definition element:");
		System.out.println(DOMUtil.serializeDOMToString(definitionElement));
		
		ObjectDelta<ResourceType> objectDelta = resource.diff(reparsedResource);
		System.out.println("Delta:");
		System.out.println(objectDelta.dump());
		assertTrue("Delta is not empty", objectDelta.isEmpty());
		
		PrismAsserts.assertEquivalent("Resource re-parsed quivalence", resource, reparsedResource);
		
//		// Compare schema container
//		
//		PrismContainer<?> originalSchemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
//		PrismContainer<?> reparsedSchemaContainer = reparsedResource.findContainer(ResourceType.F_SCHEMA);
	}
	
	/**
	 * Serialize and parse "schema" element on its own. There may be problems e.g. with preservation
	 * of namespace definitions.
	 */
	@Test
	public void testSchemaRoundtrip() throws Exception {
		System.out.println("===[ testSchemaRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
				
		assertResource(resource, true, false, false);
		
		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		
		System.out.println("Parsed schema:");
		System.out.println(schemaContainer.dump());

		Element parentElement = DOMUtil.createElement(DOMUtil.getDocument(), new QName("fakeNs", "fake"));
		
		// SERIALIZE
		
		String serializesSchema = prismContext.getPrismDomProcessor().serializeObjectToString(schemaContainer.getValue(), parentElement);
		
		System.out.println("serialized schema:");
		System.out.println(serializesSchema);
		
		// RE-PARSE
		Document reparsedDocument = DOMUtil.parseDocument(serializesSchema);
		Element reparsedSchemaElement = DOMUtil.getFirstChildElement(DOMUtil.getFirstChildElement(reparsedDocument));
		PrismContainer<Containerable> reparsedSchemaContainer = prismContext.getPrismDomProcessor().parsePrismContainer(reparsedSchemaElement);
		
		System.out.println("Re-parsed schema container:");
		System.out.println(reparsedSchemaContainer.dump());
		
		Element reparsedXsdSchemaElement = DOMUtil.getChildElement(DOMUtil.getFirstChildElement(reparsedSchemaElement), DOMUtil.XSD_SCHEMA_ELEMENT);
		
		ResourceSchema reparsedSchema = ResourceSchema.parse(reparsedXsdSchemaElement, "reparsed schema", prismContext);
		
	}
	
	private void assertResource(PrismObject<ResourceType> resource, boolean checkConsistence, boolean checkJaxb, boolean isSimple) 
			throws SchemaException, JAXBException {
		if (checkConsistence) {
			resource.checkConsistence();
		}
		assertResourcePrism(resource, isSimple);
		assertResourceJaxb(resource.asObjectable(), isSimple);
		
		if (checkJaxb) {
			serializeDom(resource);
			serializeJaxb(resource);
		}
	}

	private void assertResourcePrism(PrismObject<ResourceType> resource, boolean isSimple) {
		assertEquals("Wrong oid (prism)", RESOURCE_OID, resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", PrismTestUtil.createPolyString("Embedded Test OpenDJ"));
		assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);		
		
		if (!isSimple) {
			assertPropertyValue(resource, "namespace", RESOURCE_NAMESPACE);
			assertPropertyDefinition(resource, "namespace", DOMUtil.XSD_ANYURI, 0, 1);
		}
		
		PrismReference connectorRef = resource.findReference(ResourceType.F_CONNECTOR_REF);
		assertNotNull("No connectorRef", connectorRef);
    	PrismReferenceValue connectorRefVal = connectorRef.getValue();
    	assertNotNull("No connectorRef value", connectorRefVal);
    	assertEquals("Wrong type in connectorRef value", ConnectorType.COMPLEX_TYPE, connectorRefVal.getTargetType());
    	Element filter = connectorRefVal.getFilter();
    	assertNotNull("No filter in connectorRef value", filter);
				
		PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", isSimple ? 1 : 4, configItems.size());
		
		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		PrismContainerDefinition<?> ldapConfigPropertiesContainerDef = ldapConfigPropertiesContainer.getDefinition();
		assertNotNull("No icfcldap:configurationProperties container definition", ldapConfigPropertiesContainerDef);
		assertEquals("icfcldap:configurationProperties container definition maxOccurs", 1, ldapConfigPropertiesContainerDef.getMaxOccurs());
		List<Item<?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of ldapConfigPropItems items", 7, ldapConfigPropItems.size());
		
		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		if (isSimple) {
			assertNull("Schema sneaked in", schemaContainer);
		} else {
			assertNotNull("No schema container", schemaContainer);
		}
		
		PrismProperty<?> schemaHandlingProperty = resource.findProperty(ResourceType.F_SCHEMA_HANDLING);
		if (isSimple) {
			assertNull("SchemaHandling sneaked in", schemaHandlingProperty);
		} else {
			assertNotNull("No schemaHandling property", schemaHandlingProperty);
		}
	}
	
	private void assertResourceJaxb(ResourceType resourceType, boolean isSimple) {
		assertEquals("Wrong oid (JAXB)", RESOURCE_OID, resourceType.getOid());
		assertEquals("Wrong name (JAXB)", PrismTestUtil.createPolyStringType("Embedded Test OpenDJ"), resourceType.getName());
		String expectedNamespace = RESOURCE_NAMESPACE;
		if (isSimple) {
			expectedNamespace = MidPointConstants.NS_RI;
		}
		assertEquals("Wrong namespace (JAXB)", expectedNamespace, ResourceTypeUtil.getResourceNamespace(resourceType));
		
		ObjectReferenceType connectorRef = resourceType.getConnectorRef();
		assertNotNull("No connectorRef (JAXB)", connectorRef);
		assertEquals("Wrong type in connectorRef (JAXB)", ConnectorType.COMPLEX_TYPE, connectorRef.getType());
		ObjectReferenceType.Filter filter = connectorRef.getFilter();
    	assertNotNull("No filter in connectorRef (JAXB)", filter);
    	Element filterElement = filter.getFilter();
    	assertNotNull("No filter element in connectorRef (JAXB)", filterElement);
    	
    	XmlSchemaType xmlSchemaType = resourceType.getSchema();
    	SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
    	if (isSimple) {
    		assertNull("Schema sneaked in", xmlSchemaType);
    		assertNull("SchemaHandling sneaked in", schemaHandling);
    	} else {
	    	assertNotNull("No schema element (JAXB)", xmlSchemaType);
	    	XmlSchemaType.Definition definition = xmlSchemaType.getDefinition();
	    	assertNotNull("No definition element in schema (JAXB)", definition);
	    	List<Element> anyElements = definition.getAny();
	    	assertNotNull("Null element list in definition element in schema (JAXB)", anyElements);
	    	assertFalse("Empty element list in definition element in schema (JAXB)", anyElements.isEmpty());
			
			assertNotNull("No schema handling (JAXB)", schemaHandling);
			for(ResourceObjectTypeDefinitionType accountType: schemaHandling.getAccountType()) {
				String name = accountType.getName();
				assertNotNull("Account type without a name", name);
				assertNotNull("Account type "+name+" does not have an objectClass", accountType.getObjectClass());
			}
    	}
	}
	
	// Try to serialize it to DOM using just DOM processor. See if it does not fail.
	private void serializeDom(PrismObject<ResourceType> resource) throws SchemaException {
		PrismDomProcessor domProcessor = PrismTestUtil.getPrismContext().getPrismDomProcessor();
		Element domElement = domProcessor.serializeToDom(resource);
		assertNotNull("Null resulting DOM element after DOM serialization", domElement);
	}

	// Try to serialize it to DOM using JAXB processor. See if it does not fail.
	private void serializeJaxb(PrismObject<ResourceType> resource) throws SchemaException, JAXBException {
		JaxbTestUtil jaxbProcessor = PrismTestUtil.getPrismContext().getPrismJaxbProcessor();
		Document document = DOMUtil.getDocument();
		Element element = jaxbProcessor.marshalObjectToDom(resource.asObjectable(), new QName(SchemaConstants.NS_C, "resorce"), document);
		System.out.println("JAXB serialization result:\n"+DOMUtil.serializeDOMToString(element));
		assertNotNull("No resulting DOM element after JAXB serialization", element);
		assertNotNull("Empty resulting DOM element after JAXB serialization", element.getChildNodes().getLength() != 0);
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
