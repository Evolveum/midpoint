/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import javax.sql.DataSource;

import com.querydsl.core.types.EntityPath;
import com.querydsl.sql.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.pure.querymodel.support.InstantType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Component just under the service that orchestrates query transformation and execution.
 * Sql query executor itself does hold the query state, it uses {@link SqlQueryContext} for that.
 * This object manages configuration information and provides dataSource/connections for queries.
 */
public class SqlQueryExecutor {

    private final PrismContext prismContext;
    private final DataSource dataSource;

    private final Configuration querydslConfiguration;

    public SqlQueryExecutor(PrismContext prismContext, BaseHelper baseHelper) {
        this.prismContext = prismContext;
        this.dataSource = baseHelper.dataSource();

        SqlRepositoryConfiguration.Database database =
                baseHelper.getConfiguration().getDatabaseType();
        switch (database) {
            case H2:
                querydslConfiguration = new Configuration(H2Templates.DEFAULT);
                break;
            case MYSQL:
            case MARIADB:
                querydslConfiguration = new Configuration(MySQLTemplates.DEFAULT);
                break;
            case POSTGRESQL:
                querydslConfiguration = new Configuration(PostgreSQLTemplates.DEFAULT);
                break;
            case SQLSERVER:
                querydslConfiguration = new Configuration(SQLServer2012Templates.DEFAULT);
                break;
            case ORACLE:
                querydslConfiguration = new Configuration(OracleTemplates.DEFAULT);
                break;
            default:
                throw new SystemException("Unsupported database type " + database);
        }

        // See InstantType javadoc for the reasons why we need this to support Instant.
        querydslConfiguration.register(new InstantType());
        // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
    }

    public <S, Q extends EntityPath<R>, R> int count(
            @NotNull Class<S> schemaType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {

        SqlQueryContext<S, Q, R> context =
                SqlQueryContext.from(schemaType, prismContext, querydslConfiguration);
        if (query != null) {
            context.process(query.getFilter());
        }
        // TODO MID-6319: all options can be applied, just like for list?
        context.processOptions(options);

        try (Connection conn = getConnection()) {
            return context.executeCount(conn);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }
    }

    public <S, Q extends EntityPath<R>, R> SearchResultList<S> list(
            @NotNull Class<S> schemaType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException, SchemaException {

        SqlQueryContext<S, Q, R> context =
                SqlQueryContext.from(schemaType, prismContext, querydslConfiguration);
        if (query != null) {
            context.process(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }
        context.processOptions(options);

        PageOf<R> result;
        try (Connection conn = getConnection()) {
            result = context.executeQuery(conn);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }

        PageOf<S> map = context.transformToSchemaType(result);
        return createSearchResultList(map);
    }

    @NotNull
    private <T> SearchResultList<T> createSearchResultList(PageOf<T> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        if (result.isKnownTotalCount()) {
            metadata.setApproxNumberOfAllResults((int) result.totalCount());
        }
        return new SearchResultList<>(result.content(), metadata);
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
