package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.metamodel.QAuditEventRecord;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;

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

        List<MAuditEventRecord> records = executeQuery();

        SearchResultMetadata metadata = new SearchResultMetadata();
        return new SearchResultList<>(
                new ArrayList<>(),
                metadata);
    }

    public List<MAuditEventRecord> executeQuery() throws SQLException {
        try (Connection connection = getConnection()) {
            QAuditEventRecord aer = new QAuditEventRecord();
            return newQuery(connection)
                    .select(aer)
                    .from(aer)
                    .limit(3)
                    .fetch();
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
