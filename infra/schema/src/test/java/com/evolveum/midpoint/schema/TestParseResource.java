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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
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
	
	public static final File RESOURCE_FILE = new File("src/test/resources/schema/resource-opendj.xml");
	
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
		
		assertResource(resource);
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
		
		assertResource(resource);
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
		assertResource(resourceType.asPrismObject());
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
		assertResource(resourceType.asPrismObject());
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
		assertResource(resourceType.asPrismObject());
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
		assertResource(resourceType.asPrismObject());
	}

	
	private void assertResource(PrismObject<ResourceType> resource) {
		
		assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff", resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstants.NS_COMMON, "resource"), 
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", "Embedded Test OpenDJ");
		assertPropertyDefinition(resource, "name", DOMUtil.XSD_STRING, 0, 1);		
		assertPropertyValue(resource, "namespace", "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff");
		assertPropertyDefinition(resource, "namespace", DOMUtil.XSD_ANYURI, 1, 1);
				
		PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ResourceConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", 4, configItems.size());
		
		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		List<Item<?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of ldapConfigPropItems items", 6, ldapConfigPropItems.size());
						
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
