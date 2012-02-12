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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.Schema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * @author semancik
 *
 */
public class TestJaxbWithDynamicSchema {
	
	private static final String SCHEMA_NS = "http://foo.com/xml/ns/schema";

	@Test
	public void testJaxbRoundTripWithDynamicSchema() throws SchemaException, JAXBException {
		System.out.println("\n===[ testJaxbRoundTripWithDynamicSchema ]=====");
		// GIVEN
		Schema schema = new Schema(SCHEMA_NS);
		
		// Property container
		ResourceObjectDefinition containerDefinition = schema.createResourceObjectDefinition("AccountObjectClass");
		containerDefinition.setAccountType(true);
		containerDefinition.setDefaultAccountType(true);
		containerDefinition.setNativeObjectClass("ACCOUNT");
		// ... in it ordinary attribute - an identifier
		ResourceObjectAttributeDefinition xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
		containerDefinition.getIdentifiers().add(xloginDef);
		xloginDef.setNativeAttributeName("LOGIN");
		containerDefinition.setDisplayNameAttribute(xloginDef.getName());
		// ... and local property with a type from another schema
		ResourceObjectAttributeDefinition xpasswdDef = containerDefinition.createAttributeDefinition("password", SchemaConstants.R_PROTECTED_STRING_TYPE);
		xpasswdDef.setNativeAttributeName("PASSWORD");
		// ... property reference
		containerDefinition.createAttributeDefinition(SchemaConstants.I_CREDENTIALS, SchemaConstants.I_CREDENTIALS_TYPE);

		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.dump());
		System.out.println();

		Document xsd = schema.serializeToXsd();

		ResourceType resource = new ResourceType();
		resource.setName("JAXB With Dynamic Schemas Test");
		XmlSchemaType xmlSchemaType = new XmlSchemaType();
		xmlSchemaType.getAny().add(DOMUtil.getFirstChildElement(xsd));
		resource.setSchema(xmlSchemaType);
		
		// WHEN
		
		JAXBElement<ResourceType> resourceElement = new JAXBElement<ResourceType>(SchemaConstants.I_RESOURCE, ResourceType.class, resource);
		String marshalledResource = JAXBUtil.marshal(resourceElement);
		
		System.out.println("Marshalled resource");
		System.out.println(marshalledResource);
		
		JAXBElement<ResourceType> unmarshalledResourceElement = (JAXBElement<ResourceType>) JAXBUtil.unmarshal(marshalledResource);
		
		ResourceType unmarshalledResource = unmarshalledResourceElement.getValue();
		
		System.out.println("unmarshalled resource");
		System.out.println(ObjectTypeUtil.dump(unmarshalledResource));
		XmlSchemaType unXmlSchemaType = unmarshalledResource.getSchema();
		Element unXsd = unXmlSchemaType.getAny().get(0);
		Schema unSchema = Schema.parse(unXsd);
		
		System.out.println("unmarshalled schema");
		System.out.println(unSchema.dump());
		
		// THEN
		
		PrismContainerDefinition newContainerDef = unSchema.findContainerDefinitionByType(new QName(SCHEMA_NS,"AccountObjectClass"));
		assertEquals(new QName(SCHEMA_NS,"AccountObjectClass"),newContainerDef.getTypeName());
		assertTrue(newContainerDef instanceof ResourceObjectDefinition);
		ResourceObjectDefinition rod = (ResourceObjectDefinition) newContainerDef;
		assertTrue(rod.isAccountType());
		assertTrue(rod.isDefaultAccountType());
		
		PrismPropertyDefinition loginDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"login"));
		assertEquals(new QName(SCHEMA_NS,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());

		PrismPropertyDefinition passwdDef = newContainerDef.findPropertyDefinition(new QName(SCHEMA_NS,"password"));
		assertEquals(new QName(SCHEMA_NS,"password"), passwdDef.getName());
		assertEquals(SchemaConstants.R_PROTECTED_STRING_TYPE, passwdDef.getTypeName());

		PrismContainerDefinition credDef = newContainerDef.findContainerDefinition(new QName(SchemaConstants.NS_C,"credentials"));
		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());
	}
	
	@Test
	public void testUnmarshallResource() throws JAXBException {
		// WHEN
		Object element = JAXBUtil.unmarshal(new File("src/test/resources/schema/resource-opendj.xml"));
		
		// THEN
		assertTrue(element instanceof JAXBElement);
		Object object = ((JAXBElement)element).getValue();
		assertTrue(object instanceof ResourceType);
		ResourceType resource = (ResourceType)object;
		
		if (resource.getNativeCapabilities() != null) {
			for (Object capability : resource.getNativeCapabilities().getAny()) {
	        	System.out.println("Native Capability: "+ResourceTypeUtil.getCapabilityDisplayName(capability)+" : "+capability);
	        }
		}

        if (resource.getCapabilities() != null) {
	        for (Object capability : resource.getCapabilities().getAny()) {
	        	System.out.println("Configured Capability: "+ResourceTypeUtil.getCapabilityDisplayName(capability)+" : "+capability);
	        }
        }
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Efective Capability: "+ResourceTypeUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }

        assertNotNull(resource.getCapabilities());
        assertFalse(resource.getCapabilities().getAny().isEmpty());
        
	}
	
}
