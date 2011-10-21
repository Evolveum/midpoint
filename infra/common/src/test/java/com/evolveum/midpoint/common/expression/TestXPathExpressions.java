/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import static org.testng.AssertJUnit.assertEquals;
import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;

/**
 * @author Radovan Semancik
 *
 */
public class TestXPathExpressions {

	private static File TEST_DIR = new File("src/test/resources/expression");
	
	private ExpressionFactory factory;
	
	@BeforeClass
	public void setupFactory() {
		factory = new ExpressionFactory();
		XPathExpressionEvaluator xpathEvaluator = new XPathExpressionEvaluator();
		factory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
	}
	
	@Test
	public void testExpressionSimple() throws JAXBException, ExpressionEvaluationException {
		// GIVEN
		JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement<ExpressionType>) JAXBUtil.unmarshal(
				new File(TEST_DIR, "expression-simple.xml"));
		ExpressionType expressionType = expressionTypeElement.getValue();
		
		// WHEN
		Expression expression = factory.createExpression(expressionType, "simple thing");
		String result = expression.evaluate(String.class);
		
		// THEN
		assertEquals("foobar",result);
	}

	@Test
	public void testExpressionStringVariables() throws JAXBException, ExpressionEvaluationException {
		// GIVEN
		JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement<ExpressionType>) JAXBUtil.unmarshal(
				new File(TEST_DIR, "expression-string-variables.xml"));
		ExpressionType expressionType = expressionTypeElement.getValue();
		
		// WHEN
		Expression expression = factory.createExpression(expressionType, "string variable thing");
		String result = expression.evaluate(String.class);
		
		// THEN
		assertEquals("FOOBAR",result);
	}

	
	
}
