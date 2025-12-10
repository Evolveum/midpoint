/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Component just under the service that orchestrates query transformation and execution.
 * Sql query executor itself does hold the query state, it uses {@link SqlQueryContext} for that.
 * This object manages configuration information and provides dataSource/connections for queries.
 */
public class SqlQueryExecutor {

    private final SqlRepoContext sqlRepoContext;

    public SqlQueryExecutor(SqlRepoContext sqlRepoContext) {
        this.sqlRepoContext = sqlRepoContext;
    }

    public <S, Q extends FlexibleRelationalPathBase<R>, R> int count(
            @NotNull SqlQueryContext<S, Q, R> context,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {

        if (query != null) {
            context.processFilter(query.getFilter());
        }
        // TODO MID-6319: all options can be applied, just like for list?
        context.processOptions(options);

        context.beforeQuery();
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            return context.executeCount(jdbcSession);
        }
    }

    public @NotNull <S, Q extends FlexibleRelationalPathBase<R>, R> SearchResultList<S> list(
            @NotNull SqlQueryContext<S, Q, R> context,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException, SchemaException {

        if (query != null) {
            context.processFilter(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }
        context.processOptions(options);

        context.beforeQuery();
        PageOf<Tuple> result;
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            try (var ignored = SqlBaseOperationTracker.fetchMultiplePrimaries()){
                result = context.executeQuery(jdbcSession);
            }
            PageOf<S> transformedResult = context.transformToSchemaType(result, jdbcSession);
            return createSearchResultList(transformedResult);
        }
    }

    @NotNull
    private <T> SearchResultList<T> createSearchResultList(PageOf<T> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        if (result.isKnownTotalCount()) {
            metadata.setApproxNumberOfAllResults((int) result.totalCount());
        }
        return new SearchResultList<>(result.content(), metadata);
    }

    /**
     * Streaming iterative search that processes rows one by one without loading all into memory.
     * Uses JDBC cursor-based streaming with configurable fetch size.
     *
     * @param context query context
     * @param query object query (may be null)
     * @param options get operation options
     * @param handler handler called for each transformed result
     * @param operationResult operation result for handler
     * @param fetchSize JDBC fetch size for streaming
     * @return number of processed items
     */
    public <S, Q extends FlexibleRelationalPathBase<R>, R> int listStreaming(
            @NotNull SqlQueryContext<S, Q, R> context,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ObjectHandler<S> handler,
            @NotNull OperationResult operationResult,
            int fetchSize)
            throws RepositoryException, SchemaException {

        if (query != null) {
            context.processFilter(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }
        context.processOptions(options);
        context.beforeQuery();

        int count = 0;
        // PostgreSQL streaming requires autoCommit=false, use read-only transaction for optimization
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            try (Stream<Tuple> stream = context.executeQueryStreaming(jdbcSession, fetchSize)) {
                ResultListRowTransformer<S, Q, R> rowTransformer =
                        context.mapping().createRowTransformer(context, jdbcSession, options);

                // Note: beforeTransformation with empty list - streaming doesn't support bulk pre-processing
                rowTransformer.beforeTransformation(java.util.Collections.emptyList(), context.path());

                Q entityPath = context.path();
                Iterator<Tuple> iterator = stream.iterator();

                while (iterator.hasNext()) {
                    Tuple tuple = iterator.next();
                    S transformed = rowTransformer.transform(tuple, entityPath);

                    if (!handler.handle(transformed, operationResult)) {
                        break;
                    }
                    count++;
                }

                rowTransformer.finishTransformation();
            }

            jdbcSession.commit();
            return count;
        }
    }
}
