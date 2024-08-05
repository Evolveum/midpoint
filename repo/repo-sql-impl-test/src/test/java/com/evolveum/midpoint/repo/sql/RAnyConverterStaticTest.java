/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import jakarta.persistence.EntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BeforeAfterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RAnyConverterStaticTest extends BaseSQLRepoTest {

    private static final String NS_P = "http://example.com/p";
    private static final String NS_T = PrismConstants.NS_TYPES;
    private static final String NS_FOO_RESOURCE = "http://example.com/foo";

    @Test
    public void testExtensionPolyString() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "polyType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element poly = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        Element orig = DOMUtil.createElement(poly.getOwnerDocument(), new QName(NS_T, "orig"));
        orig.setTextContent("Foo_Bar");
        Element norm = DOMUtil.createElement(poly.getOwnerDocument(), new QName(NS_T, "norm"));
        norm.setTextContent("foo bar");

        poly.appendChild(orig);
        poly.appendChild(norm);

        Object realValue = RAnyConverter.getRealRepoValue(def, poly, prismContext);
        AssertJUnit.assertEquals(new PolyString("Foo_Bar", "foo bar"), realValue);

        em.close();
    }

    @Test
    public void testExtensionInteger() throws Exception {
        EntityManager em = getFactory().createEntityManager();

        QName valueName = new QName(NS_P, "intType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals(123L, realValue);

        em.close();
    }

    @Test
    public void testExtensionLong() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "longType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals(123L, realValue);

        em.close();
    }

    @Test
    public void testExtensionShort() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "shortType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals(123L, realValue);

        em.close();
    }

    @Test
    public void testExtensionDouble() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "doubleType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("123.1", realValue);

        em.close();
    }

    @Test
    public void testExtensionFloat() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("123.1", realValue);

        em.close();
    }

    @Test
    public void testExtensionString() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("example");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("example", realValue);

        em.close();
    }

    @Test
    public void testExtensionEnum() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "enumType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);
        PrismProperty item = (PrismProperty) def.instantiate();
        item.setRealValue(BeforeAfterType.AFTER);

        RAnyConverter converter = new RAnyConverter(prismContext, extItemDictionary);
        Set<RAnyValue<?>> values;
        try {
            values = converter.convertToRValue(item, false, RObjectExtensionType.EXTENSION);
            AssertJUnit.fail("Should have throw serialization related exception after creating ext item");
        } catch (RestartOperationRequestedException ex) {   // this is a new way
            System.out.println("Got expected exception: " + ex);
        } catch (DtoTranslationException ex) {              // this was an old way
            AssertJUnit.assertEquals("Wrong exception class", RestartOperationRequestedException.class, ex.getCause().getClass());
        }

        values = converter.convertToRValue(item, false, RObjectExtensionType.EXTENSION);

        AssertJUnit.assertEquals("Expected only one enum value, but was " + values.size(), 1, values.size());

        RAnyValue value = values.iterator().next();
        AssertJUnit.assertEquals("after", value.getValue());

        em.close();
    }

    @Test
    public void testExtensionDecimal() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "decimalType");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("1234");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("1234", realValue);

        em.close();
    }

    @Test
    public void testExtensionClob() throws Exception {
        EntityManager em = factory.createEntityManager();

        QName valueName = new QName(NS_P, "locations");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, ItemPath.create(ObjectType.F_EXTENSION, valueName));
        AssertJUnit.assertNotNull(def);

        Document document = DOMUtil.getDocument();
        Element value = DOMUtil.createElement(document, valueName);
        Element location = DOMUtil.createElement(document, new QName(NS_P, "location"));
        value.appendChild(location);
        location.setAttribute("key", "heaven");
        location.setTextContent("somewhere above");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
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

        em.close();
    }

    @Test
    public void testAttributesString() throws Exception {
        EntityManager em = factory.createEntityManager();

        ItemName valueName = new ItemName(NS_FOO_RESOURCE, "uid");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:string", "some uid");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("some uid", realValue);

        em.close();
    }

    @Test
    public void testAttributesDouble() throws Exception {
        EntityManager em = factory.createEntityManager();

        ItemName valueName = new ItemName(NS_FOO_RESOURCE, "uid");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:double", "123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals("123.1", realValue);

        em.close();
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
        EntityManager em = factory.createEntityManager();

        ItemName valueName = new ItemName(NS_FOO_RESOURCE, "uid");
        ItemDefinition<?> def = getDefinition(GenericObjectType.class, valueName);
        AssertJUnit.assertNull(def);

        Element value = createAttributeValue(valueName, "xsd:long", "123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals(123L, realValue);

        em.close();
    }

    @Test
    public void testUserFullnamePolyString() throws Exception {
        EntityManager em = factory.createEntityManager();

        ItemDefinition<?> def = getDefinition(UserType.class, UserType.F_FULL_NAME);
        AssertJUnit.assertNotNull("Definition not found for " + UserType.F_FULL_NAME, def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), UserType.F_FULL_NAME);
        Element orig = DOMUtil.createElement(value.getOwnerDocument(), new QName(NS_T, "orig"));
        orig.setTextContent("john example");
        Element norm = DOMUtil.createElement(value.getOwnerDocument(), new QName(NS_T, "norm"));
        norm.setTextContent("john example");
        value.appendChild(orig);
        value.appendChild(norm);

        Object realValue = RAnyConverter.getRealRepoValue(def, value, prismContext);
        AssertJUnit.assertEquals(new PolyString("john example", "john example"), realValue);

        em.close();
    }

    private <T extends ObjectType> ItemDefinition<?> getDefinition(Class<T> type, ItemPath path) {
        SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismObjectDefinition objectDef = registry.findObjectDefinitionByCompileTimeClass(type);

        return objectDef.findItemDefinition(path);
    }
}
