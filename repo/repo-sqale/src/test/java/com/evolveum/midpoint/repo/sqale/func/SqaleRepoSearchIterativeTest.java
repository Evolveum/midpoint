/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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

    private final TestResultHandler testHandler = new TestResultHandler();

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();
        for (int i = 1; i <= 100; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format("user-%05d", i));
            repositoryService.addObject(user.asPrismObject(), null, result);
        }
    }

    @BeforeMethod
    public void resetTestHandler() {
        testHandler.reset();
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

        then("no operation is performed");
        assertThat(searchResultMetadata).isNotNull();
        assertThat(searchResultMetadata.getApproxNumberOfAllResults()).isZero();
        // this is not the main part, just documenting that currently we short circuit the operation
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 0);
        // this is important - no actual search was called
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE, 0);
    }

    @Test
    public void test110SearchIterativeWithEmptyFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        when("calling search iterative with null query");
        SearchResultMetadata metadata =
                searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type (here User) were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(QUser.class));
    }

    @Test
    public void test115SearchIterativeWithBreakingConditionCheckingOidOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        String midOid = "80000000-0000-0000-0000-000000000000";
        given("condition that breaks iterative based on UUID");
        testHandler.setStoppingPredicate(u -> u.getOid().compareTo(midOid) >= 0);

        when("calling search iterative with null query");
        SearchResultMetadata metadata = searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isTrue(); // extremely likely with enough items

        and("search operations were called");
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);

        and("all objects up to specified UUID were processed");
        QUser u = aliasFor(QUser.class);
        assertThat(testHandler.getCounter())
                // first >= midOid was processed too
                .isEqualTo(count(u, u.oid.lt(UUID.fromString(midOid))) + 1);
    }

    @SafeVarargs
    private SearchResultMetadata searchObjectsIterative(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {

        display("QUERY: " + query);
        return repositoryService.searchObjectsIterative(
                UserType.class,
                query,
                testHandler,
                selectorOptions != null && selectorOptions.length != 0
                        ? List.of(selectorOptions) : null,
                true, // this boolean is actually ignored (assumed to be true) by new repo
                operationResult);
    }

    private void assertTypicalPageOperationCount(SearchResultMetadata metadata) {
        assertOperationRecordedCount(RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE,
                metadata.getApproxNumberOfAllResults() / getConfiguredPageSize() + 1);
    }

    private int getConfiguredPageSize() {
        return sqlRepoContext.getJdbcRepositoryConfiguration()
                .getIterativeSearchByPagingBatchSize();
    }

    private static class TestResultHandler implements ResultHandler<UserType> {

        private final AtomicInteger counter = new AtomicInteger();
        private Predicate<UserType> stoppingPredicate;

        public void reset() {
            counter.set(0);
            stoppingPredicate = o -> false;
        }

        public int getCounter() {
            return counter.get();
        }

        public void setStoppingPredicate(Predicate<UserType> stoppingPredicate) {
            this.stoppingPredicate = stoppingPredicate;
        }

        @Override
        public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
            UserType user = object.asObjectable();
            user.setIteration(counter.getAndIncrement()); // TODO can I use iteration freely?
            return !stoppingPredicate.test(user); // true means continue, so we need NOT
        }
    }
}
