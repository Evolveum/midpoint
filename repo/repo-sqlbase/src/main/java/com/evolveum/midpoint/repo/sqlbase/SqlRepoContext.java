/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.xnode.RootXNode;

import com.querydsl.sql.*;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Encapsulates Querydsl {@link Configuration}, our {@link QueryModelMappingRegistry}
 * and other parts of SQL repository config and implements methods that need these.
 * Preferably, it should hide (hence "encapsulate") the fields and offer behavior instead.
 */
public class SqlRepoContext {

    private final JdbcRepositoryConfiguration jdbcRepositoryConfiguration;
    protected final Configuration querydslConfig;
    protected final SchemaService schemaService;
    private final QueryModelMappingRegistry mappingRegistry;
    private final DataSource dataSource;

    private SQLBaseListener querydslSqlListener;

    public SqlRepoContext(
            JdbcRepositoryConfiguration jdbcRepositoryConfiguration,
            DataSource dataSource,
            SchemaService schemaService,
            QueryModelMappingRegistry mappingRegistry) {
        this.jdbcRepositoryConfiguration = jdbcRepositoryConfiguration;
        this.querydslConfig = QuerydslUtils.querydslConfiguration(
                jdbcRepositoryConfiguration.getDatabaseType());
        this.schemaService = schemaService;
        this.mappingRegistry = mappingRegistry;
        this.dataSource = dataSource;
    }

    /**
     * Sets Querydsl SQL listener for this repository, replacing any previously one.
     */
    public void setQuerydslSqlListener(SQLBaseListener listener) {
        SQLListeners listeners = querydslConfig.getListeners();
        if (querydslSqlListener != null) {
            listeners.getListeners().remove(querydslSqlListener);
        }
        listeners.add(listener);
        querydslSqlListener = listener;
    }

    public SQLQuery<?> newQuery() {
        return new SQLQuery<>(querydslConfig);
    }

    public SQLQuery<?> newQuery(Connection conn) {
        return new SQLQuery<>(conn, querydslConfig);
    }

    /**
     * @param <TQ> query type for the target table
     * @param <TR> row type related to the {@link TQ}
     */
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> QueryTableMapping<TS, TQ, TR>
    getMappingByQueryType(Class<TQ> queryType) {
        return mappingRegistry.getByQueryType(queryType);
    }

    // QM is potentially narrowed expected subtype of QueryModelMapping
    public <S, Q extends FlexibleRelationalPathBase<R>, R, QM extends QueryTableMapping<S, Q, R>>
    QM getMappingBySchemaType(Class<S> schemaType) {
        return mappingRegistry.getBySchemaType(schemaType);
    }

    public SQLTemplates getQuerydslTemplates() {
        return querydslConfig.getTemplates();
    }

    public Configuration getQuerydslConfiguration() {
        return querydslConfig;
    }

    public SQLInsertClause newInsert(Connection connection, RelationalPath<?> entity) {
        return new SQLInsertClause(connection, querydslConfig, entity);
    }

    public SQLUpdateClause newUpdate(Connection connection, RelationalPath<?> entity) {
        return new SQLUpdateClause(connection, querydslConfig, entity);
    }

    public SQLDeleteClause newDelete(Connection connection, RelationalPath<?> entity) {
        return new SQLDeleteClause(connection, querydslConfig, entity);
    }

    public JdbcRepositoryConfiguration getJdbcRepositoryConfiguration() {
        return jdbcRepositoryConfiguration;
    }

    /**
     * Creates {@link JdbcSession} that typically represents transactional work on JDBC connection.
     * All other lifecycle methods are to be called on the returned object.
     * Object is {@link AutoCloseable} and can be used in try-with-resource blocks.
     * This call be followed by {@link JdbcSession#startTransaction()} (or one of its variants).
     * If the transaction is not started the connection will likely be in auto-commit mode.
     * *We want to start transaction for any work in production code* but for tests it's ok not to.
     */
    public JdbcSession newJdbcSession() {
        try {
            return new JdbcSession(dataSource.getConnection(), jdbcRepositoryConfiguration, this);
        } catch (SQLException e) {
            throw new SystemException("Cannot create JDBC connection", e);
        }
    }

    public <T> Class<? extends T> qNameToSchemaClass(QName qName) {
        return schemaService.typeQNameToSchemaClass(qName);
    }

    public QName schemaClassToQName(Class<?> schemaClass) {
        return schemaService.schemaClassToTypeQName(schemaClass);
    }

    public @NotNull QName normalizeRelation(QName qName) {
        return schemaService.normalizeRelation(qName);
    }

    @NotNull
    public PrismSerializer<String> createStringSerializer() {
        return schemaService.createStringSerializer(
                getJdbcRepositoryConfiguration().getFullObjectFormat());
    }

    public <T> RepositoryObjectParseResult<T> parsePrismObject(
            String serializedForm, Class<T> schemaType) throws SchemaException {
        try {
            PrismContext prismContext = schemaService.prismContext();
            // "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
            ParsingContext parsingContext = prismContext.createParsingContextForCompatibilityMode();
            RootXNode xnodeValue;
            T value;
            try (var tracker = SqlBaseOperationTracker.parseJson2XNode(schemaType.getSimpleName())) {
                 xnodeValue = createStringParser(serializedForm).context(parsingContext).parseToXNode();

            }
            try (var tracker = SqlBaseOperationTracker.parseXnode2Prism(schemaType.getSimpleName())) {
                value = prismContext.parserFor(xnodeValue).context(parsingContext).fastAddOperations().parseRealValue(schemaType);
            }
            return new RepositoryObjectParseResult<>(parsingContext, value);
        } catch (RuntimeException e) {
            throw new SchemaException("Unexpected exception while parsing serialized form: " + e, e);
        }
    }

    public <T> RepositoryObjectParseResult<T> parsePrismObject(
            String serializedForm, ItemDefinition definition, Class<T> schemaType) throws SchemaException {
        try {
            PrismContext prismContext = schemaService.prismContext();
            // "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
            ParsingContext parsingContext = createParsingContext();
            RootXNode xnodeValue;
            T value;
            try (var tracker = SqlBaseOperationTracker.parseJson2XNode(schemaType.getSimpleName())) {
                xnodeValue = createStringParser(serializedForm).context(parsingContext).definition(definition).parseToXNode();

            }
            try (var tracker = SqlBaseOperationTracker.parseXnode2Prism(schemaType.getSimpleName())) {
                value = prismContext.parserFor(xnodeValue).context(parsingContext).definition(definition).fastAddOperations().parseRealValue(schemaType);
            }
            return new RepositoryObjectParseResult<>(parsingContext, value);
        } catch (RuntimeException e) {
            throw new SchemaException("Unexpected exception while parsing serialized form: " + e, e);
        }
    }

    protected ParsingContext createParsingContext() {
        return schemaService.prismContext().createParsingContextForCompatibilityMode();
    }

    @NotNull
    public PrismParserNoIO createStringParser(String serializedResult) {
        return schemaService.parserFor(serializedResult);
    }

    /**
     * Sometimes delegation is not enough - we need Prism context for schema type construction
     * with definitions (parameter to constructor).
     */
    public PrismContext prismContext() {
        return schemaService.prismContext();
    }

    public MatchingRuleRegistry matchingRuleRegistry() {
        return schemaService.matchingRuleRegistry();
    }

    public RelationRegistry relationRegistry() {
        return schemaService.relationRegistry();
    }

    public void normalizeAllRelations(PrismContainerValue<?> pcv) {
        ObjectTypeUtil.normalizeAllRelations(pcv, schemaService.relationRegistry());
    }
}
