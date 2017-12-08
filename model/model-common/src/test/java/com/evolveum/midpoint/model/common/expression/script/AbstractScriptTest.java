/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemDefinitionImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.common.LocalizationTestUtil;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractScriptTest {

	protected static final QName PROPERTY_NAME = new QName(MidPointConstants.NS_MIDPOINT_TEST_PREFIX, "whatever");
    protected static final String NS_X = "http://example.com/xxx";
    protected static final String NS_Y = "http://example.com/yyy";
	protected static File BASE_TEST_DIR = new File("src/test/resources/expression");
    protected static File OBJECTS_DIR = new File("src/test/resources/objects");
    protected static final String USER_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    public static final Trace LOGGER = TraceManager.getTrace(AbstractScriptTest.class);

    protected ScriptExpressionFactory scriptExpressionfactory;
    protected ScriptEvaluator evaluator;
    protected LocalizationService localizationService;

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @BeforeClass
    public void setupFactory() {
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
    	Protector protector = new ProtectorImpl();
        Collection<FunctionLibrary> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector));
		scriptExpressionfactory = new ScriptExpressionFactory(prismContext, protector);
		scriptExpressionfactory.setObjectResolver(resolver);
		scriptExpressionfactory.setFunctions(functions);
	    localizationService = LocalizationTestUtil.getLocalizationService();
        evaluator = createEvaluator(prismContext, protector);
        String languageUrl = evaluator.getLanguageUrl();
        System.out.println("Expression test for "+evaluator.getLanguageName()+": registering "+evaluator+" with URL "+languageUrl);
        scriptExpressionfactory.registerEvaluator(languageUrl, evaluator);
    }

	protected abstract ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector);

	protected abstract File getTestDir();

	protected boolean supportsRootNode() {
		return false;
	}

	@Test
    public void testExpressionSimple() throws Exception {
		evaluateAndAssertStringScalarExpresssion("expression-simple.xml",
				"testExpressionSimple", null, "foobar");
    }


	@Test
    public void testExpressionStringVariables() throws Exception {
		evaluateAndAssertStringScalarExpresssion(
				"expression-string-variables.xml",
				"testExpressionStringVariables",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOO",
						new QName(NS_Y, "bar"), "BAR"
				),
				"FOOBAR");
    }


    @Test
    public void testExpressionObjectRefVariables() throws Exception {
    	evaluateAndAssertStringScalarExpresssion(
    			"expression-objectref-variables.xml",
    			"testExpressionObjectRefVariables",
    			ExpressionVariables.create(
						new QName(NS_X, "foo"), "Captain",
						new QName(NS_Y, "jack"),
							MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE)
				),
    			"Captain emp1234");
    }

    @Test
    public void testExpressionObjectRefVariablesPolyString() throws Exception {
    	evaluateAndAssertStringScalarExpresssion(
    			"expression-objectref-variables-polystring.xml",
    			"testExpressionObjectRefVariablesPolyString",
    			ExpressionVariables.create(
						new QName(NS_X, "foo"), "Captain",
						new QName(NS_Y, "jack"),
							MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE)
				),
    			"Captain Jack Sparrow");
    }

    // Using similar settings that will be used with mapping and SYSTEM VARIABLES

    @Test
    public void testUserGivenName() throws Exception {
		evaluateAndAssertStringScalarExpresssion(
				"expression-user-given-name.xml",
    			"testUserGivenName",
    			createUserScriptVariables(),
    	    	"Jack");
    }

    @Test
    public void testUserExtensionShip() throws Exception {
		evaluateAndAssertStringScalarExpresssion(
				"expression-user-extension-ship.xml",
    			"testUserExtensionShip",
    			createUserScriptVariables(),
    	    	"Black Pearl");
    }

    @Test
    public void testUserExtensionShipPath() throws Exception {
		evaluateAndAssertStringScalarExpresssion(
				"expression-user-extension-ship-path.xml",
    			"testUserExtensionShipPath",
    			createUserScriptVariables(),
    	    	"Black Pearl");
    }

    @Test
    public void testUserExtensionStringifyFullName() throws Exception {
		evaluateAndAssertStringScalarExpresssion(
				"expression-user-stringify-full-name.xml",
    			"testUserExtensionStringifyFullName",
    			createUserScriptVariables(),
    	    	"Jack Sparrow");
    }

    // TODO: user + multivalue (organizationalUnit)
    // TODO: user + polystring
    // TODO: user + numeric
    // TODO: user + no property value

	private ExpressionVariables createUserScriptVariables() {
		return ExpressionVariables.create(SchemaConstants.C_USER,
    			MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE));
	}

	// TODO: shadow + attributes

	@Test
    public void testRootNode() throws Exception {
    	if (!supportsRootNode()) {
    		return;
    	}

    	evaluateAndAssertStringScalarExpresssion(
				"expression-root-node.xml",
    			"testRootNode",
    			ExpressionVariables.create(null,
    	    			MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE)),
    	    	"Black Pearl");
    }

	@Test
    public void testExpressionList() throws Exception {
		evaluateAndAssertStringListExpresssion(
				"expression-list.xml",
    			"testExpressionList",
    			ExpressionVariables.create(
						new QName(NS_Y, "jack"),
							MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE)
				),
    			"Leaders", "Followers");
    }

	@Test
    public void testExpressionFunc() throws Exception {
		evaluateAndAssertStringScalarExpresssion("expression-func.xml",
    			"testExpressionFunc", null, "gulocka v jamocke");
    }

	@Test
    public void testExpressionFuncConcatName() throws Exception {
		evaluateAndAssertStringScalarExpresssion("expression-func-concatname.xml",
    			"testExpressionFuncConcatName", null, "Horatio Torquemada Marley");
    }
	
	private ScriptExpressionEvaluatorType parseScriptType(String fileName) throws SchemaException, IOException {
		return PrismTestUtil.parseAtomicValue(
                new File(getTestDir(), fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
	}

	private <T> List<PrismPropertyValue<T>> evaluateExpression(ScriptExpressionEvaluatorType scriptType, ItemDefinition outputDefinition,
			ExpressionVariables variables, String shortDesc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ScriptExpression scriptExpression = createScriptExpression(scriptType, outputDefinition, shortDesc);
		List<PrismPropertyValue<T>> resultValues = scriptExpression.evaluate(variables, null, false, shortDesc, null, result);
		if (resultValues != null) {
			for (PrismPropertyValue<T> resultVal: resultValues) {
				if (resultVal.getParent() != null) {
					AssertJUnit.fail("Result value "+resultVal+" from expression "+scriptExpression+" has parent");
				}
			}
		}
		return resultValues;
	}
	
	private ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition, String shortDesc) throws ExpressionSyntaxException {
		ScriptExpression expression = new ScriptExpression(scriptExpressionfactory.getEvaluators().get(expressionType.getLanguage()), expressionType);
		expression.setOutputDefinition(outputDefinition);
		expression.setObjectResolver(scriptExpressionfactory.getObjectResolver());
		expression.setFunctions(scriptExpressionfactory.getFunctions());
		return expression;
	}

	private <T> List<PrismPropertyValue<T>> evaluateExpression(ScriptExpressionEvaluatorType scriptType, QName typeName, boolean scalar,
			ExpressionVariables variables, String shortDesc, OperationResult opResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ItemDefinition outputDefinition = new PrismPropertyDefinitionImpl(PROPERTY_NAME, typeName, PrismTestUtil.getPrismContext());
		if (!scalar) {
			((ItemDefinitionImpl) outputDefinition).setMaxOccurs(-1);
		}
		return evaluateExpression(scriptType, outputDefinition, variables, shortDesc, opResult);
	}

	private <T> PrismPropertyValue<T> evaluateExpressionScalar(ScriptExpressionEvaluatorType scriptType, QName typeName,
			ExpressionVariables variables, String shortDesc, OperationResult opResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		List<PrismPropertyValue<T>> expressionResultList = evaluateExpression(scriptType, typeName, true, variables, shortDesc, opResult);
		return asScalar(expressionResultList, shortDesc);
	}

	private <T> PrismPropertyValue<T> asScalar(List<PrismPropertyValue<T>> expressionResultList, String shortDesc) {
		if (expressionResultList.size() > 1) {
			AssertJUnit.fail("Expression "+shortDesc+" produces a list of "+expressionResultList.size()+" while only expected a single value: "+expressionResultList);
		}
		if (expressionResultList.isEmpty()) {
			return null;
		}
		return expressionResultList.iterator().next();
	}

	protected void evaluateAndAssertStringScalarExpresssion(String fileName, String testName, ExpressionVariables variables, String expectedValue) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		List<PrismPropertyValue<String>> expressionResultList = evaluateStringExpresssion(fileName, testName, variables, true);
		PrismPropertyValue<String> expressionResult = asScalar(expressionResultList, testName);
		assertNotNull("Expression "+testName+" resulted in null value (expected '"+expectedValue+"')", expressionResult);
		assertEquals("Expression "+testName+" resulted in wrong value", expectedValue, expressionResult.getValue());
	}

	private void evaluateAndAssertStringListExpresssion(String fileName, String testName, ExpressionVariables variables, String... expectedValues) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		List<PrismPropertyValue<String>> expressionResultList = evaluateStringExpresssion(fileName, testName, variables, true);
		TestUtil.assertSetEquals("Expression "+testName+" resulted in wrong values", PrismPropertyValue.getValues(expressionResultList), expectedValues);
	}
	protected void evaluateAndAssertBooleanScalarExpresssion(String fileName, String testName, ExpressionVariables variables, Boolean expectedValue) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		List<PrismPropertyValue<Boolean>> expressionResultList = evaluateBooleanExpresssion(fileName, testName, variables, true);
		PrismPropertyValue<Boolean> expressionResult = asScalar(expressionResultList, testName);
		assertNotNull("Expression "+testName+" resulted in null value (expected '"+expectedValue+"')", expressionResult);
		assertEquals("Expression "+testName+" resulted in wrong value", expectedValue, expressionResult.getValue());
	}

	private List<PrismPropertyValue<String>> evaluateStringExpresssion(String fileName, String testName, ExpressionVariables variables, boolean scalar) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		displayTestTitle(testName);
		ScriptExpressionEvaluatorType scriptType = parseScriptType(fileName);
        OperationResult opResult = new OperationResult(testName);

        return evaluateExpression(scriptType, DOMUtil.XSD_STRING, true, variables, testName, opResult);
	}

	private List<PrismPropertyValue<Boolean>> evaluateBooleanExpresssion(String fileName, String testName, ExpressionVariables variables, boolean scalar) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
		displayTestTitle(testName);
		ScriptExpressionEvaluatorType scriptType = parseScriptType(fileName);
        OperationResult opResult = new OperationResult(testName);
        

        return evaluateExpression(scriptType, DOMUtil.XSD_BOOLEAN, true, variables, testName, opResult);
	}


	private void displayTestTitle(String testName) {
		System.out.println("===[ "+evaluator.getLanguageName()+": "+testName+" ]===========================");
		LOGGER.info("===[ "+evaluator.getLanguageName()+": "+testName+" ]===========================");
	}

}
