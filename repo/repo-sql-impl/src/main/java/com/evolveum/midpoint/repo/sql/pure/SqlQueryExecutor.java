package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Component just under the service that orchestrates query transformation and execution.
 */
@Component
public class SqlQueryExecutor {

    // TODO configuration should reflect the used DB of course
    public static final Configuration QUERYDSL_CONFIGURATION =
            new Configuration(SQLTemplates.DEFAULT);

    @Autowired
    private DataSourceFactory dataSourceFactory;

    public <T extends Containerable> SearchResultList<T> list(
            Class<T> prismType, // ignored for the moment
            ObjectQuery query) throws SQLException {
        // TODO: what if we declare AuditEventRecordType, but we want transformed result?
        // some mapping function as an argument?
        // some builder to construct the whole definition that will be an argument?

        PageOf<MAuditEventRecord> result = executeQuery();

        PageOf<AuditEventRecordType> map = result
                .map(AuditEventRecordSqlTransformer::toAuditEventRecordType);
        //noinspection unchecked
        return (SearchResultList<T>) createSearchResultList(map);
    }

    @NotNull
    public <T extends Containerable> SearchResultList<T> createSearchResultList(PageOf<T> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        metadata.setApproxNumberOfAllResults((int) result.totalCount());
        return new SearchResultList<>(result.content(), metadata);
    }

    public PageOf<MAuditEventRecord> executeQuery() throws SQLException {
        try (Connection connection = getConnection()) {
            QAuditEventRecord aer = new QAuditEventRecord();
            SQLQuery<MAuditEventRecord> query = newQuery(connection)
                    .select(aer)
                    .from(aer)
                    // TODO add paging
//                    .offset(2)
//                    .limit(2)
                    ;
            long count = query.fetchCount();

            return new PageOf<>(query.fetch(), PageOf.PAGE_NO_PAGINATION, 0, count);
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSourceFactory.getDataSource().getConnection();
    }

    @NotNull
    public SQLQuery<Object> newQuery(Connection connection) {
        return new SQLQuery<>(connection, QUERYDSL_CONFIGURATION);
    }
}
