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

import com.evolveum.midpoint.common.expression.jsr223.Jsr223ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractExpressionTest {

    protected static File BASE_TEST_DIR = new File("src/test/resources/expression");
    protected static File OBJECTS_DIR = new File("src/test/resources/objects");

    protected ExpressionFactory factory;
    protected ExpressionEvaluator evaluator;
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @BeforeClass
    public void setupFactory() {
        factory = new ExpressionFactory();
        evaluator = createEvaluator(PrismTestUtil.getPrismContext());
        String languageUrl = evaluator.getLanguageUrl();
        System.out.println("Expression test for "+evaluator.getLanguageName()+": registering "+evaluator+" with URL "+languageUrl);
        factory.registerEvaluator(languageUrl, evaluator);
        ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
        factory.setObjectResolver(resolver);
    }

	protected abstract ExpressionEvaluator createEvaluator(PrismContext prismContext);
	
	protected abstract File getTestDir();
	
	protected boolean supportsRootNode() {
		return false;
	}
	
	@Test
    public void testExpressionSimple() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
		displayTestTitle("testExpressionSimple");
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-simple.xml"), ExpressionType.class);
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionSimple");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "simple thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("foobar", result.getValue());
    }


	@Test
    public void testExpressionStringVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-string-variables.xml"), ExpressionType.class);
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionStringVariables");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "string variable thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("FOOBAR", result.getValue());
    }


    @Test
    public void testExpressionObjectRefVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-objectref-variables.xml"), ExpressionType.class);
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionObjectRefVariables");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "objectref variable thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("Captain jack", result.getValue());
    }

    @Test
    public void testExpressionObjectRefVariablesPolyString() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-objectref-variables-polystring.xml"), ExpressionType.class);
        ExpressionType expressionType = expressionTypeElement.getValue();

        OperationResult opResult = new OperationResult("testExpressionObjectRefVariablesPolyString");

        // WHEN
        Expression expression = factory.createExpression(expressionType, "objectref variable polystring thing");
        PrismPropertyValue<String> result = expression.evaluateScalar(String.class, opResult);

        // THEN
        assertEquals("Captain Jack Sparrow", result.getValue());
    }

    @Test
    public void testSystemVariables() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-system-variables.xml"), ExpressionType.class);
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
    public void testRootNode() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
    	if (!supportsRootNode()) {
    		return;
    	}
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-root-node.xml"), ExpressionType.class);
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
    public void testExpressionList() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException {
        // GIVEN
        JAXBElement<ExpressionType> expressionTypeElement = PrismTestUtil.unmarshalElement(
                new File(getTestDir(), "expression-list.xml"), ExpressionType.class);
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

    
	private void displayTestTitle(String testName) {
		System.out.println("===[ "+evaluator.getLanguageName()+": "+testName+" ]===========================");
	}

}
