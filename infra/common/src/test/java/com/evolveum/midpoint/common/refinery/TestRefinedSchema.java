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

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestRefinedSchema {

    public static final String TEST_DIR_NAME = "src/test/resources/refinery";
	private static final File RESOURCE_COMPLEX_FILE = new File(TEST_DIR_NAME, "resource-complex.xml");
	private static final File RESOURCE_SIMPLE_FILE = new File(TEST_DIR_NAME, "resource-simple.xml");
	private static final String DEFAULT_INTENT = "default";
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
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

        assertRefinedSchema(resourceType, rSchema, true);
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
        
        assertRefinedSchema(resourceType, rSchema, false);

    }
    
    private void assertRefinedSchema(ResourceType resourceType, RefinedResourceSchema rSchema, boolean hasSchemaHandling) {
        assertFalse("No account definitions", rSchema.getAccountDefinitions().isEmpty());
        RefinedAccountDefinition rAccount = rSchema.getAccountDefinition((String)null);
        
        RefinedAccountDefinition accountDefByNullObjectclass = rSchema.findAccountDefinitionByObjectClass(null);
        assertTrue("findAccountDefinitionByObjectClass(null) returned wrong value", rAccount == accountDefByNullObjectclass);
        
        RefinedAccountDefinition accountDefByIcfAccountObjectclass = rSchema.findAccountDefinitionByObjectClass(
        		new QName(resourceType.getNamespace(), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
        assertTrue("findAccountDefinitionByObjectClass(ICF account) returned wrong value", rAccount == accountDefByIcfAccountObjectclass);

        assertAttributeDefs(rAccount, resourceType, hasSchemaHandling);
        System.out.println("Refined account definitionn:");
        System.out.println(rAccount.dump());
        
        ObjectClassComplexTypeDefinition complexTypeDefinition = rAccount.getComplexTypeDefinition();
        assertNotNull("No complexType definition", complexTypeDefinition);
        
        Collection<RefinedAttributeDefinition> attributeDefinitions = rAccount.getAttributeDefinitions();
        assertNotNull("Null attributeDefinitions", attributeDefinitions);
        assertFalse("Empty attributeDefinitions", attributeDefinitions.isEmpty());
        assertEquals("Unexpected number of attributeDefinitions", 55, attributeDefinitions.size());
        
        RefinedAttributeDefinition disabledAttribute = rAccount.findAttributeDefinition("ds-pwp-account-disabled");
        assertNotNull("No ds-pwp-account-disabled attribute", disabledAttribute);
        assertTrue("ds-pwp-account-disabled not ignored", disabledAttribute.isIgnored());
        
        // This is compatibility with PrismContainerDefinition, it should work well
        Set<PrismPropertyDefinition> propertyDefinitions = rAccount.getPropertyDefinitions();
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
        RefinedAccountDefinition defaultAccountDefinition = rSchema.getDefaultAccountDefinition();

        JAXBElement<AccountShadowType> accJaxbElement = PrismTestUtil.unmarshalElement(new File(TEST_DIR_NAME, "account-jack.xml"), AccountShadowType.class);
        AccountShadowType accType = accJaxbElement.getValue();

        // WHEN

        PrismObjectDefinition<AccountShadowType> objectDefinition = defaultAccountDefinition.getObjectDefinition();

        System.out.println("Refined account definition:");
        System.out.println(objectDefinition.dump());

        PrismObject<AccountShadowType> accObject = accType.asPrismObject();
        accObject.applyDefinition(objectDefinition);

        // THEN

        System.out.println("Parsed account:");
        System.out.println(accObject.dump());

        PrismAsserts.assertPropertyValue(accObject, SchemaConstants.C_NAME, "jack");
        PrismAsserts.assertPropertyValue(accObject, SchemaConstants.I_OBJECT_CLASS, 
        		new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"));
        PrismAsserts.assertPropertyValue(accObject, AccountShadowType.F_INTENT, DEFAULT_INTENT);

        PrismContainer<?> attributes = accObject.findOrCreateContainer(SchemaConstants.I_ATTRIBUTES);
        assertEquals("Wrong type of <attributes> definition in account", RefinedAccountDefinition.class, attributes.getDefinition().getClass());
        RefinedAccountDefinition attrDef = (RefinedAccountDefinition) attributes.getDefinition();
        assertAttributeDefs(attrDef, resourceType, true);

        PrismAsserts.assertPropertyValue(attributes, SchemaTestConstants.ICFS_NAME, "uid=jack,ou=People,dc=example,dc=com");
        PrismAsserts.assertPropertyValue(attributes, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"), "Jack Sparrow");
        PrismAsserts.assertPropertyValue(attributes, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "givenName"), "Jack");
        PrismAsserts.assertPropertyValue(attributes, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "sn"), "Sparrow");
        PrismAsserts.assertPropertyValue(attributes, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "uid"), "jack");

        assertEquals("JAXB class name doesn't match (1)", AccountShadowType.class, accObject.getCompileTimeClass());


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
        assertFalse("No account definitions", rSchema.getAccountDefinitions().isEmpty());
        RefinedAccountDefinition rAccount = rSchema.getAccountDefinition((String)null);
        
        // WHEN
        PrismObject<AccountShadowType> blankShadow = rAccount.createBlankShadow();
        
        // THEN
        assertNotNull("No blank shadow", blankShadow);
        assertNotNull("No prism context in blank shadow", blankShadow.getPrismContext());
        PrismObjectDefinition<AccountShadowType> objectDef = blankShadow.getDefinition();
        assertNotNull("Blank shadow has no definition", objectDef);
        PrismContainerDefinition<?> attrDef = objectDef.findContainerDefinition(AccountShadowType.F_ATTRIBUTES);
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
        assertFalse("No account definitions", rSchema.getAccountDefinitions().isEmpty());
        RefinedAccountDefinition rAccount = rSchema.getAccountDefinition((String)null);

        // WHEN
        Collection<ResourceObjectPattern> protectedAccounts = rAccount.getProtectedAccounts();
        
        // THEN
        assertNotNull("Null protectedAccounts", protectedAccounts);
        assertFalse("Empty protectedAccounts", protectedAccounts.isEmpty());
        assertEquals("Unexpected number of protectedAccounts", 2, protectedAccounts.size());
        Iterator<ResourceObjectPattern> iterator = protectedAccounts.iterator();
        assertProtectedAccount("first protected account", iterator.next(), "uid=idm,ou=Administrators,dc=example,dc=com");
        assertProtectedAccount("second protected account", iterator.next(), "uid=root,ou=Administrators,dc=example,dc=com");
    }

    private void assertAttributeDefs(RefinedAccountDefinition rAccount, ResourceType resourceType, boolean hasSchemaHandling) {
        assertNotNull("Null account definition", rAccount);
        assertEquals(DEFAULT_INTENT, rAccount.getAccountTypeName());
        assertEquals("AccountObjectClass", rAccount.getObjectClassDefinition().getTypeName().getLocalPart());
        assertTrue(rAccount.isDefault());

        Collection<RefinedAttributeDefinition> attrs = rAccount.getAttributeDefinitions();
        assertFalse(attrs.isEmpty());

        assertAttributeDef(attrs, SchemaTestConstants.ICFS_NAME, DOMUtil.XSD_STRING, 1, 1, "Distinguished Name", true, hasSchemaHandling);
        assertAttributeDef(attrs, SchemaTestConstants.ICFS_UID, DOMUtil.XSD_STRING, 1, 1, "Entry UUID", false, hasSchemaHandling);
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"), DOMUtil.XSD_STRING, 1, -1, "Common Name", true, hasSchemaHandling);
        assertAttributeDef(attrs, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "employeeNumber"), DOMUtil.XSD_STRING, 0, 1, null, false, hasSchemaHandling);
        // TODO: check access
    }

    private void assertAttributeDef(Collection<RefinedAttributeDefinition> attrDefs, QName name,
                                    QName typeName, int minOccurs, int maxOccurs, String displayName, 
                                    boolean hasOutbound, boolean hasSchemaHandling) {
        for (RefinedAttributeDefinition def : attrDefs) {
            if (def.getName().equals(name)) {
                assertEquals("Attribute " + name + " type mismatch", typeName, def.getTypeName());
                assertEquals("Attribute " + name + " minOccurs mismatch", minOccurs, def.getMinOccurs());
                assertEquals("Attribute " + name + " maxOccurs mismatch", maxOccurs, def.getMaxOccurs());
                if (hasSchemaHandling) {
	                assertEquals("Attribute " + name + " displayName mismatch", displayName, def.getDisplayName());
	                assertEquals("Attribute " + name + " outbound mismatch", hasOutbound, def.getOutboundValueConstructionType() != null);
                }
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
