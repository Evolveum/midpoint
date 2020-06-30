package com.evolveum.midpoint.repo.sql.pure;

import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;

/**
 * SQL path context with mapping information.
 */
public class SqlPathContext {

    private final FlexibleRelationalPathBase<?> path;
    private final QueryModelMapping<?, ?> mapping;

    public SqlPathContext(FlexibleRelationalPathBase<?> path, QueryModelMapping<?, ?> mapping) {
        this.path = path;
        this.mapping = mapping;
    }

    public FlexibleRelationalPathBase<?> path() {
        return path;
    }

    public <T extends FlexibleRelationalPathBase<?>> T path(Class<T> pathType) {
        return pathType.cast(path);
    }

    public QueryModelMapping<?, ?> mapping() {
        return mapping;
    }
}
