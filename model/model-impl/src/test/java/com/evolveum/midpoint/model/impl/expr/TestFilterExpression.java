/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFilterExpression extends AbstractInternalModelIntegrationTest {

    private static final String TEST_DIR = "src/test/resources/expr";

    @Autowired
    private ExpressionFactory expressionFactory;

    @Autowired
    private TaskManager taskManager;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test100EvaluateExpressionEmployeeTypeUndefinedFilter() throws Exception {
        // GIVEN
        Task task = getTestTask();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-undefined-filter.xml",
                null, UndefinedFilter.class, task, task.getResult());

        executeFilter(filter, 5, task, task.getResult());
    }

    @Test
    public void test110EvaluateExpressionEmployeeTypeNoneFilter() throws Exception {
        // GIVEN
        Task task = getTestTask();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-none-filter.xml",
                null, NoneFilter.class, task, task.getResult());

        executeFilter(filter, 0, task, task.getResult());
    }

    @Test
    public void test120EvaluateExpressionEmployeeTypeAllFilter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-all-filter.xml",
                null, AllFilter.class, task, result);

        executeFilter(filter, 5, task, result);
    }

    @Test
    public void test130EvaluateExpressionEmployeeTypeError() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            evaluateExpressionAssertFilter("expression-employeeType-error.xml",
                    null, NoneFilter.class, task, result);
            AssertJUnit.fail("Unexpected success");
        } catch (ExpressionEvaluationException e) {
            // this is expected
            assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("evaluated to no value"));
        }
    }


    @Test
    public void test140EvaluateExpressionEmployeeTypeEmptyFilter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-empty-filter.xml",
                null, EqualFilter.class, task, result);

        EqualFilter equalFilter = (EqualFilter) filter;
        AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());

        executeFilter(filter, 4, task, result);
    }

    @Test
    public void test150EvaluateExpressionEmployeeTypeDefaultsNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-filter-defaults.xml",
                null, EqualFilter.class, task, result);

        EqualFilter equalFilter = (EqualFilter) filter;
        AssertJUnit.assertNull("Expected NO values in filter, but found " + equalFilter.getValues(), equalFilter.getValues());

        executeFilter(filter, 4, task, result);
    }

    @Test
    public void test152EvaluateExpressionEmployeeTypeDefaultsCaptain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-employeeType-filter-defaults.xml",
                "CAPTAIN", EqualFilter.class, task, result);

        EqualFilter equalFilter = (EqualFilter) filter;
        PrismAsserts.assertValues("Wrong values in filter", equalFilter.getValues(), "CAPTAIN");

        executeFilter(filter, 1, task, result);
    }


    @Test
    public void test200EvaluateExpressionLinkRefDefaultsNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-linkref-filter-defaults.xml",
                null, RefFilter.class, task, result);

        RefFilter refFilter = (RefFilter) filter;
        AssertJUnit.assertNull("Expected NO values in filter, but found " + refFilter.getValues(), refFilter.getValues());

        executeFilter(filter, 2, task, result);
    }

    @Test
    public void test202EvaluateExpressionLinkRefObjectReferenceTypeDefaultsNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-linkref-object-reference-type-filter-defaults.xml",
                null, RefFilter.class, task, result);

        RefFilter refFilter = (RefFilter) filter;
        AssertJUnit.assertNull("Expected NO values in filter, but found " + refFilter.getValues(), refFilter.getValues());

        executeFilter(filter, 2, task, result);
    }

    @Test
    public void test210EvaluateExpressionLinkRefDefaultsVal() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-linkref-filter-defaults.xml",
                ACCOUNT_SHADOW_GUYBRUSH_OID, RefFilter.class, task, result);

        RefFilter refFilter = (RefFilter) filter;
        assertEquals("Wrong number of values in filter: " + refFilter.getValues(), 1, refFilter.getValues().size());

        executeFilter(filter, 1, task, result);
    }

    @Test
    public void test212EvaluateExpressionLinkRefObjectReferenceTypeDefaultsVal() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectFilter filter = evaluateExpressionAssertFilter("expression-linkref-object-reference-type-filter-defaults.xml",
                ACCOUNT_SHADOW_GUYBRUSH_OID, RefFilter.class, task, result);

        RefFilter refFilter = (RefFilter) filter;
        assertEquals("Wrong number of values in filter: " + refFilter.getValues(), 1, refFilter.getValues().size());

        executeFilter(filter, 1, task, result);
    }


    private ObjectFilter evaluateExpressionAssertFilter(String filename,
            String input, Class<? extends ObjectFilter> expectedType,
            Task task, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        SearchFilterType filterType = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, filename), SearchFilterType.COMPLEX_TYPE);

        ObjectFilter filter = prismContext.getQueryConverter().createObjectFilter(UserType.class, filterType);

        PrismPropertyValue<String> pval = null;
        if (input != null) {
            pval = prismContext.itemFactory().createPropertyValue(input);
        }

        ExpressionVariables variables = createVariables(
                ExpressionConstants.VAR_INPUT, pval, PrimitiveType.STRING);

        // WHEN
        ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables,
                MiscSchemaUtil.getExpressionProfile(), expressionFactory, prismContext,
                "evaluating filter with null value not allowed", task, result);

        // THEN
        display("Evaluated filter", evaluatedFilter);
        AssertJUnit.assertTrue("Expression should be evaluated to "+expectedType+", but was "+evaluatedFilter, expectedType.isAssignableFrom(evaluatedFilter.getClass()));

        return evaluatedFilter;
    }

    private void executeFilter(ObjectFilter filter, int expectedNumberOfResults, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFactory().createQuery(filter);
        SearchResultList<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
        display("Found objects", objects);
        assertEquals("Wrong number of results (found: "+objects+")", expectedNumberOfResults, objects.size());
    }

}
