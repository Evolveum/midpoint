package com.evolveum.midpoint.repo.sql.pure.mapping;

import com.querydsl.core.types.EntityPath;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Holds {@link QueryModelMapping} instances obtainable by various key (e.g. model type Q-name).
 */
public class QueryModelMappingConfig {

    private static final QueryModelMappingRegistry REGISTRY = new QueryModelMappingRegistry()
            .register(AuditEventRecordType.COMPLEX_TYPE, QAuditEventRecordMapping.INSTANCE)
            .register(QAuditItemMapping.INSTANCE);

    public static <M, Q extends EntityPath<R>, R>
    QueryModelMapping<M, Q, R> getByModelType(Class<M> modelType) {
        //noinspection unchecked
        return (QueryModelMapping<M, Q, R>) REGISTRY.getByModelType(modelType);
    }

    public static <M, Q extends EntityPath<R>, R>
    QueryModelMapping<M, Q, R> getByQueryType(Class<Q> queryType) {
        //noinspection unchecked
        return (QueryModelMapping<M, Q, R>) REGISTRY.getByQueryType(queryType);
    }
}
