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

/**
 *
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.Schema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class TestSchemaProcessorWithRegistry {

    private static final String TEST_DIR = "src/test/resources/schema-registry/";
    private static final String NS_FOO = "http://www.example.com/foo";

//    @Test
//    public void testParseUserFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {
//
//        SchemaRegistry reg = new SchemaRegistry();
//        reg.initialize();
//        Schema commonSchema = reg.getObjectSchema();
//        assertNotNull(commonSchema);
//
//        // Try to use the schema to validate Jack
//        JAXBElement<UserType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "user-jack.xml"), UserType.class);
//        UserType userType = jaxbElement.getValue();
//
//        PrismObjectDefinition<UserType> userDefinition = commonSchema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
//        assertNotNull("UserType definition not found in parsed schema", userDefinition);
//
//        // WHEN
//
//        PrismObject<UserType> user = userDefinition.parseObjectType(userType);
//
//        // THEN
//        System.out.println("Parsed user:");
//        System.out.println(user.dump());
//
//        assertProperty(user, SchemaConstants.C_NAME, "jack");
//        assertProperty(user, new QName(SchemaConstants.NS_C, "fullName"), "Cpt. Jack Sparrow");
//        assertProperty(user, new QName(SchemaConstants.NS_C, "givenName"), "Jack");
//        assertProperty(user, new QName(SchemaConstants.NS_C, "familyName"), "Sparrow");
//        assertProperty(user, new QName(SchemaConstants.NS_C, "honorificPrefix"), "Cpt.");
//        assertProperty(user.findOrCreateContainer(SchemaConstants.C_EXTENSION),
//                new QName(NS_FOO, "bar"), "BAR");
//        PrismProperty password = user.findOrCreateContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "password"));
//        assertNotNull(password);
//        // TODO: check inside
//        assertProperty(user.findOrCreateContainer(SchemaConstants.C_EXTENSION),
//                new QName(NS_FOO, "num"), 42);
//        PrismProperty multi = user.findOrCreateContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "multi"));
//        assertEquals(3, multi.getValues().size());
//
//        // WHEN
//
//        Node domNode = user.serializeToDom();
//
//        //THEN
//        System.out.println("\nSerialized user:");
//        System.out.println(DOMUtil.serializeDOMToString(domNode));
//
//        Element userEl = DOMUtil.getFirstChildElement(domNode);
//        assertEquals(SchemaConstants.I_USER, DOMUtil.getQName(userEl));
//
//        // TODO: more asserts
//    }
//
//    @Test(enabled = true)
//    public void testParseAccountFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {
//
//        SchemaRegistry reg = new SchemaRegistry();
//        reg.initialize();
//        Schema commonSchema = reg.getObjectSchema();
//        assertNotNull(commonSchema);
//
//        // Try to use the schema to validate Jack
//        JAXBElement<AccountShadowType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "account-jack.xml"), AccountShadowType.class);
//        AccountShadowType accType = jaxbElement.getValue();
//
//        PrismObjectDefinition<AccountShadowType> accDefinition = commonSchema.findObjectDefinition(ObjectTypes.ACCOUNT, AccountShadowType.class);
//        assertNotNull("account definition not found in parsed schema", accDefinition);
//
//        PrismObject<AccountShadowType> account = accDefinition.parseObjectType(accType);
//
//        System.out.println("Parsed account:");
//        System.out.println(account.dump());
//    }
//
//    private void assertProperty(PrismContainer cont, QName propName, Object value) {
//        PrismProperty prop = cont.findProperty(propName);
//        assertNotNull(propName + " in null", prop);
//        assertEquals(propName + " has wrong name", propName, prop.getName());
//        assertEquals(propName + " has wrong value", value, prop.getValue().getValue());
//    }

}
