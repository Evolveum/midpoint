/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.XmlSchemaType;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestResourceSchema {

    private static final String TEST_DIR = "src/test/resources/processor/";

    private static final String RESOURCE_SCHEMA_SIMPLE_FILENAME = TEST_DIR + "resource-schema-simple.xsd";
    private static final String RESOURCE_OBJECT_SIMPLE_FILENAME = TEST_DIR + "object1.xml";
    
    private static final String SCHEMA_NAMESPACE = "http://schema.foo.com/bar";
    
    private static final QName FIRST_QNAME = new QName(SCHEMA_NAMESPACE, "first");

    public TestResourceSchema() {
    }

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
    
    @Test
    public void parseSchemaTest() throws SchemaException {
        System.out.println("===[ parseSchemaTest ]===");
        // GIVEN

        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);

        // WHEN

        ResourceSchema schema = ResourceSchema.parse(DOMUtil.getFirstChildElement(schemaDom), 
        		RESOURCE_SCHEMA_SIMPLE_FILENAME, PrismTestUtil.getPrismContext());

        // THEN

        assertNotNull(schema);

        System.out.println("Parsed schema from " + RESOURCE_SCHEMA_SIMPLE_FILENAME + ":");
        System.out.println(schema.dump());

        ObjectClassComplexTypeDefinition accDef = schema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
        assertTrue("Not a default account", accDef.isDefaultAccountType());
        
        PrismPropertyDefinition loginDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "login"), loginDef.getName());
        assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());
        assertFalse("Ignored while it should not be", loginDef.isIgnored());
        
        PrismPropertyDefinition groupDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "group"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "group"), groupDef.getName());
        assertEquals(DOMUtil.XSD_INT, groupDef.getTypeName());
        assertFalse("Ignored while it should not be", groupDef.isIgnored());
        
        PrismPropertyDefinition ufoDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "ufo"));
        assertEquals(new QName(SCHEMA_NAMESPACE, "ufo"), ufoDef.getName());
        assertTrue("Not ignored as it should be", ufoDef.isIgnored());
    }
    
    // The support for the xsd:any properties is missing in JAXB generator. Otherwise this test should work.
	@Test(enabled=false)
	public void testResourceSchemaJaxbRoundTrip() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaJaxbRoundTrip ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();
		
		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.dump());
		System.out.println();

		Document xsd = schema.serializeToXsd();

		ResourceType resource = new ResourceType();
		resource.setName(PrismTestUtil.createPolyStringType("JAXB With Dynamic Schemas Test"));
		ResourceTypeUtil.setResourceXsdSchema(resource, DOMUtil.getFirstChildElement(xsd));
		
		// WHEN
		
		JAXBElement<ResourceType> resourceElement = new JAXBElement<ResourceType>(SchemaConstants.I_RESOURCE, ResourceType.class, resource);
		String marshalledResource = PrismTestUtil.marshalElementToString(resourceElement);
		
		System.out.println("Marshalled resource");
		System.out.println(marshalledResource); 
		
		ResourceType unmarshalledResource = PrismTestUtil.unmarshalObject(marshalledResource, ResourceType.class);
		
		System.out.println("unmarshalled resource");
		System.out.println(ObjectTypeUtil.dump(unmarshalledResource));
		XmlSchemaType unXmlSchemaType = unmarshalledResource.getSchema();
		Element unXsd = unXmlSchemaType.getDefinition().getAny().get(0);
		ResourceSchema unSchema = ResourceSchema.parse(unXsd, "unmarshalled resource", PrismTestUtil.getPrismContext());
		
		System.out.println("unmarshalled schema");
		System.out.println(unSchema.dump());
		
		// THEN
		assertResourceSchema(unSchema);
	}
	
	@Test
	public void testResourceSchemaPrismRoundTrip() throws SchemaException, JAXBException {
		System.out.println("\n===[ testResourceSchemaPrismRoundTrip ]=====");
		// GIVEN
		ResourceSchema schema = createResourceSchema();
		
		System.out.println("Resource schema before serializing to XSD: ");
		System.out.println(schema.dump());
		System.out.println();
		
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		Document xsd = schema.serializeToXsd();

		PrismObjectDefinition<ResourceType> resourceDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
		PrismObject<ResourceType> resource = resourceDefinition.instantiate();
		ResourceType resourceType = resource.asObjectable();
		resourceType.setName(PrismTestUtil.createPolyStringType("Prism With Dynamic Schemas Test"));
		ResourceTypeUtil.setResourceXsdSchema(resource, DOMUtil.getFirstChildElement(xsd));
		
		// WHEN
		
		String marshalledResource = prismContext.getPrismDomProcessor().serializeObjectToString(resource);
		
		System.out.println("Marshalled resource");
		System.out.println(marshalledResource); 
		
		PrismObject<ResourceType> unmarshalledResource = PrismTestUtil.parseObject(marshalledResource);
		
		System.out.println("unmarshalled resource");
		System.out.println(unmarshalledResource.dump());
		
		Element unXsd = ResourceTypeUtil.getResourceXsdSchema(unmarshalledResource);
		
		System.out.println("unmarshalled resource schema");
		System.out.println(DOMUtil.serializeDOMToString(unXsd));
		
		ResourceSchema unSchema = ResourceSchema.parse(unXsd, "unmarshalled resource schema", PrismTestUtil.getPrismContext());
		
		System.out.println("unmarshalled parsed schema");
		System.out.println(unSchema.dump());
		
		// THEN
		assertResourceSchema(unSchema);
	}
	
	private void assertResourceSchema(ResourceSchema unSchema) {
		ObjectClassComplexTypeDefinition objectClassDef = unSchema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE,"AccountObjectClass"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"AccountObjectClass"),objectClassDef.getTypeName());
		assertTrue("AccountObjectClass class not an account", objectClassDef.isAccountType());
		assertTrue("AccountObjectClass class not a DEFAULT account", objectClassDef.isDefaultAccountType());
		
		PrismPropertyDefinition loginDef = objectClassDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"login"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"login"), loginDef.getName());
		assertEquals(DOMUtil.XSD_STRING, loginDef.getTypeName());

		PrismPropertyDefinition passwdDef = objectClassDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE,"password"));
		assertEquals(new QName(SCHEMA_NAMESPACE,"password"), passwdDef.getName());
		assertEquals(SchemaConstants.R_PROTECTED_STRING_TYPE, passwdDef.getTypeName());

		PrismContainerDefinition credDef = objectClassDef.findContainerDefinition(new QName(SchemaConstants.NS_C,"credentials"));
		assertEquals(new QName(SchemaConstants.NS_C,"credentials"), credDef.getName());
		assertEquals(new QName(SchemaConstants.NS_C,"CredentialsType"), credDef.getTypeName());
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
		String resourceXmlString = PrismTestUtil.getPrismContext().getPrismDomProcessor().serializeObjectToString(resource);
		
		System.out.println("Serialized resource");
		System.out.println(resourceXmlString);
		
		PrismObject<ResourceType> reparsedResource = PrismTestUtil.getPrismContext().parseObject(resourceXmlString);
		
		System.out.println("Re-parsed resource");
		System.out.println(reparsedResource.dump());
		
		XmlSchemaType reparsedSchemaType = reparsedResource.asObjectable().getSchema();
		Element reparsedXsdElement = ObjectTypeUtil.findXsdElement(reparsedSchemaType);
		
		System.out.println("Reparsed XSD schema");
		System.out.println(DOMUtil.serializeDOMToString(reparsedXsdElement));
		
		assertDomSchema(reparsedXsdElement);
	}

	
	private PrismObject<ResourceType> wrapInResource(Element xsdElement) {
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
		Element accountAnnotationElement = DOMUtil.findElementRecursive(xsdElement, MidPointConstants.RA_ACCOUNT);
		assertPrefix("ra", accountAnnotationElement);
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
		ResourceSchema schema = new ResourceSchema(SCHEMA_NAMESPACE, PrismTestUtil.getPrismContext());
		
		// Property container
		ObjectClassComplexTypeDefinition containerDefinition = schema.createObjectClassDefinition("AccountObjectClass");
		containerDefinition.setAccountType(true);
		containerDefinition.setDefaultAccountType(true);
		containerDefinition.setDisplayName("The Account");
		containerDefinition.setNativeObjectClass("ACCOUNT");
		// ... in it ordinary attribute - an identifier
		ResourceAttributeDefinition icfUidDef = containerDefinition.createAttributeDefinition(
				SchemaTestConstants.ICFS_UID, DOMUtil.XSD_STRING);
		containerDefinition.getIdentifiers().add(icfUidDef);
		ResourceAttributeDefinition xloginDef = containerDefinition.createAttributeDefinition("login", DOMUtil.XSD_STRING);
		xloginDef.setNativeAttributeName("LOGIN");
		containerDefinition.setDisplayNameAttribute(xloginDef.getName());
		// ... and local property with a type from another schema
		ResourceAttributeDefinition xpasswdDef = containerDefinition.createAttributeDefinition("password", SchemaConstants.R_PROTECTED_STRING_TYPE);
		xpasswdDef.setNativeAttributeName("PASSWORD");
		// ... property reference
		containerDefinition.createAttributeDefinition(SchemaConstants.I_CREDENTIALS, SchemaConstants.I_CREDENTIALS_TYPE);

		return schema;
	}
	
	private void assertPrefix(String expectedPrefix, Element element) {
		assertEquals("Wrong prefix on element "+DOMUtil.getQName(element), expectedPrefix, element.getPrefix());
	}	

//    @Test
//    public void instantiationTest() throws SchemaException, JAXBException {
//        System.out.println("===[ instantiationTest ]===");
//        // GIVEN
//
//        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);
//        ResourceSchema schema = ResourceSchema.parse(DOMUtil.getFirstChildElement(schemaDom), PrismTestUtil.getPrismContext());
//        assertNotNull(schema);
//        System.out.println("Parsed schema:");
//        System.out.println(schema.dump());
//        ObjectClassComplexTypeDefinition accDef = schema.findObjectClassDefinition(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
//        assertNotNull("No AccountObjectClass definition",accDef);
//        assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), accDef.getTypeName());
//        PrismPropertyDefinition loginDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
//        assertEquals(new QName(SCHEMA_NAMESPACE, "login"), loginDef.getName());
//        
//        // WHEN
//
//        // Instantiate PropertyContainer (XSD type)
//        PrismContainer accInst = accDef.instantiate(FIRST_QNAME);
//        assertNotNull(accInst);
//        assertNotNull(accInst.getDefinition());
//        // as the definition is ResourceObjectDefinition, the instance should be of ResoureceObject type
//        assertTrue(accInst instanceof ResourceAttributeContainer);
//
//        // Instantiate Property (XSD element)
//        PrismProperty loginInst = loginDef.instantiate();
//        assertNotNull(loginInst);
//        assertNotNull(loginInst.getDefinition());
//        assertTrue("login is not an attribute", loginInst instanceof ResourceAttribute);
//
//        // Set some value
//        loginInst.setValue(new PrismPropertyValue("FOOBAR"));
//        accInst.getValue().getItems().add(loginInst);
//
//        // Same thing with the prop2 property (type int)
//        PrismPropertyDefinition groupDef = accDef.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "group"));
//        PrismProperty groupInst = groupDef.instantiate();
//        groupInst.setValue(new PrismPropertyValue(321));
//        accInst.getValue().getItems().add(groupInst);
//
//
//        System.out.println("AccountObjectClass INST");
//        System.out.println(accInst.dump());
//
//        // Serialize to DOM - TODO
        
//        Document doc = DOMUtil.getDocument();
//        accInst.serializeToDom(doc);
//
//        // TODO: Serialize to XML and check
//
//        System.out.println("Serialized: ");
//        System.out.println(DOMUtil.serializeDOMToString(doc));
//    }
    
	@Test
	public void testUnmarshallResource() throws JAXBException, SchemaException, FileNotFoundException {
		System.out.println("===[ testUnmarshallResource ]===");
		// WHEN
		ResourceType resource = PrismTestUtil.unmarshalObject(new File("src/test/resources/common/resource-opendj.xml"), ResourceType.class);
		
		// THEN
		
		if (resource.getNativeCapabilities() != null && resource.getNativeCapabilities().getCapabilities() != null) {
			for (Object capability : resource.getNativeCapabilities().getCapabilities().getAny()) {
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

//    @Test
//    public void valueParseTest() throws SchemaException, SchemaException {
//        System.out.println("===[ valueParseTest ]===");
//        // GIVEN
//
//        Document schemaDom = DOMUtil.parseFile(RESOURCE_SCHEMA_SIMPLE_FILENAME);
//        ResourceSchema schema = ResourceSchema.parse(DOMUtil.getFirstChildElement(schemaDom));
//        AssertJUnit.assertNotNull(schema);
//        PrismContainerDefinition type1Def = schema.findContainerDefinitionByType(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"));
//        AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE, "AccountObjectClass"), type1Def.getTypeName());
//        PrismPropertyDefinition prop1Def = type1Def.findPropertyDefinition(new QName(SCHEMA_NAMESPACE, "login"));
//        AssertJUnit.assertEquals(new QName(SCHEMA_NAMESPACE, "login"), prop1Def.getName());
//
//        // WHEN
//
//        Document dataDom = DOMUtil.parseFile(RESOURCE_OBJECT_SIMPLE_FILENAME);
//        PrismContainer container = type1Def.parseItem(DOMUtil.getFirstChildElement(dataDom));
//
//        // THEN
//
//        System.out.println("container: " + container);
//
//        assertEquals(3, container.getItems().size());
//
//        for (Item item : container.getItems()) {
//            ResourceAttribute prop = (ResourceAttribute) item;
//            if (prop.getName().getLocalPart().equals("login")) {
//                AssertJUnit.assertEquals("barbar", prop.getValue(String.class).getValue());
//            }
//            if (prop.getName().getLocalPart().equals("group")) {
//                PrismPropertyValue<Integer> val = prop.getValue(Integer.class);
//                AssertJUnit.assertEquals(Integer.valueOf(123456), val.getValue());
//            }
//            if (prop.getName().getLocalPart().equals("ufo")) {
//                AssertJUnit.assertEquals("Mars attacks!", prop.getValue(String.class).getValue());
//            }
//        }
//    }
//


}
