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
package com.evolveum.midpoint.common.expression;

import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 */
public class TestXPathExpressions {

    private static File TEST_DIR = new File("src/test/resources/expression");
    private static File OBJECTS_DIR = new File("src/test/resources/objects");

    private ExpressionFactory factory;

    @BeforeClass
    public void setupFactory() {
        factory = new ExpressionFactory();
        XPathExpressionEvaluator xpathEvaluator = new XPathExpressionEvaluator();
        factory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        factory.setObjectResolver(resolver);
    }

    @Test
    public void testExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-simple.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionSimple");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "simple thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("foobar", result.getValue());
    }

    @Test
    public void testExpressionStringVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-string-variables.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionStringVariables");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "string variable thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("FOOBAR", result.getValue());
    }


    @Test
    public void testExpressionObjectRefVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-objectref-variables.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionObjectRefVariables");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "objectref variable thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue());
    }


    @Test
    public void testSystemVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-system-variables.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testSystemVariables");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "system variable thing");

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        expression.addVariableDefinition(SchemaConstants.I_USER, ref);

        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("Jack", result.getValue());
    }

    @Test
    public void testRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-root-node.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testRootNode");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "root node thing");

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid("c0c010c0-d34d-b33f-f00d-111111111111");
        ref.setType(SchemaConstants.I_USER_TYPE);
        expression.setRootNode(ref);

        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("Black Pearl", result.getValue());
    }

    @Test
    public void testExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = (JAXBElement) PrismTestUtil.unmarshalElement(
                new File(TEST_DIR, "expression-list.xml"));
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionList");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "list thing");
        List<PrismPropertyValue<String>> results = expression.evaluateList(String.class, opResult);

        // THEN
        List<String> expected = new ArrayList<String>();
        expected.add("Leaders");
        expected.add("Followers");

        assertEquals(expected.size(), results.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), results.get(i).getValue());
        }
    }

}
