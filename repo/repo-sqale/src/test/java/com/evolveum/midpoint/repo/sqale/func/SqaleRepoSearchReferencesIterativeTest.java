/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismValueUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor.ReferenceRowValue;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRoleMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlRecorder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
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
    private static final int COUNT_OF_CREATED_REFS = ITERATION_PAGE_SIZE * 4;

    private final QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private final QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");
    private QObjectReference<MObject> qMembershipRef;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();
        // Role for some membership refs
        String role1Oid = repositoryService.addObject(new RoleType()
                .name("role1")
                .asPrismObject(), null, result);
        String role2Oid = repositoryService.addObject(new RoleType()
                .name("role2")
                .asPrismObject(), null, result);

        // we will create four refs in each user, that's four full iteration "pages" of data
        for (int i = 1; i <= ITERATION_PAGE_SIZE; i++) {
            String targetOid = UUID.randomUUID().toString();
            UserType user = new UserType()
                    .name(String.format("user-%05d", i))
                    .roleMembershipRef(role1Oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                    .roleMembershipRef(role2Oid, RoleType.COMPLEX_TYPE, relation1)
                    .roleMembershipRef(targetOid, RoleType.COMPLEX_TYPE, relation1)
                    .roleMembershipRef(UUID.randomUUID().toString(), RoleType.COMPLEX_TYPE, relation2);
            repositoryService.addObject(user.asPrismObject(), null, result);
        }

        qMembershipRef = QObjectReferenceMapping.getForRoleMembership().defaultAlias();
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

        given("special query comparing and ordering on reference value (SELF path for reference search)");
        String targetOid = UUID.randomUUID().toString();
        String ownerOid = UUID.randomUUID().toString();
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .and()
                .item(ItemPath.SELF_PATH).gt(new ReferenceRowValue(ownerOid, relation1, targetOid))
                .asc(PrismConstants.T_SELF) // ItemPath.SELF_PATH is refused, because it's empty path
                .build();

        when("reference search is executed");
        repositoryService.searchReferences(query, null, operationResult);

        then("operation is success");
        assertThatOperationResult(operationResult).isSuccess();

        and("query contains expected WHERE and ORDER BY with appropriate parameter values");
        display(queryRecorder.dumpQueryBuffer());
        SqlRecorder.QueryEntry queryEntry = queryRecorder.getQueryBuffer().stream()
                .filter(q -> q.sql.startsWith("select refrm.ownerOid"))
                .findFirst().orElseThrow();
        assertThat(queryEntry.sql)
                // Special "reference table" comparing by SELF:
                .contains("where u.oid = refrm.ownerOid) and (refrm.ownerOid > ?"
                        + " or refrm.ownerOid = ? and refrm.relationId > ?"
                        + " or refrm.ownerOid = ? and refrm.relationId = ? and refrm.targetOid > ?)")
                // Special "reference table" ordering by SELF:
                .contains("order by refrm.ownerOid asc, refrm.relationId asc, refrm.targetOid asc");
        String relationId1 = cachedUriId(relation1).toString();
        assertThat(queryEntry.params)
                // Grouped by OR, final 10000 is the sanity limit.
                .containsExactlyInAnyOrder(ownerOid,
                        ownerOid, relationId1,
                        ownerOid, relationId1, targetOid,
                        "10000");
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

        when("calling search reference iterative");
        SearchResultMetadata searchResultMetadata =
                searchReferencesIterative(query, operationResult);

        then("no operation is performed");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(searchResultMetadata).isNotNull();
        assertThat(searchResultMetadata.getApproxNumberOfAllResults()).isZero();
        // this is not the main part, just documenting that currently we short circuit the operation
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 0);
        // this is important - no actual search was called
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE_PAGE, 0);
    }

    @Test
    public void test105SearchIterativeWithEmptyFilterFails() {
        OperationResult operationResult = createOperationResult();

        expect("calling search reference iterative with empty query (null filter) fails");
        assertThatThrownBy(() ->
                searchReferencesIterative(prismContext.queryFactory().createQuery(), operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageStartingWith("Invalid filter for reference search: null");

        assertThatOperationResult(operationResult).isFatalError();
    }

    @Test
    public void test110SearchIterativeWithoutRefFilter() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("simple query for reference search");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .build();

        when("calling search reference iterative");
        queryRecorder.clearBufferAndStartRecording();
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type (here User) were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(qMembershipRef));
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE_PAGE,
                // even without explicit order, strict order should affect the page queries
                (int) queryRecorder.getQueryBuffer().stream()
                        .filter(e -> e.sql.contains("order by refrm.ownerOid asc"))
                        .count());
    }

    @Test
    public void test111SearchIterativeWithLastPageNotFull() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("total result count not multiple of the page size");
        long totalCount = count(qMembershipRef);
        int iterativePageSize = 47;
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(iterativePageSize);
        assertThat(totalCount % repositoryConfiguration.getIterativeSearchByPagingBatchSize()).isNotZero();
        queryRecorder.clearBufferAndStartRecording();

        when("calling search iterative with unrestricted query");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .build();
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and reports the handled objects");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();
        // page cookie is not null and it's OID in UUID format
        assertThat(UUID.fromString(metadata.getPagingCookie())).isNotNull();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all objects of the specified type were processed");
        assertThat(testHandler.getCounter()).isEqualTo(count(qMembershipRef));

        and("last iteration query has proper conditions");
        List<SqlRecorder.QueryEntry> iterativeSelects = queryRecorder.getQueryBuffer().stream()
                .filter(e -> e.sql.contains("order by refrm.ownerOid asc, refrm.relationId asc, refrm.targetOid asc"))
                .collect(Collectors.toList());
        assertThat(iterativeSelects).hasSize((int) totalCount / iterativePageSize + 1); // +1 for the last page
        SqlRecorder.QueryEntry lastEntry = iterativeSelects.get(iterativeSelects.size() - 1);
        // we want to be sure no accidental filter accumulation happens
        assertThat(lastEntry.sql).contains("where exists (select 1\nfrom m_user u\nwhere u.oid = refrm.ownerOid)"
                + " and (refrm.ownerOid > ? or refrm.ownerOid = ? and refrm.relationId > ?"
                + " or refrm.ownerOid = ? and refrm.relationId = ? and refrm.targetOid > ?)");
    }

    @Test
    public void test115SearchIterativeWithBreakingConditionCheckingOidOrdering() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        queryRecorder.clearBufferAndStartRecording();

        String midOid = "80000000-0000-0000-0000-000000000000";
        given("condition that breaks iterative search based on owner OID");
        testHandler.setStoppingPredicate(r -> ownerOid(r).compareTo(midOid) >= 0);

        when("calling search reference iterative with unrestricted query");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .build();
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);
        display(queryRecorder.dumpQueryBuffer());

        then("result metadata is not null and reports partial result (because of the break)");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isTrue(); // extremely likely with enough items

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all refs up to specified owner OID were processed");
        assertThat(testHandler.getCounter())
                // first >= midOid was processed too
                .isEqualTo(count(qMembershipRef,
                        qMembershipRef.ownerOid.lt(UUID.fromString(midOid))) + 1);
    }

    private static String ownerOid(ObjectReferenceType ref) {
        return Objects.requireNonNull(PrismValueUtil.getParentObject(ref.asReferenceValue()))
                .getOid();
    }

    @Test
    public void test120SearchIterativeWithMaxSize() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with maxSize specified");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .maxSize(101)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.getCounter()).isEqualTo(101);
    }

    @Test
    public void test125SearchIterativeWithCustomOrderingByTargetItem() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        // We want to test multiple iteration search, as that shows potential errors in lastRefFilter() method.
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(47);
        queryRecorder.clearBufferAndStartRecording();

        given("query with custom ordering by target item");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .asc(PrismConstants.T_OBJECT_REFERENCE, RoleType.F_NAME)
                // We want to cover refs with two different target OID values:
                .maxSize(120)
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse(); // everything was processed

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("all expected refs were processed");
        QAbstractRole<?> qar = QAbstractRoleMapping.getAbstractRoleMapping().defaultAlias();
        QUser qu = aliasFor(QUser.class);
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            // This is the expected query for the search above:
            compareRowsWithProcessedRefs(jdbcSession.newQuery()
                    .select(qMembershipRef)
                    .from(qMembershipRef)
                    .leftJoin(qar).on(qMembershipRef.targetOid.eq(qar.oid))
                    .where(sqlRepoContext.newQuery().from(qu)
                            .where(qu.oid.eq(qMembershipRef.ownerOid))
                            .exists())
                    .orderBy(qar.nameNorm.asc(),
                            qMembershipRef.ownerOid.asc(),
                            qMembershipRef.relationId.asc(),
                            qMembershipRef.targetOid.asc())
                    .limit(120) // must match the maxSize above
                    .fetch());
        }
    }

    private void compareRowsWithProcessedRefs(List<MReference> result) {
        for (int i = 0; i < result.size(); i++) {
            MReference refRow = result.get(i);
            ObjectReferenceType processedRef = testHandler.processedRefs.get(i);
            assertThat(refRow.ownerOid.toString()).describedAs("owner OID for row %d", i)
                    .isEqualTo(ownerOid(processedRef));
            assertThat(refRow.relationId).describedAs("relation for row %d", i)
                    .isEqualTo(cachedUriId(processedRef.getRelation()));
            assertThat(refRow.targetOid.toString()).describedAs("target OID for row %d", i)
                    .isEqualTo(processedRef.getOid());
        }
    }

    @Test
    public void test130SearchIterativeWithCustomOrderingByOwnerItemDesc() throws Exception {
        OperationResult operationResult = createOperationResult();
        repositoryConfiguration.setIterativeSearchByPagingBatchSize(15); // to ensure multiple pages
        queryRecorder.clearBufferAndStartRecording();

        given("query with custom ordering by owner item");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .desc(PrismConstants.T_PARENT, UserType.F_NAME)
                .maxSize(47) // see the limit below
                .build();

        when("calling search iterative");
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("multiple iterations were called");

        and("all objects were processed");
        /*
        Query is suboptimal with m_assignment_holder joined for order instead of m_user, but fix would require
        some logic taking the true parent type from owned-by filter, which is not a generic solution.
        The query lower
        */
        QUser qu = aliasFor(QUser.class);
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            compareRowsWithProcessedRefs(jdbcSession.newQuery()
                    .select(qMembershipRef)
                    .from(qMembershipRef)
                    .leftJoin(qu).on(qMembershipRef.ownerOid.eq(qu.oid))
                    .where(sqlRepoContext.newQuery().from(qu)
                            .where(qu.oid.eq(qMembershipRef.ownerOid))
                            .exists())
                    .orderBy(qu.nameNorm.desc(),
                            qMembershipRef.ownerOid.desc(),
                            qMembershipRef.relationId.desc(),
                            qMembershipRef.targetOid.desc())
                    .limit(47) // must match the maxSize above
                    .fetch());
        }
    }

    @Test
    public void test135SearchIterativeWithOffset() throws Exception {
        OperationResult operationResult = createOperationResult();
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();

        given("query with offset specified");
        ObjectQuery query = prismContext
                .queryForReferenceOwnedBy(UserType.class, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .offset(150)
                .build();

        when("calling search references iterative");
        SearchResultMetadata metadata = searchReferencesIterative(query, operationResult);

        then("result metadata is not null and not partial result");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getApproxNumberOfAllResults()).isEqualTo(testHandler.getCounter());
        assertThat(metadata.isPartialResults()).isFalse();

        and("search operations were called");
        assertOperationRecordedCount(
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE, 1);
        assertTypicalPageOperationCount(metadata);

        and("specified amount of objects was processed");
        assertThat(testHandler.getCounter()).isEqualTo(COUNT_OF_CREATED_REFS - 150);
    }

    @SafeVarargs
    private SearchResultMetadata searchReferencesIterative(
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {

        displayQuery(query);
        return repositoryService.searchReferencesIterative(
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
                REPO_OP_PREFIX + RepositoryService.OP_SEARCH_REFERENCES_ITERATIVE_PAGE,
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
    private static class TestResultHandler implements ObjectHandler<ObjectReferenceType> {

        private final List<ObjectReferenceType> processedRefs = new ArrayList<>();
        private Predicate<ObjectReferenceType> stoppingPredicate;

        public void reset() {
            processedRefs.clear();
            stoppingPredicate = o -> false;
        }

        public int getCounter() {
            return processedRefs.size();
        }

        public void setStoppingPredicate(Predicate<ObjectReferenceType> stoppingPredicate) {
            this.stoppingPredicate = stoppingPredicate;
        }

        @Override
        public boolean handle(ObjectReferenceType ref, OperationResult parentResult) {
            try {
                processedRefs.add(ref);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return !stoppingPredicate.test(ref); // true means continue, so we need NOT
        }
    }
}
