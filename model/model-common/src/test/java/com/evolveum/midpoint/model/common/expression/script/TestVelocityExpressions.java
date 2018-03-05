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

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.velocity.VelocityScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author Radovan Semancik
 * @author Pavol Mederly
 */
public class TestVelocityExpressions extends AbstractScriptTest {

	@Override
	protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
		return new VelocityScriptEvaluator(prismContext, protector);
	}

	@Override
	protected File getTestDir() {
		return new File(BASE_TEST_DIR, "velocity");
	}

	@Override
    @Test
	public void testExpressionList() throws Exception {
		evaluateAndAssertStringScalarExpresssion(		// velocity has no support for output other than String
				"expression-list.xml",
				"testExpressionList",
				ExpressionVariables.create(
						new QName(NS_Y, "jack"),
						MiscSchemaUtil.createObjectReference(USER_OID, UserType.COMPLEX_TYPE)
				),
				"[Leaders, Followers]");
	}

	@Test
    public void testExpressionPolyStringEquals101() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals101",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOO",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEquals102() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals102",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOOBAR",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals111() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals111",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);			// velocity calls '==' on toString value
    }

	@Test
    public void testExpressionPolyStringEquals112() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals112",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals121() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals121",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);			// velocity calls '==' on toString value
    }

	@Test
    public void testExpressionPolyStringEquals122() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-1.xml",
				"testExpressionPolyStringEquals122",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals201() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals201",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOO",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEquals202() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals202",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOOBAR",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals211() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals211",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);			// velocity calls '==' on toString value
    }

	@Test
    public void testExpressionPolyStringEquals212() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals212",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEquals221() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals221",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);			// velocity calls '==' on toString value
    }

	@Test
    public void testExpressionPolyStringEquals222() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-2.xml",
				"testExpressionPolyStringEquals222",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify101() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify101",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOO",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify102() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify102",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOOBAR",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify111() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify111",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify112() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify112",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify121() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify121",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify122() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-1.xml",
				"testExpressionPolyStringEqualsStringify122",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify201() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify201",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOO",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify202() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify202",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), "FOOBAR",
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify211() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify211",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify212() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify212",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyString("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify221() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify221",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOO"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.TRUE);
    }

	@Test
    public void testExpressionPolyStringEqualsStringify222() throws Exception {
		evaluateAndAssertBooleanScalarExpresssion(
				"expression-polystring-equals-stringify-2.xml",
				"testExpressionPolyStringEqualsStringify222",
				ExpressionVariables.create(
						new QName(NS_X, "foo"), PrismTestUtil.createPolyStringType("FOOBAR"),
						new QName(NS_Y, "bar"), "BAR"
				),
				Boolean.FALSE);
    }

}
