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

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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
    private static final File RESOURCE_FILE = new File("src/test/resources/common/xml/ns/resource-opendj.xml");

    @Test
    public void testParseSchema() throws Exception {
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // WHEN

        var schema = ResourceSchemaFactory.parseNativeSchemaAsBare(schemaDom);

        // THEN
        assertSimpleSchema(schema, RESOURCE_SCHEMA_SIMPLE_FILENAME);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertSimpleSchema(ResourceSchema schema, String filename) throws SchemaException {
        assertNotNull(schema);
        System.out.println("Parsed schema from " + filename + ":");
        System.out.println(schema.debugDump());

        ResourceObjectClassDefinition accDef = schema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        assertEquals("Wrong account objectclass", RI_ACCOUNT_OBJECT_CLASS, accDef.getTypeName());
        assertTrue("Not a default account", accDef.isDefaultAccountDefinition());

        PrismPropertyDefinition<String> loginAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "login"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "login"), loginAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_STRING, loginAttrDef.getTypeName());
        assertTrue("Unreadable while it should not be", loginAttrDef.canRead());

        PrismPropertyDefinition<Integer> groupAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "group"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "group"), groupAttrDef.getItemName());
        assertEquals(DOMUtil.XSD_INT, groupAttrDef.getTypeName());
        assertTrue("Unreadable while it should not be", groupAttrDef.canRead());

        PrismPropertyDefinition<String> ufoAttrDef = accDef.findPropertyDefinition(new ItemName(MidPointConstants.NS_RI, "ufo"));
        assertEquals(new ItemName(MidPointConstants.NS_RI, "ufo"), ufoAttrDef.getItemName());
        assertFalse("Not unreadable as it should be", ufoAttrDef.canRead());

        ResourceObjectClassDefinition groupDef = schema.findObjectClassDefinitionRequired(RI_GROUP_OBJECT_CLASS);
        assertEquals("Wrong group objectclass", new ItemName(MidPointConstants.NS_RI, "GroupObjectClass"), groupDef.getTypeName());
        assertFalse("Default group but it should not be", groupDef.isDefaultAccountDefinition());
    }

    // The support for the xsd:any properties is missing in JAXB generator. Otherwise this test should work.
    @Test(enabled = false)
    public void testResourceSchemaJaxbRoundTrip() throws SchemaException, ConfigurationException {
        // GIVEN
        NativeResourceSchema schema = createResourceSchema();

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
        var unSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(unXsd);

        System.out.println("unmarshalled schema");
        System.out.println(unSchema.debugDump());

        // THEN
        assertResourceSchema(unSchema);
    }

    @Test
    public void testResourceSchemaPrismRoundTrip() throws SchemaException, ConfigurationException {
        // GIVEN
        NativeResourceSchema schema = createResourceSchema();

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

        Element unXsd = Objects.requireNonNull(ResourceTypeUtil.getResourceXsdSchemaElement(unmarshalledResource));

        System.out.println("unmarshalled resource schema");
        System.out.println(DOMUtil.serializeDOMToString(unXsd));

        var unSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(unXsd);

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
        NativeResourceSchema schema = createResourceSchema();

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
        NativeResourceSchema schema = createResourceSchema();

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
        Element identifierAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_IDENTIFIER);
        assertPrefix("ra", identifierAnnotationElement);
        QName identifier = DOMUtil.getQNameValue(identifierAnnotationElement);
        assertEquals("Wrong <a:identifier> value namespace", SchemaConstants.ICFS_UID.getNamespaceURI(), identifier.getNamespaceURI());
        assertEquals("Wrong <a:identifier> value local name", SchemaConstants.ICFS_UID.getLocalPart(), identifier.getLocalPart());
        assertEquals("Wrong <a:identifier> value prefix", "icfs", identifier.getPrefix());
        Element dnaAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE);
        assertPrefix("ra", dnaAnnotationElement);
        QName dna = DOMUtil.getQNameValue(dnaAnnotationElement);
        assertEquals("Wrong <a:identifier> value prefix", "tns", dna.getPrefix());

        assertEquals("Wrong 'tns' prefix declaration", MidPointConstants.NS_RI, xsdElement.lookupNamespaceURI("tns"));
    }

    private NativeResourceSchema createResourceSchema() {
        var schema = new NativeResourceSchemaImpl();

        // Object class definition
        var accountDef = schema.newComplexTypeDefinitionLikeBuilder(ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
        accountDef.setDefaultAccountDefinition(true);
        accountDef.setNativeObjectClassName("ACCOUNT");

        // ... in it ordinary attribute - an identifier
        var uidAttrDef = accountDef.newPropertyLikeDefinition(SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING);
        accountDef.add(uidAttrDef);
        accountDef.setPrimaryIdentifierName(SchemaConstants.ICFS_UID);

        var loginAttrName = new QName(NS_RI, "login");
        var loginAttrDef = accountDef.newPropertyLikeDefinition(loginAttrName, DOMUtil.XSD_STRING);
        loginAttrDef.setNativeAttributeName("LOGIN");
        accountDef.add(loginAttrDef);
        accountDef.setDisplayNameAttributeName(loginAttrName);

        // ... and local property with a type from another schema
        var passwordAttrName = new QName(NS_RI, "password");
        var passwordAttrDef = accountDef.newPropertyLikeDefinition(passwordAttrName, ProtectedStringType.COMPLEX_TYPE);
        passwordAttrDef.setNativeAttributeName("PASSWORD");
        accountDef.add(passwordAttrDef);

        schema.add(accountDef);

        return schema;
    }

    private void assertPrefix(String expectedPrefix, Element element) {
        assertEquals("Wrong prefix on element " + DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
    }

    @Test
    public void testParseResource() throws Exception {
        // WHEN
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_FILE);

        // THEN
        assertCapabilities(resource.asObjectable());
    }

    /** A delineation is created as part of the resource definition. It is extracted, cloned, and parsed. */
    @Test
    public void testParseExtractedDelineation() throws Exception {
        given("resource with delineation");
        var resource = (ResourceType) PrismTestUtil.parseObject(RESOURCE_FILE).asObjectable();
        var delineationBean = resource.getSchemaHandling().getObjectType().get(0).getDelineation();
        assertThat(delineationBean).isNotNull();
        var typeDefinition = ResourceSchemaFactory
                .getCompleteSchemaRequired(resource)
                .getObjectTypeDefinitionRequired(ACCOUNT_DEFAULT);

        when("delineation bean is cloned and parsed");
        var clone = delineationBean.clone();
        var parsed = ResourceObjectTypeDelineation.of(clone, typeDefinition.getObjectClassName(), List.of(), typeDefinition);

        then("it is parsed correctly");
        displayValue("re-parsed delineation", parsed.debugDump());
        assertThat(parsed.getFilterClauses()).hasSize(1);
    }

    /** A filter is parsed, serialized, and re-parsed. */
    @Test(enabled = false) // MID-10837
    public void testSerializeAndParseFilter() throws Exception {
        given("resource");
        var resource = (ResourceType) PrismTestUtil.parseObject(RESOURCE_FILE).asObjectable();
        var shadowDefinition = ResourceSchemaFactory
                .getCompleteSchemaRequired(resource)
                .getObjectTypeDefinitionRequired(ACCOUNT_DEFAULT)
                .getPrismObjectDefinition();

        when("a filter is parsed, serialized, and re-parsed");
        var parsedFilter = PrismContext.get().createQueryParser().parseFilter(
                shadowDefinition, "attributes/ri:description = 'abc'");
        var serializedFilter = PrismContext.get().querySerializer().serialize(parsedFilter).toSearchFilterType();
        var reparsedFilter = PrismContext.get().getQueryConverter().createObjectFilter(shadowDefinition, serializedFilter);

        then("it is parsed correctly");
        displayValue("re-parsed filter", reparsedFilter.debugDump());
    }

    @Test
    public void testUnmarshallResource() throws Exception {
        // WHEN
        ResourceType resourceType = (ResourceType) PrismTestUtil.parseObject(RESOURCE_FILE).asObjectable();

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
