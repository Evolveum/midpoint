package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import com.querydsl.core.types.EntityPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.querymodel.support.InstantType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * Component just under the service that orchestrates query transformation and execution.
 */
@Component
public class SqlQueryExecutor {

    public static final Configuration QUERYDSL_CONFIGURATION =
            new Configuration(SQLTemplates.DEFAULT);

    static {
        // See InstantType javadoc for the reasons why we need this to support Instant.
        QUERYDSL_CONFIGURATION.register(new InstantType());
        // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
    }

    @Autowired private PrismContext prismContext;
    @Autowired private DataSourceFactory dataSourceFactory;

    public <S, Q extends EntityPath<R>, R> int count(
            @NotNull Class<S> schemaType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {

        SqlQueryContext<S, Q, R> context = SqlQueryContext.from(schemaType, prismContext);
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
            throws QueryException {

        SqlQueryContext<S, Q, R> context = SqlQueryContext.from(schemaType, prismContext);
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
        metadata.setApproxNumberOfAllResults((int) result.totalCount());
        return new SearchResultList<>(result.content(), metadata);
    }

    private Connection getConnection() throws SQLException {
        return dataSourceFactory.getDataSource().getConnection();
    }
}
