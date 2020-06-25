package com.evolveum.midpoint.repo.sql.pure;

import java.util.ArrayList;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;

/**
 * Component just under the service that orchestrates query transformation and execution.
 */
@Component
public class SqlQueryExecutor {

    @Autowired
    private DataSource dataSource;

    public SearchResultList<AuditEventRecord> list(
            Class<? extends Containerable> prismType, ObjectQuery query) {
        // TODO: what if we declare AuditEventRecordType, but we want transformed result?
        // some mapping function as an argument?
        // some builder to construct the whole definition that will be an argument?

        SearchResultMetadata metadata = new SearchResultMetadata();
        SearchResultList<AuditEventRecord> result = new SearchResultList<>(
                new ArrayList<>(),
                metadata);

        return result;
    }
}
