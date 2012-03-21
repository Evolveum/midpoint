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

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertPropertyValue;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class TestJaxbParsing {

    private static final String TEST_COMMON_DIR = "src/test/resources/common/";
    private static final String NS_FOO = "http://www.example.com/foo";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testParseUserFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        UserType userType = PrismTestUtil.unmarshalObject(new File(TEST_COMMON_DIR, "user-jack.xml"), UserType.class);

        // WHEN

        PrismObject<UserType> user = userType.asPrismObject();
        user.revive(prismContext);

        // THEN
        System.out.println("Parsed user:");
        System.out.println(user.dump());

        assertPropertyValue(user, SchemaConstants.C_NAME, "jack");
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "fullName"), "Jack Sparrow");
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "givenName"), "Jack");
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "familyName"), "Sparrow");
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "honorificPrefix"), "Cpt.");
        assertPropertyValue(user.findContainer(SchemaConstants.C_EXTENSION),
                new QName(NS_FOO, "bar"), "BAR");
        PrismProperty<ProtectedStringType> password = user.findOrCreateContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "password"));
        assertNotNull(password);
        // TODO: check inside
        assertPropertyValue(user.findOrCreateContainer(SchemaConstants.C_EXTENSION),
                new QName(NS_FOO, "num"), 42);
        PrismProperty<?> multi = user.findOrCreateContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "multi"));
        assertEquals(3, multi.getValues().size());

        // WHEN

//        Node domNode = user.serializeToDom();
//
//        //THEN
//        System.out.println("\nSerialized user:");
//        System.out.println(DOMUtil.serializeDOMToString(domNode));
//
//        Element userEl = DOMUtil.getFirstChildElement(domNode);
//        assertEquals(SchemaConstants.I_USER, DOMUtil.getQName(userEl));

        // TODO: more asserts
    }

    @Test(enabled = true)
    public void testParseAccountFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        AccountShadowType accType = PrismTestUtil.unmarshalObject(new File(TEST_COMMON_DIR, "account-jack.xml"), AccountShadowType.class);

        PrismObject<AccountShadowType> account = accType.asPrismObject();
        account.revive(prismContext);

        System.out.println("Parsed account:");
        System.out.println(account.dump());

        assertPropertyValue(account, SchemaConstants.C_NAME, "jack");
        assertPropertyValue(account, AccountShadowType.F_ACCOUNT_TYPE, "user");

        // TODO: more asserts
    }

    @Test
    public void testParseGenericObjectFromJaxb() throws Exception {
        System.out.println("\n\n ===[ testParseGenericObjectFromJaxb ]===\n");

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        GenericObjectType object = PrismTestUtil.unmarshalObject(new File(TEST_COMMON_DIR, "generic-sample-configuration.xml"),
                GenericObjectType.class);

        PrismObject<GenericObjectType> prism = object.asPrismObject();
        prism.revive(prismContext);

        assertPropertyValue(prism, GenericObjectType.F_NAME, "My Sample Config Object");
        assertPropertyValue(prism, GenericObjectType.F_DESCRIPTION, "Sample description");
        assertPropertyValue(prism, GenericObjectType.F_OBJECT_TYPE, "http://midpoint.evolveum.com/xml/ns/test/extension#SampleConfigType");
        //assert extension
        PrismContainer<?> extension = prism.findContainer(GenericObjectType.F_EXTENSION);
        assertNotNull(extension);
        
        PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_STRING_TYPE_ELEMENT, "X marks the spot");
		PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT, 1234);
		PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_DOUBLE_TYPE_ELEMENT, 456.789D);
		PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT, 567890L);
		XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar("2002-05-30T09:10:11");
		PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_DATE_TYPE_ELEMENT, calendar);
        
        //todo locations ????? how to test DOM ??????
    }
    
}
