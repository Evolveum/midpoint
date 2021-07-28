/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This tests {@link SqaleRepositoryService#searchObjectsIterative} in a gray-box style,
 * knowing that it uses {@link SqaleRepositoryService#searchObjects} internally.
 * We're not only interested in the fact that it iterates over all the objects matching criteria,
 * but we also want to assure that the internal paging is strictly sequential.
 */
public class SqaleRepoSearchIterativeTest extends SqaleRepoBaseTest {

    private TestResultHandler testHandler = new TestResultHandler();

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();
        repositoryService.addObject(
                new UserType(prismContext).name("user").asPrismObject(), null, result);
    }

    @Test
    public void test100SearchIterativeWithNoneFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with top level NONE filter");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .none()
                .build();

        when("calling search iterative");
        SearchResultMetadata searchResultMetadata =
                searchObjectsIterative(query, operationResult);

        then("result metadata is null and no operation is performed");
        assertThat(searchResultMetadata).isNull();
        // this is not the main part, just documenting that currently we short circuit the operation
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 0);
        // this is important - no actual search was called
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS, 0);
    }

    @Test
    public void test110SearchIterativeWithEmptyFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        when("calling search iterative with null query");
        SearchResultMetadata searchResultMetadata =
                searchObjectsIterative(null, operationResult);

        then("result metadata is not null");
        assertThat(searchResultMetadata).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS, 1);

        and("all objects of the specified type (here User) were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(QUser.class));
    }

    @SafeVarargs
    private SearchResultMetadata searchObjectsIterative(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {

        display("QUERY: " + query);
        testHandler.reset();

        return repositoryService.searchObjectsIterative(
                UserType.class,
                query,
                testHandler,
                selectorOptions != null && selectorOptions.length != 0
                        ? List.of(selectorOptions) : null,
                true, // this boolean is actually ignored (assumed to be true) by new repo
                operationResult);
    }

    private static class TestResultHandler implements ResultHandler<UserType> {

        private final AtomicInteger counter = new AtomicInteger();

        public void reset() {
            counter.set(0);
        }

        public int getCounter() {
            return counter.get();
        }

        @Override
        public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
            UserType user = object.asObjectable();
            user.setIteration(counter.getAndIncrement()); // TODO can I use iteration freely?
            return false;
        }
    }
}
