/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import com.evolveum.midpoint.schema.processor.*;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
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
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class TestRefinedSchema {

    public static final String TEST_DIR_NAME = "src/test/resources/refinery";
	private static final File RESOURCE_COMPLEX_FILE = new File(TEST_DIR_NAME, "resource-complex.xml");
	private static final File RESOURCE_SIMPLE_FILE = new File(TEST_DIR_NAME, "resource-simple.xml");
	private static final File RESOURCE_POSIX_FILE = new File(TEST_DIR_NAME, "resource-ldap-posix.xml");
	
	private static final String ENTITLEMENT_GROUP_INTENT = "group";
	private static final String ENTITLEMENT_LDAP_GROUP_INTENT = "ldapGroup";
	private static final String ENTITLEMENT_UNIX_GROUP_INTENT = "unixGroup";
	
	private static final QName OBJECT_CLASS_INETORGPERSON_QNAME = new QName(MidPointConstants.NS_RI, "inetOrgPerson");
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testParseFromResourceComplex() throws Exception {
    	final String TEST_NAME = "testParseFromResourceComplex";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.debugDump());
        assertRefinedSchema(resourceType, rSchema, null, LayerType.MODEL, true);
        
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.SCHEMA, LayerType.SCHEMA, true);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.MODEL, LayerType.MODEL, true);
        assertLayerRefinedSchema(resourceType, rSchema, LayerType.PRESENTATION, LayerType.PRESENTATION, true);
        
        RefinedObjectClassDefinition rAccount = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        RefinedAttributeDefinition userPasswordAttribute = rAccount.findAttributeDefinition("userPassword");
        assertNotNull("No userPassword attribute", userPasswordAttribute);
        assertTrue("userPassword not ignored", userPasswordAttribute.isIgnored());
    }

	private void assertLayerRefinedSchema(ResourceType resourceType, RefinedResourceSchema rSchema, LayerType sourceLayer,
			LayerType validationLayer, boolean assertEntitlements) {
		System.out.println("Refined schema: layer="+sourceLayer);
		LayerRefinedResourceSchema lrSchema = rSchema.forLayer(sourceLayer);
        System.out.println(lrSchema.debugDump());
        assertRefinedSchema(resourceType, lrSchema, sourceLayer, validationLayer, assertEntitlements);
	}

	@Test
    public void testParseFromResourceSimple() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseFromResourceSimple ]===\n");
    	
        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_SIMPLE_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);

        // THEN
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.debugDump());
        
        assertRefinedSchema(resourceType, rSchema, null, LayerType.SCHEMA, false);

    }
    
    private void assertRefinedSchema(ResourceType resourceType, RefinedResourceSchema rSchema, 
    		LayerType sourceLayer, LayerType validationLayer, boolean assertEntitlements) {
        assertFalse("No account definitions", rSchema.getRefinedDefinitions(ShadowKindType.ACCOUNT).isEmpty());
        
        RefinedObjectClassDefinition rAccountDef = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        
        RefinedObjectClassDefinition accountDefByNullObjectclass = rSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, null);
        assertEquals("findAccountDefinitionByObjectClass(null) returned wrong value", rAccountDef, accountDefByNullObjectclass);
        
        RefinedObjectClassDefinition accountDefByIcfAccountObjectclass = rSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT, 
        		new QName(resourceType.getNamespace(), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
        assertEquals("findAccountDefinitionByObjectClass(ICF account) returned wrong value", rAccountDef, accountDefByIcfAccountObjectclass);

        assertRObjectClassDef(rAccountDef, resourceType, sourceLayer, validationLayer);
        System.out.println("Refined account definitionn:");
        System.out.println(rAccountDef.debugDump());
        
        assertEquals("Wrong kind", ShadowKindType.ACCOUNT, rAccountDef.getKind());
        
        Collection<? extends RefinedAttributeDefinition> accAttrsDef = rAccountDef.getAttributeDefinitions();
        assertNotNull("Null attributeDefinitions", accAttrsDef);
        assertFalse("Empty attributeDefinitions", accAttrsDef.isEmpty());
        assertEquals("Unexpected number of attributeDefinitions", 55, accAttrsDef.size());
        
        RefinedAttributeDefinition disabledAttribute = rAccountDef.findAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No ds-pwp-account-disabled attribute", disabledAttribute);
        assertTrue("ds-pwp-account-disabled not ignored", disabledAttribute.isIgnored());
        
        RefinedAttributeDefinition<?> displayNameAttributeDef = rAccountDef.getDisplayNameAttribute();
        assertNotNull("No account displayNameAttribute", displayNameAttributeDef);
        assertEquals("Wrong account displayNameAttribute", new QName(resourceType.getNamespace(), "uid"), displayNameAttributeDef.getName());
        
        // This is compatibility with PrismContainerDefinition, it should work well
        Collection<? extends ItemDefinition> propertyDefinitions = rAccountDef.getDefinitions();
        assertNotNull("Null propertyDefinitions", propertyDefinitions);
        assertFalse("Empty propertyDefinitions", propertyDefinitions.isEmpty());
        assertEquals("Unexpected number of propertyDefinitions", 55, propertyDefinitions.size());
        
        if (assertEntitlements) {        	
	        assertFalse("No entitlement definitions", rSchema.getRefinedDefinitions(ShadowKindType.ENTITLEMENT).isEmpty());
	        RefinedObjectClassDefinition rEntDef = rSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, (String)null);
	        assertNotNull("No entitlement definition for null intent", rEntDef);
	        RefinedObjectClassDefinition rEntDefGroup = rSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, ENTITLEMENT_GROUP_INTENT);
	        assertNotNull("No entitlement for intent '"+ENTITLEMENT_GROUP_INTENT+"'", rEntDefGroup);
	        
	        assertEquals("Wrong kind", ShadowKindType.ENTITLEMENT, rEntDef.getKind());
	        
	        Collection<? extends RefinedAttributeDefinition> entAttrDefs = rEntDef.getAttributeDefinitions();
	        assertNotNull("Null attributeDefinitions", entAttrDefs);
	        assertFalse("Empty attributeDefinitions", entAttrDefs.isEmpty());
	        assertEquals("Unexpected number of attributeDefinitions", 11, entAttrDefs.size());
	        // TODO
	        
	        RefinedAttributeDefinition<?> entDisplayNameAttributeDef = rEntDef.getDisplayNameAttribute();
	        assertNotNull("No entitlement displayNameAttribute", entDisplayNameAttributeDef);
	        assertEquals("Wrong entitlement displayNameAttribute", new QName(resourceType.getNamespace(), "cn"), entDisplayNameAttributeDef.getName());
	        
	        assertEquals("Unexpected number of entitlement associations", 1, rAccountDef.getAssociationDefinitions().size());
        }
        
        assertRefinedToLayer(rAccountDef, sourceLayer);
        
        ResourceAttributeContainerDefinition resAttrContainerDef = rAccountDef.toResourceAttributeContainerDefinition();
        assertNotNull("No ResourceAttributeContainerDefinition", resAttrContainerDef);
        System.out.println("\nResourceAttributeContainerDefinition ("+sourceLayer+")");
        System.out.println(resAttrContainerDef.debugDump());
        
        ObjectClassComplexTypeDefinition rComplexTypeDefinition = resAttrContainerDef.getComplexTypeDefinition();
        System.out.println("\nResourceAttributeContainerDefinition ComplexTypeDefinition ("+sourceLayer+")");
        System.out.println(rComplexTypeDefinition.debugDump());
        assertRefinedToLayer(rComplexTypeDefinition, sourceLayer);

        ResourceAttributeDefinition riUidAttrDef = resAttrContainerDef.findAttributeDefinition(new QName(resourceType.getNamespace(), "uid"));
        assertNotNull("No ri:uid def in ResourceAttributeContainerDefinition", riUidAttrDef);
        System.out.println("\nri:uid def "+riUidAttrDef.getClass()+" ("+sourceLayer+")");
        System.out.println(riUidAttrDef.debugDump());
        
        assertRefinedToLayer(riUidAttrDef, sourceLayer);
        
        if (validationLayer == LayerType.PRESENTATION) {
        	assertFalse("Can update "+riUidAttrDef+" from ResourceAttributeContainerDefinition ("+sourceLayer+")", 
        			riUidAttrDef.canModify());
        } else {
        	assertTrue("Cannot update "+riUidAttrDef+" from ResourceAttributeContainerDefinition ("+sourceLayer+")", 
        			riUidAttrDef.canModify());
        }
        
        Collection<? extends ResourceAttributeDefinition> definitionsFromResAttrContainerDef = resAttrContainerDef.getDefinitions();
        for (ResourceAttributeDefinition definitionFromResAttrContainerDef: definitionsFromResAttrContainerDef) {
        	assertRefinedToLayer(definitionFromResAttrContainerDef, sourceLayer);
        }
    }

    private void assertRefinedToLayer(
			ObjectClassComplexTypeDefinition ocDef,
			LayerType expectedLayer) {
    	if (expectedLayer == null) {
    		// This is OK, it may not be layer-bound.
    		return;
    	}
    	if (!(ocDef instanceof LayerRefinedObjectClassDefinition)) {
			AssertJUnit.fail("Expected that definition of objectclass "+ocDef.getTypeName()+" in layer "+expectedLayer
					+" will be LayerRefinedObjectClassDefinition, but it is "+ocDef.getClass()+": "+ocDef);
		}
		assertEquals("Wrong layer in "+ocDef, expectedLayer, ((LayerRefinedObjectClassDefinition)ocDef).getLayer());
	}

	private void assertRefinedToLayer(ResourceAttributeDefinition attrDef, LayerType expectedLayer) {
    	if (expectedLayer == null) {
    		// This is OK, it may not be layer-bound.
    		return;
    	}
		if (!(attrDef instanceof LayerRefinedAttributeDefinition)) {
			AssertJUnit.fail("Expected that definition of attribute "+attrDef.getName()+" in layer "+expectedLayer
					+" will be LayerRefinedAttributeDefinition, but it is "+attrDef.getClass()+": "+attrDef);
		}
		assertEquals("Wrong layer in "+attrDef, expectedLayer, ((LayerRefinedAttributeDefinition)attrDef).getLayer());
	}

	@Test
    public void testParseAccount() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testParseAccount ]===\n");

        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);
        RefinedObjectClassDefinition defaultAccountDefinition = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        assertNotNull("No refined default account definition in "+rSchema, defaultAccountDefinition);

        PrismObject<ShadowType> accObject = prismContext.parseObject(new File(TEST_DIR_NAME, "account-jack.xml"));

        // WHEN

        PrismObjectDefinition<ShadowType> objectDefinition = defaultAccountDefinition.getObjectDefinition();

        System.out.println("Refined account definition:");
        System.out.println(objectDefinition.debugDump());

        accObject.applyDefinition(objectDefinition);

        // THEN

        System.out.println("Parsed account:");
        System.out.println(accObject.debugDump());

        assertAccountShadow(accObject, resource, prismContext);
    }
    
    @Test
    public void testApplyAttributeDefinition() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testApplyAttributeDefinition ]===\n");

        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_COMPLEX_FILE);
        
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resource, prismContext);
        RefinedObjectClassDefinition defaultAccountDefinition = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        assertNotNull("No refined default account definition in "+rSchema, defaultAccountDefinition);
        System.out.println("Refined account definition:");
        System.out.println(defaultAccountDefinition.debugDump());

        PrismObject<ShadowType> accObject = prismContext.parseObject(new File(TEST_DIR_NAME, "account-jack.xml"));
        PrismContainer<Containerable> attributesContainer = accObject.findContainer(ShadowType.F_ATTRIBUTES);
        System.out.println("Attributes container:");
        System.out.println(attributesContainer.debugDump());
        
        // WHEN
        attributesContainer.applyDefinition((PrismContainerDefinition)defaultAccountDefinition.toResourceAttributeContainerDefinition(), true);

        // THEN
        System.out.println("Parsed account:");
        System.out.println(accObject.debugDump());

        assertAccountShadow(accObject, resource, prismContext);
    }
    
    private void assertAccountShadow(PrismObject<ShadowType> accObject, PrismObject<ResourceType> resource, PrismContext prismContext) throws SchemaException, JAXBException {
    	ResourceType resourceType = resource.asObjectable();
        QName objectClassQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_NAME, createPolyString("jack"));
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_OBJECT_CLASS, objectClassQName);
        PrismAsserts.assertPropertyValue(accObject, ShadowType.F_INTENT, SchemaConstants.INTENT_DEFAULT);

        PrismContainer<?> attributes = accObject.findOrCreateContainer(SchemaConstants.C_ATTRIBUTES);
        assertEquals("Wrong type of <attributes> definition in account", ResourceAttributeContainerDefinitionImpl.class, attributes.getDefinition().getClass());
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
        assertEquals("Wrong JAXB name", createPolyStringType("jack"), accObjectType.getName());
        assertEquals("Wrong JAXB objectClass", objectClassQName, accObjectType.getObjectClass());
        ShadowAttributesType attributesType = accObjectType.getAttributes();
        assertNotNull("null ResourceObjectShadowAttributesType (JAXB)", attributesType);
        List<Object> attributeElements = attributesType.getAny();
        TestUtil.assertElement(attributeElements, SchemaTestConstants.ICFS_NAME, "uid=jack,ou=People,dc=example,dc=com");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "cn"), "Jack Sparrow");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "givenName"), "Jack");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "sn"), "Sparrow");
        TestUtil.assertElement(attributeElements, getAttrQName(resource, "uid"), "jack");
        
        String accString = PrismTestUtil.serializeObjectToString(accObjectType.asPrismObject());
        System.out.println("Result of JAXB marshalling:\n"+accString);
        
        accObject.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
    }
    
	private QName getAttrQName(PrismObject<ResourceType> resource, String localPart) {
		return new QName(ResourceTypeUtil.getResourceNamespace(resource), localPart);
	}

	@Test
    public void testCreateShadow() throws JAXBException, SchemaException, SAXException, IOException {
    	System.out.println("\n===[ testCreateShadow ]===\n");

        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();

        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);
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
    	PrismContext prismContext = createInitializedPrismContext();
        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_COMPLEX_FILE);
        ResourceType resourceType = resource.asObjectable();
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);
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
        assertDeprecatedProtectedAccount("first protected account", iterator.next(), "uid=idm,ou=Administrators,dc=example,dc=com", rAccount);
        assertDeprecatedProtectedAccount("second protected account", iterator.next(), "uid=root,ou=Administrators,dc=example,dc=com", rAccount);
    }

    private void assertAttributeDefs(ResourceAttributeContainerDefinition attrsDef, ResourceType resourceType, LayerType sourceLayer, LayerType validationLayer) {
        assertNotNull("Null account definition", attrsDef);
        assertEquals(SchemaConstants.INTENT_DEFAULT, attrsDef.getIntent());
        assertEquals("AccountObjectClass", attrsDef.getComplexTypeDefinition().getTypeName().getLocalPart());
        assertEquals("Wrong objectclass in the definition of <attributes> definition in account", RefinedObjectClassDefinitionImpl.class, attrsDef.getComplexTypeDefinition().getClass());
        RefinedObjectClassDefinition rAccount = (RefinedObjectClassDefinition) attrsDef.getComplexTypeDefinition();
        assertRObjectClassDef(rAccount, resourceType, sourceLayer, validationLayer);
    }
    
    private void assertRObjectClassDef(RefinedObjectClassDefinition rAccount, ResourceType resourceType, LayerType sourceLayer, LayerType validationLayer) {
        assertTrue(rAccount.isDefault());

        Collection<? extends RefinedAttributeDefinition> attrs = rAccount.getAttributeDefinitions();
        assertFalse(attrs.isEmpty());

        assertAttributeDef(attrs, SchemaTestConstants.ICFS_NAME, DOMUtil.XSD_STRING, 1, 1, "Distinguished Name", 110,
        		true, false, 
        		true, true, validationLayer == LayerType.SCHEMA, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, SchemaTestConstants.ICFS_UID, DOMUtil.XSD_STRING, 1, 1, "Entry UUID", 100,
        		false, false,
        		false, true, false, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"), DOMUtil.XSD_STRING, 
        		1,  (validationLayer == LayerType.MODEL || validationLayer == LayerType.PRESENTATION) ? 1 : -1, "Common Name", 1, 
        		true, validationLayer == LayerType.PRESENTATION, 
        		true, true, true, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "uid"), 
        		DOMUtil.XSD_STRING, 
        		validationLayer == LayerType.SCHEMA ? 0 : 1 , // minOccurs 
        		validationLayer == LayerType.SCHEMA ? -1 : 1, // maxOccurs
        		"Login Name", 2,
        		true, false,
        		true, true, validationLayer != LayerType.PRESENTATION, // Access: create, read, update
        		sourceLayer, validationLayer);
        
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "employeeNumber"), 
        		DOMUtil.XSD_STRING, 0, 1, null, null,
        		false, false,
        		true, true, true, // Access: create, read, update
        		sourceLayer, validationLayer);
    }

    private void assertAttributeDef(Collection<? extends RefinedAttributeDefinition> attrDefs, QName name,
                                    QName typeName, int minOccurs, int maxOccurs, String displayName, Integer displayOrder,
                                    boolean hasOutbound, boolean ignore, boolean canCreate, boolean canRead, boolean canUpdate, 
                                    LayerType sourceLayer, LayerType validationLayer) {
        for (RefinedAttributeDefinition def : attrDefs) {
            if (def.getName().equals(name)) {
                assertEquals("Attribute " + name + " ("+sourceLayer+") type mismatch", typeName, def.getTypeName());
                assertEquals("Attribute " + name + " ("+sourceLayer+") minOccurs mismatch", minOccurs, def.getMinOccurs());
                assertEquals("Attribute " + name + " ("+sourceLayer+") maxOccurs mismatch", maxOccurs, def.getMaxOccurs());
                if (validationLayer == LayerType.MODEL || validationLayer == LayerType.PRESENTATION) {
	                assertEquals("Attribute " + name + " ("+sourceLayer+") displayName mismatch", displayName, def.getDisplayName());
	                assertEquals("Attribute " + name + " ("+sourceLayer+") displayOrder mismatch", displayOrder, def.getDisplayOrder());
	                assertEquals("Attribute " + name + " ("+sourceLayer+") outbound mismatch", hasOutbound, def.getOutboundMappingType() != null);
                }
                assertEquals("Attribute " + name + " ("+sourceLayer+") ignored flag mismatch", ignore, def.isIgnored());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canCreate mismatch", canCreate, def.canAdd());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canRead mismatch", canRead, def.canRead());
                assertEquals("Attribute " + name + " ("+sourceLayer+") canUpdate mismatch", canUpdate, def.canModify());
                return;
            }
        }
        Assert.fail("Attribute " + name + " not found");
    }
    
	private void assertDeprecatedProtectedAccount(String message, ResourceObjectPattern protectedPattern, String identifierValue, RefinedObjectClassDefinition rAccount) throws SchemaException {
		Collection<ResourceAttribute<?>> identifiers = protectedPattern.getIdentifiers();
		assertNotNull("Null identifiers in "+message, identifiers);
		assertEquals("Wrong number identifiers in "+message, 1, identifiers.size());
		ResourceAttribute<?> identifier = identifiers.iterator().next();
		assertNotNull("Null identifier in "+message, identifier);
		assertEquals("Wrong identifier value in "+message, identifier.getRealValue(), identifierValue);
		
		// Try matching	
		PrismObject<ShadowType> shadow = rAccount.getObjectDefinition().instantiate();
		ResourceAttributeContainer attributesContainer = ShadowUtil.getOrCreateAttributesContainer(shadow, rAccount);
		ResourceAttribute<String> confusingAttr1 = createStringAttribute(new QName("http://whatever.com","confuseMe"), "HowMuchWoodWouldWoodchuckChuckIfWoodchuckCouldChudkWood");
		attributesContainer.add(confusingAttr1);
		ResourceAttribute<String> nameAttr = createStringAttribute(SchemaTestConstants.ICFS_NAME, identifierValue);
		attributesContainer.add(nameAttr);
		ResourceAttribute<String> confusingAttr2 = createStringAttribute(new QName("http://whatever.com","confuseMeAgain"), "WoodchuckWouldChuckNoWoodAsWoodchuckCannotChuckWood");
		attributesContainer.add(confusingAttr2);
				
		assertTrue("Test attr not matched in "+message, protectedPattern.matches(shadow, null));
		nameAttr.setRealValue("huhulumululul");
		assertFalse("Test attr nonsense was matched in "+message, protectedPattern.matches(shadow, null));
	}

	private ResourceAttribute<String> createStringAttribute(QName attrName, String value) {
		ResourceAttributeDefinition testAttrDef = new ResourceAttributeDefinitionImpl(attrName, DOMUtil.XSD_STRING, getPrismContext());
		ResourceAttribute<String> testAttr = testAttrDef.instantiate();
		testAttr.setRealValue(value);
		return testAttr;
	}

	@Test
    public void testParseFromResourcePosix() throws Exception {
    	final String TEST_NAME = "testParseFromResourcePosix";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	PrismContext prismContext = createInitializedPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_POSIX_FILE);
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.parse(resourceType, prismContext);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.debugDump());
        
        assertFalse("No account definitions", rSchema.getRefinedDefinitions(ShadowKindType.ACCOUNT).isEmpty());
        
        // ### default account objectType
        
        RefinedObjectClassDefinition rAccountDef = rSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, (String)null);
        assertNotNull("No default account definition", rAccountDef);
        
        RefinedObjectClassDefinition accountDefByIcfAccountObjectclass = rSchema.findRefinedDefinitionByObjectClassQName(ShadowKindType.ACCOUNT,
        		OBJECT_CLASS_INETORGPERSON_QNAME);
        assertTrue("findAccountDefinitionByObjectClass("+OBJECT_CLASS_INETORGPERSON_QNAME+") returned wrong value", rAccountDef.equals(accountDefByIcfAccountObjectclass));
        
        assertTrue(rAccountDef.isDefault());

        Collection<? extends RefinedAttributeDefinition> rAccountAttrs = rAccountDef.getAttributeDefinitions();
        assertFalse(rAccountAttrs.isEmpty());

        assertAttributeDef(rAccountAttrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "dn"), 
        		DOMUtil.XSD_STRING, 1, 1, "Distinguished Name", 110,
        		true, false, 
        		true, true, true, // Access: create, read, update
        		LayerType.SCHEMA, LayerType.PRESENTATION);
        
        assertAttributeDef(rAccountAttrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "entryUUID"), 
        		DOMUtil.XSD_STRING, 0, 1, null, 100,
        		false, false,
        		false, true, false, // Access: create, read, update
        		LayerType.SCHEMA, LayerType.PRESENTATION);
        
        assertAttributeDef(rAccountAttrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"), 
        		DOMUtil.XSD_STRING, 1, -1, "Common Name", 590, 
        		true, false, 
        		true, true, true, // Access: create, read, update
        		LayerType.SCHEMA, LayerType.PRESENTATION);
        
        assertAttributeDef(rAccountAttrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "uid"), 
        		DOMUtil.XSD_STRING, 0, -1, "Login Name", 300,
        		true, false,
        		true, true, true, // Access: create, read, update
        		LayerType.SCHEMA, LayerType.PRESENTATION);
        
        assertAttributeDef(rAccountAttrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "employeeNumber"), 
        		DOMUtil.XSD_STRING, 0, 1, null, 140,
        		false, false,
        		true, true, true, // Access: create, read, update
        		LayerType.SCHEMA, LayerType.PRESENTATION);
        
        System.out.println("Refined account definitionn:");
        System.out.println(rAccountDef.debugDump());
        
        assertEquals("Wrong kind", ShadowKindType.ACCOUNT, rAccountDef.getKind());
        
        Collection<? extends RefinedAttributeDefinition> accAttrsDef = rAccountDef.getAttributeDefinitions();
        assertNotNull("Null attributeDefinitions", accAttrsDef);
        assertFalse("Empty attributeDefinitions", accAttrsDef.isEmpty());
        assertEquals("Unexpected number of attributeDefinitions", 53, accAttrsDef.size());
        
        RefinedAttributeDefinition disabledAttribute = rAccountDef.findAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No ds-pwp-account-disabled attribute", disabledAttribute);
        assertTrue("ds-pwp-account-disabled not ignored", disabledAttribute.isIgnored());
        
        RefinedAttributeDefinition<?> displayNameAttributeDef = rAccountDef.getDisplayNameAttribute();
        assertNotNull("No account displayNameAttribute", displayNameAttributeDef);
        assertEquals("Wrong account displayNameAttribute", new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "dn"), 
        		displayNameAttributeDef.getName());
        
        // This is compatibility with PrismContainerDefinition, it should work well
        Collection<? extends ItemDefinition> propertyDefinitions = rAccountDef.getDefinitions();
        assertNotNull("Null propertyDefinitions", propertyDefinitions);
        assertFalse("Empty propertyDefinitions", propertyDefinitions.isEmpty());
        assertEquals("Unexpected number of propertyDefinitions", 53, propertyDefinitions.size());
                	
        assertFalse("No entitlement definitions", rSchema.getRefinedDefinitions(ShadowKindType.ENTITLEMENT).isEmpty());
        RefinedObjectClassDefinition rEntDef = rSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, (String)null);
        assertNotNull("No entitlement definition for null intent", rEntDef);
        RefinedObjectClassDefinition rEntDefGroup = rSchema.getRefinedDefinition(ShadowKindType.ENTITLEMENT, ENTITLEMENT_LDAP_GROUP_INTENT);
        assertNotNull("No entitlement for intent '"+ENTITLEMENT_LDAP_GROUP_INTENT+"'", rEntDefGroup);
        
        assertEquals("Wrong kind", ShadowKindType.ENTITLEMENT, rEntDef.getKind());
        
        Collection<? extends RefinedAttributeDefinition> entAttrDefs = rEntDef.getAttributeDefinitions();
        assertNotNull("Null attributeDefinitions", entAttrDefs);
        assertFalse("Empty attributeDefinitions", entAttrDefs.isEmpty());
        assertEquals("Unexpected number of attributeDefinitions", 12, entAttrDefs.size());

        
        RefinedAttributeDefinition<?> entDisplayNameAttributeDef = rEntDef.getDisplayNameAttribute();
        assertNotNull("No entitlement displayNameAttribute", entDisplayNameAttributeDef);
        assertEquals("Wrong entitlement displayNameAttribute", new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "dn"),
        		entDisplayNameAttributeDef.getName());
        
        assertEquals("Unexpected number of entitlement associations", 1, rAccountDef.getAssociationDefinitions().size());
        
        ResourceAttributeContainerDefinition resAttrContainerDef = rAccountDef.toResourceAttributeContainerDefinition();
        assertNotNull("No ResourceAttributeContainerDefinition", resAttrContainerDef);
        System.out.println("\nResourceAttributeContainerDefinition");
        System.out.println(resAttrContainerDef.debugDump());
        
        ObjectClassComplexTypeDefinition rComplexTypeDefinition = resAttrContainerDef.getComplexTypeDefinition();
        System.out.println("\nResourceAttributeContainerDefinition ComplexTypeDefinition");
        System.out.println(rComplexTypeDefinition.debugDump());

        ResourceAttributeDefinition<String> riUidAttrDef = resAttrContainerDef.findAttributeDefinition(new QName(resourceType.getNamespace(), "uid"));
        assertNotNull("No ri:uid def in ResourceAttributeContainerDefinition", riUidAttrDef);
        System.out.println("\nri:uid def "+riUidAttrDef.getClass());
        System.out.println(riUidAttrDef.debugDump());
        
    }
	
}
