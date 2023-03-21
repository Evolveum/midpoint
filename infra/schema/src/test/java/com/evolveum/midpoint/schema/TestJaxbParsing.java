/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertPropertyValue;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * @author Radovan Semancik
 */
public class TestJaxbParsing extends AbstractSchemaTest {

    private static final String NS_FOO = "http://www.example.com/foo";

    @Test
    public void testParseUserFromJaxb() throws SchemaException, IOException {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        UserType userType = PrismTestUtil.parseObjectable(
                new File(TestConstants.COMMON_DIR, "user-jack.xml"));

        // WHEN

        PrismObject<UserType> user = userType.asPrismObject();
        user.revive(prismContext);

        // THEN
        System.out.println("Parsed user:");
        System.out.println(user.debugDump());

        user.checkConsistence();
        assertPropertyValue(user, UserType.F_NAME, PolyString.fromOrig("jack"));
        assertPropertyValue(user, new ItemName(SchemaConstants.NS_C, "fullName"), new PolyString("Jack Sparrow", "jack sparrow"));
        assertPropertyValue(user, new ItemName(SchemaConstants.NS_C, "givenName"), new PolyString("Jack", "jack"));
        assertPropertyValue(user, new ItemName(SchemaConstants.NS_C, "familyName"), new PolyString("Sparrow", "sparrow"));
        assertPropertyValue(user, new ItemName(SchemaConstants.NS_C, "honorificPrefix"), new PolyString("Cpt.", "cpt"));
        assertPropertyValue(user.findContainer(UserType.F_EXTENSION),
                new ItemName(NS_FOO, "bar"), "BAR");
        PrismProperty<ProtectedStringType> password = user.findOrCreateContainer(UserType.F_EXTENSION).findProperty(new ItemName(NS_FOO, "password"));
        assertNotNull(password);
        // TODO: check inside
        assertPropertyValue(user.findOrCreateContainer(UserType.F_EXTENSION),
                new ItemName(NS_FOO, "num"), 42);
        PrismProperty<?> multi = user.findOrCreateContainer(UserType.F_EXTENSION).findProperty(new ItemName(NS_FOO, "multi"));
        assertEquals(3, multi.getValues().size());

        // TODO: more asserts
    }

    @Test
    public void testParseAccountFromJaxb() throws SchemaException, IOException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // Try to use the schema to validate Jack
        ShadowType accType = PrismTestUtil.parseObjectable(new File(TestConstants.COMMON_DIR, "account-jack.xml"));

        PrismObject<ShadowType> account = accType.asPrismObject();
        account.revive(prismContext);

        System.out.println("Parsed account:");
        System.out.println(account.debugDump(1));

        account.checkConsistence();
        assertPropertyValue(account, ShadowType.F_NAME, PolyString.fromOrig("jack"));
        assertPropertyValue(account, ShadowType.F_OBJECT_CLASS, RI_ACCOUNT_OBJECT_CLASS);
        assertPropertyValue(account, ShadowType.F_INTENT, "default");

        // TODO: more asserts
    }

    @Test
    public void testParseModernRoleFromJaxb() throws SchemaException, IOException {
        testParseRoleFromJaxb(new File(TestConstants.COMMON_DIR, "role.xml"));
    }

    /**
     * Test of parsing role with elements that were removed in 4.0.
     */
    @Test
    public void testParseLegacyRoleFromJaxb() throws SchemaException, IOException {
        PrismParser parser = PrismTestUtil.getPrismContext()
                .parserFor(new File(TestConstants.COMMON_DIR, "role-legacy.xml"))
                .compat();

        // WHEN
        PrismObject<RoleType> role = parser.parse();

        // THEN
        System.out.println("Parsed role:");
        System.out.println(role.debugDump(1));

        role.checkConsistence();
        assertPropertyValue(role, RoleType.F_NAME, PolyString.fromOrig("r3"));
    }

    public void testParseRoleFromJaxb(File file) throws SchemaException, IOException {
        PrismParser parser = PrismTestUtil.getPrismContext().parserFor(file);

        // WHEN
        PrismObject<RoleType> role = parser.parse();

        // THEN
        System.out.println("Parsed role:");
        System.out.println(role.debugDump(1));

        role.checkConsistence();
        assertPropertyValue(role, RoleType.F_NAME, PolyString.fromOrig("r3"));

        // TODO: more asserts?
    }

    @Test
    public void testParseGenericObjectFromJaxb() throws Exception {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        GenericObjectType object = PrismTestUtil.parseObjectable(
                new File(TestConstants.COMMON_DIR, "generic-sample-configuration.xml"));

        PrismObject<GenericObjectType> prism = object.asPrismObject();
        prism.revive(prismContext);

        prism.checkConsistence();
        assertPropertyValue(prism, GenericObjectType.F_NAME, PolyString.fromOrig("My Sample Config Object"));
        assertPropertyValue(prism, GenericObjectType.F_DESCRIPTION, "Sample description");
        //assert extension
        PrismContainer<?> extension = prism.findContainer(GenericObjectType.F_EXTENSION);
        assertNotNull(extension);

        PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_STRING_TYPE_ELEMENT, "X marks the spot");
        PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT, 1234);
        PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_DOUBLE_TYPE_ELEMENT, 456.789D);
        PrismAsserts.assertPropertyValue(extension, SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT, 567890L);
        XMLGregorianCalendar calendar = XmlTypeConverter.createXMLGregorianCalendar("2002-05-30T09:10:11");
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
        ItemPath path = ItemPath.create(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD);
        item1.setPath(new ItemPathType(path));
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setEncryptedData(new EncryptedDataType());
        RawType value = new RawType(((PrismContextImpl) PrismTestUtil.getPrismContext()).getBeanMarshaller().marshall(protectedString).frozen(), PrismTestUtil.getPrismContext());
        item1.getValue().add(value);

        String xml = PrismTestUtil.serializeJaxbElementToString(
                new JAXBElement<>(new QName("http://www.example.com", "custom"), Object.class, delta));
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
        assertJaxbElement(oValue, SchemaConstantsGenerated.C_VALUE, RawType.class);
    }

    private void assertJaxbElement(JAXBElement jaxbElement, QName name, Class<?> clazz) {
        assertEquals("Wrong JAXB element name", name, jaxbElement.getName());
        assertEquals("Wrong JAXB element declared type", clazz, jaxbElement.getDeclaredType());
        assertEquals("Wrong JAXB element value type", clazz, jaxbElement.getValue().getClass());
    }

    private String dumpResult(String data, JAXBElement jaxb) {
        return "Parsed expression evaluator: " + data + " as " + jaxb + " (name=" + jaxb.getName()
                + ", declaredType=" + jaxb.getDeclaredType() + ", value=" + jaxb.getValue() + ")";
    }
}
