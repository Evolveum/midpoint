package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.EntityPath;

import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;

/**
 * SQL path context with mapping information.
 */
public class SqlPathContext {

    private final EntityPath<?> path;
    private final QueryModelMapping<?, ?> mapping;

    public SqlPathContext(EntityPath<?> path, QueryModelMapping<?, ?> mapping) {
        this.path = path;
        this.mapping = mapping;
    }

    public EntityPath<?> path() {
        return path;
    }

    public <T extends FlexibleRelationalPathBase<?>> T path(Class<T> pathType) {
        return pathType.cast(path);
    }

    public QueryModelMapping<?, ?> mapping() {
        return mapping;
    }
}
