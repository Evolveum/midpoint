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
package com.evolveum.midpoint.common.refinery;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * @author semancik
 */
public class TestRefinedSchema {

    public static final String TEST_DIR_NAME = "src/test/resources/refinery";
	private static final File RESOURCE_COMPLEX_FILE = new File(TEST_DIR_NAME, "resource-complex.xml");
	private static final File RESOURCE_COMPLEX_DEPRECATED_FILE = new File(TEST_DIR_NAME, "resource-complex-deprecated.xml");
	private static final File RESOURCE_SIMPLE_FILE = new File(TEST_DIR_NAME, "resource-simple.xml");
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testParseFromResourceComplexDeprecated() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseFromResourceComplexDeprecated ]===\n");
    	
        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_DEPRECATED_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);

        // THEN
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.dump());
        assertRefinedSchema(resourceType, rSchema, null, LayerType.MODEL);
        
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.SCHEMA, LayerType.SCHEMA);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.MODEL, LayerType.MODEL);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.PRESENTATION, LayerType.MODEL);

        
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        RefinedAttributeDefinition userPasswordAttribute = rAccount.findAttributeDefinition("userPassword");
        assertNotNull("No userPassword attribute", userPasswordAttribute);
        assertTrue("userPassword not ignored", userPasswordAttribute.isIgnored());
    }
    
    @Test
    public void testParseFromResourceComplex() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseFromResourceComplex ]===\n");
    	
        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);

        // THEN
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.dump());
        assertRefinedSchema(resourceType, rSchema, null, LayerType.MODEL);
        
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.SCHEMA, LayerType.SCHEMA);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.MODEL, LayerType.MODEL);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.PRESENTATION, LayerType.PRESENTATION);
        
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        RefinedAttributeDefinition userPasswordAttribute = rAccount.findAttributeDefinition("userPassword");
        assertNotNull("No userPassword attribute", userPasswordAttribute);
        assertTrue("userPassword not ignored", userPasswordAttribute.isIgnored());
    }

	private void assertLayerRefinedSchema(ResourceType resourceType, RefinedResourceSchema rSchema, LayerType sourceLayer,
			LayerType validationLayer) {
		System.out.println("Refined schema: layer="+sourceLayer);
		LayerRefinedResourceSchema lrSchema = rSchema.forLayer(sourceLayer);
        System.out.println(lrSchema.dump());
        assertRefinedSchema(resourceType, lrSchema, sourceLayer, validationLayer);
	}

	@Test
    public void testParseFromResourceSimple() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseFromResourceSimple ]===\n");
    	
        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_SIMPLE_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);

        // THEN
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.dump());
        
        assertRefinedSchema(resourceType, rSchema, null, LayerType.SCHEMA);

    }
    
    private void assertRefinedSchema(ResourceType resourceType, RefinedResourceSchema rSchema, LayerType sourceLayer, LayerType validationLayer) {
        assertFalse("No account definitions", rSchema.getRefinedDefinitions(ShadowKindType.ACCOUNT).isEmpty());
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        
        RefinedObjectClassDefinition accountDefByNullObjectclass = rSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, null);
        assertTrue("findAccountDefinitionByObjectClass(null) returned wrong value", rAccount.equals(accountDefByNullObjectclass));
        
        RefinedObjectClassDefinition accountDefByIcfAccountObjectclass = rSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, 
        		new QName(resourceType.getNamespace(), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
        assertTrue("findAccountDefinitionByObjectClass(ICF account) returned wrong value", rAccount.equals(accountDefByIcfAccountObjectclass));

        assertRObjectClassDef(rAccount, resourceType, sourceLayer, validationLayer);
        System.out.println("Refined account definitionn:");
        System.out.println(rAccount.dump());
        
        Collection<? extends RefinedAttributeDefinition> attributeDefinitions = rAccount.getAttributeDefinitions();
        assertNotNull("Null attributeDefinitions", attributeDefinitions);
        assertFalse("Empty attributeDefinitions", attributeDefinitions.isEmpty());
        assertEquals("Unexpected number of attributeDefinitions", 55, attributeDefinitions.size());
        
        RefinedAttributeDefinition disabledAttribute = rAccount.findAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No ds-pwp-account-disabled attribute", disabledAttribute);
        assertTrue("ds-pwp-account-disabled not ignored", disabledAttribute.isIgnored());
        
        // This is compatibility with PrismContainerDefinition, it should work well
        Collection<? extends ItemDefinition> propertyDefinitions = rAccount.getDefinitions();
        assertNotNull("Null propertyDefinitions", propertyDefinitions);
        assertFalse("Empty propertyDefinitions", propertyDefinitions.isEmpty());
        assertEquals("Unexpected number of propertyDefinitions", 55, propertyDefinitions.size());
    }


    @Test
    public void testParseAccount() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseAccount ]===\n");

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);
        RefinedObjectClassDefinition defaultAccountDefinition = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        assertNotNull("No refined default account definition in "+rSchema, defaultAccountDefinition);

        PrismObject<ShadowType> accObject = PrismTestUtil.parseObject(new File(TEST_DIR_NAME, "account-jack.xml"));

        // WHEN

        PrismObjectDefinition<ShadowType> objectDefinition = defaultAccountDefinition.getObjectDefinition();

        System.out.println("Refined account definition:");
        System.out.println(objectDefinition.dump());

        accObject.applyDefinition(objectDefinition);

        // THEN

        System.out.println("Parsed account:");
        System.out.println(accObject.dump());

        assertAccountShadow(accObject, resource, prismContext);
    }
    
    @Test
    public void testApplyAttributeDefinition() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testApplyAttributeDefinition ]===\n");

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_FILE);
        
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resource, prismContext);
        RefinedObjectClassDefinition defaultAccountDefinition = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        assertNotNull("No refined default account definition in "+rSchema, defaultAccountDefinition);
        System.out.println("Refined account definition:");
        System.out.println(defaultAccountDefinition.dump());

        PrismObject<ShadowType> accObject = PrismTestUtil.parseObject(new File(TEST_DIR_NAME, "account-jack.xml"));
        PrismContainer<Containerable> attributesContainer = accObject.findContainer(ShadowType.F_ATTRIBUTES);
        System.out.println("Attributes container:");
        System.out.println(attributesContainer.dump());
        
        // WHEN
        attributesContainer.applyDefinition(defaultAccountDefinition.toResourceAttributeContainerDefinition(), true);

        // THEN
        System.out.println("Parsed account:");
        System.out.println(accObject.dump());

        assertAccountShadow(accObject, resource, prismContext);
    }
    
    private void assertAccountShadow(PrismObject<ShadowType> accObject, PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException, JAXBException {
    	ResourceType resourceType = resource.asObjectable();
        QName objectClassQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_NAME, PrismTestUtil.createPolyString("jack"));
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_OBJECT_CLASS, objectClassQName);
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_INTENT, SchemaConstants.INTENT_DEFAULT);

        PrismContainer<?> attributes = accObject.findOrCreateContainer(SchemaConstants.C_ATTRIBUTES);
        assertEquals("Wrong type of <attributes> definition in account", ResourceAttributeContainerDefinition.class, attributes.getDefinition().getClass());
        ResourceAttributeContainerDefinition attrDef = (ResourceAttributeContainerDefinition)attributes.getDefinition();
        assertAttributeDefs(attrDef, resourceType, null, LayerType.MODEL);

        PrismAsserts.assertPropertyValue(attributes, SchemaTestConstants.ICFS_NAME, "uid=jack,ou=People,dc=example,dc=com");
        PrismAsserts.assertPropertyValue(attributes, getAttrQName(resource, "cn"), "Jack Sparrow");
        PrismAsserts.assertPropertyValue(attributes, getAttrQName(resource, "givenName"), "Jack");
        PrismAsserts.assertPropertyValue(attributes, getAttrQName(resource, "sn"), "Sparrow");
        PrismAsserts.assertPropertyValue(attributes, getAttrQName(resource, "uid"), "jack");

        assertEquals("JAXB class name doesn't match (1)", ShadowType.class, accObject.getCompileTimeClass());
        
        accObject.checkConsistence();

        ShadowType accObjectType = accObject.asObjectable();
        assertEquals("Wrong JAXB name", PrismTestUtil.createPolyStringType("jack"), accObjectType.getName());
        assertEquals("Wrong JAXB objectClass", objectClassQName, accObjectType.getObjectClass());
        ShadowAttributesType attributesType = accObjectType.getAttributes();
        assertNotNull("null ResourceObjectShadowAttributesType (JAXB)", attributesType);
        List<Object> attributeElements = attributesType.getAny();
        TestUtil.assertElement(attributeElements, SchemaTestConstants.ICFS_NAME, "uid=jack,ou=People,dc=example,dc=com");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "cn"), "Jack Sparrow");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "givenName"), "Jack");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "sn"), "Sparrow");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "uid"), "jack");
        
        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
        Element accDomElement = jaxbProcessor.marshalObjectToDom(accObjectType, new QName(SchemaConstants.NS_C, "account"), DOMUtil.getDocument());
        System.out.println("Result of JAXB marshalling:\n"+DOMUtil.serializeDOMToString(accDomElement));
        
        accObject.checkConsistence(true, true);
    }
    
	private QName getAttrQName(PrismObject<ResourceType> resource, String localPart) {
		return new QName(ResourceTypeUtil.getResourceNamespace(resource), localPart);
	}

	@Test
    public void testCreateShadow() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testCreateShadow ]===\n");

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);
        assertNotNull("Refined schema is null", rSchema);
        assertFalse("No account definitions", rSchema.getRefinedDefinitions(ShadowKindType.ACCOUNT).isEmpty());
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        
        // WHEN
        PrismObject<ShadowType> blankShadow = rAccount.createBlankShadow();
        
        // THEN
        assertNotNull("No blank shadow", blankShadow);
        assertNotNull("No prism context in blank shadow", blankShadow.getPrismContext());
        PrismObjectDefinition<ShadowType> objectDef = blankShadow.getDefinition();
        assertNotNull("Blank shadow has no definition", objectDef);
        PrismContainerDefinition<?> attrDef = objectDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
        assertNotNull("Blank shadow has no definition for attributes", attrDef);
        assertTrue("Wrong class for attributes definition: "+attrDef.getClass(), attrDef instanceof ResourceAttributeContainerDefinition);
        
    }
    
    @Test
    public void testProtectedAccount() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testProtectedAccount ]===\n");

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);
        assertNotNull("Refined schema is null", rSchema);
        assertFalse("No account definitions", rSchema.getRefinedDefinitions(ShadowKindType.ACCOUNT).isEmpty());
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);

        // WHEN
        Collection<ResourceObjectPattern> protectedAccounts = rAccount.getProtectedObjectPatterns();
        
        // THEN
        assertNotNull("Null protectedAccounts", protectedAccounts);
        assertFalse("Empty protectedAccounts", protectedAccounts.isEmpty());
        assertEquals("Unexpected number of protectedAccounts", 2, protectedAccounts.size());
        Iterator<ResourceObjectPattern> iterator = protectedAccounts.iterator();
        assertProtectedAccount("first protected account", iterator.next(), "uid=idm,ou=Administrators,dc=example,dc=com");
        assertProtectedAccount("second protected account", iterator.next(), "uid=root,ou=Administrators,dc=example,dc=com");
    }

    private void assertAttributeDefs(ResourceAttributeContainerDefinition attrsDef, ResourceType resourceType, LayerType sourceLayer, LayerType validationLayer) {
        assertNotNull("Null account definition", attrsDef);
        assertEquals(SchemaConstants.INTENT_DEFAULT, attrsDef.getIntent());
        assertEquals("AccountObjectClass", attrsDef.getComplexTypeDefinition().getTypeName().getLocalPart());
        assertEquals("Wrong objectclass in the definition of <attributes> definition in account", RefinedObjectClassDefinition.class, attrsDef.getComplexTypeDefinition().getClass());
        RefinedObjectClassDefinition rAccount = (RefinedObjectClassDefinition) attrsDef.getComplexTypeDefinition();
        assertRObjectClassDef(rAccount, resourceType, sourceLayer, validationLayer);
    }
    
    private void assertRObjectClassDef(RefinedObjectClassDefinition rAccount, ResourceType resourceType, LayerType sourceLayer, LayerType validationLayer) {
        assertTrue(rAccount.isDefault());

        Collection<? extends RefinedAttributeDefinition> attrs = rAccount.getAttributeDefinitions();
        assertFalse(attrs.isEmpty());

        assertAttributeDef(attrs, SchemaTestConstants.ICFS_NAME, DOMUtil.XSD_STRING, 1, 1, "Distinguished Name", 
        		true, false, 
        		true, true, validationLayer == LayerType.SCHEMA, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, SchemaTestConstants.ICFS_UID, DOMUtil.XSD_STRING, 1, 1, "Entry UUID", 
        		false, false,
        		false, true, false, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"), DOMUtil.XSD_STRING, 
        		1,  (validationLayer == LayerType.MODEL || validationLayer == LayerType.PRESENTATION) ? 1 : -1, "Common Name", 
        		true, validationLayer == LayerType.PRESENTATION, 
        		true, true, true, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "uid"), 
        		DOMUtil.XSD_STRING, 
        		validationLayer == LayerType.SCHEMA ? 0 : 1 , // minOccurs 
        		validationLayer == LayerType.SCHEMA ? -1 : 1, // maxOccurs
        		"Login Name",
        		true, false,
        		true, true, validationLayer != LayerType.PRESENTATION, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "employeeNumber"), 
        		DOMUtil.XSD_STRING, 0, 1, null,
        		false, false,
        		true, true, true, // Access: create, read, update
        		sourceLayer, validationLayer);
    }

    private void assertAttributeDef(Collection<? extends RefinedAttributeDefinition> attrDefs, QName name,
                                    QName typeName, int minOccurs, int maxOccurs, String displayName, 
                                    boolean hasOutbound, boolean ignore, boolean canCreate, boolean canRead, boolean canUpdate, 
                                    LayerType sourceLayer, LayerType validationLayer) {
        for (RefinedAttributeDefinition def : attrDefs) {
            if (def.getName().equals(name)) {
                assertEquals("Attribute " + name + " ("+sourceLayer+") type mismatch", typeName, def.getTypeName());
                assertEquals("Attribute " + name + " ("+sourceLayer+") minOccurs mismatch", minOccurs, def.getMinOccurs());
                assertEquals("Attribute " + name + " ("+sourceLayer+") maxOccurs mismatch", maxOccurs, def.getMaxOccurs());
                if (validationLayer == LayerType.MODEL || validationLayer == LayerType.PRESENTATION) {
	                assertEquals("Attribute " + name + " ("+sourceLayer+") displayName mismatch", displayName, def.getDisplayName());
	                assertEquals("Attribute " + name + " ("+sourceLayer+") outbound mismatch", hasOutbound, def.getOutboundMappingType() != null);
                }
                assertEquals("Attribute " + name + " ("+sourceLayer+") ignored flag mismatch", ignore, def.isIgnored());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canCreate mismatch", canCreate, def.canCreate());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canRead mismatch", canRead, def.canRead());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canUpdate mismatch", canUpdate, def.canUpdate());
                return;
            }
        }
        Assert.fail("Attribute " + name + " not found");
    }
    
	private void assertProtectedAccount(String message, ResourceObjectPattern protectedAccount, String value) {
		Collection<ResourceAttribute<?>> identifiers = protectedAccount.getIdentifiers();
		assertNotNull("Null identifiers in "+message, identifiers);
		assertEquals("Wrong number identifiers in "+message, 1, identifiers.size());
		ResourceAttribute<?> identifier = identifiers.iterator().next();
		assertNotNull("Null identifier in "+message, identifier);
		assertEquals("Wrong identifier value in "+message, identifier.getRealValue(), value);
		
		// Try matching	
		Collection<ResourceAttribute<?>> testAttrs = new ArrayList<ResourceAttribute<?>>(3);
		ResourceAttribute<String> confusingAttr1 = createStringAttribute(new QName("http://whatever.com","confuseMe"), "HowMuchWoodWouldWoodchuckChuckIfWoodchuckCouldChudkWood");
		testAttrs.add(confusingAttr1);
		ResourceAttribute<String> nameAttr = createStringAttribute(SchemaTestConstants.ICFS_NAME, value);
		testAttrs.add(nameAttr);
		ResourceAttribute<String> confusingAttr2 = createStringAttribute(new QName("http://whatever.com","confuseMeAgain"), "WoodchuckWouldChuckNoWoodAsWoodchuckCannotChuckWood");
		testAttrs.add(confusingAttr2);
		
		assertTrue("Test attr not matched in "+message, protectedAccount.matches(testAttrs));
		nameAttr.setRealValue("huhulumululul");
		assertFalse("Test attr nonsense was matched in "+message, protectedAccount.matches(testAttrs));
	}

	private ResourceAttribute<String> createStringAttribute(QName attrName, String value) {
		ResourceAttributeDefinition testAttrDef = new ResourceAttributeDefinition(attrName, attrName, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
		ResourceAttribute<String> testAttr = testAttrDef.instantiate();
		testAttr.setRealValue(value);
		return testAttr;
	}

}
