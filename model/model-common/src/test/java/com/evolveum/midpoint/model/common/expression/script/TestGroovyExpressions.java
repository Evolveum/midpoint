/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 */
public class TestGroovyExpressions extends AbstractScriptTest {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
	 */
	@Override
	protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
		return new Jsr223ScriptEvaluator("groovy", prismContext, protector);
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
				Boolean.FALSE);
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
				Boolean.FALSE);
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
				Boolean.FALSE);
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
				Boolean.FALSE);
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
