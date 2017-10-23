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
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
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
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 */
public class TestScriptCaching {

	private static final File TEST_DIR = new File("src/test/resources/expression/groovy");
	protected static File OBJECTS_DIR = new File("src/test/resources/objects");

	private static final QName PROPERTY_NAME = new QName(MidPointConstants.NS_MIDPOINT_TEST_PREFIX, "whatever");
	private static final String NS_WHATEVER = "http://whatever/xml/ns";

	 protected ScriptExpressionFactory scriptExpressionfactory;
	 protected ScriptEvaluator evaluator;
	 
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @BeforeClass
    public void setupFactory() {
    	System.out.println("Setting up expression factory and evaluator");
    	PrismContext prismContext = PrismTestUtil.getPrismContext();
    	ObjectResolver resolver = new DirectoryFileObjectResolver(OBJECTS_DIR);
    	Protector protector = new ProtectorImpl();
        Collection<FunctionLibrary> functions = new ArrayList<FunctionLibrary>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector));
		scriptExpressionfactory = new ScriptExpressionFactory(prismContext, protector);
		scriptExpressionfactory.setObjectResolver(resolver);
		scriptExpressionfactory.setFunctions(functions);
        evaluator = new Jsr223ScriptEvaluator("groovy", prismContext, protector);
        String languageUrl = evaluator.getLanguageUrl();
        scriptExpressionfactory.registerEvaluator(languageUrl, evaluator);
    }

    @Test
    public void testGetExtensionPropertyValue() throws Exception {
    	final String TEST_NAME = "testGetExtensionPropertyValue";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
    	InternalMonitor.reset();

    	assertScriptMonitor(0,0, "init");

    	// WHEN, THEN
    	long etimeFirst = executeScript("expression-string-variables.xml", "FOOBAR", "first");
    	assertScriptMonitor(1,1, "first");

    	long etimeSecond = executeScript("expression-string-variables.xml", "FOOBAR", "second");
    	assertScriptMonitor(1,2, "second");
    	assertTrue("Einstein was wrong! "+etimeFirst+" -> "+etimeSecond, etimeSecond <= etimeFirst);

    	long etimeThird = executeScript("expression-string-variables.xml", "FOOBAR", "second");
    	assertScriptMonitor(1,3, "third");
    	assertTrue("Einstein was wrong again! "+etimeFirst+" -> "+etimeThird, etimeThird <= etimeFirst);

    	// Different script. Should compile.
    	long horatio1Time = executeScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio");
    	assertScriptMonitor(2,4, "horatio");

    	// Same script. No compilation.
    	long etimeFourth = executeScript("expression-string-variables.xml", "FOOBAR", "fourth");
    	assertScriptMonitor(2,5, "fourth");
    	assertTrue("Einstein was wrong all the time! "+etimeFirst+" -> "+etimeFourth, etimeFourth <= etimeFirst);

    	// Try this again. No compile.
    	long horatio2Time = executeScript("expression-func-concatname.xml", "Horatio Torquemada Marley", "horatio2");
    	assertScriptMonitor(2,6, "horatio2");
    	assertTrue("Even Horatio was wrong! "+horatio1Time+" -> "+horatio2Time, horatio2Time <= horatio1Time);
    }

    private void assertScriptMonitor(int expCompilations, int expExecutions, String desc) {
		assertEquals("Unexpected number of script compilations after "+desc, expCompilations, InternalMonitor.getCount(InternalCounters.SCRIPT_COMPILE_COUNT));
		assertEquals("Unexpected number of script executions after "+desc, expExecutions, InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT));
	}

	private long executeScript(String filname, String expectedResult, String desc) throws SchemaException, IOException, JAXBException, ExpressionEvaluationException, ObjectNotFoundException {
        // GIVEN
    	OperationResult result = new OperationResult(desc);
    	ScriptExpressionEvaluatorType scriptType = parseScriptType(filname);
    	ItemDefinition outputDefinition = new PrismPropertyDefinitionImpl(PROPERTY_NAME, DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

    	ScriptExpression scriptExpression = createScriptExpression(scriptType, outputDefinition, desc);

        ExpressionVariables variables = ExpressionVariables.create(
				new QName(NS_WHATEVER, "foo"), "FOO",
				new QName(NS_WHATEVER, "bar"), "BAR"
		);

		// WHEN
        long startTime = System.currentTimeMillis();
    	List<PrismPropertyValue<String>> scripResults = scriptExpression.evaluate(variables , null, false, desc, null, result);
    	long endTime = System.currentTimeMillis();

        // THEN
    	System.out.println("Script results "+desc+", etime: "+(endTime - startTime)+" ms");
    	System.out.println(scripResults);

    	String scriptResult = asScalarString(scripResults);
    	assertEquals("Wrong script "+desc+" result", expectedResult, scriptResult);

    	return (endTime - startTime);
    }

	private ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition, String shortDesc) throws ExpressionSyntaxException {
		ScriptExpression expression = new ScriptExpression(scriptExpressionfactory.getEvaluators().get(expressionType.getLanguage()), expressionType);
		expression.setOutputDefinition(outputDefinition);
		expression.setObjectResolver(scriptExpressionfactory.getObjectResolver());
		expression.setFunctions(scriptExpressionfactory.getFunctions());
		return expression;
	}
	
    private ScriptExpressionEvaluatorType parseScriptType(String fileName) throws SchemaException, IOException, JAXBException {
		ScriptExpressionEvaluatorType expressionType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, fileName), ScriptExpressionEvaluatorType.COMPLEX_TYPE);
		return expressionType;
	}

    private String asScalarString(List<PrismPropertyValue<String>> expressionResultList) {
		if (expressionResultList.size() > 1) {
			AssertJUnit.fail("Expression produces a list of "+expressionResultList.size()+" while only expected a single value: "+expressionResultList);
		}
		if (expressionResultList.isEmpty()) {
			return null;
		}
		return expressionResultList.iterator().next().getValue();
	}

}
