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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.web.bean.XPathVariableBean;
import com.evolveum.midpoint.web.controller.XPathDebugController;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Katuska
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/application-context-webapp.xml", "file:src/main/webapp/WEB-INF/application-context-security.xml", "classpath:applicationContext-test.xml"})
public class XPathDebugControllerTest {

    @Autowired
    XPathDebugController xpathController;

    public XPathDebugControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private void setAttributes() {
        xpathController.prepareXpathDebugPage();
        String expression = "declare namespace x='http://xxx.com/'; concat($x:foo,' ',$x:bar)";
        xpathController.setExpression(expression);

        XPathVariableBean variable1 = new XPathVariableBean();
        variable1.setType("String");
        variable1.setValue("salala");
        variable1.setVariableName("x:foo");
        xpathController.setVariable1(variable1);
        XPathVariableBean variable2 = new XPathVariableBean();
        variable2.setType("String");
        variable2.setValue("tralala");
        variable2.setVariableName("x:bar");
        xpathController.setVariable2(variable2);

        xpathController.setReturnType("String");
    }

    @Test
    public void testEvaluate() throws Exception {
        setAttributes();
        String result = xpathController.evaluate();
        System.out.println("result: " + result);
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
