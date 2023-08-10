/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.query.PreparedQuery;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestQueryExpression extends AbstractInternalModelIntegrationTest {

    private static final String TEST_DIR = "src/test/resources/expr";
    @Autowired MidpointFunctions midpoint;

    @Autowired
    private ExpressionFactory expressionFactory;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test100EvaluateAxiomGivenNameModel() throws Exception {
        Task task = getTestTask();
        var result = createOperationResult();
        var query = TypedQuery.parse(UserType.class, "givenName = 'Elaine'");
        var objects = modelService.searchObjects(query, task, result);
        assertEquals("Only one user should be found", 1, objects.size());
        assertEquals("User should be Elaine",userTypeElaine.getOid(),objects.get(0).getOid());
    }

    @Test
    public void test101EvaluatePreparedAxiomGivenNameModel() throws Exception {
        Task task = getTestTask();
        var result = createOperationResult();
        var prepared = PreparedQuery.parse(UserType.class, "givenName = ?");

        // Return read only result
        prepared.operationOptionsBuilder().readOnly();

        var query = prepared.bind(new PolyString("Elaine"));
        var objects = modelService.searchObjects(query, task, result);
        assertEquals("Only one user should be found", 1, objects.size());
        var only = objects.get(0);
        assertEquals("User should be Elaine",userTypeElaine.getOid(),only.getOid());

        // FIXME: Investigate why readOnly option does not work
        //assertTrue("Object should be frozen", userTypeElaine.isImmutable());
    }

    @Test
    public void test101EvaluatePreparedOrderingAndPagingModel() throws Exception {
        Task task = getTestTask();
        var result = createOperationResult();
        var prepared = PreparedQuery.parse(UserType.class, "assignment/construction/resourceRef/@/name =  ?");
        prepared.bind(new PolyString("Dummy Resource"));
        prepared.operationOptionsBuilder().distinct();
        // Search all
        var query = prepared.toTypedQuery();
        var list = modelService.searchObjects(prepared.toTypedQuery(),task, result);
        assertEquals("Wrong number of results", 2, list.size());
        // Default order is barbarossa, elaine

        // order should be elaine, barbarossa
        prepared.orderBy(UserType.F_NAME, OrderDirection.DESCENDING)
                .maxSize(1);
        list = modelService.searchObjects(prepared.build(), task, result);
        assertEquals("Wrong number of results", 1, list.size());
        assertEquals("User should be Elaine", userTypeElaine.getOid(), list.get(0).getOid());

        // Lets change offset on result query (and verify prepared is unchanged
        list = modelService.searchObjects(prepared.build().offset(1), task, result);

        assertEquals("Wrong number of results", 1, list.size());
        assertEquals("User should be Barbarossa", userTypeBarbossa.getOid(), list.get(0).getOid());
        assertEquals("Offset in prepared query should be null", null, prepared.getOffset());
    }

    @Test
    public void test102CompleteExampleWithPagingModel() throws Exception {
        Task task = getTestTask();
        var result = createOperationResult();
        var typedQuery = PreparedQuery.parse(UserType.class, "assignment/construction/resourceRef/@/name =  ?")
                .orderBy(UserType.F_NAME, OrderDirection.DESCENDING)
                .maxSize(1)
                .offset(1)
                .bindValue(new PolyString("Dummy Resource"))
                .build();

        // Alternative with separate value bindings


        // Lets change offset on result query (and verify prepared is unchanged
        var list = modelService.searchObjects(typedQuery, task, result);
        assertEquals("Wrong number of results", 1, list.size());
        assertEquals("User should be Barbarossa", userTypeBarbossa.getOid(), list.get(0).getOid());

        // Converting typedQuery to normal queries:
        var type = typedQuery.getType();
        ObjectQuery objectQuery = typedQuery.toObjectQuery();
        var options = typedQuery.getOptions();
        var otherList = modelService.searchObjects(type, objectQuery, options, task, result);
        assertEquals(list, otherList);
    }

        @Test(enabled = false)
    public void test200EvaluateAxiomGivenNameMidpointFunctions() throws Exception {
        // FIXME: Requires Script evaluation context
        Task task = getTestTask();
        // givenName>Elaine</givenName>
        //    <familyName>Marley</familyName
        var query = midpoint.queryFor(UserType.class, "givenName = 'Elaine'");
        var objects = midpoint.searchObjects(query);
        assertEquals("Wrong number of results", 1, objects.size());
    }


    private void executeFilter(ObjectFilter filter, int expectedNumberOfResults, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFactory().createQuery(filter);
        SearchResultList<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
        display("Found objects", objects);
        assertEquals("Wrong number of results (found: " + objects + ")", expectedNumberOfResults, objects.size());
    }

}
