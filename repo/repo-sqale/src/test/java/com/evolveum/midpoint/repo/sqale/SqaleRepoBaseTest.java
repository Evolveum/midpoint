/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class SqaleRepoBaseTest extends AbstractSpringTest
        implements InfraTestMixin {

    @Autowired protected SqaleRepositoryService repositoryService;
    @Autowired protected SqaleRepoContext sqlRepoContext;
    @Autowired protected PrismContext prismContext;

    @BeforeClass
    public void cleanDatabase() {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            // object delete cascades to sub-rows of the "object aggregate"
            jdbcSession.newDelete(QObjectMapping.INSTANCE.defaultAlias()).execute();
        }
    }

    protected void assertResult(OperationResult opResult) {
        if (opResult.isEmpty()) {
            // this is OK. Nothing added to result.
            return;
        }
        opResult.computeStatus();
        TestUtil.assertSuccess(opResult);
    }

    /**
     * Returns default query instance (alias) for the specified query type.
     * Don't use this for multi-table types like references, use something like this instead:
     *
     * ----
     * QObjectReference r = QObjectReferenceMapping.INSTANCE_PROJECTION.defaultAlias();
     * ----
     */
    protected <R, Q extends FlexibleRelationalPathBase<R>> Q aliasFor(Class<Q> entityPath) {
        return sqlRepoContext.getMappingByQueryType(entityPath).defaultAlias();
    }

    /** Returns new named query instance (alias) for the specified query type. */
    protected <R, Q extends FlexibleRelationalPathBase<R>> Q aliasFor(
            Class<Q> entityPath, String name) {
        return sqlRepoContext.getMappingByQueryType(entityPath).newAlias(name);
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> void assertCount(
            Class<Q> queryType, long expectedCount) {
        assertCount(aliasFor(queryType), expectedCount);
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> void assertCount(
            Q path, long expectedCount, Predicate... conditions) {
        assertThat(count(path, conditions))
                .as("Count for %s where %s", path, Arrays.toString(conditions))
                .isEqualTo(expectedCount);
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> long count(
            Class<Q> queryType, Predicate... conditions) {
        return count(aliasFor(queryType), conditions);
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> long count(
            Q path, Predicate[] conditions) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            SQLQuery<?> query = jdbcSession.newQuery()
                    .from(path)
                    .where(conditions);
            return query.fetchCount();
        }
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> List<R> select(
            Q path, Predicate... conditions) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            return jdbcSession.newQuery()
                    .from(path)
                    .where(conditions)
                    .select(path)
                    .fetch();
        }
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> R selectOne(
            Q path, Predicate... conditions) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            return jdbcSession.newQuery()
                    .from(path)
                    .where(conditions)
                    .select(path)
                    .fetchOne();
        }
    }

    protected String cachedUriById(Integer uriId) {
        QUri qUri = QUri.DEFAULT;
        return selectOne(qUri, qUri.id.eq(uriId)).uri;
    }

    protected void assertCachedUri(Integer uriId, QName qName) {
        assertCachedUri(uriId, QNameUtil.qNameToUri(qName));
    }

    protected void assertCachedUri(Integer uriId, String uri) {
        assertThat(cachedUriById(uriId)).isEqualTo(uri);
    }

    /** Sets original and normalized name for provided {@link MObject}. */
    protected void setName(MObject object, String origName) {
        object.nameOrig = origName;
        object.nameNorm = prismContext.getDefaultPolyStringNormalizer().normalize(origName);
    }
}
