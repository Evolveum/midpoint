package com.evolveum.midpoint.repo.sql.pure.mapping;

import com.querydsl.core.types.EntityPath;

import com.evolveum.midpoint.repo.sql.pure.querymodel.mapping.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Holds {@link QueryModelMapping} instances obtainable by various key (e.g. model type Q-name).
 */
public class QueryModelMappingConfig {

    private static final QueryModelMappingRegistry REGISTRY = new QueryModelMappingRegistry()
            .register(AuditEventRecordType.COMPLEX_TYPE, QAuditEventRecordMapping.INSTANCE)
            .register(QAuditItemMapping.INSTANCE)
            .register(QAuditPropertyValueMapping.INSTANCE)
            .register(QAuditRefValueMapping.INSTANCE)
            .register(QAuditResourceMapping.INSTANCE)
            .register(QAuditDeltaMapping.INSTANCE);

    // see QueryModelMapping javadoc for type parameter explanation
    public static <S, Q extends EntityPath<R>, R>
    QueryModelMapping<S, Q, R> getBySchemaType(Class<S> schemaType) {
        return REGISTRY.getBySchemaType(schemaType);
    }

    public static <S, Q extends EntityPath<R>, R>
    QueryModelMapping<S, Q, R> getByQueryType(Class<Q> queryType) {
        return REGISTRY.getByQueryType(queryType);
    }
}
