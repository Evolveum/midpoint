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
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.XmlSchemaType;

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
	private static final String RESOURCE_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String RESOURCE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseResourceFile() throws SchemaException {
		System.out.println("===[ testParseResourceFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true);
	}

	@Test
	public void testParseResourceDom() throws SchemaException {
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
		
		assertResource(resource, true);
	}

	@Test
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ResourceType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_FILE, ResourceType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false);
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
		ObjectType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_FILE, ObjectType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false);
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
		JAXBElement<ResourceType> jaxbElement = jaxbProcessor.unmarshalElement(RESOURCE_FILE, ResourceType.class);
		ResourceType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false);
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
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(RESOURCE_FILE, ObjectType.class);
		ObjectType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false);
	}

	
	@Test
	public void testParseResourceRoundtrip() throws SchemaException {
		System.out.println("===[ testParseResourceRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
		
		System.out.println("Parsed resource:");
		System.out.println(resource.dump());
		
		assertResource(resource, true);
		
		// SERIALIZE
		
		String serializedResource = prismContext.getPrismDomProcessor().serializeObjectToString(resource);
		
		System.out.println("serialized resource:");
		System.out.println(serializedResource);
		
		// RE-PARSE
		
		PrismObject<ResourceType> reparsedResource = prismContext.parseObject(serializedResource);
		
		System.out.println("Re-parsed resource:");
		System.out.println(reparsedResource.dump());
		
		assertResource(resource, true);
		
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
	public void testSchemaRoundtrip() throws SchemaException {
		System.out.println("===[ testSchemaRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_FILE);
				
		assertResource(resource, true);
		
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
	
	private void assertResource(PrismObject<ResourceType> resource, boolean checkConsistence) {
		if (checkConsistence) {
			resource.checkConsistence();
		}
		assertResourcePrism(resource);
		assertResourceJaxb(resource.asObjectable());
	}
		
	private void assertResourcePrism(PrismObject<ResourceType> resource) {
		assertEquals("Wrong oid (prism)", RESOURCE_OID, resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", "Embedded Test OpenDJ");
		assertPropertyDefinition(resource, "name", DOMUtil.XSD_STRING, 0, 1);		
		assertPropertyValue(resource, "namespace", RESOURCE_NAMESPACE);
		assertPropertyDefinition(resource, "namespace", DOMUtil.XSD_ANYURI, 0, 1);
		
		PrismReference connectorRef = resource.findReference(ResourceType.F_CONNECTOR_REF);
		assertNotNull("No connectorRef", connectorRef);
    	PrismReferenceValue connectorRefVal = connectorRef.getValue();
    	assertNotNull("No connectorRef value", connectorRefVal);
    	assertEquals("Wrong type in connectorRef value", ConnectorType.COMPLEX_TYPE, connectorRefVal.getTargetType());
    	Element filter = connectorRefVal.getFilter();
    	assertNotNull("No filter in connectorRef value", filter);
				
		PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ResourceConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", 4, configItems.size());
		
		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		PrismContainerDefinition<?> ldapConfigPropertiesContainerDef = ldapConfigPropertiesContainer.getDefinition();
		assertNotNull("No icfcldap:configurationProperties container definition", ldapConfigPropertiesContainerDef);
		assertEquals("icfcldap:configurationProperties container definition maxOccurs", 1, ldapConfigPropertiesContainerDef.getMaxOccurs());
		List<Item<?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of ldapConfigPropItems items", 7, ldapConfigPropItems.size());
	}
	
	private void assertResourceJaxb(ResourceType resourceType) {
		assertEquals("Wrong oid (JAXB)", RESOURCE_OID, resourceType.getOid());
		assertEquals("Wrong name (JAXB)", "Embedded Test OpenDJ", resourceType.getName());
		assertEquals("Wrong namespace (JAXB)", RESOURCE_NAMESPACE, ResourceTypeUtil.getResourceNamespace(resourceType));
		
		ObjectReferenceType connectorRef = resourceType.getConnectorRef();
		assertNotNull("No connectorRef (JAXB)", connectorRef);
		assertEquals("Wrong type in connectorRef (JAXB)", ConnectorType.COMPLEX_TYPE, connectorRef.getType());
		ObjectReferenceType.Filter filter = connectorRef.getFilter();
    	assertNotNull("No filter in connectorRef (JAXB)", filter);
    	Element filterElement = filter.getFilter();
    	assertNotNull("No filter element in connectorRef (JAXB)", filterElement);
		
		SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
		assertNotNull("No schema handling (JAXB)", schemaHandling);
		for(ResourceAccountTypeDefinitionType accountType: schemaHandling.getAccountType()) {
			String name = accountType.getName();
			assertNotNull("Account type without a name", name);
			assertNotNull("Account type "+name+" does not have an objectClass", accountType.getObjectClass());
		}
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
