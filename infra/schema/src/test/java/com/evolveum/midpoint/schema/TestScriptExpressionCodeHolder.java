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

package com.evolveum.midpoint.schema;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.ExpressionCodeHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class TestScriptExpressionCodeHolder {

    private static final String FILENAME_EXPRESSION_1 = "src/test/resources/expression/expression-1.xml";
    private static final String FILENAME_EXPRESSION_EXPLICIT_NS = "src/test/resources/expression/expression-explicit-ns.xml";

    public TestScriptExpressionCodeHolder() {
    }

    @Test
    public void basicExpressionHolderTest() throws FileNotFoundException, JAXBException {

        File file = new File(FILENAME_EXPRESSION_1);
        FileInputStream fis = new FileInputStream(file);

        Unmarshaller u = null;

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        u = jc.createUnmarshaller();

        Object object = u.unmarshal(fis);

        @SuppressWarnings("rawtypes")
		ScriptExpressionEvaluatorType expressionType = (ScriptExpressionEvaluatorType) ((JAXBElement) object).getValue();

        ExpressionCodeHolder ex = new ExpressionCodeHolder(expressionType.getCode());

        AssertJUnit.assertEquals("$c:user/c:extension/foo:something/bar:somethingElse", ex.getExpressionAsString().trim());
        
        Map<String, String> namespaceMap = ex.getNamespaceMap();

        for(String key : namespaceMap.keySet()) {
            String uri = namespaceMap.get(key);
            System.out.println(key+" : "+uri);
        }

        AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/samples/piracy", namespaceMap.get("piracy"));
        AssertJUnit.assertEquals(SchemaConstants.NS_C, namespaceMap.get(""));

    }

    @Test
    public void explicitNsTest() throws FileNotFoundException, JAXBException {

        File file = new File(FILENAME_EXPRESSION_EXPLICIT_NS);
        FileInputStream fis = new FileInputStream(file);

        Unmarshaller u = null;

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        u = jc.createUnmarshaller();

        Object object = u.unmarshal(fis);

		@SuppressWarnings("rawtypes")
		ScriptExpressionEvaluatorType expressionType = (ScriptExpressionEvaluatorType) ((JAXBElement) object).getValue();

        ExpressionCodeHolder ex = new ExpressionCodeHolder(expressionType.getCode());

        AssertJUnit.assertEquals("$c:user/c:extension/foo:something/bar:somethingElse", ex.getExpressionAsString().trim());

        Map<String, String> namespaceMap = ex.getNamespaceMap();

        for(String key : namespaceMap.keySet()) {
            String uri = namespaceMap.get(key);
            System.out.println(key+" : "+uri);
        }

        AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/samples/piracy", namespaceMap.get("piracy"));
        AssertJUnit.assertEquals("http://midpoint.evolveum.com/xml/ns/samples/bar", namespaceMap.get("bar"));
        AssertJUnit.assertEquals(SchemaConstants.NS_C, namespaceMap.get(""));

    }


}