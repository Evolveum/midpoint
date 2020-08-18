/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.helpers.JdbcSession;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Component just under the service that orchestrates query transformation and execution.
 * Sql query executor itself does hold the query state, it uses {@link SqlQueryContext} for that.
 * This object manages configuration information and provides dataSource/connections for queries.
 */
public class SqlQueryExecutor {

    private final BaseHelper baseHelper;
    private final PrismContext prismContext;

    public SqlQueryExecutor(BaseHelper baseHelper, PrismContext prismContext) {
        this.prismContext = prismContext;
        this.baseHelper = baseHelper;
    }

    public <S, Q extends FlexibleRelationalPathBase<R>, R> int count(
            @NotNull Class<S> schemaType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {

        SqlQueryContext<S, Q, R> context =
                SqlQueryContext.from(schemaType, prismContext, baseHelper.querydslConfiguration());
        if (query != null) {
            context.process(query.getFilter());
        }
        // TODO MID-6319: all options can be applied, just like for list?
        context.processOptions(options);

        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startReadOnlyTransaction()) {
            return context.executeCount(jdbcSession.connection());
        }
    }

    public <S, Q extends FlexibleRelationalPathBase<R>, R> SearchResultList<S> list(
            @NotNull Class<S> schemaType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException, SchemaException {

        SqlQueryContext<S, Q, R> context =
                SqlQueryContext.from(schemaType, prismContext, baseHelper.querydslConfiguration());
        if (query != null) {
            context.process(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }
        context.processOptions(options);

        PageOf<R> result;
        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startReadOnlyTransaction()) {
            result = context.executeQuery(jdbcSession.connection());
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
}
