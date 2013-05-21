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