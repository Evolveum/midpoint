package com.evolveum.midpoint.model.impl.expr;

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
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFilterExpression extends AbstractModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/expr";
  
	 @Autowired(required=true)
		private ExpressionFactory expressionFactory;
	
	@Autowired(required = true)
    private TaskManager taskManager;
	
  
	 @BeforeSuite
		public void setup() throws SchemaException, SAXException, IOException {
			PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
			PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		}
	 
	
  @Test
  public void testEvaluateExpressionToNoneFilter() throws Exception {
  	final String TEST_NAME = "testEvaluateExpressionToNoneFilter";
  	TestUtil.displayTestTile(TEST_NAME);
  	PrismContext prismContext = PrismTestUtil.getPrismContext();
  	// GIVEN
  	OperationResult result = new OperationResult(TestFilterExpression.class.getName()+"."+TEST_NAME);
  	Task task = taskManager.createTaskInstance(TEST_NAME);
  	
  	String filename = "expression-to-none-filter.xml";
  	
  	SearchFilterType filterType = PrismTestUtil.parseAtomicValue(
              new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);
  	
  	ObjectFilter filter = QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext);
  	
  	Map<QName, Object> params = new HashMap<>();
  	params.put(UserType.F_NAME, null);
  	
  	ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(params);
  	
  	ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext, "evaluating filter with null value not allowed", task, result);

  	AssertJUnit.assertTrue("Expression should be evaluated to NoneFilter", (evaluatedFilter instanceof NoneFilter));
  }
	
	@Test
  public void testEvaluateExpressionToAllFilter() throws Exception {
  	final String TEST_NAME = "testEvaluateExpressionToAllFilter";
  	TestUtil.displayTestTile(TEST_NAME);
  	PrismContext prismContext = PrismTestUtil.getPrismContext();
  	// GIVEN
  	OperationResult result = new OperationResult(TestFilterExpression.class.getName()+"."+TEST_NAME);
  	Task task = taskManager.createTaskInstance(TEST_NAME);
  	
  	String filename = "expression-to-all-filter.xml";
  	
  	SearchFilterType filterType = PrismTestUtil.parseAtomicValue(
              new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);
  	
  	ObjectFilter filter = QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext);
  	
  	Map<QName, Object> params = new HashMap<>();
  	params.put(UserType.F_NAME, null);
  	
  	ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(params);
  	
  	ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext, "evaluating filter with null value not allowed", task, result);

  	AssertJUnit.assertTrue("Expression should be evaluated to AllFilter", (evaluatedFilter instanceof AllFilter));
  }
	
	@Test
  public void testEvaluateExpressionToEmptyFilter() throws Exception {
  	final String TEST_NAME = "testEvaluateExpressionToEmptyFilter";
  	TestUtil.displayTestTile(TEST_NAME);
  	
  	// GIVEN
  	OperationResult result = new OperationResult(TestFilterExpression.class.getName()+"."+TEST_NAME);
  	Task task = taskManager.createTaskInstance(TEST_NAME);
  	PrismContext prismContext = PrismTestUtil.getPrismContext();
  	String filename = "expression-to-empty-filter.xml";
  	
  	SearchFilterType filterType = PrismTestUtil.parseAtomicValue(
              new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);
  	
  	ObjectFilter filter = QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext);
  	
  	Map<QName, Object> params = new HashMap<>();
  	params.put(UserType.F_NAME, null);
  	
  	ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(params);
  	
  	ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, expressionFactory, prismContext, "evaluating filter with null value not allowed", task, result);

  	AssertJUnit.assertTrue("Expression should be evaluated to EqualFilter " + evaluatedFilter, (evaluatedFilter instanceof EqualFilter));
  	
  	EqualFilter equalFilter = (EqualFilter) evaluatedFilter;
  	AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());
  	
  }
}
