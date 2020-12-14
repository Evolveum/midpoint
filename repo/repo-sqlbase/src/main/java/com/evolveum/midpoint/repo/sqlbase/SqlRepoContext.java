package com.evolveum.midpoint.repo.sqlbase;

import java.sql.Connection;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;

import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Encapsulates Querydsl {@link Configuration}, our {@link QueryModelMappingRegistry}
 * and other parts of SQL repository config and implements methods that need these.
 * Preferably, it should hide (hence "encapsulate") the fields and offer behavior instead.
 */
public class SqlRepoContext {

    private final Configuration querydslConfig;
    private final QueryModelMappingRegistry mappingRegistry;
    // TODO: add datasource? can this be replacement for BaseHelper?

    public SqlRepoContext(Configuration querydslConfig, QueryModelMappingRegistry mappingRegistry) {
        this.querydslConfig = querydslConfig;
        this.mappingRegistry = mappingRegistry;
    }

    public SQLQuery<?> newQuery() {
        return new SQLQuery<>(querydslConfig);
    }

    public SQLQuery<?> newQuery(Connection conn) {
        return new SQLQuery<>(conn, querydslConfig);
    }

    public <DR, DQ extends FlexibleRelationalPathBase<DR>> QueryModelMapping<?, DQ, DR>
    getMappingByQueryType(Class<DQ> queryType) {
        return mappingRegistry.getByQueryType(queryType);
    }

    public <S, R, Q extends FlexibleRelationalPathBase<R>> QueryModelMapping<S, Q, R>
    getMappingBySchemaType(Class<S> schemaType) {
        return mappingRegistry.getBySchemaType(schemaType);
    }

    public SQLTemplates getQuerydslTemplates() {
        return querydslConfig.getTemplates();
    }

    public SQLInsertClause newInsert(Connection connection, RelationalPath<?> entity) {
        return new SQLInsertClause(connection, querydslConfig, entity);
    }

    public SQLDeleteClause newDelete(Connection connection, RelationalPath<?> entity) {
        return new SQLDeleteClause(connection, querydslConfig, entity);
    }
}
