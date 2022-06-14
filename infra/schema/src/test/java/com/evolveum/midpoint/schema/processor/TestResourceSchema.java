/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestResourceSchema extends AbstractSchemaTest {

    private static final String TEST_DIR = "src/test/resources/processor/";

    private static final String RESOURCE_SCHEMA_SIMPLE_FILENAME = TEST_DIR + "resource-schema-simple.xsd";

    @Test
    public void testParseSchema() throws Exception {
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // WHEN

        ResourceSchema schema = ResourceSchemaParser.parse(DOMUtil.getFirstChildElement(schemaDom), RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // THEN
        assertSimpleSchema(schema, RESOURCE_SCHEMA_SIMPLE_FILENAME);
    }

    private void assertSimpleSchema(ResourceSchema schema, String filename) {
        assertNotNull(schema);
        System.out.println("Parsed schema from " + filename + ":");
        System.out.println(schema.debugDump());

        ResourceObjectClassDefinition accDef = schema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        assertEquals("Wrong account objectclass", RI_ACCOUNT_OBJECT_CLASS, accDef.getTypeName());
        assertTrue("Not a default account", accDef.isDefaultAccountDefinition());

        PrismPropertyDefinition<String> loginAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "login"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "login"), loginAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", loginAttrDef.isIgnored());

        PrismPropertyDefinition<Integer> groupAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "group"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "group"), groupAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_INT, groupAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", groupAttrDef.isIgnored());

        PrismPropertyDefinition<String> ufoAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "ufo"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "ufo"), ufoAttrDef.getItemName());
        assertTrue("Not ignored as it should be", ufoAttrDef.isIgnored());

        ResourceObjectClassDefinition groupDef = schema.findObjectClassDefinition(new ItemName(MidPointConstants.NS_RI, "GroupObjectClass"));
        assertEquals("Wrong group objectclass", new ItemName(MidPointConstants.NS_RI, "GroupObjectClass"), groupDef.getTypeName());
        assertFalse("Default group but it should not be", groupDef.isDefaultAccountDefinition());
    }

    // The support for the xsd:any properties is missing in JAXB generator. Otherwise this test should work.
    @Test(enabled = false)
    public void testResourceSchemaJaxbRoundTrip() throws SchemaException {
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

        String marshalledResource = PrismTestUtil.serializeObjectToString(resource.asPrismObject());

        System.out.println("Marshalled resource");
        System.out.println(marshalledResource);

        ResourceType unmarshalledResource = (ResourceType) PrismTestUtil.parseObject(marshalledResource).asObjectable();

        System.out.println("unmarshalled resource");
        System.out.println(ObjectTypeUtil.dump(unmarshalledResource));
        XmlSchemaType unXmlSchemaType = unmarshalledResource.getSchema();
        Element unXsd = unXmlSchemaType.getDefinition().getAny().get(0);
        ResourceSchema unSchema = ResourceSchemaParser.parse(unXsd, "unmarshalled resource");

        System.out.println("unmarshalled schema");
        System.out.println(unSchema.debugDump());

        // THEN
        assertResourceSchema(unSchema);
    }

    @Test
    public void testResourceSchemaPrismRoundTrip() throws SchemaException {
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

        String marshalledResource = prismContext.xmlSerializer().serialize(resource);

        System.out.println("Marshalled resource");
        System.out.println(marshalledResource);

        PrismObject<ResourceType> unmarshalledResource = PrismTestUtil.parseObject(marshalledResource);

        System.out.println("unmarshalled resource");
        System.out.println(unmarshalledResource.debugDump());

        Element unXsd = ResourceTypeUtil.getResourceXsdSchema(unmarshalledResource);

        System.out.println("unmarshalled resource schema");
        System.out.println(DOMUtil.serializeDOMToString(unXsd));

        ResourceSchema unSchema = ResourceSchemaParser.parse(unXsd, "unmarshalled resource schema");

        System.out.println("unmarshalled parsed schema");
        System.out.println(unSchema.debugDump());

        // THEN
        assertResourceSchema(unSchema);
    }

    private void assertResourceSchema(ResourceSchema unSchema) {
        ResourceObjectClassDefinition objectClassDef = unSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("No object class def", objectClassDef);
        assertEquals(RI_ACCOUNT_OBJECT_CLASS, objectClassDef.getTypeName());
        assertTrue("AccountObjectClass class not a DEFAULT account", objectClassDef.isDefaultAccountDefinition());

        PrismPropertyDefinition<String> loginDef = objectClassDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "login"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "login"), loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());

        PrismPropertyDefinition<ProtectedStringType> passwdDef = objectClassDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "password"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "password"), passwdDef.getItemName());
        assertEquals(ProtectedStringType.COMPLEX_TYPE, passwdDef.getTypeName());
    }

    @Test
    public void testResourceSchemaSerializationDom() throws SchemaException {
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
    public void testResourceSchemaSerializationInResource() throws SchemaException {
        // GIVEN
        ResourceSchema schema = createResourceSchema();

        // WHEN
        Document xsdDocument = schema.serializeToXsd();
        Element xsdElement = DOMUtil.getFirstChildElement(xsdDocument);

        PrismObject<ResourceType> resource = wrapInResource(xsdElement);
        String resourceXmlString = PrismTestUtil.getPrismContext().xmlSerializer().serialize(resource);

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
        Element identifierAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_IDENTIFIER);
        assertPrefix("ra", identifierAnnotationElement);
        QName identifier = DOMUtil.getQNameValue(identifierAnnotationElement);
        assertEquals("Wrong <a:identifier> value namespace", SchemaConstants.ICFS_UID.getNamespaceURI(), identifier.getNamespaceURI());
        assertEquals("Wrong <a:identifier> value localname", SchemaConstants.ICFS_UID.getLocalPart(), identifier.getLocalPart());
        assertEquals("Wrong <a:identifier> value prefix", "icfs", identifier.getPrefix());
        Element dnaAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE);
        assertPrefix("ra", dnaAnnotationElement);
        QName dna = DOMUtil.getQNameValue(dnaAnnotationElement);
        assertEquals("Wrong <a:identifier> value prefix", "tns", dna.getPrefix());

        assertEquals("Wrong 'tns' prefix declaration", MidPointConstants.NS_RI, xsdElement.lookupNamespaceURI("tns"));
    }

    private ResourceSchema createResourceSchema() {
        ResourceSchemaImpl schema = new ResourceSchemaImpl();

        // Property container
        ResourceObjectClassDefinitionImpl containerDefinition = (ResourceObjectClassDefinitionImpl)
                schema.createObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        containerDefinition.setDefaultAccountDefinition(true);
        //containerDefinition.setDisplayName("The Account"); // currently not supported
        containerDefinition.setNativeObjectClass("ACCOUNT");

        // ... in it ordinary attribute - an identifier
        ResourceAttributeDefinition<?> icfUidDef =
                containerDefinition.createAttributeDefinition(
                        SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING, def -> {});
        containerDefinition.addPrimaryIdentifierName(icfUidDef.getItemName());

        ResourceAttributeDefinition<?> xLoginDef =
                containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING,
                        def -> def.setNativeAttributeName("LOGIN"));
        containerDefinition.setDisplayNameAttributeName(xLoginDef.getItemName());

        // ... and local property with a type from another schema
        containerDefinition.createAttributeDefinition("password", ProtectedStringType.COMPLEX_TYPE,
                def -> def.setNativeAttributeName("PASSWORD"));

        return schema;
    }

    private void assertPrefix(String expectedPrefix, Element element) {
        assertEquals("Wrong prefix on element " + DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
    }

    @Test
    public void testParseResource() throws Exception {
        // WHEN
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File("src/test/resources/common/xml/ns/resource-opendj.xml"));

        // THEN
        assertCapabilities(resource.asObjectable());
    }

    @Test
    public void testUnmarshallResource() throws Exception {
        // WHEN
        ResourceType resourceType = (ResourceType) PrismTestUtil.parseObject(new File("src/test/resources/common/xml/ns/resource-opendj.xml")).asObjectable();

        // THEN
        assertCapabilities(resourceType);
    }

    private void assertCapabilities(ResourceType resource) throws SchemaException {
        CapabilitiesType capabilities = resource.getCapabilities();
        assertThat(capabilities).as("capabilities").isNotNull();

        CapabilityCollectionType nativeCapabilities = capabilities.getNative();
        CapabilityCollectionType configuredCapabilities = capabilities.getConfigured();

        for (CapabilityType capability : CapabilityUtil.getAllCapabilities(nativeCapabilities)) {
            System.out.println("Native Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        for (CapabilityType capability : CapabilityUtil.getAllCapabilities(configuredCapabilities)) {
            System.out.println("Configured Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        List<CapabilityType> enabledCapabilities = ResourceTypeUtil.getEnabledCapabilities(resource);
        for (CapabilityType capability : enabledCapabilities) {
            System.out.println("Enabled capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        assertNotNull("null native capabilities", nativeCapabilities);
        assertEquals("Unexpected number of native capabilities", 3, CapabilityUtil.size(nativeCapabilities));

        assertNotNull("null configured capabilities", configuredCapabilities);
        assertEquals("Unexpected number of configured capabilities", 2, CapabilityUtil.size(configuredCapabilities));

        assertEquals("Unexpected number of enabled capabilities", 3, enabledCapabilities.size());
        assertNotNull("No credentials effective capability",
                ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class));
        assertNotNull("No activation effective capability",
                ResourceTypeUtil.getEnabledCapability(resource, ActivationCapabilityType.class));
        assertNull("Unexpected liveSync effective capability",
                ResourceTypeUtil.getEnabledCapability(resource, LiveSyncCapabilityType.class));
    }
}
