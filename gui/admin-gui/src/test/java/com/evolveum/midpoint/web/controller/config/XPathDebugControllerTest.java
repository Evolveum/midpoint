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

package com.evolveum.midpoint.web.controller.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.evolveum.midpoint.web.bean.XPathVariableBean;

/**
 * 
 * @author Katuska
 */

@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-init.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:application-context-test.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class XPathDebugControllerTest extends AbstractTestNGSpringContextTests {

	@Autowired
	private XPathDebugController xpathController;

	private void setAttributes() {
		xpathController.cleanupController();
		String expression = "declare namespace x='http://xxx.com/'; concat($x:foo,' ',$x:bar)";
		xpathController.setExpression(expression);

		xpathController.addVariablePerformed();
		XPathVariableBean variable1 = xpathController.getVariables().get(0);
		variable1.setType("String");
		variable1.setValue("salala");
		variable1.setVariableName("x:foo");

		xpathController.addVariablePerformed();
		XPathVariableBean variable2 = xpathController.getVariables().get(0);
		variable2.setType("String");
		variable2.setValue("tralala");
		variable2.setVariableName("x:bar");

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
