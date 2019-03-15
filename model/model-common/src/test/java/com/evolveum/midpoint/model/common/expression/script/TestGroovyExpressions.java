/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressions extends AbstractScriptTest {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
	 */
	@Override
	protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
		return new GroovyScriptEvaluator(prismContext, protector, localizationService);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#getTestDir()
	 */
	@Override
	protected File getTestDir() {
		return new File(BASE_TEST_DIR, "groovy");
	}

	@Test
    public void testExpressionPolyStringEquals101() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals101",
				createVariables(
						NS_X, "foo", "FOO", PrimitiveType.STRING,
						NS_Y, "bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEquals102() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals102",
				createVariables(
						"foo", "FOOBAR", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals111() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals111",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING 
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals112() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals112",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals121() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals121",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals122() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals122",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals201() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals201",
				createVariables(
						"foo", "FOO", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEquals202() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals202",
				createVariables(
						"foo", "FOOBAR", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals211() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals211",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals212() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals212",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals221() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals221",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals222() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals222",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify101() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify101",
				createVariables(
						"foo", "FOO", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify102() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify102",
				createVariables(
						"foo", "FOOBAR", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify111() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify111",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify112() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify112",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify121() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify121",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify122() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify122",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify201() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify201",
				createVariables(
						"foo", "FOO", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify202() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify202",
				createVariables(
						"foo", "FOOBAR", PrimitiveType.STRING,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify211() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify211",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify212() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify212",
				createVariables(
						"foo", PrismTestUtil.createPolyString("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify221() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify221",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOO"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify222() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify222",
				createVariables(
						"foo", PrismTestUtil.createPolyStringType("FOOBAR"), PolyStringType.COMPLEX_TYPE,
						"bar", "BAR", PrimitiveType.STRING
				),
				Boolean.FALSE);
    }

}
