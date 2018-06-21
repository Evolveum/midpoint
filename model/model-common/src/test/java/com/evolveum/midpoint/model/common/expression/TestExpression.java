/*
 * Copyright (c) 2013-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.*;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
public class TestExpression {

	private static final File TEST_DIR = new File("src/test/resources/expression/expression");

	private static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");

	private static final File ACCOUNT_JACK_DUMMYFILE = new File(TEST_DIR, "account-jack-dummy.xml");

	private static final File EXPRESSION_ITERATION_CONDITION_FILE = new File(TEST_DIR, "iteration-condition.xml");

    private PrismContext prismContext;

	private long lastScriptExecutionCount;
    private static ExpressionFactory expressionFactory;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);

		prismContext = PrismTestUtil.createInitializedPrismContext();
		ObjectResolver resolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
		ProtectorImpl protector = ExpressionTestUtil.createInitializedProtector(prismContext);
		expressionFactory = ExpressionTestUtil.createInitializedExpressionFactory(resolver, protector, prismContext, null, null);
	}

	@Test
    public void testIterationCondition() throws Exception {
    	final String TEST_NAME = "testIterationCondition";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
    	OperationResult result = new OperationResult(TestExpression.class.getName()+"."+TEST_NAME);

    	rememberScriptExecutionCount();

    	ExpressionType expressionType = PrismTestUtil.parseAtomicValue(
    			EXPRESSION_ITERATION_CONDITION_FILE, ExpressionType.COMPLEX_TYPE);

    	PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, outputDefinition , TEST_NAME, null, result);

		ExpressionVariables variables = new ExpressionVariables();
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
		variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, user);
		variables.addVariableDefinition(ExpressionConstants.VAR_USER, user);
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMYFILE);
		variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, 1);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, "001");

		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, TEST_NAME, null, result);

		// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(expressionContext);

		// THEN
		assertNotNull(outputTriple);
		outputTriple.checkConsistence();

		// Make sure that the script is executed only once. There is no delta in the variables, no need to do it twice.
		assertScriptExecutionIncrement(1);
    }

    protected void rememberScriptExecutionCount() {
		lastScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
	}

	protected void assertScriptExecutionIncrement(int expectedIncrement) {
		long currentScriptExecutionCount = InternalMonitor.getCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
		long actualIncrement = currentScriptExecutionCount - lastScriptExecutionCount;
		assertEquals("Unexpected increment in script execution count", (long)expectedIncrement, actualIncrement);
		lastScriptExecutionCount = currentScriptExecutionCount;
	}

}
