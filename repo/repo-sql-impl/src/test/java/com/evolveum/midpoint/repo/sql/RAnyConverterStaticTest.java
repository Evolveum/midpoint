/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RAnyConverter;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */

@ContextConfiguration(locations = {
        "../../../../../application-context-sql-no-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class RAnyConverterStaticTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverterStaticTest.class);
    private static final String NS_P = "http://example.com/p";
    @Autowired
    PrismContext prismContext;
    @Autowired
    SessionFactory factory;

    @Test
    public void testExtensionInteger() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);


        QName valueName = new QName(NS_P, "intType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);
    }

    @Test
    public void testExtensionLong() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);

        QName valueName = new QName(NS_P, "longType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);
    }

    @Test
    public void testExtensionShort() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);

        QName valueName = new QName(NS_P, "shortType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals(123L, realValue);
    }

    @Test
    public void testExtensionDouble() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);

        QName valueName = new QName(NS_P, "doubleType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("123.1", realValue);
    }

    @Test
    public void testExtensionFloat() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("123.1");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("123.1", realValue);
    }

    @Test
    public void testExtensionString() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, GenericObjectType.class, prismContext);

        QName valueName = new QName(NS_P, "floatType");
        ItemDefinition def = getDefinition(interpreter, valueName);
        AssertJUnit.assertNotNull(def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), valueName);
        value.setTextContent("example");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("example", realValue);
    }

    @Test
    public void testExtensionClob() {
        //todo test
    }

    @Test
    public void testAttributesString() {
        //todo test
    }

    @Test
    public void testUserFullnameString() throws Exception {
        Session session = factory.openSession();
        QueryInterpreter interpreter = new QueryInterpreter(session, UserType.class, prismContext);

        ItemDefinition def = interpreter.findDefinition(null, UserType.F_FULL_NAME);
        AssertJUnit.assertNotNull("Definition not found for " + UserType.F_FULL_NAME, def);

        Element value = DOMUtil.createElement(DOMUtil.getDocument(), UserType.F_FULL_NAME);
        value.setTextContent("john example");

        Object realValue = RAnyConverter.getRealRepoValue(def, value);
        AssertJUnit.assertEquals("john example", realValue);
    }

    private ItemDefinition getDefinition(QueryInterpreter interpreter, QName valueName) {
        Element extension = createExtensionPath();
        return interpreter.findDefinition(extension, valueName);
    }

    private Element createExtensionPath() {
        Document document = DOMUtil.getDocument();
        Element extension = DOMUtil.createElement(document, SchemaConstants.C_PATH);
        extension.setAttributeNS("xmlns", "c", SchemaConstants.NS_COMMON);
        extension.setTextContent("c:extension");

        return extension;
    }
}
