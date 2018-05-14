/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import java.util.Set;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RAnyConverterStaticTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverterStaticTest.class);
    private static final String NS_P = "http://example.com/p";
    private static final String NS_T = PrismConstants.NS_TYPES;
    private static final String NS_FOO_RESOURCE = "http://example.com/foo";

    @Test
    public void testExtensionPolyString() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "polyType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element poly = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        Element orig = DOMUtil.createElement(poly.getOwnerDocument(), new QName(NS_T, "orig"));
        orig.setTextContent("Foo_Bar");
        Element norm = DOMUtil.createElement(poly.getOwnerDocument(), new QName(NS_T, "norm"));
        norm.setTextContent("foo bar");

        poly.appendChild(orig);
        poly.appendChild(norm);

        Object realValue = RAnyConverter.getRealRepoValue(def, poly);
        AssertJUnit.assertEquals(new PolyString("Foo_Bar", "foo bar"), realValue);

        session.close();
    }

    @Test
    public void testExtensionInteger() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "intType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);

        session.close();
    }

    @Test
    public void testExtensionLong() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "longType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);

        session.close();
    }

    @Test
    public void testExtensionShort() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "shortType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);

        session.close();
    }

    @Test
    public void testExtensionDouble() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "doubleType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("123.1", realValue);

        session.close();
    }

    @Test
    public void testExtensionFloat() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("123.1", realValue);

        session.close();
    }

    @Test
    public void testExtensionString() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("example");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("example", realValue);

        session.close();
    }

    @Test
    public void testExtensionEnum() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "enumType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);
        PrismProperty item = (PrismProperty) def.instantiate();
        item.setValue(new PrismPropertyValue(BeforeAfterType.AFTER));
        ((ItemDefinitionImpl) def).setName(valueName);


        RAnyConverter converter = new RAnyConverter(prismContext, extItemDictionary);
        Set<RAnyValue<?>> values = converter.convertToRValue(item, false, null);

        AssertJUnit.assertEquals("Expected only one enum value, but was " + values.size(), 1, values.size());

        RAnyValue value = values.iterator().next();
        AssertJUnit.assertEquals("after", value.getValue());

        session.close();
    }

    @Test
    public void testExtensionDecimal() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "decimalType");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("1234");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("1234", realValue);

        session.close();
    }

    @Test
    public void testExtensionClob() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_P, "locations");
        ItemDefinition def = getDefinition(GenericObjectType.class, new ItemPath(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Document document = DOMUtil.getDocument();
        Element value = DOMUtil.createElement(document, valueName);
        Element location = DOMUtil.createElement(document, new QName(NS_P, "location"));
        value.appendChild(location);
        location.setAttribute("key", "heaven");
        location.setTextContent("somewhere above");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        //asserting simple dom
        document = DOMUtil.parseDocument((String) realValue);
        Element root = document.getDocumentElement();
        AssertJUnit.assertNotNull(root);
        AssertJUnit.assertEquals("locations", root.getLocalName());
        AssertJUnit.assertEquals(NS_P, root.getNamespaceURI());
        AssertJUnit.assertEquals(1, DOMUtil.listChildElements(root).size());

        location = DOMUtil.listChildElements(root).get(0);
        AssertJUnit.assertNotNull(location);
        AssertJUnit.assertEquals("location", location.getLocalName());
        AssertJUnit.assertEquals(NS_P, location.getNamespaceURI());
        AssertJUnit.assertEquals(0, DOMUtil.listChildElements(location).size());
        AssertJUnit.assertEquals("heaven", location.getAttribute("key"));
        AssertJUnit.assertEquals("somewhere above", location.getTextContent());

        session.close();
    }

    @Test
    public void testAttributesString() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_FOO_RESOURCE, "uid");
        ItemDefinition def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:string", "some uid");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("some uid", realValue);

        session.close();
    }

    @Test
    public void testAttributesDouble() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_FOO_RESOURCE, "uid");
        ItemDefinition def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:double", "123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("123.1", realValue);

        session.close();
    }

    private Element createAttributeValue(QName valueName, String xsdType, String textContent) {
        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        value.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type", xsdType);
        value.setTextContent(textContent);

        return value;
    }

    @Test
    public void testAttributesLong() throws Exception {
        Session session = getFactory().openSession();

        QName valueName = new QName(NS_FOO_RESOURCE, "uid");
        ItemDefinition def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:long", "123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);

        session.close();
    }

    @Test
    public void testUserFullnamePolyString() throws Exception {
        Session session = getFactory().openSession();

        ItemDefinition def = getDefinition(UserType.class, UserType.F_FULL_NAME);
        AssertJUnit.assertNotNull("Definition not found for " + UserType.F_FULL_NAME, def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), UserType.F_FULL_NAME);
        Element orig = DOMUtil.createElement(value.getOwnerDocument(), new QName(NS_T, "orig"));
        orig.setTextContent("john example");
        Element norm = DOMUtil.createElement(value.getOwnerDocument(), new QName(NS_T, "norm"));
        norm.setTextContent("john example");
        value.appendChild(orig);
        value.appendChild(norm);

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(new PolyString("john example", "john example"), realValue);

        session.close();
    }

    private <T extends ObjectType> ItemDefinition getDefinition(Class<T> type, QName name) {
        return getDefinition(type, new ItemPath(name));
    }

    private <T extends ObjectType> ItemDefinition getDefinition(Class<T> type, ItemPath path) {
        SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(type);

        return objectDef.findItemDefinition(path);
    }

    private Element createExtensionPath() {
        Document document = DOMUtil.getDocument();
        Element extension = DOMUtil.createElement(document, DeltaConvertor.PATH_ELEMENT_NAME);
        extension.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:c", SchemaConstantsGenerated.NS_COMMON);
        extension.setTextContent("c:extension");

        return extension;
    }
}
