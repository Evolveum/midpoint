package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.EntityPath;

import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;

/**
 * SQL path context with mapping information.
 */
public class SqlPathContext<Q extends EntityPath<R>, R> {

    private final Q path;
    private final QueryModelMapping<?, Q, R> mapping;

    public SqlPathContext(Q path, QueryModelMapping<?, Q, R> mapping) {
        this.path = path;
        this.mapping = mapping;
    }

    public Q path() {
        return path;
    }

    public <T extends FlexibleRelationalPathBase<?>> T path(Class<T> pathType) {
        return pathType.cast(path);
    }

    public QueryModelMapping<?, Q, R> mapping() {
        return mapping;
    }
}
