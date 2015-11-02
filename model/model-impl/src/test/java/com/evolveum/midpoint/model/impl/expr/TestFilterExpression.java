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

package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFilterExpression extends AbstractModelIntegrationTest {

	private static final String TEST_DIR = "src/test/resources/expr";

	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testEvaluateExpressionToUndefinedFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToUndefinedFilter";
		evaluateExpressionAssertFilter(TEST_NAME, "expression-to-undefined-filter.xml", UndefinedFilter.class);
	}
	
	@Test
	public void testEvaluateExpressionToNoneFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToNoneFilter";
		evaluateExpressionAssertFilter(TEST_NAME, "expression-to-none-filter.xml", NoneFilter.class);
	}

	@Test
	public void testEvaluateExpressionToAllFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToAllFilter";
		evaluateExpressionAssertFilter(TEST_NAME, "expression-to-all-filter.xml", AllFilter.class);
	}

	@Test
	public void testEvaluateExpressionToError() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToError";
		try {
			evaluateExpressionAssertFilter(TEST_NAME, "expression-to-error.xml", NoneFilter.class);
			AssertJUnit.fail("Unexpected success");
		} catch (ExpressionEvaluationException e) {
			// this is expected
			assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("evaluated to no value"));
		}
	}

	
	@Test
	public void testEvaluateExpressionToEmptyFilter() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionToEmptyFilter";
		
		ObjectFilter evaluatedFilter = evaluateExpressionAssertFilter(TEST_NAME, "expression-to-empty-filter.xml", EqualFilter.class);
		
		EqualFilter equalFilter = (EqualFilter) evaluatedFilter;
		AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());
	}

	@Test
	public void testEvaluateExpressionDefaults() throws Exception {
		final String TEST_NAME = "testEvaluateExpressionDefaults";
		
		ObjectFilter evaluatedFilter = evaluateExpressionAssertFilter(TEST_NAME, "expression-filter-defaults.xml", EqualFilter.class);

		EqualFilter equalFilter = (EqualFilter) evaluatedFilter;
		AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());
	}
	
	public ObjectFilter evaluateExpressionAssertFilter(final String TEST_NAME, String filename, Class<? extends ObjectFilter> expectedType) throws SchemaException, IOException, ObjectNotFoundException, ExpressionEvaluationException {
		TestUtil.displayTestTile(TEST_NAME);
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// GIVEN
		OperationResult result = new OperationResult(TestFilterExpression.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);

		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);

		ObjectFilter filter = QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext);

		Map<QName, Object> params = new HashMap<>();
		params.put(UserType.F_NAME, null);

		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(params);

		// WHEN
		ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext,
				"evaluating filter with null value not allowed", task, result);

		// THEN
		AssertJUnit.assertTrue("Expression should be evaluated to "+expectedType+", but was "+evaluatedFilter, expectedType.isAssignableFrom(evaluatedFilter.getClass()));
		
		return evaluatedFilter;
	}

}
