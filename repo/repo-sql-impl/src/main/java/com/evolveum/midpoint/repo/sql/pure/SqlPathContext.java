package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.EntityPath;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;

/**
 * SQL path context with mapping information.
 */
public class SqlPathContext<Q extends EntityPath<R>, R> {

    private final Q path;
    private final QueryModelMapping<?, Q, R> mapping;
    private final PrismContext prismContext;

    public SqlPathContext(Q path, QueryModelMapping<?, Q, R> mapping, PrismContext prismContext) {
        this.path = path;
        this.mapping = mapping;
        this.prismContext = prismContext;
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

    // TODO - will be necessary for mappers requiring model insight, e.g. path->CanonicalItemPath, etc.
    public PrismContext prismContext() {
        return prismContext;
    }
}
