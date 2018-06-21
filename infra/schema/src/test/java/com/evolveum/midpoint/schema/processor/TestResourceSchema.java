/*
 * Copyright (c) 2010-2017 Evolveum
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestResourceSchema {

    private static final String TEST_DIR = "src/test/resources/processor/";

    private static final String RESOURCE_SCHEMA_SIMPLE_FILENAME = TEST_DIR + "resource-schema-simple.xsd";
    private static final String RESOURCE_SCHEMA_SIMPLE_DEPRECATED_FILENAME = TEST_DIR + "resource-schema-simple-deprecated.xsd";

    private static final String SCHEMA_NAMESPACE = "http://schema.foo.com/bar";

    public TestResourceSchema() {
    }

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testParseSchema() throws Exception {
        System.out.println("===[ testParseSchema ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // WHEN

        ResourceSchema schema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(schemaDom),
        		RESOURCE_SCHEMA_SIMPLE_FILENAME, PrismTestUtil.getPrismContext());

        // THEN
        assertSimpleSchema(schema, RESOURCE_SCHEMA_SIMPLE_FILENAME);
    }

    @Test
    public void testParseSchemaDeprecated() throws Exception {
        System.out.println("===[ testParseSchemaDeprecated ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_DEPRECATED_FILENAME);

        // WHEN

        ResourceSchema schema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(schemaDom),
        		RESOURCE_SCHEMA_SIMPLE_DEPRECATED_FILENAME, PrismTestUtil.getPrismContext());

        // THEN
        assertSimpleSchema(schema, RESOURCE_SCHEMA_SIMPLE_DEPRECATED_FILENAME);
    }

    private void assertSimpleSchema(ResourceSchema schema, String filename) {
    	assertNotNull(schema);
        System.out.println("Parsed schema from " + filename + ":");
        System.out.println(schema.debugDump());

        ObjectClassComplexTypeDefinition accDef = schema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertEquals("Wrong account objectclass", new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
        assertEquals("Wrong account kind", ShadowKindType.ACCOUNT, accDef.getKind());
        assertEquals("Wrong account intent", "admin", accDef.getIntent());
        assertTrue("Not a default account", accDef.isDefaultInAKind());

        PrismPropertyDefinition<String> loginAttrDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "login"), loginAttrDef.getName());
        assertEquals(DOMUtil.XSD_STRING, loginAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", loginAttrDef.isIgnored());

        PrismPropertyDefinition<Integer> groupAttrDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "group"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "group"), groupAttrDef.getName());
        assertEquals(DOMUtil.XSD_INT, groupAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", groupAttrDef.isIgnored());

        PrismPropertyDefinition<String> ufoAttrDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "ufo"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "ufo"), ufoAttrDef.getName());
        assertTrue("Not ignored as it should be", ufoAttrDef.isIgnored());

        ObjectClassComplexTypeDefinition groupDef = schema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE, "GroupObjectClass"));
        assertEquals("Wrong group objectclass", new QName(SCHEMA_NAMESPACE, "GroupObjectClass"), groupDef.getTypeName());
        assertEquals("Wrong group kind", ShadowKindType.ENTITLEMENT, groupDef.getKind());
        assertEquals("Wrong group intent", null, groupDef.getIntent());
        assertFalse("Default group but it should not be", groupDef.isDefaultInAKind());
    }

    // The support for the xsd:any properties is missing in JAXB generator. Otherwise this test should work.
	@Test(enabled=false)
	public void testResourceSchemaJaxbRoundTrip() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaJaxbRoundTrip ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();

		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.debugDump());
		System.out.println();

		Document xsd = schema.serializeToXsd();

		ResourceType resource = new ResourceType();
		resource.setName(PrismTestUtil.createPolyStringType("JAXB With Dynamic Schemas Test"));
		ResourceTypeUtil.setResourceXsdSchema(resource, DOMUtil.getFirstChildElement(xsd));

		// WHEN

//		JAXBElement<ResourceType> resourceElement = new JAXBElement<ResourceType>(SchemaConstants.C_RESOURCE, ResourceType.class, resource);
//		String marshalledResource = PrismTestUtil.marshalElementToString(resourceElement);
        String marshalledResource = PrismTestUtil.serializeObjectToString(resource.asPrismObject());

        System.out.println("Marshalled resource");
		System.out.println(marshalledResource);

		ResourceType unmarshalledResource = (ResourceType) PrismTestUtil.parseObject(marshalledResource).asObjectable();

		System.out.println("unmarshalled resource");
		System.out.println(ObjectTypeUtil.dump(unmarshalledResource));
		XmlSchemaType unXmlSchemaType = unmarshalledResource.getSchema();
		Element unXsd = unXmlSchemaType.getDefinition().getAny().get(0);
		ResourceSchema unSchema = ResourceSchemaImpl.parse(unXsd, "unmarshalled resource", PrismTestUtil.getPrismContext());

		System.out.println("unmarshalled schema");
		System.out.println(unSchema.debugDump());

		// THEN
		assertResourceSchema(unSchema);
	}

	@Test
	public void testResourceSchemaPrismRoundTrip() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaPrismRoundTrip ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();

		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.debugDump());
		System.out.println();

		PrismContext prismContext = PrismTestUtil.getPrismContext();

		Document xsd = schema.serializeToXsd();

		PrismObjectDefinition<ResourceType> resourceDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
		PrismObject<ResourceType> resource = resourceDefinition.instantiate();
		ResourceType resourceType = resource.asObjectable();
		resourceType.setName(PrismTestUtil.createPolyStringType("Prism With Dynamic Schemas Test"));
		ResourceTypeUtil.setResourceXsdSchema(resource, DOMUtil.getFirstChildElement(xsd));

		// WHEN

		String marshalledResource = prismContext.serializeObjectToString(resource, PrismContext.LANG_XML);

		System.out.println("Marshalled resource");
		System.out.println(marshalledResource);

		PrismObject<ResourceType> unmarshalledResource = PrismTestUtil.parseObject(marshalledResource);

		System.out.println("unmarshalled resource");
		System.out.println(unmarshalledResource.debugDump());

		Element unXsd = ResourceTypeUtil.getResourceXsdSchema(unmarshalledResource);

		System.out.println("unmarshalled resource schema");
		System.out.println(DOMUtil.serializeDOMToString(unXsd));

		ResourceSchema unSchema = ResourceSchemaImpl.parse(unXsd, "unmarshalled resource schema", PrismTestUtil.getPrismContext());

		System.out.println("unmarshalled parsed schema");
		System.out.println(unSchema.debugDump());

		// THEN
		assertResourceSchema(unSchema);
	}

	private void assertResourceSchema(ResourceSchema unSchema) {
		ObjectClassComplexTypeDefinition objectClassDef = unSchema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE,"AccountObjectClass"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"AccountObjectClass"),objectClassDef.getTypeName());
		assertEquals("AccountObjectClass class not an account", ShadowKindType.ACCOUNT, objectClassDef.getKind());
		assertTrue("AccountObjectClass class not a DEFAULT account", objectClassDef.isDefaultInAKind());

		PrismPropertyDefinition<String> loginDef = objectClassDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"login"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());

		PrismPropertyDefinition<ProtectedStringType> passwdDef = objectClassDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"password"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"password"), passwdDef.getName());
		assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());

//		PrismContainerDefinition<CredentialsType> credDef = objectClassDef.findContainerDefinition(new QName(SchemaConstants.NS_C,"credentials"));
//		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
//		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());
	}

	@Test
	public void testResourceSchemaSerializationDom() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaSerializationDom ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();

		// WHEN
		Document xsdDocument = schema.serializeToXsd();
		Element xsdElement = DOMUtil.getFirstChildElement(xsdDocument);

		System.out.println("Serialized XSD schema");
		System.out.println(DOMUtil.serializeDOMToString(xsdElement));

		assertDomSchema(xsdElement);
	}

	@Test
	public void testResourceSchemaSerializationInResource() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaSerializationInResource ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();

		// WHEN
		Document xsdDocument = schema.serializeToXsd();
		Element xsdElement = DOMUtil.getFirstChildElement(xsdDocument);

		PrismObject<ResourceType> resource = wrapInResource(xsdElement);
		String resourceXmlString = PrismTestUtil.getPrismContext().serializeObjectToString(resource, PrismContext.LANG_XML);

		System.out.println("Serialized resource");
		System.out.println(resourceXmlString);

		PrismObject<ResourceType> reparsedResource = PrismTestUtil.getPrismContext().parseObject(resourceXmlString);

		System.out.println("Re-parsed resource");
		System.out.println(reparsedResource.debugDump());

		XmlSchemaType reparsedSchemaType = reparsedResource.asObjectable().getSchema();
		Element reparsedXsdElement = ObjectTypeUtil.findXsdElement(reparsedSchemaType);

		System.out.println("Reparsed XSD schema");
		System.out.println(DOMUtil.serializeDOMToString(reparsedXsdElement));

		assertDomSchema(reparsedXsdElement);
	}


	private PrismObject<ResourceType> wrapInResource(Element xsdElement) throws SchemaException {
		PrismObjectDefinition<ResourceType> resourceDefinition =
			PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
		PrismObject<ResourceType> resource = resourceDefinition.instantiate();
		ResourceTypeUtil.setResourceXsdSchema(resource, xsdElement);
		return resource;
	}

	private void assertDomSchema(Element xsdElement) {
		assertPrefix("xsd", xsdElement);
		Element displayNameAnnotationElement = DOMUtil.findElementRecursive(xsdElement, PrismConstants.A_DISPLAY_NAME);
		assertPrefix(PrismConstants.PREFIX_NS_ANNOTATION, displayNameAnnotationElement);
		Element kindAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_KIND);
		assertPrefix("ra", kindAnnotationElement);
		assertEquals(ShadowKindType.ACCOUNT.value(), kindAnnotationElement.getTextContent());
		Element identifierAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_IDENTIFIER);
		assertPrefix("ra", identifierAnnotationElement);
		QName identifier = DOMUtil.getQNameValue(identifierAnnotationElement);
		assertEquals("Wrong <a:identifier> value namespace", SchemaTestConstants.ICFS_UID.getNamespaceURI(), identifier.getNamespaceURI());
		assertEquals("Wrong <a:identifier> value localname", SchemaTestConstants.ICFS_UID.getLocalPart(), identifier.getLocalPart());
		assertEquals("Wrong <a:identifier> value prefix", "icfs", identifier.getPrefix());
		Element dnaAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE);
		assertPrefix("ra", dnaAnnotationElement);
		QName dna = DOMUtil.getQNameValue(dnaAnnotationElement);
		assertEquals("Wrong <a:identifier> value prefix", "tns", dna.getPrefix());

		assertEquals("Wrong 'tns' prefix declaration", SCHEMA_NAMESPACE, xsdElement.lookupNamespaceURI("tns"));
	}

	private ResourceSchema createResourceSchema() {
		ResourceSchemaImpl schema = new ResourceSchemaImpl(SCHEMA_NAMESPACE, PrismTestUtil.getPrismContext());

		// Property container
		ObjectClassComplexTypeDefinitionImpl containerDefinition = (ObjectClassComplexTypeDefinitionImpl) schema.createObjectClassDefinition("AccountObjectClass");
		containerDefinition.setKind(ShadowKindType.ACCOUNT);
		containerDefinition.setDefaultInAKind(true);
		containerDefinition.setDisplayName("The Account");
		containerDefinition.setNativeObjectClass("ACCOUNT");
		// ... in it ordinary attribute - an identifier
		ResourceAttributeDefinition<String> icfUidDef = containerDefinition.createAttributeDefinition(
				SchemaTestConstants.ICFS_UID, DOMUtil.XSD_STRING);
		((Collection)containerDefinition.getPrimaryIdentifiers()).add(icfUidDef);
		ResourceAttributeDefinitionImpl<String> xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
		xloginDef.setNativeAttributeName("LOGIN");
		containerDefinition.setDisplayNameAttribute(xloginDef.getName());
		// ... and local property with a type from another schema
		ResourceAttributeDefinitionImpl<String> xpasswdDef = containerDefinition.createAttributeDefinition("password", ProtectedStringType.COMPLEX_TYPE);
		xpasswdDef.setNativeAttributeName("PASSWORD");
		// ... property reference
		// TODO this should not go here, as it is not a ResourceAttributeDefinition
		//containerDefinition.createAttributeDefinition(SchemaConstants.C_CREDENTIALS, SchemaConstants.C_CREDENTIALS_TYPE);

		return schema;
	}

	private void assertPrefix(String expectedPrefix, Element element) {
		assertEquals("Wrong prefix on element "+DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
	}

	@Test
	public void testParseResource() throws Exception {
		System.out.println("===[ testParseResource ]===");
		// WHEN
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File("src/test/resources/common/xml/ns/resource-opendj.xml"));

		// THEN
		assertCapabilities(resource.asObjectable());
	}

	@Test
	public void testUnmarshallResource() throws Exception {
		System.out.println("===[ testUnmarshallResource ]===");
		// WHEN
		ResourceType resourceType = (ResourceType) PrismTestUtil.parseObject(new File("src/test/resources/common/xml/ns/resource-opendj.xml")).asObjectable();

		// THEN
		assertCapabilities(resourceType);
	}


	private void assertCapabilities(ResourceType resourceType) throws SchemaException {
		if (resourceType.getCapabilities() != null) {
			if (resourceType.getCapabilities().getNative() != null) {
				for (Object capability : resourceType.getCapabilities().getNative().getAny()) {
		        	System.out.println("Native Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
		        }
			}

	        if (resourceType.getCapabilities().getConfigured() != null) {
		        for (Object capability : resourceType.getCapabilities().getConfigured().getAny()) {
		        	System.out.println("Configured Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
		        }
	        }
		}

        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Efective Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }

        assertNotNull("null native capabilities", resourceType.getCapabilities().getNative());
        assertFalse("empty native capabilities", resourceType.getCapabilities().getNative().getAny().isEmpty());
        assertEquals("Unexepected number of native capabilities", 3, resourceType.getCapabilities().getNative().getAny().size());

        assertNotNull("null configured capabilities", resourceType.getCapabilities().getConfigured());
        assertFalse("empty configured capabilities", resourceType.getCapabilities().getConfigured().getAny().isEmpty());
        assertEquals("Unexepected number of configured capabilities", 2, resourceType.getCapabilities().getConfigured().getAny().size());

        assertEquals("Unexepected number of effective capabilities", 3,effectiveCapabilities.size());
        assertNotNull("No credentials effective capability",
        		ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class));
        assertNotNull("No activation effective capability",
        		ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class));
        assertNull("Unexpected liveSync effective capability",
        		ResourceTypeUtil.getEffectiveCapability(resourceType, LiveSyncCapabilityType.class));

	}


}
