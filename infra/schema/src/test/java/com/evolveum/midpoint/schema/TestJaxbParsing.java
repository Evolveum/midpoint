/*
 * Copyright (c) 2010-2015 Evolveum
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

/**
 *
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertPropertyValue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class TestJaxbParsing {

    private static final String NS_FOO = "http://www.example.com/foo";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testParseUserFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        UserType userType = PrismTestUtil.parseObjectable(new File(TestConstants.COMMON_DIR, "user-jack.xml"), UserType.class);

        // WHEN

        PrismObject<UserType> user = userType.asPrismObject();
        user.revive(prismContext);

        // THEN
        System.out.println("Parsed user:");
        System.out.println(user.debugDump());

        user.checkConsistence();
        assertPropertyValue(user, UserType.F_NAME, PrismTestUtil.createPolyString("jack"));
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "fullName"), new PolyString("Jack Sparrow", "jack sparrow"));
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "givenName"), new PolyString("Jack", "jack"));
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "familyName"), new PolyString("Sparrow", "sparrow"));
        assertPropertyValue(user, new QName(SchemaConstants.NS_C, "honorificPrefix"), new PolyString("Cpt.", "cpt"));
        assertPropertyValue(user.findContainer(UserType.F_EXTENSION),
                new QName(NS_FOO, "bar"), "BAR");
        PrismProperty<ProtectedStringType> password = user.findOrCreateContainer(UserType.F_EXTENSION).findProperty(new QName(NS_FOO, "password"));
        assertNotNull(password);
        // TODO: check inside
        assertPropertyValue(user.findOrCreateContainer(UserType.F_EXTENSION),
                new QName(NS_FOO, "num"), 42);
        PrismProperty<?> multi = user.findOrCreateContainer(UserType.F_EXTENSION).findProperty(new QName(NS_FOO, "multi"));
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

    @Test
    public void testParseAccountFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        ShadowType accType = PrismTestUtil.parseObjectable(new File(TestConstants.COMMON_DIR, "account-jack.xml"), ShadowType.class);

        PrismObject<ShadowType> account = accType.asPrismObject();
        account.revive(prismContext);

        System.out.println("Parsed account:");
        System.out.println(account.debugDump());

        account.checkConsistence();
        assertPropertyValue(account, ShadowType.F_NAME, PrismTestUtil.createPolyString("jack"));
        assertPropertyValue(account, ShadowType.F_OBJECT_CLASS,
        		new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff", "AccountObjectClass"));
        assertPropertyValue(account, ShadowType.F_INTENT, "default");

        // TODO: more asserts
    }

    @Test
    public void testParseExpressionFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        RoleType roleType = PrismTestUtil.parseObjectable(new File(TestConstants.COMMON_DIR, "role.xml"), RoleType.class);

        // WHEN

        PrismObject<RoleType> role = roleType.asPrismObject();
        role.revive(prismContext);

        // THEN
        System.out.println("Parsed role:");
        System.out.println(role.debugDump());

        role.checkConsistence();
        assertPropertyValue(role, RoleType.F_NAME, PrismTestUtil.createPolyString("r3"));
        PrismAsserts.assertEquals("Wrong number of approver expressions", 1, role.asObjectable().getApproverExpression().size());
        Object o = role.asObjectable().getApproverExpression().get(0).getExpressionEvaluator().get(0).getValue();
        PrismAsserts.assertEquals("Invalid evaluator type", ScriptExpressionEvaluatorType.class, o.getClass());
        String code = ((ScriptExpressionEvaluatorType) o).getCode();
        PrismAsserts.assertEquals("Incorrect code parsed", "midpoint.oid2ort(user.getOid())", code);
    }

    @Test
    public void testParseGenericObjectFromJaxb() throws Exception {
        System.out.println("\n\n ===[ testParseGenericObjectFromJaxb ]===\n");

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        GenericObjectType object = PrismTestUtil.parseObjectable(new File(TestConstants.COMMON_DIR, "generic-sample-configuration.xml"),
                GenericObjectType.class);

        PrismObject<GenericObjectType> prism = object.asPrismObject();
        prism.revive(prismContext);

        prism.checkConsistence();
        assertPropertyValue(prism, GenericObjectType.F_NAME, PrismTestUtil.createPolyString("My Sample Config Object"));
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

    @Test
    public void testMarshallObjectDeltaType() throws Exception {
        ObjectDeltaType delta = new ObjectDeltaType();
        delta.setOid("07b32c14-0c18-460b-bd4a-99b96699f952");
        delta.setChangeType(ChangeTypeType.MODIFY);

        ItemDeltaType item1 = new ItemDeltaType();
        delta.getItemDelta().add(item1);
        item1.setModificationType(ModificationTypeType.REPLACE);
        Document document = DOMUtil.getDocument();
//        Element path = document.createElementNS(SchemaConstantsGenerated.NS_TYPES, "path");
//        path.setTextContent("c:credentials/c:password");
        ItemPath path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD);
        item1.setPath(new ItemPathType(path));
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setEncryptedData(new EncryptedDataType());
        RawType value = new RawType(((PrismContextImpl) PrismTestUtil.getPrismContext()).getBeanMarshaller().marshall(protectedString), PrismTestUtil.getPrismContext());
        item1.getValue().add(value);

        String xml = PrismTestUtil.serializeJaxbElementToString(
                new JAXBElement<Object>(new QName("http://www.example.com", "custom"), Object.class, delta));
        assertNotNull(xml);
    }

    @Test
    public void testParseAnyValue() throws Exception {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN

        String dataAsIs = "<asIs/>";
        String dataValue = "<c:value xmlns:c='" + SchemaConstants.NS_C + "'>12345</c:value>";

        // THEN

        JAXBElement oAsIs = prismContext.parserFor(dataAsIs).xml().parseRealValueToJaxbElement();
        System.out.println(dumpResult(dataAsIs, oAsIs));
        assertJaxbElement(oAsIs, new QName("asIs"), AsIsExpressionEvaluatorType.class);

        JAXBElement oValue = prismContext.parserFor(dataValue).xml().parseRealValueToJaxbElement();
        System.out.println(dumpResult(dataValue, oValue));
        //assertJaxbElement(oValue, SchemaConstantsGenerated.C_VALUE, String.class);
        assertJaxbElement(oValue, SchemaConstantsGenerated.C_VALUE, RawType.class);
    }

    private void assertJaxbElement(JAXBElement jaxbElement, QName name, Class<?> clazz) {
        assertEquals("Wrong JAXB element name", name, jaxbElement.getName());
        assertEquals("Wrong JAXB element declared type", clazz, jaxbElement.getDeclaredType());
        assertEquals("Wrong JAXB element value type", clazz, jaxbElement.getValue().getClass());
    }

    private String dumpResult(String data, JAXBElement jaxb) {
        return "Parsed expression evaluator: "  + data + " as " + jaxb + " (name=" + jaxb.getName() + ", declaredType=" + jaxb.getDeclaredType() + ", value=" + jaxb.getValue() + ")";
    }

    @Test
    public void testParseValueFilterWithAny() throws Exception {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        Document doc = DOMUtil.parseFile(new File(TestConstants.COMMON_DIR, "value-filter-with-any.xml"));
        Element rootElement = DOMUtil.getFirstChildElement(doc);

        // WHEN

        Object parsedObject = prismContext.parserFor(rootElement).parseRealValue();

        // THEN
        System.out.println("Parsed object: "  + parsedObject);
    }

}
