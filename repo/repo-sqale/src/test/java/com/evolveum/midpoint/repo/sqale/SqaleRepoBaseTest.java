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
import java.util.UUID;
import java.util.stream.Stream;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.QNameUtil;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class SqaleRepoBaseTest extends AbstractSpringTest
        implements InfraTestMixin {

    @Autowired protected SqaleRepositoryService repositoryService;
    @Autowired protected SqaleRepoContext sqlRepoContext;
    @Autowired protected PrismContext prismContext;
    @Autowired protected RelationRegistry relationRegistry;

    private static boolean uriCacheCleared = false;

    @BeforeClass
    public void clearDatabase() {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            // object delete cascades to sub-rows of the "object aggregate"

            jdbcSession.executeStatement("TRUNCATE m_object CASCADE;");
            // truncate does not run ON DELETE trigger, many refs/container tables are not cleaned
            jdbcSession.executeStatement("TRUNCATE m_object_oid CASCADE;");
            // but after truncating m_object_oid it cleans all the tables

            /*
            Truncates are much faster than this delete probably because it works row by row:
            long count = jdbcSession.newDelete(QObjectMapping.INSTANCE.defaultAlias()).execute();
            display("Deleted " + count + " objects from DB");
            */
            jdbcSession.commit();
        }

        // this is "suite" scope code, but @BeforeSuite can't use injected fields
        if (!uriCacheCleared) {
            QUri u = QUri.DEFAULT;
            try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                jdbcSession.newDelete(u)
                        // We could skip default relation ID with .where(u.id.gt(0)),
                        // but it must work even when it's gone.
                        .execute();
                jdbcSession.commit();
            }

            sqlRepoContext.clearCaches(); // uses its own transaction

            // It would work with URI cache cleared before every class, but that's not
            // how midPoint will work either.
            uriCacheCleared = true;
        }
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
            Q path, Predicate... conditions) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()) {
            SQLQuery<?> query = jdbcSession.newQuery()
                    .from(path)
                    .where(conditions);
            return query.fetchCount();
        }
    }

    protected <R, Q extends FlexibleRelationalPathBase<R>> List<R> select(
            Class<Q> queryType, Predicate... conditions) {
        return select(aliasFor(queryType), conditions);
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

    protected <R extends MObject, Q extends QObject<R>> R selectObjectByOid(
            Class<Q> queryType, String oid) {
        return selectObjectByOid(queryType, UUID.fromString(oid));
    }

    protected <R extends MObject, Q extends QObject<R>> R selectObjectByOid(
            Class<Q> queryType, UUID oid) {
        Q path = aliasFor(queryType);
        return selectOne(path, path.oid.eq(oid));
    }

    protected String cachedUriById(Integer uriId) {
        QUri qUri = QUri.DEFAULT;
        return selectOne(qUri, qUri.id.eq(uriId)).uri;
    }

    protected void assertCachedUri(Integer uriId, QName qName) {
        assertCachedUri(uriId, QNameUtil.qNameToUri(qName));
    }

    protected void assertCachedUri(Integer uriId, String uri) {
        assertThat(uriId)
                .withFailMessage("Unexpected NULL ID for cached URI %s", uri)
                .isNotNull();
        assertThat(cachedUriById(uriId)).isEqualTo(uri);
    }

    /** Resolves multiple URI IDs to the URI strings. */
    protected String[] resolveCachedUriIds(Integer[] uriIds) {
        return Stream.of(uriIds)
                .map(id -> cachedUriById(id))
                .toArray(String[]::new);
    }

    /** Sets original and normalized name for provided {@link MObject}. */
    protected void setName(MObject object, String origName) {
        object.nameOrig = origName;
        object.nameNorm = prismContext.getDefaultPolyStringNormalizer().normalize(origName);
    }

    protected java.util.function.Predicate<? super Referencable> refMatcher(UUID targetOid, QName relation) {
        return ref -> ref.getOid().equals(targetOid.toString())
                && ref.getRelation().equals(relation);
    }

    protected java.util.function.Predicate<? super MReference> refRowMatcher(UUID targetOid, QName relation) {
        return ref -> ref.targetOid.equals(targetOid)
                && cachedUriById(ref.relationId).equals(QNameUtil.qNameToUri(relation));
    }

    protected java.util.function.Predicate<? super MReference> refRowMatcher(
            UUID targetOid, MObjectType targetType, QName relation) {
        return ref -> ref.targetOid.equals(targetOid)
                && ref.targetType == targetType
                && cachedUriById(ref.relationId).equals(QNameUtil.qNameToUri(relation));
    }
}
