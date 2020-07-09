package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMappingConfig;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditPropertyValue;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.pure.querymodel.support.InstantType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

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

    public <T extends Containerable> SearchResultList<T> list(
            @NotNull Class<T> prismType,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) throws QueryException {

        QueryModelMapping<?, ?> rootMapping = QueryModelMappingConfig.getByModelType(prismType);
        EntityPath<?> root = rootMapping.defaultAlias();
        SqlQueryContext context = new SqlQueryContext(root, rootMapping);
        // TODO: cover with tests, not relevant for Audit though (at least not yet without multiplying joins)
        context.setDistinct(
                GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options)));

        // add conditions (with exists clauses as necessary)
        if (query != null) {
            context.process(query.getFilter());
            context.processObjectPaging(query.getPaging());
        }

        // TODO: what if we declare AuditEventRecordType, but we want transformed result?
        // some mapping function as an argument?
        // some builder to construct the whole definition that will be an argument?
        // The goal is to have "audit" stuff out of general SqlQueryExecutor.

        PageOf<Tuple> result = executeQuery(context);
        // TODO: reading of to many relations here

        AuditEventRecordSqlTransformer transformer = new AuditEventRecordSqlTransformer(prismContext);
        PageOf<AuditEventRecordType> map = result
                .map(t -> (MAuditEventRecord) t.get(root))
                .map(transformer::toAuditEventRecordType);
        //noinspection unchecked
        return (SearchResultList<T>) createSearchResultList(map);
    }

    public PageOf<Tuple> executeQuery(SqlQueryContext context) throws QueryException {
        try (Connection conn = getConnection()) {
            EntityPath<?> root = context.root();
            SQLQuery<Tuple> query = context.query(conn)
                    .select(Projections.tuple(root));
            // TODO logging
            System.out.println("query = " + query);
            long count = query.fetchCount();

            List<Tuple> data = query.fetch();
            data.forEach(t -> loadPropertyValues(t, conn, context));
            return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0, count);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }
    }

    private void loadPropertyValues(Tuple rowTuple, Connection conn, SqlQueryContext context) {
        MAuditEventRecord row = (MAuditEventRecord) Objects.requireNonNull(
                rowTuple.get(context.root()));
        QAuditPropertyValue apv = new QAuditPropertyValue("apv");
        context.newQuery(conn)
                .select(apv)
                .from(apv)
                .where(apv.recordId.eq(row.id))
                .fetch()
                .forEach(p -> row.addProperty(p));
    }

    @NotNull
    public <T extends Containerable> SearchResultList<T> createSearchResultList(PageOf<T> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        metadata.setApproxNumberOfAllResults((int) result.totalCount());
        return new SearchResultList<>(result.content(), metadata);
    }

    private Connection getConnection() throws SQLException {
        return dataSourceFactory.getDataSource().getConnection();
    }
}
