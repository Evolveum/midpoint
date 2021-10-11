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

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestResourceSchema extends AbstractSchemaTest {

    private static final String TEST_DIR = "src/test/resources/processor/";

    private static final String RESOURCE_SCHEMA_SIMPLE_FILENAME = TEST_DIR + "resource-schema-simple.xsd";

    private static final String SCHEMA_NAMESPACE = "http://schema.foo.com/bar";

    @Test
    public void testParseSchema() throws Exception {
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // WHEN

        ResourceSchema schema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(schemaDom),
                "http://schema.foo.com/bar", RESOURCE_SCHEMA_SIMPLE_FILENAME, PrismTestUtil.getPrismContext());

        // THEN
        assertSimpleSchema(schema, RESOURCE_SCHEMA_SIMPLE_FILENAME);
    }

    private void assertSimpleSchema(ResourceSchema schema, String filename) {
        assertNotNull(schema);
        System.out.println("Parsed schema from " + filename + ":");
        System.out.println(schema.debugDump());

        ObjectClassComplexTypeDefinition accDef = schema.findObjectClassDefinition(new ItemName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertEquals("Wrong account objectclass", new ItemName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
        assertEquals("Wrong account kind", ShadowKindType.ACCOUNT, accDef.getKind());
        assertEquals("Wrong account intent", "admin", accDef.getIntent());
        assertTrue("Not a default account", accDef.isDefaultInAKind());

        PrismPropertyDefinition<String> loginAttrDef = accDef.findPropertyDefinition(new ItemName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "login"), loginAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", loginAttrDef.isIgnored());

        PrismPropertyDefinition<Integer> groupAttrDef = accDef.findPropertyDefinition(new ItemName(SCHEMA_NAMESPACE, "group"));
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "group"), groupAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_INT, groupAttrDef.getTypeName());
        assertFalse("Ignored while it should not be", groupAttrDef.isIgnored());

        PrismPropertyDefinition<String> ufoAttrDef = accDef.findPropertyDefinition(new ItemName(SCHEMA_NAMESPACE, "ufo"));
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "ufo"), ufoAttrDef.getItemName());
        assertTrue("Not ignored as it should be", ufoAttrDef.isIgnored());

        ObjectClassComplexTypeDefinition groupDef = schema.findObjectClassDefinition(new ItemName(SCHEMA_NAMESPACE, "GroupObjectClass"));
        assertEquals("Wrong group objectclass", new ItemName(SCHEMA_NAMESPACE, "GroupObjectClass"), groupDef.getTypeName());
        assertEquals("Wrong group kind", ShadowKindType.ENTITLEMENT, groupDef.getKind());
        assertEquals("Wrong group intent", null, groupDef.getIntent());
        assertFalse("Default group but it should not be", groupDef.isDefaultInAKind());
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
        ResourceSchema unSchema = ResourceSchemaImpl.parse(unXsd, "unmarshalled resource", PrismTestUtil.getPrismContext());

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
        resourceType.setNamespace(SCHEMA_NAMESPACE);
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

        ResourceSchema unSchema = ResourceSchemaImpl.parse(unXsd, "unmarshalled resource schema", PrismTestUtil.getPrismContext());

        System.out.println("unmarshalled parsed schema");
        System.out.println(unSchema.debugDump());

        // THEN
        assertResourceSchema(unSchema);
    }

    private void assertResourceSchema(ResourceSchema unSchema) {
        ObjectClassComplexTypeDefinition objectClassDef = unSchema.findObjectClassDefinition(new ItemName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertNotNull("No object class def", objectClassDef);
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "AccountObjectClass"), objectClassDef.getTypeName());
        assertEquals("AccountObjectClass class not an account", ShadowKindType.ACCOUNT, objectClassDef.getKind());
        assertTrue("AccountObjectClass class not a DEFAULT account", objectClassDef.isDefaultInAKind());

        PrismPropertyDefinition<String> loginDef = objectClassDef.findPropertyDefinition(new ItemName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "login"), loginDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());

        PrismPropertyDefinition<ProtectedStringType> passwdDef = objectClassDef.findPropertyDefinition(new ItemName(SCHEMA_NAMESPACE, "password"));
        assertEquals(new ItemName(SCHEMA_NAMESPACE, "password"), passwdDef.getItemName());
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
        ((Collection) containerDefinition.getPrimaryIdentifiers()).add(icfUidDef);
        ResourceAttributeDefinitionImpl<String> xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
        xloginDef.setNativeAttributeName("LOGIN");
        containerDefinition.setDisplayNameAttribute(xloginDef.getItemName());
        // ... and local property with a type from another schema
        ResourceAttributeDefinitionImpl<String> xpasswdDef = containerDefinition.createAttributeDefinition("password", ProtectedStringType.COMPLEX_TYPE);
        xpasswdDef.setNativeAttributeName("PASSWORD");
        // ... property reference
        // TODO this should not go here, as it is not a ResourceAttributeDefinition
        //containerDefinition.createAttributeDefinition(SchemaConstants.C_CREDENTIALS, SchemaConstants.C_CREDENTIALS_TYPE);

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

    private void assertCapabilities(ResourceType resourceType) throws SchemaException {
        if (resourceType.getCapabilities() != null) {
            if (resourceType.getCapabilities().getNative() != null) {
                for (Object capability : resourceType.getCapabilities().getNative().getAny()) {
                    System.out.println("Native Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
                }
            }

            if (resourceType.getCapabilities().getConfigured() != null) {
                for (Object capability : resourceType.getCapabilities().getConfigured().getAny()) {
                    System.out.println("Configured Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
                }
            }
        }

        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
        for (Object capability : effectiveCapabilities) {
            System.out.println("Efective Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        assertNotNull("null native capabilities", resourceType.getCapabilities().getNative());
        assertFalse("empty native capabilities", resourceType.getCapabilities().getNative().getAny().isEmpty());
        assertEquals("Unexpected number of native capabilities", 3, resourceType.getCapabilities().getNative().getAny().size());

        assertNotNull("null configured capabilities", resourceType.getCapabilities().getConfigured());
        assertFalse("empty configured capabilities", resourceType.getCapabilities().getConfigured().getAny().isEmpty());
        assertEquals("Unexpected number of configured capabilities", 2, resourceType.getCapabilities().getConfigured().getAny().size());

        assertEquals("Unexpected number of effective capabilities", 3, effectiveCapabilities.size());
        assertNotNull("No credentials effective capability",
                ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class));
        assertNotNull("No activation effective capability",
                ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class));
        assertNull("Unexpected liveSync effective capability",
                ResourceTypeUtil.getEffectiveCapability(resourceType, LiveSyncCapabilityType.class));

    }

}
