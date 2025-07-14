/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class TestParseGenericObject extends AbstractSchemaTest {

    private static final QName EXTENSION_TYPE_QNAME = new QName(NS_MODEL_EXT, "GenericObjectExtensionType");
    public static final File GENERIC_FILE = new File("src/test/resources/common/generic-sample-configuration.xml");

    @Test
    public void testParseGenericFile() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        // WHEN
        PrismObject<GenericObjectType> generic = prismContext.parserFor(GENERIC_FILE).xml().parse();

        // THEN
        System.out.println("Parsed generic object:");
        System.out.println(generic.debugDump());

        assertGenericObject(generic);
    }

    @Test
    public void testParseGenericDom() throws SchemaException, DatatypeConfigurationException {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        Document document = DOMUtil.parseFile(GENERIC_FILE);
        Element resourceElement = DOMUtil.getFirstChildElement(document);

        // WHEN
        PrismObject<GenericObjectType> generic = prismContext.parserFor(resourceElement).parse();

        // THEN
        System.out.println("Parsed generic object:");
        System.out.println(generic.debugDump());

        assertGenericObject(generic);
    }

    @Test
    public void testParseGenericRoundtrip() throws Exception {
        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<GenericObjectType> generic = prismContext.parseObject(GENERIC_FILE);

        System.out.println("Parsed generic object:");
        System.out.println(generic.debugDump());

        assertGenericObject(generic);

        // SERIALIZE

        String serializedGeneric = prismContext.xmlSerializer().serialize(generic);

        System.out.println("serialized generic object:");
        System.out.println(serializedGeneric);

        // RE-PARSE

        PrismObject<GenericObjectType> reparsedGeneric = prismContext.parseObject(serializedGeneric);

        System.out.println("Re-parsed generic object:");
        System.out.println(reparsedGeneric.debugDump());

        assertGenericObject(generic);

        ObjectDelta<GenericObjectType> objectDelta = generic.diff(reparsedGeneric);
        System.out.println("Delta:");
        System.out.println(objectDelta.debugDump());
        assertTrue("Delta is not empty", objectDelta.isEmpty());

        PrismAsserts.assertEquivalent("generic object re-parsed quivalence", generic, reparsedGeneric);
    }

    private void assertGenericObject(PrismObject<GenericObjectType> generic) throws DatatypeConfigurationException {
        generic.checkConsistence();

        assertEquals("Wrong oid", "c0c010c0-d34d-b33f-f00d-999111111111", generic.getOid());
        PrismObjectDefinition<GenericObjectType> resourceDefinition = generic.getDefinition();
        assertNotNull("No resource definition", resourceDefinition);
        PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "genericObject"),
                GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        assertEquals("Wrong class in resource", GenericObjectType.class, generic.getCompileTimeClass());
        GenericObjectType genericType = generic.asObjectable();
        assertNotNull("asObjectable resulted in null", genericType);

        assertPropertyValue(generic, "name", PrismTestUtil.createPolyString("My Sample Config Object"));
        assertPropertyDefinition(generic, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        PrismContainer<?> extensionContainer = generic.findContainer(GenericObjectType.F_EXTENSION);
        assertContainerDefinition(extensionContainer, "extension", EXTENSION_TYPE_QNAME, 0, 1);
        PrismContainerDefinition<?> extensionContainerDefinition = extensionContainer.getDefinition();
        //assertTrue("Extension container definition is NOT dynamic", extensionContainerDefinition.isDynamic());
        PrismContainerValue<?> extensionContainerValue = extensionContainer.getValue();
        Collection<Item<?, ?>> extensionItems = extensionContainerValue.getItems();
        assertEquals("Wrong number of extension items", 5, extensionItems.size());

        PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_STRING_TYPE_ELEMENT, "X marks the spot");
        PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_INT_TYPE_ELEMENT, 1234);
        PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_DOUBLE_TYPE_ELEMENT, 456.789D);
        PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_LONG_TYPE_ELEMENT, 567890L);
        XMLGregorianCalendar calendar = XmlTypeConverter.createXMLGregorianCalendar("2002-05-30T09:10:11");
        PrismAsserts.assertPropertyValue(extensionContainerValue, SchemaTestConstants.EXTENSION_DATE_TYPE_ELEMENT, calendar);

    }

    private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
            int maxOccurs) {
        QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(SchemaConstantsGenerated.NS_COMMON, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

    private void assertContainerDefinition(PrismContainer<?> container,
            String contName, QName xsdType, int minOccurs, int maxOccurs) {
        QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
        PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
    }
}
