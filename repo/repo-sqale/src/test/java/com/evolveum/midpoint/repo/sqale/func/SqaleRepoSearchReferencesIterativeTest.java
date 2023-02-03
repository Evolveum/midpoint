/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.xml.namespace.QName;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor.ReferenceRowValue;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlRecorder;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This tests {@link SqaleRepositoryService#searchReferencesIterative} in a gray-box style.
 * knowing that it uses {@link SqaleRepositoryService#executeSearchReferences} internally.
 * We're not only interested in the fact that it iterates over all the refs matching criteria,
 * but we also want to assure that the internal paging is strictly sequential.
 * Each test can take a bit longer (~500ms) because the handler updates the objects
 * to mark them for later assertions, so there's actually a lot of repository calls.
 */
// TODO add to the suite
public class SqaleRepoSearchReferencesIterativeTest extends SqaleRepoBaseTest {

    private final TestResultHandler testHandler = new TestResultHandler();

    // default page size for iterative search, reset before each test
    private static final int ITERATION_PAGE_SIZE = 100;
    private static final int COUNT_OF_CREATED_USERS = ITERATION_PAGE_SIZE * 2 + 1;

    private final QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private final QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();
        // we will create two full "pages" of data
        for (int i = 1; i <= ITERATION_PAGE_SIZE; i++) {
            String targetOid = UUID.randomUUID().toString();
            UserType user = new UserType()
                    .name(String.format("user-%05d", i))
                    .roleMembershipRef(targetOid, RoleType.COMPLEX_TYPE, relation1)
                    .roleMembershipRef(targetOid, RoleType.COMPLEX_TYPE, relation2)
                    .roleMembershipRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE, relation2)
                    .costCenter(String.valueOf(i / 10)); // 10 per cost center
            repositoryService.addObject(user.asPrismObject(), null, result);
        }
        // MID-7860: Special name that breaks iteration conditions which should be only by orig.
        // If additional conditions for further "pages" use strict poly match (which is default)
        // then both orig and norm is used for GT/LT operations and it doesn't work for some values.
        repositoryService.addObject(new UserType()
                .name("α-user-0001")
                .asPrismObject(), null, result);
    }

    @BeforeMethod
    public void resetTestHandler() {
        testHandler.reset();
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(ITERATION_PAGE_SIZE);
    }

    @AfterMethod
    public void methodCleanup() {
        queryRecorder.stopRecording();
    }

    @Test
    public void test010RefFilterComparison() throws SchemaException {
        OperationResult operationResult = createOperationResult();
        queryRecorder.clearBufferAndStartRecording();

        given("special query comparing on reference value (SELF path for reference search)");
        String targetOid = UUID.randomUUID().toString();
        String ownerOid = UUID.randomUUID().toString();
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .and()
                .item(ItemPath.SELF_PATH).gt(new ReferenceRowValue(ownerOid, relation1, targetOid))
                .build();

        when("reference search is executed");
        repositoryService.searchReferences(query, null, operationResult);

        then("operation is success");
        assertThatOperationResult(operationResult).isSuccess();

        and("query contains expected WHERE with appropriate parameter values");
        display(queryRecorder.dumpQueryBuffer());
        SqlRecorder.QueryEntry queryEntry = queryRecorder.getQueryBuffer().stream()
                .filter(q -> q.sql.startsWith("select refrm.ownerOid"))
                .findFirst().orElseThrow();
        assertThat(queryEntry.sql)
                .contains("where u.oid = refrm.ownerOid) and (refrm.ownerOid > ?"
                        + " or refrm.ownerOid = ? and refrm.relationId > ?"
                        + " or refrm.ownerOid = ? and refrm.relationId = ? and refrm.targetOid > ?)");
        String relationId1 = cachedUriId(relation1).toString();
        assertThat(queryEntry.params)
                // Grouped by OR, final 10000 is the sanity limit.
                .containsExactlyInAnyOrder(ownerOid,
                        ownerOid, relationId1,
                        ownerOid, relationId1, targetOid,
                        "10000");
    }

    /*
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
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 0);
        // this is important - no actual search was called
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE, 0);
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
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type (here User) were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(QUser.class));
    }

    @Test
    public void test111SearchIterativeWithLastPageNotFull() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("total result count not multiple of the page size");
        long totalCount = count(QObject.CLASS);
        int iterativePageSize = 47;
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(iterativePageSize);
        assertThat(totalCount % repositoryConfiguration.getIterativeSearchByPagingBatchSize()).isNotZero();
        queryRecorder.clearBufferAndStartRecording();

        when("calling search iterative with null query");
        SearchResultMetadata metadata =
                searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(QUser.class));

        and("last iteration query has proper conditions");
        List<SqlRecorder.QueryEntry> iterativeSelects = queryRecorder.getQueryBuffer().stream()
                .filter(e -> e.sql.contains("order by u.oid asc"))
                .collect(Collectors.toList());
        assertThat(iterativeSelects).hasSize((int) totalCount / iterativePageSize + 1); // +1 for the last page
        SqlRecorder.QueryEntry lastEntry = iterativeSelects.get(iterativeSelects.size() - 1);
        // we want to be sure no accidental filter accumulation happens
        assertThat(lastEntry.sql).contains("where u.oid > ?\norder by u.oid asc");
    }

    @Test
    public void test112SearchIterativeWithLastPageNotFullWithAndFilter() throws Exception {
        // Like test111 but detects error when conditions are accumulating in provided AND filter with each page.
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("total result count not multiple of the page size");
        long totalCount = count(QUser.class);
        int iterativePageSize = 47;
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(iterativePageSize);
        assertThat(totalCount % repositoryConfiguration.getIterativeSearchByPagingBatchSize()).isNotZero();
        queryRecorder.clearBufferAndStartRecording();

        when("calling search iterative with query containing condition");
        SearchResultMetadata metadata = searchObjectsIterative(
                prismContext.queryFor(UserType.class)
                        .not().item(UserType.F_NAME).isNull() // not null, matches all users
                        .and()
                        .item(UserType.F_GIVEN_NAME).isNull() // this is null in setup above
                        .build(),
                operationResult);
        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(QUser.class));

        and("last iteration query has proper conditions");
        List<SqlRecorder.QueryEntry> iterativeSelects = queryRecorder.getQueryBuffer().stream()
                .filter(e -> e.sql.contains("order by u.oid asc"))
                .collect(Collectors.toList());
        assertThat(iterativeSelects).hasSize((int) totalCount / iterativePageSize + 1); // +1 for the last page
        SqlRecorder.QueryEntry lastEntry = iterativeSelects.get(iterativeSelects.size() - 1);
        // We want to be sure no accidental filter accumulation happens, see ObjectQueryUtil.filterAnd() vs createAnd().
        assertThat(lastEntry.sql).contains("where not (u.nameNorm is null and u.nameOrig is null)"
                + " and (u.givenNameNorm is null and u.givenNameOrig is null) and u.oid > ?\norder");
    }

    @Test
    public void test115SearchIterativeWithBreakingConditionCheckingOidOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        String midOid = "80000000-0000-0000-0000-000000000000";
        given("condition that breaks iterative search based on UUID");
        testHandler.setStoppingPredicate(u -> u.getOid().compareTo(midOid) >= 0);

        when("calling search iterative with null query");
        SearchResultMetadata metadata = searchObjectsIterative(null, operationResult);

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isTrue(); // extremely likely with enough items

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects up to specified UUID were processed");
        QUser u = aliasFor(QUser.class);
        assertThat(testHandler.getCounter())
                // first >= midOid was processed too
                .isEqualTo(count(u, u.oid.lt(UUID.fromString(midOid))) + 1);
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

        then("result metadata is not null and not partial result");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.getCounter()).isEqualTo(101);
    }

    @Test(description = "MID-7860")
    public void test125SearchIterativeWithCustomOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with custom ordering");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .asc(UserType.F_COST_CENTER)
                .maxSize(47) // see the limit below
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse(); // everything was processed

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects were processed");
        QUser u = aliasFor(QUser.class);
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            List<String> result = jdbcSession.newQuery()
                    .from(u)
                    .orderBy(u.costCenter.asc(), u.oid.asc())
                    .select(u.employeeNumber)
                    .limit(47) // must match the maxSize above
                    .fetch();

            for (int i = 1; i < result.size(); i++) {
                assertThat(result.get(i)).isEqualTo(getTestNumber() + "-" + i); // order matches
            }
        }
    }

    @Test
    public void test130SearchIterativeWithCustomOrderingByName() throws Exception {
        OperationResult operationResult = createOperationResult();

        given("query with custom ordering by name (poly-string)");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .asc(UserType.F_NAME)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("all objects were processed");
        QUser u = aliasFor(QUser.class);
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            long processed = jdbcSession.newQuery()
                    .from(u)
                    .where(u.employeeNumber.startsWith(getTestNumber()))
                    .fetchCount();

            assertThat(processed).isEqualTo(count(QUser.class)); // all users should be processed
        }
    }

    @Test
    public void test135SearchIterativeWithOffset() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with offset specified");
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .offset(100)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchObjectsIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.getCounter()).isEqualTo(COUNT_OF_CREATED_USERS - 100);
    }

    @SafeVarargs
    private SearchResultMetadata searchObjectsIterative(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {

        displayQuery(query);
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
        boolean lastRowCausingPartialResult = metadata.isPartialResults()
                && metadata.getApproxNumberOfAllResults() % getConfiguredPageSize() == 0;

        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_OBJECTS_ITERATIVE_PAGE,
                metadata.getApproxNumberOfAllResults() / getConfiguredPageSize()
                        + (lastRowCausingPartialResult ? 0 : 1));
    }

    private int getConfiguredPageSize() {
        return sqlRepoContext.getJdbcRepositoryConfiguration()
                .getIterativeSearchByPagingBatchSize();
    }

    /**
     * Counts processed objects and changes user's employee number (test+count).
     */
    private class TestResultHandler implements ObjectHandler<ObjectReferenceType> {

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
        public boolean handle(ObjectReferenceType object, OperationResult parentResult) {
            System.out.println("object = " + object);
            /* TODO
            UserType user = object.asObjectable();
            try {
                repositoryService.modifyObject(UserType.class, user.getOid(),
                        prismContext.deltaFor(UserType.class)
                                .item(UserType.F_EMPLOYEE_NUMBER)
                                .replace(getTestNumber() + "-" + counter.getAndIncrement())
                                .asObjectDelta(user.getOid())
                                .getModifications(),
                        parentResult);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return !stoppingPredicate.test(user); // true means continue, so we need NOT
            */
            return true;
        }
    }
}