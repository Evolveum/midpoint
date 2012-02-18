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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.testng.Assert;
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

    @Test
    public void testParseFromResource() throws JAXBException, SchemaException, SAXException, IOException {

        // GIVEN
    	PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        JAXBElement<ResourceType> jaxbElement = PrismTestUtil.unmarshalElement(new File(TEST_DIR_NAME, "resource.xml"), ResourceType.class);
        ResourceType resourceType = jaxbElement.getValue();

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

        JAXBElement<ResourceType> resJaxbElement = PrismTestUtil.unmarshalElement(new File(TEST_DIR_NAME, "resource.xml"), ResourceType.class);
        ResourceType resourceType = resJaxbElement.getValue();

        RefinedResourceSchema rSchema = RefinedResourceSchema.parse(resourceType, prismContext);
        RefinedAccountDefinition defaultAccountDefinition = rSchema.getDefaultAccountDefinition();

        JAXBElement<AccountShadowType> accJaxbElement = PrismTestUtil.unmarshalElement(new File(TEST_DIR_NAME, "account-jack.xml"), AccountShadowType.class);
        AccountShadowType accType = accJaxbElement.getValue();

        // WHEN

        PrismObjectDefinition<AccountShadowType> objectDefinition = defaultAccountDefinition.getObjectDefinition();

        System.out.println("Refined account definition:");
        System.out.println(objectDefinition.dump());

        PrismObject<AccountShadowType> accObject = objectDefinition.parseObjectType(accType);

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

        // WHEN
        AccountShadowType storedObjectType = accObject.asObjectable();
        //AccountShadowType storedObjectType = accObject.getOrParseObjectType();

        // THEN
        System.out.println("storedObjectType:");
        System.out.println(PrismTestUtil.marshalWrap(storedObjectType));
        //assertTrue(accType.equals(storedObjectType));
        assertTrue(accType == storedObjectType);
        assertEquals("JAXB class name doesn't match (2)", AccountShadowType.class, accObject.getCompileTimeClass());

        // GIVEN
        // FIXME
//        accObject.setObjectType(null);
//        assertEquals("JAXB class name doesn't match (3)", AccountShadowType.class, accObject.getCompileTimeClass());

        // WHEN
        AccountShadowType convertedObjectType = accObject.asObjectable();
        //AccountShadowType convertedObjectType = accObject.getOrParseObjectType();

        // THEN
        assertEquals("JAXB class name doesn't match (4)", AccountShadowType.class, accObject.getCompileTimeClass());
        System.out.println("convertedObjectType:");
        System.out.println(PrismTestUtil.marshalWrap(convertedObjectType));
        assertFalse(accType == convertedObjectType);
//		assertTrue(accType.equals(convertedObjectType));

    }

    private void assertProperty(PrismContainer cont, QName propName, Object value) {
        PrismProperty prop = cont.findProperty(propName);
        assertNotNull(propName + " in null", prop);
        assertEquals(propName + " has wrong name", propName, prop.getName());
        assertEquals(propName + " has wrong value", value, prop.getValue().getValue());
    }

}
