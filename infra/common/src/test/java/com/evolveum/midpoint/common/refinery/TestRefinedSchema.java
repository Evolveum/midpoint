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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestRefinedSchema {

    public static final String TEST_DIR_NAME = "src/test/resources/refinery";
    private static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd";
    private static final QName ICFS_NAME = new QName(NS_ICFS, "name");
    private static final QName ICFS_UID = new QName(NS_ICFS, "uid");
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testParseFromResource() throws JAXBException, SchemaException, SAXException, IOException {

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_DIR_NAME, "resource.xml"));
        ResourceType resourceType = resource.asObjectable();

        // WHEN
        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);

        // THEN
        assertNotNull("Refined schema is null", rSchema);
        System.out.println("Refined schema");
        System.out.println(rSchema.dump());

        assertFalse("No account definitions", rSchema.getAccountDefinitions().isEmpty());
        RefinedAccountDefinition rAccount = rSchema.getAccountDefinition("user");

        assertAttributeDefs(rAccount, resourceType);
    }

    private void assertAttributeDefs(RefinedAccountDefinition rAccount, ResourceType resourceType) {
        assertNotNull("Null account definition", rAccount);
        assertEquals("user", rAccount.getAccountTypeName());
        assertEquals("AccountObjectClass", rAccount.getObjectClassDefinition().getTypeName().getLocalPart());
        assertTrue(rAccount.isDefault());

        Collection<RefinedAttributeDefinition> attrs = rAccount.getAttributeDefinitions();
        assertFalse(attrs.isEmpty());

        assertAttributeDef(attrs, ICFS_NAME, DOMUtil.XSD_STRING, 1, 1, "Distinguished Name", true);
        assertAttributeDef(attrs, ICFS_UID, DOMUtil.XSD_STRING, 1, 1, "Entry UUID", false);
        assertAttributeDef(attrs, new QName(resourceType.getNamespace(), "cn"), DOMUtil.XSD_STRING, 1, -1, "Common Name", true);
        assertAttributeDef(attrs, new QName(resourceType.getNamespace(), "employeeNumber"), DOMUtil.XSD_STRING, 0, 1, null, false);
        // TODO: check access
    }

    private void assertAttributeDef(Collection<RefinedAttributeDefinition> attrDefs, QName name,
                                    QName typeName, int minOccurs, int maxOccurs, String displayName, boolean hasOutbound) {
        for (RefinedAttributeDefinition def : attrDefs) {
            if (def.getName().equals(name)) {
                assertEquals("Attribute " + name + " type mismatch", typeName, def.getTypeName());
                assertEquals("Attribute " + name + " minOccurs mismatch", minOccurs, def.getMinOccurs());
                assertEquals("Attribute " + name + " maxOccurs mismatch", maxOccurs, def.getMaxOccurs());
                assertEquals("Attribute " + name + " displayName mismatch", displayName, def.getDisplayName());
                assertEquals("Attribute " + name + " outbound mismatch", hasOutbound, def.getOutboundValueConstructionType() != null);
                return;
            }
        }
        Assert.fail("Attribute " + name + " not found");
    }

    @Test
    public void testParseAccount() throws JAXBException, SchemaException, SAXException, IOException {

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_DIR_NAME, "resource.xml"));
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

        assertProperty(accObject, SchemaConstants.C_NAME, "jack");
        assertProperty(accObject, SchemaConstants.I_OBJECT_CLASS, new QName(resourceType.getNamespace(), "AccountObjectClass"));
        assertProperty(accObject, new QName(SchemaConstants.NS_C, "accountType"), "user");

        PrismContainer attributes = accObject.findOrCreateContainer(SchemaConstants.I_ATTRIBUTES);
        RefinedAccountDefinition attrDef = (RefinedAccountDefinition) attributes.getDefinition();
        assertAttributeDefs(attrDef, resourceType);

        assertProperty(attributes, ICFS_NAME, "uid=jack,ou=People,dc=example,dc=com");
        assertProperty(attributes, new QName(resourceType.getNamespace(), "cn"), "Jack Sparrow");
        assertProperty(attributes, new QName(resourceType.getNamespace(), "givenName"), "Jack");
        assertProperty(attributes, new QName(resourceType.getNamespace(), "sn"), "Sparrow");
        assertProperty(attributes, new QName(resourceType.getNamespace(), "uid"), "jack");

        assertEquals("JAXB class name doesn't match (1)", AccountShadowType.class, accObject.getCompileTimeClass());


    }
    
    @Test
    public void testCreateShadow() throws JAXBException, SchemaException, SAXException, IOException {

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_DIR_NAME, "resource.xml"));
        ResourceType resourceType = resource.asObjectable();

        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);
        assertNotNull("Refined schema is null", rSchema);
        assertFalse("No account definitions", rSchema.getAccountDefinitions().isEmpty());
        RefinedAccountDefinition rAccount = rSchema.getAccountDefinition("user");
        
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

    private void assertProperty(PrismContainer cont, QName propName, Object value) {
        PrismProperty prop = cont.findProperty(propName);
        assertNotNull(propName + " in null", prop);
        assertEquals(propName + " has wrong name", propName, prop.getName());
        assertEquals(propName + " has wrong value", value, prop.getValue().getValue());
    }

}
