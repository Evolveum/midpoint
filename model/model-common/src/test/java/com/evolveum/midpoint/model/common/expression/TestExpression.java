/*
 * Copyright (c) 2013-2015 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.AESProtector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
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
	
    private PrismContext prismContext;

	private long lastScriptExecutionCount;
    private static ExpressionFactory expressionFactory;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
		prismContext = PrismTestUtil.createInitializedPrismContext();
		ObjectResolver resolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
		AESProtector protector = ExpressionTestUtil.createInitializedProtector(prismContext);
    	expressionFactory = ExpressionTestUtil.createInitializedExpressionFactory(resolver, protector, prismContext, null);
	}

    @Test
    public void testIterationCondition() throws Exception {
    	final String TEST_NAME = "testIterationCondition";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// GIVEN
    	OperationResult result = new OperationResult(TestExpression.class.getName()+"."+TEST_NAME);
    	String filename = "iteration-condition.xml";
    	
    	rememberScriptExecutionCount();
    	
    	ExpressionType expressionType = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, filename), ExpressionType.COMPLEX_TYPE);

    	PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinition<Boolean>(ExpressionConstants.OUTPUT_ELMENT_NAME,
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, outputDefinition , TEST_NAME, null, result);
		
		ExpressionVariables variables = new ExpressionVariables();
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-jack.xml"));
		variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, user);
		variables.addVariableDefinition(ExpressionConstants.VAR_USER, user);
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(TEST_DIR, "account-jack-dummy.xml"));
		variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, 1);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, "001");
		
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, TEST_NAME, null, result);
		
		// WHEN
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(expressionContext);
    	
		// THEN
		
		// Make sure that the script is executed only once. There is no delta in the variables, no need to do it twice.
		assertScriptExecutionIncrement(1);
    }
    
    protected void rememberScriptExecutionCount() {
		lastScriptExecutionCount = InternalMonitor.getScriptExecutionCount();
	}

	protected void assertScriptExecutionIncrement(int expectedIncrement) {
		long currentScriptExecutionCount = InternalMonitor.getScriptExecutionCount();
		long actualIncrement = currentScriptExecutionCount - lastScriptExecutionCount;
		assertEquals("Unexpected increment in script execution count", (long)expectedIncrement, actualIncrement);
		lastScriptExecutionCount = currentScriptExecutionCount;
	}
    
}
