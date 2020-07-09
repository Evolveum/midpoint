package com.evolveum.midpoint.repo.sql.pure;

import static java.util.stream.Collectors.toMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.querydsl.core.types.EntityPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMappingConfig;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditPropertyValue;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditPropertyValue;
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

    /**
     * If no other limit is used for query this limit will be used for sanity reasons.
     */
    public static final long NO_PAGINATION_LIMIT = 10_000;

    /**
     * Number of values (identifiers) used in the IN clause to-many fetching selects.
     * This works effectively as factor of how bad N+1 select is, it's at most N/this-limit+1 bad.
     * For obvious reasons, this works only for non-composite PKs (IDs) on the master entity.
     */
    public static final int MAX_ID_IN_FOR_TO_MANY_FETCH = 100;

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
        Q root = rootMapping.defaultAlias();
        SqlQueryContext<Q, R> context = new SqlQueryContext<>(root, rootMapping);
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
            Q root = context.root();
            SQLQuery<R> query = context.query(conn)
                    .select(root);
            if (query.getMetadata().getModifiers().getLimit() == null) {
                query.limit(NO_PAGINATION_LIMIT);
            }
            // TODO logging
            System.out.println("SQL query = " + query);

            long count = query.fetchCount();
            List<R> data = query.fetch();

            // TODO MID-6319: reading of to many relations here - generalize it
            loadPropertyValues(context, conn, data);

            return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0, count);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }
    }

    private <Q extends EntityPath<R>, R> void loadPropertyValues(
            SqlQueryContext<Q, R> context, Connection conn, List<R> data) {
        if (data.size() > MAX_ID_IN_FOR_TO_MANY_FETCH) {
            Lists.partition(data, MAX_ID_IN_FOR_TO_MANY_FETCH)
                    .forEach(partition -> loadPropertyValues(context, conn, partition));
        }

        // TODO MID-6319: fix generalization, up to here it's parametrized already
        Map<Long, MAuditEventRecord> rowById = ((List<MAuditEventRecord>) data).stream()
                .collect(toMap(row -> row.id, row -> row));
        QAuditPropertyValue apv = new QAuditPropertyValue("apv");
        SQLQuery<MAuditPropertyValue> query = context.newQuery(conn)
                .select(apv)
                .from(apv)
                .where(apv.recordId.in(rowById.keySet()));
        System.out.println("SQL query = " + query);
        List<MAuditPropertyValue> details = query.fetch();
        for (MAuditPropertyValue detail : details) {
            rowById.get(detail.recordId).addProperty(detail);
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
