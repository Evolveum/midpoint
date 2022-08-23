/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.init.AuditServiceProxy;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditService;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This tests {@link SqaleAuditService#searchObjectsIterative} in a gray-box style,
 * knowing that it uses {@link SqaleAuditService#executeSearchObjects} internally.
 * Inspired by {@link SqaleRepoSearchIterativeTest}.
 */
public class SqaleAuditSearchIterativeTest extends SqaleRepoBaseTest {

    private final TestResultHandler testHandler = new TestResultHandler();

    // default page size for iterative search, reset before each test
    private static final int ITERATION_PAGE_SIZE = 100;
    private static final int COUNT_OF_CREATED_RECORDS = ITERATION_PAGE_SIZE * 2;

    private final long startTimestamp = System.currentTimeMillis();

    private SqaleAuditService sqaleAuditService;
    private SqlPerformanceMonitorImpl performanceMonitor;

    // alias for queries, can't be static/final, the mapping it's not yet initialized
    private QAuditEventRecord aer;

    @BeforeClass
    public void initObjects() throws Exception {
        sqaleAuditService = ((AuditServiceProxy) auditService).getImplementation(SqaleAuditService.class);
        performanceMonitor = sqaleAuditService.getPerformanceMonitor();

        aer = QAuditEventRecordMapping.get().defaultAlias();
        clearAudit();

        OperationResult result = createOperationResult();

        long timestamp = startTimestamp;
        Random random = new Random();
        // we will create two full "pages" of data
        for (int i = 1; i <= COUNT_OF_CREATED_RECORDS; i++) {
            AuditEventRecord record = new AuditEventRecord();
            record.setParameter(paramString(i));
            record.setTimestamp(timestamp);
            auditService.audit(record, NullTaskImpl.INSTANCE, result);

            // 50% chance to change the timestamp by up to a second
            timestamp += random.nextInt(2) * random.nextInt(1000);
        }
    }

    // We need to pad the param string so it's possible to compare it.
    private String paramString(int i) {
        return String.format("%05d", i);
    }

    @BeforeMethod
    public void resetTestHandler() {
        testHandler.reset();
        sqaleAuditService.repositoryConfiguration().setIterativeSearchByPagingBatchSize(ITERATION_PAGE_SIZE);
    }

    @Test
    public void test100SearchIterativeWithNoneFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with top level NONE filter");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .none()
                .build();

        when("calling search iterative");
        SearchResultMetadata searchResultMetadata =
                searchObjectsIterative(query, operationResult);

        then("no operation is performed");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(searchResultMetadata).isNotNull();
        assertThat(searchResultMetadata.getApproxNumberOfAllResults()).isZero();
        // this is not the main part, just documenting that currently we short circuit the operation
        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 0);
        // this is important - no actual search was called
        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE, 0);
    }

    @Test
    public void test110SearchIterativeWithEmptyFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        when("calling search iterative with null query");
        SearchResultMetadata metadata =
                searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.isPartialResults()).isFalse();
        assertThat(metadata.getPagingCookie()).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type (here User) were processed");
        assertThat(testHandler.processedCount()).isEqualTo(count(aer));
    }

    @Test
    public void test111SearchIterativeWithLastPageNotFull() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        given("total result count not multiple of the page size");
        sqaleAuditService.repositoryConfiguration().setIterativeSearchByPagingBatchSize(47);

        when("calling search iterative with null query");
        SearchResultMetadata metadata =
                searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.isPartialResults()).isFalse();
        assertThat(metadata.getPagingCookie()).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type were processed");
        assertThat(testHandler.processedCount()).isEqualTo(count(aer));
    }

    @Test
    public void test115SearchIterativeWithBreakingConditionCheckingOidOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("condition that breaks iterative search based on parameter (count)");
        testHandler.setStoppingPredicate(aer -> aer.getParameter().equals(paramString(ITERATION_PAGE_SIZE)));

        when("calling search iterative with null query");
        SearchResultMetadata metadata = searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.isPartialResults()).isTrue(); // extremely likely with enough items

        and("search operations were called");
        assertOperationRecordedCount(AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects up to specified parameter value were processed");
        assertThat(testHandler.processedCount())
                .isEqualTo(count(aer, aer.parameter.loe(paramString(ITERATION_PAGE_SIZE))));
    }

    @Test
    public void test120SearchIterativeWithMaxSize() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with maxSize specified");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .maxSize(101)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.processedCount()).isEqualTo(101);
    }

    @Test
    public void test125SearchIterativeWithCustomOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with custom ordering");
        int limit = 47;
        ObjectQuery query = prismContext.queryFor(AuditEventRecordType.class)
                .asc(AuditEventRecordType.F_PARAMETER)
                .maxSize(limit)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(limit);
        assertThat(metadata.isPartialResults()).isFalse(); // everything was processed

        and("search operations were called");
        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects were processed in proper order");
        Iterator<AuditEventRecordType> processedItems = testHandler.processedEvents.iterator();
        for (int i = 1; i < limit; i++) {
            assertThat(processedItems.next().getParameter()).isEqualTo(paramString(i));
        }
    }

    @Test
    public void test130SearchIterativeWithOffset() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with offset specified");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .offset(100)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.processedCount());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.processedCount()).isEqualTo(COUNT_OF_CREATED_RECORDS - 100);
    }

    @SafeVarargs
    private SearchResultMetadata searchObjectsIterative(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {

        display("QUERY: " + query);
        return auditService.searchObjectsIterative(
                query,
                testHandler,
                selectorOptions != null && selectorOptions.length != 0
                        ? List.of(selectorOptions) : null,
                operationResult);
    }

    private void assertTypicalPageOperationCount(SearchResultMetadata metadata) {
        boolean lastRowCausingPartialResult = metadata.isPartialResults()
                && metadata.getApproxNumberOfAllResults() % getConfiguredPageSize() == 0;

        assertOperationRecordedCount(
                AUDIT_OP_PREFIX + AuditService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE,
                metadata.getApproxNumberOfAllResults() / getConfiguredPageSize()
                        + (lastRowCausingPartialResult ? 0 : 1));
    }

    private int getConfiguredPageSize() {
        return sqaleAuditService.repositoryConfiguration().getIterativeSearchByPagingBatchSize();
    }

    private static class TestResultHandler implements AuditResultHandler {

        private final List<AuditEventRecordType> processedEvents = new ArrayList<>();
        private Predicate<AuditEventRecordType> stoppingPredicate;

        public void reset() {
            processedEvents.clear();
            stoppingPredicate = o -> false;
        }

        public int processedCount() {
            return processedEvents.size();
        }

        public void setStoppingPredicate(Predicate<AuditEventRecordType> stoppingPredicate) {
            this.stoppingPredicate = stoppingPredicate;
        }

        @Override
        public boolean handle(AuditEventRecordType eventRecord, OperationResult parentResult) {
            processedEvents.add(eventRecord);
            return !stoppingPredicate.test(eventRecord); // true means continue, so we need NOT
        }
    }

    @Override
    protected SqlPerformanceMonitorImpl getPerformanceMonitor() {
        return performanceMonitor;
    }
}
