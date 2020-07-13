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
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMappingConfig;
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

    // TODO configuration should reflect the used DB of course (if necessary)
    public static final Configuration QUERYDSL_CONFIGURATION =
            new Configuration(SQLTemplates.DEFAULT);

    static {
        // See InstantType javadoc for the reasons why we need this to support Instant.
        QUERYDSL_CONFIGURATION.register(new InstantType());
        // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
    }

    @Autowired private PrismContext prismContext;
    @Autowired private DataSourceFactory dataSourceFactory;

    public <M, Q extends EntityPath<R>, R>
    SearchResultList<M> list(
            @NotNull Class<M> prismType,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws QueryException {

        QueryModelMapping<M, Q, R> rootMapping = QueryModelMappingConfig.getByModelType(prismType);
        SqlQueryContext<Q, R> context = new SqlQueryContext<>(rootMapping, prismContext);
        // TODO: cover with tests, not relevant for Audit though (at least not yet without multiplying joins)
        context.setDistinct(
                GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options)));

        // add conditions (with exists clauses as necessary)
        if (query != null) {
            context.process(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }

        PageOf<R> result = executeQuery(context);

        SqlTransformer<M, R> transformer = rootMapping.createTransformer(prismContext);
        PageOf<M> map = result
                .map(transformer::toSchemaObject);
        return createSearchResultList(map);
    }

    private <Q extends EntityPath<R>, R>
    PageOf<R> executeQuery(SqlQueryContext<Q, R> context) throws QueryException {
        try (Connection conn = getConnection()) {
            return context.executeQuery(conn);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }
    }

    @NotNull
    private <M> SearchResultList<M> createSearchResultList(PageOf<M> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        metadata.setApproxNumberOfAllResults((int) result.totalCount());
        return new SearchResultList<>(result.content(), metadata);
    }

    private Connection getConnection() throws SQLException {
        return dataSourceFactory.getDataSource().getConnection();
    }
}
